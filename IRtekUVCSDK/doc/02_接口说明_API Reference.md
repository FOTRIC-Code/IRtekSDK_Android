# 02 接口说明

## 一、UVC 设备接口

### `UVCManager.init()`

用途：初始化 UVC SDK。必须在搜索、登录、取流等接口前调用一次。

返回值：`UVCResult<Unit>`。`code == 0` 表示成功；失败时通过 `message` 查看原因。

调用时机：应用启动后、使用 UVC 设备前。

### `UVCManager.enableLog(enable: Boolean)`

用途：开启或关闭底层 SDK 日志。

参数：`enable` 为 `true` 时开启日志，为 `false` 时关闭日志。

返回值：`UVCResult<Unit>`。

调用时机：建议初始化前后开启，便于定位设备枚举、登录、取流问题。

### `UVCManager.getSDKVersion()`

用途：获取 UVC SDK 版本字符串。

返回值：`UVCResult<String>`。成功时 `data` 为版本号。

调用时机：初始化成功后，可展示在关于页面或日志中。

### `UVCManager.loginByFd(fd: Int, device: UVCDeviceInfo)`

用途：通过 Android USB 文件描述符登录设备。

参数：`fd` 来自 `UsbDeviceConnection.fileDescriptor`；`device` 用于保存设备路径、VID 和 PID。

返回值：`UVCResult<UVCDeviceInfo>`。成功后后续接口都使用返回的设备对象。

调用时机：用户授予 USB 权限并成功 `openDevice()` 后。

### `UVCManager.getFrameList(device: UVCDeviceInfo)`

用途：获取设备支持的帧格式列表。

参数：已登录的 `UVCDeviceInfo`。

返回值：`UVCResult<List<UVCFrameInfo>>`。每个 `UVCFrameInfo` 包含宽、高、格式和 UVC 流参数。

调用时机：登录设备成功后，开启预览前。

### `UVCManager.setStreamCallback(device, frameInfo, callback)`

用途：开启指定帧格式的数据回调。

参数：`device` 为已登录设备；`frameInfo` 为帧格式；`callback` 的 `onFrameReceived(frame)` 会收到 `UVCFrameRawData`。

返回值：`UVCResult<Unit>`。

调用时机：用户点击开始预览或业务需要实时取帧时。

### `UVCFrameRawData`

用途：表示 UVC SDK 回调的一帧原始热像数据。

关键字段：`data` 为图像原始字节；`width`、`height` 为图像尺寸；`adValues` 为 AD 值数组；`targetTemps`、`lutEnvTemp`、`sensorTemp`、`frameNo` 等为设备侧解析出的帧元数据。

使用方式：不要在业务层手写复杂成像算法，建议将其转换为 `IrFileFrame` 后交给 `IRtekFileSDK` 生成图像和温度。

### `UVCManager.getAcousticDbScale(device)` / `UVCManager.setAcousticDbScale(device, scale)`

用途：读取或设置声像图像模式。对应 native 接口 `uvc_get_acoustic_db_scale()` / `uvc_set_acoustic_db_scale()`。

参数：`AcousticDbScale`。`mode` 为 `1` 单声源、`2` 多声源、`4` 全息；`fixedSpan` 为 `1` 时启用固定声宽，`span` 为声宽跨度；`sensitivity` 为灵敏度，`sourceSize` 为声源尺寸；`limit` 为声压下限；`focusEnabled` 为 `1` 时开启聚焦。`max` / `min` 为声宽上下限。

返回值：读取返回 `UVCResult<AcousticDbScale>`；写入返回 `UVCResult<Unit>`。热像设备会返回不支持。

调用时机：声像设备登录后。

### `uvc_get_acoustic_freq_range` / `uvc_set_acoustic_freq_range`

用途：读取或设置声像频率范围。Java SDK 未封装这两个接口，Demo 通过 `libacoustic_preview.so` 调用。

参数：`min`、`max` 单位为 Hz，取值范围 `0.0`～`1000000.0`，默认 `5000.0` 和 `100000.0`。Demo 界面以 kHz 显示，可设置 `2`～`100` kHz，写入前乘以 `1000`，并要求下限小于上限。

返回值：`0` 表示成功，其它值为 SDK 错误码。热像设备不支持。

调用时机：声像设备登录后。

### `UVCManager.logout(device)` / `UVCManager.cleanup()`

用途：登出设备并释放 SDK 资源。

调用时机：停止预览、设备拔出、Activity 销毁或应用退出时。

