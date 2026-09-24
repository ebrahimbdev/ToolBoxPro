# ToolBox Pro

An Android toolbox application with QR Code Generator and Local File Sharing features.

## Features

### QR Code Generator
- Generate QR codes for URLs, Text, WiFi, and Contacts
- Custom logo support (add your own logo to QR codes)
- Color customization with gradients
- Pixel and ball shape options
- Save and share QR codes

### Local File Sharing
- Share files over local WiFi network
- Browser-based file manager
- QR code for sharing connection link
- No internet required - works offline

### Also included
- Speed Test
- Device Info
- English / Persian / Arabic / Turkish UI

## Tech Stack

- **Language:** Kotlin 2.0+
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM + Clean Architecture
- **QR Code:** ZXing
- **HTTP Server:** Ktor CIO
- **DI:** Hilt
- **Database:** Room

## Project Structure

```
ToolBoxPro/
├── app/                 # Main application
├── admin/               # Admin management app
├── gradle/              # Gradle configuration
├── version.properties   # Single source of truth for versioning
└── .github/workflows/   # CI + auto release/tag
```

## Versioning & Releases

- Version lives in `version.properties` (`VERSION_NAME`, `VERSION_CODE`).
- **CI:** every push/PR to `main` builds a release APK artifact.
- **Release:** run the **Release** workflow from the GitHub *Actions* tab.
  - Choose `patch` / `minor` / `major`
  - It bumps `version.properties`, commits, creates tag `vX.Y.Z`,
    builds the APK, and publishes a GitHub Release with the APK attached.

```bash
# local build
./gradlew :app:assembleRelease
```

## Setup

1. Open project in Android Studio
2. Sync Gradle
3. Run on device or emulator

## Requirements

- Android Studio Ladybug (2024.2.1)+
- JDK 21
- Android SDK 35
- Min SDK: 26 (Android 8.0)
