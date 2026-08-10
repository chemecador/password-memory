import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSigner;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Verifies the JAR signature used by an Android App Bundle.
 *
 * <p>The verifier reads every payload entry so the JDK validates every digest and signature. It
 * rejects unsigned payload entries, mixed signer sets, invalid certificates, archives missing
 * basic AAB markers, and bundles not signed by the configured upload key. The release script runs
 * bundletool separately for semantic AAB validation. Passwords are read inside this short-lived
 * process and are never accepted as command-line arguments or printed.
 */
public final class VerifySignedBundle {

    private static final int BUFFER_SIZE = 16 * 1024;
    private static final String BUNDLE_CONFIG_ENTRY = "BundleConfig.pb";
    private static final String BASE_MANIFEST_ENTRY = "base/manifest/AndroidManifest.xml";

    private VerifySignedBundle() {
    }

    public static void main(String[] args) {
        try {
            if (args.length == 3 && args[0].equals("--check-keystore")) {
                SigningIdentity identity = loadSigningIdentity(
                        Path.of(args[1]),
                        Path.of(args[2])
                );
                printSigningIdentity(identity);
                return;
            }

            if (args.length == 5
                    && args[0].equals("--verify")
                    && args[2].equals("--properties")) {
                SigningIdentity identity = loadSigningIdentity(
                        Path.of(args[3]),
                        Path.of(args[4])
                );
                verifyBundle(Path.of(args[1]), identity.fingerprint);
                return;
            }

            if (args.length == 4
                    && args[0].equals("--verify")
                    && args[2].equals("--expected-sha256")) {
                verifyBundle(Path.of(args[1]), normalizeFingerprint(args[3]));
                return;
            }

            fail(
                    "Usage: java VerifySignedBundle.java --check-keystore "
                            + "<keystore.properties> <repository-root>\n"
                            + "   or: java VerifySignedBundle.java --verify <bundle.aab> "
                            + "--properties <keystore.properties> <repository-root>\n"
                            + "   or: java VerifySignedBundle.java --verify <bundle.aab> "
                            + "--expected-sha256 <certificate-fingerprint>"
            );
        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    private static SigningIdentity loadSigningIdentity(
            Path propertiesFile,
            Path repositoryRoot
    ) throws Exception {
        Path normalizedProperties = propertiesFile.toAbsolutePath().normalize();
        Path normalizedRoot = repositoryRoot.toRealPath();
        if (!Files.isRegularFile(normalizedProperties)) {
            throw new IllegalArgumentException(
                    "Signing properties do not exist: " + normalizedProperties
            );
        }

        DuplicateRejectingProperties properties = new DuplicateRejectingProperties();
        try (Reader reader = Files.newBufferedReader(
                normalizedProperties,
                StandardCharsets.UTF_8
        )) {
            properties.load(reader);
        }

        String configuredStoreFile = requiredProperty(properties, "storeFile");
        String storePasswordValue = requiredProperty(properties, "storePassword");
        String alias = requiredProperty(properties, "keyAlias");
        String keyPasswordValue = requiredProperty(properties, "keyPassword");

        Path configuredPath = Path.of(configuredStoreFile);
        Path storeFile = (configuredPath.isAbsolute()
                ? configuredPath
                : normalizedRoot.resolve(configuredPath))
                .normalize();
        if (!Files.isRegularFile(storeFile)) {
            throw new IllegalArgumentException(
                    "The configured upload keystore does not exist"
            );
        }
        Path realStoreFile = storeFile.toRealPath();
        if (realStoreFile.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException(
                    "The upload keystore must be stored outside the repository"
            );
        }
        if (alias.equalsIgnoreCase("androiddebugkey")) {
            throw new IllegalArgumentException("The Android debug key cannot be an upload key");
        }

        char[] storePassword = storePasswordValue.toCharArray();
        char[] keyPassword = keyPasswordValue.toCharArray();
        properties.remove("storePassword");
        properties.remove("keyPassword");

        try {
            KeyStore keyStore = KeyStore.getInstance(realStoreFile.toFile(), storePassword);
            if (!keyStore.isKeyEntry(alias)) {
                throw new IllegalArgumentException(
                        "The configured alias is not a private-key entry"
                );
            }

            Key key = keyStore.getKey(alias, keyPassword);
            if (!(key instanceof PrivateKey)) {
                throw new IllegalArgumentException(
                        "The configured alias does not contain a private key"
                );
            }

            Certificate certificate = keyStore.getCertificate(alias);
            if (!(certificate instanceof X509Certificate)) {
                throw new IllegalArgumentException(
                        "The configured alias does not contain an X.509 certificate"
                );
            }

            X509Certificate x509 = (X509Certificate) certificate;
            x509.checkValidity();
            rejectDebugOrWeakCertificate(alias, x509);

            PrivateKey privateKey = (PrivateKey) key;
            if (!privateKey.getAlgorithm().equalsIgnoreCase(
                    x509.getPublicKey().getAlgorithm()
            )) {
                throw new IllegalArgumentException(
                        "The private key and certificate use different algorithms"
                );
            }

            return new SigningIdentity(
                    sha256Fingerprint(x509),
                    x509.getNotAfter().toInstant()
            );
        } finally {
            Arrays.fill(storePassword, '\0');
            Arrays.fill(keyPassword, '\0');
        }
    }

    private static void verifyBundle(Path bundlePath, String expectedSigner) throws Exception {
        Path bundle = bundlePath.toAbsolutePath().normalize();
        if (!Files.isRegularFile(bundle)) {
            throw new IllegalArgumentException("Bundle does not exist: " + bundle);
        }
        if (!bundle.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".aab")) {
            throw new IllegalArgumentException("Expected an .aab file: " + bundle);
        }

        Set<String> expectedSignerFingerprints = null;
        Instant earliestExpiration = null;
        int verifiedPayloadEntries = 0;
        boolean hasBundleConfig = false;
        boolean hasBaseManifest = false;

        try (JarFile jar = new JarFile(bundle.toFile(), true)) {
            Enumeration<JarEntry> entries = jar.entries();
            byte[] buffer = new byte[BUFFER_SIZE];

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || isSignatureMetadata(entry.getName())) {
                    continue;
                }

                readFully(jar, entry, buffer);
                if (entry.getName().equals(BUNDLE_CONFIG_ENTRY)) {
                    hasBundleConfig = true;
                } else if (entry.getName().equals(BASE_MANIFEST_ENTRY)) {
                    hasBaseManifest = true;
                }

                CodeSigner[] signers = entry.getCodeSigners();
                if (signers == null || signers.length == 0) {
                    throw new SecurityException("Unsigned payload entry: " + entry.getName());
                }

                Set<String> entryFingerprints = new LinkedHashSet<>();
                for (CodeSigner signer : signers) {
                    Certificate certificate = signer.getSignerCertPath().getCertificates().get(0);
                    if (!(certificate instanceof X509Certificate)) {
                        throw new SecurityException(
                                "Non-X.509 signer on payload entry: " + entry.getName()
                        );
                    }

                    X509Certificate x509 = (X509Certificate) certificate;
                    x509.checkValidity();
                    rejectDebugOrWeakCertificate("", x509);
                    entryFingerprints.add(sha256Fingerprint(x509));
                    Instant expiration = x509.getNotAfter().toInstant();
                    if (earliestExpiration == null || expiration.isBefore(earliestExpiration)) {
                        earliestExpiration = expiration;
                    }
                }

                if (expectedSignerFingerprints == null) {
                    expectedSignerFingerprints = entryFingerprints;
                } else if (!expectedSignerFingerprints.equals(entryFingerprints)) {
                    throw new SecurityException(
                            "Payload entries are not signed by one consistent signer set"
                    );
                }
                verifiedPayloadEntries++;
            }
        } catch (SecurityException exception) {
            throw new SecurityException("Invalid bundle signature: " + exception.getMessage());
        }

