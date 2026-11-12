# ScanPro Android

Starter project for ScanPro, a native Android document scanner built with Kotlin and Jetpack Compose.

## Included in this first implementation
- Modern home, documents, and settings screens
- Google ML Kit Document Scanner integration in FULL mode
- Gallery import and up to 50 pages per scan
- PDF and JPEG scan outputs requested from the scanner SDK
- Copy generated PDF into app-private local storage
- Open/share saved PDF using Android FileProvider
- Initial on-device Latin OCR dependency and integration hook (PDF page rasterization remains a next milestone)

## Requirements
- Android Studio (recent stable version)
- JDK 17
- Android SDK 36
- Device with Google Play services; first scanner launch may download scanner components

## Run
Open this folder in Android Studio, allow Gradle sync, then run the `app` configuration on an Android device/emulator.

## Important implementation notes
- Minimum SDK is 23. The ML Kit Document Scanner requires API 21+ and at least 1.7 GB device RAM.
- Document Scanner runs its capture/editing flow through Google Play services and does not require the app to request camera permission.
- Documents are stored in the app's private files directory (`files/scans`). They are not yet exported to a user-selected public folder.
- OCR currently has the text-recognition dependency and initial hook; extracting text from PDF pages requires a PDF-to-bitmap pipeline and is planned next.
- Production hardening still needed: persistent metadata/database, robust error states, PDF page count restoration, delete/rename/search, localization, accessibility, automated tests, and signing/release configuration.
