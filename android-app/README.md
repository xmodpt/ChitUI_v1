# ChitUI Android WebView App

A simple Android application that displays the ChitUI web interface in a fullscreen WebView without frames or borders.

## Features

- **Server Address Configuration**: On first launch, the app prompts you to enter your ChitUI server address
- **Persistent Settings**: The server address is saved and remembered for future launches
- **Fullscreen WebView**: Displays the ChitUI interface without any frames, borders, or unnecessary UI elements
- **JavaScript Support**: Full JavaScript and DOM storage support for ChitUI functionality
- **Back Button Navigation**: Use the device back button to navigate within the WebView

## Requirements

- Android SDK 21 (Android 5.0 Lollipop) or higher
- Android Studio (for building the app)
- Java 8 or higher

## Building the App

### Using Android Studio

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the `android-app` directory
4. Wait for Gradle to sync
5. Click "Run" or press Shift+F10 to build and install the app on a connected device or emulator

### Using Command Line

```bash
cd android-app
./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`

## Installation

### From Android Studio
- Connect your Android device via USB (with USB debugging enabled)
- Click the "Run" button in Android Studio

### Manual Installation
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Launch the app
2. On first launch, enter your ChitUI server address (e.g., `192.168.1.100:8000`)
3. Click "Connect"
4. The ChitUI interface will load in fullscreen mode

### Changing Server Address

To change the server address:
1. Clear the app data from Android Settings
2. Relaunch the app
3. Enter the new server address

Or modify the code to add a settings screen for changing the server address.

## App Structure

- **MainActivity.java**: Handles initial server address input and storage
- **WebViewActivity.java**: Displays the ChitUI webpage in a fullscreen WebView
- **SharedPreferences**: Used to persist the server address

## Permissions

The app requires the following permissions:
- `INTERNET`: To load web content
- `ACCESS_NETWORK_STATE`: To check network connectivity

## Security Notes

- The app allows cleartext HTTP traffic for local server access
- Mixed content (HTTP and HTTPS) is allowed
- Consider using HTTPS for production deployments

## Customization

### App Name and Icon
- App name: Modify `app/src/main/res/values/strings.xml`
- App icon: Replace files in `app/src/main/res/mipmap-*` directories

### Theme Colors
- Modify colors in `app/src/main/res/values/colors.xml`

## License

This app is part of the ChitUI project.
