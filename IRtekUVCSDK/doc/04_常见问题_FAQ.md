# 04 常见问题

## 1. 搜索不到设备

请确认 Android 设备支持 USB Host，并且数据线、转接头和热像设备供电正常。也可以先用系统 USB 设置确认设备是否被系统识别。

## 2. 不弹 USB 授权框

部分系统要求先授予 `CAMERA` 权限，才允许 UVC 摄像头类设备弹出 USB 授权框。请确认 Manifest 中声明了 `CAMERA` 权限，并在运行时申请权限。

## 3. 登录失败

请确认已经通过 `UsbManager.requestPermission()` 获得授权，并且 `usbManager.openDevice(device)` 返回非空连接。登录时传入的 `fd` 必须来自当前授权设备的 `UsbDeviceConnection.fileDescriptor`。

## 4. `UnsatisfiedLinkError`

通常是 AAR 未正确加入依赖、ABI 不匹配或 APK 未打包 native so。请确认：

- `IRtekUVCSDK.aar` 和 `IRtekFileSDK.aar` 都已加入 `dependencies`。
- 真机 ABI 在 AAR 支持范围内。
- 不要只复制 Java/Kotlin 类，必须复制完整 AAR。

## 5. 有帧回调但没有预览图

请检查 `IRtekFileSDK.getThermalImageFromFrame()` 的返回 `errorCode`。如果失败，通常是 `IrFileFrame.buffer`、`length`、图像尺寸或热像上下文缺少必要参数。

## 6. 温度为空或异常

请确认 IRtekFileSDK 上下文已经接收过有效的全辐射帧，并且测温点坐标位于热图宽高范围内。发射率、距离、反射温度等参数也会影响温度结果。

## 7. 预览卡顿

成像和测温不建议在 UI 线程执行。Demo 为了展示流程写得较直观，正式项目应在后台线程处理帧，并限制 UI 刷新帧率。

---

# 04 FAQ

## 1. Device not found

Make sure the Android device supports USB Host, and that the data cable, adapter, and thermal camera power supply are working normally. You can also check the system USB settings first to confirm that the device is recognized by the OS.

## 2. USB permission dialog does not appear

Some systems require `CAMERA` permission to be granted before a UVC camera-class device can show the USB permission dialog. Make sure `CAMERA` permission is declared in the Manifest and requested at runtime.

## 3. Login failed

Make sure permission has been obtained via `UsbManager.requestPermission()`, and that `usbManager.openDevice(device)` returns a non-null connection. The `fd` passed at login must come from `UsbDeviceConnection.fileDescriptor` of the currently authorized device.

## 4. `UnsatisfiedLinkError`

This is usually caused by AARs not being added to dependencies correctly, an ABI mismatch, or native `.so` files not being packaged into the APK. Please confirm:

- Both `IRtekUVCSDK.aar` and `IRtekFileSDK.aar` have been added to `dependencies`.
- The device ABI is within the range supported by the AARs.
- Do not copy only Java/Kotlin classes; you must copy the complete AARs.

## 5. Frame callbacks are received but there is no preview image

Check the `errorCode` returned by `IRtekFileSDK.getThermalImageFromFrame()`. If it fails, the usual causes are `IrFileFrame.buffer`, `length`, image dimensions, or missing required parameters in the thermal imaging context.

## 6. Temperature is empty or abnormal

Make sure the IRtekFileSDK context has already received a valid full-radiometric frame, and that the measurement point coordinates are within the thermal image width and height. Parameters such as emissivity, distance, and reflected temperature also affect the temperature result.

## 7. Preview stuttering

Imaging and temperature measurement should not be performed on the UI thread. The Demo is written in a straightforward way to illustrate the flow; a production project should process frames on a background thread and limit the UI refresh rate.
