# IRtekNetSDK Android - Code Wiki

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 项目架构](#2-项目架构)
- [3. 目录结构](#3-目录结构)
- [4. 环境要求与依赖](#4-环境要求与依赖)
- [5. 快速开始](#5-快速开始)
- [6. 核心模块说明](#6-核心模块说明)
- [7. SDK API 详细说明](#7-sdk-api-详细说明)
- [8. Demo 应用详解](#8-demo-应用详解)
- [9. 关键数据结构与枚举](#9-关键数据结构与枚举)
- [10. 常见使用流程](#10-常见使用流程)
- [11. 构建与部署](#11-构建与部署)

---

## 1. 项目概述

**IRtekNetSDK** 是 FOTRIC 红外热像仪网络 SDK（Android 平台），提供通过网络（HTTP REST API）与红外热像设备通信的完整能力。SDK 基于 Kotlin/Java 开发，底层通过 JNI 调用 Native C/C++ 实现，支持设备发现、登录认证、视频流预览、热像参数配置、测温管理、报警联动等功能。

### 主要特性

| 特性分类 | 功能描述 |
|---------|---------|
| **设备管理** | 设备搜索、登录/注销、设备信息获取、重启/恢复出厂、固件升级 |
| **网络配置** | IP/子网/网关/DNS、NTP时间同步、FTP上传配置、服务端口配置 |
| **用户管理** | 用户增删改查、三级权限（管理员/操作员/观察者） |
| **视频流** | H.264/RGBA/YUV422/BGRA 流获取、本地录制、实时帧回调 |
| **热像设置** | 快门校正、测温量程、LUT表、测温参数、温宽模式、调色板 |
| **显示设置** | 通道管理、OSD字符叠加、图像参数（对比度/亮度/DDE）、图像翻转 |
| **测温标识** | 点/矩形/椭圆/多边形/折线测温对象管理 |
| **报警联动** | 温度报警规则、FTP上传联动 |
| **热像数据** | 原始AD数据、全局温度统计、测温对象温度、实时数据流 |

### 通信协议

- **协议**: HTTP REST API（Basic Authentication）
- **默认HTTP端口**: 80（可配置）
- **RTSP端口**: 554（可配置）
- **RTP端口**: 5004（可配置）

---

## 2. 项目架构

### 整体架构图

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
│  │  生命周期管理 / JSON序列化 / 协程封装 / 回调调度       │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────▼───────────────────────────────┐  │
│  │               NativeSDK (JNI Bridge)                 │  │
│  │   StreamFrameListener / EventCallback / JNI方法绑定  │  │
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
│                  红外热像设备 (Hardware)                     │
│            IP Camera / Thermal Sensor / Embedded OS         │
└─────────────────────────────────────────────────────────────┘
```

### 分层说明

| 层级 | 模块 | 职责 |
|-----|------|-----|
| **应用层** | Demo App | 展示SDK功能的参考实现，使用Jetpack Compose构建UI |
| **Kotlin API层** | NetSDKManager | 提供Kotlin友好的API，封装JSON序列化、协程调度、错误处理 |
| **JNI桥接层** | NativeSDK | JNI接口定义，Native回调到Java层的Listener接口 |
| **Native层** | libIRtekNetSDK.so | C/C++实现的核心功能：HTTP通信、RTSP流解析、热像数据处理 |

---

## 3. 目录结构

```
IRtekNetSDK/
├── README.md                          # SDK 快速说明
├── library/                           # SDK 发布产物
│   ├── aar/
│   │   └── IRtekNetSDK-release.aar    # Android AAR 库
│   └── header/
│       └── IRtekNetSDK.h              # C API 头文件（完整接口参考）
└── demo/
    └── IRtekNetSDKDemo/               # Android Studio Demo 工程
        ├── build.gradle               # 根构建脚本
        ├── settings.gradle            # 模块配置
        ├── gradle.properties          # Gradle 属性
        ├── gradle/
        │   ├── libs.versions.toml     # 版本目录（依赖版本管理）
        │   └── wrapper/
        │       ├── gradle-wrapper.jar
        │       └── gradle-wrapper.properties
        ├── app/                       # Demo 应用模块
        │   ├── build.gradle           # 应用构建配置
        │   ├── proguard-rules.pro     # ProGuard 规则
        │   └── src/main/
        │       ├── AndroidManifest.xml
        │       ├── java/com/irtek/netsdk/demo/
        │       │   ├── MainActivity.kt         # 主Activity（所有UI和逻辑）
        │       │   └── ui/theme/
        │       │       └── Theme.kt            # Compose 主题配置
        │       └── res/                        # 资源文件
        │           ├── drawable/               # 图标资源
        │           ├── mipmap-*/               # 启动图标
        │           └── values/                 # 字符串/主题/颜色
        └── IRtekNetSDK/                        # SDK 库模块（外部源码路径）
```

---

## 4. 环境要求与依赖

### 开发环境

| 工具 | 版本要求 |
|-----|---------|
| Android Studio | Iguana 或更高版本（推荐最新版） |
| Gradle | 8.7+（Wrapper 已配置） |
| JDK | 11（compileOptions 配置） |
| Android SDK | compileSdk 36, minSdk 24, targetSdk 36 |
| Kotlin | 2.2.0 |

### Android 权限

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

此外需要在 `application` 标签中启用明文传输（HTTP）：

```xml
android:usesCleartextTraffic="true"
```

### 第三方依赖

| 依赖 | 版本 | 用途 |
|-----|------|-----|
| AndroidX Core KTX | 1.10.1 | Android KTX 扩展 |
| AndroidX Lifecycle Runtime | 2.6.1 | Lifecycle 感知组件 |
| AndroidX Activity Compose | 1.8.0 | Compose Activity 支持 |
| Kotlin Coroutines Android | 1.7.3 | 异步协程支持 |
| Jetpack Compose BOM | 2024.09.00 | Compose UI 框架 |
| Compose Material3 | - | Material3 组件 |
| Compose Material Icons Extended | - | 扩展图标集 |
| OkHttp | 4.12.0 | HTTP 客户端（SDK内部使用） |
| Gson | 2.11.0 | JSON 序列化（SDK内部使用） |

---

## 5. 快速开始

### 5.1 集成 AAR 到现有项目

1. 将 `IRtekNetSDK-release.aar` 复制到应用模块的 `libs/` 目录
2. 在 `build.gradle` 中添加依赖：

```groovy
dependencies {
    implementation files('libs/IRtekNetSDK-release.aar')
    // 或使用 fileTree
    implementation fileTree(dir: 'libs', include: ['*.aar', '*.jar'])
}
```

3. 同步 Gradle

### 5.2 基本使用流程

```kotlin
// 1. 初始化 SDK
NetSDKManager.enableLog(true)
NetSDKManager.init()

// 2. 登录设备
val result = NetSDKManager.login(
    ip = "192.168.10.177",
    port = 80,
    userName = "admin",
    password = "admin"
)
if (result.isSuccess) {
    // 登录成功，result.data 为设备句柄（内部管理）
}

// 3. 搜索局域网设备
val searchResult = NetSDKManager.searchDevices(timeoutMs = 3000)
if (searchResult.isSuccess) {
    val devices = searchResult.data  // JSONArray
}

// 4. 获取设备信息
val deviceInfo = NetSDKManager.getDeviceInfo()

// 5. 开始视频流预览
NetSDKManager.setStreamFrameListener(object : NativeSDK.StreamFrameListener {
    override fun onStreamFrame(handle: Long, streamId: Int, streamType: Int,
                               width: Int, height: Int, data: ByteArray,
                               timestampUs: Long, keyFrame: Boolean) {
        // 将 data 转为 Bitmap 显示
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(data))
    }
})
NetSDKManager.startStream(streamId = firstStreamId, streamType = 2)  // 2 = RGBA

// 6. 注销并清理
NetSDKManager.stopStream()
NetSDKManager.logout()
NetSDKManager.cleanup()
```

### 5.3 运行 Demo

1. 使用 Android Studio 打开 `IRtekNetSDK/demo/IRtekNetSDKDemo/`
2. 将 Demo 工程中的 SDK 模块路径配置正确（`settings.gradle` 中的 `projectDir`）
3. 连接 Android 设备（确保设备与热像仪在同一局域网）
4. 点击运行按钮部署到设备
5. 默认连接参数：IP `192.168.10.177`，端口 `80`，用户名/密码 `admin/admin`

---

## 6. 核心模块说明

### 6.1 NetSDKManager (Kotlin API 入口)

**包名**: `com.irtek.netsdk.NetSDKManager`

这是 SDK 的核心管理类，提供所有 Kotlin 层 API 接口。采用单例模式设计，所有方法都是静态/伴生对象方法。

**核心职责**:
- SDK 生命周期管理（init/cleanup）
- 设备登录/注销与会话管理
- 设备发现（局域网搜索）
- 所有业务 API 的 Kotlin 封装
- 回调监听器管理
- JSON 序列化/反序列化
- 协程调度（IO线程执行，主线程回调）

**返回值类型**:
所有 API 调用返回 `SDKResult<T>` 类型：
```kotlin
data class SDKResult<T>(
    val isSuccess: Boolean,
    val code: Int,           // 0 表示成功，负数表示错误码
    val message: String?,    // 错误信息
    val data: T?             // 返回数据（JSONObject/JSONArray/Int/Long等）
)
```

### 6.2 NativeSDK (JNI 桥接层)

**包名**: `com.irtek.netsdk.NativeSDK`

提供 JNI 层的接口定义和回调 Listener：

**关键接口**:
```kotlin
interface StreamFrameListener {
    fun onStreamFrame(
        handle: Long,        // 设备句柄
        streamId: Int,       // 流ID
        streamType: Int,     // 流类型 (1=H264, 2=RGBA, 3=YUV422, 4=BGRA)
        width: Int,          // 图像宽度
        height: Int,         // 图像高度
        data: ByteArray,     // 图像数据
        timestampUs: Long,   // 时间戳（微秒）
        keyFrame: Boolean    // 是否关键帧
    )
}

interface EventCallback {
    fun onEvent(eventType: Int, streamId: Int, message: String)
}
```

### 6.3 Demo 应用模块

**包名**: `com.irtek.netsdk.demo`

Demo 应用是一个完整的功能展示程序，使用 Jetpack Compose 构建现代化 UI。

**主要组件**:

| 组件 | 文件 | 职责 |
|-----|------|-----|
| MainActivity | [MainActivity.kt](file:///c:/Users/FOTRIC/.trae-cn/worktrees/Volga.IRtekSDK_Android/feat-irtek-net-sdk-demo-wiki-IzpIcg/IRtekNetSDK/demo/IRtekNetSDKDemo/app/src/main/java/com/irtek/netsdk/demo/MainActivity.kt) | 主Activity，包含所有UI和业务逻辑 |
| ConnectScreen | MainActivity.kt L89-L279 | 设备连接页面：登录表单、设备搜索列表 |
| DeviceScreen | MainActivity.kt L285-L925 | 设备主页面：视频预览、功能标签页、日志 |
| TabProperties | MainActivity.kt L977-L1000 | 属性标签页：设备信息、镜头信息、视频流 |
| TabThermal | MainActivity.kt L1005-L1150 | 热像仪标签页：校正、测温参数、温宽、单位 |
| TabDisplay | MainActivity.kt L1590-L1778 | 显示标签页：通道、OSD叠加、图像参数 |
| TabTime | MainActivity.kt L1280-L1396 | 时间标签页：时间模式、NTP配置 |
| TabNetwork | MainActivity.kt L1155-L1275 | 网络标签页：IP配置、FTP、端口 |
| TabUsers | MainActivity.kt L1401-L1501 | 用户标签页：用户列表、增删改 |
| TabMarkers | MainActivity.kt L1783-L1851 | 标记标签页：测温标记列表 |
| TabAlarms | MainActivity.kt L1856-L1929 | 报警标签页：报警规则、联动设置 |
| IRtekNetSDKDemoTheme | [Theme.kt](file:///c:/Users/FOTRIC/.trae-cn/worktrees/Volga.IRtekSDK_Android/feat-irtek-net-sdk-demo-wiki-IzpIcg/IRtekNetSDK/demo/IRtekNetSDKDemo/app/src/main/java/com/irtek/netsdk/demo/ui/theme/Theme.kt) | Compose 主题配置 |

---

## 7. SDK API 详细说明

### 7.1 SDK 生命周期

| 方法 | 说明 | 参数 | 返回 |
|-----|------|-----|------|
| `init()` | 初始化SDK | 无 | `SDKResult<Unit>` |
| `cleanup()` | 释放SDK资源 | 无 | `SDKResult<Unit>` |
| `enableLog(enable: Boolean)` | 启用/禁用日志 | enable: 是否启用 | `SDKResult<Unit>` |
| `getSdkVersion()` | 获取SDK版本 | 无 | `SDKResult<JSONObject>` (major/minor/patch/build) |

### 7.2 设备连接管理

| 方法 | 说明 | 参数 | 返回 |
|-----|------|-----|------|
| `login(ip, port, user, pwd)` | 登录设备 | ip: String, port: Int, userName: String, password: String | `SDKResult<Long>` (handle) |
| `logout()` | 注销当前设备 | 无 | `SDKResult<Unit>` |
| `searchDevices(timeoutMs)` | 搜索局域网设备 | timeoutMs: Int (超时毫秒) | `SDKResult<JSONArray>` |

**设备搜索返回字段**:
```json
{
  "ip": "192.168.10.177",
  "name": "设备名称",
  "model": "设备型号",
  "serial_no": "序列号",
  "id": "设备ID",
  "firmware_version": "固件版本"
}
```

### 7.3 设备管理

| 方法 | 说明 | 参数 | 返回 |
|-----|------|-----|------|
| `getDeviceInfo()` | 获取设备信息 | 无 | `SDKResult<JSONObject>` |
| `setDeviceInfo(json)` | 设置设备信息 | json: String (JSON) | `SDKResult<Unit>` |
| `deviceRestart()` | 重启设备 | 无 | `SDKResult<Unit>` |
| `deviceReset()` | 恢复出厂设置 | 无 | `SDKResult<Unit>` |
| `deviceUpgrade(json)` | 远程固件升级 | json: String (本地路径) | `SDKResult<Unit>` |
| `getTime()` | 获取设备时间 | 无 | `SDKResult<JSONObject>` |
| `setTime(json)` | 设置设备时间 | json: String | `SDKResult<Unit>` |
| `getPort()` | 获取服务端口 | 无 | `SDKResult<JSONObject>` |
| `setPort(json)` | 设置服务端口 | json: String | `SDKResult<Unit>` |

**DeviceInfo 字段**: name, model, spec, serial_no, id, brand, company, firmware_name, firmware_version

**PortInfo 字段**: http_port_no, rtsp_port_no, rtp_port_no

**TimeInfo 字段**: time_mode (0=本地/1=NTP), local_time, time_zone

### 7.4 网络配置

| 方法 | 说明 | 参数 | 返回 |
|-----|------|-----|------|
| `getNetworks()` | 获取网络配置列表 | 无 | `SDKResult<JSONArray>` |
| `getNetwork(id)` | 获取指定网络配置 | id: Int | `SDKResult<JSONObject>` |
| `setNetwork(id, json)` | 设置网络配置 | id: Int, json: String | `SDKResult<Unit>` |
| `getNtpServers()` | 获取NTP服务器列表 | 无 | `SDKResult<JSONArray>` |
| `getNtpServer(id)` | 获取指定NTP服务器 | id: Int | `SDKResult<JSONObject>` |
| `setNtpServer(id, json)` | 设置NTP服务器 | id: Int, json: String | `SDKResult<Unit>` |
| `getFtp()` | 获取FTP配置 | 无 | `SDKResult<JSONObject>` |
| `setFtp(json)` | 设置FTP配置 | json: String | `SDKResult<Unit>` |

**NetworkInfo 字段**: id, name, dhcp (0/1), ip, netmask, dns (Array), gateway (Array)

**NtpInfo 字段**: id, host_name, port_no, syn_interval

**FtpInfo 字段**: use_ssl, ip, port_no, user_name, pwd, enable_annoy, path_template

### 7.5 用户管理

| 方法 | 说明 | 参数 | 返回 |
|-----|------|-----|------|
| `getUsers()` | 获取用户列表 | 无 | `SDKResult<JSONArray>` |
| `getUser(id)` | 获取指定用户 | id: Int | `SDKResult<JSONObject>` |
| `updateUser(json)` | 添加/修改用户 | json: String | `SDKResult<Unit>` |
| `deleteUser(id)` | 删除用户 | id: Int | `SDKResult<Unit>` |

**UserInfo 字段**: id, user_level (0=ROOT/1=OPERATOR/2=VIEWER), user_name

**权限级别说明**:
- **ROOT (0)**: 管理员 - 系统级操作、用户管理、网络配置、固件升级、重启/重置
- **OPERATOR (1)**: 操作员 - 日常调优、图像/视频/热像参数设置
- **VIEWER (2)**: 观察者 - 只读，仅能查看状态和数据

### 7.6 视频流

| 方法 | 说明 | 参数 | 返回 |
|-----|------|-----|------|
| `getStreams()` | 获取视频流列表 | 无 | `SDKResult<JSONArray>` |
| `getStream(id)` | 获取指定流参数 | id: Int | `SDKResult<JSONObject>` |
| `getStreamStatus(id)` | 获取流连接状态 | id: Int | `SDKResult<JSONObject>` |
| `startStream(streamId, streamType)` | 开始接收视频流 | streamId: Int, streamType: Int (2=RGBA推荐) | `SDKResult<Unit>` |
| `stopStream()` | 停止视频流 | 无 | `SDKResult<Unit>` |
| `setStreamFrameListener(listener)` | 设置帧回调 | listener: StreamFrameListener? | Unit |
| `startStreamRecord(id, path)` | 开始本地录制 | id: Int, path: String | `SDKResult<Unit>` |
| `stopStreamRecord(id)` | 停止本地录制 | id: Int | `SDKResult<Unit>` |

**StreamInfo 字段**: id, bit_rate, codec_type, disable_skip, enable_rc, gop_size, h264_profile, initial_qp, max_frame_rate, maximum_qp, quality_control_type, resolution_width, resolution_height

**流类型**:
- 1: H.264
- 2: RGBA（推荐用于预览，可直接转Bitmap）
- 3: YUV422
- 4: BGRA

### 7.7 通道与显示设置

| 方法 | 说明 | 参数 | 返回 |
|-----|------|-----|------|
| `getChannels()` | 获取通道列表 | 无 | `SDKResult<JSONArray>` |
| `getChannel(id)` | 获取通道信息 | id: Int | `SDKResult<JSONObject>` |
| `setChannel(id, json)` | 设置通道信息 | id: Int, json: String | `SDKResult<Unit>` |
| `getOverlay(channelId)` | 获取OSD叠加 | channelId: Int | `SDKResult<JSONObject>` |
| `setOverlay(channelId, json)` | 设置OSD叠加 | channelId: Int, json: String | `SDKResult<Unit>` |
| `getImageParams(channelId)` | 获取图像参数 | channelId: Int | `SDKResult<JSONObject>` |
| `setImageParams(channelId, json)` | 设置图像参数 | channelId: Int, json: String | `SDKResult<Unit>` |
| `getImageFlip(channelId)` | 获取翻转模式 | channelId: Int | `SDKResult<Int>` |
| `setImageFlip(channelId, mode)` | 设置翻转模式 | channelId: Int, mode: Int | `SDKResult<Unit>` |

**ImageParams 字段**: contrast, brightness, dde_enable (0/1), dde_value

**Overlay 结构**:
```json
{
  "channel_name_overlay": { "enabled": 0/1, "align": 0-3, "mode": 0/1, "x": 0, "y": 0 },
  "time_overlay": { "enabled": 0/1, "align": 0-3, "mode": 0/1, "x": 0, "y": 0 }
}
```

**对齐方式**: 0=左上, 1=右上, 2=左下, 3=右下
**显示模式**: 0=常亮, 1=闪烁
**翻转模式**: 0=不翻转, 1=上下翻转, 2=左右翻转, 3=中心翻转

### 7.8 热像设置

| 方法 | 说明 | 参数 | 返回 |
|-----|------|-----|------|
| `getThermalLensInfo()` | 获取镜头信息 | 无 | `SDKResult<JSONObject>` |
| `thermalCalibrate()` | 执行热像校正（快门） | 无 | `SDKResult<Unit>` |
| `getThermalShutterMode()` | 获取快门模式 | 无 | `SDKResult<Int>` |
| `setThermalShutterMode(mode)` | 设置快门模式 | mode: Int | `SDKResult<Unit>` |
| `getThermalShutterTime()` | 获取自动校正间隔 | 无 | `SDKResult<Int>` (分钟) |
| `setThermalShutterTime(minutes)` | 设置自动校正间隔 | minutes: Int | `SDKResult<Unit>` |
| `getThermalResolution()` | 获取热像分辨率 | 无 | `SDKResult<JSONObject>` |
| `getThermalRange()` | 获取测温量程 | 无 | `SDKResult<Int>` (rangeId) |
| `setThermalRange(id)` | 设置测温量程 | id: Int | `SDKResult<Unit>` |
| `getThermalLut()` | 获取LUT表 | 无 | `SDKResult<JSONObject>` |
| `getThermalParams()` | 获取测温参数 | 无 | `SDKResult<JSONObject>` |
| `setThermalParams(json)` | 设置测温参数 | json: String | `SDKResult<Unit>` |
| `getThermalColorDist()` | 获取色彩分布类型 | 无 | `SDKResult<Int>` |
| `setThermalColorDist(type)` | 设置色彩分布类型 | type: Int | `SDKResult<Unit>` |
| `getThermalGain()` | 获取温宽模式 | 无 | `SDKResult<JSONObject>` |
| `setThermalGain(json)` | 设置温宽模式 | json: String | `SDKResult<Unit>` |
| `getThermalPresetPalettes()` | 获取预置调色板列表 | 无 | `SDKResult<JSONArray>` |
| `getThermalPalette()` | 获取当前调色板 | 无 | `SDKResult<JSONObject>` |
| `setThermalPalette(json)` | 设置调色板 | json: String | `SDKResult<Unit>` |
| `getThermalOverlay()` | 获取热像OSD | 无 | `SDKResult<JSONObject>` |
| `setThermalOverlay(json)` | 设置热像OSD | json: String | `SDKResult<Unit>` |
| `getThermalUnit()` | 获取单位设置 | 无 | `SDKResult<JSONObject>` |
| `setThermalUnit(json)` | 设置单位 | json: String | `SDKResult<Unit>` |
| `getThermalCapture(mode, path)` | 热像抓拍 | mode: Int (0=纯/1=IRtek), path: String | `SDKResult<Unit>` |
| `getThermalSr()` | 获取超分辨率模式 | 无 | `SDKResult<Int>` |
| `setThermalSr(mode)` | 设置超分辨率模式 | mode: Int | `SDKResult<Unit>` |
| `setThermalDpc(json)` | 设置坏点校正 | json: String | `SDKResult<Unit>` |

**LensInfo 字段**: name, focal (焦距mm), hfov (水平视场角), vfov (垂直视场角), band (波段), range_map (量程映射数组)

**ThermalParams 字段**: emissivity (发射率), distance (距离m), refl_temp (反射温度K), atm_temp (大气温度K), rel_humidity (相对湿度0-1)

**Gain 字段**: mode (1=固定/2=自动), max, min, fixedSpan (0/1), span

**Palette 字段**: ref_no (编号), colors (IntArray), inverted (0/1)

**预置调色板对照**:

| ref_no | 名称 | ref_no | 名称 |
|--------|------|--------|------|
| 1 | Iron | 9 | Rain |
| 2 | Grey | 10 | Rain10 |
| 3 | Iron10 | 11 | Blue |
| 4 | Rainbow | 12 | GlowBow |
| 5 | Grey10 | 13 | Medical |
| 6 | GreyRed | 14 | MidGreen |
| 7 | MidGrey | 15 | Prism |
| 8 | Yellow | 16 | Medical10 |

**快门模式**: 0=自动, 1=手动

**色彩分布类型**:
- 1: TWB (高温差均衡成像分布)
- 2: 温度线性分布
- 3: 信号线性分布（注：Demo中"线性"对应不同值，请以设备返回为准）

**温宽模式**: 1=固定模式, 2=自动模式

**超分辨率模式**: 0=关闭, 1=SR超分辨率

**温度单位**: 0=摄氏度, 1=开尔文, 2=华氏度

**距离单位**: 0=米, 1=英尺

### 7.9 测温标识与报警

| 方法 | 说明 | 参数 | 返回 |
|-----|------|-----|------|
| `getThermalMarkers()` | 获取测温标记列表 | 无 | `SDKResult<JSONArray>` |
| `setThermalMarkers(json)` | 设置测温标记 | json: String | `SDKResult<Unit>` |
| `getThermalAlarms()` | 获取报警规则列表 | 无 | `SDKResult<JSONArray>` |
| `setThermalAlarms(json)` | 设置报警规则 | json: String | `SDKResult<Unit>` |
| `getAlarmTrigger(type)` | 获取报警联动配置 | type: Int (1=热像) | `SDKResult<JSONObject>` |
| `setAlarmTrigger(type, json)` | 设置报警联动 | type: Int, json: String | `SDKResult<Unit>` |

**MarkerInfo 字段**: id, type (1=点/3=矩形/4=椭圆/5=多边形/6=折线), name, local_params, emissivity, distance, refl_temp, points (点数组), points_count

**Alarm 字段**: marker_name, enabled, alarm_type, alarm_temp, alarm_delay_time, threshold_temp, threshold_delay_time, trigger_temp, trigger_delay_time

**报警类型**:
| 值 | 类型 | 值 | 类型 |
|----|------|----|------|
| 1 | 最高温>阈值 | 5 | 平均温>阈值 |
| 2 | 最高温<阈值 | 6 | 平均温<阈值 |
| 3 | 最低温>阈值 | 7 | 温差>阈值 |
| 4 | 最低温<阈值 | 8 | 温差<阈值 |

### 7.10 热像数据流

| 方法 | 说明 | 参数 | 返回 |
|-----|------|-----|------|
| `getThermalData(dataTypes)` | 获取热像数据 | dataTypes: Int (位掩码) | `SDKResult<JSONObject>` |
| `setThermalDataStreamCallback(cb)` | 设置热像数据流回调 | cb: Callback | `SDKResult<Unit>` |
| `setThermalDataStreamConfig(json)` | 设置数据流配置 | json: String | `SDKResult<Unit>` |

**数据类型位掩码**:
- 0x01: 原始数据 (RAW)
- 0x02: 全局数据 (GLOBAL)
- 0x04: 标记数据 (MARKER)
- 0x08: 报警数据 (ALARM)

### 7.11 事件回调

| 方法 | 说明 |
|-----|------|
| `setEventCallback(callback)` | 设置事件回调 |

**事件类型**:
- 1: 网络断开 (NET_EVENT_NETWORK_DISCONNECTED)
- 2: 网络恢复 (NET_EVENT_NETWORK_RESUMED)
- 3: 视频丢失 (NET_EVENT_VIDEO_LOST)

---

## 8. Demo 应用详解

### 8.1 UI 架构

Demo 使用 **Jetpack Compose** 构建单 Activity 应用，采用状态驱动的声明式 UI：

```
MainActivity
├── 初始化: enableLog → init
├── 状态管理: remember / mutableStateOf
└── 页面路由 (基于 isConnected 状态)
    ├── ConnectScreen (未连接)
    │   ├── SDK版本显示
    │   ├── 登录表单 (IP/端口/用户名/密码)
    │   ├── 设备搜索按钮
    │   └── 发现设备列表（可点击选择IP）
    └── DeviceScreen (已连接)
        ├── TopAppBar (返回/断开按钮)
        ├── 视频预览区（动态宽高比）
        │   ├── 视频画面 (Bitmap → Compose Image)
        │   ├── 分辨率/FPS/码率显示
        │   └── 底部工具栏:
        │       ├── 开始/停止 预览
        │       ├── 手动校正
        │       ├── 截图保存
        │       ├── 翻转选择
        │       ├── 调色板色条 + 调色板选择
        │       ├── 调色板反转
        │       ├── 色彩分布选择
        │       └── 量程选择
        ├── 功能标签页（左侧，55%宽度）
        │   ├── 属性: 设备信息/镜头信息/视频流
        │   ├── 热像仪: 校正/测温参数/温宽/单位
        │   ├── 显示: 通道/OSD/热像OSD/图像参数
        │   ├── 时间: 本地时间/NTP配置
        │   ├── 网络: IP/FTP/端口
        │   ├── 用户: 用户列表/增删改
        │   ├── 标记: 测温标记列表
        │   └── 报警: 报警规则/联动
        └── 日志面板（右侧，45%宽度）
            └── 操作结果实时输出
```

### 8.2 视频预览实现

视频预览是 Demo 的核心功能之一，关键步骤：

1. **设置帧监听器** (DisposableEffect):
```kotlin
NetSDKManager.setStreamFrameListener(object : NativeSDK.StreamFrameListener {
    override fun onStreamFrame(handle, streamId, streamType, width, height, data, ...) {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(data))
        streamBitmap = bmp  // 更新State → 触发重组
    }
})
```

2. **开始流**:
```kotlin
val firstStreamId = streams.getJSONObject(0).optInt("id")
NetSDKManager.startStream(streamId = firstStreamId, streamType = 2)  // RGBA
```

3. **FPS/码率统计**:
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

4. **清理**:
```kotlin
onDispose {
    NetSDKManager.setStreamFrameListener(null)
    NetSDKManager.stopStream()
}
```

### 8.3 异步操作模式

Demo 使用 Kotlin 协程处理所有异步操作，避免阻塞主线程：

```kotlin
fun runCmd(name: String, block: suspend () -> String) {
    scope.launch {
        val detail = try {
            withContext(Dispatchers.IO) { block() }  // IO线程执行
        } catch (e: Exception) {
            "异常: ${e.message}"
        }
        appendLog("$name: $detail")  // 主线程更新UI
    }
}
```

### 8.4 配置刷新模式

每个标签页的配置刷新遵循统一模式：
1. 点击刷新按钮
2. 调用 SDK 获取接口
3. 解析 JSON 到各 State 变量
4. UI 自动重组显示新值

设置操作则：
1. 从 UI 控件收集参数
2. 构建 JSON 字符串
3. 调用 SDK 设置接口
4. 结果输出到日志

---

## 9. 关键数据结构与枚举

### 9.1 错误码

| 错误码 | 值 | 说明 |
|-------|----|-----|
| NET_EC_OK | 0 | 成功 |
| NET_EC_FAIL | -1 | 通用失败 |
| NET_EC_MEMORY | -2 | 内存不足 |
| NET_EC_NULL_POINTER | -3 | 空指针 |
| NET_EC_INVALID_PARAM | -4 | 参数无效 |
| NET_EC_NOT_SUPPORTED | -5 | 不支持 |
| NET_EC_NOT_INITIALIZED | -6 | 未初始化 |
| NET_EC_HTTP_ERROR | -7 | HTTP 通信错误 |
| NET_EC_AUTH_FAILED | -8 | 认证失败 |
| NET_EC_TIMEOUT | -9 | 超时 |

### 9.2 核心枚举汇总

详细枚举值参见 [7.6-7.10节](#76-视频流) 和 C 头文件 [IRtekNetSDK.h](file:///c:/Users/FOTRIC/.trae-cn/worktrees/Volga.IRtekSDK_Android/feat-irtek-net-sdk-demo-wiki-IzpIcg/IRtekNetSDK/library/header/IRtekNetSDK.h)。

### 9.3 JSON 数据约定

SDK 使用 **org.json** 进行数据交换：
- 列表数据使用 `JSONArray`
- 对象数据使用 `JSONObject`
- 数值字段使用 `optInt()`, `optDouble()`, `optLong()`, `optString()` 读取，避免空指针
- 布尔值使用整数 0/1 表示
- 配置设置接口接收 JSON 字符串参数

---

## 10. 常见使用流程

### 10.1 连接设备并预览

```kotlin
// 1. 初始化
NetSDKManager.enableLog(true)
NetSDKManager.init()

// 2. 搜索设备
val searchResult = NetSDKManager.searchDevices(3000)

// 3. 登录
val loginResult = NetSDKManager.login("192.168.10.177", 80, "admin", "admin")
if (!loginResult.isSuccess) return

// 4. 获取视频流列表
val streamsResult = NetSDKManager.getStreams()
val streamId = streamsResult.data!!.getJSONObject(0).optInt("id")

// 5. 设置帧回调
NetSDKManager.setStreamFrameListener(object : NativeSDK.StreamFrameListener {
    override fun onStreamFrame(handle: Long, streamId: Int, streamType: Int,
                               width: Int, height: Int, data: ByteArray,
                               timestampUs: Long, keyFrame: Boolean) {
        // 在UI线程更新Bitmap
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(data))
        // 显示bmp...
    }
})

// 6. 开始预览
NetSDKManager.startStream(streamId, 2)  // RGBA
```

### 10.2 获取/设置热像参数

```kotlin
// 获取测温参数
val params = NetSDKManager.getThermalParams()
if (params.isSuccess) {
    val emissivity = params.data!!.optDouble("emissivity")
}

// 设置测温参数
val json = """{
    "emissivity": 0.95,
    "distance": 1.0,
    "refl_temp": 293.15,
    "atm_temp": 293.15,
    "rel_humidity": 0.5
}"""
NetSDKManager.setThermalParams(json)
```

### 10.3 切换调色板

```kotlin
// 获取预置调色板
val palettes = NetSDKManager.getThermalPresetPalettes()

// 设置调色板 (ref_no从预置列表中获取)
NetSDKManager.setThermalPalette("""{"ref_no":1}""")  // Iron

// 反转调色板
val current = NetSDKManager.getThermalPalette()
val inverted = current.data!!.optInt("inverted", 0)
NetSDKManager.setThermalPalette("""{"ref_no":1,"inverted":${1 - inverted}}""")
```

### 10.4 生命周期管理

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

## 11. 构建与部署

### 11.1 构建 Demo APK

```bash
cd IRtekNetSDK/demo/IRtekNetSDKDemo
# Windows
gradlew.bat :app:assembleDebug
# Linux/macOS
./gradlew :app:assembleDebug
```

APK 输出路径: `app/build/outputs/apk/debug/app-debug.apk`

### 11.2 构建 SDK AAR

```bash
cd IRtekNetSDK/demo/IRtekNetSDKDemo
./gradlew :IRtekNetSDK:assembleRelease
```

AAR 输出路径: `IRtekNetSDK/build/outputs/aar/IRtekNetSDK-release.aar`

### 11.3 注意事项

1. **SDK 模块路径**: `settings.gradle` 中 SDK 模块默认指向 `F:/Volga.IRtekNetSDK/android`，需要根据实际源码路径修改，或直接使用预编译的 AAR。

2. **网络安全配置**: Android 9+ 默认禁止明文 HTTP 通信，必须在 `AndroidManifest.xml` 中设置 `android:usesCleartextTraffic="true"`，或配置 network security config。

3. **线程安全**: SDK 的网络调用应在 IO 线程执行，Demo 使用 `Dispatchers.IO` 调度。回调可能来自非主线程，更新 UI 需切到主线程。

4. **资源释放**: 务必在 Activity/Fragment 销毁时调用 `stopStream()` → `logout()` → `cleanup()`，避免内存泄漏和Native资源占用。

5. **设备兼容性**: minSdk 24 (Android 7.0)，设备需支持 arm64-v8a 或 armeabi-v7a 架构（SDK包含对应ABI的so库）。

6. **截图权限**: `getThermalCapture()` 保存到 `/sdcard/DCIM/` 需要写存储权限（Android 10+ 可使用 MediaStore 或应用专属目录）。

---

## 附录：相关文件索引

| 文件 | 路径 | 说明 |
|-----|------|-----|
| SDK 说明 | [README.md](file:///c:/Users/FOTRIC/.trae-cn/worktrees/Volga.IRtekSDK_Android/feat-irtek-net-sdk-demo-wiki-IzpIcg/IRtekNetSDK/README.md) | SDK 快速开始和接口概览 |
| C API 头文件 | [IRtekNetSDK.h](file:///c:/Users/FOTRIC/.trae-cn/worktrees/Volga.IRtekSDK_Android/feat-irtek-net-sdk-demo-wiki-IzpIcg/IRtekNetSDK/library/header/IRtekNetSDK.h) | Native C 接口完整定义 |
| Demo 主界面 | [MainActivity.kt](file:///c:/Users/FOTRIC/.trae-cn/worktrees/Volga.IRtekSDK_Android/feat-irtek-net-sdk-demo-wiki-IzpIcg/IRtekNetSDK/demo/IRtekNetSDKDemo/app/src/main/java/com/irtek/netsdk/demo/MainActivity.kt) | Demo 完整 UI 与业务逻辑 |
| Compose 主题 | [Theme.kt](file:///c:/Users/FOTRIC/.trae-cn/worktrees/Volga.IRtekSDK_Android/feat-irtek-net-sdk-demo-wiki-IzpIcg/IRtekNetSDK/demo/IRtekNetSDKDemo/app/src/main/java/com/irtek/netsdk/demo/ui/theme/Theme.kt) | Material3 主题配置 |
| 应用配置 | [AndroidManifest.xml](file:///c:/Users/FOTRIC/.trae-cn/worktrees/Volga.IRtekSDK_Android/feat-irtek-net-sdk-demo-wiki-IzpIcg/IRtekNetSDK/demo/IRtekNetSDKDemo/app/src/main/AndroidManifest.xml) | 权限与组件声明 |
| 应用构建 | [build.gradle](file:///c:/Users/FOTRIC/.trae-cn/worktrees/Volga.IRtekSDK_Android/feat-irtek-net-sdk-demo-wiki-IzpIcg/IRtekNetSDK/demo/IRtekNetSDKDemo/app/build.gradle) | 依赖与编译配置 |
| 版本目录 | [libs.versions.toml](file:///c:/Users/FOTRIC/.trae-cn/worktrees/Volga.IRtekSDK_Android/feat-irtek-net-sdk-demo-wiki-IzpIcg/IRtekNetSDK/demo/IRtekNetSDKDemo/gradle/libs.versions.toml) | 依赖版本管理 |
| SDK AAR | [IRtekNetSDK-release.aar](file:///c:/Users/FOTRIC/.trae-cn/worktrees/Volga.IRtekSDK_Android/feat-irtek-net-sdk-demo-wiki-IzpIcg/IRtekNetSDK/library/aar/IRtekNetSDK-release.aar) | 预编译 SDK 库 |
