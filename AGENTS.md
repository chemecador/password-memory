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

- **App lock**: opening the app requires authentication via `BiometricPrompt` (biometric with fallback to device PIN/pattern, `DEVICE_CREDENTIAL`). No separate master password.
- **Encryption key**: AES-256 in Android Keystore. Known consequence: the key does not survive reinstalls or migrate between devices — hence the backup feature.
- **Backup export/import** (offline; user moves files as desired):
  - On export, a **one-time passphrase** (not saved) is requested to encrypt the file (key derived with PBKDF2, AES-GCM). This is necessary because the Keystore key is not exportable.
  - On import, the same passphrase is requested to decrypt and restore entries.
  - Hashed entries are exported as-is (hash + salt); encrypted entries are exported in plaintext within the encrypted file and re-encrypted with the target device's Keystore on import.
- **Clipboard**: when copying a password, use `ClipDescription.EXTRA_IS_SENSITIVE` so it doesn't appear in the clipboard preview.
- `android:allowBackup="false"` in the manifest (only app-initiated backups, no system backup).
- Never log passwords, hashes, salts, or cryptographic material.

## Features

- Entry list with **search** by service name.
- **Categories** and **favorites** to organize the list.
- **Password generator** for random passwords (configurable length and character types) integrated into the creation form.
- Add, edit, and delete entries. In a hashed entry, the password is not editable (only delete the entry and create a new one).
- Detail screen: view/copy (encrypted) or guess form with unlimited attempts (hashed).

## Tech Stack

- **Kotlin + Jetpack Compose** (Material 3), single-activity.
- **MVVM architecture**: ViewModels with `StateFlow`, immutable UI state.
- **Room** for persistence.
- **Hilt** for dependency injection.
- Cryptography using platform APIs (`javax.crypto`, `java.security`, Android Keystore). No external crypto libraries.
- minSdk 26, targetSdk 36, Java 11. Namespace: `com.chemecador.passwordmemory`.
- Dependencies managed with **version catalog** (`gradle/libs.versions.toml`).

## Package Structure (indicative)

```
com.chemecador.passwordmemory
├── data
│   ├── db          # Room: entities, DAOs, database
│   ├── repository
│   └── backup      # export/import
├── domain          # models and use cases if valuable
├── security        # CryptoManager (Keystore/AES), Hasher (SHA-256+salt), generator
├── ui
│   ├── list        # list, search, categories
│   ├── detail      # view/copy or guess
│   ├── edit        # create/edit entry
│   ├── lock        # biometric lock screen
│   └── theme
└── di              # Hilt modules
```

## Languages

UI in **English by default** (`values/strings.xml`) with **Spanish translation** (`values-es/strings.xml`). No hardcoded strings in composables: always use `stringResource`.

## Conventions

- Commits and code in English; conversation with the user in Spanish.
- Unit tests for security logic (hash, encrypt/decrypt, generator) and repository.
- Do not add network dependencies or new permissions without discussing first.
