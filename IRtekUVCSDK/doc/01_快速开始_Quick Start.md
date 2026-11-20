# 01 快速开始

## 1. 复制 AAR

将发布包中的两个 AAR 复制到业务工程的 `app/libs/` 目录：

```text
app/libs/IRtekUVCSDK.aar
app/libs/IRtekFileSDK.aar
```

## 2. 配置 Gradle

在应用模块 `build.gradle` 中加入：

```groovy
dependencies {
    implementation files('libs/IRtekUVCSDK.aar')
    implementation files('libs/IRtekFileSDK.aar')
}
```

如果项目统一使用 `fileTree`，也可以写成：

```groovy
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar', '*.aar'])
}
```

## 3. 配置权限

`AndroidManifest.xml` 至少需要包含 USB Host 和相机权限。部分 Android 设备会把 UVC 设备识别为摄像头，必须先授予 `CAMERA` 权限才会弹出 USB 授权框。

```xml
<uses-feature android:name="android.hardware.usb.host" android:required="false" />
<uses-permission android:name="android.permission.CAMERA" />
```

如需接收设备插入事件，可为 Activity 增加 `android.hardware.usb.action.USB_DEVICE_ATTACHED` 的 `intent-filter` 和 `device_filter.xml`。

## 4. 初始化与释放

```kotlin
UVCManager.enableLog(true)
val initResult = UVCManager.init()
if (!initResult.isSuccess) {
    // 根据 initResult.code 和 initResult.message 处理失败
}

// 应用退出或不再使用设备时调用
UVCManager.cleanup()
```

## 5. 基本调用流程

1. 使用 Android `UsbManager` 搜索 USB 设备。
2. 对目标设备请求 USB 授权。
3. 授权成功后调用 `usbManager.openDevice(device)` 获取文件描述符。
4. 调用 `UVCManager.loginByFd(fd, uvcDeviceInfo)` 登录设备。
5. 调用 `UVCManager.getFrameList(device)` 获取帧格式。
6. 调用 `UVCManager.setStreamCallback(device, frameInfo, callback)` 接收原始帧。
7. 将 `UVCFrameRawData` 转成 `IrFileFrame`，交给 `IRtekFileSDK` 生成图像和温度。
8. 调用 `UVCManager.logout(device)` 和 `UVCManager.cleanup()` 释放资源。

---

# 01 Quick Start

## 1. Copy the AARs

Copy the two AARs from the release package into the `app/libs/` directory of your application project:

```text
app/libs/IRtekUVCSDK.aar
app/libs/IRtekFileSDK.aar
```

## 2. Configure Gradle

Add the following to the app module `build.gradle`:

```groovy
dependencies {
    implementation files('libs/IRtekUVCSDK.aar')
    implementation files('libs/IRtekFileSDK.aar')
}
```

If the project uses `fileTree` uniformly, you can also write:

```groovy
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar', '*.aar'])
}
```

## 3. Configure Permissions

`AndroidManifest.xml` must at least include USB Host and camera permissions. Some Android devices recognize UVC devices as cameras, so the USB permission dialog will not appear until `CAMERA` permission is granted.

```xml
<uses-feature android:name="android.hardware.usb.host" android:required="false" />
<uses-permission android:name="android.permission.CAMERA" />
```

To receive device attach events, add an `intent-filter` for `android.hardware.usb.action.USB_DEVICE_ATTACHED` to the Activity, along with `device_filter.xml`.

## 4. Initialization and Cleanup

```kotlin
UVCManager.enableLog(true)
val initResult = UVCManager.init()
if (!initResult.isSuccess) {
    // Handle failure based on initResult.code and initResult.message
}

// Call this when the app exits or the device is no longer used
UVCManager.cleanup()
```

## 5. Basic Call Flow

1. Use Android `UsbManager` to search for USB devices.
2. Request USB permission for the target device.
3. After permission is granted, call `usbManager.openDevice(device)` to obtain the file descriptor.
4. Call `UVCManager.loginByFd(fd, uvcDeviceInfo)` to log in to the device.
5. Call `UVCManager.getFrameList(device)` to get the frame formats.
6. Call `UVCManager.setStreamCallback(device, frameInfo, callback)` to receive raw frames.
7. Convert `UVCFrameRawData` to `IrFileFrame`, then pass it to `IRtekFileSDK` to generate images and temperatures.
8. Call `UVCManager.logout(device)` and `UVCManager.cleanup()` to release resources.
