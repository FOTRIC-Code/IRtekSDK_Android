package com.fotric.cardinal.uvc

import android.graphics.Bitmap
import android.hardware.usb.UsbDeviceConnection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fotric.holley.uvc.AcousticDbScale
import com.fotric.holley.uvc.UVCDeviceInfo

/**
 * UVC Demo 的界面状态持有类。
 * 原本全部堆在 UVCContent 里的几十个 remember 状态集中到这里，
 * 便于将各功能面板拆分为独立组件并按需读写状态。
 */
internal class UvcUiState {
    // 设备与连接
    var deviceCount by mutableStateOf(0)
    var currentDevice by mutableStateOf<UVCDeviceInfo?>(null)
    var deviceInfo by mutableStateOf<String?>(null)
    var usbConnection by mutableStateOf<UsbDeviceConnection?>(null)

    // 预览
    var previewBitmap by mutableStateOf<Bitmap?>(null)
    var centerMarkerTemp by mutableStateOf<Float?>(null)
    var thermalFrameText by mutableStateOf("")
    var isStreaming by mutableStateOf(false)
    var lastFrameTimeMs by mutableStateOf(0L)
    var streamError by mutableStateOf<String?>(null)

    // 结果日志与一键测试
    var apiResultText by mutableStateOf("")
    var isBatchTesting by mutableStateOf(false)

    // 版本
    var uvcSdkVersion by mutableStateOf<String?>(null)
    var fileSdkVersion by mutableStateOf<String?>(null)

    // 语言与导航
    var language by mutableStateOf(AppLanguage.English)
    var selectedMainTab by mutableIntStateOf(0)
    var selectedDeviceCommandTab by mutableIntStateOf(0)

    // 热像指令
    var selectedShutterMode by mutableStateOf<Byte>(0)
    var selectedShutterTime by mutableStateOf<Short>(5)
    var selectedRangeIndex by mutableStateOf<Short>(0)
    var rangeItems by mutableStateOf<List<Triple<Short, Int, Int>>?>(null)
    var selectedThermalParamIndex by mutableIntStateOf(0)
    var selectedImageParamIndex by mutableIntStateOf(0)
    var selectedFlipMode by mutableStateOf<Short>(0)
    var selectedSrMode by mutableStateOf<Short>(0)

    // 声像
    var acousticMode by mutableStateOf(ACOUSTIC_MODE_MONO)
    var acousticFixedSpan by mutableStateOf(false)
    var acousticSpanText by mutableStateOf("0.1")
    var acousticSensitivityText by mutableStateOf("3.2")
    var acousticSourceSizeText by mutableStateOf("3")
    var acousticLimitText by mutableStateOf("-100")
    var acousticMaxText by mutableStateOf("1000")
    var acousticMinText by mutableStateOf("-100")
    var acousticFocusEnabled by mutableStateOf(false)
    var acousticFreqMinText by mutableStateOf("5")
    var acousticFreqMaxText by mutableStateOf("100")

    // 成像（温宽/颜色分布/报警）。界面保存摄氏度，读写 SDK 时换算成 AD。
    @Volatile
    var thermalLut: ThermalLut? = null
    var gainMode by mutableStateOf(GAIN_AUTO)
    var gainMinText by mutableStateOf("20")
    var gainMaxText by mutableStateOf("40")
    var gainFixedSpan by mutableStateOf(false)
    var gainSpanText by mutableStateOf("2")
    var colorDist by mutableStateOf(COLOR_AD_LINEAR)
    var alarmType by mutableStateOf(ALARM_NORMAL)
    var alarmCustom by mutableStateOf(false)
    var alarmColorText by mutableStateOf("7FFF0000")
    var alarmMinText by mutableStateOf("20")
    var alarmMaxText by mutableStateOf("40")

    // 调色板
    var paletteOptions by mutableStateOf(fallbackPalettes(false).map { it.first to "${it.second}(${it.first})" })
    var selectedPaletteRefNo by mutableStateOf(1.toByte())
    var paletteInverted by mutableStateOf(false)
    var paletteAlpha by mutableIntStateOf(60)

    val isAcoustic: Boolean
        get() = currentDevice?.isAcousticDevice() == true

    fun appendApiResult(message: String) {
        apiResultText = (if (apiResultText.isEmpty()) "" else apiResultText + "\n") + message
    }

    fun clearPreview() {
        previewBitmap = null
        centerMarkerTemp = null
        thermalFrameText = ""
    }

    fun applyAcousticScale(scale: AcousticDbScale) {
        val mode = normalizedAcousticMode(scale.mode)
        acousticMode = mode
        acousticFixedSpan = mode == ACOUSTIC_MODE_HOLO && scale.fixedSpan.toInt() != 0
        acousticSpanText = formatAcousticFloat(scale.span)
        acousticSensitivityText = formatAcousticFloat(scale.sensitivity)
        acousticSourceSizeText = scale.sourceSize.toString()
        acousticLimitText = formatAcousticFloat(scale.limit)
        acousticMaxText = formatAcousticFloat(scale.max)
        acousticMinText = formatAcousticFloat(scale.min)
        acousticFocusEnabled = scale.focusEnabled.toInt() != 0
    }
}