## 二、IRtekFileSDK 接口

`IRtekFileSDK.aar` 对外包名为 `com.fotric.irtekfilesdk`，核心入口类是 `IRtekFile`。Java 层方法最终调用 native 方法，其中 `IRtekFile.getThermalImageFromFrame()` 对应 native 接口 `ir_file_get_thermal_image_from_frame()`。

### 文件对象创建与保存

#### `IRtekFile.openIrtekFile(fileName: String)`

用途：打开已有热像文件。

参数：`fileName` 为本地文件路径。

返回值：成功返回 `IRtekFile` 对象，失败返回 `null`。

调用时机：读取已有 FJPG、热视频或热像文件时使用。

#### `IRtekFile.createIrtekFile(fileName: String, type: Int)`

用途：创建热像文件对象，作为写入全辐射帧、生成热图和计算温度的上下文。

参数：`fileName` 为临时或目标文件路径；`type` 使用 `IrFileEnumFileType`，常用 `IrFileThermalpicture` 或 `IrFileThermalVideo`。

返回值：成功返回 `IRtekFile` 对象，失败返回 `null`。

调用时机：UVC 实时预览时，建议先创建一个临时热视频上下文，再把 `UVCFrameRawData` 转为 `IrFileFrame` 交给 IRtekFileSDK。

#### `save()` / `saveAs(fileName)` / `saveFjpg()` / `saveAsFjpg(fileName)` / `saveFrameAs(fileName, coverImage)`

用途：保存当前热像文件、另存为新文件、保存为 FJPG，或将当前帧另存为热像文件。

返回值：`IrFileErrorCode`，`errorCode == IrFileEcOk` 表示成功。

调用时机：需要把实时帧或编辑后的热像数据落盘时使用。

#### `saveAsStream(fileName)`

用途：创建热视频裁剪或视频流文件对象。

返回值：成功返回新的 `IRtekFile` 对象。

### 文件与设备基础信息

#### `getSDKVersion()` / `getFileVersion()`

用途：读取 IRtekFileSDK 版本和当前热像文件版本。

返回值：`IrFileVersion`。

#### `peekFileType(fileName)` / `getFileType()`

用途：检查指定文件或当前文件的热像文件类型。

返回值：`IrFileEnumFileType`，常见值包括 `IrFileThermalpicture`、`IrFileThermalVideo`、`IrFileVisualPicture`。

#### `getFileName()` / `getShotTime()` / `getModifiedTime()`

用途：读取文件名、拍摄时间和修改时间。

返回值：分别为 `IrFileBuffer` 或 `IrFileTime`。

#### `getCameraInfo()` / `setCameraInfo(value)`

用途：读取或写入设备信息，如型号、品牌、序列号等。

返回值：读取返回 `IrFileCameraInfo`；写入返回 `IrFileErrorCode`。

#### `getLensInfo()` / `setLensInfo(lensInfo)` / `getLensInfoV2()` / `setLensInfoV2(lensInfoV2)`

用途：读取或写入镜头信息，包括镜头名称、焦距、视场角和量程能力。

返回值：读取返回镜头信息对象；写入返回 `IrFileErrorCode`。

### 测温参数与 LUT

#### `getTempRange()` / `setTempRange(tempRange)`

用途：读取或设置当前测温量程。

返回值：读取返回 `IrFileTempRange`；写入返回 `IrFileErrorCode`。

#### `getLut()` / `getCalculatedLut()` / `setLut(lut)`

用途：读取出厂温度查找表、补偿后的温度查找表，或写入温度查找表。

返回值：读取返回 `IrFileLut`，其中包含 AD 数组和温度数组；写入返回 `IrFileErrorCode`。

#### `getThermalParams()` / `setThermalParams(thermalParams)`

用途：读取或设置测温参数，例如发射率、距离、反射温度、环境温度和相对湿度。

返回值：读取返回 `IrFileThermalParams`；写入返回 `IrFileErrorCode`。

#### `getReference()` / `getReferenceTemp()` / `setReference(reference)`

用途：读取或设置参考温度信息，或读取参考源温度值。

返回值：读取返回 `IrFileReference` 或 `IrFileReferenceTemp`；写入返回 `IrFileErrorCode`。

#### `getGain()` / `setGain(gain)`

用途：读取或设置温宽信息。

返回值：读取返回 `IrFileGain`；写入返回 `IrFileErrorCode`。

### 调色板与图像显示参数

#### `getPalette()` / `getPaletteV2()` / `setPalette(palette)`

