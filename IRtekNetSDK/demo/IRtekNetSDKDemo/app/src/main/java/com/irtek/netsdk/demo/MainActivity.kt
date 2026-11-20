package com.irtek.netsdk.demo

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irtek.netsdk.NativeSDK
import com.irtek.netsdk.NetSDKManager
import com.irtek.netsdk.NetSDKResult
import com.irtek.netsdk.demo.ui.theme.IRtekNetSDKDemoTheme
import kotlinx.coroutines.*
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/** 厂商约定：接口返回值/结果码 >= 0 表示成功（厂商 AAR 的 isSuccess 仍按 ==0，Demo 统一用本判断） */
private fun NetSDKResult<*>.isOk(): Boolean = code >= 0

private fun sdkOk(code: Int): Boolean = code >= 0

/** net_enum_marker_type：点 / 多边形(3~6点) / 折线(2点) */
private const val MARKER_TYPE_SPOT = 1
private const val MARKER_TYPE_POLYGON = 5
private const val MARKER_TYPE_POLYLINE = 6
private const val MARKER_POLYGON_MIN_POINTS = 3
private const val MARKER_POLYGON_MAX_POINTS = 6

/** 旧版矩形(3)/椭圆(4)按多边形处理，其余原样返回 */
private fun normalizeMarkerType(type: Int): Int = when (type) {
    3, 4 -> MARKER_TYPE_POLYGON
    else -> type
}

data class DrawMarker(
    var id: Int = 0,
    var type: Int = MARKER_TYPE_SPOT,
    var name: String = "",
    var points: MutableList<Pair<Int, Int>> = mutableListOf()
) {
    fun toJson(): org.json.JSONObject {
        val sdkType = normalizeMarkerType(type)
        val sdkPoints = when (sdkType) {
            MARKER_TYPE_SPOT -> points.take(1)
            MARKER_TYPE_POLYLINE -> points.take(2)
            MARKER_TYPE_POLYGON -> points.take(MARKER_POLYGON_MAX_POINTS)
            else -> points
        }
        val j = org.json.JSONObject()
        j.put("id", id)
        j.put("type", sdkType)
        j.put("name", name)
        j.put("local_params", 0)
        j.put("refl_temp", 0.0)
        j.put("distance", 0.0)
        j.put("emissivity", 0.0)
        val pts = org.json.JSONArray()
        sdkPoints.forEach { (x, y) ->
            pts.put(org.json.JSONObject().put("x", x).put("y", y))
        }
        j.put("points", pts)
        j.put("points_count", sdkPoints.size)
        return j
    }
}

data class EditableAlarmRule(
    val markerName: String = "",
    val enabled: Int = 0,
    val alarmType: Int = 1,
    val alarmTemp: String = "0.0",
    val alarmDelay: String = "0",
    val warningTemp: String = "0.0",
    val warningDelay: String = "0",
    val alertTemp: String = "0.0",
    val alertDelay: String = "0",
) {
    companion object {
        fun fromJson(o: JSONObject, tempUnit: Int) = EditableAlarmRule(
            markerName = o.optString("marker_name", ""),
            enabled = o.optInt("enabled", 0),
            alarmType = o.optInt("alarm_type", 1),
            alarmTemp = formatTempValue(o.optDouble("alarm_temp", 0.0), tempUnit),
            alarmDelay = o.optInt("alarm_delay_time", 0).toString(),
            warningTemp = formatTempValue(o.optDouble("warning_temp", 0.0), tempUnit),
            warningDelay = o.optInt("warning_delay_time", 0).toString(),
            alertTemp = formatTempValue(o.optDouble("alert_temp", 0.0), tempUnit),
            alertDelay = o.optInt("alert_delay_time", 0).toString(),
        )

        /** 按标记补全时使用的默认规则（默认关闭，避免误触发） */
        fun defaultForMarker(name: String, tempUnit: Int) = EditableAlarmRule(
            markerName = name,
            enabled = 0,
            alarmType = 1,
            alarmTemp = formatTempValue(0.0, tempUnit),
            alarmDelay = "0",
            warningTemp = formatTempValue(0.0, tempUnit),
            warningDelay = "0",
            alertTemp = formatTempValue(0.0, tempUnit),
            alertDelay = "0",
        )
    }

    fun toJson(tempUnit: Int): JSONObject = JSONObject().apply {
        put("marker_name", markerName)
        put("enabled", enabled)
        put("alarm_type", alarmType)
        put("alarm_temp", parseTempInput(alarmTemp, tempUnit))
        put("alarm_delay_time", alarmDelay.toIntOrNull() ?: 0)
        put("warning_temp", parseTempInput(warningTemp, tempUnit))
        put("warning_delay_time", warningDelay.toIntOrNull() ?: 0)
        put("alert_temp", parseTempInput(alertTemp, tempUnit))
        put("alert_delay_time", alertDelay.toIntOrNull() ?: 0)
    }
}

/** 保留设备已有规则，并为当前标记列表中缺失的名称补默认规则 */
private fun mergeAlarmRulesWithMarkers(
    existing: List<EditableAlarmRule>,
    markerNames: List<String>,
    tempUnit: Int
): List<EditableAlarmRule> {
    val merged = existing.toMutableList()
    val known = merged.map { it.markerName }.toMutableSet()
    for (name in markerNames) {
        if (name.isBlank() || name in known) continue
        merged.add(EditableAlarmRule.defaultForMarker(name, tempUnit))
        known.add(name)
    }
    return merged
}

private fun tempUnitLabel(unit: Int): String = when (unit) {
    1 -> "K"
    2 -> "°F"
    else -> "°C"
}

private fun distUnitLabel(unit: Int): String = when (unit) {
    1 -> "ft"
    else -> "m"
}

private fun kelvinToDisplay(kelvin: Double, tempUnit: Int): Double = when (tempUnit) {
    1 -> kelvin
    2 -> (kelvin - 273.15) * 9.0 / 5.0 + 32.0
    else -> kelvin - 273.15
}

private fun displayToKelvin(display: Double, tempUnit: Int): Double = when (tempUnit) {
    1 -> display
    2 -> (display - 32.0) * 5.0 / 9.0 + 273.15
    else -> display + 273.15
}

private fun meterToDisplay(meters: Double, distUnit: Int): Double = when (distUnit) {
    1 -> meters / 0.3048
    else -> meters
}

private fun displayToMeter(display: Double, distUnit: Int): Double = when (distUnit) {
    1 -> display * 0.3048
    else -> display
}

private fun formatTempValue(value: Double, tempUnit: Int = 0): String =
    "%.1f".format(Locale.US, value)

private fun formatDistValue(value: Double, distUnit: Int = 0): String =
    "%.1f".format(Locale.US, value)

private fun parseTempInput(display: String, tempUnit: Int = 0): Double =
    display.toDoubleOrNull() ?: 0.0

private fun parseDistInput(display: String, distUnit: Int = 0): Double =
    display.toDoubleOrNull() ?: 0.0

private fun convertTempDisplayValue(display: String, fromUnit: Int, toUnit: Int): String {
    if (fromUnit == toUnit) return display
    val kelvin = displayToKelvin(display.toDoubleOrNull() ?: 0.0, fromUnit)
    return "%.1f".format(Locale.US, kelvinToDisplay(kelvin, toUnit))
}

private fun convertDistDisplayValue(display: String, fromUnit: Int, toUnit: Int): String {
    val v = display.toDoubleOrNull() ?: 0.0
    val meters = displayToMeter(v, fromUnit)
    return "%.1f".format(Locale.US, meterToDisplay(meters, toUnit))
}

/** Android Bitmap.copyPixelsFromBuffer expects RGBA bytes; SDK NET_BGRA=4 is BGRA. */
private const val STREAM_TYPE_BGRA = 4

