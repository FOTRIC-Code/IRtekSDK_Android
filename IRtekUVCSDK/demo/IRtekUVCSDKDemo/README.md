# UVC Android SDK Demo

这是一个集成 **IRtekUVCSDK** 的 Android 示例项目，演示如何在 Android 应用中使用 UVC SDK。

## 项目结构

```
app/
├── src/main/
│   ├── cpp/
│   │   ├── CMakeLists.txt          # NDK 构建配置
│   │   └── uvc_sdk_jni.cpp         # JNI 包装层（C++）
│   ├── java/com/fotric/cardinal/uvc/
│   │   ├── UVCManager.kt           # UVC SDK 管理器（Kotlin）
│   │   ├── MainActivity.kt         # 主界面（演示用法）
│   │   └── ...
│   └── AndroidManifest.xml         # 已添加 USB 权限
└── build.gradle                    # 已配置 NDK + CMake
```

## 依赖关系

项目依赖上级目录的 SDK 源码：
- `../../IRtekUVCSDK/` - UVC SDK C++ 源码
- `../../libusb/` - libusb 源码
- `../../android/config.h` - libusb Android 配置

## 构建说明

### 前置条件

1. **Android Studio**（已安装 NDK 和 CMake）
   - SDK Manager → SDK Tools → 勾选 "NDK" 和 "CMake"
   - 推荐 NDK 版本：r21+（项目使用 r25+）

2. **项目路径**
   - 确保 `UVCAndroidSDKDEMO` 位于 `android/` 目录下
   - 与 `IRtekUVCSDK`、`libusb` 目录同级

### 构建步骤

1. **打开项目**
   ```bash
   # 在 Android Studio 中打开
   android/UVCAndroidSDKDEMO/
   ```

2. **同步 Gradle**
   - Android Studio 会自动检测 `CMakeLists.txt` 并配置 NDK 构建
   - 如果提示找不到 SDK，检查路径是否正确

3. **编译运行**
   - 点击 "Run" 或 `Shift+F10`
   - 构建过程会：
     - 编译 libusb（静态库）
     - 编译 IRtekUVCSDK（共享库 `libirtek_uvc_sdk.so`）
     - 编译 JNI 包装层
     - 打包到 APK 的 `lib/<abi>/` 目录

## 使用方法

### 基本用法

```kotlin
// 1. 初始化 SDK
val uvcManager = UVCManager()
if (uvcManager.init()) {
    // 2. 启用日志（可选）
    uvcManager.enableLog(true)
    
    // 3. 获取 SDK 版本
    val version = uvcManager.getSDKVersion()
    Log.d("UVC", "SDK Version: ${version?.major}.${version?.minor}")
    
    // 4. 获取设备列表
    val devices = uvcManager.getUVCList()
    devices.forEach { device ->
        Log.d("UVC", "Device: VID=0x${device.vid.toString(16)}, PID=0x${device.pid.toString(16)}")
    }
    
    // 5. 登录设备（通过 VID/PID）
    val handle = uvcManager.login(0x1234, 0x5678)
    if (handle > 0) {
        // 使用设备...
        
        // 6. 登出设备
        uvcManager.logout(handle)
    }
    
    // 7. 清理 SDK
    uvcManager.cleanup()
}
```

### USB 权限

Android 需要 USB 权限才能访问设备：

1. **AndroidManifest.xml** 已添加：
   ```xml
   <uses-feature android:name="android.hardware.usb.host" />
   <uses-permission android:name="android.permission.USB_PERMISSION" />
   ```

2. **运行时请求权限**（示例）：
   ```kotlin
   val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
   val device = usbManager.deviceList.values.firstOrNull()
   if (device != null) {
       val permissionIntent = PendingIntent.getBroadcast(
           this, 0, Intent(ACTION_USB_PERMISSION), 
           PendingIntent.FLAG_IMMUTABLE
       )
       usbManager.requestPermission(device, permissionIntent)
   }
   ```

## API 说明

### UVCManager

| 方法 | 说明 |
|------|------|
| `init()` | 初始化 SDK，返回 `true` 表示成功 |
| `cleanup()` | 清理 SDK 资源 |
| `enableLog(enable: Boolean)` | 启用/禁用日志 |
| `getSDKVersion()` | 获取 SDK 版本信息 |
| `getUVCList()` | 获取当前连接的 UVC 设备列表 |
| `login(vid: Short, pid: Short)` | 通过 VID/PID 登录设备，返回句柄 |
| `loginByPath(path: String)` | 通过设备路径登录，返回句柄 |
| `logout(handle: Long)` | 登出设备 |

### 数据类

- **SDKVersion**: `major`, `minor`, `patch`, `build`
- **UVCDeviceInfo**: `path`, `vid`, `pid`
- **DeviceInfo**: `model`, `serialNo`, `brand`, `company`, `firmwareName`, `firmwareVersion`

## 注意事项

1. **设备权限**：Android 需要用户授权才能访问 USB 设备
2. **线程安全**：UVC SDK 不是线程安全的，建议在主线程或单线程中使用
3. **生命周期**：在 `Activity.onDestroy()` 中调用 `cleanup()`
4. **ABI 支持**：默认构建 `arm64-v8a`、`armeabi-v7a`、`x86_64`，可在 `build.gradle` 中修改

## 故障排查

### 构建失败

- **找不到 SDK 源码**：检查 `CMakeLists.txt` 中的路径是否正确
- **NDK 版本不兼容**：更新到 NDK r21+ 或修改 `build.gradle` 中的 `ndkVersion`
- **CMake 版本警告**：可忽略（使用 legacy toolchain），或升级 CMake 到 3.19+

### 运行时错误

- **UnsatisfiedLinkError**：检查 `.so` 文件是否打包到 APK（查看 `app/build/intermediates/merged_native_libs/`）
- **设备未找到**：检查 USB 权限是否已授予，设备是否已连接

## 扩展功能

当前 JNI 层仅实现了基础功能，如需更多 API（如帧回调、流控制等），可参考 `irtek_uvc_sdk.h` 添加对应的 JNI 方法。