用途：读取或设置调色板。

返回值：读取返回 `IrFilePalette` 或 `IrFilePalette_v2`；写入返回 `IrFileErrorCode`。

#### `getPresetPalettes()` / `getPresetPaletteImage(refNo, offset)` / `getPaletteImage(offset)`

用途：读取预置调色板列表，或生成调色板色带图像。

返回值：`IrFilePresetPalettes` 或 `IrFileImage`。

#### `getVtParams()` / `setVtParams(value)` / `getVtParamsV2()` / `setVtParamsV2(value)`

用途：读取或设置图像显示参数。V2 接口会根据 ISP 类型返回 buffer 或图像参数对象。

返回值：读取返回 `IrFileBuffer` 或 `IrFileVtParamsV2`；写入返回 `IrFileErrorCode`。

#### `getIspType()` / `setIspType(ispType)`

用途：读取或设置图像 ISP 类型，例如 NEMO 或 HOLLY。

返回值：读取返回 `IrFileIspType`；写入返回 `IrFileErrorCode`。

#### `getColorDistType()` / `setColorDistType(colorDistType)`

用途：读取或设置色彩分布类型，如温度线性、AD 线性等。

返回值：读取返回 `IrFileEnumColorDistType`；写入返回 `IrFileErrorCode`。

#### `setRatotion(rotation)` / `setMirrorType(mirrorType)` / `getImageEnh()` / `setImageEnh(imageEnh)`

用途：旋转图像、镜像翻转图像，或读取和设置图像细节增强。

返回值：`IrFileErrorCode` 或 `IrFileImageEnh`。

### 原始帧、热图生成与实时预览

#### `IrFileFrame`

用途：IRtekFileSDK 接收的全辐射帧数据结构。

关键字段：`buffer` 为 AD 值数组；`length` 为 AD 值数量；`timeStamp` 为帧时间戳。

和 UVC 的关系：可由 `UVCFrameRawData.adValues` 直接填充；如果只有字节数组，可按小端序每两个字节转换为一个 `short`。

#### `appendFrame(frame)`

用途：将一帧全辐射数据追加到 IRtekFileSDK 上下文中。

参数：`frame` 为 `IrFileFrame`。

返回值：`IrFileErrorCode`。

调用时机：实时预览中每收到一帧 UVC 原始帧后调用。

#### `getFrame(index)` / `getFrameCount()` / `getFrameIndex()` / `setFrameIndex(index)`

用途：读取帧总数、按索引读取帧、读取或设置当前帧索引。

返回值：`IrFileFrame`、`IrFileFrameCount`、`IrFileFrameIndex` 或 `IrFileErrorCode`。

#### `getFrameRate()` / `setFrameRate(frameRate)` / `getCompressType()` / `setCompressType(compressType)`

用途：读取或设置热视频帧率、帧内压缩方式。

返回值：读取返回帧率或压缩类型对象；写入返回 `IrFileErrorCode`。

#### `getThermalImage(offset)`

用途：基于当前热像文件和当前帧生成热图图像。

参数：`offset` 为行字节对齐偏移，通常传 `0`。

返回值：`IrFileImage`。

#### `getThermalImageV2(colorSpace, offset)`

用途：基于当前热像文件和当前帧，按指定颜色空间生成热图图像。

参数：`colorSpace` 为颜色空间，常用 `1` 表示 BGR，`2` 表示 RGBA；`offset` 通常传 `0`。

返回值：`IrFileImage`。

#### `getThermalImageFromFrame(frame, colorSpace, offset)`

用途：直接从一帧全辐射数据生成热图图像。该 Java 接口对应 native `ir_file_get_thermal_image_from_frame()`。

参数：`frame` 为 `IrFileFrame`；`colorSpace` 常用 `2` 表示 RGBA；`offset` 为行字节对齐偏移，通常传 `0`。

返回值：`IrFileImage`。成功时 `data` 是图像字节数组，可按帧宽高转换为 Android `Bitmap`。

调用时机：实时 UVC 预览推荐使用该接口。Demo 中的流程是 `UVCFrameRawData` -> `IrFileFrame` -> `getThermalImageFromFrame()` -> `Bitmap`。

### 点、线、区域测温

#### `getTempSpot(point)`

用途：读取指定点温度。

参数：`IrFileTempPoint`，其中 `point.x` 和 `point.y` 为热图坐标；`emissivity`、`distance`、`reflTemp` 可设置局部测温参数。

