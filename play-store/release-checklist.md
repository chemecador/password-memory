# Password Memory release checklist

## Repository gates

- [x] JVM unit tests pass.
- [x] Instrumented Room migration and security tests pass on the current target API.
- [x] A smoke test and the migration/security tests pass on Android 8.0 / API 26.
- [x] `lintRelease` finishes with no errors.
- [x] `bundleRelease` finishes successfully with R8 optimization enabled.
- [x] The release AAB is signed with the upload key.
- [x] `play-store/verify-release.ps1` passes and its signer fingerprint plus AAB
      SHA-256 are recorded with the release.
- [x] The merged release manifest contains no `INTERNET` or advertising permission.
- [x] Android cloud backup and device-to-device transfer exclude all app data.
- [x] `FLAG_SECURE` is active on `MainActivity` and `AutofillUnlockActivity`.
- [x] Autofill shows the requesting origin and requires explicit confirmation
      before disclosing credentials.
- [x] Backup export and import fail atomically on unreadable or malformed entries.
- [x] Oversized backup and CSV inputs are rejected without loading them in memory.
- [x] English and Spanish resources are complete.
- [x] Store icon, localized feature graphics, and eight 1080 x 1920 phone
      screenshots per locale (`en-US` and `es-ES`) are prepared and validated.

## Play Console gates

- [x] Public account values identified from the live Secretaria listing:
      Chemecador; Alejandro Gata Gascón; Spain; chemecador@gmail.com.
- [ ] Confirm the existing developer identity and private contact email/phone
      remain verified in Play Console.
- [x] New-personal-account physical-device verification is N/A based on the
      developer's confirmation that this is the long-standing Play Console
      account already used for Secretaria.
- [ ] If this is an organization account, complete the D-U-N-S and organization
      identity verification requested by Play.
- [ ] Configure this app with public developer name Chemecador and mandatory
      support email chemecador@gmail.com.
- [ ] Confirm ownership/availability of `com.chemecador.passwordmemory` and check
      its Android developer-verification registration status; take action only
      if Play does not register it automatically.
- [ ] Enroll in Play App Signing and protect the upload key outside the repository.
- [ ] Add the public privacy-policy URL and verify it while signed out.
- [ ] Ensure the public and in-app privacy policies identify the app/developer,
      provide a monitored inquiry mechanism, and match the final listing.
- [ ] Complete Data safety using `data-safety.md`.
- [ ] Complete App content using `app-content.md`, including the device-credential
      access instructions, content rating, target audience, and ads declarations.
- [ ] Upload a 512 x 512 app icon, the localized 1024 x 500 feature graphic, and
      the eight matching phone screenshots for each Play Store locale.
- [x] The new-personal-account 12-tester/14-day gate is N/A based on the
      developer's confirmation that this is the long-standing Play Console
      account already used for Secretaria. Reopen this item if only the Gmail
      account, rather than the Play developer account, predates November 13, 2023.

## Recommended rollout quality gates

- [ ] Upload first to internal testing when the account permits it.
- [ ] Review the automated pre-launch report and resolve material crashes,
      security findings, or compatibility failures.
- [ ] Stage the release through internal/closed testing before production rollout.
