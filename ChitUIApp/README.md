# ChitUI Android App

A modern Android application that displays the ChitUI web interface in a fullscreen WebView with no frames or borders.

## Features

- **First-Run Setup**: On first launch, prompts for ChitUI server address
- **Persistent Configuration**: Server address is saved using SharedPreferences
- **Fullscreen WebView**: Displays ChitUI without frames, action bars, or borders
- **Modern Android**: Built with Kotlin, Material Design 3, and ViewBinding
- **Full Web Support**: JavaScript, DOM storage, and modern web features enabled
- **Navigation**: Back button support for WebView history

## Requirements

- **Android**: Minimum SDK 24 (Android 7.0 Nougat)
- **Target SDK**: 34 (Android 14)
- **Build Tools**: Android Studio Hedgehog (2023.1.1) or later
- **Java**: JDK 17 or higher

## Getting Started

### Open in Android Studio

1. Launch Android Studio
2. Select **File → Open**
3. Navigate to the `ChitUIApp` directory and click **OK**
4. Wait for Gradle sync to complete
5. Connect an Android device or start an emulator
6. Click **Run** (Shift+F10) or the green play button

### Build from Command Line

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (unsigned)
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Installation

### Via Android Studio
Connect your Android device via USB with USB debugging enabled, then click **Run**.

### Via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. **First Launch**: Enter your ChitUI server address
   - Example: `192.168.1.100:8000`
   - Example: `http://10.0.0.5:5000`
   - The app automatically adds `http://` if no protocol is specified

2. **Subsequent Launches**: The app loads your saved server directly

3. **Reset Server**: To change servers, clear app data:
   - Settings → Apps → ChitUI → Storage → Clear Data

## Project Structure

```
ChitUIApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/chitui/app/
│   │   │   ├── MainActivity.kt          # Server setup screen
│   │   │   └── WebViewActivity.kt       # Fullscreen WebView
│   │   ├── res/
│   │   │   ├── layout/                  # UI layouts
│   │   │   ├── values/                  # Strings, colors, themes
│   │   │   ├── xml/                     # Config files
│   │   │   └── mipmap-*/                # App icons
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts                 # App-level build config
├── build.gradle.kts                     # Project-level build config
├── settings.gradle.kts
└── gradle.properties
```

## Key Technologies

- **Language**: Kotlin
- **UI**: Material Design 3, ViewBinding
- **WebView**: AndroidX WebKit
- **Storage**: SharedPreferences
- **Min SDK**: 24 (Android 7.0)
- **Build System**: Gradle 8.2 with Kotlin DSL

## Configuration

### Network Security
The app allows cleartext HTTP traffic for local server access via `network_security_config.xml`.

### Permissions
- `INTERNET`: Load web content
- `ACCESS_NETWORK_STATE`: Check connectivity

## Customization

### App Name
Edit `app/src/main/res/values/strings.xml`:
```xml
<string name="app_name">Your App Name</string>
```

### Theme Colors
Edit `app/src/main/res/values/themes.xml` or `colors.xml`

### App Icon
Replace files in `app/src/main/res/drawable/ic_launcher_foreground.xml` or add custom mipmap icons

## Troubleshooting

### Gradle Sync Issues
- Ensure you have JDK 17 installed
- Check internet connection for dependency downloads
- Try **File → Invalidate Caches → Invalidate and Restart**

### WebView Not Loading
- Verify server address is correct and accessible
- Check that ChitUI server is running
- Ensure device/emulator can reach the server IP
- Try accessing the URL in Chrome on the same device

### Build Errors
```bash
# Clean build
./gradlew clean

# Rebuild
./gradlew build
```

## Security Notes

- Cleartext HTTP is enabled for local development
- For production, use HTTPS
- The app trusts system certificates only
- No custom certificate pinning

## License

Part of the ChitUI project.
