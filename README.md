Based on the repository structure, source code, and `metadata.json`, here is a comprehensive `README.md` for the **AegisLock** (AppLock) project.

# AegisLock 🔐

[![Build APK](https://github.com/Ricksark/AppLock/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Ricksark/AppLock/actions/workflows/build-apk.yml)
[![Platform](https://img.shields.io/badge/platform-Android-brightgreen)](https://developer.android.com)
[![Min SDK](https://img.shields.io/badge/minSdk-24-blue)](https://developer.android.com/studio/releases/platforms)
[![Target SDK](https://img.shields.io/badge/targetSdk-36-blue)](https://developer.android.com/studio/releases/platforms)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9+-purple)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Compose-✓-green)](https://developer.android.com/jetpack/compose)

# follow now
[![Instagram](https://img.shields.io/badge/instagram-1.5K+-DD2A7B?style=flat&logo=instagram&logoColor=white)](https://www.instagram.com/_priyangshu_2)
[![Facebook](https://img.shields.io/badge/Facebook-20-1877F2?style=flat&logo=facebook&logoColor=white)](https://www.facebook.com/mrpriyangshusarkar)

**AegisLock** is a secure Android app locker that protects your sensitive applications and files with military‑grade encryption. It combines biometric authentication, custom passcode fallback, an AES‑256 encrypted vault for photos & documents, real‑time app monitoring, and remote‑wipe capabilities.

---

## ✨ Features

- **🔒 App Lock** – Lock any installed app behind biometric (fingerprint/face) or a custom passcode.
- **🛡️ Biometric & Passcode Authentication** – Uses Android BiometricPrompt with a secure fallback keypad.
- **📁 Encrypted Vault** – Store private photos and documents in an AES‑256 encrypted container.
- **👁️ Real‑Time Protection** – Monitors app launches and overlay attacks on Android 10+ using Accessibility Service & foreground service.
- **📡 Remote Wipe** – Remotely erase vault contents and lock settings via Firebase (optional).
- **📊 Security Logs** – Room‑backed audit log of unlock attempts and security events.
- **🎨 Modern UI** – Fully built with Jetpack Compose, Material 3, and smooth animations.
- **🔔 Notification Channels** – Separate channels for monitor service and high‑priority security alerts.
- **⚙️ CI/CD** – GitHub Actions workflow to build a signed APK automatically.

---

## 🧱 Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM + Repository (partially) |
| Local DB | Room (SQLite) |
| Encryption | Android Keystore + AES‑256 |
| Auth | AndroidX Biometric |
| Background | Accessibility Service, Foreground Service |
| DI / Config | Secrets Gradle Plugin (`.env`) |
| CI | GitHub Actions |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

---

## 📂 Project Structure

```
app/
├── src/main/java/com/example/
│   ├── crypto/          # BiometricHelper, CryptoManager (AES‑256)
│   ├── data/            # Room entities, DAO, SecurityPreferences
│   ├── receiver/        # Broadcast receivers (boot, package events)
│   ├── service/         # AccessibilityService, MonitorService, OverlayLauncher
│   ├── ui/              # Compose screens, components, theme
│   ├── vault/           # Vault logic (photo/document encryption)
│   ├── AegisApplication.kt
│   └── MainActivity.kt
├── src/androidTest/     # Instrumented tests
├── src/test/            # Unit tests
└── build.gradle.kts     # App‑level build config
```

---

## 🚀 Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 11+
- Android SDK 36
- A Firebase project (optional, for remote wipe)
- Google Services JSON (`google-services.json`) – optional; build will warn if missing.

### Clone & Build

```bash
git clone https://github.com/Ricksark/AppLock.git
cd AppLock
```

1. **Configure secrets**  
   Copy `.env.example` to `.env` and fill in any required keys (e.g., Firebase tokens).  
   The Secrets Gradle Plugin reads this file automatically.

2. **Firebase (optional)**  
   Place `google-services.json` in the `app/` directory if you need remote‑wipe / cloud features.

3. **Build the APK**

   ```bash
   ./gradlew assembleDebug
   ```

   The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

4. **Release build**  
   Set the following environment variables before running `./gradlew assembleRelease`:
   - `KEYSTORE_PATH`
   - `STORE_PASSWORD`
   - `KEY_PASSWORD`

---

## 🔐 Security Highlights

- **AES‑256 Encryption** – All vault files are encrypted with a key derived from the user’s passcode/biometric and stored in the Android Keystore.
- **Secure Biometric Prompt** – Uses `BiometricPrompt` with `CryptoObject` to ensure only authenticated users can decrypt data.
- **Accessibility Service** – Detects when a locked app comes to the foreground and immediately shows the lock overlay.
- **Foreground Service** – Keeps the monitor alive and shows a persistent notification (low importance).
- **Remote Wipe** – Sends a silent push (via Firebase) that triggers a local wipe of the vault and preferences.

---

## 🧪 Testing

- **Unit tests** – `./gradlew test`
- **Instrumented tests** – `./gradlew connectedAndroidTest`

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

Please ensure your code follows the existing style and passes all tests.

---

## 📄 License

This project is not currently licensed. Please contact the repository owner for usage terms.

---

## 🙏 Acknowledgements

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [AndroidX Biometric](https://developer.android.com/jetpack/androidx/releases/biometric)
- [Room](https://developer.android.com/training/data-storage/room)
- [Firebase](https://firebase.google.com)

---

**AegisLock** – *Your apps, your files, your rules.*
```
