# ChitUI Android App - Complete Implementation

Full-featured Android application for controlling ChitUI and Chitu 3D printers remotely.

## ✨ Features

### Core Functionality
- 📱 **Remote Control** - Control ChitUI from anywhere
- 🖨️ **Printer Management** - View and control all connected printers
- 📂 **File Browser** - Browse files on printer storage and USB
- ▶️ **Print Control** - Start, pause, resume, and stop prints
- 📊 **Real-time Updates** - Live status via Socket.IO
- 🖼️ **Printer Images** - Display configured printer images
- ⚙️ **Settings** - Configure server URL for local/remote access

### Technical Features
- 🔐 **JWT Authentication** - Secure token-based auth
- 🌐 **Network Flexibility** - Supports local IP and internet access
- 📡 **WebSocket** - Real-time bidirectional communication
- 🎨 **Modern UI** - Material Design 3 with Jetpack Compose
- 🏗️ **MVVM Architecture** - Clean, maintainable code structure

---

## 🚀 Quick Start

### Prerequisites

1. **Android Studio** (Electric Eel or newer)
   - Download: https://developer.android.com/studio

2. **JDK 17+** (bundled with Android Studio)

3. **ChitUI Server** running with:
   - PyJWT installed: `pip3 install PyJWT Flask-CORS`
   - Network access configured

### Installation Steps

#### 1. Open Project in Android Studio

```bash
# Open Android Studio
# File → Open → Select android_app folder
```

#### 2. Sync Gradle

Android Studio will automatically prompt to sync Gradle. If not:
```
File → Sync Project with Gradle Files
```

#### 3. Configure Server URL

Edit `RetrofitClient.kt`:
```kotlin
// Line 32: Update to your Pi's IP address
private const val BASE_URL = "http://YOUR_PI_IP:8080"
```

Also update `ChitUISocketClient.kt`:
```kotlin
// Line 24: Update to match your Pi's IP
private val serverUrl = "http://YOUR_PI_IP:8080"
```

#### 4. Build and Run

```
Run → Run 'app'
```

Or press `Shift + F10`

---

## 📁 Project Structure

```
android_app/
└── app/
    ├── build.gradle.kts              # App dependencies
    └── src/main/
        ├── AndroidManifest.xml       # App configuration
        └── java/com/example/chitui/
            ├── api/
            │   ├── ChitUIApi.kt           # REST API interface
            │   ├── RetrofitClient.kt      # HTTP client
            │   └── ChitUIApiExtended.kt   # Extended models
            ├── socket/
            │   └── ChitUISocketClient.kt  # Socket.IO client
            ├── viewmodel/
            │   ├── PrinterViewModel.kt    # Printer data management
            │   └── SettingsViewModel.kt   # Settings management
            └── ui/
                ├── MainActivity.kt            # Main app screen
                ├── LoginActivity.kt           # Login screen
                ├── PrinterListScreen.kt       # Printer list
                ├── PrinterDetailScreen.kt     # Printer details
                └── SettingsScreen.kt          # Settings

```

---

## 🔧 Configuration

### Local Network (Testing)

1. Find your Pi's IP:
   ```bash
   hostname -I
   # Example: 192.168.1.100
   ```

2. Update app configuration:
   ```kotlin
   private const val BASE_URL = "http://192.168.1.100:8080"
   ```

3. Connect phone to same WiFi network

4. Run the app!

### Remote Access (Internet)

#### Option 1: Tailscale VPN (Recommended)

**On Raspberry Pi:**
```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
tailscale ip -4  # Get your Tailscale IP
```

**On Android:**
- Install Tailscale app from Play Store
- Login with same account
- Connect to network

**Update app:**
```kotlin
private const val BASE_URL = "http://100.64.X.X:8080"  // Use Tailscale IP
```

#### Option 2: ngrok (Quick Testing)

**On Raspberry Pi:**
```bash
./ngrok http 8080
# Copy the https://xxx.ngrok.io URL
```

**Update app:**
```kotlin
private const val BASE_URL = "https://xxx.ngrok.io"
```

#### Option 3: Dynamic DNS + Port Forwarding

1. Set up DDNS (DuckDNS, No-IP, etc.)
2. Configure router port forwarding: `443 → Pi:8080`
3. Set up HTTPS on ChitUI
4. Update app with your domain

---

## 📖 Usage Guide

### First Time Setup

1. **Launch App** - Opens login screen

2. **Enter Password** - Default is `admin` (change on ChitUI!)

3. **Configure Server** (if needed):
   - Tap Settings tab
   - Enter server URL
   - Tap "Save" then "Test"

### Viewing Printers

- **Printers Tab** - Shows all discovered printers
- **Pull down** to refresh
- **Tap printer** to see details

### Controlling Prints

1. **Tap a printer** to open details
2. **View files** on printer storage/USB
3. **Tap Print** on any file to start
4. **Use control buttons:**
   - ⏸️ **Pause** - Pause current print
   - ▶️ **Resume** - Resume paused print
   - ⏹️ **Stop** - Stop print (cannot resume)

