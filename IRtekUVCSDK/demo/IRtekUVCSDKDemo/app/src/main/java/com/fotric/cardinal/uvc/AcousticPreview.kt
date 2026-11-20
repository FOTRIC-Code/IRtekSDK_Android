package com.fotric.cardinal.uvc

import com.fotric.holley.uvc.AcousticSummary
import com.fotric.holley.uvc.UVCDeviceInfo
import com.fotric.holley.uvc.UVCManager

internal data class DevicePalette(
    val refNo: Byte,
    val inverted: Byte,
    val alpha: Int
)

internal object AcousticPreview {
    fun readSensorInfo(device: UVCDeviceInfo): String? {
        val result = UVCManager.getAcousticSensorInfo(device)
        val info = result.getOrNull() ?: return "code=${result.code}"
        val range = info.freqRange
        val detect = "${formatAcousticFloat(range.min)}-${formatAcousticFloat(range.max)} Hz"
        return "Sensor serial: ${info.serialNo.trim()}\n" +
            "Sensor firmware: ${info.firmwareVersion.trim()}\n" +
            "Sample rate: ${info.sampleRate} Hz\n" +
            "Microphones: ${info.micCount}\n" +
            "Detect range: $detect"
    }

    fun readFreqRange(device: UVCDeviceInfo): Pair<Int, AcousticFreqRange?> {
        val result = UVCManager.getAcousticFreqRange(device)
        val range = result.getOrNull() ?: return result.code to null
        return 0 to AcousticFreqRange(range.min, range.max)
    }

    fun writeFreqRange(device: UVCDeviceInfo, minHz: Float, maxHz: Float): Int {
        return UVCManager.setAcousticFreqRange(
            device,
            com.fotric.holley.uvc.AcousticFreqRange(minHz, maxHz)
        ).code
    }

    fun readResolution(device: UVCDeviceInfo): Pair<Int, Int>? {
        val size = UVCManager.getAcousticResolution(device).getOrNull() ?: return null
        return (size.width.toInt() and 0xFFFF) to (size.height.toInt() and 0xFFFF)
    }

    fun readPresetPalettes(device: UVCDeviceInfo, acoustic: Boolean): List<Pair<Byte, String>>? {
        val result = if (acoustic) {
            UVCManager.getAcousticPresetPalettes(device)
        } else {
            UVCManager.getThermalPresetPalettes(device)
        }
        val palettes = result.getOrNull() ?: return null
        if (palettes.isEmpty()) return null
        return palettes.map { palette ->
            val name = palette.name.trim().ifEmpty { "Palette ${palette.refNo}" }
            palette.refNo to name
        }
    }

    fun readPalette(device: UVCDeviceInfo, acoustic: Boolean): DevicePalette? {
        val result = if (acoustic) {
            UVCManager.getAcousticPalette(device)
        } else {
            UVCManager.getThermalPalette(device)
        }
        val palette = result.getOrNull() ?: return null
        return DevicePalette(palette.refNo, palette.inverted, palette.alpha.toInt() and 0xFF)
    }

    fun applyPalette(
        device: UVCDeviceInfo,
        acoustic: Boolean,
        refNo: Byte,
        inverted: Boolean,
        alpha: Int
    ): Int {
        val current = if (acoustic) {
            UVCManager.getAcousticPalette(device)
        } else {
            UVCManager.getThermalPalette(device)
        }
        val palette = current.getOrNull() ?: return current.code
        palette.refNo = refNo
        palette.inverted = if (inverted) 1 else 0
        if (acoustic) palette.alpha = alpha.coerceIn(0, 100).toByte()
        val written = if (acoustic) {
            UVCManager.setAcousticPalette(device, palette)
        } else {
            UVCManager.setThermalPalette(device, palette)
        }
        return written.code
    }

    fun readColorDist(device: UVCDeviceInfo): Byte? {
        return UVCManager.getThermalColorDist(device).getOrNull()
    }

    fun writeColorDist(device: UVCDeviceInfo, type: Byte): Int {
        return UVCManager.setThermalColorDist(device, type).code
    }
}

internal fun describeAcousticSummary(summary: AcousticSummary?): String {
    if (summary == null) return ""
    val max = summary.max
    val min = summary.min
    val count = summary.extremePointsCount.coerceAtLeast(0)
    val points = summary.extremePoints
        ?.take(count)
        ?.joinToString { point ->
            "(${point.x},${point.y}) ${formatAcousticFloat(point.value)} dB"
        }
        .orEmpty()
    return "max=(${max.x},${max.y}) ${formatAcousticFloat(max.value)} dB, " +
        "min=(${min.x},${min.y}) ${formatAcousticFloat(min.value)} dB, " +
        "sources=$count${if (points.isEmpty()) "" else " $points"}"
}

internal fun fallbackPalettes(acoustic: Boolean): List<Pair<Byte, String>> {
    val names = if (acoustic) {
        listOf("Red-Blue", "Iron", "Grey")
    } else {
        listOf(
            "Iron", "Grey", "Iron10", "Rainbow", "Grey10", "GreyRed",
            "MidGrey", "Yellow", "Rain", "Rain10", "Blue", "GlowBow",
            "Medical", "MidGreen", "Prism", "Medical10"
        )
    }
    return names.mapIndexed { index, name -> (index + 1).toByte() to name }
}
