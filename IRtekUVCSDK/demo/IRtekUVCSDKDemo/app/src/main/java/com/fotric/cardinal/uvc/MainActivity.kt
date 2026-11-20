package com.fotric.cardinal.uvc

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fotric.cardinal.uvc.ui.theme.UVCAndroidSdkDemoTheme
import com.fotric.holley.uvc.FrameCallback
import com.fotric.holley.uvc.UVCDeviceInfo
import com.fotric.holley.uvc.UVCManager
import com.fotric.holley.uvc.UVCFrameRawData
import kotlinx.coroutines.*

private const val UPGRADE_A_ASSET_NAME = "upgrade_a.bin"

internal fun formatUsbId(value: Int): String = "0x%04X".format(value and 0xFFFF)

internal fun formatUsbId(value: Short): String = formatUsbId(value.toInt())

class MainActivity : ComponentActivity() {
    companion object {
        private const val ACTION_USB_PERMISSION = "com.fotric.cardinal.uvc.USB_PERMISSION"
    }

    private lateinit var usbManager: UsbManager

    // 协程作用域，用于后台任务
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /** 是否已获得 USB 权限（或无设备需授权），用于控制是否初始化 UVC SDK */
    private val usbPermissionReady = mutableStateOf(false)

    /** 收到 USB 权限拒绝且未看到弹窗时提示用户（如先授予相机权限） */
    private val usbPermissionDeniedHint = mutableStateOf(false)

    /** UVC SDK 是否已初始化 */
    private var uvcInited = false

    /** 相机权限通过后要请求 USB 权限的设备（部分设备需先有相机权限才弹 USB 对话框） */
    private var pendingDeviceForUsbAfterCamera: UsbDevice? = null