返回值：`IrFileTemp`，成功时 `temp` 为温度值。

#### `getTempRect(rect)` / `getTempEllipse(ellipse)`

用途：读取矩形或椭圆区域内的温度数组。

参数：`IrFileTempRect`，包含区域坐标、宽高和局部测温参数。

返回值：`IrFileTemps`，成功时 `temps` 为温度数组，可计算最高温、最低温或平均温。

#### `getTempPolyline(polyline)` / `getTempPolygon(polygon)`

用途：读取折线或多边形区域的温度数组。

参数：`IrFileTempPoints`，包含点列表和局部测温参数。

返回值：`IrFileTemps`。

### 测量标识、温差与报警

#### `getMarkerNames()` / `getMarker(name)` / `setMarker(marker)` / `removeMarker(name)` / `renameMarker(sourceName, targetName)`

用途：读取、写入、删除或重命名点、线、矩形、椭圆、多边形等测量标识。

返回值：读取返回 `IrFileMarkerNames` 或 `IrFileMarker`；写入和删除返回 `IrFileErrorCode`。

#### `getIndexMarker(index, name)`

用途：按帧索引读取测量标识。

返回值：`IrFileMarker`。

#### `getDeltaNames()` / `getDelta(name)` / `setDelta(delta)` / `removeDelta(deltaName)` / `getIndexDelta(index, name)`

用途：读取、写入、删除温差标记，或按帧读取温差标记。

返回值：名称列表、温差对象、温差值或错误码。

#### `getAlarm()` / `getAlarmV2()` / `setAlarm(alarm)`

用途：读取或设置测温报警信息。

返回值：读取返回报警对象；写入返回 `IrFileErrorCode`。

### 可见光、封面、备注和用户数据

#### `getVisualImage()` / `setVisualImage(value)` / `getCoverImage()` / `setCoverImage(value)`

用途：读取或写入可见光图片和封面图。

返回值：读取返回 `IrFileBuffer`；写入返回 `IrFileErrorCode`。

#### `getNote()` / `setNote(value)` / `getAudio()` / `setAudio(value)`

用途：读取或写入文本备注、音频数据。

返回值：读取返回 `IrFileBuffer` 或 `IrFileAudio`；写入返回 `IrFileErrorCode`。

#### `getKeys()` / `getUserData(key)` / `setUserData(value)`

用途：读取用户数据键列表、读取指定键的用户数据、写入用户数据。

返回值：`IrFileKeys`、`IrFileKvPair` 或 `IrFileErrorCode`。

#### `getGeoLocation()` / `setGeoLocation(geoLocation)`

用途：读取或写入地理位置信息。

返回值：读取返回 `IrFileGeoLocation`；写入返回 `IrFileErrorCode`。

#### `getZoomInfo()` / `setZoomInfo(zoomInfo)` / `getViewInfo()` / `setViewInfo(value)` / `getMappingInfo()` / `setMappingInfo(mappingInfo)`

用途：读取或设置数码变焦、显示信息、热像区域映射信息。

返回值：读取返回对应信息对象；写入返回 `IrFileErrorCode`。

### 错误码与常量

#### `IrFileErrorCode`

用途：所有 IRtekFileSDK 返回对象都继承或包含 `errorCode`，用于判断调用是否成功。

常见值：`IrFileEcOk = 0` 表示成功；`IrFileEcFail = -1` 表示失败；`IrFileEcMemory = -2` 表示内存分配失败；`IrFileEcPointer = -3` 表示空指针；`IrFileEcInvalidKey = -4` 表示索引不存在；`IrFileEcNotSupported = -5` 表示功能不支持。

#### `IrFileEnumFileType`

用途：创建或判断文件类型。

常见值：`IrFileThermalpicture` 表示热像图片；`IrFileThermalVideo` 表示热视频；`IrFileVisualPicture` 表示可见光图片；`IrFileGridThermalPicture` 表示电网热像图片。

#### `IrFileEnumColorDistType`

用途：设置色彩分布方式。

常见值：`IrFileTWB`、`IrFileTempLinear`、`IrFileAdLinear`。

#### `IrFileEnumRotateType` / `IrFileEnumMirrorType`

用途：设置图像旋转和镜像方式。

## 三、推荐职责边界

- `IRtekUVCSDK.aar`：负责设备搜索、USB 登录、码流回调和设备参数。
- `IRtekFileSDK.aar`：负责将 UVC 原始帧转成热图，并计算点温、线温、区域温。
- 业务层：负责权限申请、UI 展示、生命周期管理和错误提示。

