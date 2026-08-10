# Google Play App content draft

Verified against version `1.0` (`versionCode 1`) before release. These are
source-backed draft answers. The developer must still review every question
shown by Play Console because the available declarations depend on the account,
release countries, and current policy.

## Privacy policy

- Proposed URL:
  `https://github.com/chemecador/password-memory/blob/main/PRIVACY.md`
- The current file is published and was verified without authentication on
  August 10, 2026. Recheck it before each submission.
- The policy identifies the publisher as Chemecador (Alejandro Gata Gascón),
  Spain, and provides the monitored `chemecador@gmail.com` address plus the
  project issue tracker for privacy inquiries. These values match the existing
  public Secretaria listing on the same Play developer account.
- Keep the public policy and the in-app English and Spanish policy synchronized.

## Data safety

- Data collected: **No**
- Data shared: **No**
- Advertising or analytics SDKs: **No**
- Account creation: **No**

See `data-safety.md` for the source-code rationale. Local processing is outside
the Data safety collection scope. Autofill, clipboard, import, and export are
specific user-initiated transfers and are disclosed in the privacy policy.

## Ads

- Does the app contain ads? **No**
- Advertising ID: **Not used or requested**

## App access / sign-in details

Select that some or all functionality is restricted by authentication and add
review instructions. Suggested text:

> Password Memory has no developer account, server login, or fixed review
> credentials. The vault is intentionally protected by Android BiometricPrompt
> using a strong biometric or the review device's PIN, pattern, or password.
> Configure a secure screen lock on the review device, launch the app, and
> authenticate with that device credential. If a biometric prompt appears,
> choose its device-credential fallback. After unlocking, create a sample entry
> to test the vault. To test Autofill, select Password Memory as the Android
> Autofill service in system settings; each fill again requires confirmation
> and the review device credential.

Do not add a bypass, hardcoded password, or reusable credential to the release
build for review.

## Target audience and content rating

- Complete the IARC content-rating questionnaire accurately.
- The app contains no developer-provided violence, sexual content, gambling,
  advertising, social features, or public user-generated content.
- Select only the age groups the product is actually designed and marketed for.
  If the intended audience is adults, select adult age groups only. Selecting
  child age groups introduces Families policy obligations and must be a
  deliberate product decision.

## Other declarations

- App category: **Tools**
- Account deletion: **Not applicable** because the app cannot create an account.
- Financial features: **No**; storing a credential locally is not a financial
  product or transaction feature.
- Health app: **No**
- Government app: **No**
- News or magazine app: **No**
- VPN service: **No**
- High-risk permission declaration: **None expected**. The release manifest
  requests only `USE_BIOMETRIC`; confirm again after uploading the AAB.

## Account and review prerequisites

- Confirm that the existing Play Console identity and its private contact email
  and phone are still verified.
- Use the existing public account values: developer name **Chemecador**, legal
  identity **Alejandro Gata Gascón**, country **Spain**, and public/support email
  **chemecador@gmail.com**. Keep the public, support, and private contact roles
  current even when the same monitored mailbox is deliberately reused.
- New-personal-account device verification is not expected to apply: the
  developer states that this is the long-standing Play Console account already
  used to publish Secretaria. Reconfirm this status in Play Console.
- If this is an organization account, complete its organization verification,
  including the D-U-N-S and public organization details requested by Play.
- Confirm ownership/availability of `com.chemecador.passwordmemory`.
- Check Android developer verification and package-name registration status in
  Play Console. Google attempts to register Play packages automatically;
  registration becomes mandatory by September 30, 2026.
- The 12-tester/14-day production-access gate is recorded as **not applicable**
  on the same basis: this is the long-standing Play Console account already used
  for Secretaria. If the age statement referred only to the Gmail account rather
  than the Play developer account, reclassify this item as required.

## Official references

- User Data and privacy policy:
  <https://support.google.com/googleplay/android-developer/answer/10144311>
- Data safety:
  <https://support.google.com/googleplay/android-developer/answer/10787469>
- App content and review access:
  <https://support.google.com/googleplay/android-developer/answer/9859455>
- Play App Signing:
  <https://support.google.com/googleplay/android-developer/answer/9842756>
- Personal-account testing:
  <https://support.google.com/googleplay/android-developer/answer/14151465>
- Developer contact verification:
  <https://support.google.com/googleplay/android-developer/answer/10840893>
- Personal-account device verification:
  <https://support.google.com/googleplay/android-developer/answer/14316361>
- Store-listing support contact:
  <https://support.google.com/googleplay/android-developer/answer/113477>
- Developer and organization identity:
  <https://support.google.com/googleplay/android-developer/answer/10841920>
- Package-name registration:
  <https://support.google.com/googleplay/android-developer/answer/16984799>
