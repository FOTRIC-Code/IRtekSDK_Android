package com.fotric.cardinal.uvc

import com.fotric.holley.uvc.LutTable
import com.fotric.holley.uvc.UVCDeviceInfo
import com.fotric.holley.uvc.UVCFrameRawData
import com.fotric.holley.uvc.UVCManager
import kotlin.math.abs

/**
 * AD 与温度的分段线性换算。
 * LUT 温度大于 200 时按开尔文处理，界面使用摄氏度。
 */
internal class ThermalLut private constructor(
    private val ad: FloatArray,
    private val temperature: FloatArray
) {
    private val kelvinScale = temperature.first() > 200f || temperature.last() > 200f

    fun adToCelsius(value: Float): Float {
        val sample = interpolate(value, ad, temperature)
        return if (kelvinScale) sample - 273.15f else sample
    }

    fun celsiusToAd(celsius: Float): Float {
        val sample = if (kelvinScale) celsius + 273.15f else celsius
        return interpolate(sample, temperature, ad).coerceIn(0f, AD_MAX)
    }

    fun spanCelsius(minAd: Float, spanAd: Float): Float {
        val min = adToCelsius(minAd)
        return adToCelsius(minAd + spanAd) - min
    }

    fun spanAd(minCelsius: Float, spanCelsius: Float): Float {
        val minAd = celsiusToAd(minCelsius)
        return abs(celsiusToAd(minCelsius + spanCelsius) - minAd).coerceIn(1f, AD_MAX)
    }

    companion object {
        private const val AD_MAX = 65535f

        fun fromDevice(table: LutTable?): ThermalLut? {
            if (table == null) return null
            val ad = table.ad ?: return null
            val temperature = table.t ?: return null
            val count = minOf(
                table.adLength.toInt() and 0xFFFF,
                table.tLength.toInt() and 0xFFFF,
                ad.size,
                temperature.size
            )
            if (count < 2) return null
            return build(
                FloatArray(count) { index -> (ad[index].toInt() and 0xFFFF).toFloat() },
                FloatArray(count) { index -> temperature[index].toFloat() }
            )
        }

        fun fromFrame(frame: UVCFrameRawData?): ThermalLut? {
            if (frame == null) return null
            val ad = frame.adValues ?: return null
            val temperature = frame.targetTemps ?: return null
            val count = minOf(frame.lutLength, ad.size, temperature.size)
            if (count < 2) return null
            return build(
                FloatArray(count) { index -> (ad[index].toInt() and 0xFFFF).toFloat() },
                FloatArray(count) { index -> temperature[index] }
            )
        }

        private fun build(ad: FloatArray, temperature: FloatArray): ThermalLut? {
            val order = ad.indices.sortedBy { ad[it] }
            val keptAd = ArrayList<Float>(order.size)
            val keptTemperature = ArrayList<Float>(order.size)
            for (index in order) {
                if (keptAd.isNotEmpty() && ad[index] <= keptAd.last()) continue
                if (keptTemperature.isNotEmpty() && temperature[index] <= keptTemperature.last()) continue
                keptAd.add(ad[index])
                keptTemperature.add(temperature[index])
            }
            if (keptAd.size < 2) return null
            return ThermalLut(keptAd.toFloatArray(), keptTemperature.toFloatArray())
        }
    }
}

internal fun loadThermalLut(device: UVCDeviceInfo, cached: ThermalLut?): ThermalLut? {
    return ThermalLut.fromDevice(UVCManager.getThermalLut(device).getOrNull()) ?: cached
}

internal fun missingThermalLut(language: AppLanguage): String {
    return if (language == AppLanguage.Chinese) {
        "没有可用的测温 LUT，无法在温度和 AD 之间换算"
    } else {
        "No thermal LUT is available, so temperature cannot be converted to AD"
    }
}

private fun interpolate(x: Float, xs: FloatArray, ys: FloatArray): Float {
    if (x <= xs.first()) return project(x, xs[0], xs[1], ys[0], ys[1])
    val last = xs.lastIndex
    if (x >= xs[last]) return project(x, xs[last - 1], xs[last], ys[last - 1], ys[last])
    var low = 0
    var high = last
    while (high - low > 1) {
        val mid = (low + high) / 2
        if (xs[mid] <= x) low = mid else high = mid
    }
    return project(x, xs[low], xs[high], ys[low], ys[high])
}

private fun project(x: Float, x0: Float, x1: Float, y0: Float, y1: Float): Float {
    val span = x1 - x0
    if (span == 0f) return y0
    return y0 + (y1 - y0) * ((x - x0) / span)
}