---

# 02 API Reference

## 1. UVC Device APIs

### `UVCManager.init()`

Purpose: Initialize the UVC SDK. Must be called once before search, login, streaming, and other APIs.

Return value: `UVCResult<Unit>`. `code == 0` indicates success; on failure, check `message` for the reason.

When to call: After the app starts and before using a UVC device.

### `UVCManager.enableLog(enable: Boolean)`

Purpose: Enable or disable underlying SDK logging.

Parameters: Set `enable` to `true` to turn logs on, or `false` to turn them off.

Return value: `UVCResult<Unit>`.

When to call: Recommended around initialization, to help diagnose device enumeration, login, and streaming issues.

### `UVCManager.getSDKVersion()`

Purpose: Get the UVC SDK version string.

Return value: `UVCResult<String>`. On success, `data` is the version number.

When to call: After initialization succeeds; can be shown on an About page or in logs.

### `UVCManager.loginByFd(fd: Int, device: UVCDeviceInfo)`

Purpose: Log in to a device using an Android USB file descriptor.

Parameters: `fd` comes from `UsbDeviceConnection.fileDescriptor`; `device` is used to store the device path, VID, and PID.

Return value: `UVCResult<UVCDeviceInfo>`. After success, subsequent APIs all use the returned device object.

When to call: After the user grants USB permission and `openDevice()` succeeds.

### `UVCManager.getFrameList(device: UVCDeviceInfo)`

Purpose: Get the list of frame formats supported by the device.

Parameters: A logged-in `UVCDeviceInfo`.

Return value: `UVCResult<List<UVCFrameInfo>>`. Each `UVCFrameInfo` contains width, height, format, and UVC stream parameters.

When to call: After login succeeds and before starting preview.

### `UVCManager.setStreamCallback(device, frameInfo, callback)`

Purpose: Start data callbacks for the specified frame format.

Parameters: `device` is the logged-in device; `frameInfo` is the frame format; `callback.onFrameReceived(frame)` receives `UVCFrameRawData`.

Return value: `UVCResult<Unit>`.

When to call: When the user starts preview or the application needs real-time frames.

### `UVCFrameRawData`

Purpose: Represents one frame of raw thermal data delivered by the UVC SDK callback.

Key fields: `data` is the raw image bytes; `width` and `height` are the image dimensions; `adValues` is the AD value array; `targetTemps`, `lutEnvTemp`, `sensorTemp`, `frameNo`, and others are frame metadata parsed on the device side.

Usage: Do not implement complex imaging algorithms in the application layer. Convert the data to `IrFileFrame` and let `IRtekFileSDK` generate images and temperatures.

### `UVCManager.getAcousticDbScale(device)` / `UVCManager.setAcousticDbScale(device, scale)`

Purpose: Read or set the acoustic image mode. These methods correspond to the native APIs `uvc_get_acoustic_db_scale()` and `uvc_set_acoustic_db_scale()`.

Parameters: `AcousticDbScale`. `mode` is `1` for a single source, `2` for multiple sources, or `4` for holographic mode. `fixedSpan` is `1` when a fixed span is enabled, and `span` is that span. `sensitivity` is the sensitivity and `sourceSize` is the source size. `limit` is the lower sound-pressure limit. `focusEnabled` is `1` when focus is enabled. `max` and `min` are the upper and lower span limits.

Return value: Reading returns `UVCResult<AcousticDbScale>`; writing returns `UVCResult<Unit>`. A thermal device returns not supported.

When to call: After an acoustic device is logged in.

### `uvc_get_acoustic_freq_range` / `uvc_set_acoustic_freq_range`

Purpose: Read or set the acoustic frequency range. The Java SDK does not wrap these two APIs. The demo calls them through `libacoustic_preview.so`.

Parameters: `min` and `max` are in Hz, from `0.0` to `1000000.0`. The defaults are `5000.0` and `100000.0`. The demo page shows kHz, accepts `2`–`100` kHz, multiplies by `1000` before writing, and requires the minimum to be less than the maximum.

Return value: `0` means success. Any other value is an SDK error code. A thermal device does not support these APIs.

When to call: After an acoustic device is logged in.

### `UVCManager.logout(device)` / `UVCManager.cleanup()`

Purpose: Log out of the device and release SDK resources.

When to call: When stopping preview, when the device is unplugged, when the Activity is destroyed, or when the app exits.

## 2. IRtekFileSDK APIs

