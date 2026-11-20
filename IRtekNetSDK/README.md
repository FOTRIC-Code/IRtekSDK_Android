# IRtekNetSDK Android

## 项目结构（Project Structure）

```
IRtekNetSDK/
├── README.md
├── library/
│   ├── aar/                    # IRtekNetSDK.aar / .pom
│   └── header/
│       └── IRtekNetSDK.h
└── demo/
    └── IRtekNetSDKDemo/        # Android Studio Project
        ├── IRtekNetSDK/        # SDK Packaging Module（library/aar）
        └── app/                # Demo 
```

## 快速开始（Quick Start）

1. 使用 Android Studio 打开 `demo/IRtekNetSDKDemo/`（Open `demo/IRtekNetSDKDemo/` using Android Studio）
2. 同步 Gradle 依赖（Synchronize Gradle dependencies）
3. 编译运行 `app` 模块到 Android 设备（Compile and run the 'app' module to an Android device）

## 构建 Demo（Build Demo）

```bash
cd demo/IRtekNetSDKDemo
./gradlew :app:assembleDebug
```

## SDK 使用（SDK Usage）

```kotlin
NetSDKManager.enableLog(true)
NetSDKManager.init()

val result = NetSDKManager.login(
    ip = "192.168.10.177",
    port = 8000,
    userName = "admin",
    password = "admin"
)
val handle = result.getOrThrow()

val deviceInfo = NetSDKManager.getDeviceInfo(handle).getOrNull()

NetSDKManager.logout(handle)
NetSDKManager.cleanup()
```