    private val requestCameraLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) pendingDeviceForUsbAfterCamera?.let { requestUsbPermissionInternal(it) }
            pendingDeviceForUsbAfterCamera = null
        }

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                device?.let { requestUsbPermission(it) }
            } else if (ACTION_USB_PERMISSION == intent.action) {
                // 打印所有 extras，便于排查 EXTRA_PERMISSION_GRANTED 为 false 的原因
                intent.extras?.keySet()?.forEach { key ->
                    val v = intent.extras?.get(key)
                    android.util.Log.d(
                        "MainActivity",
                        "USB_PERMISSION extra: key=$key type=${v?.javaClass?.simpleName} value=$v"
                    )
                }
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                // 兼容部分机型用 Integer 1/0 表示
                val grantedCompat = intent.extras?.get(UsbManager.EXTRA_PERMISSION_GRANTED)?.let {
                    when (it) {
                        is Boolean -> it; is Int -> it != 0; else -> granted
                    }
                } ?: granted
                android.util.Log.d(
                    "MainActivity",
                    "EXTRA_PERMISSION_GRANTED: raw=$granted key=${UsbManager.EXTRA_PERMISSION_GRANTED} resolved=$grantedCompat"
                )
                if (grantedCompat) {
                    device?.let {
                        android.util.Log.d(
                            "MainActivity",
                            "USB permission granted: VID=${formatUsbId(it.vendorId)}, PID=${formatUsbId(it.productId)}"
                        )
                    }
                    usbPermissionReady.value = true
                    initUvcIfNeeded()
                    refreshDeviceList()
                } else {
                    android.util.Log.w(
                        "MainActivity",
                        "USB permission denied (user denied or extra missing)"
                    )
                    usbPermissionDeniedHint.value = true
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化 USB Manager
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        UVCManager.enableLog(true)
        // 注册 USB 广播接收器
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                usbPermissionReceiver,
                filter,
                android.content.Context.RECEIVER_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(usbPermissionReceiver, filter)
        }

        // 先处理 USB 权限：有设备则先请求权限，无设备或已有权限再初始化 UVC
        val deviceList = usbManager.deviceList
        val needPermission = deviceList.values.any { !usbManager.hasPermission(it) }

        when {
            deviceList.isEmpty() -> {
                // 无设备，直接视为就绪并初始化 UVC（插上设备后再请求权限）
                usbPermissionReady.value = true
                initUvcIfNeeded()
            }

            needPermission -> {
                // 有设备但未授权：先请求权限，等授权后再初始化 UVC
                usbPermissionReady.value = false
                deviceList.values.firstOrNull { !usbManager.hasPermission(it) }?.let {
                    requestUsbPermission(it)
                }
                // 若通过 Intent 带入了设备，也请求该设备权限
                intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.let {
                    requestUsbPermission(it)
                }
            }

            else -> {
                // 已有权限，直接就绪并初始化 UVC
                usbPermissionReady.value = true
                initUvcIfNeeded()
            }
        }

        setContent {
            UVCAndroidSdkDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UVCContent(
                        modifier = Modifier.padding(innerPadding),
                        usbPermissionReady = usbPermissionReady.value,
                        usbPermissionDeniedHint = usbPermissionDeniedHint.value,
                        onRefreshDeviceList = { refreshDeviceList() },
                        onRequestUsbPermission = { device -> requestUsbPermission(device) },
                        onEnterDevicePage = { enterDevicePage() },
                        usbManager = usbManager
                    )
                }
            }
        }
    }

    /** 在已获得 USB 权限的前提下初始化 UVC SDK（仅执行一次） */
    private fun initUvcIfNeeded() {
        if (uvcInited) return
        if (!UVCManager.init().isSuccess) {
            android.util.Log.e("MainActivity", "Failed to initialize UVC SDK")
            return
        }
        uvcInited = true
        UVCManager.enableLog(true)
        android.util.Log.d("MainActivity", "UVC SDK initialized (after USB permission)")
        val version = UVCManager.getSDKVersion().getOrNull()
        version?.let {
            android.util.Log.d(
                "MainActivity",
                "UVC SDK Version: $it"
            )
        }
        refreshDeviceList()
    }

    override fun onResume() {
        super.onResume()
        // 应用在前台时，检查是否有未授权的 USB 设备并请求权限
        // （部分机型/场景下插入设备时 ACTION_USB_DEVICE_ATTACHED 不会发给动态注册的接收器）
        usbManager.deviceList.values
            .firstOrNull { !usbManager.hasPermission(it) }
            ?.let { requestUsbPermission(it) }
    }

    /** 进入设备连接界面 */
    private fun enterDevicePage() {
        usbPermissionReady.value = true
        initUvcIfNeeded()
        refreshDeviceList()
    }

    override fun onDestroy() {
        coroutineScope.cancel() // 取消所有协程
        super.onDestroy()
        try {
            unregisterReceiver(usbPermissionReceiver)
        } catch (e: Exception) {
            // 忽略未注册的异常
        }
        UVCManager.cleanup()
    }

    /**
     * 请求 USB 设备权限（入口）。部分设备/ROM 在 USB 为摄像头时需先有相机权限才会弹出 USB 授权对话框，故先请求相机权限再请求 USB。
     */
    private fun requestUsbPermission(device: UsbDevice) {
        usbPermissionDeniedHint.value = false
        if (usbManager.hasPermission(device)) {
            android.util.Log.d(
                "MainActivity",
                "Already has permission for device: ${device.deviceName}"
            )
            usbPermissionReady.value = true
            initUvcIfNeeded()
            refreshDeviceList()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingDeviceForUsbAfterCamera = device
            requestCameraLauncher.launch(Manifest.permission.CAMERA)
            android.util.Log.d(
                "MainActivity",
                "Requesting CAMERA permission first (required on some devices for USB camera dialog)"
            )
            return
        }
        requestUsbPermissionInternal(device)
    }

    /**
     * 实际发起 USB 权限请求。
     * Android U+ (API 34+) 要求：FLAG_MUTABLE 必须配合显式 Intent，故设置 setPackage。
     */
    private fun requestUsbPermissionInternal(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            usbPermissionReady.value = true
            initUvcIfNeeded()
            refreshDeviceList()
            return
        }
        val intent = Intent(ACTION_USB_PERMISSION).apply {
            setPackage(packageName) // 显式 Intent，满足 U+ 对 FLAG_MUTABLE 的安全要求
        }
        val flags = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }

            else -> android.app.PendingIntent.FLAG_UPDATE_CURRENT
        }
        val permissionIntent = android.app.PendingIntent.getBroadcast(this, 0, intent, flags)
        val requested = usbManager.requestPermission(device, permissionIntent)
        android.util.Log.d(
            "MainActivity",
            "Requesting USB permission for device: ${device.deviceName}, requestPermission returned: $requested"
        )
    }

    /**
     * 刷新设备列表（Android 上从 UsbManager 取列表）
     */
    private fun refreshDeviceList() {
        val devices = UVCManager.getUVCList(usbManager)
        android.util.Log.d("MainActivity", "Refreshed: Found ${devices.size} UVC devices")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UVCContent(
    modifier: Modifier = Modifier,
    usbPermissionReady: Boolean,
    usbPermissionDeniedHint: Boolean = false,
    onRefreshDeviceList: () -> Unit,
    onRequestUsbPermission: (UsbDevice) -> Unit,
    onEnterDevicePage: () -> Unit,
    usbManager: UsbManager
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val frameProcessor = remember(context) { IRtekFileFrameProcessor(context) }
    val state = remember { UvcUiState() }
    val strings = remember(state.language) { appStringsOf(state.language) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSettings by remember { mutableStateOf(false) }

    fun releaseStream() {
        state.currentDevice?.let { dev ->
            try {
                UVCManager.setStreamCallback(dev, null, null)
            } catch (_: Exception) {
            }
        }
    }

    fun readAcousticDeviceText(dev: UVCDeviceInfo): String {
        val camera = UVCManager.getDeviceInfo(dev).getOrNull()?.let {
            "Model: ${it.model.trim()}, Serial: ${it.serialNo.trim()}"
        }
        val sensor = AcousticPreview.readSensorInfo(dev)
        val freq = AcousticPreview.readFreqRange(dev).second?.let { "Frequency: ${describeAcousticFreq(it)}" }
        val size = AcousticPreview.readResolution(dev)?.let { "Acoustic size: ${it.first}x${it.second}" }
        return listOfNotNull(camera, sensor, freq, size).joinToString("\n").ifEmpty { "-" }
    }

    fun runDeviceCommand(
        name: String,
        block: suspend (UVCDeviceInfo) -> String
    ) {
        val dev = state.currentDevice
        if (dev == null) {
            state.appendApiResult("$name: ${strings.connectDeviceFirst}")
            return
        }
        scope.launch {
            val detail = try {
                block(dev)
            } catch (e: Exception) {
                "${strings.exceptionPrefix}: ${e.message}"
            }
            state.appendApiResult("$name: $detail")
        }
    }

    fun connectFirstDevice() {
        val devices = UVCManager.getUVCList(usbManager)
        if (devices.isEmpty()) return
        val first = usbManager.deviceList.values.firstOrNull {
            usbManager.hasPermission(it)
        } ?: usbManager.deviceList.values.firstOrNull()
        if (first != null) {
            val connection = usbManager.openDevice(first)
            if (connection != null) {
                val fd = connection.fileDescriptor
                val deviceInfoForUvc = UVCDeviceInfo(
                    first.deviceName,
                    first.vendorId.toShort(),
                    first.productId.toShort()
                )
                if (UVCManager.loginByFd(fd, deviceInfoForUvc).isSuccess) {
                    state.usbConnection = connection
                    state.currentDevice = deviceInfoForUvc
                    state.appendApiResult(
                        strings.connected(
                            formatUsbId(first.vendorId),
                            formatUsbId(first.productId)
                        ) + " (${if (deviceInfoForUvc.isAcousticDevice()) strings.deviceModeAcoustic else strings.deviceModeThermal})"
                    )
                    scope.launch {
                        if (deviceInfoForUvc.isAcousticDevice()) {
                            state.deviceInfo = withContext(Dispatchers.IO) {
                                readAcousticDeviceText(deviceInfoForUvc)
                            }
                        } else {
                            val info = withContext(Dispatchers.IO) {
                                UVCManager.getDeviceInfo(deviceInfoForUvc).getOrNull()
                            }
                            val lensInfo = withContext(Dispatchers.IO) {
                                UVCManager.getLensInfo(deviceInfoForUvc).getOrNull()
                            }
                            info?.let {
                                state.deviceInfo =
                                    "Model: ${it.model.trim()}, Serial: ${it.serialNo.trim()}"
                            }
                            lensInfo?.let {
                                state.deviceInfo +=
                                    "  LensName: ${it.name.trim()}, RangesCount: ${it.rangesCount}"
                            }
                        }
                    }
                } else {
                    connection.close()
                    state.appendApiResult(strings.connectFailed)
                }
            }
        }
    }

    fun disconnectDevice() {
        if (state.isStreaming) {
            releaseStream()
            state.isStreaming = false
            state.clearPreview()
        }
        state.currentDevice?.let { dev ->
            try {
                val ok = UVCManager.logout(dev).isSuccess
                state.appendApiResult(strings.loggedOut(ok))
            } catch (e: Exception) {
                state.appendApiResult(strings.logoutException(e.message))
            }
        }
        try { state.usbConnection?.close() } catch (_: Exception) { }
        state.usbConnection = null
        state.currentDevice = null
        state.deviceInfo = null
        state.clearPreview()
        try { state.deviceCount = UVCManager.getUVCList(usbManager).size } catch (_: Exception) { }
    }

    fun startPreview() {
        val device = state.currentDevice ?: return
        if (device.isAcousticDevice()) {
            scope.launch {
                state.streamError = null
                state.lastFrameTimeMs = System.currentTimeMillis()
                state.isStreaming = true
                val ok = withContext(Dispatchers.IO) {
                    UVCManager.setStreamCallback(
                        device,
                        null,
                        object : FrameCallback {
                            override fun onFrameReceived(frame: UVCFrameRawData) {
                                if (!state.isStreaming) return
                                val rendered = UVCManager
                                    .getAcousticImageFromFrame(device, frame)
                                    .getOrNull()
                                val bitmap = rendered?.bitmap ?: return
                                val summary = describeAcousticSummary(rendered.info?.summary)
                                mainHandler.post {
                                    state.lastFrameTimeMs = System.currentTimeMillis()
                                    state.previewBitmap = bitmap
                                    state.thermalFrameText = summary
                                    state.streamError = null
                                }
                            }
                        }
                    ).isSuccess
                }
                if (ok) {
                    state.appendApiResult(strings.previewAcousticStarted)
                } else {
                    releaseStream()
                    state.isStreaming = false
                    state.appendApiResult(strings.setStreamCallbackFailed)
                }
            }
            return
        }
        scope.launch {
            val frameList = withContext(Dispatchers.IO) {
                UVCManager.getFrameList(device).getOrNull() ?: emptyList()
            }
            if (frameList.isEmpty()) {
                state.appendApiResult(strings.noFrameFormat)
                return@launch
            }
            val frameInfo = frameList.firstOrNull {
                it.width.toInt() == 320 && it.height.toInt() == 244
            } ?: frameList.firstOrNull {
                it.width.toInt() == 320
            } ?: frameList.first()

            state.streamError = null
            state.thermalFrameText = strings.initializingFileSdk
            state.lastFrameTimeMs = System.currentTimeMillis()

            val thermalParams = withContext(Dispatchers.IO) {
                UVCManager.getThermalParams(device).getOrNull()
            }
            val lut = withContext(Dispatchers.IO) {
                UVCManager.getThermalLut(device).getOrNull()
            }
            ThermalLut.fromDevice(lut)?.let { state.thermalLut = it }
            val setupResult = withContext(Dispatchers.IO) {
                frameProcessor.configure(
                    frameInfo.width.toInt(),
                    frameInfo.height.toInt(),
                    thermalParams,
                    lut
                )
            }
            state.thermalFrameText = setupResult.message
            if (!setupResult.success) {
                state.streamError = setupResult.message
                state.appendApiResult(setupResult.message)
            }
            val flipMode = withContext(Dispatchers.IO) {
                UVCManager.getImageFlip(device).getOrNull()
            }
            flipMode?.let {
                state.selectedFlipMode = it.flipMode
                frameProcessor.applyImageFlipMode(it.flipMode)
            }

            state.isStreaming = true
            val ok = UVCManager.setStreamCallback(
                device,
                frameInfo,
                object : FrameCallback {
                    override fun onFrameReceived(frame: UVCFrameRawData) {
                        if (!state.isStreaming) return
                        if (frame.shutterState == 1) {
                            mainHandler.post {
                                state.lastFrameTimeMs = System.currentTimeMillis()
                                state.thermalFrameText =
                                    strings.shuttering(frame.frameNo, frame.lutLength)
                                state.streamError = null
                            }
                            return
                        }
                        val image = UVCManager.getThermalImageFromFrame(device, frame)
                        val measured = if (setupResult.success) {
                            frameProcessor.measureCenter(frame)
                        } else {
                            null
                        }
                        val frameLut = ThermalLut.fromFrame(frame)
                        if (frameLut != null) state.thermalLut = frameLut
                        val gain = image.getOrNull()?.info?.gain
                        val lut = frameLut ?: state.thermalLut
                        val gainText = if (gain != null && lut != null) {
                            "gain ${formatAcousticFloat(lut.adToCelsius(gain.min))}-${formatAcousticFloat(lut.adToCelsius(gain.max))} °C mode=${gain.mode}"
                        } else {
                            null
                        }
                        val frameDetail = listOfNotNull(measured?.message, gainText)
                            .joinToString(", ")
                            .ifEmpty { image.message }
                        mainHandler.post {
                            state.lastFrameTimeMs = System.currentTimeMillis()
                            state.thermalFrameText =
                                strings.stableFrame(
                                    frame.frameNo,
                                    frame.shutterState,
                                    frame.lutLength,
                                    frameDetail
                                )
                            image.getOrNull()?.bitmap?.let { state.previewBitmap = it }
                            state.centerMarkerTemp = measured?.centerTemp
                            state.streamError = if (image.isSuccess) null else image.message
                        }
                    }
                }).isSuccess
            if (ok) {
                state.appendApiResult(
                    strings.previewStarted(
                        frameInfo.width.toInt(),
                        frameInfo.height.toInt(),
                        frameInfo.format
                    )
                )
            } else {
                releaseStream()
                state.isStreaming = false
                state.thermalFrameText = ""
                state.appendApiResult(strings.setStreamCallbackFailed)
            }
        }
    }

    fun stopPreview() {
        releaseStream()
        state.isStreaming = false
        state.clearPreview()
        state.streamError = null
        state.appendApiResult(strings.previewStopped)
    }

    fun refreshDeviceInfo() {
        val dev = state.currentDevice ?: return
        scope.launch {
            if (dev.isAcousticDevice()) {
                val info = withContext(Dispatchers.IO) {
                    readAcousticDeviceText(dev)
                }
                state.deviceInfo = info
                state.appendApiResult("${strings.deviceModeAcoustic}: $info")
            } else {
                val resolution = withContext(Dispatchers.IO) {
                    UVCManager.getThermalResolution(dev).getOrNull()
                }
                val lens = withContext(Dispatchers.IO) {
                    UVCManager.getLensInfo(dev).getOrNull()
                }
                val ranges = lens?.ranges
                    ?.take(lens.rangesCount.toInt())
                    ?.joinToString { "[${it.id}: ${it.min}-${it.max}]" }
                    ?: "-"
                state.appendApiResult(
                    strings.deviceInfoRefreshed(
                        "${resolution?.width ?: "-"}x${resolution?.height ?: "-"}",
                        ranges
                    )
                )
            }
        }
    }

    fun getPalette() {
        val dev = state.currentDevice
        if (dev == null) {
            state.appendApiResult(strings.connectDeviceFirst)
            return
        }
        scope.launch {
            val acoustic = dev.isAcousticDevice()
            val loaded = withContext(Dispatchers.IO) {
                AcousticPreview.readPresetPalettes(dev, acoustic)
            }
            val presets = loaded?.takeIf { it.isNotEmpty() } ?: fallbackPalettes(acoustic)
            state.paletteOptions = presets.map { it.first to "${it.second}(${it.first})" }
            val current = withContext(Dispatchers.IO) {
                AcousticPreview.readPalette(dev, acoustic)
            }
            if (current != null) {
                state.selectedPaletteRefNo = current.refNo
                state.paletteInverted = current.inverted.toInt() != 0
                state.paletteAlpha = current.alpha.coerceIn(0, 100)
            } else if (state.paletteOptions.isNotEmpty()) {
                state.selectedPaletteRefNo = state.paletteOptions.first().first
            }
            state.appendApiResult(strings.getPalette("count=${presets.size}, ref=${state.selectedPaletteRefNo}"))
        }
    }

    fun applyPalette() {
        val dev = state.currentDevice
        if (dev == null) {
            state.appendApiResult(strings.connectDeviceFirst)
            return
        }
        scope.launch {
            val acoustic = dev.isAcousticDevice()
            val code = withContext(Dispatchers.IO) {
                AcousticPreview.applyPalette(
                    dev,
                    acoustic,
                    state.selectedPaletteRefNo,
                    state.paletteInverted,
                    state.paletteAlpha
                )
            }
            state.appendApiResult(strings.applyPalette("code=$code, ref=${state.selectedPaletteRefNo}, inverted=${state.paletteInverted}, alpha=${state.paletteAlpha}"))
        }
    }

    fun runBatchTest() {
        if (state.isBatchTesting) return
        val dev = state.currentDevice ?: return
        state.isBatchTesting = true
        scope.launch {
            state.apiResultText = strings.batchTestStart
            val (passCount, totalCount) = runBatchApiTest(dev, strings, frameProcessor) { name, ok, detail ->
                state.appendApiResult("${if (ok) "PASS" else "FAIL"} | $name | $detail")
            }
            val header = if (passCount == totalCount) {
                strings.batchTestPass(passCount, totalCount)
            } else {
                strings.batchTestFail(passCount, totalCount)
            }
            state.appendApiResult(header)
            state.isBatchTesting = false
        }
    }

    LaunchedEffect(state.language) {
        frameProcessor.setUseChinese(state.language == AppLanguage.Chinese)
    }

    LaunchedEffect(usbPermissionReady) {
        if (!usbPermissionReady) return@LaunchedEffect
        state.uvcSdkVersion = UVCManager.getSDKVersion().getOrNull()
        state.fileSdkVersion = withContext(Dispatchers.IO) {
            frameProcessor.getFileSdkVersion()
        }
        state.deviceCount = UVCManager.getUVCList(usbManager).size
    }

    // 断开设备时清除预览
    LaunchedEffect(state.currentDevice) {
        if (state.currentDevice == null) {
            state.clearPreview()
            state.isStreaming = false
            state.streamError = null
        }
    }

    // 预览中：定时检测是否超过一定时间未收到帧（帧中断）
    LaunchedEffect(state.isStreaming) {
        if (!state.isStreaming) return@LaunchedEffect
        val timeoutMs = 3000L
        val checkIntervalMs = 1000L
        while (state.isStreaming) {
            delay(checkIntervalMs)
            if (!state.isStreaming) break
            val now = System.currentTimeMillis()
            if (state.lastFrameTimeMs > 0L && (now - state.lastFrameTimeMs) > timeoutMs) {
                val message = strings.streamTimeout(timeoutMs / 1000)
                state.streamError = message
                state.appendApiResult(message)
                releaseStream()
                state.isStreaming = false
                state.clearPreview()
                break
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部栏：标题 + 语言切换（系统栏 inset 由 Scaffold 的 innerPadding 统一处理）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 12.dp)
        ) {
            Text(
                text = "UVC SDK Demo",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleLarge
            )
            LanguageSelector(
                language = state.language,
                onLanguageChange = { state.language = it },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        if (!usbPermissionReady) {
            UsbPermissionPanel(
                strings = strings,
                permissionDeniedHint = usbPermissionDeniedHint,
                onRequestPermission = {
                    usbManager.deviceList.values.forEach { device ->
                        if (!usbManager.hasPermission(device)) {
                            onRequestUsbPermission(device)
                        }
                    }
                },
                onEnterDevicePage = onEnterDevicePage,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
            return@Column
        }

        // 声像/热像设备可用指令不同，连接的设备类型变化时修正选中的指令项
        if (state.isAcoustic && state.selectedDeviceCommandTab != 8 && state.selectedDeviceCommandTab != 9) {
            state.selectedDeviceCommandTab = 9
        } else if (!state.isAcoustic && (state.selectedDeviceCommandTab == 8 || state.selectedDeviceCommandTab == 9)) {
            state.selectedDeviceCommandTab = 0
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // 视频流占满屏幕主要区域
            PreviewPanel(
                bitmap = state.previewBitmap,
                isAcoustic = state.isAcoustic,
                centerTemp = state.centerMarkerTemp,
                deviceConnected = state.currentDevice != null,
                isStreaming = state.isStreaming,
                streamError = state.streamError,
                frameText = state.thermalFrameText,
                strings = strings,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // 底部操作栏：预览开关 + 打开设置面板
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { if (state.isStreaming) stopPreview() else startPreview() },
                    enabled = state.currentDevice != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.isStreaming) strings.stopPreview else strings.startPreview)
                }
                OutlinedButton(
                    onClick = { showSettings = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.settingsTitle)
                }
            }
        }
    }

    // 底部上拉设置面板：展开后接近全屏，各功能模块有完整展示空间
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState
        ) {
            val visibleMainTabs = listOf(
                0 to strings.tabDeviceInfo,
                1 to strings.tabDeviceControl,
                2 to strings.tabPalette,
                3 to strings.tabVersion,
                4 to strings.tabLog
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                ScrollableTabRow(
                    selectedTabIndex = visibleMainTabs.indexOfFirst { it.first == state.selectedMainTab }.coerceAtLeast(0),
                    edgePadding = 0.dp
                ) {
                    visibleMainTabs.forEach { (index, title) ->
                        Tab(
                            selected = state.selectedMainTab == index,
                            onClick = { state.selectedMainTab = index },
                            text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .imePadding()
                        .padding(top = 10.dp)
                ) {
                    val tabContentModifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                    when (state.selectedMainTab) {
                        // 设备页：连接管理 + 设备信息
                        0 -> Column(
                            modifier = tabContentModifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ConnectionPanel(
                                deviceCount = state.deviceCount,
                                deviceConnected = state.currentDevice != null,
                                isAcoustic = state.isAcoustic,
                                isStreaming = state.isStreaming,
                                strings = strings,
                                onRefresh = {
                                    onRefreshDeviceList()
                                    state.deviceCount = UVCManager.getUVCList(usbManager).size
                                    state.appendApiResult(strings.refreshedDevices(state.deviceCount))
                                },
                                onRequestPermission = {
                                    usbManager.deviceList.values.forEach { device ->
                                        if (!usbManager.hasPermission(device)) {
                                            onRequestUsbPermission(device)
                                        }
                                    }
                                    state.appendApiResult(strings.requestedUsbPermission)
                                },
                                onConnect = { connectFirstDevice() },
                                onDisconnect = { disconnectDevice() },
                                onTogglePreview = { if (state.isStreaming) stopPreview() else startPreview() },
                                onGetResolution = {
                                    state.currentDevice?.let { dev ->
                                        val resolution = UVCManager.getThermalResolution(dev).getOrNull()
                                        state.appendApiResult("Resolution: ${resolution?.width ?: "-"}x${resolution?.height ?: "-"}")
                                    }
                                }
                            )
                            DeviceInfoPanel(
                                state = state,
                                strings = strings,
                                onRefreshInfo = { refreshDeviceInfo() }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        1 -> DeviceControlPanel(
                            state = state,
                            strings = strings,
                            frameProcessor = frameProcessor,
                            onRunCommand = { name, block -> runDeviceCommand(name, block) },
                            modifier = tabContentModifier.verticalScroll(rememberScrollState())
                        )

                        2 -> PalettePanel(
                            state = state,
                            strings = strings,
                            onGetPalette = { getPalette() },
                            onSetPalette = { applyPalette() },
                            modifier = tabContentModifier.verticalScroll(rememberScrollState())
                        )

                        3 -> VersionPanel(
                            uvcSdkVersion = state.uvcSdkVersion,
                            fileSdkVersion = state.fileSdkVersion,
                            strings = strings,
                            modifier = tabContentModifier.verticalScroll(rememberScrollState())
                        )

                        4 -> LogPanel(
                            state = state,
                            strings = strings,
                            onRunBatchTest = { runBatchTest() },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSelector(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }) {
            Text(language.label, style = MaterialTheme.typography.labelLarge)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppLanguage.values().forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label, style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        onLanguageChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    UVCAndroidSdkDemoTheme {
        Greeting("Android")
    }
}