The public package name of `IRtekFileSDK.aar` is `com.fotric.irtekfilesdk`. The core entry class is `IRtekFile`. Java-layer methods ultimately call native methods; `IRtekFile.getThermalImageFromFrame()` corresponds to the native API `ir_file_get_thermal_image_from_frame()`.

### File Object Creation and Saving

#### `IRtekFile.openIrtekFile(fileName: String)`

Purpose: Open an existing thermal imaging file.

Parameters: `fileName` is the local file path.

Return value: Returns an `IRtekFile` object on success, or `null` on failure.

When to call: When reading an existing FJPG, thermal video, or thermal imaging file.

#### `IRtekFile.createIrtekFile(fileName: String, type: Int)`

Purpose: Create a thermal imaging file object as the context for writing full-radiometric frames, generating thermal images, and calculating temperatures.

Parameters: `fileName` is a temporary or target file path; `type` uses `IrFileEnumFileType`, commonly `IrFileThermalpicture` or `IrFileThermalVideo`.

Return value: Returns an `IRtekFile` object on success, or `null` on failure.

When to call: During UVC live preview, create a temporary thermal video context first, then convert `UVCFrameRawData` to `IrFileFrame` and pass it to IRtekFileSDK.

#### `save()` / `saveAs(fileName)` / `saveFjpg()` / `saveAsFjpg(fileName)` / `saveFrameAs(fileName, coverImage)`

Purpose: Save the current thermal imaging file, save as a new file, save as FJPG, or save the current frame as a thermal imaging file.

Return value: `IrFileErrorCode`; `errorCode == IrFileEcOk` indicates success.

When to call: When you need to persist a live frame or edited thermal imaging data to disk.

#### `saveAsStream(fileName)`

Purpose: Create a thermal video clip or video stream file object.

Return value: Returns a new `IRtekFile` object on success.

### File and Device Basic Information

#### `getSDKVersion()` / `getFileVersion()`

Purpose: Read the IRtekFileSDK version and the current thermal imaging file version.

Return value: `IrFileVersion`.

#### `peekFileType(fileName)` / `getFileType()`

Purpose: Check the thermal imaging file type of a specified file or the current file.

Return value: `IrFileEnumFileType`. Common values include `IrFileThermalpicture`, `IrFileThermalVideo`, and `IrFileVisualPicture`.

#### `getFileName()` / `getShotTime()` / `getModifiedTime()`

Purpose: Read the file name, capture time, and modified time.

Return value: `IrFileBuffer` or `IrFileTime`, respectively.

#### `getCameraInfo()` / `setCameraInfo(value)`

Purpose: Read or write device information, such as model, brand, and serial number.

Return value: Read returns `IrFileCameraInfo`; write returns `IrFileErrorCode`.

#### `getLensInfo()` / `setLensInfo(lensInfo)` / `getLensInfoV2()` / `setLensInfoV2(lensInfoV2)`

Purpose: Read or write lens information, including lens name, focal length, field of view, and range capability.

Return value: Read returns a lens information object; write returns `IrFileErrorCode`.

### Temperature Measurement Parameters and LUT

#### `getTempRange()` / `setTempRange(tempRange)`

Purpose: Read or set the current temperature measurement range.

Return value: Read returns `IrFileTempRange`; write returns `IrFileErrorCode`.

#### `getLut()` / `getCalculatedLut()` / `setLut(lut)`

Purpose: Read the factory temperature lookup table, the compensated temperature lookup table, or write a temperature lookup table.

Return value: Read returns `IrFileLut`, which contains an AD array and a temperature array; write returns `IrFileErrorCode`.

#### `getThermalParams()` / `setThermalParams(thermalParams)`

Purpose: Read or set temperature measurement parameters, such as emissivity, distance, reflected temperature, ambient temperature, and relative humidity.

Return value: Read returns `IrFileThermalParams`; write returns `IrFileErrorCode`.

#### `getReference()` / `getReferenceTemp()` / `setReference(reference)`

Purpose: Read or set reference temperature information, or read the reference source temperature value.

Return value: Read returns `IrFileReference` or `IrFileReferenceTemp`; write returns `IrFileErrorCode`.

#### `getGain()` / `setGain(gain)`

Purpose: Read or set gain (temperature span) information.

Return value: Read returns `IrFileGain`; write returns `IrFileErrorCode`.

### Palette and Image Display Parameters

#### `getPalette()` / `getPaletteV2()` / `setPalette(palette)`

Purpose: Read or set the palette.

