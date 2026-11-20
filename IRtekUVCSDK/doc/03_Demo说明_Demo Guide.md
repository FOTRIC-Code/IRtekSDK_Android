# 03 Demo说明

## 运行环境

- Android Studio
- Android SDK 24 或以上
- 支持 USB Host 的 Android 真机
- 已连接 IRtek UVC 热像设备

## 打开工程

在 Android Studio 中打开：

```text
docs/IRtekUVCSDK_Android/demo/IRtekUVCSDKDemo
```

Demo 的 `app/build.gradle` 已经通过相对路径引用发布包中的两个 AAR：

```groovy
implementation files('../../../library/aar/IRtekUVCSDK.aar')
implementation files('../../../library/aar/IRtekFileSDK.aar')
```

## 页面功能

Demo 页面采用“固定预览 + 下方主功能 Tab”的布局：

- 上方固定显示 SDK 状态、连接状态、热像视频画面、CENTER marker 温度和快捷操作。
- 视频下方提供刷新、请求权限、连接/断开、开始/停止预览等常用操作。
- 下方主功能 Tab 包含“设备信息”“设备控制”“调色板”“日志”，切换 Tab 时热像画面始终可见。

### 搜索设备

点击“搜索设备”后，Demo 使用 Android `UsbManager.deviceList` 枚举当前连接的 USB 设备，并显示设备名称、VID 和 PID。

### 登录设备

点击某个设备的“登录设备”按钮后，Demo 会先检查 `CAMERA` 权限，再请求 USB 授权。授权成功后通过 `usbManager.openDevice()` 获取文件描述符，并调用 `UVCManager.loginByFd()` 登录 UVC 设备。

### 开始预览/测温

点击“开始预览/测温”后，Demo 调用 `UVCManager.getFrameList()` 获取帧格式，再调用 `UVCManager.setStreamCallback()` 接收 `UVCFrameRawData`。

每一帧的处理流程如下：

1. 将 `UVCFrameRawData.data` 转换为 `IrFileFrame`。
2. 将流内 LUT 写入 IRtekFileSDK 上下文。
3. 调用 `IRtekFile.getThermalImageFromFrame()` 生成热图图像；该 Java 接口对应 native `ir_file_get_thermal_image_from_frame()`。
4. 在热图中心创建名为 `CENTER` 的点 marker，并绘制到预览画面上。
5. 调用 `IRtekFile.setMarker()` 写入 marker，再调用 `IRtekFile.getMarker("CENTER")` 读取 `tempValues` 作为中心 marker 温度。
6. 将图像、marker 和温度显示到界面。

### 设备信息

“设备信息”Tab 显示型号、序列号、镜头、VID、PID、分辨率和量程等信息。

### 设备控制

“设备控制”Tab 会直接向设备发送控制命令。只有“执行热像校正”是立即执行按钮，其它配置项都采用“获取 / 下拉选择 / 设置”的方式，设置值可在页面中选择。

- 执行热像校正。
- 获取/设置热像校正模式。
- 获取/设置热像自动校正时间。
- 获取/设置热像测温量程。
- 获取/设置热像测温参数。
- 获取/设置图像参数。
- 获取/设置图像翻转模式。
- 获取/设置声像频率范围。界面单位为 kHz，写入设备前换算为 Hz。可设置范围是 2～100 kHz，且下限必须小于上限。
- 获取/设置声像图像模式。单声源和多声源可设置灵敏度和声源尺寸；全息模式可设置固定声宽。点击“恢复默认”只改页面上的值，需要再点“设置”才会写入设备。设置时声宽上下限固定为 `1000` 和 `-100`。

图像参数变更后，Demo 会从设备重新读取图像参数，并调用 IRtekFileSDK 的图像显示参数接口同步到 FileSDK 上下文后才会影响热图生成。图像翻转模式变更后，Demo 会从设备重新读取翻转模式，并在上层预览 Bitmap 时应用翻转。

### 调色板

“调色板”Tab 通过 IRtekFileSDK 读取和设置预置调色板：

1. 点击“获取”调用 `IRtekFile.getPresetPalettes()` 读取预置调色板列表。
2. 在下拉框选择调色板。
3. 可通过“反转调色板”开关设置 `IrFilePalette.inverted`。
4. 点击“设置”调用 `IRtekFile.setPalette()`，后续 `getThermalImageFromFrame()` 生成的热图会使用新的调色板。

调色板只影响 FileSDK 生成热图，不向 UVC 设备发送参数。

### 日志

“日志”Tab 显示所有设备操作、预览和 FileSDK 处理结果，可点击“清空”清除页面日志。

### 释放资源

点击“释放资源”或退出页面时，Demo 会调用 `UVCManager.logout()`、关闭 `UsbDeviceConnection`，并在 Activity 销毁时调用 `UVCManager.cleanup()`。

## 代码入口

- `MainActivity.kt`：USB 权限、搜索、登录、开启预览和 UI 展示。
- `IRtekFileFrameProcessor.java`：演示 UVC 原始帧到 `IrFileFrame`，再通过 `ir_file_get_thermal_image_from_frame()` 生成图像，并通过 `ir_file_set_marker()` / `ir_file_get_marker()` 获取中心 marker 温度的转换流程。

