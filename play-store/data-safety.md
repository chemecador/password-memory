# Google Play Data safety declaration

Verified against version `1.0` (`versionCode 1`) before release.

## Data collection and sharing

- Does the app collect any required user-data type? **No.**
- Does the app share any required user-data type with third parties? **No.**
- Does the app contain advertising or analytics SDKs? **No.**
- Does the app transmit data to developer-controlled servers? **No.**

Passwords, usernames, service names, hints, categories, and preferences are
processed in app-private storage on the device. Android Autofill, clipboard
operations, CSV import, and encrypted backup export happen only after a user
action and are described in the privacy policy. They do not send data to the
developer. The destination app, clipboard or keyboard service, and user-selected
document provider may process or synchronize the data under their own policies.

## Security and deletion

- Data is protected locally with Android platform security and app-level
  encryption as described in `PRIVACY.md`.
- The app does not create user accounts.
- Users can delete individual entries in the app.
- Clearing app data or uninstalling deletes the private local vault.
- User-created backup and CSV files live outside app-private storage and must be
  deleted by the user; after CSV import, the app offers to delete the source.

The "data encrypted in transit" question is not applicable because the app has no
network data transmission.

## Policy consistency checks

Before every Play submission, confirm:

- the merged manifest still has no `android.permission.INTERNET`;
- no dependency introduces analytics, advertising, or telemetry;
- Android cloud backup and device transfer remain excluded;
- the in-app policy and public `PRIVACY.md` describe the current data flows;
- the Play Console developer contact is current.
