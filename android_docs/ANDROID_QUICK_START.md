# ChitUI Android App - Quick Start Guide

Complete guide to building an Android app that remotely controls your Raspberry Pi running ChitUI.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Project Setup](#project-setup)
4. [Implementation Steps](#implementation-steps)
5. [Testing](#testing)
6. [Remote Access Setup](#remote-access-setup)
7. [Troubleshooting](#troubleshooting)
8. [API Reference](#api-reference)

---

## Architecture Overview

```
┌─────────────────────┐         Internet/LAN        ┌──────────────────┐
│  Android App        │ ◄─────────────────────────► │  Raspberry Pi    │
│                     │    HTTPS + WebSocket        │  + ChitUI        │
│  - Retrofit (REST)  │                             │  (Server)        │
│  - Socket.IO        │                             └────────┬─────────┘
│  - Jetpack Compose  │                                      │
└─────────────────────┘                             ┌────────▼─────────┐
                                                    │  Chitu Printers  │
                                                    └──────────────────┘
```

### Key Components:

1. **Token Authentication**: JWT tokens for secure mobile access
2. **REST API**: HTTP endpoints for printer management
3. **Socket.IO**: Real-time printer status updates
4. **Local/Remote**: Works on same network or over internet

---

## Prerequisites

### Required Software:

- **Android Studio** (Latest version: Electric Eel or newer)
  - Download: https://developer.android.com/studio

- **JDK 17+** (Usually bundled with Android Studio)

- **Physical Android Device or Emulator**
  - Minimum API 24 (Android 7.0)
  - Recommended API 30+ (Android 11+)

### ChitUI Server Requirements:

- Raspberry Pi running ChitUI with token authentication enabled
- Network connectivity (same network for testing)
- Python dependencies installed: `PyJWT`

---

## Project Setup

### Step 1: Create New Android Project

1. Open Android Studio
2. Click "New Project"
3. Select **"Empty Activity"** template
4. Configure:
   - **Name**: ChitUI Remote
   - **Package name**: com.example.chitui
   - **Language**: Kotlin
   - **Minimum SDK**: API 24 (Android 7.0)

### Step 2: Add Dependencies

Edit `app/build.gradle.kts` (Module level):

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.chitui"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.chitui"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // Networking - Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Socket.IO for real-time updates
    implementation("io.socket:socket.io-client:2.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Swipe to refresh
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")

    // Image loading (for printer images/camera feeds)
    implementation("io.coil-kt:coil-compose:2.5.0")
}
```

### Step 3: Add Permissions

Edit `AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Internet permission for API calls -->
    <uses-permission android:name="android.permission.INTERNET" />

    <!-- Network state for connection checking -->
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.ChitUI"
        android:usesCleartextTraffic="true">

        <!-- usesCleartextTraffic allows HTTP for local testing -->
        <!-- Remove this in production and use HTTPS only -->

        <activity
            android:name=".ui.LoginActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".ui.MainActivity"
            android:exported="false" />
    </application>

</manifest>
```

### Step 4: Project Structure

Create the following package structure:

```
com.example.chitui/
├── api/
│   ├── ChitUIApi.kt            (From code_examples)
│   ├── RetrofitClient.kt       (From code_examples)
│   └── TokenManager.kt         (Included in RetrofitClient.kt)
├── socket/
│   └── ChitUISocketClient.kt   (From code_examples)
├── viewmodel/
│   └── PrinterViewModel.kt     (From code_examples)
└── ui/
    ├── LoginActivity.kt        (From code_examples)
    ├── MainActivity.kt         (Create new - see below)
    └── PrinterListScreen.kt    (From code_examples)
```

---

## Implementation Steps

### Step 5: Copy Code Examples

Copy all the code files from the `code_examples/` folder into your Android project:

1. **ChitUIApi.kt** → `app/src/main/java/com/example/chitui/api/`
2. **RetrofitClient.kt** → `app/src/main/java/com/example/chitui/api/`
3. **ChitUISocketClient.kt** → `app/src/main/java/com/example/chitui/socket/`
4. **PrinterViewModel.kt** → `app/src/main/java/com/example/chitui/viewmodel/`
5. **LoginActivity.kt** → `app/src/main/java/com/example/chitui/ui/`
6. **PrinterListScreen.kt** → `app/src/main/java/com/example/chitui/ui/`

### Step 6: Create MainActivity

Create `MainActivity.kt`:

```kotlin
package com.example.chitui.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.chitui.api.RetrofitClient
import com.example.chitui.api.TokenManager
import com.example.chitui.viewmodel.PrinterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PrinterViewModel by viewModels()
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(this)

        // Check if logged in
        if (!tokenManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            MaterialTheme {
                PrinterListScreen(
                    viewModel = viewModel,
                    onPrinterClick = { printer ->
                        // Navigate to printer detail screen
                        // TODO: Implement detail screen
                    }
                )
            }
        }

        // Load initial data
        viewModel.loadPrinters()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnectSocket()
    }
}
```

### Step 7: Configure Server URL

In `RetrofitClient.kt`, update the BASE_URL to match your Raspberry Pi:

```kotlin
// Local network (find your Pi's IP with: hostname -I)
private const val BASE_URL = "http://192.168.1.100:8080"

// Or use Tailscale VPN IP
// private const val BASE_URL = "http://100.64.0.1:8080"
```

Also update the same URL in `ChitUISocketClient.kt`:

```kotlin
private val serverUrl = "http://192.168.1.100:8080"
```

---

## Testing

### Local Network Testing (Same WiFi)

1. **Find your Pi's IP address:**
   ```bash
   # On Raspberry Pi:
   hostname -I
   # Example output: 192.168.1.100
   ```

2. **Ensure ChitUI is running:**
   ```bash
   cd ChitUI_v1
   python3 main.py
   ```

3. **Install PyJWT on Raspberry Pi:**
   ```bash
   pip3 install PyJWT
   ```

4. **Test from browser first:**
   - Open: `http://192.168.1.100:8080`
   - Login with password (default: `admin`)
   - Verify web interface works

5. **Test API endpoints:**
   ```bash
   # Login and get token
   curl -X POST http://192.168.1.100:8080/api/mobile/login \
     -H "Content-Type: application/json" \
     -d '{"password": "admin"}'

   # Should return:
   # {"success": true, "token": "eyJ0eXAi...", ...}

   # Test authenticated endpoint
   curl http://192.168.1.100:8080/api/mobile/printers \
     -H "Authorization: Bearer YOUR_TOKEN_HERE"
   ```

6. **Run Android app:**
   - Connect phone to same WiFi network as Pi
   - Or use Android emulator
   - Launch app
   - Login with password
   - Should see printer list

### Troubleshooting Connection Issues:

**Problem: "Connection refused" or "Network error"**

Solutions:
- ✓ Check Pi and phone on same WiFi network
- ✓ Verify ChitUI is running: `ps aux | grep main.py`
- ✓ Check firewall: `sudo ufw status` (should allow port 8080)
- ✓ Test Pi is reachable: `ping 192.168.1.100` (from phone terminal app)
- ✓ Enable `usesCleartextTraffic="true"` in AndroidManifest.xml

**Problem: "401 Unauthorized"**

Solutions:
- ✓ Verify password is correct
- ✓ Check token is being saved: Add logs in TokenManager
- ✓ Ensure token is being sent in Authorization header

**Problem: Socket.IO not connecting**

Solutions:
- ✓ Check Socket.IO URL matches REST API URL
- ✓ Verify ChitUI Socket.IO is running on same port
- ✓ Check Android logs for connection errors

---

## Remote Access Setup

To access your Pi from anywhere (not just local network):

### Option 1: Tailscale VPN (Recommended - Secure & Easy)

1. **Install on Raspberry Pi:**
   ```bash
   curl -fsSL https://tailscale.com/install.sh | sh
   sudo tailscale up
   ```

2. **Install on Android:**
   - Download Tailscale app from Play Store
   - Login with same account
   - Connect

3. **Update Android app URLs:**
   ```kotlin
   // Use Tailscale IP (find with: tailscale ip -4)
   private const val BASE_URL = "http://100.64.0.1:8080"
   ```

4. **Benefits:**
   - ✓ Secure encrypted connection
   - ✓ No port forwarding needed
   - ✓ Works from anywhere
   - ✓ Free for personal use

### Option 2: ngrok (Quick Testing)

1. **Install on Raspberry Pi:**
   ```bash
   wget https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-arm.tgz
   tar xvzf ngrok-v3-stable-linux-arm.tgz
   ```

2. **Start tunnel:**
   ```bash
   ./ngrok http 8080
   ```

3. **Use provided URL:**
   ```
   Forwarding  https://abc123.ngrok.io -> http://localhost:8080
   ```

4. **Update Android app:**
   ```kotlin
   private const val BASE_URL = "https://abc123.ngrok.io"
   ```

### Option 3: Port Forwarding (Advanced)

**⚠️ Security Warning:** Exposing Pi directly to internet requires HTTPS and strong passwords!

1. **Set up HTTPS with Let's Encrypt**
2. **Configure router port forwarding:**
   - Forward external port 443 → Pi:8080
3. **Use Dynamic DNS** for changing IP
4. **Change default password!**

---

## API Reference

### Authentication

#### POST `/api/mobile/login`
Login and receive JWT token

**Request:**
```json
{
  "password": "your_password"
}
```

**Response:**
```json
{
  "success": true,
  "token": "eyJ0eXAiOiJKV1QiLCJhbGci...",
  "expires_in": 2592000,
  "user_id": "admin"
}
```

#### POST `/api/mobile/refresh-token`
Refresh JWT token (requires Authorization header)

**Response:**
```json
{
  "success": true,
  "token": "new_token_here",
  "expires_in": 2592000
}
```

### Printer Management

#### GET `/api/mobile/printers`
Get list of all printers

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "printers": [
    {
      "id": "printer123",
      "name": "My Printer",
      "ip": "192.168.1.50",
      "status": "connected",
      "current_file": "model.ctb",
      "progress": 45
    }
  ],
  "count": 1
}
```

#### GET `/api/mobile/printer/{printer_id}/info`
Get detailed printer information

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "printer": {
    "id": "printer123",
    "Attributes": {
      "MachineName": "Chitu Printer",
      "CurrentStatus": "Printing",
      "FileList": [...]
    }
  }
}
```

### System Status

#### GET `/api/mobile/status`
Get ChitUI system status

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "status": {
    "printer_count": 2,
    "usb_gadget_enabled": true,
    "camera_support": false
  }
}
```

### Socket.IO Events

**Emit:**
- `get_printers` - Request printer list update
- `get_attributes` - Request printer details
- `action_print` - Start print job
- `action_pause` - Pause current print
- `action_resume` - Resume paused print
- `action_stop` - Stop current print

**Listen:**
- `printers` - Printer list update
- `printer_info` - Printer detail update
- `connect` - Socket connected
- `disconnect` - Socket disconnected

---

## Next Steps

### Enhancements to Add:

1. **Printer Detail Screen**
   - File list
   - Camera view (if enabled)
   - Temperature graphs
   - Print history

2. **File Upload**
   - Pick .ctb/.goo files from phone
   - Upload to printer
   - Progress indicator

3. **Notifications**
   - Print completion alerts
   - Error notifications
   - FCM integration

4. **Settings Screen**
   - Server URL configuration
   - Theme selection (dark/light mode)
   - Refresh interval settings

5. **Multi-printer Support**
   - Manage multiple printers
   - Favorite printers
   - Printer groups

---

## Resources

- **Android Development:** https://developer.android.com
- **Retrofit Documentation:** https://square.github.io/retrofit/
- **Socket.IO Android:** https://socket.io/docs/v4/client-api/
- **Jetpack Compose:** https://developer.android.com/jetpack/compose
- **ChitUI GitHub:** https://github.com/yourusername/ChitUI_v1

---

## Support

If you encounter issues:

1. Check ChitUI server logs
2. Check Android Logcat for errors
3. Verify network connectivity
4. Test API with curl/Postman first
5. Open an issue on GitHub

---

**Happy Printing! 🎉**