Return value: Read returns `IrFilePalette` or `IrFilePalette_v2`; write returns `IrFileErrorCode`.

#### `getPresetPalettes()` / `getPresetPaletteImage(refNo, offset)` / `getPaletteImage(offset)`

Purpose: Read the preset palette list, or generate a palette color-bar image.

Return value: `IrFilePresetPalettes` or `IrFileImage`.

#### `getVtParams()` / `setVtParams(value)` / `getVtParamsV2()` / `setVtParamsV2(value)`

Purpose: Read or set image display parameters. The V2 APIs return a buffer or an image parameter object depending on the ISP type.

Return value: Read returns `IrFileBuffer` or `IrFileVtParamsV2`; write returns `IrFileErrorCode`.

#### `getIspType()` / `setIspType(ispType)`

Purpose: Read or set the image ISP type, such as NEMO or HOLLY.

Return value: Read returns `IrFileIspType`; write returns `IrFileErrorCode`.

#### `getColorDistType()` / `setColorDistType(colorDistType)`

Purpose: Read or set the color distribution type, such as temperature-linear or AD-linear.

Return value: Read returns `IrFileEnumColorDistType`; write returns `IrFileErrorCode`.

#### `setRatotion(rotation)` / `setMirrorType(mirrorType)` / `getImageEnh()` / `setImageEnh(imageEnh)`

Purpose: Rotate the image, mirror/flip the image, or read and set image detail enhancement.

Return value: `IrFileErrorCode` or `IrFileImageEnh`.

### Raw Frames, Thermal Image Generation, and Live Preview

#### `IrFileFrame`

Purpose: The full-radiometric frame data structure received by IRtekFileSDK.

Key fields: `buffer` is the AD value array; `length` is the number of AD values; `timeStamp` is the frame timestamp.

Relationship with UVC: Can be filled directly from `UVCFrameRawData.adValues`. If only a byte array is available, convert every two bytes in little-endian order into one `short`.

#### `appendFrame(frame)`

Purpose: Append one full-radiometric frame to the IRtekFileSDK context.

Parameters: `frame` is an `IrFileFrame`.

Return value: `IrFileErrorCode`.

When to call: After each UVC raw frame is received during live preview.

#### `getFrame(index)` / `getFrameCount()` / `getFrameIndex()` / `setFrameIndex(index)`

Purpose: Read the total frame count, read a frame by index, or read/set the current frame index.

Return value: `IrFileFrame`, `IrFileFrameCount`, `IrFileFrameIndex`, or `IrFileErrorCode`.

#### `getFrameRate()` / `setFrameRate(frameRate)` / `getCompressType()` / `setCompressType(compressType)`

Purpose: Read or set the thermal video frame rate and intra-frame compression method.

Return value: Read returns a frame rate or compression type object; write returns `IrFileErrorCode`.

#### `getThermalImage(offset)`

Purpose: Generate a thermal image based on the current thermal imaging file and the current frame.

Parameters: `offset` is the row byte-alignment offset, usually `0`.

Return value: `IrFileImage`.

#### `getThermalImageV2(colorSpace, offset)`

Purpose: Generate a thermal image based on the current thermal imaging file and the current frame, using the specified color space.

Parameters: `colorSpace` is the color space; commonly `1` for BGR and `2` for RGBA. `offset` is usually `0`.

Return value: `IrFileImage`.

#### `getThermalImageFromFrame(frame, colorSpace, offset)`

Purpose: Generate a thermal image directly from one full-radiometric frame. This Java API corresponds to the native `ir_file_get_thermal_image_from_frame()`.

Parameters: `frame` is an `IrFileFrame`; `colorSpace` commonly uses `2` for RGBA; `offset` is the row byte-alignment offset, usually `0`.

Return value: `IrFileImage`. On success, `data` is the image byte array, which can be converted to an Android `Bitmap` using the frame width and height.

When to call: Recommended for real-time UVC preview. The Demo flow is `UVCFrameRawData` -> `IrFileFrame` -> `getThermalImageFromFrame()` -> `Bitmap`.

### Spot, Line, and Area Temperature Measurement

#### `getTempSpot(point)`

Purpose: Read the temperature at a specified point.

Parameters: `IrFileTempPoint`, where `point.x` and `point.y` are thermal image coordinates; `emissivity`, `distance`, and `reflTemp` can set local measurement parameters.

Return value: `IrFileTemp`; on success, `temp` is the temperature value.

#### `getTempRect(rect)` / `getTempEllipse(ellipse)`