---

# 03 Demo Guide

## Runtime Environment

- Android Studio
- Android SDK 24 or later
- An Android physical device that supports USB Host
- An IRtek UVC thermal imaging device connected

## Open the Project

Open the following in Android Studio:

```text
docs/IRtekUVCSDK_Android/demo/IRtekUVCSDKDemo
```

The Demo `app/build.gradle` already references the two AARs in the release package via relative paths:

```groovy
implementation files('../../../library/aar/IRtekUVCSDK.aar')
implementation files('../../../library/aar/IRtekFileSDK.aar')
```

## Page Features

The Demo page uses a layout of “fixed preview + main feature tabs below”:

- The top area always shows SDK status, connection status, the thermal video view, the CENTER marker temperature, and shortcut actions.
- Below the video are common actions such as refresh, request permission, connect/disconnect, and start/stop preview.
- The main feature tabs below include “Device Info”, “Device Control”, “Palette”, and “Log”. The thermal image remains visible when switching tabs.

### Search Devices

After tapping “Search Devices”, the Demo uses Android `UsbManager.deviceList` to enumerate currently connected USB devices and displays the device name, VID, and PID.

### Log In to a Device

After tapping the “Log In” button for a device, the Demo first checks `CAMERA` permission, then requests USB authorization. After authorization succeeds, it obtains a file descriptor via `usbManager.openDevice()` and calls `UVCManager.loginByFd()` to log in to the UVC device.

### Start Preview / Temperature Measurement

After tapping “Start Preview / Temperature Measurement”, the Demo calls `UVCManager.getFrameList()` to get frame formats, then calls `UVCManager.setStreamCallback()` to receive `UVCFrameRawData`.

Each frame is processed as follows:

1. Convert `UVCFrameRawData.data` to `IrFileFrame`.
2. Write the in-stream LUT into the IRtekFileSDK context.
3. Call `IRtekFile.getThermalImageFromFrame()` to generate a thermal image; this Java API corresponds to the native `ir_file_get_thermal_image_from_frame()`.
4. Create a point marker named `CENTER` at the center of the thermal image and draw it on the preview.
5. Call `IRtekFile.setMarker()` to write the marker, then call `IRtekFile.getMarker("CENTER")` to read `tempValues` as the center marker temperature.
6. Display the image, marker, and temperature on the UI.

### Device Info

The “Device Info” tab shows model, serial number, lens, VID, PID, resolution, temperature range, and other information.

### Device Control

The “Device Control” tab sends control commands directly to the device. Only “Perform Thermal Calibration” is an immediate-action button. Other settings use a “Get / dropdown select / Set” pattern, and the value to set can be chosen on the page.

- Perform thermal calibration.
- Get/set thermal calibration mode.
- Get/set thermal auto-calibration interval.
- Get/set thermal measurement range.
- Get/set thermal measurement parameters.
- Get/set image parameters.
- Get/set image flip mode.
- Get/set the acoustic frequency range. The page uses kHz and converts the values to Hz before writing them to the device. Allowed values are 2–100 kHz, and the minimum must be less than the maximum.
- Get/set the acoustic image mode. Single-source and multi-source modes expose sensitivity and source size; holographic mode exposes a fixed span. “Restore defaults” only changes the values on the page; tap “Set” to write them to the device. The upper and lower span limits sent to the device are fixed at `1000` and `-100`.

After image parameters change, the Demo re-reads image parameters from the device and syncs them to the FileSDK context via the IRtekFileSDK image display parameter APIs before they affect thermal image generation. After image flip mode changes, the Demo re-reads the flip mode from the device and applies the flip when rendering the preview Bitmap.

### Palette

The “Palette” tab reads and sets preset palettes through IRtekFileSDK:

1. Tap “Get” to call `IRtekFile.getPresetPalettes()` and load the preset palette list.
2. Select a palette from the dropdown.
3. Use the “Invert Palette” switch to set `IrFilePalette.inverted`.
4. Tap “Set” to call `IRtekFile.setPalette()`. Subsequent thermal images generated by `getThermalImageFromFrame()` will use the new palette.

The palette only affects thermal images generated by FileSDK; it does not send parameters to the UVC device.

### Log

The “Log” tab shows all device operations, preview, and FileSDK processing results. Tap “Clear” to clear the page logs.

### Release Resources

When you tap “Release Resources” or leave the page, the Demo calls `UVCManager.logout()`, closes `UsbDeviceConnection`, and calls `UVCManager.cleanup()` when the Activity is destroyed.

## Code Entry Points

- `MainActivity.kt`: USB permission, search, login, starting preview, and UI display.
- `IRtekFileFrameProcessor.java`: Demonstrates converting a UVC raw frame to `IrFileFrame`, then generating an image via `ir_file_get_thermal_image_from_frame()`, and obtaining the center marker temperature via `ir_file_set_marker()` / `ir_file_get_marker()`.
