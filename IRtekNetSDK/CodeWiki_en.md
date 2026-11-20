# IRtekNetSDK Android - Code Wiki

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Architecture](#2-architecture)
- [3. Directory Structure](#3-directory-structure)
- [4. Environment and Dependencies](#4-environment-and-dependencies)
- [5. Quick Start](#5-quick-start)
- [6. Core Modules](#6-core-modules)
- [7. SDK API Reference](#7-sdk-api-reference)
- [8. Demo App Walkthrough](#8-demo-app-walkthrough)
- [9. Key Data Structures and Enums](#9-key-data-structures-and-enums)
- [10. Common Usage Flows](#10-common-usage-flows)
- [11. Build and Deployment](#11-build-and-deployment)

---

## 1. Project Overview

**IRtekNetSDK** is FOTRIC's infrared thermal camera network SDK for Android. It provides full capabilities for communicating with infrared thermal devices over the network (HTTP REST API). The SDK is written in Kotlin/Java, with Native C/C++ called through JNI. It supports device discovery, login and authentication, video stream preview, thermal parameter configuration, temperature measurement management, alarm linkage, and more.

### Key Features

| Category | Description |
|---------|---------|
| **Device management** | Device search, login/logout, device info, restart/factory reset, firmware upgrade |
| **Network configuration** | IP/subnet/gateway/DNS, NTP time sync, FTP upload, service port configuration |
| **User management** | User CRUD, three privilege levels (administrator / operator / viewer) |
| **Video streaming** | H.264/RGBA/YUV422/BGRA stream capture, local recording, real-time frame callbacks |
| **Thermal settings** | Shutter calibration, measurement range, LUT, measurement parameters, span mode, palettes |
| **Display settings** | Channel management, OSD overlay, image parameters (contrast/brightness/DDE), image flip |
| **Measurement markers** | Point / rectangle / ellipse / polygon / polyline measurement objects |
| **Alarm linkage** | Temperature alarm rules, FTP upload linkage |
| **Thermal data** | Raw AD data, global temperature statistics, marker temperatures, real-time data stream |

### Communication Protocol

- **Protocol**: HTTP REST API (Basic Authentication)
- **Default HTTP port**: 80 (configurable)
- **RTSP port**: 554 (configurable)
- **RTP port**: 5004 (configurable)

---

## 2. Architecture

### Overall Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Demo Application (app)                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              UI Layer (Jetpack Compose)              │  │
│  │  ConnectScreen / DeviceScreen / Setting Tabs / Log   │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────▼───────────────────────────────┐  │
│  │              NetSDKManager (Kotlin API)              │  │
│  │  Lifecycle / JSON serialization / Coroutines / Callbacks │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────▼───────────────────────────────┐  │
│  │               NativeSDK (JNI Bridge)                 │  │
│  │   StreamFrameListener / EventCallback / JNI bindings │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
└─────────────────────────┼────────────────────────────────────┘
                          │ JNI
┌─────────────────────────▼────────────────────────────────────┐
│                  IRtekNetSDK Native Library                  │
│  ┌─────────────┐ ┌─────────────┐ ┌──────────────────────┐  │
│  │  HTTP Client│ │ RTSP/RTP    │ │  Thermal Processing  │  │
│  │  (REST API) │ │  Streaming  │ │  (Decoding/Palette)  │  │
│  └─────────────┘ └─────────────┘ └──────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                          │ HTTP/RTSP
┌─────────────────────────▼────────────────────────────────────┐
│              Infrared Thermal Device (Hardware)              │
│            IP Camera / Thermal Sensor / Embedded OS         │
└─────────────────────────────────────────────────────────────┘
```

### Layer Description

| Layer | Module | Responsibility |
|-----|------|-----|
| **Application layer** | Demo App | Reference implementation of SDK features, UI built with Jetpack Compose |
| **Kotlin API layer** | NetSDKManager | Kotlin-friendly API wrapping JSON serialization, coroutine dispatch, and error handling |
| **JNI bridge layer** | NativeSDK | JNI interface definitions and Listener interfaces for Native-to-Java callbacks |
| **Native layer** | libIRtekNetSDK.so | Core C/C++ implementation: HTTP communication, RTSP stream parsing, thermal data processing |

---

## 3. Directory Structure

```
IRtekNetSDK/
├── README.md                          # SDK quick start
├── library/                           # SDK release artifacts
│   ├── aar/
│   │   └── IRtekNetSDK-release.aar    # Android AAR library
│   └── header/
│       └── IRtekNetSDK.h              # C API header (full interface reference)
└── demo/
    └── IRtekNetSDKDemo/               # Android Studio demo project
        ├── build.gradle               # Root build script
        ├── settings.gradle            # Module configuration
        ├── gradle.properties          # Gradle properties
        ├── gradle/
        │   ├── libs.versions.toml     # Version catalog (dependency versions)
        │   └── wrapper/
        │       ├── gradle-wrapper.jar
        │       └── gradle-wrapper.properties
        ├── app/                       # Demo application module
        │   ├── build.gradle           # App build configuration
        │   ├── proguard-rules.pro     # ProGuard rules
        │   └── src/main/
        │       ├── AndroidManifest.xml
        │       ├── java/com/irtek/netsdk/demo/
        │       │   ├── MainActivity.kt         # Main Activity (all UI and logic)
        │       │   └── ui/theme/
        │       │       └── Theme.kt            # Compose theme
        │       └── res/                        # Resources
        │           ├── drawable/               # Drawable assets
        │           ├── mipmap-*/               # Launcher icons
        │           └── values/                 # Strings / themes / colors
        └── IRtekNetSDK/                        # SDK library module (external source path)
```

---

## 4. Environment and Dependencies

### Development Environment

| Tool | Version |
|-----|---------|
| Android Studio | Iguana or later (latest recommended) |
| Gradle | 8.7+ (Wrapper already configured) |
| JDK | 11 (compileOptions) |
| Android SDK | compileSdk 36, minSdk 24, targetSdk 36 |
| Kotlin | 2.2.0 |

### Android Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

Cleartext traffic (HTTP) must also be enabled on the `application` tag:

```xml
android:usesCleartextTraffic="true"
```

### Third-Party Dependencies

| Dependency | Version | Purpose |
|-----|------|-----|
| AndroidX Core KTX | 1.10.1 | Android KTX extensions |
| AndroidX Lifecycle Runtime | 2.6.1 | Lifecycle-aware components |
| AndroidX Activity Compose | 1.8.0 | Compose Activity support |
| Kotlin Coroutines Android | 1.7.3 | Asynchronous coroutines |
| Jetpack Compose BOM | 2024.09.00 | Compose UI framework |
| Compose Material3 | - | Material3 components |
| Compose Material Icons Extended | - | Extended icon set |
| OkHttp | 4.12.0 | HTTP client (used internally by the SDK) |
| Gson | 2.11.0 | JSON serialization (used internally by the SDK) |

---

## 5. Quick Start

### 5.1 Integrate the AAR into an Existing Project

1. Copy `IRtekNetSDK-release.aar` into the app module's `libs/` directory
2. Add the dependency in `build.gradle`:

```groovy
dependencies {
    implementation files('libs/IRtekNetSDK-release.aar')
    // or use fileTree
    implementation fileTree(dir: 'libs', include: ['*.aar', '*.jar'])
}
```

3. Sync Gradle

### 5.2 Basic Usage Flow

```kotlin
// 1. Initialize the SDK
NetSDKManager.enableLog(true)
NetSDKManager.init()

// 2. Log in to the device
val result = NetSDKManager.login(
    ip = "192.168.10.177",
    port = 80,
    userName = "admin",
    password = "admin"
)
if (result.isSuccess) {
    // Login succeeded; result.data is the device handle (managed internally)
}

// 3. Search for devices on the LAN
val searchResult = NetSDKManager.searchDevices(timeoutMs = 3000)
if (searchResult.isSuccess) {
    val devices = searchResult.data  // JSONArray
}

// 4. Get device information
val deviceInfo = NetSDKManager.getDeviceInfo()

// 5. Start video stream preview
NetSDKManager.setStreamFrameListener(object : NativeSDK.StreamFrameListener {
    override fun onStreamFrame(handle: Long, streamId: Int, streamType: Int,
                               width: Int, height: Int, data: ByteArray,
                               timestampUs: Long, keyFrame: Boolean) {
        // Convert data to a Bitmap for display
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(data))
    }
})
NetSDKManager.startStream(streamId = firstStreamId, streamType = 2)  // 2 = RGBA

// 6. Log out and clean up
NetSDKManager.stopStream()
NetSDKManager.logout()
NetSDKManager.cleanup()
```

### 5.3 Run the Demo

1. Open `IRtekNetSDK/demo/IRtekNetSDKDemo/` in Android Studio
2. Configure the SDK module path correctly in the demo project (`projectDir` in `settings.gradle`)
3. Connect an Android device (the device and the thermal camera must be on the same LAN)
4. Click Run to deploy to the device
5. Default connection parameters: IP `192.168.10.177`, port `80`, username/password `admin/admin`

---

## 6. Core Modules

### 6.1 NetSDKManager (Kotlin API Entry Point)

**Package**: `com.irtek.netsdk.NetSDKManager`

This is the SDK's core manager class and the Kotlin-layer API surface. It is designed as a singleton; all methods are static / companion-object methods.

**Responsibilities**:
- SDK lifecycle management (init/cleanup)
- Device login/logout and session management
- Device discovery (LAN search)
- Kotlin wrappers for all business APIs
- Callback listener management
- JSON serialization/deserialization
- Coroutine dispatch (IO thread execution, main-thread callbacks)

**Return type**:
All API calls return `SDKResult<T>`:
```kotlin
data class SDKResult<T>(
    val isSuccess: Boolean,
    val code: Int,           // 0 means success; negative values are error codes
    val message: String?,    // Error message
    val data: T?             // Returned data (JSONObject/JSONArray/Int/Long, etc.)
)
```

### 6.2 NativeSDK (JNI Bridge)

**Package**: `com.irtek.netsdk.NativeSDK`

Defines the JNI-layer interfaces and callback listeners:

**Key interfaces**:
```kotlin
interface StreamFrameListener {
    fun onStreamFrame(
        handle: Long,        // Device handle
        streamId: Int,       // Stream ID
        streamType: Int,     // Stream type (1=H264, 2=RGBA, 3=YUV422, 4=BGRA)
        width: Int,          // Image width
        height: Int,         // Image height
        data: ByteArray,     // Image data
        timestampUs: Long,   // Timestamp (microseconds)
        keyFrame: Boolean    // Whether this is a key frame
    )
}

interface EventCallback {
    fun onEvent(eventType: Int, streamId: Int, message: String)
}
```

### 6.3 Demo Application Module

**Package**: `com.irtek.netsdk.demo`

The demo app is a full feature showcase built with a modern Jetpack Compose UI.

**Main components**:

| Component | File | Responsibility |
|-----|------|-----|
| MainActivity | [MainActivity.kt](demo/IRtekNetSDKDemo/app/src/main/java/com/irtek/netsdk/demo/MainActivity.kt) | Main Activity containing all UI and business logic |
| ConnectScreen | MainActivity.kt L89-L279 | Device connection page: login form and device search list |
| DeviceScreen | MainActivity.kt L285-L925 | Device main page: video preview, feature tabs, and logs |
| TabProperties | MainActivity.kt L977-L1000 | Properties tab: device info, lens info, video streams |
| TabThermal | MainActivity.kt L1005-L1150 | Thermal tab: calibration, measurement parameters, span, units |
| TabDisplay | MainActivity.kt L1590-L1778 | Display tab: channels, OSD overlay, image parameters |
| TabTime | MainActivity.kt L1280-L1396 | Time tab: time mode and NTP configuration |
| TabNetwork | MainActivity.kt L1155-L1275 | Network tab: IP configuration, FTP, ports |
| TabUsers | MainActivity.kt L1401-L1501 | Users tab: user list, add/update/delete |
| TabMarkers | MainActivity.kt L1783-L1851 | Markers tab: measurement marker list |
| TabAlarms | MainActivity.kt L1856-L1929 | Alarms tab: alarm rules and linkage settings |
| IRtekNetSDKDemoTheme | [Theme.kt](demo/IRtekNetSDKDemo/app/src/main/java/com/irtek/netsdk/demo/ui/theme/Theme.kt) | Compose theme configuration |

---

## 7. SDK API Reference

### 7.1 SDK Lifecycle

| Method | Description | Parameters | Returns |
|-----|------|-----|------|
| `init()` | Initialize the SDK | None | `SDKResult<Unit>` |
| `cleanup()` | Release SDK resources | None | `SDKResult<Unit>` |
| `enableLog(enable: Boolean)` | Enable/disable logging | enable: whether to enable | `SDKResult<Unit>` |
| `getSdkVersion()` | Get the SDK version | None | `SDKResult<JSONObject>` (major/minor/patch/build) |

### 7.2 Device Connection Management

| Method | Description | Parameters | Returns |
|-----|------|-----|------|
| `login(ip, port, user, pwd)` | Log in to a device | ip: String, port: Int, userName: String, password: String | `SDKResult<Long>` (handle) |
| `logout()` | Log out of the current device | None | `SDKResult<Unit>` |
| `searchDevices(timeoutMs)` | Search for devices on the LAN | timeoutMs: Int (timeout in milliseconds) | `SDKResult<JSONArray>` |

**Device search result fields**:
```json
{
  "ip": "192.168.10.177",
  "name": "device name",
  "model": "device model",
  "serial_no": "serial number",
  "id": "device ID",
  "firmware_version": "firmware version"
}
```

### 7.3 Device Management

| Method | Description | Parameters | Returns |
|-----|------|-----|------|
| `getDeviceInfo()` | Get device information | None | `SDKResult<JSONObject>` |
| `setDeviceInfo(json)` | Set device information | json: String (JSON) | `SDKResult<Unit>` |
| `deviceRestart()` | Restart the device | None | `SDKResult<Unit>` |
| `deviceReset()` | Restore factory settings | None | `SDKResult<Unit>` |
| `deviceUpgrade(json)` | Remote firmware upgrade | json: String (local path) | `SDKResult<Unit>` |
| `getTime()` | Get device time | None | `SDKResult<JSONObject>` |
| `setTime(json)` | Set device time | json: String | `SDKResult<Unit>` |
| `getPort()` | Get service ports | None | `SDKResult<JSONObject>` |
| `setPort(json)` | Set service ports | json: String | `SDKResult<Unit>` |

**DeviceInfo fields**: name, model, spec, serial_no, id, brand, company, firmware_name, firmware_version

**PortInfo fields**: http_port_no, rtsp_port_no, rtp_port_no

**TimeInfo fields**: time_mode (0=local / 1=NTP), local_time, time_zone

### 7.4 Network Configuration

| Method | Description | Parameters | Returns |
|-----|------|-----|------|
| `getNetworks()` | Get the network configuration list | None | `SDKResult<JSONArray>` |
| `getNetwork(id)` | Get a specific network configuration | id: Int | `SDKResult<JSONObject>` |
| `setNetwork(id, json)` | Set a network configuration | id: Int, json: String | `SDKResult<Unit>` |
| `getNtpServers()` | Get the NTP server list | None | `SDKResult<JSONArray>` |
| `getNtpServer(id)` | Get a specific NTP server | id: Int | `SDKResult<JSONObject>` |
| `setNtpServer(id, json)` | Set an NTP server | id: Int, json: String | `SDKResult<Unit>` |
| `getFtp()` | Get FTP configuration | None | `SDKResult<JSONObject>` |
| `setFtp(json)` | Set FTP configuration | json: String | `SDKResult<Unit>` |

**NetworkInfo fields**: id, name, dhcp (0/1), ip, netmask, dns (Array), gateway (Array)

**NtpInfo fields**: id, host_name, port_no, syn_interval

**FtpInfo fields**: use_ssl, ip, port_no, user_name, pwd, enable_annoy, path_template

### 7.5 User Management

| Method | Description | Parameters | Returns |
|-----|------|-----|------|
| `getUsers()` | Get the user list | None | `SDKResult<JSONArray>` |
| `getUser(id)` | Get a specific user | id: Int | `SDKResult<JSONObject>` |
| `updateUser(json)` | Add or update a user | json: String | `SDKResult<Unit>` |
| `deleteUser(id)` | Delete a user | id: Int | `SDKResult<Unit>` |

**UserInfo fields**: id, user_level (0=ROOT / 1=OPERATOR / 2=VIEWER), user_name

**Privilege levels**:
- **ROOT (0)**: Administrator — system operations, user management, network configuration, firmware upgrade, restart/reset
- **OPERATOR (1)**: Operator — day-to-day tuning, image/video/thermal parameter settings
- **VIEWER (2)**: Viewer — read-only; can only view status and data

### 7.6 Video Streams

| Method | Description | Parameters | Returns |
|-----|------|-----|------|
| `getStreams()` | Get the video stream list | None | `SDKResult<JSONArray>` |
| `getStream(id)` | Get parameters for a specific stream | id: Int | `SDKResult<JSONObject>` |
| `getStreamStatus(id)` | Get stream connection status | id: Int | `SDKResult<JSONObject>` |
| `startStream(streamId, streamType)` | Start receiving a video stream | streamId: Int, streamType: Int (2=RGBA recommended) | `SDKResult<Unit>` |
| `stopStream()` | Stop the video stream | None | `SDKResult<Unit>` |
| `setStreamFrameListener(listener)` | Set the frame callback | listener: StreamFrameListener? | Unit |
| `startStreamRecord(id, path)` | Start local recording | id: Int, path: String | `SDKResult<Unit>` |
| `stopStreamRecord(id)` | Stop local recording | id: Int | `SDKResult<Unit>` |

**StreamInfo fields**: id, bit_rate, codec_type, disable_skip, enable_rc, gop_size, h264_profile, initial_qp, max_frame_rate, maximum_qp, quality_control_type, resolution_width, resolution_height

**Stream types**:
- 1: H.264
- 2: RGBA (recommended for preview; can be converted directly to a Bitmap)
- 3: YUV422
- 4: BGRA

### 7.7 Channel and Display Settings

| Method | Description | Parameters | Returns |
|-----|------|-----|------|
| `getChannels()` | Get the channel list | None | `SDKResult<JSONArray>` |
| `getChannel(id)` | Get channel information | id: Int | `SDKResult<JSONObject>` |
| `setChannel(id, json)` | Set channel information | id: Int, json: String | `SDKResult<Unit>` |
| `getOverlay(channelId)` | Get OSD overlay | channelId: Int | `SDKResult<JSONObject>` |
| `setOverlay(channelId, json)` | Set OSD overlay | channelId: Int, json: String | `SDKResult<Unit>` |
| `getImageParams(channelId)` | Get image parameters | channelId: Int | `SDKResult<JSONObject>` |
| `setImageParams(channelId, json)` | Set image parameters | channelId: Int, json: String | `SDKResult<Unit>` |
| `getImageFlip(channelId)` | Get flip mode | channelId: Int | `SDKResult<Int>` |
| `setImageFlip(channelId, mode)` | Set flip mode | channelId: Int, mode: Int | `SDKResult<Unit>` |

**ImageParams fields**: contrast, brightness, dde_enable (0/1), dde_value

**Overlay structure**:
```json
{
  "channel_name_overlay": { "enabled": 0/1, "align": 0-3, "mode": 0/1, "x": 0, "y": 0 },
  "time_overlay": { "enabled": 0/1, "align": 0-3, "mode": 0/1, "x": 0, "y": 0 }
}
```

**Alignment**: 0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right
**Display mode**: 0=always on, 1=blinking
**Flip mode**: 0=none, 1=vertical, 2=horizontal, 3=center (rotate 180°)

### 7.8 Thermal Settings

| Method | Description | Parameters | Returns |
|-----|------|-----|------|
| `getThermalLensInfo()` | Get lens information | None | `SDKResult<JSONObject>` |
| `thermalCalibrate()` | Perform thermal calibration (shutter) | None | `SDKResult<Unit>` |
| `getThermalShutterMode()` | Get shutter mode | None | `SDKResult<Int>` |
| `setThermalShutterMode(mode)` | Set shutter mode | mode: Int | `SDKResult<Unit>` |
| `getThermalShutterTime()` | Get auto-calibration interval | None | `SDKResult<Int>` (minutes) |
| `setThermalShutterTime(minutes)` | Set auto-calibration interval | minutes: Int | `SDKResult<Unit>` |
| `getThermalResolution()` | Get thermal resolution | None | `SDKResult<JSONObject>` |
| `getThermalRange()` | Get measurement range | None | `SDKResult<Int>` (rangeId) |
| `setThermalRange(id)` | Set measurement range | id: Int | `SDKResult<Unit>` |
| `getThermalLut()` | Get LUT | None | `SDKResult<JSONObject>` |
| `getThermalParams()` | Get measurement parameters | None | `SDKResult<JSONObject>` |
| `setThermalParams(json)` | Set measurement parameters | json: String | `SDKResult<Unit>` |
| `getThermalColorDist()` | Get color distribution type | None | `SDKResult<Int>` |
| `setThermalColorDist(type)` | Set color distribution type | type: Int | `SDKResult<Unit>` |
| `getThermalGain()` | Get span mode | None | `SDKResult<JSONObject>` |
| `setThermalGain(json)` | Set span mode | json: String | `SDKResult<Unit>` |
| `getThermalPresetPalettes()` | Get preset palette list | None | `SDKResult<JSONArray>` |
| `getThermalPalette()` | Get the current palette | None | `SDKResult<JSONObject>` |
| `setThermalPalette(json)` | Set the palette | json: String | `SDKResult<Unit>` |
| `getThermalOverlay()` | Get thermal OSD | None | `SDKResult<JSONObject>` |
| `setThermalOverlay(json)` | Set thermal OSD | json: String | `SDKResult<Unit>` |
| `getThermalUnit()` | Get unit settings | None | `SDKResult<JSONObject>` |
| `setThermalUnit(json)` | Set units | json: String | `SDKResult<Unit>` |
| `getThermalCapture(mode, path)` | Thermal snapshot | mode: Int (0=raw / 1=IRtek), path: String | `SDKResult<Unit>` |
| `getThermalSr()` | Get super-resolution mode | None | `SDKResult<Int>` |
| `setThermalSr(mode)` | Set super-resolution mode | mode: Int | `SDKResult<Unit>` |
| `setThermalDpc(json)` | Set dead-pixel correction | json: String | `SDKResult<Unit>` |

**LensInfo fields**: name, focal (focal length in mm), hfov (horizontal FOV), vfov (vertical FOV), band (wavelength band), range_map (range mapping array)

**ThermalParams fields**: emissivity, distance (meters), refl_temp (reflected temperature in K), atm_temp (atmospheric temperature in K), rel_humidity (relative humidity 0-1)

**Gain fields**: mode (1=fixed / 2=auto), max, min, fixedSpan (0/1), span

**Palette fields**: ref_no (index), colors (IntArray), inverted (0/1)

**Preset palettes**:

| ref_no | Name | ref_no | Name |
|--------|------|--------|------|
| 1 | Iron | 9 | Rain |
| 2 | Grey | 10 | Rain10 |
| 3 | Iron10 | 11 | Blue |
| 4 | Rainbow | 12 | GlowBow |
| 5 | Grey10 | 13 | Medical |
| 6 | GreyRed | 14 | MidGreen |
| 7 | MidGrey | 15 | Prism |
| 8 | Yellow | 16 | Medical10 |

**Shutter mode**: 0=auto, 1=manual

**Color distribution types**:
- 1: TWB (high-temperature-difference balanced imaging)
- 2: Temperature-linear distribution
- 3: Signal-linear distribution (note: "linear" in the demo may map to different values; follow the device response)

**Span mode**: 1=fixed, 2=auto

**Super-resolution mode**: 0=off, 1=SR super-resolution

**Temperature unit**: 0=Celsius, 1=Kelvin, 2=Fahrenheit

**Distance unit**: 0=meters, 1=feet

### 7.9 Measurement Markers and Alarms

| Method | Description | Parameters | Returns |
|-----|------|-----|------|
| `getThermalMarkers()` | Get the measurement marker list | None | `SDKResult<JSONArray>` |
| `setThermalMarkers(json)` | Set measurement markers | json: String | `SDKResult<Unit>` |
| `getThermalAlarms()` | Get the alarm rule list | None | `SDKResult<JSONArray>` |
| `setThermalAlarms(json)` | Set alarm rules | json: String | `SDKResult<Unit>` |
| `getAlarmTrigger(type)` | Get alarm linkage configuration | type: Int (1=thermal) | `SDKResult<JSONObject>` |
| `setAlarmTrigger(type, json)` | Set alarm linkage | type: Int, json: String | `SDKResult<Unit>` |

**MarkerInfo fields**: id, type (1=point / 3=rectangle / 4=ellipse / 5=polygon / 6=polyline), name, local_params, emissivity, distance, refl_temp, points (point array), points_count

**Alarm fields**: marker_name, enabled, alarm_type, alarm_temp, alarm_delay_time, threshold_temp, threshold_delay_time, trigger_temp, trigger_delay_time

**Alarm types**:
| Value | Type | Value | Type |
|----|------|----|------|
| 1 | Max temperature > threshold | 5 | Average temperature > threshold |
| 2 | Max temperature < threshold | 6 | Average temperature < threshold |
| 3 | Min temperature > threshold | 7 | Temperature difference > threshold |
| 4 | Min temperature < threshold | 8 | Temperature difference < threshold |

### 7.10 Thermal Data Stream

| Method | Description | Parameters | Returns |
|-----|------|-----|------|
| `getThermalData(dataTypes)` | Get thermal data | dataTypes: Int (bitmask) | `SDKResult<JSONObject>` |
| `setThermalDataStreamCallback(cb)` | Set thermal data stream callback | cb: Callback | `SDKResult<Unit>` |
| `setThermalDataStreamConfig(json)` | Set data stream configuration | json: String | `SDKResult<Unit>` |

**Data type bitmask**:
- 0x01: Raw data (RAW)
- 0x02: Global data (GLOBAL)
- 0x04: Marker data (MARKER)
- 0x08: Alarm data (ALARM)

### 7.11 Event Callbacks

| Method | Description |
|-----|------|
| `setEventCallback(callback)` | Set the event callback |

**Event types**:
- 1: Network disconnected (NET_EVENT_NETWORK_DISCONNECTED)
- 2: Network resumed (NET_EVENT_NETWORK_RESUMED)
- 3: Video lost (NET_EVENT_VIDEO_LOST)

---

## 8. Demo App Walkthrough

### 8.1 UI Architecture

The demo is a single-Activity app built with **Jetpack Compose**, using a state-driven declarative UI:

```
MainActivity
├── Initialization: enableLog → init
├── State management: remember / mutableStateOf
└── Page routing (based on isConnected)
    ├── ConnectScreen (not connected)
    │   ├── SDK version display
    │   ├── Login form (IP / port / username / password)
    │   ├── Device search button
    │   └── Discovered device list (tap to select IP)
    └── DeviceScreen (connected)
        ├── TopAppBar (back / disconnect)
        ├── Video preview area (dynamic aspect ratio)
        │   ├── Video frame (Bitmap → Compose Image)
        │   ├── Resolution / FPS / bitrate display
        │   └── Bottom toolbar:
        │       ├── Start / stop preview
        │       ├── Manual calibration
        │       ├── Snapshot save
        │       ├── Flip selection
        │       ├── Palette color bar + palette picker
        │       ├── Palette invert
        │       ├── Color distribution selection
        │       └── Range selection
        ├── Feature tabs (left, 55% width)
        │   ├── Properties: device info / lens info / video streams
        │   ├── Thermal: calibration / measurement parameters / span / units
        │   ├── Display: channels / OSD / thermal OSD / image parameters
        │   ├── Time: local time / NTP configuration
        │   ├── Network: IP / FTP / ports
        │   ├── Users: user list / add-update-delete
        │   ├── Markers: measurement marker list
        │   └── Alarms: alarm rules / linkage
        └── Log panel (right, 45% width)
            └── Real-time operation results
```

### 8.2 Video Preview Implementation

Video preview is one of the demo's core features. Key steps:

1. **Set the frame listener** (DisposableEffect):
```kotlin
NetSDKManager.setStreamFrameListener(object : NativeSDK.StreamFrameListener {
    override fun onStreamFrame(handle, streamId, streamType, width, height, data, ...) {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(data))
        streamBitmap = bmp  // Update State → trigger recomposition
    }
})
```

2. **Start the stream**:
```kotlin
val firstStreamId = streams.getJSONObject(0).optInt("id")
NetSDKManager.startStream(streamId = firstStreamId, streamType = 2)  // RGBA
```

3. **FPS / bitrate statistics**:
```kotlin
LaunchedEffect(isStreaming) {
    while (isStreaming) {
        delay(1000)
        streamFps = (fpsCounter.frames * 1000 / dt).toInt()
        streamBytesPerSec = fpsCounter.bytes * 1000 / dt
        fpsCounter.frames = 0; fpsCounter.bytes = 0
    }
}
```

4. **Cleanup**:
```kotlin
onDispose {
    NetSDKManager.setStreamFrameListener(null)
    NetSDKManager.stopStream()
}
```

### 8.3 Asynchronous Operation Pattern

The demo uses Kotlin coroutines for all asynchronous work to avoid blocking the main thread:

```kotlin
fun runCmd(name: String, block: suspend () -> String) {
    scope.launch {
        val detail = try {
            withContext(Dispatchers.IO) { block() }  // Run on IO thread
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
        appendLog("$name: $detail")  // Update UI on the main thread
    }
}
```

### 8.4 Configuration Refresh Pattern

Each tab refreshes configuration with the same pattern:
1. Tap the refresh button
2. Call the SDK getter
3. Parse JSON into State variables
4. The UI recomposes automatically with the new values

Set operations:
1. Collect parameters from UI controls
2. Build a JSON string
3. Call the SDK setter
4. Write the result to the log

---

## 9. Key Data Structures and Enums

### 9.1 Error Codes

| Error code | Value | Description |
|-------|----|-----|
| NET_EC_OK | 0 | Success |
| NET_EC_FAIL | -1 | Generic failure |
| NET_EC_MEMORY | -2 | Out of memory |
| NET_EC_NULL_POINTER | -3 | Null pointer |
| NET_EC_INVALID_PARAM | -4 | Invalid parameter |
| NET_EC_NOT_SUPPORTED | -5 | Not supported |
| NET_EC_NOT_INITIALIZED | -6 | Not initialized |
| NET_EC_HTTP_ERROR | -7 | HTTP communication error |
| NET_EC_AUTH_FAILED | -8 | Authentication failed |
| NET_EC_TIMEOUT | -9 | Timeout |

### 9.2 Core Enum Summary

For detailed enum values, see [sections 7.6–7.10](#76-video-streams) and the C header [IRtekNetSDK.h](library/header/IRtekNetSDK.h).

### 9.3 JSON Data Conventions

The SDK uses **org.json** for data exchange:
- Lists use `JSONArray`
- Objects use `JSONObject`
- Numeric fields are read with `optInt()`, `optDouble()`, `optLong()`, and `optString()` to avoid null-pointer issues
- Booleans are represented as integers 0/1
- Configuration setters take JSON string arguments

---

## 10. Common Usage Flows

### 10.1 Connect a Device and Preview

```kotlin
// 1. Initialize
NetSDKManager.enableLog(true)
NetSDKManager.init()

// 2. Search for devices
val searchResult = NetSDKManager.searchDevices(3000)

// 3. Log in
val loginResult = NetSDKManager.login("192.168.10.177", 80, "admin", "admin")
if (!loginResult.isSuccess) return

// 4. Get the video stream list
val streamsResult = NetSDKManager.getStreams()
val streamId = streamsResult.data!!.getJSONObject(0).optInt("id")

// 5. Set the frame callback
NetSDKManager.setStreamFrameListener(object : NativeSDK.StreamFrameListener {
    override fun onStreamFrame(handle: Long, streamId: Int, streamType: Int,
                               width: Int, height: Int, data: ByteArray,
                               timestampUs: Long, keyFrame: Boolean) {
        // Update the Bitmap on the UI thread
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(data))
        // Display bmp...
    }
})

// 6. Start preview
NetSDKManager.startStream(streamId, 2)  // RGBA
```

### 10.2 Get / Set Thermal Parameters

```kotlin
// Get measurement parameters
val params = NetSDKManager.getThermalParams()
if (params.isSuccess) {
    val emissivity = params.data!!.optDouble("emissivity")
}

// Set measurement parameters
val json = """{
    "emissivity": 0.95,
    "distance": 1.0,
    "refl_temp": 293.15,
    "atm_temp": 293.15,
    "rel_humidity": 0.5
}"""
NetSDKManager.setThermalParams(json)
```

### 10.3 Switch Palettes

```kotlin
// Get preset palettes
val palettes = NetSDKManager.getThermalPresetPalettes()

// Set a palette (ref_no comes from the preset list)
NetSDKManager.setThermalPalette("""{"ref_no":1}""")  // Iron

// Invert the palette
val current = NetSDKManager.getThermalPalette()
val inverted = current.data!!.optInt("inverted", 0)
NetSDKManager.setThermalPalette("""{"ref_no":1,"inverted":${1 - inverted}}""")
```

### 10.4 Lifecycle Management

```kotlin
class MyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetSDKManager.enableLog(true)
        NetSDKManager.init()
    }

    override fun onDestroy() {
        super.onDestroy()
        NetSDKManager.setStreamFrameListener(null)
        NetSDKManager.stopStream()
        NetSDKManager.logout()
        NetSDKManager.cleanup()
    }
}
```

---

## 11. Build and Deployment

### 11.1 Build the Demo APK

```bash
cd IRtekNetSDK/demo/IRtekNetSDKDemo
# Windows
gradlew.bat :app:assembleDebug
# Linux/macOS
./gradlew :app:assembleDebug
```

APK output path: `app/build/outputs/apk/debug/app-debug.apk`

### 11.2 Build the SDK AAR

```bash
cd IRtekNetSDK/demo/IRtekNetSDKDemo
./gradlew :IRtekNetSDK:assembleRelease
```

AAR output path: `IRtekNetSDK/build/outputs/aar/IRtekNetSDK-release.aar`

### 11.3 Notes

1. **SDK module path**: In `settings.gradle`, the SDK module defaults to `F:/Volga.IRtekNetSDK/android`. Change this to the actual source path, or use the prebuilt AAR directly.

2. **Network security**: Android 9+ blocks cleartext HTTP by default. Set `android:usesCleartextTraffic="true"` in `AndroidManifest.xml`, or configure a network security config.

3. **Thread safety**: SDK network calls should run on an IO thread. The demo uses `Dispatchers.IO`. Callbacks may arrive off the main thread; switch to the main thread before updating the UI.

4. **Resource cleanup**: Always call `stopStream()` → `logout()` → `cleanup()` when an Activity/Fragment is destroyed, to avoid memory leaks and Native resource leaks.

5. **Device compatibility**: minSdk 24 (Android 7.0). The device must support arm64-v8a or armeabi-v7a (the SDK ships `.so` libraries for these ABIs).

6. **Snapshot permissions**: Saving with `getThermalCapture()` to `/sdcard/DCIM/` requires storage write permission (on Android 10+, use MediaStore or the app-specific directory).

---

## Appendix: Related File Index

| File | Path | Description |
|-----|------|-----|
| SDK overview | [README.md](README.md) | SDK quick start and API overview |
| C API header | [IRtekNetSDK.h](library/header/IRtekNetSDK.h) | Full Native C interface definitions |
| Demo main UI | [MainActivity.kt](demo/IRtekNetSDKDemo/app/src/main/java/com/irtek/netsdk/demo/MainActivity.kt) | Complete demo UI and business logic |
| Compose theme | [Theme.kt](demo/IRtekNetSDKDemo/app/src/main/java/com/irtek/netsdk/demo/ui/theme/Theme.kt) | Material3 theme configuration |
| App manifest | [AndroidManifest.xml](demo/IRtekNetSDKDemo/app/src/main/AndroidManifest.xml) | Permissions and component declarations |
| App build | [build.gradle](demo/IRtekNetSDKDemo/app/build.gradle) | Dependencies and compile configuration |
| Version catalog | [libs.versions.toml](demo/IRtekNetSDKDemo/gradle/libs.versions.toml) | Dependency version management |
| SDK AAR | [IRtekNetSDK-release.aar](library/aar/IRtekNetSDK-release.aar) | Prebuilt SDK library |