### Monitoring Progress

- Real-time layer count
- Progress percentage
- Time remaining
- Current file name

---

## 🛠️ Development

### Building APK

**Debug APK:**
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

**Release APK:**
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

Sign with keystore for production.

### Testing

**Run on Emulator:**
```
Tools → Device Manager → Create Virtual Device
```

**Run on Physical Device:**
1. Enable Developer Options on phone
2. Enable USB Debugging
3. Connect via USB
4. Run app

---

## 🔐 Security

### Best Practices

1. **Change Default Password** on ChitUI immediately

2. **Use HTTPS** in production:
   ```kotlin
   private const val BASE_URL = "https://your-domain.com"
   ```

3. **Disable HTTP** in production:
   Remove `android:usesCleartextTraffic="true"` from AndroidManifest.xml

4. **Use VPN** for remote access (Tailscale recommended)

5. **Network Settings** on ChitUI:
   - Configure allowed origins
   - Enable external access carefully
   - Monitor access logs

---

## 🐛 Troubleshooting

### Cannot Connect to Server

**Check:**
- [ ] Phone and Pi on same network (for local)
- [ ] Server URL is correct in app
- [ ] ChitUI is running: `ps aux | grep main.py`
- [ ] Port 8080 is accessible: `netstat -an | grep 8080`
- [ ] Firewall allows port 8080

**Test Connection:**
```bash
# From computer on same network:
curl http://192.168.1.100:8080/api/mobile/status
```

### Login Fails

**Check:**
- [ ] Password is correct
- [ ] PyJWT is installed: `pip3 list | grep PyJWT`
- [ ] Check ChitUI logs for errors

### Socket.IO Not Connecting

**Check:**
- [ ] Socket URL matches REST API URL
- [ ] No firewall blocking WebSocket
- [ ] Check Android Logcat for errors:
  ```
  Logcat → Filter: ChitUISocket
  ```

### Images Not Loading

**Check:**
- [ ] Printer has image configured in ChitUI
- [ ] Image URL is accessible
- [ ] Network permissions granted
- [ ] Coil dependency installed

### App Crashes

**View Logs:**
```
View → Tool Windows → Logcat
```

Filter by `Exception` or `Error`

---

## 📦 Dependencies

All dependencies are in `app/build.gradle.kts`:

```kotlin
// Core
androidx.core:core-ktx:1.12.0
androidx.activity:activity-compose:1.8.2

// Jetpack Compose
androidx.compose:compose-bom:2024.01.00

// Navigation
androidx.navigation:navigation-compose:2.7.6

// Networking
com.squareup.retrofit2:retrofit:2.9.0
io.socket:socket.io-client:2.1.0

// Image Loading
io.coil-kt:coil-compose:2.5.0
```

---

## 🎨 Customization

### Change App Theme

Edit `res/values/themes.xml` (create if needed):
```xml
<resources>
    <style name="Theme.ChitUI" parent="Theme.Material3.DayNight">
        <item name="colorPrimary">@color/purple_500</item>
        <item name="colorSecondary">@color/teal_200</item>
    </style>
</resources>
```

### Change App Name

Edit `res/values/strings.xml`:
```xml
<string name="app_name">My ChitUI Remote</string>
```

### Change App Icon

Replace files in `res/mipmap-*/`:
- `ic_launcher.png`
- `ic_launcher_round.png`

---

## 🚢 Deployment

### Google Play Store

1. **Create Keystore:**
   ```bash
   keytool -genkey -v -keystore chitui-release.jks \
     -alias chitui -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Configure Signing:**
   Edit `app/build.gradle.kts`:
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file("chitui-release.jks")
               storePassword = "your_password"
               keyAlias = "chitui"
               keyPassword = "your_password"
           }
       }
   }
   ```

3. **Build Release:**
   ```
   Build → Generate Signed Bundle / APK
   ```

4. **Upload to Play Console**

### Alternative: Direct APK Distribution

1. Build release APK
2. Share APK file directly
3. Users must enable "Install Unknown Apps"

---

## 📚 API Reference

See complete API documentation in:
- `/android_docs/API_DOCUMENTATION.md`
- `/android_docs/ANDROID_QUICK_START.md`

### Key Endpoints

```kotlin
// Authentication
POST /api/mobile/login
POST /api/mobile/refresh-token

// Printers
GET /api/mobile/printers
GET /api/mobile/printer/{id}/info

// Status
GET /api/mobile/status
```

---

## 🤝 Contributing

Want to improve the app?

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

---

## 📄 License

Same as ChitUI project (MIT).

---

## 🎉 You're All Set!

The app is now ready to build and use.

**Next Steps:**
1. Build the app in Android Studio
2. Install on your Android device
3. Login and start controlling your printers!

**Need Help?**
- Check the troubleshooting section
- Review API documentation
- Open an issue on GitHub

**Happy Printing! 🖨️📱**
