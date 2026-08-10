# Google Play readiness report

Assessment date: August 10, 2026

## Verdict

The repository, release code, and signed AAB are ready for an internal Play
Console upload. Production still depends on completing the external Play Console
declarations and rollout gates listed below.

## Verified

- 100 JVM tests passed with no failures or skipped tests.
- 11 instrumented tests passed on Android 16 / API 36:
  - the full Room migration chain;
  - absence of network and advertising permissions in the installed APK;
  - system backup disabled in the installed APK;
  - `FLAG_SECURE` on `MainActivity`;
  - all eight deterministic Play Store screenshot renders.
- 3 instrumented tests passed on Android 8.0 / API 26:
  - the full Room migration chain from version 1 to the current schema;
  - the installed APK security configuration;
  - launch-time enforcement of `FLAG_SECURE` on `MainActivity`.
- A separate API 26 cold-process smoke launch completed successfully with no
  fatal exception.
- `lintRelease` completed with 0 errors. Its 32 remaining warnings are dependency
  update notices or intentional compatibility/cosmetic warnings; no correctness
  or security warning remains.
- R8 release optimization completed and generated a mapping file.
- The release AAB was generated with R8, signed by the protected upload key, and
  passed semantic and cryptographic verification on August 10, 2026. The
  verifier checked 474 signed payload entries. The upload certificate SHA-256 is
  `3F:3F:23:5D:3D:E1:DD:EB:2C:20:1F:09:E7:97:26:32:4A:D4:2E:89:CE:F6:DA:63:27:A1:B8:B1:90:BA:09:04`,
  and the verified AAB SHA-256 was
  `F03B4108144C32A7CADE5EAD2145D37982A853CFE863D6805264BACEC45AB008`.
- The release verifier uses AGP's bundled `bundletool` for semantic AAB
  validation, rejects unsigned AABs, validates every bundle entry
  cryptographically, and requires an exact match with the configured upload-key
  certificate. Its positive, unsigned, and wrong-signer paths were exercised,
  and a marker-only fake bundle was rejected before signature verification. No
  test key or password remains in the repository.
- The release APK passed `zipalign -c -P 16 -v 4`; all four packaged native
  libraries satisfy 16 KiB alignment.
- The merged release manifest targets API 36, has no `INTERNET`,
  `ACCESS_NETWORK_STATE`, or advertising-ID permission, and disables Android
  cloud backup and device transfer.
- Backup export/import is all-or-nothing, imports are size-bounded, and new
  backup passphrases require 12 characters plus confirmation.
- Autofill displays both untrusted origin identifiers and requires explicit
  confirmation before authentication and disclosure.
- The privacy policy is available in English and Spanish inside the app and as
  `PRIVACY.md`; it now identifies the established publisher and direct monitored
  contact used by the existing production app.
- The live Secretaria listing on the same account confirms the reusable public
  values: Chemecador, Alejandro Gata Gascón, Spain, and
  `chemecador@gmail.com`.
- Listing copy, Data safety answers, icon, localized feature graphics, and
  sixteen representative phone screenshots are prepared under `play-store/`.
  The screenshot set contains eight English and eight Spanish 1080 x 1920 JPEGs,
  uses only test fixtures, and was visually reviewed after automated format
  validation.

## Remaining before the first Play upload

1. Confirm that the existing Play Console identity and its private contact email
   and phone remain verified. The developer states that this is the long-standing
   account already used for Secretaria, so new-personal-account device
   verification is recorded as not applicable.
2. Confirm ownership/availability of `com.chemecador.passwordmemory` and inspect
   its Android developer-verification registration status. Play attempts
   automatic registration; registration is mandatory by September 30, 2026.

## Required before review or production

1. Configure this app with public developer name **Chemecador** and support
   email **chemecador@gmail.com**. The public and in-app policies already
   identify Chemecador (Alejandro Gata Gascón), Spain, and use that monitored
   address.
2. Enter the published privacy-policy URL in Play Console and recheck it without
   authentication before submitting:
   `https://github.com/chemecador/password-memory/blob/main/PRIVACY.md`.
3. Enroll in Play App Signing and complete the Data safety and App content
   declarations using `data-safety.md` and `app-content.md`, including reviewer
   instructions for the device-authentication gate.
4. The 12-tester/14-continuous-day gate is recorded as not applicable because
   this is the long-standing Play Console account already publishing Secretaria.
   Reopen it only if the reported account age referred to Gmail rather than the
   Play developer account.

An internal test, staged rollout, and review of the automatically generated
pre-launch report remain strongly recommended quality gates, but are not
unconditional Play publication requirements.