private fun pixelsForAndroidBitmap(data: ByteArray, streamType: Int): ByteArray {
    if (streamType != STREAM_TYPE_BGRA) return data
    val out = data.copyOf()
    var i = 0
    while (i + 3 < out.size) {
        val b = out[i]
        out[i] = out[i + 2]
        out[i + 2] = b
        i += 4
    }
    return out
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NetSDKManager.enableLog(true)
        NetSDKManager.init()
        setContent {
            IRtekNetSDKDemoTheme {
                val scope = rememberCoroutineScope()
                var isConnected by remember { mutableStateOf(false) }
                var connectedIp by remember { mutableStateOf("") }
                var lang by remember { mutableStateOf(loadAppLang(this@MainActivity)) }
                currentStrings = remember(lang) { AppStrings(lang) }

                if (!isConnected) {
                    ConnectScreen(
                        onConnected = { ip ->
                            isConnected = true
                            connectedIp = ip
                        },
                        lang = lang,
                        onLangChange = { next ->
                            lang = next
                            saveAppLang(this@MainActivity, next)
                        }
                    )
                } else {
                    DeviceScreen(
                        deviceIp = connectedIp,
                        onDisconnect = {
                            scope.launch {
                                NetSDKManager.logout()
                            }
                            isConnected = false
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NetSDKManager.cleanup()
    }
}

// ==================== Connect Screen ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    onConnected: (ip: String) -> Unit,
    lang: AppLang,
    onLangChange: (AppLang) -> Unit
) {
    val scope = rememberCoroutineScope()

    var ipAddress by remember { mutableStateOf("192.168.10.177") }
    var port by remember { mutableStateOf("80") }
    var userName by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("admin") }
    var isConnecting by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var discoveredDevices by remember { mutableStateOf<JSONArray?>(null) }
    var statusText by remember { mutableStateOf("") }
    var sdkVersion by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val r = NetSDKManager.getSdkVersion()
        if (r.isOk() && r.data != null) {
            val obj = r.data!!
            sdkVersion = "${obj.optInt("major")}.${obj.optInt("minor")}.${obj.optInt("patch")}.${obj.optInt("build")}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NetSDK Demo") },
                actions = {
                    var langMenu by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { langMenu = true }) {
                            Icon(Icons.Filled.Language, contentDescription = tr("语言"), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tr("语言"), style = MaterialTheme.typography.labelLarge)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = langMenu, onDismissRequest = { langMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("English") },
                                onClick = { onLangChange(AppLang.EN); langMenu = false },
                                trailingIcon = {
                                    if (lang == AppLang.EN) Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("中文") },
                                onClick = { onLangChange(AppLang.ZH); langMenu = false },
                                trailingIcon = {
                                    if (lang == AppLang.ZH) Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "SDK: ${sdkVersion ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("设备连接"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = ipAddress, onValueChange = { ipAddress = it },
                        label = { Text(tr("设备 IP 地址")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = port, onValueChange = { port = it },
                            label = { Text(tr("端口")) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = userName, onValueChange = { userName = it },
                            label = { Text(tr("用户名")) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            label = { Text(tr("密码")) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                if (isConnecting) return@Button
                                isConnecting = true
                                statusText = tr("连接中...")
                                scope.launch {
                                    val result = NetSDKManager.login(
                                        ipAddress,
                                        port.toIntOrNull() ?: 80,
                                        userName,
                                        password
                                    )
                                    if (result.isOk()) {
                                        onConnected(ipAddress)
                                    } else {
                                        statusText = tr("连接失败: %s", result.message)
                                    }
                                    isConnecting = false
                                }
                            },
                            enabled = !isConnecting,
                            modifier = Modifier.weight(1f)
                        ) { Text(if (isConnecting) tr("连接中...") else tr("连接")) }

                        OutlinedButton(
                            onClick = {
                                if (isSearching) return@OutlinedButton
                                isSearching = true
                                statusText = tr("搜索设备中...")
                                scope.launch {
                                    val result = NetSDKManager.searchDevices(3000)
                                    discoveredDevices = if (result.isOk()) result.data else null
                                    val count = discoveredDevices?.length() ?: 0
                                    statusText = if (result.isOk()) tr("发现 %s 台设备", count) else tr("搜索失败: %s", result.message)
                                    isSearching = false
                                }
                            },
                            enabled = !isSearching,
                            modifier = Modifier.weight(1f)
                        ) { Text(if (isSearching) tr("搜索中...") else tr("搜索设备")) }
                    }

                    if (statusText.isNotEmpty()) {
                        Text(statusText, style = MaterialTheme.typography.bodySmall,
                            color = if (statusText.startsWith(tr("连接失败")) || statusText.startsWith(tr("搜索失败")))
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                }
            }

            val devices = discoveredDevices
            if (devices != null && devices.length() > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(tr("发现 %s 台设备", devices.length()), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                        // Table header
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), MaterialTheme.shapes.small)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text("IP", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(tr("型号"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.2f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(tr("序列号"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.2f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(tr("固件"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.8f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Table rows
                        for (i in 0 until devices.length()) {
                            val dev = devices.getJSONObject(i)
                            val devIp = dev.optString("ip", "")
                            val devModel = dev.optString("model", "")
                            val devSerial = dev.optString("serial_no", "")
                            val devFirmware = dev.optString("firmware_version", "")
                            val devConflict = dev.optInt("wandering", 0) != 0
                            Surface(
                                onClick = {
                                    ipAddress = devIp
                                    statusText = tr("已选择: %s", devIp)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (ipAddress == devIp) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(devIp, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                                    Text(devModel, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(devSerial, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        if (devConflict) "$devFirmware*" else devFirmware,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (devConflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (i < devices.length() - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== Device Screen ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(deviceIp: String, onDisconnect: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var logText by remember { mutableStateOf("") }
    var showLogPanel by remember { mutableStateOf(false) }
    var fabOffsetX by remember { mutableFloatStateOf(0f) }
    var fabOffsetY by remember { mutableFloatStateOf(0f) }

    fun appendLog(msg: String) {
        logText = (if (logText.isEmpty()) "" else "$logText\n") + msg
    }

    fun runCmd(name: String, block: suspend () -> String) {
        scope.launch {
            val detail = try { withContext(Dispatchers.IO) { block() } }
            catch (e: Exception) { tr("异常: %s", e.message) }
            appendLog("$name: $detail")
        }
    }

    // Tabs: 属性, 热像仪, 显示, 时间, 网络, 用户, 标记, 数据, 报警
    val mainTabs = listOf(tr("属性"), tr("热像仪"), tr("显示"), tr("时间"), tr("网络"), tr("用户"), tr("标记"), tr("数据"), tr("报警"))
    var selectedMainTab by remember { mutableIntStateOf(0) }

    // Stream state
    var isStreaming by remember { mutableStateOf(false) }
    var streamHandle by remember { mutableLongStateOf(0L) }
    var activePlayStreamId by remember { mutableIntStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }
    var selectedStreamId by remember { mutableIntStateOf(0) }
    var streamOptions by remember { mutableStateOf(listOf<Pair<Int, String>>()) }
    var streamDetailText by remember { mutableStateOf<String?>(null) }
    var deviceName by remember { mutableStateOf("") }
    var thermalDataCallbackText by remember { mutableStateOf("") }
    var thermalDataStreamActive by remember { mutableStateOf(false) }
    val thermalDataStartupLogRemaining = remember { AtomicInteger(0) }
    val thermalDataStartupLogTotal = 3
    var streamBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var streamResolution by remember { mutableStateOf("") }
    var frameCount by remember { mutableLongStateOf(0L) }
    var videoAspect by remember { mutableFloatStateOf(4f / 3f) }
    var streamFps by remember { mutableIntStateOf(0) }
    var streamBytesPerSec by remember { mutableLongStateOf(0L) }
    val fpsCounter = remember { object { var frames = 0L; var bytes = 0L; var lastTs = System.currentTimeMillis() } }

    LaunchedEffect(isStreaming) {
        if (!isStreaming) { streamFps = 0; streamBytesPerSec = 0; return@LaunchedEffect }
        while (isStreaming) {
            delay(1000)
            val now = System.currentTimeMillis()
            val dt = (now - fpsCounter.lastTs).coerceAtLeast(1)
            streamFps = (fpsCounter.frames * 1000 / dt).toInt()
            streamBytesPerSec = fpsCounter.bytes * 1000 / dt
            fpsCounter.frames = 0; fpsCounter.bytes = 0; fpsCounter.lastTs = now
        }
    }

    val streamHandleState = rememberUpdatedState(streamHandle)
    val activePlayStreamIdState = rememberUpdatedState(activePlayStreamId)
    val isStreamingState = rememberUpdatedState(isStreaming)
    val thermalDataStreamActiveState = rememberUpdatedState(thermalDataStreamActive)
    val isRecordingState = rememberUpdatedState(isRecording)

    DisposableEffect(Unit) {
        NetSDKManager.setStreamFrameListener(object : NativeSDK.StreamFrameListener {
            override fun onStreamFrame(
                handle: Long, streamId: Int, streamType: Int,
                width: Int, height: Int, data: ByteArray,
                timestampUs: Long, keyFrame: Boolean
            ) {
                if (width <= 0 || height <= 0 || data.size < width * height * 4) return
                try {
                    val pixels = pixelsForAndroidBitmap(data, streamType)
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bmp.copyPixelsFromBuffer(ByteBuffer.wrap(pixels))
                    scope.launch(Dispatchers.Main.immediate) {
                        streamBitmap = bmp
                        streamResolution = "${width}x${height}"
                        videoAspect = width.toFloat() / height.toFloat()
                        frameCount++
                        fpsCounter.frames++
                        fpsCounter.bytes += data.size
                    }
                } catch (_: Exception) {}
            }
        })
        onDispose {
            NetSDKManager.setStreamFrameListener(null)
            scope.launch {
                val sid = activePlayStreamIdState.value
                if (isRecordingState.value && sid > 0) {
                    NetSDKManager.stopStreamRecord(sid)
                }
                if (isStreamingState.value) {
                    NetSDKManager.stopStream()
                }
                val deviceHandle = NetSDKManager.getActiveHandle()
                if (deviceHandle != 0L) {
                    NativeSDK.nativeSetThermalDataStreamCallback(deviceHandle, false)
                }
                withContext(Dispatchers.Main) {
                    thermalDataStreamActive = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        NetSDKManager.setEventListener(object : NativeSDK.EventListener {
            override fun onEvent(handle: Long, streamId: Int, code: Int, message: String) {
                scope.launch(Dispatchers.Main) {
                    appendLog(tr("事件回调: stream=%s code=%s %s", streamId, code, message))
                }
            }
        })
        val deviceHandle = NetSDKManager.getActiveHandle()
        if (deviceHandle != 0L) {
            NativeSDK.nativeSetEventCallback(deviceHandle, true)
        }
        onDispose {
            val h = NetSDKManager.getActiveHandle()
            if (h != 0L) NativeSDK.nativeSetEventCallback(h, false)
            NetSDKManager.setEventListener(null)
        }
    }

    var markerTemps by remember { mutableStateOf<Map<String, Triple<Double, Double, Double>>>(emptyMap()) }
    var markerAlarmLevels by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    DisposableEffect(Unit) {
        NetSDKManager.setThermalDataListener(object : NativeSDK.ThermalDataListener {
            override fun onThermalData(handle: Long, json: String) {
                var summary: String
                var parseError: String? = null
                var newTemps = emptyMap<String, Triple<Double, Double, Double>>()
                try {
                    val obj = JSONObject(json)
                    val types = obj.optInt("data_types", 0)
                    val ts = obj.optLong("timestamp_us", 0L)
                    val markersCount = obj.optInt("markers_count", obj.optJSONArray("marker_temps")?.length() ?: 0)
                    summary = "types=$types, ts=$ts, markers=$markersCount"

                    val temps = obj.optJSONArray("marker_temps")
                    if (temps != null && temps.length() > 0) {
                        val tempMap = mutableMapOf<String, Triple<Double, Double, Double>>()
                        for (i in 0 until temps.length()) {
                            val t = temps.getJSONObject(i)
                            val tpi = t.optJSONObject("temp_point_info")
                            if (tpi != null) {
                                val mx = tpi.optJSONObject("max")?.optDouble("temperature", 0.0) ?: 0.0
                                val mn = tpi.optJSONObject("min")?.optDouble("temperature", 0.0) ?: 0.0
                                val avg = tpi.optDouble("avg_temp", 0.0)
                                tempMap[t.optString("marker_name", "")] = Triple(mx, mn, avg)
                            }
                        }
                        newTemps = tempMap
                    }
                } catch (e: Exception) {
                    parseError = e.message ?: "parse error"
                    summary = json.take(120)
                }
                scope.launch(Dispatchers.Main) {
                    // 流已在推数据但按钮仍显示“未启动”时，用回调自愈 UI
                    if (!thermalDataStreamActiveState.value) {
                        thermalDataStreamActive = true
                    }
                    thermalDataCallbackText = summary
                    if (parseError != null) {
                        appendLog(tr("热像数据回调异常: %s, raw=%s", parseError, summary))
                    } else {
                        val remaining = thermalDataStartupLogRemaining.get()
                        if (remaining > 0 && thermalDataStartupLogRemaining.compareAndSet(remaining, remaining - 1)) {
                            val index = thermalDataStartupLogTotal - remaining + 1
                            appendLog(tr("热像数据回调(%s/%s): %s", index, thermalDataStartupLogTotal, summary))
                        }
                    }
                    if (newTemps.isNotEmpty()) markerTemps = newTemps
                }
            }
        })
        onDispose {
            NetSDKManager.setThermalDataListener(null)
            thermalDataStartupLogRemaining.set(0)
        }
    }

    // Device info
    var deviceInfoText by remember { mutableStateOf<String?>(null) }
    var lensInfoText by remember { mutableStateOf<String?>(null) }
    var streamsText by remember { mutableStateOf<String?>(null) }
    var rangesText by remember { mutableStateOf<String?>(null) }
    var lensRangeItems by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var rangeSourceTempUnit by remember { mutableIntStateOf(0) }

    // Thermal settings state
    var selectedShutterMode by remember { mutableIntStateOf(0) }
    var selectedShutterTime by remember { mutableIntStateOf(5) }
    var selectedRangeId by remember { mutableIntStateOf(1) }
    var rangeOptions by remember { mutableStateOf(listOf(1 to tr("量程1"), 2 to tr("量程2"))) }
    var tpEmissivity by remember { mutableStateOf("0.95") }
    var tpDistance by remember { mutableStateOf("1.0") }
    var tpReflTemp by remember { mutableStateOf("20.0") }
    var tpAtmTemp by remember { mutableStateOf("20.0") }
    var tpRelHumidity by remember { mutableStateOf("0.5") }
    var paletteOptions by remember { mutableStateOf(listOf(0 to "Iron(0)")) }
    var selectedPaletteRefNo by remember { mutableIntStateOf(0) }
    var paletteColors by remember { mutableStateOf<IntArray>(intArrayOf()) }
    var selectedGainMode by remember { mutableIntStateOf(2) }
    var gainMax by remember { mutableStateOf("") }
    var gainMin by remember { mutableStateOf("") }
    var gainFixedSpan by remember { mutableIntStateOf(0) }
    var gainSpan by remember { mutableStateOf("") }
    var selectedSrMode by remember { mutableIntStateOf(0) }
    var selectedTempUnit by remember { mutableIntStateOf(0) }
    var selectedDistanceUnit by remember { mutableIntStateOf(0) }
    var selectedFlipMode by remember { mutableIntStateOf(0) }
    var selectedColorDist by remember { mutableIntStateOf(1) }
    var channelId by remember { mutableIntStateOf(101) }

    // Channel
    var channelOptions by remember { mutableStateOf(listOf(101 to tr("通道101"))) }
    var channelName by remember { mutableStateOf("") }
    var resW by remember { mutableIntStateOf(640) }
    var resH by remember { mutableIntStateOf(480) }

    // Marker drawing state
    var markers by remember { mutableStateOf<List<DrawMarker>>(emptyList()) }
    var markerResW by remember { mutableIntStateOf(320) }
    var markerResH by remember { mutableIntStateOf(240) }
    var markerMode by remember { mutableIntStateOf(0) }
    var spotCounter by remember { mutableIntStateOf(0) }
    var areaCounter by remember { mutableIntStateOf(0) }
    var lineCounter by remember { mutableIntStateOf(0) }
    var nextMarkerId by remember { mutableIntStateOf(1) }
    var draggingMarker by remember { mutableStateOf<DrawMarker?>(null) }
    var dragHitType by remember { mutableIntStateOf(-1) }
    var dragStartPt by remember { mutableStateOf(Pair(0, 0)) }
    var dragMarkerOrigPoints by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }

    // Overlay (channel_name_overlay, time_overlay)
    var ovChNameEnabled by remember { mutableIntStateOf(0) }
    var ovChNameAlign by remember { mutableIntStateOf(0) }
    var ovChNameMode by remember { mutableIntStateOf(0) }
    var ovChNameX by remember { mutableIntStateOf(0) }
    var ovChNameY by remember { mutableIntStateOf(0) }
    var ovTimeEnabled by remember { mutableIntStateOf(0) }
    var ovTimeAlign by remember { mutableIntStateOf(0) }
    var ovTimeMode by remember { mutableIntStateOf(0) }
    var ovTimeX by remember { mutableIntStateOf(0) }
    var ovTimeY by remember { mutableIntStateOf(0) }
    var ovCustom by remember { mutableStateOf(intArrayOf(0, 0, 0, 0, 0)) }
    var ovCustomText by remember { mutableStateOf("") }

    // Thermal overlay - overlay_block fields (enabled, align, mode, x, y)
    var tovUnit by remember { mutableStateOf(intArrayOf(0, 0, 0, 0, 0)) }
    var tovPlt by remember { mutableStateOf(intArrayOf(0, 0, 0, 0, 0)) }
    var tovThermalParams by remember { mutableStateOf(intArrayOf(0, 0, 0, 0, 0)) }
    var tovAlarm by remember { mutableStateOf(intArrayOf(0, 0, 0, 0, 0)) }
    // follow_block: enabled(0), align(1), follow(2), max(3), min(4), avg(5), emissivity(6), x(7), y(8)
    var tovGlobal by remember { mutableStateOf(intArrayOf(0,0,0,0,0,0,0,0,0)) }
    var tovMarker by remember { mutableStateOf(intArrayOf(0,0,0,0,0,0,0,0,0)) }

    // Image params
    var imgContrast by remember { mutableStateOf("0") }
    var imgBrightness by remember { mutableStateOf("0") }
    var imgDdeEnable by remember { mutableIntStateOf(0) }
    var imgDdeValue by remember { mutableStateOf("0") }

    // Network settings
    var netId by remember { mutableIntStateOf(0) }
    var netName by remember { mutableStateOf("eth0") }
    var netDhcp by remember { mutableIntStateOf(0) }
    var netIp by remember { mutableStateOf("") }
    var netMask by remember { mutableStateOf("") }
    var netGateway by remember { mutableStateOf("") }
    var netDns by remember { mutableStateOf("") }
    var ftpServer by remember { mutableStateOf("") }
    var ftpPort by remember { mutableStateOf("21") }
    var ftpUser by remember { mutableStateOf("") }
    var ftpPwd by remember { mutableStateOf("") }
    var ftpSsl by remember { mutableIntStateOf(0) }
    var ftpAnonymous by remember { mutableIntStateOf(0) }
    var ftpPath by remember { mutableStateOf("{DEVICE_NAME}/{DEVICE_SERIAL_NO}/123/{DEVICE_IP}/{CHANNEL_NAME}/{CHANNEL_ID}/{yyyyMMddHHmmssfff}.jpg") }
    var portHttp by remember { mutableStateOf("80") }
    var portRtsp by remember { mutableStateOf("554") }
    var portRtp by remember { mutableStateOf("5004") }

    // Alarm extension
    var alarmListenerActive by remember { mutableStateOf(false) }
    var alarmListenerHandle by remember { mutableLongStateOf(0L) }
    var alarmEventText by remember { mutableStateOf("") }
    var triggerFtpEnabled by remember { mutableIntStateOf(0) }
    var triggerWebhookEnabled by remember { mutableIntStateOf(0) }
    val alarmStartupLogRemaining = remember { AtomicInteger(0) }
    val alarmStartupLogTotal = 3

    // User management
    var userList by remember { mutableStateOf<JSONArray>(JSONArray()) }
    var selectedUserId by remember { mutableIntStateOf(-1) }
    var editUserName by remember { mutableStateOf("") }
    var editUserLevel by remember { mutableIntStateOf(0) }
    var editLoginPwd by remember { mutableStateOf("") }
    var editNewPwd by remember { mutableStateOf("") }

    // Time settings
    var timeMode by remember { mutableIntStateOf(0) }
    var timeZone by remember { mutableStateOf("") }
    var localTime by remember { mutableStateOf("") }
    var ntpHost by remember { mutableStateOf("") }
    var ntpPort by remember { mutableStateOf("123") }
    var ntpInterval by remember { mutableStateOf("60") }

    val alarmListenerHandleState = rememberUpdatedState(alarmListenerHandle)
    val alarmListenerActiveState = rememberUpdatedState(alarmListenerActive)
    val tempUnitState = rememberUpdatedState(selectedTempUnit)

    DisposableEffect(Unit) {
        NetSDKManager.setAlarmListener(object : NativeSDK.AlarmListener {
            override fun onAlarm(sessionId: Long, ip: String, json: String) {
                var summary: String
                var newTemps = emptyMap<String, Triple<Double, Double, Double>>()
                var newAlarms = emptyMap<String, Int>()
                try {
                    val obj = JSONObject(json)
                    // 厂商 JNI 按报警类型封装：NET_ALARM_THERMAL_TEMP -> "thermal_temp"
                    val info = obj.optJSONObject("thermal_temp")
                        ?: obj.optJSONObject("alarm_info")
                    val devInfo = info?.optJSONObject("dev_info")
                    val srcIp = devInfo?.optString("ip")?.takeIf { it.isNotEmpty() } ?: ip
                    val srcPort = devInfo?.optInt("port_no", 0) ?: 0
                    val srcChannel = devInfo?.optInt("channel_id", 0) ?: 0
                    val global = info?.optJSONObject("global_temp_info")
                    val maxTemp = global?.optJSONObject("max")?.optDouble("temperature", 0.0) ?: 0.0
                    val msg = info?.optString("message", "")
                        ?: obj.optString("message", "")
                    val unit = tempUnitState.value
                    val alarmLevelNames = mapOf(1 to tr("提醒"), 2 to tr("预警"), 3 to tr("报警"))
                    val activeAlarms = mutableListOf<String>()
                    val tempMap = mutableMapOf<String, Triple<Double, Double, Double>>()
                    val alarmMap = mutableMapOf<String, Int>()
                    val alarms = info?.optJSONArray("marker_alarms")
                    val alarmCount = info?.optInt("marker_alarms_count", alarms?.length() ?: 0)
                        ?: (alarms?.length() ?: 0)
                    if (alarms != null) {
                        for (i in 0 until minOf(alarmCount, alarms.length())) {
                            val a = alarms.getJSONObject(i)
                            val name = a.optString("marker_name", "")
                            val level = a.optInt("alarm_level", 0)
                            alarmMap[name] = level
                            if (level > 0) {
                                activeAlarms.add("$name:${alarmLevelNames[level] ?: level}")
                            }
                        }
                    }
                    val temps = info?.optJSONArray("marker_temps")
                    val tempCount = info?.optInt("markers_count", temps?.length() ?: 0)
                        ?: (temps?.length() ?: 0)
                    if (temps != null) {
                        for (i in 0 until minOf(tempCount, temps.length())) {
                            val t = temps.getJSONObject(i)
                            val tpi = t.optJSONObject("temp_point_info")
                            if (tpi != null) {
                                val mx = tpi.optJSONObject("max")?.optDouble("temperature", 0.0) ?: 0.0
                                val mn = tpi.optJSONObject("min")?.optDouble("temperature", 0.0) ?: 0.0
                                val avg = tpi.optDouble("avg_temp", 0.0)
                                tempMap[t.optString("marker_name", "")] = Triple(mx, mn, avg)
                            }
                        }
                    }
                    newTemps = tempMap
                    newAlarms = alarmMap
                    val alarmsText = if (activeAlarms.isEmpty()) "none" else activeAlarms.joinToString(",")
                    summary = "ip=$srcIp, port=$srcPort, ch=$srcChannel, max=${formatTempValue(maxTemp, unit)}${tempUnitLabel(unit)}, alarms=$alarmsText, msg=$msg"
                } catch (_: Exception) {
                    summary = "ip=$ip, $json"
                }
                scope.launch(Dispatchers.Main) {
                    alarmEventText = summary
                    val remaining = alarmStartupLogRemaining.get()
                    if (remaining > 0 && alarmStartupLogRemaining.compareAndSet(remaining, remaining - 1)) {
                        val index = alarmStartupLogTotal - remaining + 1
                        appendLog(tr("报警回调(%s/%s): %s", index, alarmStartupLogTotal, summary))
                    }
                    if (newTemps.isNotEmpty()) markerTemps = newTemps
                    if (newAlarms.isNotEmpty()) markerAlarmLevels = newAlarms
                }
            }
        })
        onDispose {
            NetSDKManager.setAlarmListener(null)
            alarmStartupLogRemaining.set(0)
            scope.launch {
                if (alarmListenerActiveState.value && alarmListenerHandleState.value > 0) {
                    NetSDKManager.stopAlarmListener(alarmListenerHandleState.value)
                }
            }
        }
    }

    fun pushMarkers() {
        val arr = org.json.JSONArray()
        markers.forEach { marker ->
            val sdkType = normalizeMarkerType(marker.type)
            val pointCount = marker.points.size
            val valid = when (sdkType) {
                MARKER_TYPE_SPOT -> pointCount >= 1
                MARKER_TYPE_POLYLINE -> pointCount == 2
                MARKER_TYPE_POLYGON -> pointCount in MARKER_POLYGON_MIN_POINTS..MARKER_POLYGON_MAX_POINTS
                else -> false
            }
            if (valid) arr.put(marker.toJson())
        }
        val json = arr.toString()
        scope.launch {
            val r = withContext(Dispatchers.IO) { NetSDKManager.setThermalMarkers(json) }
            if (r.isOk()) appendLog("Markers updated (${markers.size})")
            else appendLog("Set markers failed: ${r.message}")
        }
    }

    fun refreshStreamDetail() {
        scope.launch {
            try {
                val sr = NetSDKManager.getStream(selectedStreamId)
                val st = NetSDKManager.getStreamStatus(selectedStreamId)
                streamDetailText = buildString {
                    if (sr.isOk() && sr.data != null) {
                        val s = sr.data!!
                        append(tr("流参数: id=%s, %sx%s, %s, %skbps, %sfps", s.optInt("id"), s.optInt("resolution_width"), s.optInt("resolution_height"), s.optString("codec_type"), s.optInt("bit_rate") / 1024, s.optInt("max_frame_rate")))
                    } else append(tr("流参数: 获取失败 %s", sr.message))
                    append('\n')
                    if (st.isOk() && st.data != null) {
                        val d = st.data!!
                        append(tr("流状态: connected=%s, playing=%s, %s", d.optInt("connected", d.optInt("is_connected", -1)), d.optInt("playing", d.optInt("is_playing", -1)), d.toString()))
                    } else append(tr("流状态: 获取失败 %s", st.message))
                }
            } catch (e: Exception) {
                streamDetailText = tr("刷新异常: %s", e.message)
            }
        }
    }

    fun refreshAllProperties() {
        scope.launch {
            try {
                val r = NetSDKManager.getDeviceInfo()
                deviceInfoText = if (r.isOk() && r.data != null) {
                    val d = r.data!!
                    deviceName = d.optString("name", deviceName)
                    buildString {
                        append(tr("名称: %s", d.optString("name")))
                        append("\n" + tr("型号: %s", d.optString("model")))
                        append("\n" + tr("规格: %s", d.optString("spec")))
                        append("\n" + tr("序列号: %s", d.optString("serial_no")))
                        append("\n" + tr("设备ID: %s", d.optString("id")))
                        append("\n" + tr("品牌: %s", d.optString("brand")))
                        append("\n" + tr("公司: %s", d.optString("company")))
                        append("\n" + tr("固件名称: %s", d.optString("firmware_name")))
                        append("\n" + tr("固件版本: %s", d.optString("firmware_version")))
                    }
                } else tr("获取失败: %s", r.message)

                val ur = NetSDKManager.getThermalUnit()
                if (ur.isOk() && ur.data != null) {
                    selectedTempUnit = ur.data!!.optInt("temp_unit", 0)
                    selectedDistanceUnit = ur.data!!.optInt("distance_unit", 0)
                }

                val lr = NetSDKManager.getThermalLensInfo()
                if (lr.isOk() && lr.data != null) {
                    val d = lr.data!!
                    val rangeMap = d.optJSONArray("range_map")
                    if (rangeMap != null && rangeMap.length() > 0) {
                        val items = (0 until rangeMap.length()).map { rangeMap.getJSONObject(it) }
                        lensRangeItems = items
                        rangeSourceTempUnit = selectedTempUnit
                        val (text, options) = buildRangeDisplay(items, selectedTempUnit, selectedTempUnit)
                        rangesText = text
                        rangeOptions = options
                    } else {
                        lensRangeItems = emptyList()
                        rangesText = tr("无")
                    }
                    lensInfoText = buildString {
                        append(tr("名称: %s", d.optString("name")))
                        append("\n" + tr("焦距: %smm", d.optDouble("focal", 0.0)))
                        append("\n" + tr("水平视场角: %s°", d.optDouble("hfov", 0.0)))
                        append("\n" + tr("垂直视场角: %s°", d.optDouble("vfov", 0.0)))
                        append("\n" + tr("波段: %s", d.optString("band")))
                    }
                } else {
                    lensInfoText = tr("获取失败: %s", lr.message)
                    rangesText = tr("获取失败: %s", lr.message)
                }

                val rr = NetSDKManager.getThermalRange()
                if (rr.isOk() && rr.data != null) {
                    selectedRangeId = rr.data!!
                }

                val tpr = NetSDKManager.getThermalParams()
                if (tpr.isOk() && tpr.data != null) {
                    val d = tpr.data!!
                    tpEmissivity = "%.2f".format(Locale.US, d.optDouble("emissivity", 0.95))
                    tpDistance = formatDistValue(d.optDouble("distance", 1.0), selectedDistanceUnit)
                    tpReflTemp = formatTempValue(d.optDouble("refl_temp", 20.0), selectedTempUnit)
                    tpAtmTemp = formatTempValue(d.optDouble("atm_temp", 20.0), selectedTempUnit)
                    tpRelHumidity = "%.2f".format(Locale.US, d.optDouble("rel_humidity", 0.5))
                }
                val gr = NetSDKManager.getThermalGain()
                if (gr.isOk() && gr.data != null) {
                    val d = gr.data!!
                    selectedGainMode = d.optInt("mode", 2)
                    gainMax = formatTempValue(d.optDouble("max", 0.0), selectedTempUnit)
                    gainMin = formatTempValue(d.optDouble("min", 0.0), selectedTempUnit)
                    gainFixedSpan = d.optInt("fixedSpan", 0)
                    gainSpan = formatTempValue(d.optDouble("span", 0.0), selectedTempUnit)
                }

                val sr = NetSDKManager.getStreams()
                streamsText = if (sr.isOk() && sr.data != null) {
                    val arr = sr.data!!
                    if (arr.length() == 0) {
                        streamOptions = emptyList()
                        tr("无视频流")
                    } else {
                        streamOptions = (0 until arr.length()).map { i ->
                            val s = arr.getJSONObject(i)
                            val id = s.optInt("id", i)
                            id to tr("流%s: %s", id, "${s.optInt("resolution_width")}x${s.optInt("resolution_height")}")
                        }
                        if (streamOptions.none { it.first == selectedStreamId }) {
                            selectedStreamId = streamOptions.first().first
                        }
                        (0 until arr.length()).joinToString("\n") { i ->
                            val s = arr.getJSONObject(i)
                            tr("流%s: %s", s.optInt("id"), "${s.optInt("resolution_width")}x${s.optInt("resolution_height")} ${s.optString("codec_type")} ${s.optInt("bit_rate") / 1024}kbps ${s.optInt("max_frame_rate")}fps")
                        }
                    }
                } else {
                    streamOptions = emptyList()
                    tr("获取失败: %s", sr.message)
                }
                refreshStreamDetail()

                val cr = NetSDKManager.getChannels()
                if (cr.isOk() && cr.data != null && cr.data!!.length() > 0) {
                    channelId = cr.data!!.getJSONObject(0).optInt("id", 101)
                }

                val pr = NetSDKManager.getThermalPresetPalettes()
                if (pr.isOk() && pr.data != null && pr.data!!.length() > 0) {
                    val arr = pr.data!!
                    paletteOptions = (0 until arr.length()).map { i ->
                        val item = arr.getJSONObject(i)
                        item.optInt("ref_no") to item.optString("name", "Palette$i")
                    }
                }
                val curPal = NetSDKManager.getThermalPalette()
                if (curPal.isOk() && curPal.data != null) {
                    val pd = curPal.data!!
                    selectedPaletteRefNo = pd.optInt("ref_no", 0)
                    val ca = pd.optJSONArray("colors")
                    if (ca != null && ca.length() > 0) {
                        paletteColors = IntArray(ca.length()) { i -> ca.optInt(i) }
                    }
                }

                val flipR = NetSDKManager.getImageFlip(channelId)
                if (flipR.isOk() && flipR.data != null) {
                    selectedFlipMode = flipR.data!!
                }

                val cdR = NetSDKManager.getThermalColorDist()
                if (cdR.isOk() && cdR.data != null) {
                    selectedColorDist = cdR.data!!
                }
                appendLog(tr("属性: 已刷新"))
            } catch (e: Exception) {
                appendLog(tr("属性刷新异常: %s", e.message))
            }
        }
    }

    LaunchedEffect(selectedTempUnit, lensRangeItems) {
        if (lensRangeItems.isNotEmpty()) {
            val (text, options) = buildRangeDisplay(lensRangeItems, rangeSourceTempUnit, selectedTempUnit)
            rangesText = text
            rangeOptions = options
        }
    }

    LaunchedEffect(Unit) {
        refreshAllProperties()
        launch {
            try {
                val r = NetSDKManager.getThermalResolution()
                if (r.isOk() && r.data != null) {
                    markerResW = r.data!!.optInt("width", 320)
                    markerResH = r.data!!.optInt("height", 240)
                }
            } catch (e: Exception) {
                appendLog(tr("获取热像分辨率失败: %s", e.message))
            }
        }
        launch {
            try {
                val r = NetSDKManager.getThermalMarkers()
                if (r.isOk() && r.data != null) {
                    val list = mutableListOf<DrawMarker>()
                    var sCnt = 0; var aCnt = 0; var lCnt = 0
                    for (i in 0 until r.data!!.length()) {
                        val m = r.data!!.getJSONObject(i)
                        val pts = mutableListOf<Pair<Int, Int>>()
                        val ptsArr = m.optJSONArray("points")
                        if (ptsArr != null) {
                            for (j in 0 until ptsArr.length()) {
                                val p = ptsArr.getJSONObject(j)
                                pts.add(Pair(p.optInt("x"), p.optInt("y")))
                            }
                        }
                        val dm = DrawMarker(
                            id = m.optInt("id"),
                            type = normalizeMarkerType(m.optInt("type")),
                            name = m.optString("name", ""),
                            points = pts
                        )
                        list.add(dm)
                        when (dm.type) {
                            MARKER_TYPE_SPOT -> sCnt++
                            MARKER_TYPE_POLYLINE -> lCnt++
                            MARKER_TYPE_POLYGON -> aCnt++
                        }
                    }
                    markers = list
                    spotCounter = sCnt; areaCounter = aCnt; lineCounter = lCnt
                    nextMarkerId = (list.maxOfOrNull { it.id } ?: 0) + 1
                    appendLog("Loaded ${list.size} markers (res: ${markerResW}x${markerResH})")
                }
            } catch (e: Exception) {
                appendLog(tr("获取标记失败: %s", e.message))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deviceIp, style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("断开"))
                    }
                },
                actions = {
                    TextButton(onClick = onDisconnect) {
                        Text(tr("断开"), color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {}
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // ── Video stream preview (4:3 aspect ratio) with controls overlay ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val bmp = streamBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Stream",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    Text(
                        if (isStreaming) tr("等待画面...") else tr("未开始"),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // Resolution, FPS, bitrate (top-right)
                if (streamResolution.isNotEmpty()) {
                    val bwText = if (streamBytesPerSec >= 1_000_000) "%.1fMB/s".format(streamBytesPerSec / 1_000_000.0)
                                 else "%.0fKB/s".format(streamBytesPerSec / 1_000.0)
                    Text(
                        "$streamResolution  ${streamFps}fps  $bwText",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    val mx = (offset.x * markerResW / w).toInt().coerceIn(0, markerResW - 1)
                                    val my = (offset.y * markerResH / h).toInt().coerceIn(0, markerResH - 1)

                                    if (markerMode == -1) {
                                        val hit = hitTestMarker(mx, my, markers, w / markerResW, h / markerResH)
                                        if (hit != null) {
                                            markers = markers.filter { it.id != hit.first.id }
                                            markerMode = 0
                                            pushMarkers()
                                        }
                                        return@detectDragGestures
                                    }

                                    if (markerMode == 1) {
                                        spotCounter++
                                        val m = DrawMarker(id = nextMarkerId++, type = MARKER_TYPE_SPOT, name = "S%02d".format(spotCounter),
                                            points = mutableListOf(Pair(mx, my)))
                                        markers = markers + m
                                        markerMode = 0
                                        pushMarkers()
                                        return@detectDragGestures
                                    }

                                    if (markerMode == 2) {
                                        lineCounter++
                                        val m = DrawMarker(id = nextMarkerId++, type = MARKER_TYPE_POLYLINE, name = "L%02d".format(lineCounter),
                                            points = mutableListOf(Pair(mx, my), Pair((mx + 40).coerceAtMost(markerResW - 1), (my + 40).coerceAtMost(markerResH - 1))))
                                        markers = markers + m
                                        markerMode = 0
                                        pushMarkers()
                                        return@detectDragGestures
                                    }

                                    if (markerMode == 3) {
                                        areaCounter++
                                        val x0 = mx; val y0 = my
                                        val x1 = (mx + 30).coerceAtMost(markerResW - 1)
                                        val y1 = (my + 30).coerceAtMost(markerResH - 1)
                                        val m = DrawMarker(id = nextMarkerId++, type = MARKER_TYPE_POLYGON, name = "A%02d".format(areaCounter),
                                            points = mutableListOf(
                                                Pair(x0, y0), Pair(x1, y0), Pair(x1, y1), Pair(x0, y1)
                                            ))
                                        markers = markers + m
                                        markerMode = 0
                                        pushMarkers()
                                        return@detectDragGestures
                                    }

                                    val hit = hitTestMarker(mx, my, markers, w / markerResW, h / markerResH)
                                    if (hit != null) {
                                        draggingMarker = hit.first
                                        dragHitType = hit.second
                                        dragStartPt = Pair(mx, my)
                                        dragMarkerOrigPoints = hit.first.points.map { it.copy() }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    val dm = draggingMarker ?: return@detectDragGestures
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    val dx = (dragAmount.x * markerResW / w).toInt()
                                    val dy = (dragAmount.y * markerResH / h).toInt()
                                    if (dx == 0 && dy == 0) return@detectDragGestures

                                    var newDraggingMarker: DrawMarker? = null
                                    val updated = markers.map { m ->
                                        if (m.id == dm.id) {
                                            val newPts = m.points.toMutableList()
                                            when (m.type) {
                                                MARKER_TYPE_SPOT -> {
                                                    val nx = (newPts[0].first + dx).coerceIn(0, markerResW - 1)
                                                    val ny = (newPts[0].second + dy).coerceIn(0, markerResH - 1)
                                                    newPts[0] = Pair(nx, ny)
                                                }
                                                MARKER_TYPE_POLYLINE -> {
                                                    if (dragHitType == 0) {
                                                        val nx = (newPts[0].first + dx).coerceIn(0, markerResW - 1)
                                                        val ny = (newPts[0].second + dy).coerceIn(0, markerResH - 1)
                                                        newPts[0] = Pair(nx, ny)
                                                    } else if (dragHitType == 1) {
                                                        val nx = (newPts[1].first + dx).coerceIn(0, markerResW - 1)
                                                        val ny = (newPts[1].second + dy).coerceIn(0, markerResH - 1)
                                                        newPts[1] = Pair(nx, ny)
                                                    } else {
                                                        for (i in newPts.indices) {
                                                            val nx = (newPts[i].first + dx).coerceIn(0, markerResW - 1)
                                                            val ny = (newPts[i].second + dy).coerceIn(0, markerResH - 1)
                                                            newPts[i] = Pair(nx, ny)
                                                        }
                                                    }
                                                }
                                                MARKER_TYPE_POLYGON -> {
                                                    if (dragHitType == -2) {
                                                        for (i in newPts.indices) {
                                                            val nx = (newPts[i].first + dx).coerceIn(0, markerResW - 1)
                                                            val ny = (newPts[i].second + dy).coerceIn(0, markerResH - 1)
                                                            newPts[i] = Pair(nx, ny)
                                                        }
                                                    } else {
                                                        val vi = dragHitType.coerceIn(0, newPts.size - 1)
                                                        val nx = (newPts[vi].first + dx).coerceIn(0, markerResW - 1)
                                                        val ny = (newPts[vi].second + dy).coerceIn(0, markerResH - 1)
                                                        newPts[vi] = Pair(nx, ny)
                                                    }
                                                }
                                            }
                                            val copied = m.copy(points = newPts)
                                            newDraggingMarker = copied
                                            copied
                                        } else m
                                    }
                                    markers = updated
                                    if (newDraggingMarker != null) draggingMarker = newDraggingMarker
                                },
                                onDragEnd = {
                                    if (draggingMarker != null) {
                                        pushMarkers()
                                    }
                                    draggingMarker = null
                                    dragHitType = -1
                                },
                                onDragCancel = {
                                    val dm = draggingMarker
                                    if (dm != null) {
                                        val restored = markers.map { m ->
                                            if (m.id == dm.id) m.copy(points = dragMarkerOrigPoints.toMutableList()) else m
                                        }
                                        markers = restored
                                    }
                                    draggingMarker = null
                                    dragHitType = -1
                                }
                            )
                        }
                ) {
                    val sx = size.width / markerResW
                    val sy = size.height / markerResH
                    val markerColor = Color(0xFFFFEB3B)
                    val handleSize = 6f * sx

                    markers.forEach { m ->
                        when (m.type) {
                            MARKER_TYPE_SPOT -> {
                                if (m.points.isNotEmpty()) {
                                    val (mx, my) = m.points[0]
                                    val px = mx * sx; val py = my * sy
                                    val r = 3f * sx; val dist = 14f * sx
                                    drawCircle(markerColor, r, Offset(px, py))
                                    drawLine(markerColor, Offset(px, py - dist), Offset(px, py - r), strokeWidth = 2f)
                                    drawLine(markerColor, Offset(px, py + r), Offset(px, py + dist), strokeWidth = 2f)
                                    drawLine(markerColor, Offset(px - dist, py), Offset(px - r, py), strokeWidth = 2f)
                                    drawLine(markerColor, Offset(px + r, py), Offset(px + dist, py), strokeWidth = 2f)
                                }
                            }
                            MARKER_TYPE_POLYLINE -> {
                                if (m.points.size >= 2) {
                                    val p0 = Offset(m.points[0].first * sx, m.points[0].second * sy)
                                    val p1 = Offset(m.points[1].first * sx, m.points[1].second * sy)
                                    drawLine(markerColor, p0, p1, strokeWidth = 2f)
                                    drawRect(Color.White, Offset(p0.x - handleSize / 2, p0.y - handleSize / 2), Size(handleSize, handleSize))
                                    drawRect(markerColor, Offset(p0.x - handleSize / 2, p0.y - handleSize / 2), Size(handleSize, handleSize), style = Stroke(1.5f))
                                    drawRect(Color.White, Offset(p1.x - handleSize / 2, p1.y - handleSize / 2), Size(handleSize, handleSize))
                                    drawRect(markerColor, Offset(p1.x - handleSize / 2, p1.y - handleSize / 2), Size(handleSize, handleSize), style = Stroke(1.5f))
                                }
                            }
                            MARKER_TYPE_POLYGON -> {
                                if (m.points.size >= MARKER_POLYGON_MIN_POINTS) {
                                    val path = Path()
                                    val pts = m.points.map { Offset(it.first * sx, it.second * sy) }
                                    path.moveTo(pts[0].x, pts[0].y)
                                    for (i in 1 until pts.size) path.lineTo(pts[i].x, pts[i].y)
                                    path.close()
                                    drawPath(path, markerColor, style = Stroke(2f))
                                    pts.forEach { p ->
                                        drawRect(Color.White, Offset(p.x - handleSize / 2, p.y - handleSize / 2), Size(handleSize, handleSize))
                                        drawRect(markerColor, Offset(p.x - handleSize / 2, p.y - handleSize / 2), Size(handleSize, handleSize), style = Stroke(1.5f))
                                    }
                                }
                            }
                        }
                    }
                }
                // Bottom toolbar (two rows)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    // Row 1: Playback controls + palette/flip
                    Row(
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Start
                    IconButton(
                        onClick = {
                            if (!isStreaming) {
                                scope.launch {
                                    frameCount = 0
                                    val streamId = if (selectedStreamId > 0) selectedStreamId
                                        else streamOptions.firstOrNull()?.first ?: run {
                                            val streamsR = NetSDKManager.getStreams()
                                            streamsR.data?.optJSONObject(0)?.optInt("id", 0) ?: 0
                                        }
                                    if (streamId <= 0) {
                                        appendLog(tr("开始视频流失败: 无可用流"))
                                        return@launch
                                    }
                                    appendLog(tr("开启 BGRA 流 id=%s", streamId))
                                    val r = NetSDKManager.startStream(streamId = streamId, streamType = 4)
                                    if (r.isOk()) {
                                        activePlayStreamId = streamId
                                        streamHandle = NetSDKManager.getActiveHandle()
                                        isStreaming = true
                                        appendLog(tr("视频流已开启 id=%s", streamId))
                                    } else {
                                        appendLog(tr("开始视频流失败(id=%s): %s (code=%s)", streamId, r.message, r.code))
                                    }
                                }
                            }
                        },
                        enabled = !isStreaming,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = tr("开始"),
                            tint = if (!isStreaming) Color.White else Color.Gray,
                            modifier = Modifier.size(16.dp))
                    }
                    // Stop
                    IconButton(
                        onClick = {
                            if (isStreaming) {
                                scope.launch {
                                    if (isRecording && activePlayStreamId > 0) {
                                        NetSDKManager.stopStreamRecord(activePlayStreamId)
                                        isRecording = false
                                    }
                                    NetSDKManager.stopStream()
                                    streamHandle = 0L
                                    activePlayStreamId = 0
                                    isStreaming = false
                                }
                            }
                        },
                        enabled = isStreaming,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = tr("停止"),
                            tint = if (isStreaming) Color.Red else Color.Gray,
                            modifier = Modifier.size(16.dp))
                    }
                    // Calibrate (aperture icon)
                    IconButton(
                        onClick = {
                            scope.launch {
                                val r = withContext(Dispatchers.IO) { NetSDKManager.thermalCalibrate() }
                                appendLog(tr("校准: %s", if (r.isOk()) "OK" else tr("失败: %s", r.message)))
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.Adjust, contentDescription = tr("校准"),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp))
                    }
                    // Capture pure image
                    IconButton(
                        onClick = {
                            scope.launch {
                                val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                                    ?: context.filesDir
                                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                val file = File(picturesDir, "capture_${ts}.jpg").absolutePath
                                val r = withContext(Dispatchers.IO) { NetSDKManager.getThermalCapture(0, file) }
                                appendLog(tr("截图: %s", if (r.isOk()) file else tr("失败: %s", r.message)))
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = tr("截图"),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp))
                    }
                    // Record MP4
                    IconButton(
                        onClick = {
                            if (!isRecording) {
                                scope.launch {
                                    val sid = if (activePlayStreamId > 0) activePlayStreamId else selectedStreamId
                                    if (sid <= 0) {
                                        appendLog(tr("开始录制失败: 无可用流 id"))
                                        return@launch
                                    }
                                    val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                                        ?: context.filesDir
                                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    val file = File(moviesDir, "record_$ts.mp4").absolutePath
                                    val r = withContext(Dispatchers.IO) { NetSDKManager.startStreamRecord(sid, file) }
                                    if (r.isOk()) {
                                        isRecording = true
                                        appendLog(tr("开始录制: %s", file))
                                    } else {
                                        appendLog(tr("开始录制失败: %s", r.message))
                                    }
                                }
                            } else {
                                scope.launch {
                                    val sid = if (activePlayStreamId > 0) activePlayStreamId else selectedStreamId
                                    val r = withContext(Dispatchers.IO) { NetSDKManager.stopStreamRecord(sid) }
                                    isRecording = false
                                    appendLog(tr("停止录制: %s", if (r.isOk()) "OK" else tr("失败: %s", r.message)))
                                }
                            }
                        },
                        enabled = isStreaming && activePlayStreamId > 0,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.FiberManualRecord,
                            contentDescription = if (isRecording) tr("停止录制") else tr("录制"),
                            tint = when {
                                !isStreaming || activePlayStreamId <= 0 -> Color.Gray
                                isRecording -> Color.Red
                                else -> Color.White
                            },
                            modifier = Modifier.size(if (isRecording) 14.dp else 16.dp)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Flip dropdown
                    val flipOptions = listOf(0 to tr("不翻转"), 1 to tr("上下"), 2 to tr("左右"), 3 to tr("中心"))
                    var flipExpanded by remember { mutableStateOf(false) }
                    ToolbarDropdown(
                        label = flipOptions.firstOrNull { it.first == selectedFlipMode }?.second ?: tr("翻转"),
                        expanded = flipExpanded,
                        onToggle = { flipExpanded = it },
                        options = flipOptions,
                        onSelect = { id, _ ->
                            selectedFlipMode = id
                            scope.launch {
                                val r = withContext(Dispatchers.IO) { NetSDKManager.setImageFlip(channelId, id) }
                                appendLog(tr("翻转=%s: %s", id, if (r.isOk()) "OK" else tr("失败: %s", r.message)))
                            }
                        }
                    )

                    // Palette color strip (vertical 14x14)
                    if (paletteColors.isNotEmpty()) {
                        PaletteStrip(colors = paletteColors, modifier = Modifier.size(14.dp))
                    }

                    // Palette dropdown
                    var paletteExpanded by remember { mutableStateOf(false) }
                    ToolbarDropdown(
                        label = paletteOptions.firstOrNull { it.first == selectedPaletteRefNo }?.second ?: tr("调色板"),
                        expanded = paletteExpanded,
                        onToggle = { paletteExpanded = it },
                        options = paletteOptions,
                        onSelect = { refNo, _ ->
                            selectedPaletteRefNo = refNo
                            scope.launch {
                                val r = withContext(Dispatchers.IO) { NetSDKManager.setThermalPalette("""{"ref_no":$refNo}""") }
                                appendLog(tr("调色板=%s: %s", refNo, if (r.isOk()) "OK" else tr("失败: %s", r.message)))
                                if (r.isOk()) {
                                    val p = withContext(Dispatchers.IO) { NetSDKManager.getThermalPalette() }
                                    if (p.isOk() && p.data != null) {
                                        val ca = p.data!!.optJSONArray("colors")
                                        if (ca != null && ca.length() > 0) {
                                            paletteColors = IntArray(ca.length()) { i -> ca.optInt(i) }
                                        }
                                    }
                                }
                            }
                        }
                    )

                    // Invert palette
                    IconButton(
                        onClick = {
                            scope.launch {
                                val cur = withContext(Dispatchers.IO) { NetSDKManager.getThermalPalette() }
                                if (cur.isOk() && cur.data != null) {
                                    val inv = if (cur.data!!.optInt("inverted", 0) == 0) 1 else 0
                                    val refNo = cur.data!!.optInt("ref_no", 0)
                                    val r = withContext(Dispatchers.IO) { NetSDKManager.setThermalPalette("""{"ref_no":$refNo,"inverted":$inv}""") }
                                    appendLog(tr("反转调色板: %s", if (r.isOk()) "OK (inv=$inv)" else tr("失败: %s", r.message)))
                                    if (r.isOk()) {
                                        val p = withContext(Dispatchers.IO) { NetSDKManager.getThermalPalette() }
                                        if (p.isOk() && p.data != null) {
                                            val ca = p.data!!.optJSONArray("colors")
                                            if (ca != null && ca.length() > 0) {
                                                paletteColors = IntArray(ca.length()) { i -> ca.optInt(i) }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(Icons.Filled.SwapVert, contentDescription = tr("反转"),
                            tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    }

                    // Row 2: Marker tools + color/range
                    Row(
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    val markerTools = listOf(
                        Triple(0, Icons.Filled.NearMe, if (markerMode == 0) Color(0xFFFF9800) else Color.White),
                        Triple(1, Icons.Filled.FiberManualRecord, if (markerMode == 1) Color(0xFFFF9800) else Color.White),
                        Triple(2, Icons.Filled.Remove, if (markerMode == 2) Color(0xFFFF9800) else Color.White),
                        Triple(3, Icons.Filled.CropSquare, if (markerMode == 3) Color(0xFFFF9800) else Color.White),
                        Triple(-1, Icons.Filled.Delete, if (markerMode == -1) Color(0xFFE53935) else Color.White),
                    )
                    markerTools.forEach { (mode, icon, color) ->
                        IconButton(
                            onClick = { markerMode = mode },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(if (markerMode == mode) color.copy(alpha = 0.3f) else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = color,
                                    modifier = Modifier.size(if (mode == 1) 12.dp else 16.dp))
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Color distribution dropdown — net_enum_color_dist_type
                    val colorDistOptions = listOf(
                        1 to tr("高温差均衡成像分布"),
                        2 to tr("温度线性分布"),
                        3 to tr("信号线性分布")
                    )
                    var colorDistExpanded by remember { mutableStateOf(false) }
                    ToolbarDropdown(
                        label = colorDistOptions.firstOrNull { it.first == selectedColorDist }?.second ?: tr("色彩"),
                        expanded = colorDistExpanded,
                        onToggle = { colorDistExpanded = it },
                        options = colorDistOptions,
                        onSelect = { id, _ ->
                            selectedColorDist = id
                            scope.launch {
                                val r = withContext(Dispatchers.IO) { NetSDKManager.setThermalColorDist(id) }
                                appendLog(tr("色彩分布=%s: %s", id, if (r.isOk()) "OK" else tr("失败: %s", r.message)))
                            }
                        }
                    )

                    // Range dropdown
                    var rangeExpanded by remember { mutableStateOf(false) }
                    ToolbarDropdown(
                        label = rangeOptions.firstOrNull { it.first == selectedRangeId }?.second ?: tr("量程"),
                        expanded = rangeExpanded,
                        onToggle = { rangeExpanded = it },
                        options = rangeOptions,
                        onSelect = { id, _ ->
                            selectedRangeId = id
                            scope.launch {
                                val r = withContext(Dispatchers.IO) { NetSDKManager.setThermalRange(id) }
                                appendLog(tr("量程=%s: %s", id, if (r.isOk()) "OK" else tr("失败: %s", r.message)))
                            }
                        }
                    )
                    }
                }
            }

            // ── Below video: settings tabs (full width) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedMainTab,
                    edgePadding = 0.dp
                ) {
                    mainTabs.forEachIndexed { index, title ->
                        Tab(selected = selectedMainTab == index, onClick = { selectedMainTab = index },
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp).padding(horizontal = 2.dp),
                            text = { Text(title, style = MaterialTheme.typography.labelSmall) })
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    when (selectedMainTab) {
                        0 -> TabProperties(
                            deviceInfoText, lensInfoText, streamsText, rangesText,
                            deviceName, { deviceName = it },
                            selectedStreamId, { selectedStreamId = it },
                            streamOptions, streamDetailText,
                            ::refreshAllProperties, ::refreshStreamDetail, ::runCmd
                        )
                        1 -> TabThermal(
                            selectedShutterMode, { selectedShutterMode = it },
                            selectedShutterTime, { selectedShutterTime = it },
                            tpEmissivity, { tpEmissivity = it },
                            tpDistance, { tpDistance = it },
                            tpReflTemp, { tpReflTemp = it },
                            tpAtmTemp, { tpAtmTemp = it },
                            tpRelHumidity, { tpRelHumidity = it },
                            selectedGainMode, { selectedGainMode = it },
                            gainMax, { gainMax = it },
                            gainMin, { gainMin = it },
                            gainFixedSpan, { gainFixedSpan = it },
                            gainSpan, { gainSpan = it },
                            selectedTempUnit, { selectedTempUnit = it },
                            selectedDistanceUnit, { selectedDistanceUnit = it },
                            ::runCmd
                        )
                        2 -> TabDisplay(
                            channelId, { channelId = it }, channelOptions, { channelOptions = it },
                            channelName, { channelName = it }, resW, { resW = it }, resH, { resH = it },
                            ovChNameEnabled, { ovChNameEnabled = it }, ovChNameAlign, { ovChNameAlign = it }, ovChNameMode, { ovChNameMode = it }, ovChNameX, { ovChNameX = it }, ovChNameY, { ovChNameY = it },
                            ovTimeEnabled, { ovTimeEnabled = it }, ovTimeAlign, { ovTimeAlign = it }, ovTimeMode, { ovTimeMode = it }, ovTimeX, { ovTimeX = it }, ovTimeY, { ovTimeY = it },
                            tovUnit, { tovUnit = it }, tovPlt, { tovPlt = it }, tovThermalParams, { tovThermalParams = it },
                            tovAlarm, { tovAlarm = it },
                            tovGlobal, { tovGlobal = it }, tovMarker, { tovMarker = it },
                            ovCustom, { ovCustom = it }, ovCustomText, { ovCustomText = it },
                            imgContrast, { imgContrast = it }, imgBrightness, { imgBrightness = it }, imgDdeEnable, { imgDdeEnable = it }, imgDdeValue, { imgDdeValue = it },
                            ::runCmd
                        )
                        3 -> TabTime(
                            timeMode, { timeMode = it },
                            timeZone, { timeZone = it },
                            localTime, { localTime = it },
                            ntpHost, { ntpHost = it }, ntpPort, { ntpPort = it }, ntpInterval, { ntpInterval = it },
                            ::runCmd
                        )
                        4 -> TabNetwork(
                            netId, { netId = it },
                            netName, { netName = it },
                            netDhcp, { netDhcp = it },
                            netIp, { netIp = it }, netMask, { netMask = it },
                            netGateway, { netGateway = it }, netDns, { netDns = it },
                            ftpServer, { ftpServer = it }, ftpPort, { ftpPort = it },
                            ftpUser, { ftpUser = it }, ftpPwd, { ftpPwd = it },
                            ftpSsl, { ftpSsl = it }, ftpAnonymous, { ftpAnonymous = it }, ftpPath, { ftpPath = it },
                            portHttp, { portHttp = it }, portRtsp, { portRtsp = it }, portRtp, { portRtp = it },
                            ::runCmd
                        )
                        5 -> TabUsers(
                            userList, { userList = it },
                            selectedUserId, { selectedUserId = it },
                            editUserName, { editUserName = it },
                            editUserLevel, { editUserLevel = it },
                            editLoginPwd, { editLoginPwd = it },
                            editNewPwd, { editNewPwd = it },
                            ::runCmd
                        )
                        6 -> TabMarkers(selectedTempUnit, selectedDistanceUnit, markerTemps, markerAlarmLevels, ::runCmd)
                        7 -> TabData(
                            thermalDataCallbackText,
                            thermalDataStreamActive,
                            { thermalDataStreamActive = it },
                            thermalDataStartupLogRemaining,
                            thermalDataStartupLogTotal,
                            ::runCmd
                        )
                        8 -> TabAlarms(
                            alarmListenerActive, { alarmListenerActive = it },
                            alarmListenerHandle, { alarmListenerHandle = it },
                            alarmEventText, { alarmEventText = it },
                            triggerFtpEnabled, { triggerFtpEnabled = it },
                            triggerWebhookEnabled, { triggerWebhookEnabled = it },
                            selectedTempUnit,
                            alarmStartupLogRemaining,
                            alarmStartupLogTotal,
                            ::runCmd
                        )
                    }
                }
            }
            }

            // Floating log panel
            if (showLogPanel) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 60.dp, end = 10.dp)
                        .width(280.dp)
                        .height(320.dp)
                        .shadow(6.dp, RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tr("日志"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Row {
                                TextButton(onClick = { logText = "" }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                                    Text(tr("清空"), style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { showLogPanel = false }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = tr("关闭"), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(4.dp)
                        ) {
                            val scrollState = rememberScrollState()
                            LaunchedEffect(logText) { scrollState.animateScrollTo(scrollState.maxValue) }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                            ) {
                                Text(
                                    logText.ifEmpty { tr("操作结果将显示在此处") },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 12.sp),
                                    color = if (logText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Draggable log FAB
            FloatingActionButton(
                onClick = { showLogPanel = !showLogPanel },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .offset { IntOffset(fabOffsetX.roundToInt(), fabOffsetY.roundToInt()) }
                    .size(40.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            fabOffsetX += dragAmount.x
                            fabOffsetY += dragAmount.y
                        }
                    }
            ) {
                BadgedBox(badge = {
                    if (!showLogPanel && logText.isNotEmpty()) {
                        Badge(containerColor = MaterialTheme.colorScheme.error, modifier = Modifier.size(8.dp))
                    }
                }) {
                    if (showLogPanel) {
                        Icon(Icons.Filled.Close, contentDescription = tr("收起日志"), modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Filled.Description, contentDescription = tr("展开日志"), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ==================== Log Section ====================

@Composable
private fun LogSection(logText: String, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr("日志"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                    Text(tr("清空"), style = MaterialTheme.typography.labelSmall)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                    .padding(3.dp)
            ) {
                val scrollState = rememberScrollState()
                LaunchedEffect(logText) { scrollState.animateScrollTo(scrollState.maxValue) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        logText.ifEmpty { tr("操作结果将显示在此处") },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 12.sp),
                        color = if (logText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ==================== Tab 0: Properties ====================

@Composable
private fun TabProperties(
    deviceInfoText: String?,
    lensInfoText: String?,
    streamsText: String?,
    rangesText: String?,
    deviceName: String,
    setDeviceName: (String) -> Unit,
    selectedStreamId: Int,
    setSelectedStreamId: (Int) -> Unit,
    streamOptions: List<Pair<Int, String>>,
    streamDetailText: String?,
    onRefresh: () -> Unit,
    onRefreshStreamDetail: () -> Unit,
    runCmd: (String, suspend () -> String) -> Unit
) {
    val context = LocalContext.current
    var showRestartConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showUpgradeConfirm by remember { mutableStateOf(false) }
    var pendingUpgradePath by remember { mutableStateOf<String?>(null) }

    val upgradeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCmd(tr("复制升级包")) {
                val dir = File(context.filesDir, "upgrade").apply { mkdirs() }
                val dest = File(dir, "firmware.bin")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                } ?: return@runCmd tr("无法读取所选文件")
                pendingUpgradePath = dest.absolutePath
                showUpgradeConfirm = true
                tr("已复制到 %s", dest.absolutePath)
            }
        }
    }

    if (showRestartConfirm) {
        AlertDialog(
            onDismissRequest = { showRestartConfirm = false },
            title = { Text(tr("确认重启")) },
            text = { Text(tr("确定要重启设备吗？")) },
            confirmButton = {
                TextButton(onClick = {
                    showRestartConfirm = false
                    runCmd(tr("设备重启")) {
                        val r = NetSDKManager.deviceRestart()
                        if (r.isOk()) "OK" else tr("失败: %s", r.message)
                    }
                }) { Text(tr("重启")) }
            },
            dismissButton = { TextButton(onClick = { showRestartConfirm = false }) { Text(tr("取消")) } }
        )
    }
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(tr("确认恢复出厂")) },
            text = { Text(tr("确定要恢复出厂设置吗？此操作不可撤销。")) },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    runCmd(tr("恢复出厂")) {
                        val r = NetSDKManager.deviceReset()
                        if (r.isOk()) "OK" else tr("失败: %s", r.message)
                    }
                }) { Text(tr("恢复")) }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text(tr("取消")) } }
        )
    }
    if (showUpgradeConfirm && pendingUpgradePath != null) {
        AlertDialog(
            onDismissRequest = { showUpgradeConfirm = false },
            title = { Text(tr("确认升级")) },
            text = { Text(tr("使用以下文件升级固件？\n%s", pendingUpgradePath)) },
            confirmButton = {
                TextButton(onClick = {
                    val path = pendingUpgradePath
                    showUpgradeConfirm = false
                    pendingUpgradePath = null
                    runCmd(tr("固件升级")) {
                        tr("当前厂商 AAR (1.0.1.73) 未导出 deviceUpgrade 接口")
                    }
                }) { Text(tr("升级")) }
            },
            dismissButton = { TextButton(onClick = { showUpgradeConfirm = false }) { Text(tr("取消")) } }
        )
    }

    SectionCard(tr("设备信息"), action = {
        IconButton(onClick = onRefresh, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"),
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        Text(deviceInfoText ?: tr("加载中..."), style = MaterialTheme.typography.bodySmall)
    }

    SectionCard(tr("镜头信息")) {
        Text(lensInfoText ?: tr("加载中..."), style = MaterialTheme.typography.bodySmall)
        if (rangesText != null) {
            Spacer(Modifier.height(2.dp))
            Text(rangesText, style = MaterialTheme.typography.bodySmall)
        }
    }

    SectionCard(tr("视频流"), action = {
        IconButton(onClick = onRefreshStreamDetail, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新流详情"),
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        Text(streamsText ?: tr("加载中..."), style = MaterialTheme.typography.bodySmall)
        if (streamOptions.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            CompactSettingRow(tr("流 ID"), streamOptions, selectedStreamId, setSelectedStreamId, showSetButton = false) {}
            Text(streamDetailText ?: tr("点击刷新查看流详情"), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    SectionCard(tr("设备维护")) {
        CompactInputRow(tr("设备名称"), deviceName, setDeviceName)
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick = {
                    runCmd(tr("设置设备名称")) {
                        val r = NetSDKManager.setDeviceInfo("""{"name":"$deviceName"}""")
                        if (r.isOk()) "OK" else tr("失败: %s", r.message)
                    }
                },
                modifier = Modifier.weight(1f).height(24.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) { Text(tr("设置名称"), style = MaterialTheme.typography.labelSmall) }
            Button(
                onClick = { showRestartConfirm = true },
                modifier = Modifier.weight(1f).height(24.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) { Text(tr("重启"), style = MaterialTheme.typography.labelSmall) }
        }
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick = { showResetConfirm = true },
                modifier = Modifier.weight(1f).height(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) { Text(tr("恢复出厂"), style = MaterialTheme.typography.labelSmall) }
            Button(
                onClick = { upgradeLauncher.launch(arrayOf("application/octet-stream")) },
                modifier = Modifier.weight(1f).height(24.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) { Text(tr("固件升级"), style = MaterialTheme.typography.labelSmall) }
        }
    }
}

// ==================== Tab 1: Thermal ====================

@Composable
private fun TabThermal(
    shutterMode: Int, setShutterMode: (Int) -> Unit,
    shutterTime: Int, setShutterTime: (Int) -> Unit,
    emissivity: String, setEmissivity: (String) -> Unit,
    distance: String, setDistance: (String) -> Unit,
    reflTemp: String, setReflTemp: (String) -> Unit,
    atmTemp: String, setAtmTemp: (String) -> Unit,
    relHumidity: String, setRelHumidity: (String) -> Unit,
    gainMode: Int, setGainMode: (Int) -> Unit,
    gainMax: String, setGainMax: (String) -> Unit,
    gainMin: String, setGainMin: (String) -> Unit,
    gainFixedSpan: Int, setGainFixedSpan: (Int) -> Unit,
    gainSpan: String, setGainSpan: (String) -> Unit,
    tempUnit: Int, setTempUnit: (Int) -> Unit,
    distanceUnit: Int, setDistanceUnit: (Int) -> Unit,
    runCmd: (String, suspend () -> String) -> Unit
) {
    SectionCard(tr("校正"), action = {
        IconButton(onClick = {
            runCmd(tr("刷新校正")) {
                val mr = NetSDKManager.getThermalShutterMode()
                if (mr.isOk() && mr.data != null) setShutterMode(mr.data!!)
                val shutterTimeR = NetSDKManager.getThermalShutterTime()
                if (shutterTimeR.isOk() && shutterTimeR.data != null) setShutterTime(shutterTimeR.data!!)
                if (mr.isOk()) "mode=${mr.data}, interval=${shutterTimeR.data}" else tr("失败")
            }
        }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        CompactSettingRow(tr("校正模式"), listOf(0 to tr("自动"), 1 to tr("手动")), shutterMode, setShutterMode) {
            runCmd(tr("设置校正模式")) { val r = NetSDKManager.setThermalShutterMode(shutterMode); if (r.isOk()) "OK" else tr("失败: %s", r.message) }
        }
        CompactSettingRow(tr("校正间隔(分)"), listOf(1 to "1", 5 to "5", 10 to "10", 15 to "15", 30 to "30", 60 to "60"), shutterTime, setShutterTime) {
            runCmd(tr("设置校正时间")) { val r = NetSDKManager.setThermalShutterTime(shutterTime); if (r.isOk()) "OK" else tr("失败: %s", r.message) }
        }
    }

    SectionCard(tr("测温参数"), action = {
        IconButton(onClick = {
            runCmd(tr("刷新测温参数")) {
                val r = NetSDKManager.getThermalParams()
                if (r.isOk() && r.data != null) {
                    val d = r.data!!
                    setEmissivity("%.2f".format(Locale.US, d.optDouble("emissivity", 0.95)))
                    setDistance(formatDistValue(d.optDouble("distance", 1.0), distanceUnit))
                    setReflTemp(formatTempValue(d.optDouble("refl_temp", 20.0), tempUnit))
                    setAtmTemp(formatTempValue(d.optDouble("atm_temp", 20.0), tempUnit))
                    setRelHumidity("%.2f".format(Locale.US, d.optDouble("rel_humidity", 0.5)))
                    "OK"
                } else tr("失败: %s", r.message)
            }
        }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        CompactInputRow(tr("发射率"), emissivity, setEmissivity)
        CompactInputRow(tr("距离(%s)", distUnitLabel(distanceUnit)), distance, setDistance)
        CompactInputRow(tr("反射温度(%s)", tempUnitLabel(tempUnit)), reflTemp, setReflTemp)
        CompactInputRow(tr("大气温度(%s)", tempUnitLabel(tempUnit)), atmTemp, setAtmTemp)
        CompactInputRow(tr("相对湿度"), relHumidity, setRelHumidity)
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                runCmd(tr("设置测温参数")) {
                    val json = """{"emissivity":${emissivity.toDoubleOrNull() ?: 0.95},"distance":${parseDistInput(distance, distanceUnit)},"refl_temp":${parseTempInput(reflTemp, tempUnit)},"atm_temp":${parseTempInput(atmTemp, tempUnit)},"rel_humidity":${relHumidity.toDoubleOrNull() ?: 0.5}}"""
                    val r = NetSDKManager.setThermalParams(json)
                    if (r.isOk()) "OK" else tr("失败: %s", r.message)
                }
            }, modifier = Modifier.height(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(tr("设置"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    SectionCard(tr("温宽"), action = {
        IconButton(onClick = {
            runCmd(tr("刷新温宽")) {
                val r = NetSDKManager.getThermalGain()
                if (r.isOk() && r.data != null) {
                    val d = r.data!!
                    setGainMode(d.optInt("mode", 2))
                    setGainMax(formatTempValue(d.optDouble("max", 0.0), tempUnit))
                    setGainMin(formatTempValue(d.optDouble("min", 0.0), tempUnit))
                    val fs = when (val raw = d.opt("fixedSpan")) {
                        is Boolean -> if (raw) 1 else 0
                        is Number -> if (raw.toInt() != 0) 1 else 0
                        else -> d.optInt("fixedSpan", 0)
                    }
                    setGainFixedSpan(fs)
                    setGainSpan(formatTempValue(d.optDouble("span", 0.0), tempUnit))
                    "mode=${d.optInt("mode")}"
                } else tr("失败: %s", r.message)
            }
        }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        CompactSettingRow(tr("模式"), listOf(2 to tr("自动"), 1 to tr("固定")), gainMode, setGainMode, showSetButton = false) {}
        if (gainMode == 1) {
            CompactSettingRow(tr("固定Span"), listOf(0 to tr("关闭"), 1 to tr("开启")), gainFixedSpan, setGainFixedSpan, showSetButton = false) {}
            if (gainFixedSpan == 1) {
                CompactInputRow("Span(${tempUnitLabel(tempUnit)})", gainSpan, setGainSpan)
            }
            CompactInputRow(tr("上限(%s)", tempUnitLabel(tempUnit)), gainMax, setGainMax)
            CompactInputRow(tr("下限(%s)", tempUnitLabel(tempUnit)), gainMin, setGainMin)
        }
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                runCmd(tr("设置温宽")) {
                    // 设备要求自动/固定都带 max/min；仅 {"mode":2} 会返回 Invalid enum value -> SDK -7
                    var maxV = parseTempInput(gainMax, tempUnit)
                    var minV = parseTempInput(gainMin, tempUnit)
                    var spanV = parseTempInput(gainSpan, tempUnit)
                    if (gainMax.isBlank() || gainMin.isBlank()) {
                        val cur = NetSDKManager.getThermalGain()
                        if (cur.isOk() && cur.data != null) {
                            val d = cur.data!!
                            if (gainMax.isBlank()) maxV = d.optDouble("max", maxV)
                            if (gainMin.isBlank()) minV = d.optDouble("min", minV)
                            if (gainSpan.isBlank()) spanV = d.optDouble("span", spanV)
                        }
                    }
                    val fixedSpanBool = gainMode == 1 && gainFixedSpan == 1
                    val spanOut = when {
                        gainMode != 1 -> 0.0
                        fixedSpanBool -> spanV
                        else -> maxV - minV
                    }
                    val json =
                        """{"mode":$gainMode,"max":$maxV,"min":$minV,"fixedSpan":$fixedSpanBool,"span":$spanOut}"""
                    val r = NetSDKManager.setThermalGain(json)
                    if (r.isOk()) "OK" else tr("失败: %s", r.message)
                }
            }, modifier = Modifier.height(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(tr("设置"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    SectionCard(tr("单位设置"), action = {
        IconButton(onClick = {
            runCmd(tr("刷新单位")) {
                val r = NetSDKManager.getThermalUnit()
                if (r.isOk() && r.data != null) {
                    val newTempUnit = r.data!!.optInt("temp_unit")
                    val newDistUnit = r.data!!.optInt("distance_unit")
                    setTempUnit(newTempUnit)
                    setDistanceUnit(newDistUnit)
                    val pr = NetSDKManager.getThermalParams()
                    if (pr.isOk() && pr.data != null) {
                        val d = pr.data!!
                        setEmissivity("%.2f".format(Locale.US, d.optDouble("emissivity", 0.95)))
                        setDistance(formatDistValue(d.optDouble("distance", 1.0), newDistUnit))
                        setReflTemp(formatTempValue(d.optDouble("refl_temp", 20.0), newTempUnit))
                        setAtmTemp(formatTempValue(d.optDouble("atm_temp", 20.0), newTempUnit))
                        setRelHumidity("%.2f".format(Locale.US, d.optDouble("rel_humidity", 0.5)))
                    }
                    val gr = NetSDKManager.getThermalGain()
                    if (gr.isOk() && gr.data != null) {
                        val d = gr.data!!
                        setGainMode(d.optInt("mode", 2))
                        setGainMax(formatTempValue(d.optDouble("max", 0.0), newTempUnit))
                        setGainMin(formatTempValue(d.optDouble("min", 0.0), newTempUnit))
                        setGainFixedSpan(d.optInt("fixedSpan", 0))
                        setGainSpan(formatTempValue(d.optDouble("span", 0.0), newTempUnit))
                    }
                    "OK"
                } else tr("失败: %s", r.message)
            }
        }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        CompactSettingRow(tr("温度单位"), listOf(0 to tr("摄氏度"), 1 to tr("开尔文"), 2 to tr("华氏度")), tempUnit, { newUnit ->
            if (newUnit != tempUnit) {
                setReflTemp(convertTempDisplayValue(reflTemp, tempUnit, newUnit))
                setAtmTemp(convertTempDisplayValue(atmTemp, tempUnit, newUnit))
                if (gainMode == 1) {
                    if (gainFixedSpan == 1) {
                        setGainSpan(convertTempDisplayValue(gainSpan, tempUnit, newUnit))
                    } else {
                        setGainMax(convertTempDisplayValue(gainMax, tempUnit, newUnit))
                        setGainMin(convertTempDisplayValue(gainMin, tempUnit, newUnit))
                    }
                }
                setTempUnit(newUnit)
            }
        }) {
            runCmd(tr("设置单位")) { val r = NetSDKManager.setThermalUnit("""{"temp_unit":$tempUnit,"distance_unit":$distanceUnit}"""); if (r.isOk()) "OK" else tr("失败: %s", r.message) }
        }
        CompactSettingRow(tr("距离单位"), listOf(0 to tr("米"), 1 to tr("英尺")), distanceUnit, { newUnit ->
            if (newUnit != distanceUnit) {
                setDistance(convertDistDisplayValue(distance, distanceUnit, newUnit))
                setDistanceUnit(newUnit)
            }
        }) {
            runCmd(tr("设置单位")) { val r = NetSDKManager.setThermalUnit("""{"temp_unit":$tempUnit,"distance_unit":$distanceUnit}"""); if (r.isOk()) "OK" else tr("失败: %s", r.message) }
        }
    }
}

// ==================== Tab: 数据 (Thermal Data) ====================

@Composable
private fun TabData(
    callbackSummary: String,
    thermalDataStreamActive: Boolean,
    setThermalDataStreamActive: (Boolean) -> Unit,
    thermalDataStartupLogRemaining: AtomicInteger,
    thermalDataStartupLogTotal: Int,
    runCmd: (String, suspend () -> String) -> Unit
) {
    var rawEnabled by remember { mutableStateOf(true) }
    var globalEnabled by remember { mutableStateOf(false) }
    var markerEnabled by remember { mutableStateOf(false) }
    var sampleInterval by remember { mutableStateOf("100") }

    fun dataTypesMask(): Int {
        var mask = 0
        if (rawEnabled) mask = mask or 1
        if (globalEnabled) mask = mask or 2
        if (markerEnabled) mask = mask or 4
        return mask
    }

    SectionCard(tr("数据流配置")) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CheckboxRow("RAW(1)", rawEnabled) { rawEnabled = it }
            CheckboxRow("GLOBAL(2)", globalEnabled) { globalEnabled = it }
            CheckboxRow("MARKER(4)", markerEnabled) { markerEnabled = it }
        }
        CompactInputRow(tr("采样间隔(ms)"), sampleInterval) { sampleInterval = it }
        Row(Modifier.fillMaxWidth().height(30.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    runCmd(tr("开始热像数据流")) {
                        val interval = (sampleInterval.toIntOrNull() ?: 100).coerceAtLeast(100)
                        val mask = dataTypesMask()
                        if (mask == 0) return@runCmd tr("请至少选择一种数据类型")
                        val deviceHandle = NetSDKManager.getActiveHandle()
                        if (deviceHandle == 0L) return@runCmd tr("未连接设备")
                        val json = """{"data_types":$mask,"sample_interval":$interval}"""
                        val cfg = NativeSDK.nativeSetThermalDataStreamConfig(deviceHandle, json)
                        if (!sdkOk(cfg)) return@runCmd tr("配置失败: %s", cfg)
                        val ret = NativeSDK.nativeSetThermalDataStreamCallback(deviceHandle, true)
                        if (!sdkOk(ret)) return@runCmd tr("失败: %s", ret)
                        thermalDataStartupLogRemaining.set(thermalDataStartupLogTotal)
                        withContext(Dispatchers.Main) {
                            setThermalDataStreamActive(true)
                        }
                        tr("OK(cfg=%s,cb=%s), 将输出前%s条热像数据回调到日志", cfg, ret, thermalDataStartupLogTotal)
                    }
                },
                enabled = !thermalDataStreamActive,
                modifier = Modifier.weight(1f).height(26.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) { Text(tr("开始数据流"), style = MaterialTheme.typography.labelSmall) }
            Button(
                onClick = {
                    runCmd(tr("停止热像数据流")) {
                        if (!thermalDataStreamActive) return@runCmd tr("未启动")
                        val deviceHandle = NetSDKManager.getActiveHandle()
                        if (deviceHandle == 0L) return@runCmd tr("未连接设备")
                        val ret = NativeSDK.nativeSetThermalDataStreamCallback(deviceHandle, false)
                        if (!sdkOk(ret)) return@runCmd tr("失败: %s", ret)
                        thermalDataStartupLogRemaining.set(0)
                        withContext(Dispatchers.Main) {
                            setThermalDataStreamActive(false)
                        }
                        "OK(cb=$ret)"
                    }
                },
                enabled = thermalDataStreamActive,
                modifier = Modifier.weight(1f).height(26.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) { Text(tr("停止数据流"), style = MaterialTheme.typography.labelSmall) }
        }
    }

    SectionCard(tr("单次获取")) {
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = {
                    runCmd(tr("获取热像数据")) {
                        val mask = dataTypesMask()
                        if (mask == 0) return@runCmd tr("请至少选择一种数据类型")
                        val r = NetSDKManager.getThermalData(mask)
                        if (r.isOk() && r.data != null) {
                            val d = r.data!!
                            "types=${d.optInt("data_types")}, markers=${d.optJSONArray("marker_temps")?.length() ?: 0}"
                        } else tr("失败: %s", r.message)
                    }
                },
                modifier = Modifier.height(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) { Text(tr("获取"), style = MaterialTheme.typography.labelSmall) }
        }
    }

    SectionCard(tr("回调摘要")) {
        Text(
            if (callbackSummary.isEmpty()) tr("尚未收到热像数据回调") else callbackSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== Tab 4: Network ====================

@Composable
private fun TabNetwork(
    netId: Int, setNetId: (Int) -> Unit,
    netName: String, setNetName: (String) -> Unit,
    dhcp: Int, setDhcp: (Int) -> Unit,
    ip: String, setIp: (String) -> Unit, mask: String, setMask: (String) -> Unit,
    gateway: String, setGateway: (String) -> Unit, dns: String, setDns: (String) -> Unit,
    ftpServer: String, setFtpServer: (String) -> Unit, ftpPort: String, setFtpPort: (String) -> Unit,
    ftpUser: String, setFtpUser: (String) -> Unit, ftpPwd: String, setFtpPwd: (String) -> Unit,
    ftpSsl: Int, setFtpSsl: (Int) -> Unit, ftpAnonymous: Int, setFtpAnonymous: (Int) -> Unit, ftpPath: String, setFtpPath: (String) -> Unit,
    httpPort: String, setHttpPort: (String) -> Unit, rtspPort: String, setRtspPort: (String) -> Unit, rtpPort: String, setRtpPort: (String) -> Unit,
    runCmd: (String, suspend () -> String) -> Unit
) {
    fun applyNetworkInfo(n: JSONObject) {
        setNetId(n.optInt("id", 0))
        setNetName(n.optString("name", "eth0").ifBlank { "eth0" })
        val dhcpVal = when (val raw = n.opt("dhcp")) {
            is Boolean -> if (raw) 1 else 0
            is Number -> if (raw.toInt() != 0) 1 else 0
            else -> n.optInt("dhcp", 0)
        }
        setDhcp(dhcpVal)
        setIp(n.optString("ip", ""))
        setMask(n.optString("netmask", ""))
        val gw = n.optJSONArray("gateway")
        if (gw != null && gw.length() > 0) setGateway(gw.getString(0)) else setGateway("")
        val dn = n.optJSONArray("dns")
        if (dn != null && dn.length() > 0) setDns(dn.getString(0)) else setDns("")
    }

    fun buildNetworkJson(id: Int, name: String): String {
        // 设备 Web API 要求：dhcp 为布尔；带 id/name；空 gateway/dns 发 [] 而不是 [""]
        val dhcpJson = if (dhcp != 0) "true" else "false"
        val gatewayJson = if (gateway.isNotBlank()) """["$gateway"]""" else "[]"
        val dnsJson = if (dns.isNotBlank()) """["$dns"]""" else "[]"
        return """{"id":$id,"name":"$name","dhcp":$dhcpJson,"ip":"$ip","netmask":"$mask","gateway":$gatewayJson,"dns":$dnsJson}"""
    }

    SectionCard(tr("IP 设置"), action = {
        IconButton(onClick = {
            runCmd(tr("刷新网络")) {
                val r = NetSDKManager.getNetworks()
                if (r.isOk() && r.data != null && r.data!!.length() > 0) {
                    val n = r.data!!.getJSONObject(0)
                    withContext(Dispatchers.Main) { applyNetworkInfo(n) }
                    "OK, id=${n.optInt("id")}, name=${n.optString("name")}, ip=${n.optString("ip")}"
                } else tr("失败: %s", r.message)
            }
        }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        CompactSettingRow("DHCP", listOf(0 to tr("关闭"), 1 to tr("开启")), dhcp, setDhcp, showSetButton = false) {}
        CompactInputRow(tr("IP地址"), ip, setIp)
        CompactInputRow(tr("子网掩码"), mask, setMask)
        CompactInputRow(tr("网关"), gateway, setGateway)
        CompactInputRow("DNS", dns, setDns)
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                runCmd(tr("设置网络")) {
                    var id = netId
                    var name = netName.ifBlank { "eth0" }
                    if (id <= 0) {
                        val networks = NetSDKManager.getNetworks()
                        if (!networks.isOk() || networks.data == null || networks.data!!.length() == 0) {
                            return@runCmd tr("失败: 无法获取网卡列表 (%s)", networks.message)
                        }
                        val n = networks.data!!.getJSONObject(0)
                        id = n.optInt("id", 0)
                        name = n.optString("name", "eth0").ifBlank { "eth0" }
                        if (id <= 0) return@runCmd tr("失败: 无效网卡 id")
                        withContext(Dispatchers.Main) { applyNetworkInfo(n) }
                    }
                    val json = buildNetworkJson(id, name)
                    val r = NetSDKManager.setNetwork(id, json)
                    if (r.isOk()) "OK(id=$id)" else tr("失败: %s", r.message)
                }
            }, modifier = Modifier.height(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(tr("设置"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    SectionCard("FTP", action = {
        IconButton(onClick = {
            runCmd(tr("刷新FTP")) {
                val r = NetSDKManager.getFtp()
                if (r.isOk() && r.data != null) {
                    val d = r.data!!
                    setFtpServer(d.optString("ip", ""))
                    setFtpPort(d.optInt("port_no", 21).toString())
                    setFtpUser(d.optString("user_name", ""))
                    setFtpPwd(d.optString("pwd", ""))
                    setFtpSsl(d.optInt("use_ssl", 0))
                    setFtpAnonymous(d.optInt("anonymous", 0))
                    val pt = d.optString("path_template", "")
                    if (pt.isNotEmpty()) setFtpPath(pt)
                    "OK"
                } else tr("失败: %s", r.message)
            }
        }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        CompactInputRow(tr("服务器"), ftpServer, setFtpServer)
        CompactInputRow(tr("端口"), ftpPort, setFtpPort)
        CompactSettingRow(tr("匿名登录"), listOf(0 to tr("关闭"), 1 to tr("开启")), ftpAnonymous, setFtpAnonymous, showSetButton = false) {}
        if (ftpAnonymous == 0) {
            CompactInputRow(tr("用户名"), ftpUser, setFtpUser)
            CompactInputRow(tr("密码"), ftpPwd, setFtpPwd)
        }
        CompactSettingRow("SSL", listOf(0 to tr("关闭"), 1 to tr("开启")), ftpSsl, setFtpSsl, showSetButton = false) {}
        CompactInputRow(tr("路径模板"), ftpPath, setFtpPath)
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                runCmd(tr("设置FTP")) {
                    val json = """{"ip":"$ftpServer","port_no":${ftpPort.toIntOrNull() ?: 21},"anonymous":$ftpAnonymous,"user_name":"$ftpUser","pwd":"$ftpPwd","use_ssl":$ftpSsl,"path_template":"$ftpPath"}"""
                    val r = NetSDKManager.setFtp(json)
                    if (r.isOk()) "OK" else tr("失败: %s", r.message)
                }
            }, modifier = Modifier.height(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(tr("设置"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    SectionCard(tr("端口"), action = {
        IconButton(onClick = {
            runCmd(tr("刷新端口")) {
                val r = NetSDKManager.getPort()
                if (r.isOk() && r.data != null) {
                    val d = r.data!!
                    setHttpPort(d.optInt("http_port_no", 80).toString())
                    setRtspPort(d.optInt("rtsp_port_no", 554).toString())
                    setRtpPort(d.optInt("rtp_port_no", 5004).toString())
                    "OK"
                } else tr("失败: %s", r.message)
            }
        }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        CompactInputRow(tr("HTTP端口"), httpPort, setHttpPort)
        CompactInputRow(tr("RTSP端口"), rtspPort, setRtspPort)
        CompactInputRow(tr("RTP端口"), rtpPort, setRtpPort)
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                runCmd(tr("设置端口")) {
                    val json = """{"http_port_no":${httpPort.toIntOrNull() ?: 80},"rtsp_port_no":${rtspPort.toIntOrNull() ?: 554},"rtp_port_no":${rtpPort.toIntOrNull() ?: 5004}}"""
                    val r = NetSDKManager.setPort(json)
                    if (r.isOk()) "OK" else tr("失败: %s", r.message)
                }
            }, modifier = Modifier.height(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(tr("设置"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }

}

@Composable
private fun TabTime(
    timeMode: Int, setTimeMode: (Int) -> Unit,
    timeZone: String, setTimeZone: (String) -> Unit,
    localTime: String, setLocalTime: (String) -> Unit,
    ntpHost: String, setNtpHost: (String) -> Unit, ntpPort: String, setNtpPort: (String) -> Unit, ntpInterval: String, setNtpInterval: (String) -> Unit,
    runCmd: (String, suspend () -> String) -> Unit
) {
    val tzOptions = remember { listOf(
        "UTC-12:00" to "UTC-12:00", "UTC-11:00" to "UTC-11:00", "UTC-10:00" to "UTC-10:00", "UTC-09:00" to "UTC-09:00",
        "UTC-08:00" to "UTC-08:00", "UTC-07:00" to "UTC-07:00", "UTC-06:00" to "UTC-06:00", "UTC-05:00" to "UTC-05:00",
        "UTC-04:00" to "UTC-04:00", "UTC-03:00" to "UTC-03:00", "UTC-02:00" to "UTC-02:00", "UTC-01:00" to "UTC-01:00",
        "UTC+00:00" to "UTC+00:00",
        "UTC+01:00" to "UTC+01:00", "UTC+02:00" to "UTC+02:00", "UTC+03:00" to "UTC+03:00", "UTC+04:00" to "UTC+04:00",
        "UTC+05:00" to "UTC+05:00", "UTC+05:30" to "UTC+05:30", "UTC+06:00" to "UTC+06:00", "UTC+07:00" to "UTC+07:00",
        "UTC+08:00" to "UTC+08:00", "UTC+09:00" to "UTC+09:00", "UTC+09:30" to "UTC+09:30", "UTC+10:00" to "UTC+10:00",
        "UTC+11:00" to "UTC+11:00", "UTC+12:00" to "UTC+12:00", "UTC+13:00" to "UTC+13:00"
    ) }

    SectionCard(tr("时间设置"), action = {
        IconButton(onClick = {
            runCmd(tr("刷新时间")) {
                val r = NetSDKManager.getTime()
                if (r.isOk() && r.data != null) {
                    val d = r.data!!
                    setTimeMode(d.optInt("time_mode", 0))
                    setTimeZone(d.optString("time_zone", ""))
                    setLocalTime(d.optString("local_time", ""))
                    "mode=${d.optInt("time_mode")}, tz=${d.optString("time_zone")}"
                } else tr("失败: %s", r.message)
            }
            runCmd(tr("刷新NTP")) {
                val r = NetSDKManager.getNtpServer(0)
                if (r.isOk() && r.data != null) {
                    val s = r.data!!
                    setNtpHost(s.optString("host_name", ""))
                    setNtpPort(s.optInt("port_no", 123).toString())
                    setNtpInterval(s.optInt("syn_interval", 60).toString())
                    "OK"
                } else tr("失败: %s", r.message)
            }
        }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        CompactSettingRow(tr("模式"), listOf(0 to tr("本地"), 1 to "NTP"), timeMode, setTimeMode, showSetButton = false) {}
        CompactSettingRow(tr("时区"), tzOptions, timeZone, setTimeZone, showSetButton = false) {}
    }

    if (timeMode == 0) {
        SectionCard(tr("本地时间")) {
            Row(
                Modifier.fillMaxWidth().height(26.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr("时间"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(32.dp),
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                BasicTextField(
                    value = localTime,
                    onValueChange = setLocalTime,
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp),
                    textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() }
                    }
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    val now = sdf.format(java.util.Date())
                    setLocalTime(now)
                }, modifier = Modifier.height(24.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text(tr("同步"), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.width(4.dp))
                Button(onClick = {
                    runCmd(tr("设置本地时间")) {
                        val json = """{"time_mode":0,"time_zone":"$timeZone","local_time":"$localTime"}"""
                        val r = NetSDKManager.setTime(json)
                        if (r.isOk()) "OK, $localTime" else tr("失败: %s", r.message)
                    }
                }, modifier = Modifier.height(24.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text(tr("设置"), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    } else {
        SectionCard(tr("NTP 服务器")) {
            CompactInputRow(tr("主机"), ntpHost, setNtpHost)
            CompactInputRow(tr("端口"), ntpPort, setNtpPort)
            CompactInputRow(tr("同步间隔(s)"), ntpInterval, setNtpInterval)
            Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = {
                    runCmd(tr("设置NTP")) {
                        val json = """{"host_name":"$ntpHost","port_no":${ntpPort.toIntOrNull() ?: 123},"syn_interval":${ntpInterval.toIntOrNull() ?: 60}}"""
                        val rTime = NetSDKManager.setTime("""{"time_mode":1,"time_zone":"$timeZone"}""")
                        val rNtp = NetSDKManager.setNtpServer(0, json)
                        if (rTime.isOk() && rNtp.isOk()) "OK" else tr("失败")
                    }
                }, modifier = Modifier.height(24.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text(tr("设置"), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ==================== Tab 5: Users ====================

@Composable
private fun TabUsers(
    userList: JSONArray, setUserList: (JSONArray) -> Unit,
    selectedId: Int, setSelectedId: (Int) -> Unit,
    userName: String, setUserName: (String) -> Unit,
    userLevel: Int, setUserLevel: (Int) -> Unit,
    loginPwd: String, setLoginPwd: (String) -> Unit,
    newPwd: String, setNewPwd: (String) -> Unit,
    runCmd: (String, suspend () -> String) -> Unit
) {
    val levelOptions = listOf(0 to tr("管理员"), 1 to tr("操作员"), 2 to tr("观察者"))

    fun refreshUsers() {
        runCmd(tr("刷新用户")) {
            val r = NetSDKManager.getUsers()
            if (r.isOk() && r.data != null) {
                setUserList(r.data!!)
                tr("共%s个用户", r.data!!.length())
            } else tr("失败: %s", r.message)
        }
    }

    SectionCard(tr("用户列表"), action = {
        IconButton(onClick = { refreshUsers() }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text("ID", style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.weight(0.5f))
            Text(tr("用户名"), style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.weight(1.5f))
            Text(tr("权限"), style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        for (i in 0 until userList.length()) {
            val u = userList.getJSONObject(i)
            val id = u.optInt("id", -1)
            val isSelected = id == selectedId
            Surface(
                onClick = {
                    setSelectedId(id)
                    setUserName(u.optString("user_name", ""))
                    setUserLevel(u.optInt("user_level", 0))
                    setLoginPwd("")
                    setNewPwd("")
                },
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text("$id", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.5f))
                    Text(u.optString("user_name", ""), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.5f))
                    val lvl = levelOptions.firstOrNull { it.first == u.optInt("user_level") }?.second ?: "${u.optInt("user_level")}"
                    Text(lvl, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                }
            }
            if (i < userList.length() - 1) HorizontalDivider()
        }
    }

    SectionCard(tr("用户编辑")) {
        CompactInputRow(tr("用户名"), userName, setUserName)
        CompactSettingRow(tr("权限"), levelOptions, userLevel, setUserLevel, showSetButton = false) {}
        CompactInputRow(tr("登录密码"), loginPwd, setLoginPwd)
        CompactInputRow(tr("新密码"), newPwd, setNewPwd)
        Row(
            Modifier.fillMaxWidth().height(26.dp).padding(top = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
        ) {
            Button(onClick = {
                runCmd(tr("添加用户")) {
                    val json = """{"id":0,"user_name":"$userName","user_level":$userLevel,"login_pwd":"$loginPwd","pwd":"$newPwd"}"""
                    val r = NetSDKManager.updateUser(json)
                    if (r.isOk()) { refreshUsers(); "OK" } else tr("失败: %s", r.message)
                }
            }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                Text(tr("添加"), style = MaterialTheme.typography.labelSmall)
            }
            Button(onClick = {
                if (selectedId < 0) return@Button
                runCmd(tr("修改用户")) {
                    val json = """{"id":$selectedId,"user_name":"$userName","user_level":$userLevel,"login_pwd":"$loginPwd","pwd":"$newPwd"}"""
                    val r = NetSDKManager.updateUser(json)
                    if (r.isOk()) { refreshUsers(); "OK" } else tr("失败: %s", r.message)
                }
            }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                Text(tr("修改"), style = MaterialTheme.typography.labelSmall)
            }
            Button(onClick = {
                if (selectedId < 0) return@Button
                runCmd(tr("删除用户")) {
                    val r = NetSDKManager.deleteUser(selectedId)
                    if (r.isOk()) { setSelectedId(-1); refreshUsers(); "OK" } else tr("失败: %s", r.message)
                }
            }, modifier = Modifier.height(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                Text(tr("删除"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ==================== Tab 2: Display ====================

private fun parseOvBlock(j: JSONObject?): IntArray =
    intArrayOf(j?.optInt("enabled", 0) ?: 0, j?.optInt("align", 0) ?: 0, j?.optInt("mode", 0) ?: 0, j?.optInt("x", 0) ?: 0, j?.optInt("y", 0) ?: 0)

private fun ovBlockJson(b: IntArray): String =
    """{"enabled":${b[0]},"align":${b[1]},"mode":${b[2]},"x":${b[3]},"y":${b[4]}}"""

private fun customOverlayJson(b: IntArray, text: String): String =
    """{"enabled":${b[0]},"align":${b[1]},"mode":${b[2]},"x":${b[3]},"y":${b[4]},"text":"${text.replace("\"", "\\\"")}"}"""

private fun parseFollowBlock(j: JSONObject?): IntArray =
    intArrayOf(j?.optInt("enabled", 0) ?: 0, j?.optInt("align", 0) ?: 0, j?.optInt("follow", 0) ?: 0,
        j?.optInt("max_enabled", 0) ?: 0, j?.optInt("min_enabled", 0) ?: 0, j?.optInt("avg_enabled", 0) ?: 0,
        j?.optInt("emissivity_enabled", 0) ?: 0, j?.optInt("x", 0) ?: 0, j?.optInt("y", 0) ?: 0)

private fun followBlockJson(b: IntArray): String =
    """{"enabled":${b[0]},"align":${b[1]},"follow":${b[2]},"max_enabled":${b[3]},"min_enabled":${b[4]},"avg_enabled":${b[5]},"emissivity_enabled":${b[6]},"x":${b[7]},"y":${b[8]}}"""

@Composable
private fun OverlayBlockUI(
    b: IntArray, setB: (IntArray) -> Unit,
    resW: Int, resH: Int,
    alignOptions: List<Pair<Int, String>>, modeOptions: List<Pair<Int, String>>
) {
    fun upd(i: Int, v: Int) { val c = b.copyOf(); c[i] = v; setB(c) }
    OverlayControlRow(
        listOf(tr("启用") to (listOf(0 to tr("关"), 1 to tr("开")) to b[0]),
               tr("对齐") to (alignOptions to b[1]),
               tr("模式") to (modeOptions to b[2]))
    ) { idx, v -> upd(idx, v) }
    CompactSliderRow("X", b[3], 0, resW) { upd(3, it) }
    CompactSliderRow("Y", b[4], 0, resH) { upd(4, it) }
}

@Composable
private fun FollowBlockUI(
    b: IntArray, setB: (IntArray) -> Unit,
    resW: Int, resH: Int,
    alignOptions: List<Pair<Int, String>>
) {
    fun upd(i: Int, v: Int) { val c = b.copyOf(); c[i] = v; setB(c) }
    OverlayControlRow(
        listOf(tr("启用") to (listOf(0 to tr("关"), 1 to tr("开")) to b[0]),
               tr("对齐") to (alignOptions to b[1]),
               tr("跟随") to (listOf(0 to tr("关"), 1 to tr("开")) to b[2]))
    ) { idx, v -> upd(idx, v) }
    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        CheckboxRow(tr("最高"), b[3] == 1) { upd(3, if (it) 1 else 0) }
        CheckboxRow(tr("最低"), b[4] == 1) { upd(4, if (it) 1 else 0) }
        CheckboxRow(tr("平均"), b[5] == 1) { upd(5, if (it) 1 else 0) }
        CheckboxRow(tr("发射率"), b[6] == 1) { upd(6, if (it) 1 else 0) }
    }
    CompactSliderRow("X", b[7], 0, resW) { upd(7, it) }
    CompactSliderRow("Y", b[8], 0, resH) { upd(8, it) }
}

@Composable
private fun OverlayControlRow(
    items: List<Pair<String, Pair<List<Pair<Int, String>>, Int>>>,
    onSelect: (Int, Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { idx, (label, optVal) ->
            val (options, selected) = optVal
            var expanded by remember { mutableStateOf(false) }
            val selText = options.firstOrNull { it.first == selected }?.second ?: "$selected"
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().height(24.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                    Text(selText, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (v, l) ->
                        DropdownMenuItem(text = { Text(l, style = MaterialTheme.typography.bodySmall) },
                            onClick = { onSelect(idx, v); expanded = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun TabDisplay(
    channelId: Int, setChannelId: (Int) -> Unit, channelOptions: List<Pair<Int, String>>, setChannelOptions: (List<Pair<Int, String>>) -> Unit,
    channelName: String, setChannelName: (String) -> Unit, resW: Int, setResW: (Int) -> Unit, resH: Int, setResH: (Int) -> Unit,
    chNameEnabled: Int, setChNameEnabled: (Int) -> Unit, chNameAlign: Int, setChNameAlign: (Int) -> Unit, chNameMode: Int, setChNameMode: (Int) -> Unit, chNameX: Int, setChNameX: (Int) -> Unit, chNameY: Int, setChNameY: (Int) -> Unit,
    timeEnabled: Int, setTimeEnabled: (Int) -> Unit, timeAlign: Int, setTimeAlign: (Int) -> Unit, timeMode: Int, setTimeMode: (Int) -> Unit, timeX: Int, setTimeX: (Int) -> Unit, timeY: Int, setTimeY: (Int) -> Unit,
    tovUnit: IntArray, setTovUnit: (IntArray) -> Unit, tovPlt: IntArray, setTovPlt: (IntArray) -> Unit, tovThermalParams: IntArray, setTovThermalParams: (IntArray) -> Unit,
    tovAlarm: IntArray, setTovAlarm: (IntArray) -> Unit,
    tovGlobal: IntArray, setTovGlobal: (IntArray) -> Unit, tovMarker: IntArray, setTovMarker: (IntArray) -> Unit,
    ovCustom: IntArray, setOvCustom: (IntArray) -> Unit, ovCustomText: String, setOvCustomText: (String) -> Unit,
    contrast: String, setContrast: (String) -> Unit, brightness: String, setBrightness: (String) -> Unit, ddeEnable: Int, setDdeEnable: (Int) -> Unit, ddeValue: String, setDdeValue: (String) -> Unit,
    runCmd: (String, suspend () -> String) -> Unit
) {
    val alignOptions = listOf(0 to tr("左上"), 1 to tr("右上"), 2 to tr("左下"), 3 to tr("右下"))
    val modeOptions = listOf(0 to tr("常亮"), 1 to tr("闪烁"))

    SectionCard(tr("通道"), action = {
        IconButton(onClick = {
            runCmd(tr("刷新通道")) {
                val cr = NetSDKManager.getChannels()
                if (cr.isOk() && cr.data != null) {
                    val opts = (0 until cr.data!!.length()).map { i ->
                        val ch = cr.data!!.getJSONObject(i)
                        ch.optInt("id") to ch.optString("name", tr("通道%s", ch.optInt("id")))
                    }
                    if (opts.isNotEmpty()) setChannelOptions(opts)
                }
                val nr = NetSDKManager.getChannel(channelId)
                if (nr.isOk() && nr.data != null) setChannelName(nr.data!!.optString("name", ""))
                val rr = NetSDKManager.getThermalResolution()
                if (rr.isOk() && rr.data != null) { setResW(rr.data!!.optInt("width", 640)); setResH(rr.data!!.optInt("height", 480)) }
                "OK"
            }
        }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        CompactSettingRow(tr("通道"), channelOptions, channelId, setChannelId, showSetButton = false) {}
        CompactInputRow(tr("通道名称"), channelName, setChannelName)
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                runCmd(tr("设置通道")) {
                    val r = NetSDKManager.setChannel(channelId, """{"name":"$channelName"}""")
                    if (r.isOk()) "OK" else tr("失败: %s", r.message)
                }
            }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(tr("设置"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    SectionCard(tr("通道名叠加"), action = {
        IconButton(onClick = {
            runCmd(tr("刷新OSD")) {
                val r = NetSDKManager.getOverlay(channelId)
                if (r.isOk() && r.data != null) {
                    val cn = r.data!!.optJSONObject("channel_name_overlay")
                    if (cn != null) { setChNameEnabled(cn.optInt("enabled", 0)); setChNameAlign(cn.optInt("align", 0)); setChNameMode(cn.optInt("mode", 0)); setChNameX(cn.optInt("x", 0)); setChNameY(cn.optInt("y", 0)) }
                    val tm = r.data!!.optJSONObject("time_overlay")
                    if (tm != null) { setTimeEnabled(tm.optInt("enabled", 0)); setTimeAlign(tm.optInt("align", 0)); setTimeMode(tm.optInt("mode", 0)); setTimeX(tm.optInt("x", 0)); setTimeY(tm.optInt("y", 0)) }
                    val custom = r.data!!.optJSONObject("custom_overlay")
                    if (custom != null) {
                        setOvCustom(parseOvBlock(custom))
                        setOvCustomText(custom.optString("text", ""))
                    }
                    "OK"
                } else tr("失败: %s", r.message)
            }
        }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        OverlayControlRow(
            listOf(tr("启用") to (listOf(0 to tr("关"), 1 to tr("开")) to chNameEnabled),
                   tr("对齐") to (alignOptions to chNameAlign),
                   tr("模式") to (modeOptions to chNameMode))
        ) { idx, v -> when(idx) { 0 -> setChNameEnabled(v); 1 -> setChNameAlign(v); 2 -> setChNameMode(v) } }
        CompactSliderRow("X", chNameX, 0, resW) { setChNameX(it) }
        CompactSliderRow("Y", chNameY, 0, resH) { setChNameY(it) }
    }

    SectionCard(tr("时间叠加")) {
        OverlayControlRow(
            listOf(tr("启用") to (listOf(0 to tr("关"), 1 to tr("开")) to timeEnabled),
                   tr("对齐") to (alignOptions to timeAlign),
                   tr("模式") to (modeOptions to timeMode))
        ) { idx, v -> when(idx) { 0 -> setTimeEnabled(v); 1 -> setTimeAlign(v); 2 -> setTimeMode(v) } }
        CompactSliderRow("X", timeX, 0, resW) { setTimeX(it) }
        CompactSliderRow("Y", timeY, 0, resH) { setTimeY(it) }
    }

    SectionCard(tr("自定义叠加")) {
        OverlayBlockUI(ovCustom, setOvCustom, resW, resH, alignOptions, modeOptions)
        CompactInputRow(tr("文本"), ovCustomText, setOvCustomText)
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                runCmd(tr("设置OSD")) {
                    val json = """{"channel_name_overlay":{"enabled":$chNameEnabled,"align":$chNameAlign,"mode":$chNameMode,"x":$chNameX,"y":$chNameY},"time_overlay":{"enabled":$timeEnabled,"align":$timeAlign,"mode":$timeMode,"x":$timeX,"y":$timeY},"custom_overlay":${customOverlayJson(ovCustom, ovCustomText)}}"""
                    val r = NetSDKManager.setOverlay(channelId, json)
                    if (r.isOk()) "OK" else tr("失败: %s", r.message)
                }
            }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(tr("设置"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    // Thermal overlay cards
    val tovNames = listOf(tr("单位叠加") to "unit_overlay", tr("调色板叠加") to "plt_overlay", tr("测温参数叠加") to "thermal_params_overlay", tr("报警叠加") to "alarm_overlay")
    val tovStates = listOf(tovUnit to setTovUnit, tovPlt to setTovPlt, tovThermalParams to setTovThermalParams, tovAlarm to setTovAlarm)

    SectionCard(tr("热像OSD"), action = {
        IconButton(onClick = {
            runCmd(tr("刷新热像OSD")) {
                val r = NetSDKManager.getThermalOverlay()
                if (r.isOk() && r.data != null) {
                    val d = r.data!!
                    setTovUnit(parseOvBlock(d.optJSONObject("unit_overlay")))
                    setTovPlt(parseOvBlock(d.optJSONObject("plt_overlay")))
                    setTovThermalParams(parseOvBlock(d.optJSONObject("thermal_params_overlay")))
                    setTovAlarm(parseOvBlock(d.optJSONObject("alarm_overlay")))
                    setTovGlobal(parseFollowBlock(d.optJSONObject("global_overlay")))
                    setTovMarker(parseFollowBlock(d.optJSONObject("marker_overlay")))
                    "OK"
                } else tr("失败: %s", r.message)
            }
        }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        Text(tr("点击刷新获取所有热像叠加设置"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    tovNames.forEachIndexed { i, (label, _) ->
        val (state, setter) = tovStates[i]
        SectionCard(label) {
            OverlayBlockUI(state, setter, resW, resH, alignOptions, modeOptions)
        }
    }

    SectionCard(tr("全局温度叠加")) {
        FollowBlockUI(tovGlobal, setTovGlobal, resW, resH, alignOptions)
    }

    SectionCard(tr("标记温度叠加")) {
        FollowBlockUI(tovMarker, setTovMarker, resW, resH, alignOptions)
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                runCmd(tr("设置热像OSD")) {
                    val json = buildString {
                        append("{")
                        tovNames.forEachIndexed { i, (_, key) ->
                            if (i > 0) append(",")
                            append("\"$key\":${ovBlockJson(tovStates[i].first)}")
                        }
                        append(",\"global_overlay\":${followBlockJson(tovGlobal)}")
                        append(",\"marker_overlay\":${followBlockJson(tovMarker)}")
                        append("}")
                    }
                    val r = NetSDKManager.setThermalOverlay(json)
                    if (r.isOk()) "OK" else tr("失败: %s", r.message)
                }
            }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(tr("设置"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    SectionCard(tr("图像参数"), action = {
        IconButton(onClick = {
            runCmd(tr("刷新图像参数")) {
                val r = NetSDKManager.getImageParams(channelId)
                if (r.isOk() && r.data != null) {
                    val d = r.data!!
                    setContrast(d.optInt("contrast", 0).toString()); setBrightness(d.optInt("brightness", 0).toString())
                    setDdeEnable(d.optInt("dde_enable", 0)); setDdeValue(d.optInt("dde_value", 0).toString())
                    "OK"
                } else tr("失败: %s", r.message)
            }
        }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        CompactInputRow(tr("对比度"), contrast, setContrast)
        CompactInputRow(tr("亮度"), brightness, setBrightness)
        CompactSettingRow("DDE", listOf(0 to tr("关闭"), 1 to tr("开启")), ddeEnable, setDdeEnable, showSetButton = false) {}
        CompactInputRow(tr("DDE强度"), ddeValue, setDdeValue)
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                runCmd(tr("设置图像参数")) {
                    val json = """{"contrast":${contrast.toIntOrNull() ?: 0},"brightness":${brightness.toIntOrNull() ?: 0},"dde_enable":$ddeEnable,"dde_value":${ddeValue.toIntOrNull() ?: 0}}"""
                    val r = NetSDKManager.setImageParams(channelId, json)
                    if (r.isOk()) "OK" else tr("失败: %s", r.message)
                }
            }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(tr("设置"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ==================== Tab: 标记 (Markers) ====================

@Composable
private fun TabMarkers(
    tempUnit: Int, distUnit: Int,
    markerTemps: Map<String, Triple<Double, Double, Double>>,
    markerAlarmLevels: Map<String, Int>,
    runCmd: (String, suspend () -> String) -> Unit
) {
    val markerTypeNames = mapOf(
        MARKER_TYPE_SPOT to tr("点"),
        MARKER_TYPE_POLYGON to tr("多边形"),
        MARKER_TYPE_POLYLINE to tr("折线")
    )
    val alarmLevelNames = mapOf(1 to tr("提醒"), 2 to tr("预警"), 3 to tr("报警"))
    val alarmLevelColors = mapOf(
        0 to Color.Unspecified, 1 to Color(0xFF2196F3),
        2 to Color(0xFFFF9800), 3 to Color(0xFFF44336)
    )
    var markers by remember { mutableStateOf<List<org.json.JSONObject>>(emptyList()) }
    val tempLabel = tempUnitLabel(tempUnit)
    val distLabel = distUnitLabel(distUnit)
    val scrollState = rememberScrollState()

    SectionCard(tr("测温标记列表"), action = {
        IconButton(onClick = {
            runCmd(tr("获取标记")) {
                val r = NetSDKManager.getThermalMarkers()
                if (r.isOk() && r.data != null) {
                    val list = mutableListOf<org.json.JSONObject>()
                    for (i in 0 until r.data!!.length()) list.add(r.data!!.getJSONObject(i))
                    markers = list
                    tr("获取到 %s 个标记", list.size)
                } else { markers = emptyList(); tr("失败: %s", r.message) }
            }
        }, modifier = Modifier.size(22.dp)) {
            Icon(Icons.Default.Refresh, tr("刷新"), Modifier.size(14.dp))
        }
    }) {
        if (markers.isEmpty()) {
            Text(tr("点击刷新获取标记列表"), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                Column {
                    Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 4.dp, vertical = 1.dp)) {
                        Text("ID", Modifier.width(30.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(tr("名称"), Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(tr("类型"), Modifier.width(44.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(tr("最高温(%s)", tempLabel), Modifier.width(68.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(tr("最低温(%s)", tempLabel), Modifier.width(68.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(tr("平均温(%s)", tempLabel), Modifier.width(68.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(tr("报警状态"), Modifier.width(52.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(tr("发射率"), Modifier.width(48.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(tr("距离(%s)", distLabel), Modifier.width(52.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(tr("反射温(%s)", tempLabel), Modifier.width(56.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(tr("本地参数"), Modifier.width(52.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(tr("点数"), Modifier.width(30.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    markers.forEach { m ->
                        val name = m.optString("name", "")
                        val temps = markerTemps[name]
                        val hasAlarmData = markerAlarmLevels.containsKey(name)
                        val alarmLevel = markerAlarmLevels[name] ?: 0
                        Row(Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("${m.optInt("id")}", Modifier.width(30.dp), style = MaterialTheme.typography.bodySmall)
                            Text(name, Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(markerTypeNames[m.optInt("type")] ?: "${m.optInt("type")}",
                                Modifier.width(44.dp), style = MaterialTheme.typography.bodySmall)
                            Text(if (temps != null) formatTempValue(temps.first, tempUnit) else "--",
                                Modifier.width(68.dp), style = MaterialTheme.typography.bodySmall)
                            Text(if (temps != null) formatTempValue(temps.second, tempUnit) else "--",
                                Modifier.width(68.dp), style = MaterialTheme.typography.bodySmall)
                            Text(if (temps != null) formatTempValue(temps.third, tempUnit) else "--",
                                Modifier.width(68.dp), style = MaterialTheme.typography.bodySmall)
                            Text(if (!hasAlarmData) "--" else alarmLevelNames[alarmLevel] ?: "--",
                                Modifier.width(52.dp), style = MaterialTheme.typography.bodySmall,
                                color = if (hasAlarmData) alarmLevelColors[alarmLevel] ?: Color.Unspecified else Color.Unspecified,
                                fontWeight = if (alarmLevel > 0) FontWeight.Bold else FontWeight.Normal)
                            Text("%.2f".format(Locale.US, m.optDouble("emissivity", 0.0)),
                                Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
                            Text(formatDistValue(m.optDouble("distance", 0.0), distUnit),
                                Modifier.width(52.dp), style = MaterialTheme.typography.bodySmall)
                            Text(formatTempValue(m.optDouble("refl_temp", 0.0), tempUnit),
                                Modifier.width(56.dp), style = MaterialTheme.typography.bodySmall)
                            Text(if (m.optInt("local_params") != 0) tr("是") else tr("否"),
                                Modifier.width(52.dp), style = MaterialTheme.typography.bodySmall)
                            Text("${m.optInt("points_count")}", Modifier.width(30.dp), style = MaterialTheme.typography.bodySmall)
                        }
                        val pts = m.optJSONArray("points")
                        if (pts != null && pts.length() > 0) {
                            Text(
                                (0 until pts.length()).joinToString("  ") { i ->
                                    val p = pts.getJSONObject(i)
                                    "(${p.optInt("x")},${p.optInt("y")})"
                                },
                                Modifier.padding(start = 30.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Tab: 报警 (Alarms) ====================

@Composable
private fun TabAlarms(
    alarmListenerActive: Boolean, setAlarmListenerActive: (Boolean) -> Unit,
    alarmListenerHandle: Long, setAlarmListenerHandle: (Long) -> Unit,
    alarmEventText: String, setAlarmEventText: (String) -> Unit,
    triggerFtpEnabled: Int, setTriggerFtpEnabled: (Int) -> Unit,
    triggerWebhookEnabled: Int, setTriggerWebhookEnabled: (Int) -> Unit,
    tempUnit: Int,
    alarmStartupLogRemaining: AtomicInteger,
    alarmStartupLogTotal: Int,
    runCmd: (String, suspend () -> String) -> Unit
) {
    val alarmTypeOptions = listOf(
        1 to tr("最高温>"), 2 to tr("最高温<"), 3 to tr("最低温>"), 4 to tr("最低温<"),
        5 to tr("平均温>"), 6 to tr("平均温<")
    )
    var alarmRules by remember { mutableStateOf(listOf<EditableAlarmRule>()) }
    val tempLabel = tempUnitLabel(tempUnit)
    var prevTempUnit by remember { mutableIntStateOf(tempUnit) }

    LaunchedEffect(tempUnit) {
        if (alarmRules.isNotEmpty() && tempUnit != prevTempUnit) {
            alarmRules = alarmRules.map { rule ->
                rule.copy(
                    alarmTemp = convertTempDisplayValue(rule.alarmTemp, prevTempUnit, tempUnit),
                    warningTemp = convertTempDisplayValue(rule.warningTemp, prevTempUnit, tempUnit),
                    alertTemp = convertTempDisplayValue(rule.alertTemp, prevTempUnit, tempUnit),
                )
            }
        }
        prevTempUnit = tempUnit
    }

    SectionCard(tr("报警规则"), action = {
        Row {
            IconButton(onClick = {
                runCmd(tr("获取报警")) {
                    val r = NetSDKManager.getThermalAlarms()
                    if (!r.isOk() || r.data == null) {
                        alarmRules = emptyList()
                        return@runCmd tr("失败: %s", r.message)
                    }
                    val existing = (0 until r.data!!.length()).map { i ->
                        EditableAlarmRule.fromJson(r.data!!.getJSONObject(i), tempUnit)
                    }
                    val markerNames = mutableListOf<String>()
                    val markersR = NetSDKManager.getThermalMarkers()
                    if (markersR.isOk() && markersR.data != null) {
                        for (i in 0 until markersR.data!!.length()) {
                            val name = markersR.data!!.getJSONObject(i).optString("name", "")
                            if (name.isNotBlank()) markerNames.add(name)
                        }
                    }
                    val list = mergeAlarmRulesWithMarkers(existing, markerNames, tempUnit)
                    val filled = list.size - existing.size
                    alarmRules = list
                    if (filled > 0) {
                        tr("获取到 %s 条，按标记补全 %s 条，共 %s 条", existing.size, filled, list.size)
                    } else {
                        tr("获取到 %s 条报警规则", list.size)
                    }
                }
            }, modifier = Modifier.size(22.dp)) {
                Icon(Icons.Default.Refresh, tr("刷新"), Modifier.size(14.dp))
            }
        }
    }) {
        if (alarmRules.isEmpty()) {
            Text(tr("点击刷新获取报警规则"), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            alarmRules.forEachIndexed { index, rule ->
                if (index > 0) HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Text(tr("规则 %s: %s", index + 1, rule.markerName), style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                CompactInputRow(tr("标记名"), rule.markerName) { v ->
                    alarmRules = alarmRules.toMutableList().also { it[index] = it[index].copy(markerName = v) }
                }
                CompactSettingRow(tr("启用"), listOf(0 to tr("否"), 1 to tr("是")), rule.enabled, { v ->
                    alarmRules = alarmRules.toMutableList().also { it[index] = it[index].copy(enabled = v) }
                }, showSetButton = false) {}
                CompactSettingRow(tr("报警类型"), alarmTypeOptions, rule.alarmType, { v ->
                    alarmRules = alarmRules.toMutableList().also { it[index] = it[index].copy(alarmType = v) }
                }, showSetButton = false) {}
                CompactInputRow(tr("报警温度(%s)", tempLabel), rule.alarmTemp) { v ->
                    alarmRules = alarmRules.toMutableList().also { it[index] = it[index].copy(alarmTemp = v) }
                }
                CompactInputRow(tr("报警延迟(s)"), rule.alarmDelay) { v ->
                    alarmRules = alarmRules.toMutableList().also { it[index] = it[index].copy(alarmDelay = v) }
                }
                CompactInputRow(tr("预警温度(%s)", tempLabel), rule.warningTemp) { v ->
                    alarmRules = alarmRules.toMutableList().also { it[index] = it[index].copy(warningTemp = v) }
                }
                CompactInputRow(tr("预警延迟(s)"), rule.warningDelay) { v ->
                    alarmRules = alarmRules.toMutableList().also { it[index] = it[index].copy(warningDelay = v) }
                }
                CompactInputRow(tr("提醒温度(%s)", tempLabel), rule.alertTemp) { v ->
                    alarmRules = alarmRules.toMutableList().also { it[index] = it[index].copy(alertTemp = v) }
                }
                CompactInputRow(tr("提醒延迟(s)"), rule.alertDelay) { v ->
                    alarmRules = alarmRules.toMutableList().also { it[index] = it[index].copy(alertDelay = v) }
                }
            }
            Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = {
                    runCmd(tr("保存报警规则")) {
                        val arr = JSONArray()
                        alarmRules.forEach { arr.put(it.toJson(tempUnit)) }
                        val r = NetSDKManager.setThermalAlarms(arr.toString())
                        if (r.isOk()) tr("已保存 %s 条", alarmRules.size) else tr("失败: %s", r.message)
                    }
                }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text(tr("保存"), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    SectionCard(tr("报警联动"), action = {
        IconButton(onClick = {
            runCmd(tr("刷新报警联动")) {
                val r = NetSDKManager.getAlarmTrigger(1)
                if (r.isOk() && r.data != null) {
                    val d = r.data!!
                    setTriggerFtpEnabled(d.optInt("trigger_ftp_enabled", 0))
                    setTriggerWebhookEnabled(d.optInt("trigger_webhook_enabled", 0))
                    tr("原始数据: %s", d)
                } else tr("失败: %s", r.message)
            }
        }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = tr("刷新"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }) {
        CompactSettingRow(tr("FTP上传"), listOf(0 to tr("关闭"), 1 to tr("开启")), triggerFtpEnabled, setTriggerFtpEnabled, showSetButton = false) {}
        CompactSettingRow("Webhook", listOf(0 to tr("关闭"), 1 to tr("开启")), triggerWebhookEnabled, setTriggerWebhookEnabled, showSetButton = false) {}
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                runCmd(tr("设置报警联动")) {
                    val json = """{"trigger_ftp_enabled":$triggerFtpEnabled,"trigger_webhook_enabled":$triggerWebhookEnabled}"""
                    val r = NetSDKManager.setAlarmTrigger(1, json)
                    if (r.isOk()) "OK" else tr("失败: %s", r.message)
                }
            }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(tr("设置"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    SectionCard(tr("报警监听")) {
        Text(
            if (alarmEventText.isEmpty()) tr("尚未收到报警回调") else tr("最近回调: %s", alarmEventText),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(Modifier.fillMaxWidth().height(30.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    runCmd(tr("开始报警监听")) {
                        val r = NetSDKManager.startAlarmListener()
                        if (r.isOk() && r.data != null && r.data!! >= 0) {
                            setAlarmListenerHandle(r.data!!)
                            setAlarmListenerActive(true)
                            alarmStartupLogRemaining.set(alarmStartupLogTotal)
                            tr("handle=%s, 将输出前%s条报警回调到日志", r.data, alarmStartupLogTotal)
                        } else tr("失败: %s", r.message)
                    }
                },
                enabled = !alarmListenerActive,
                modifier = Modifier.weight(1f).height(26.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) { Text(tr("开始监听"), style = MaterialTheme.typography.labelSmall) }
            Button(
                onClick = {
                    runCmd(tr("停止报警监听")) {
                        if (alarmListenerHandle <= 0) return@runCmd tr("未启动")
                        val r = NetSDKManager.stopAlarmListener(alarmListenerHandle)
                        if (r.isOk()) {
                            setAlarmListenerActive(false)
                            setAlarmListenerHandle(0L)
                            setAlarmEventText("")
                            alarmStartupLogRemaining.set(0)
                            "OK"
                        } else tr("失败: %s", r.message)
                    }
                },
                enabled = alarmListenerActive,
                modifier = Modifier.weight(1f).height(26.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) { Text(tr("停止监听"), style = MaterialTheme.typography.labelSmall) }
        }
    }
}

// ==================== Palette Strip ====================

@Composable
private fun PaletteStrip(colors: IntArray, modifier: Modifier = Modifier) {
    val bmp = remember(colors) {
        val h = colors.size.coerceAtLeast(1)
        val pixels = IntArray(h) { i ->
            val c = colors[h - 1 - i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        Bitmap.createBitmap(pixels, 1, h, Bitmap.Config.ARGB_8888)
    }
    Image(
        bitmap = bmp.asImageBitmap(),
        contentDescription = tr("调色板"),
        modifier = modifier,
        contentScale = ContentScale.FillBounds
    )
}

// ==================== Video Toolbar Dropdown ====================

@Composable
private fun ToolbarDropdown(
    label: String,
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    options: List<Pair<Int, String>>,
    onSelect: (Int, String) -> Unit
) {
    Box {
        Surface(
            onClick = { onToggle(true) },
            color = Color.Transparent,
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onToggle(false) }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, style = MaterialTheme.typography.bodySmall) },
                    onClick = { onToggle(false); onSelect(value, text) }
                )
            }
        }
    }
}

// ==================== Compact Setting Row ====================

@Composable
private fun <T> CompactSettingRow(
    title: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelected: (T) -> Unit,
    showSetButton: Boolean = true,
    onSet: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = options.firstOrNull { it.first == selectedValue }?.second ?: selectedValue.toString()

    Row(
        Modifier.fillMaxWidth().height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.weight(if (showSetButton) 1.2f else 1.7f)) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().height(26.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(selectedText, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, label) ->
                    DropdownMenuItem(text = { Text(label, style = MaterialTheme.typography.bodySmall) },
                        onClick = { onSelected(value); expanded = false })
                }
            }
        }
        if (showSetButton) {
            Button(onClick = onSet, modifier = Modifier.weight(0.5f).height(26.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                Text(tr("设置"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ==================== Compact Slider Row ====================

@Composable
private fun CompactSliderRow(label: String, value: Int, min: Int, max: Int, onValueChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(24.dp),
            color = MaterialTheme.colorScheme.onSurface)
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat().coerceAtLeast(1f),
            modifier = Modifier.weight(1f).height(14.dp)
        )
        Text("$value", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ==================== Checkbox Row ====================

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(1.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

// ==================== Compact Input Row ====================

@Composable
private fun CompactInputRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1.2f)
                .height(24.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp),
            textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() }
            }
        )
    }
}

// ==================== Shared Components ====================

@Composable
private fun SectionCard(title: String, action: @Composable (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                if (action != null) action()
            }
            content()
        }
    }
}

@Composable
private fun <T> DeviceSettingRow(
    title: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelected: (T) -> Unit,
    onGet: () -> Unit,
    onSet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = options.firstOrNull { it.first == selectedValue }?.second ?: selectedValue.toString()

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onGet, modifier = Modifier.weight(0.7f), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
                Text(tr("获取"), style = MaterialTheme.typography.labelSmall)
            }
            Box(Modifier.weight(1.5f)) {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
                    Text(selectedText, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (value, label) ->
                        DropdownMenuItem(text = { Text(label, style = MaterialTheme.typography.bodySmall) }, onClick = { onSelected(value); expanded = false })
                    }
                }
            }
            Button(onClick = onSet, modifier = Modifier.weight(0.7f), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
                Text(tr("设置"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun hitTestMarker(
    mx: Int, my: Int,
    markers: List<DrawMarker>,
    scaleX: Float, scaleY: Float
): Pair<DrawMarker, Int>? {
    val hitRadius = (8 * scaleX).toInt().coerceAtLeast(6)
    for (i in markers.indices.reversed()) {
        val m = markers[i]
        when (m.type) {
            MARKER_TYPE_SPOT -> {
                if (m.points.isNotEmpty()) {
                    val dx = mx - m.points[0].first; val dy = my - m.points[0].second
                    if (Math.abs(dx) <= hitRadius && Math.abs(dy) <= hitRadius)
                        return Pair(m, -2)
                }
            }
            MARKER_TYPE_POLYLINE -> {
                if (m.points.size >= 2) {
                    val (x0, y0) = m.points[0]; val (x1, y1) = m.points[1]
                    if (Math.abs(mx - x0) <= hitRadius && Math.abs(my - y0) <= hitRadius)
                        return Pair(m, 0)
                    if (Math.abs(mx - x1) <= hitRadius && Math.abs(my - y1) <= hitRadius)
                        return Pair(m, 1)
                    val dist = distanceToSegment(mx.toFloat(), my.toFloat(), x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat())
                    if (dist < hitRadius) return Pair(m, -2)
                }
            }
            MARKER_TYPE_POLYGON -> {
                if (m.points.size >= MARKER_POLYGON_MIN_POINTS) {
                    for (vi in m.points.indices) {
                        val (vx, vy) = m.points[vi]
                        if (Math.abs(mx - vx) <= hitRadius / 2 && Math.abs(my - vy) <= hitRadius / 2)
                            return Pair(m, vi)
                    }
                    if (pointInPolygon(mx, my, m.points)) return Pair(m, -2)
                }
            }
        }
    }
    return null
}

private fun distanceToSegment(px: Float, py: Float, x0: Float, y0: Float, x1: Float, y1: Float): Float {
    val dx = x1 - x0; val dy = y1 - y0
    val len2 = dx * dx + dy * dy
    if (len2 == 0f) return Math.hypot((px - x0).toDouble(), (py - y0).toDouble()).toFloat()
    var t = ((px - x0) * dx + (py - y0) * dy) / len2
    t = t.coerceIn(0f, 1f)
    return Math.hypot((px - (x0 + t * dx)).toDouble(), (py - (y0 + t * dy)).toDouble()).toFloat()
}

private fun formatTempRangeText(item: JSONObject, srcUnit: Int, dstUnit: Int, spaced: Boolean): String {
    val rng = item.optJSONObject("range")
    val minV = rng?.optDouble("min", 0.0) ?: item.optDouble("min", 0.0)
    val maxV = rng?.optDouble("max", 0.0) ?: item.optDouble("max", 0.0)
    val minK = displayToKelvin(minV, srcUnit)
    val maxK = displayToKelvin(maxV, srcUnit)
    val minD = kelvinToDisplay(minK, dstUnit).roundToInt()
    val maxD = kelvinToDisplay(maxK, dstUnit).roundToInt()
    val unit = tempUnitLabel(dstUnit)
    return if (spaced) "$minD ~ $maxD$unit" else "$minD~$maxD$unit"
}

private fun buildRangeDisplay(items: List<JSONObject>, srcUnit: Int, dstUnit: Int): Pair<String, List<Pair<Int, String>>> {
    val options = items.map { item ->
        item.optInt("id") to tr("量程%s: %s", item.optInt("id"), formatTempRangeText(item, srcUnit, dstUnit, spaced = false))
    }
    val text = items.joinToString("\n") { item ->
        tr("量程%s: %s", item.optInt("id"), formatTempRangeText(item, srcUnit, dstUnit, spaced = true))
    }
    return text to options
}

private fun pointInPolygon(x: Int, y: Int, poly: List<Pair<Int, Int>>): Boolean {
    var inside = false
    for (i in poly.indices) {
        val j = (i + 1) % poly.size
        val (xi, yi) = poly[i]; val (xj, yj) = poly[j]
        val intersect = ((yi > y) != (yj > y)) &&
            (x < (xj - xi) * (y - yi) / (yj - yi).toDouble() + xi)
        if (intersect) inside = !inside
    }
    return inside
}
