# 👽 AlienKDS

**AlienKDS** is a simple, lightweight Android **Kitchen Display System (KDS)** designed to work with a restaurant POS system.

Instead of running a full native kitchen application, AlienKDS acts as a dedicated Android client for the POS's existing kitchen interface. It connects to the restaurant server over the local network and displays the kitchen dashboard in a fullscreen WebView.

## Features

* 📱 **Android KDS client**
* 🌐 Connects to a local restaurant POS server
* 🖥️ Displays the POS kitchen interface through WebView
* 🔌 Uses a configurable server IP address
* 💾 Remembers the server IP after setup
* 🔄 Automatically loads the kitchen screen on subsequent launches
* 🔒 Supports local HTTP connections
* 🔄 Landscape-only interface
* 💡 Keeps the screen awake while the KDS is running
* ⬅️ WebView back-navigation support
* ⚡ Lightweight and fast
* 🏪 Designed for dedicated kitchen tablets/phones

## How It Works

AlienKDS does not contain the restaurant's kitchen logic itself.

The architecture is intentionally simple:

```text
┌─────────────────────┐
│   Restaurant POS    │
│                     │
│  Node.js / Express  │
│                     │
│   :3000/kitchen     │
└──────────┬──────────┘
           │
           │ Local Network
           │ HTTP
           ▼
┌─────────────────────┐
│      AlienKDS       │
│                     │
│   Android Device    │
│                     │
│      WebView        │
└─────────────────────┘
```

The Android application loads:

```text
http://<server-ip>:3000/kitchen
```

For example:

```text
http://192.168.1.14:3000/kitchen
```

The kitchen UI, order management, and business logic remain on the POS server.

## First Launch

When AlienKDS is opened for the first time, it asks for the IP address of the computer running the restaurant POS server.

Example:

```text
Server address:

192.168.1.14
```

After saving the address, AlienKDS connects to:

```text
http://192.168.1.14:3000/kitchen
```

The address is stored locally using Android `SharedPreferences`, so it does not need to be entered again every time the application starts.

## Requirements

### Android

* Android 5.0+ (API 21 minimum)
* Android 16 tested
* Landscape-capable device
* Wi-Fi or network connection to the POS server

### Server

The restaurant POS server must:

1. Be running on the same local network as the Android device.
2. Listen on port `3000`.
3. Expose the kitchen interface at:

```text
/kitchen
```

For example:

```text
http://192.168.1.14:3000/kitchen
```

## Network Setup

The Android device and POS computer need to be able to communicate with each other.

Example:

```text
POS Computer
IP: 192.168.1.14
Port: 3000

        │
        │ Wi-Fi / LAN
        ▼

Android KDS
IP: 192.168.1.25
```

You should be able to open the following URL from a browser on the Android device:

```text
http://192.168.1.14:3000/kitchen
```

If the page cannot be opened in the browser, AlienKDS will not be able to connect either.

### Firewall

If the POS computer uses Windows Firewall or another firewall, make sure TCP port `3000` is accessible from devices on the local network.

## Project Structure

```text
AlienKDS/
│
├── build.gradle
├── settings.gradle
├── gradle.properties
│
└── app/
    ├── build.gradle
    │
    └── src/
        └── main/
            │
            ├── AndroidManifest.xml
            │
            ├── java/
            │   └── com/
            │       └── restaurantpos/
            │           └── kds/
            │               └── MainActivity.java
            │
            └── res/
                ├── layout/
                │   └── activity_main.xml
                │
                └── values/
                    └── styles.xml
```

## Technology

AlienKDS is intentionally minimal.

| Component            | Technology                   |
| -------------------- | ---------------------------- |
| Language             | Java                         |
| UI                   | Android XML                  |
| Web interface        | Android WebView              |
| Storage              | SharedPreferences            |
| Build system         | Gradle                       |
| Android build plugin | Android Gradle Plugin        |
| Minimum Android      | API 21                       |
| Target SDK           | API 34                       |
| Server communication | HTTP                         |
| Frontend             | Existing POS `/kitchen` page |

## Application Flow

```text
Application starts
       │
       ▼
Load saved server IP
       │
       ├── IP exists ──────► Load /kitchen
       │
       ▼
No IP saved
       │
       ▼
Ask user for server IP
       │
       ▼
Save IP
       │
       ▼
Load /kitchen
       │
       ▼
Kitchen Display
```

## Building

Clone the repository and open the project in Android Studio.

Make sure Android SDK and JDK 17 are installed.

Then build the APK:

```bash
./gradlew assembleDebug
```

On Windows:

```bash
gradlew.bat assembleDebug
```

The generated APK will be located under:

```text
app/build/outputs/apk/debug/
```

## Configuration

The server address is currently configured from inside the Android application.

On first launch, enter the local IP address of the computer running the POS.

For example:

```text
192.168.1.14
```

AlienKDS automatically adds:

```text
:3000/kitchen
```

resulting in:

```text
http://192.168.1.14:3000/kitchen
```

## Why AlienKDS?

AlienKDS is intentionally designed to be **simple**.

The restaurant POS already contains the kitchen functionality, so there is no reason to recreate that functionality inside a separate native Android application.

AlienKDS provides the missing bridge:

```text
Existing POS
     +
Android device
     ↓
Dedicated Kitchen Display
```

This keeps the Android application small while allowing the kitchen interface to be updated on the server without requiring a new APK for every UI change.

## Future Improvements

Possible future features include:

* 🔧 Server IP configuration screen
* 🔄 Automatic reconnection
* ❤️ Connection/status indicator
* 🔔 Kitchen order notifications
* 🖥️ Fullscreen immersive mode
* 🔒 Optional HTTPS support
* 📡 Automatic server discovery
* 🏷️ Multiple kitchen stations
* ⚙️ KDS-specific settings
* 🔄 Automatic page reload after connection loss
* 💤 Better handling of Android battery optimization
* 📦 Release APK distribution

## License

MIT License

Copyright (c) 2026 Joao Pedro Brito

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

**AlienKDS — a simple kitchen display for your restaurant POS.** 👽🍳
