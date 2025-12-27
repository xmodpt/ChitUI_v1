# ChitUI Android Client

A modern Android client application for ChitUI Plus, allowing you to remotely monitor and control Chitu-based 3D printers from your Android device.

## Features

### 🖨️ Printer Management
- **Real-time Printer Monitoring**: View online/offline status of all configured printers
- **Printer Discovery**: Automatic and manual printer discovery
- **Multi-Printer Support**: Connect to and manage multiple printers
- **Live Status Updates**: Real-time printer status via Socket.IO

### 📊 Print Control
- **Start Prints**: Launch print jobs from your file library
- **Pause/Resume**: Control active print jobs
- **Stop Prints**: Cancel running prints with confirmation
- **Progress Monitoring**: Real-time progress, layer count, and time tracking
- **Print Status**: View detailed machine and print status information

### 📁 File Management
- **File Browser**: Browse files on local and USB storage
- **File Operations**: Delete files, start prints
- **Storage Information**: View storage capacity and usage
- **Sort & Filter**: Sort files by name, size, or date
- **File Details**: View file size, date, layer count

### 🔌 Plugin Support
All ChitUI plugins are fully supported:

#### GPIO Relay Control
- Control up to 3 GPIO relays
- Toggle relays on/off
- Custom relay names
- Real-time status updates

#### Raspberry Pi Stats
- System information (hostname, model, OS)
- Real-time CPU usage and temperature
- Memory usage monitoring
- Disk space tracking
- Auto-refreshing stats (5-second intervals)

#### IP Camera (Planned)
- Multiple camera support
- RTSP and MJPEG streaming
- Camera controls

#### Terminal (Planned)
- Send raw SDCP commands
- View command history
- Monitor system messages

### 🔐 Security
- Session-based authentication
- Remember me functionality
- Secure credential storage
- Automatic session management

## Screenshots

[Screenshots to be added]

## Requirements

- **Android Version**: Android 7.0 (API 24) or higher
- **ChitUI Server**: ChitUI Plus v1.0+ running on a Raspberry Pi or server
- **Network**: Local network access to ChitUI server

## Installation

### Option 1: Build from Source

1. **Clone the Repository**
   ```bash
   git clone https://github.com/xmodpt/ChitUI_v1.git
   cd ChitUI_v1/android-client
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the `android-client` folder
   - Wait for Gradle sync to complete

3. **Build and Run**
   - Connect your Android device or start an emulator
   - Click the "Run" button (green triangle) in Android Studio
   - Select your device
   - The app will build and install automatically

### Option 2: Install APK (Coming Soon)

Pre-built APK releases will be available in the [Releases](https://github.com/xmodpt/ChitUI_v1/releases) section.

## Setup and Configuration

### First Time Setup

1. **Launch the App**
   - Open ChitUI Client on your Android device

2. **Server Configuration**
   - Enter your ChitUI server URL (e.g., `http://192.168.1.100:5000`)
   - Make sure to use `http://` prefix and include the port number
   - The default ChitUI port is `5000`

3. **Login**
   - Enter your ChitUI username (default: `admin`)
   - Enter your password (default: `chitui`)
   - Check "Remember me" to save credentials
   - Tap "Login"

4. **Discover Printers**
   - Once logged in, tap the menu button (⋮)
   - Select "Discover Printers"
   - Wait for printers to appear in the sidebar

5. **Select a Printer**
   - Tap on a printer in the sidebar
   - View printer information, files, and plugins

## Usage Guide

### Monitoring Print Jobs

1. Select a printer from the sidebar
2. The "Printer" tab shows:
   - Printer status and information
   - Active print progress (if printing)
   - Machine information
   - Current status details

3. During printing, you can:
   - Pause the print job
   - Resume a paused print
   - Stop the print (with confirmation)

### Managing Files

1. Navigate to the "Files" tab
2. View all files on the printer's storage
3. Tap the play button (▶) to start a print
4. Tap the menu button (⋮) for more options:
   - Print the file
   - Delete the file

5. Use the toolbar to:
   - Sort files by name, size, or date
   - Toggle ascending/descending order
   - Refresh the file list

