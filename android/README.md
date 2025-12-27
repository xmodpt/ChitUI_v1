# ChitUI Android App

Android companion app for ChitUI - remote control and monitoring for Chitu 3D Printers.

## Features

- **Real-time Monitoring**: Live printer status updates via WebSocket
- **Remote Control**: Start, pause, resume, and stop print jobs
- **File Management**: Upload and manage print files
- **Multi-Printer Support**: Monitor and control multiple printers
- **Notifications**: Get notified when prints complete or errors occur
- **Secure Authentication**: Session-based login with remember me option
- **Material Design 3**: Modern UI with dynamic colors

## Requirements

- Android 7.0 (API 24) or higher
- ChitUI server running on your network
- Network connectivity to ChitUI server

## Building the App

### Prerequisites

1. **Android Studio**: Download from [developer.android.com](https://developer.android.com/studio)
2. **JDK 17**: Required for Gradle 8.x

### Build Steps

1. **Clone the repository** (if you haven't already):
   ```bash
   git clone <repository-url>
   cd ChitUI_v1/android
   ```

2. **Open in Android Studio**:
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the `android` directory
   - Wait for Gradle sync to complete

3. **Build the APK**:

   **Option A: Using Android Studio**
   - Click Build > Build Bundle(s) / APK(s) > Build APK(s)
   - The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

   **Option B: Using Command Line**
   ```bash
   cd android
   ./gradlew assembleDebug
   ```
   - APK location: `app/build/outputs/apk/debug/app-debug.apk`

4. **Install on Device**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Building Release APK

1. **Generate signing key** (first time only):
   ```bash
   keytool -genkey -v -keystore chitui-release-key.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias chitui
   ```

2. **Create `keystore.properties`** in the `android/` directory:
   ```properties
   storePassword=<your-store-password>
   keyPassword=<your-key-password>
   keyAlias=chitui
   storeFile=../chitui-release-key.jks
   ```

3. **Update `app/build.gradle`** to include signing config:
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file(keystoreProperties['storeFile'])
               storePassword keystoreProperties['storePassword']
               keyAlias keystoreProperties['keyAlias']
               keyPassword keystoreProperties['keyPassword']
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               // existing config
           }
       }
   }
   ```

4. **Build release APK**:
   ```bash
   ./gradlew assembleRelease
   ```
   - APK location: `app/build/outputs/apk/release/app-release.apk`

## Configuration

### Server Connection

On first launch, enter your ChitUI server details:

- **Server URL**: `http://<server-ip>:8080` (e.g., `http://192.168.1.100:8080`)
- **Username**: Your ChitUI username
- **Password**: Your ChitUI password

**Note**: Ensure your Android device can reach the ChitUI server on your local network.

### Permissions

The app requires the following permissions:

- `INTERNET`: Connect to ChitUI server
- `ACCESS_NETWORK_STATE`: Check network connectivity
- `POST_NOTIFICATIONS`: Display print status notifications (Android 13+)
- `FOREGROUND_SERVICE`: Background monitoring service
- `WAKE_LOCK`: Keep connection alive

## Architecture

### Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Networking**: Retrofit + OkHttp
- **Real-time**: Socket.IO Android Client
- **Async**: Kotlin Coroutines + Flow
- **Navigation**: Jetpack Navigation Compose
- **Persistence**: DataStore Preferences
- **Architecture**: MVVM with Repository pattern

### Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/chitui/android/
│   │   │   ├── api/              # REST & WebSocket clients
│   │   │   │   ├── ChitUIApi.kt
│   │   │   │   ├── RetrofitClient.kt
│   │   │   │   └── SocketIOClient.kt
│   │   │   ├── data/             # Data models & repository
│   │   │   │   ├── Printer.kt
│   │   │   │   ├── ApiModels.kt
│   │   │   │   ├── PreferencesManager.kt
│   │   │   │   └── ChitUIRepository.kt
│   │   │   ├── ui/               # UI screens & ViewModels
│   │   │   │   ├── LoginScreen.kt
│   │   │   │   ├── PrintersScreen.kt
│   │   │   │   ├── PrinterDetailScreen.kt
│   │   │   │   ├── *ViewModel.kt
│   │   │   │   └── theme/
│   │   │   ├── service/          # Background services
│   │   │   │   └── PrinterMonitorService.kt
│   │   │   ├── ChitUIApplication.kt
│   │   │   └── MainActivity.kt
│   │   ├── res/                  # Resources
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## API Integration

### REST Endpoints Used

- `POST /auth/login` - User authentication
- `POST /auth/logout` - User logout
- `GET /settings` - Get server settings
- `POST /discover` - Trigger printer discovery
- `POST /printer/manual` - Add printer manually
- `DELETE /printer/{id}` - Remove printer
- `POST /upload` - Upload print file

### WebSocket Events

**Client → Server:**
- `printers` - Request printer list
- `action_print` - Start print
- `action_pause` - Pause print
- `action_resume` - Resume print
- `action_stop` - Stop print

**Server → Client:**
- `printers` - Printer list update
- `printer_status` - Real-time status update
- `printer_error` - Error notification
- `upload_progress` - File upload progress

## Troubleshooting

### Connection Issues

1. **Cannot connect to server**:
   - Verify ChitUI server is running: `curl http://<server-ip>:8080/status`
   - Check firewall allows port 8080
   - Ensure Android device is on same network
   - Try server IP instead of hostname

2. **WebSocket disconnects frequently**:
   - Check network stability
   - Disable battery optimization for ChitUI app
   - Ensure ChitUI server is running latest version

### Build Issues

1. **Gradle sync failed**:
   - Update Android Studio to latest version
   - Invalidate caches: File > Invalidate Caches / Restart
   - Check internet connection (downloads dependencies)

2. **Compilation errors**:
   - Ensure JDK 17 is installed and selected
   - Clean and rebuild: Build > Clean Project, then Build > Rebuild Project

### Runtime Issues

1. **App crashes on startup**:
   - Check logcat for stack trace: `adb logcat | grep ChitUI`
   - Clear app data: Settings > Apps > ChitUI > Storage > Clear Data

2. **Notifications not working**:
   - Grant notification permission: Settings > Apps > ChitUI > Permissions
   - Check notification settings in app
   - Disable battery optimization

## Development

### Running in Debug Mode

1. Connect Android device via USB or start emulator
2. Enable USB debugging on device
3. Click Run (▶) in Android Studio or:
   ```bash
   ./gradlew installDebug
   ```

### Logging

The app uses Android's Log system. View logs with:
```bash
adb logcat -s ChitUI:V SocketIOClient:V RetrofitClient:V
```

### Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests (requires device/emulator):
```bash
./gradlew connectedAndroidTest
```

## License

This project is part of ChitUI and follows the same license as the main project.

## Contributing

Contributions are welcome! Please submit pull requests to the main ChitUI repository.

## Support

For issues and questions:
- GitHub Issues: [Create an issue](https://github.com/xmodpt/ChitUI_v1/issues)
- ChitUI Documentation: See main project README

## Changelog

### Version 1.0.0
- Initial release
- Real-time printer monitoring
- Print control (start, pause, resume, stop)
- File upload support
- Multi-printer support
- Push notifications
- Material Design 3 UI
