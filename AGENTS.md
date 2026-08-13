# PasswordMemory

A 100% local password manager for Android. The application **never connects to the internet**: no syncing, no analytics, no network permission (`INTERNET` must not appear in the manifest).

## Concept

Users save password entries locally. Each entry has:

- **Service name** (required): e.g., "Gmail", "Netflix".
- **Email or username** (optional).
- **Password** (required).
- **Hint** (optional): free text to aid memory.
- **Category** (optional) and **favorite** flag.

When creating an entry, the user chooses between two protection modes **per entry** (irreversible once saved):

### Encrypted mode (recoverable)
- The password is encrypted with **AES-256-GCM** using a key generated and stored in the **Android Keystore** (the key never leaves the secure hardware).
- The user can decrypt, view, and copy it to the clipboard at any time.

### Hashed mode (irrecoverable)
- The password is stored as **SHA-256 with a per-entry random salt** (16+ bytes via `SecureRandom`). It is never stored plaintext or encrypted: **it cannot be recovered**.
- In the form, when this mode is enabled, a **helper text** warns that the password cannot be seen afterward.
- If the user forgets it, they can **attempt unlimited guesses, without rate-limiting** (deliberate choice: it's their own device and their own password).
- When a guess matches the hash, the app displays the password that was just typed (which they already know) and enables the copy button — the real value is never stored.

## Security

- **App lock**: opening the app requires authentication via `BiometricPrompt` (biometric with fallback to device PIN/pattern, `DEVICE_CREDENTIAL`). No separate master password. The lock comes back after a configurable **auto-lock timeout** (immediate by default, up to 5 minutes): the app is locked on the way out when the timeout is immediate, and otherwise on the way back in once the time away has run out. The system file picker keeps its exemption, or the screen waiting for a result would be torn down.
- **Screen capture**: `FLAG_SECURE` on `MainActivity` and `AutofillUnlockActivity`, so screenshots, screen recording and the recents thumbnail are all blocked. Not configurable.
- **Encryption key**: AES-256 in Android Keystore, **bound to user authentication** with a 30-second window (`KeystoreCryptoManager.AUTH_WINDOW_SECONDS`). The biometric prompt is therefore a cryptographic gate, not just a screen in front of readable data. Consequences that shape the rest of the app:
  - The keyguard only trusts a **strong** authentication, so `ALLOWED_AUTHENTICATORS` is `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`. A device whose only biometric is weak falls back to the PIN.
  - Any operation can fail with `AuthenticationRequiredException` once the window closes. That is never reported as a failure: `revealPassword` returns `RevealResult.AuthenticationRequired`, the backup/CSV flows park the work in `pendingRetry`, and the screen asks `LocalCryptoGate` to prompt before retrying. `exportEntries` deliberately throws rather than writing a backup with entries silently missing.
  - The autofill **save** path cannot prompt from a service, so the credential is parked in `PendingCapture` (in memory, never an `Intent` extra) and `AutofillUnlockActivity` authenticates and commits it. Below API 28 the save callback cannot launch an activity, so saving only works while the window happens to be open.
  - A timeout-based key is not invalidated by enrolling a new fingerprint, which is the behaviour we want: losing every entry over an added finger would be far worse.
  - **Key rotation**: entries written by earlier versions used an unauthenticated key. `PasswordRepository.rotateEncryptionKey()` runs after each unlock, re-encrypts them and deletes the old alias once nothing needs it; `KeystoreCryptoManager.decrypt` falls back to the old key until then. Interrupting it is safe.
  - Unchanged: the key does not survive reinstalls or migrate between devices — hence the backup feature.
- **Backup export/import** (offline; user moves files as desired):
  - On export, a **one-time passphrase** (not saved) is requested to encrypt the file (key derived with PBKDF2, AES-GCM). This is necessary because the Keystore key is not exportable.
  - On import, the same passphrase is requested to decrypt and restore entries.
  - Hashed entries are exported as-is (hash + salt); encrypted entries are exported in plaintext within the encrypted file and re-encrypted with the target device's Keystore on import.
- **CSV import** (Google Password Manager / Chrome): the user exports a CSV from Google and picks it with SAF. Columns are matched by header name (`name,url,username,password,note`), everything is imported in encrypted mode, duplicates (same service + username) are shown before writing anything, and the app offers to delete the plaintext CSV afterwards. The file is never copied into app storage.
- **Autofill** (`AutofillService`): once the user picks Password Memory as the system autofill service, Android asks this app to fill logins in other apps and browsers, and the "save password" prompt becomes this app's instead of Google's.
  - Suggestions never carry the password: every dataset is *authenticated*, so the dropdown only shows service name and username, and `AutofillUnlockActivity` decrypts the entry after a `BiometricPrompt` and returns the finished dataset.
  - Only encrypted entries are offered; a hashed password cannot be recovered, so there is nothing to fill with.
  - Entries do not store the site they belong to, so matching is by name: the meaningful labels of the domain or package (`github` out of `github.com`, `twitter` out of `com.twitter.android`) against the service name and any URL left in the hint. Generous on purpose — the user still picks the row, and a last "choose another entry" row opens the full list.
  - Saved credentials become encrypted entries named after the site (or the app), with the origin (`https://…` / `android://…`) kept in the hint so the same form is recognised next time. An existing encrypted entry with the same service and username has its password replaced instead of being duplicated.
- **Clipboard**: every copy goes through `SecureClipboard`, which flags the clip with `ClipDescription.EXTRA_IS_SENSITIVE` so it doesn't appear in the clipboard preview, and then wipes it after a configurable delay (30 s by default, or never). The timer lives in the application process, so a copy outlives an app Android decides to kill — hence the sensitive flag as the backstop.
  - Before wiping, the clip is checked against the **`ClipDescription` timestamp** recorded when it was written, never against its label. Anything that re-posts the clip rewrites the label — a clipboard manager, a keyboard, the emulator's bridge to the host — so a label check silently skips the wipe exactly when another tool is holding the password. This was a real bug, found on a device: the emulator relabels the clip `host clipboard` and the password survived indefinitely.
  - Reading the description is not reading the clipboard, so no "pasted from your clipboard" toast is raised.
- **Settings** live in plain `SharedPreferences` (`SettingsRepository`), keyed by duration in seconds so renaming an enum constant cannot orphan a user's choice. Nothing secret is stored there, and both the app lock and the autofill service need to read them synchronously.
- `android:allowBackup="false"` in the manifest (only app-initiated backups, no system backup).
- Never log passwords, hashes, salts, or cryptographic material.

## Features

- Entry list with **search** by service name.
- **Service names** (`ServiceName`) answer two different questions, deliberately with two rules:
  - `clean` — *written differently, or a different name?* Strips the URL prefixes (`https://`, `http://`, `www.`) a paste or a CSV leaves behind, and runs on every path into the database (form, autofill capture, backup restore, CSV import), so the name is stored the way it is sorted and shown. Without it a pasted `https://netflix.com` sorted under H, wore an H in its avatar and sat apart from the `netflix.com` typed by hand. Entries saved before the rule were cleaned by `MIGRATION_1_2`, which strips the same prefixes in SQL. A name that is nothing but a prefix is left alone: the field is required and must not end up blank.
  - `key` — *the same service?* The cleaned name, lowercased, with any trailing domain suffix dropped, so `Gmail` and `gmail.com` are one service. Labels of two characters or fewer count as suffixes without being listed, which is what keeps `amazon.co.uk` from reading as a service called `amazon.co`; longer ones (`com`, `info`, `store`…) are named in `DOMAIN_SUFFIXES`. Only trailing labels go and never all of them, so `mail.google.com` stays its own service and one called `com` keeps its name. The suffix is part of the name: this fold is for comparison only and never reaches what is stored. Used by `ServiceGroup` and by `EntryKey.of`, the duplicate check an import runs — a false match there is counted on screen and let through on a checkbox before anything is written.
  - `exactKey` — the cleaned name lowercased and nothing looser, behind `EntryKey.exact`. The autofill save uses it, and only it, to decide whether to replace the password of an entry it did not create: that write is silent and irreversible, so it is the one decision that gets no help from a resemblance. A second entry is the better way to be wrong there.
- **Grouping by service**: entries are collapsed into one row per `ServiceName.key`, so two accounts on the same site are a single row. Where the accounts spell the service differently, the shortest spelling labels the row, so it does not depend on which of them holds the username that sorts first. A row with one account shows its username and its favorite star as before; a row with several shows how many accounts there are and opens the list of them, from which the user reaches the detail of one. Filters run over entries and the survivors are grouped, so a filtered row only counts the accounts that matched. The autofill entry picker stays flat: it chooses a credential, not a service, and an extra tap in the middle of filling a form is worse than a longer list.
- **Categories** and **favorites** to organize the list.
- **Password generator** for random passwords (configurable length and character types) integrated into the creation form.
- Add, edit, and delete entries. In a hashed entry, the password is not editable (only delete the entry and create a new one).
- Detail screen: view/copy (encrypted) or guess form with unlimited attempts (hashed).
- **Autofill**: fills logins in other apps and browsers and takes over the "save password" prompt. A screen in the overflow menu shows whether the app is the system autofill service and sends the user to the system picker.
- **Settings** screen in the overflow menu: auto-lock timeout and clipboard clear delay, plus notes explaining what is fixed and cannot be turned off.

## Tech Stack

- **Kotlin + Jetpack Compose** (Material 3), single-activity.
- **MVVM architecture**: ViewModels with `StateFlow`, immutable UI state.
- **Room** for persistence.
- **Hilt** for dependency injection.
- Cryptography using platform APIs (`javax.crypto`, `java.security`, Android Keystore). No external crypto libraries.
- minSdk 26, targetSdk 36, Java 11. Namespace: `com.chemecador.passwordmemory`.
- The debug variant installs as `com.chemecador.passwordmemory.debug` so it never has to replace the released app on a device that holds a real vault. Its data, Keystore keys and autofill registration are separate, and `src/debug/res` relabels it "Password Memory (debug)". Anything addressing the app by package name from outside (adb, scripts) has to use the variant's id, not the namespace.
- Dependencies managed with **version catalog** (`gradle/libs.versions.toml`).

## Package Structure (indicative)

```
com.chemecador.passwordmemory
├── autofill        # AutofillService, form parsing, entry matching, unlock activity
├── data
│   ├── db          # Room: entities, DAOs, database
│   ├── repository
│   ├── backup      # export/import
│   ├── importer    # CSV from Google Password Manager / Chrome
│   └── settings    # user preferences (SharedPreferences)
├── domain          # models and use cases if valuable
├── security        # CryptoManager (Keystore/AES), Hasher (SHA-256+salt), generator
├── ui
│   ├── list        # list grouped by service, search, categories, accounts of a service
│   ├── detail      # view/copy or guess
│   ├── edit        # create/edit entry
│   ├── lock        # biometric lock screen
│   ├── autofill    # autofill settings and the entry picker shown while filling
│   ├── settings    # auto-lock and clipboard preferences
│   └── theme
└── di              # Hilt modules
```

## Languages

UI in **English by default** (`values/strings.xml`) with **Spanish translation** (`values-es/strings.xml`). No hardcoded strings in composables: always use `stringResource`.

## Conventions

- Commits and code in English; conversation with the user in Spanish.
- Unit tests for security logic (hash, encrypt/decrypt, generator) and repository.
- **Room**: `exportSchema = true`, schemas committed under `app/schemas`. Every schema change bumps `DATABASE_VERSION` and adds its `Migration` to `PasswordDatabase.MIGRATIONS`; there is no destructive fallback, and `PasswordDatabaseMigrationTest` (instrumented) validates the chain.
- Do not add network dependencies or new permissions without discussing first.