### Using Plugins

1. Navigate to the "Plugins" tab
2. View all enabled plugins

**GPIO Relay Control:**
- Toggle each relay on/off with the switch
- Changes take effect immediately
- Current state is displayed

**Raspberry Pi Stats:**
- View system information
- Monitor CPU usage and temperature
- Track memory and disk usage
- Stats refresh every 5 seconds

## Architecture

### Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Networking**:
  - Retrofit for REST API calls
  - Socket.IO for real-time updates
- **Storage**: DataStore for preferences
- **Concurrency**: Kotlin Coroutines and Flow
- **Video**: ExoPlayer (for future camera support)

### Project Structure

```
app/src/main/java/com/chitui/client/
├── data/
│   ├── model/          # Data models
│   ├── remote/         # API service and Socket.IO
│   └── repository/     # Repository layer
├── presentation/
│   ├── login/          # Login screen and ViewModel
│   ├── main/           # Main screen with navigation
│   ├── printer/        # Printer information screen
│   ├── files/          # File management screen
│   ├── plugins/        # Plugin screens
│   ├── theme/          # App theme and colors
│   └── components/     # Reusable UI components
└── util/               # Utilities and helpers
```

## API Communication

The Android client communicates with the ChitUI server using two protocols:

### REST API
- Authentication (login/logout)
- File uploads
- Settings management
- Plugin configuration

### Socket.IO (WebSocket)
- Real-time printer status updates
- File list updates
- Print control (start, pause, resume, stop)
- Toast notifications
- Plugin events

All communication matches the ChitUI Plus web interface protocol.

## Development

### Building for Development

```bash
./gradlew assembleDebug
```

### Building for Release

1. Generate a signing key (first time only):
   ```bash
   keytool -genkey -v -keystore chitui-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias chitui
   ```

2. Build the release APK:
   ```bash
   ./gradlew assembleRelease
   ```

### Running Tests

```bash
./gradlew test
```

## Troubleshooting

### Cannot Connect to Server

- **Check Server URL**: Ensure the URL is correct and includes `http://` and port
- **Network Access**: Make sure your Android device is on the same network as the ChitUI server
- **Firewall**: Check that port 5000 is not blocked
- **Server Running**: Verify ChitUI server is running (`systemctl status chitui`)

### Login Fails

- **Credentials**: Verify username and password are correct
- **Server Version**: Ensure ChitUI server is version 1.0 or higher
- **Logs**: Check ChitUI server logs for authentication errors

### Printers Not Appearing

- **Discovery**: Tap the menu (⋮) and select "Discover Printers"
- **Network**: Ensure printers are on the same network
- **Printer Status**: Verify printers are powered on and connected

### Socket.IO Connection Issues

- **Connection Status**: Check the connection indicator in the top bar
- **Re-login**: Try logging out and logging back in
- **Server Restart**: Restart the ChitUI server if issues persist

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is part of ChitUI Plus and follows the same license.

## Support

For issues, questions, or feature requests:
- Open an issue on [GitHub](https://github.com/xmodpt/ChitUI_v1/issues)
- Check the [ChitUI Plus documentation](https://github.com/xmodpt/ChitUI_v1)

## Credits

- **ChitUI Plus**: Original web-based interface
- **Android Client**: Mobile companion app
- **SDCP Protocol**: Chitu printer communication protocol
- **Libraries**: Thanks to all open-source library authors

## Roadmap

### Planned Features

- [ ] Camera streaming (RTSP/MJPEG)
- [ ] File upload from device
- [ ] Terminal/command interface
- [ ] Print history viewer
- [ ] Temperature monitoring
- [ ] Dark mode toggle
- [ ] Notifications for print completion
- [ ] Widget support
- [ ] Tablet UI optimization
- [ ] Offline mode with cached data

## Changelog

### Version 1.0.0 (Initial Release)
- ✅ Full printer monitoring and control
- ✅ File management
- ✅ GPIO relay control plugin
- ✅ RPi stats plugin
- ✅ Real-time Socket.IO updates
- ✅ Material 3 design
- ✅ Multi-printer support
