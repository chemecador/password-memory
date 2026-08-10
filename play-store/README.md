# Google Play release material

This directory contains the source material for the Password Memory store listing.
It does not contain signing keys or credentials.

## Listing

Copy the localized text from:

- `listings/en-US/`
- `listings/es-ES/`

The proposed privacy-policy URL is:

`https://github.com/chemecador/password-memory/blob/main/PRIVACY.md`

The current `PRIVACY.md` is published on the public `main` branch and was
verified without authentication on August 10, 2026. Recheck the URL before each
submission. The developer contact in that policy must also match the public
contact configured in Play Console.

Use the established values already visible on the production Secretaria listing:
developer name **Chemecador**, legal identity **Alejandro Gata Gascón**, country
**Spain**, and public/support email **chemecador@gmail.com**.

## Data safety

`data-safety.md` records the answers supported by the current source code. Recheck
the declaration whenever a permission, SDK, data flow, or external service changes.

`app-content.md` contains the draft Play Console declarations and the reviewer
instructions needed because the vault is protected by the review device's
biometric or device credential.

## Release artifacts

Create or locate the real upload key before generating the Play artifact. Do not
copy Secretaria's tracked KMP release signing configuration: it assigns the debug
certificate and is not evidence of the upload key used by Play. Android Studio's
**Generate Signed Bundle / APK** wizard is suitable. If `keytool` is
used instead, let it prompt interactively so passwords never appear in command
history; use RSA with at least 2048 bits and a long certificate validity.

The canonical release gate is:

```powershell
.\play-store\verify-release.ps1
```

It requires `keystore.properties`, runs clean JVM tests, release lint, and the
release bundle with the Gradle daemon and configuration cache disabled. It then
uses AGP's bundled `bundletool` for semantic validation and cryptographically
reads every AAB payload entry to prove that:

- the archive is a semantically valid Android App Bundle;
- every payload entry has a valid signature;
- all entries have exactly one consistent signer;
- that signer is the certificate belonging to the configured upload-key alias;
- the certificate is current, non-debug, and uses a modern RSA or EC key.

The script prints only the public certificate fingerprint and final AAB hash. It
never accepts or forwards signing passwords as command-line arguments. Only
upload an AAB after this command succeeds.

To enable release signing:

1. Create and protect a dedicated upload keystore outside the repository.
2. Copy `keystore.properties.example` to `keystore.properties`.
3. Replace every example value with the upload-key details.
4. Run `.\play-store\verify-release.ps1`.
5. Record the reported signer fingerprint and AAB SHA-256 with the release.

To verify an already-built AAB without exposing the keystore, use the public
SHA-256 fingerprint of its expected upload certificate:

```powershell
.\play-store\verify-release.ps1 `
    -VerifyOnly `
    -BundlePath C:\path\to\app-release.aab `
    -ExpectedSignerSha256 AA:BB:CC:...
```

Without `-ExpectedSignerSha256`, verify-only mode requires the ignored
`keystore.properties` and compares against its configured alias.

When Play App Signing is enabled, Google protects the app-signing key and this
local key is the replaceable upload key. Back up both the keystore and its
credentials in a separate secure location.

## Assets

The store icon and localized feature graphics are generated in `assets/` by
`generate-assets.ps1`.

The ready-to-upload phone screenshots are under:

- `assets/screenshots/en-US/`
- `assets/screenshots/es-ES/`

Each locale contains eight 1080 x 1920 JPEG files. They use representative,
non-secret sample data and meet Google Play's phone-screenshot limits. Suggested
accessible descriptions are recorded in `assets/screenshots/alt-text.md`.

To reproduce them on a connected Android emulator:

```powershell
.\play-store\capture-screenshots.ps1 -DeviceId emulator-5554
```

The script installs the debug and instrumented-test APKs, temporarily sets the
emulator to 1080 x 1920, captures both locales, restores the previous display and
locale settings, and validates every exported JPEG. Pass `-SkipBuild` only when
the current APKs are already installed.

`PlayStoreScreenshotTest` renders the real production composables using a
temporary in-memory database and public test-only credentials. It never writes
those fixtures to the installed vault or Android Keystore. The test host is
intentionally separate from `MainActivity` and `AutofillUnlockActivity`, so both
production activities keep `FLAG_SECURE`.

The unlock asset is a deterministic still frame from the real Compose animation.
Google Play screenshots are static JPEG or PNG files; showing the complete
animation would require an optional YouTube preview video. Never use real
credentials in a store asset and never disable `FLAG_SECURE` in production for
marketing capture.
