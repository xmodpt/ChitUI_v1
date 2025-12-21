# ChitUI Remote - Android App

Native Android application for remotely controlling and monitoring 3D resin printers through ChitUI server.

## Features

- **Real-time Printer Monitoring**: View printer status, print progress, and temperature
- **Remote Control**: Start, pause, resume, and stop prints
- **File Management**: Browse and manage print files
- **Camera Streaming**: View live camera feeds from your printer
- **System Information**: Monitor Raspberry Pi system stats (CPU, memory, disk)
- **Plugin Support**: Interact with ChitUI plugins

## Requirements

- Android 7.0 (API 24) or higher
- ChitUI server running on your network
- Network access to the ChitUI server

## Installation

### Option 1: Build from Source (Android Studio)

1. Open Android Studio
2. Select **File > Open** and navigate to `ChitUI_v1/android-app`
3. Wait for Gradle to sync
4. Connect your Android device or start an emulator
5. Click **Run** (or press Shift+F10)

### Option 2: Install APK (Coming Soon)

Download the latest APK from the releases page and install on your device.

## Usage

1. Launch the app
2. Enter your ChitUI server URL (e.g., `http://192.168.1.100:8080`)
3. Enter your ChitUI password
4. Click **Connect**

## Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Networking**:
  - Retrofit for REST API calls
  - Socket.IO for real-time updates
- **State Management**: Kotlin Coroutines + Flows
- **Persistence**: DataStore for preferences

## Project Structure

```
app/src/main/java/com/chitui/remote/
├── data/
│   ├── api/            # API client and interfaces
│   ├── models/         # Data models
│   ├── preferences/    # DataStore preferences
│   └── websocket/      # Socket.IO manager
├── ui/
│   ├── screens/        # Composable screens
│   └── theme/          # Material Design theme
└── viewmodel/          # ViewModels for state management
```

## Dependencies

- AndroidX Core, AppCompat, Material Components
- Jetpack Compose (UI)
- Retrofit (HTTP client)
- Socket.IO Client (WebSocket)
- Gson (JSON parsing)
- Coil (Image loading)
- ExoPlayer (Video streaming)
- DataStore (Preferences)

## Development

### Building

```bash
./gradlew assembleDebug
```

### Running Tests

```bash
./gradlew test
```

### Creating Release Build

```bash
./gradlew assembleRelease
```

## Troubleshooting

### Cannot connect to server

- Ensure your Android device is on the same network as the ChitUI server
- Check that the server URL is correct (include `http://` and port number)
- Verify the ChitUI server's network access is enabled in Settings > Network
- Make sure the password is correct

### Connection drops frequently

- Check your Wi-Fi connection stability
- Ensure the ChitUI server is running continuously
- Check if your router has aggressive power-saving features

## Contributing

Contributions are welcome! Please feel free to submit pull requests.

## License

This project is part of ChitUI and follows the same license.

## Support

For issues and feature requests, please visit:
https://github.com/xmodpt/ChitUI_v1/issues