        if (!hasBundleConfig || !hasBaseManifest) {
            throw new SecurityException(
                    "The archive is missing required Android App Bundle entries"
            );
        }
        if (verifiedPayloadEntries == 0 || expectedSignerFingerprints == null) {
            throw new SecurityException("The bundle contains no signed payload entries");
        }
        if (expectedSignerFingerprints.size() != 1
                || !expectedSignerFingerprints.contains(expectedSigner)) {
            throw new SecurityException(
                    "The AAB signer does not match the expected upload certificate"
            );
        }

        System.out.println("Signed AAB verification passed.");
        System.out.println("Verified payload entries: " + verifiedPayloadEntries);
        System.out.println("Signer SHA-256: " + expectedSigner);
        System.out.println("Signer certificate valid until: " + earliestExpiration);
    }

    private static String requiredProperty(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing or blank '" + name + "' in keystore.properties"
            );
        }

        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("replace-with")
                || normalized.equals("changeme")
                || normalized.equals("change-me")) {
            throw new IllegalArgumentException(
                    "Example placeholder remains in '" + name + "'"
            );
        }
        return value;
    }

    private static void rejectDebugOrWeakCertificate(
            String alias,
            X509Certificate certificate
    ) {
        String subject = certificate.getSubjectX500Principal()
                .getName()
                .toLowerCase(Locale.ROOT);
        if (alias.equalsIgnoreCase("androiddebugkey")
                || subject.contains("cn=android debug")) {
            throw new IllegalArgumentException(
                    "The Android debug certificate cannot sign a Play release"
            );
        }

        PublicKey publicKey = certificate.getPublicKey();
        if (publicKey instanceof RSAPublicKey) {
            int keySize = ((RSAPublicKey) publicKey).getModulus().bitLength();
            if (keySize < 2048) {
                throw new IllegalArgumentException(
                        "The RSA signing key must be at least 2048 bits"
                );
            }
            return;
        }
        if (publicKey instanceof ECPublicKey) {
            int keySize = ((ECPublicKey) publicKey)
                    .getParams()
                    .getCurve()
                    .getField()
                    .getFieldSize();
            if (keySize < 256) {
                throw new IllegalArgumentException(
                        "The EC signing key must be at least 256 bits"
                );
            }
            return;
        }
        throw new IllegalArgumentException(
                "Unsupported signing-key algorithm: " + publicKey.getAlgorithm()
        );
    }

    private static void readFully(JarFile jar, JarEntry entry, byte[] buffer) throws IOException {
        try (InputStream input = jar.getInputStream(entry)) {
            while (input.read(buffer) != -1) {
                // Reading every byte triggers the JDK's signature and digest verification.
            }
        }
        Arrays.fill(buffer, (byte) 0);
    }

    private static boolean isSignatureMetadata(String entryName) {
        String upper = entryName.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("META-INF/")) {
            return false;
        }

        String fileName = upper.substring("META-INF/".length());
        if (fileName.contains("/")) {
            return false;
        }
        return fileName.equals("MANIFEST.MF")
                || fileName.endsWith(".SF")
                || fileName.endsWith(".RSA")
                || fileName.endsWith(".DSA")
                || fileName.endsWith(".EC")
                || fileName.startsWith("SIG-");
    }

    private static String sha256Fingerprint(X509Certificate certificate) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
        StringBuilder fingerprint = new StringBuilder(digest.length * 3 - 1);
        for (int index = 0; index < digest.length; index++) {
            if (index > 0) {
                fingerprint.append(':');
            }
            fingerprint.append(String.format(Locale.ROOT, "%02X", digest[index]));
        }
        Arrays.fill(digest, (byte) 0);
        return fingerprint.toString();
    }

    private static String normalizeFingerprint(String value) {
        String compact = value.replace(":", "")
                .replace("-", "")
                .replaceAll("\\s", "")
                .toUpperCase(Locale.ROOT);
        if (!compact.matches("[0-9A-F]{64}")) {
            throw new IllegalArgumentException(
                    "Expected signer SHA-256 must contain exactly 64 hexadecimal digits"
            );
        }

        StringBuilder formatted = new StringBuilder(95);
        for (int index = 0; index < compact.length(); index += 2) {
            if (index > 0) {
                formatted.append(':');
            }
            formatted.append(compact, index, index + 2);
        }
        return formatted.toString();
    }

    private static void printSigningIdentity(SigningIdentity identity) {
        System.out.println("Upload keystore verification passed.");
        System.out.println("Expected signer SHA-256: " + identity.fingerprint);
        System.out.println("Signer certificate valid until: " + identity.expiration);
    }

    private static void fail(String message) {
        System.err.println("Bundle signature verification failed: " + message);
        System.exit(1);
    }

    private static final class SigningIdentity {
        private final String fingerprint;
        private final Instant expiration;

        private SigningIdentity(String fingerprint, Instant expiration) {
            this.fingerprint = fingerprint;
            this.expiration = expiration;
        }
    }

    private static final class DuplicateRejectingProperties extends Properties {
        private static final long serialVersionUID = 1L;

        @Override
        public synchronized Object put(Object key, Object value) {
            if (containsKey(key)) {
                throw new IllegalArgumentException(
                        "Duplicate property in keystore.properties: " + key
                );
            }
            return super.put(key, value);
        }
    }
}