Purpose: Read the temperature array within a rectangle or ellipse region.

Parameters: `IrFileTempRect`, which contains region coordinates, width/height, and local measurement parameters.

Return value: `IrFileTemps`; on success, `temps` is the temperature array, from which max, min, or average temperature can be calculated.

#### `getTempPolyline(polyline)` / `getTempPolygon(polygon)`

Purpose: Read the temperature array for a polyline or polygon region.

Parameters: `IrFileTempPoints`, which contains a point list and local measurement parameters.

Return value: `IrFileTemps`.

### Measurement Markers, Temperature Difference, and Alarms

#### `getMarkerNames()` / `getMarker(name)` / `setMarker(marker)` / `removeMarker(name)` / `renameMarker(sourceName, targetName)`

Purpose: Read, write, delete, or rename measurement markers such as points, lines, rectangles, ellipses, and polygons.

Return value: Read returns `IrFileMarkerNames` or `IrFileMarker`; write and delete return `IrFileErrorCode`.

#### `getIndexMarker(index, name)`

Purpose: Read a measurement marker by frame index.

Return value: `IrFileMarker`.

#### `getDeltaNames()` / `getDelta(name)` / `setDelta(delta)` / `removeDelta(deltaName)` / `getIndexDelta(index, name)`

Purpose: Read, write, or delete temperature-difference markers, or read a temperature-difference marker by frame.

Return value: A name list, a delta object, a delta value, or an error code.

#### `getAlarm()` / `getAlarmV2()` / `setAlarm(alarm)`

Purpose: Read or set temperature measurement alarm information.

Return value: Read returns an alarm object; write returns `IrFileErrorCode`.

### Visible Light, Cover Image, Notes, and User Data

#### `getVisualImage()` / `setVisualImage(value)` / `getCoverImage()` / `setCoverImage(value)`

Purpose: Read or write the visible-light image and the cover image.

Return value: Read returns `IrFileBuffer`; write returns `IrFileErrorCode`.

#### `getNote()` / `setNote(value)` / `getAudio()` / `setAudio(value)`

Purpose: Read or write text notes and audio data.

Return value: Read returns `IrFileBuffer` or `IrFileAudio`; write returns `IrFileErrorCode`.

#### `getKeys()` / `getUserData(key)` / `setUserData(value)`

Purpose: Read the user-data key list, read user data for a specified key, or write user data.

Return value: `IrFileKeys`, `IrFileKvPair`, or `IrFileErrorCode`.

#### `getGeoLocation()` / `setGeoLocation(geoLocation)`

Purpose: Read or write geographic location information.

Return value: Read returns `IrFileGeoLocation`; write returns `IrFileErrorCode`.

#### `getZoomInfo()` / `setZoomInfo(zoomInfo)` / `getViewInfo()` / `setViewInfo(value)` / `getMappingInfo()` / `setMappingInfo(mappingInfo)`

Purpose: Read or set digital zoom, display information, and thermal region mapping information.

Return value: Read returns the corresponding information object; write returns `IrFileErrorCode`.

### Error Codes and Constants

#### `IrFileErrorCode`

Purpose: All IRtekFileSDK return objects inherit or contain `errorCode`, used to determine whether a call succeeded.

Common values: `IrFileEcOk = 0` means success; `IrFileEcFail = -1` means failure; `IrFileEcMemory = -2` means memory allocation failed; `IrFileEcPointer = -3` means a null pointer; `IrFileEcInvalidKey = -4` means the index does not exist; `IrFileEcNotSupported = -5` means the feature is not supported.

#### `IrFileEnumFileType`

Purpose: Create or identify a file type.

Common values: `IrFileThermalpicture` for a thermal image; `IrFileThermalVideo` for a thermal video; `IrFileVisualPicture` for a visible-light image; `IrFileGridThermalPicture` for a power-grid thermal image.

#### `IrFileEnumColorDistType`

Purpose: Set the color distribution method.

Common values: `IrFileTWB`, `IrFileTempLinear`, `IrFileAdLinear`.

#### `IrFileEnumRotateType` / `IrFileEnumMirrorType`

Purpose: Set image rotation and mirroring.

## 3. Recommended Responsibility Boundary

- `IRtekUVCSDK.aar`: Handles device search, USB login, stream callbacks, and device parameters.
- `IRtekFileSDK.aar`: Converts UVC raw frames into thermal images and calculates spot, line, and area temperatures.
- Application layer: Handles permission requests, UI display, lifecycle management, and error prompts.
