# ChitUI Android Documentation

Complete documentation and code examples for building an Android app to remotely control ChitUI.

## 📁 Contents

### Documentation
- **[ANDROID_QUICK_START.md](ANDROID_QUICK_START.md)** - Complete step-by-step guide to build your Android app
- **[API_DOCUMENTATION.md](API_DOCUMENTATION.md)** - Full API reference for mobile clients

### Code Examples (`code_examples/`)

Ready-to-use Kotlin code that you can copy directly into your Android project:

1. **ChitUIApi.kt** - Retrofit API interface with all endpoints
2. **RetrofitClient.kt** - Pre-configured Retrofit client with JWT token handling
3. **ChitUISocketClient.kt** - Socket.IO client for real-time updates
4. **PrinterViewModel.kt** - ViewModel for managing printer data
5. **PrinterListScreen.kt** - Jetpack Compose UI for printer list
6. **LoginActivity.kt** - Login screen with password authentication

## 🚀 Quick Start

### For Beginners:

1. Read **[ANDROID_QUICK_START.md](ANDROID_QUICK_START.md)** from start to finish
2. Follow the step-by-step instructions
3. Copy code examples into your project
4. Test on local network first

### For Experienced Developers:

1. Add dependencies from Quick Start Guide
2. Copy all files from `code_examples/` to your project
3. Update server URLs in `RetrofitClient.kt` and `ChitUISocketClient.kt`
4. Run and test

## 🏗️ Architecture

```
Android App
├── API Layer (Retrofit)
│   ├── ChitUIApi.kt - API endpoint definitions
│   ├── RetrofitClient.kt - HTTP client configuration
│   └── TokenManager.kt - JWT token storage
│
├── Real-time Layer (Socket.IO)
│   └── ChitUISocketClient.kt - WebSocket connection
│
├── ViewModel Layer
│   └── PrinterViewModel.kt - Business logic & state
│
└── UI Layer (Jetpack Compose)
    ├── LoginActivity.kt - Authentication screen
    ├── MainActivity.kt - Main app screen
    └── PrinterListScreen.kt - Printer list UI
```

## 🔐 Authentication Flow

```
1. User enters password in LoginActivity
   ↓
2. App sends POST /api/mobile/login
   ↓
3. Server validates and returns JWT token
   ↓
4. App stores token in SharedPreferences
   ↓
5. All API requests include: Authorization: Bearer <token>
   ↓
6. Token valid for 30 days (configurable)
```

## 📡 Communication Methods

### REST API (via Retrofit)
- Used for: Login, getting printer lists, system status
- Protocol: HTTP/HTTPS
- Format: JSON
- Authentication: JWT token in header

### Socket.IO (Real-time)
- Used for: Live printer updates, print control
- Protocol: WebSocket
- Format: JSON events
- Benefits: Instant updates, bidirectional communication

## 🛠️ Development Tools

- **Android Studio** - IDE for Android development
- **Postman/curl** - API testing
- **Logcat** - Android debugging
- **Chrome DevTools** - Network inspection

## 📱 Requirements

### Android App:
- Minimum SDK: API 24 (Android 7.0)
- Target SDK: API 34 (Android 14)
- Language: Kotlin
- UI Framework: Jetpack Compose

### ChitUI Server:
- ChitUI running on Raspberry Pi
- PyJWT installed: `pip3 install PyJWT`
- Network connectivity

## 🔗 API Endpoints

### Authentication
- `POST /api/mobile/login` - Login and get token
- `POST /api/mobile/refresh-token` - Refresh token

### Printers
- `GET /api/mobile/printers` - List all printers
- `GET /api/mobile/printer/{id}/info` - Get printer details

### System
- `GET /api/mobile/status` - System status

See [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for complete reference.

## 🌐 Network Setup

### Local Network (Testing)
```
Phone/Emulator  ←→  WiFi Router  ←→  Raspberry Pi
                    (Same Network)
```

### Remote Access (Production)

**Option 1: Tailscale VPN (Recommended)**
- Secure, encrypted connection
- Works from anywhere
- No port forwarding needed
- Free for personal use

**Option 2: ngrok**
- Quick temporary tunnel
- Good for testing
- Free tier available

**Option 3: Port Forwarding**
- Direct internet access
- Requires HTTPS setup
- Security considerations

## 🧪 Testing Checklist

Before deploying your app:

- [ ] Test login with correct password
- [ ] Test login with wrong password
- [ ] Verify token is stored
- [ ] Test printer list loads
- [ ] Test real-time updates via Socket.IO
- [ ] Test network error handling
- [ ] Test token expiration
- [ ] Test on physical device
- [ ] Test on different network (mobile data)
- [ ] Verify HTTPS in production

## 📚 Learning Resources

### Android Development:
- [Android Developers](https://developer.android.com) - Official docs
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI framework
- [Kotlin](https://kotlinlang.org) - Programming language

### Networking:
- [Retrofit](https://square.github.io/retrofit/) - HTTP client
- [Socket.IO](https://socket.io) - Real-time communication
- [JWT](https://jwt.io) - Token authentication

### ChitUI:
- [ChitUI GitHub](#) - Source code
- [API Documentation](API_DOCUMENTATION.md) - API reference

## 🐛 Troubleshooting

### Common Issues:

**"Connection refused"**
- Check Pi and phone on same WiFi
- Verify ChitUI is running
- Check firewall settings

**"401 Unauthorized"**
- Verify password is correct
- Check token is being sent in headers
- Token may have expired

**"Socket.IO not connecting"**
- Verify Socket.IO URL matches REST API
- Check CORS settings
- Enable WebSocket in OkHttp

See [ANDROID_QUICK_START.md](ANDROID_QUICK_START.md#troubleshooting) for detailed solutions.

## 🤝 Contributing

Found a bug or want to improve the documentation?

1. Open an issue on GitHub
2. Submit a pull request
3. Share your improvements

## 📄 License

Same license as ChitUI project (MIT).

## 🎉 Get Started Now!

Ready to build your app?

👉 **Start with [ANDROID_QUICK_START.md](ANDROID_QUICK_START.md)**

---

**Questions?** Open an issue on GitHub or check the documentation.

**Happy Coding! 📱🎨**
