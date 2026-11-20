package com.fotric.cardinal.uvc

import com.fotric.holley.uvc.UVCDeviceInfo
import com.fotric.holley.uvc.UVCManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 一键测试：逐项调用 SDK 接口，每项结果通过 [onCaseResult] 回报，
 * 返回 (通过数, 总项数)。从界面代码中抽离，便于维护与复用。
 */
internal suspend fun runBatchApiTest(
    dev: UVCDeviceInfo,
    strings: AppStrings,
    frameProcessor: IRtekFileFrameProcessor,
    onCaseResult: (name: String, passed: Boolean, detail: String) -> Unit
): Pair<Int, Int> {
    var passCount = 0
    var totalCount = 0

    suspend fun runCase(
        name: String,
        block: suspend () -> Pair<Boolean, String>
    ) {
        totalCount += 1
        val (ok, detail) = try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: Exception) {
            false to "${strings.exceptionPrefix}: ${e.message}"
        }
        if (ok) passCount += 1
        onCaseResult(name, ok, detail)
    }

    runCase("DeviceInfo") {
        if (dev.isAcousticDevice()) {
            val info = AcousticPreview.readSensorInfo(dev)
            val ok = info != null && !info.startsWith("code=")
            ok to (info ?: "uvc_get_acoustic_sensor_info failed")
        } else {
            val r = UVCManager.getDeviceInfo(dev)
            val info = r.getOrNull()
            (r.isSuccess && info != null) to
                    "model=${info?.model?.trim()}, serial=${info?.serialNo?.trim()}"
        }
    }
    if (!dev.isAcousticDevice()) {
        runCase("LensInfo") {
            val r = UVCManager.getLensInfo(dev)
            val info = r.getOrNull()
            (r.isSuccess && info != null) to
                    "name=${info?.name?.trim()}, rangesCount=${info?.rangesCount}"
        }
        runCase("GetThermalResolution") {
            val r = UVCManager.getThermalResolution(dev)
            val d = r.getOrNull()
            (r.isSuccess && d != null) to "res=${d?.width}x${d?.height}"
        }
        runCase("ThermalCalibrate") {
            val r = UVCManager.thermalCalibrate(dev)
            r.isSuccess to "code=${r.code}, msg=${r.message}"
        }
        runCase("GetThermalShutterMode") {
            val r = UVCManager.getThermalShutterMode(dev)
            val d = r.getOrNull()
            (r.isSuccess && d != null) to "mode=${d?.mode}"
        }
        runCase("SetThermalShutterMode") {
            val getR = UVCManager.getThermalShutterMode(dev)
            val mode = getR.getOrNull()
            if (!getR.isSuccess || mode == null) {
                false to "${strings.skipped}: ${strings.getAction("ShutterMode")} ${strings.failedPrefix} code=${getR.code}, msg=${getR.message}"
            } else {
                val setR = UVCManager.setThermalShutterMode(dev, mode)
                setR.isSuccess to "code=${setR.code}, msg=${setR.message}, mode=${mode.mode}"
            }
        }
        runCase("GetThermalShutterTime") {
            val r = UVCManager.getThermalShutterTime(dev)
            val d = r.getOrNull()
            (r.isSuccess && d != null) to "minutes=${d}"
        }
        runCase("SetThermalShutterTime") {
            val getR = UVCManager.getThermalShutterTime(dev)
            val minutes = getR.getOrNull()
            if (!getR.isSuccess || minutes == null) {
                false to "${strings.skipped}: ${strings.getAction("ShutterTime")} ${strings.failedPrefix} code=${getR.code}, msg=${getR.message}"
            } else {
                val setR = UVCManager.setThermalShutterTime(dev, minutes)
                setR.isSuccess to "code=${setR.code}, msg=${setR.message}, minutes=$minutes"
            }
        }
        runCase("GetThermalLut") {
            val r = UVCManager.getThermalLut(dev)
            val d = r.getOrNull()
            (r.isSuccess && d != null) to "rangeId=${d?.rangeId}, adLength=${d?.adLength}"
        }
        runCase("GetThermalRangeIndex") {
            val r = UVCManager.getThermalRangeIndex(dev)
            val d = r.getOrNull()
            (r.isSuccess && d != null) to "rangeIndex=${d?.rangeIndex}"
        }
        runCase("SetThermalRangeIndex") {
            val getR = UVCManager.getThermalRangeIndex(dev)
            val index = getR.getOrNull()
            if (!getR.isSuccess || index == null) {
                false to "${strings.skipped}: ${strings.getAction("RangeIndex")} ${strings.failedPrefix} code=${getR.code}, msg=${getR.message}"
            } else {
                val setR = UVCManager.setThermalRangeIndex(dev, index)
                setR.isSuccess to "code=${setR.code}, msg=${setR.message}, rangeIndex=${index.rangeIndex}"
            }
        }
        runCase("GetThermalParams") {
            val r = UVCManager.getThermalParams(dev)
            val d = r.getOrNull()
            (r.isSuccess && d != null) to
                    "emissivity=${d?.emissivity}, distance=${d?.distance}"
        }
        runCase("SetThermalParams") {
            val getR = UVCManager.getThermalParams(dev)
            val p = getR.getOrNull()
            if (!getR.isSuccess || p == null) {
                false to "${strings.skipped}: ${strings.getAction("ThermalParams")} ${strings.failedPrefix} code=${getR.code}, msg=${getR.message}"
            } else {
                val setR = UVCManager.setThermalParams(dev, p)
                val readBack = UVCManager.getThermalParams(dev).getOrNull() ?: p
                val syncR = frameProcessor.applyThermalParams(readBack)
                setR.isSuccess to "code=${setR.code}, msg=${setR.message}, FileSDK=${syncR.message}"
            }
        }
        runCase("GetImageParams") {
            val r = UVCManager.getImageParams(dev)
            val d = r.getOrNull()
            (r.isSuccess && d != null) to
                    "contrast=${d?.contrast}, brightness=${d?.brightness}"
        }
        runCase("SetImageParams") {
            val getR = UVCManager.getImageParams(dev)
            val p = getR.getOrNull()
            if (!getR.isSuccess || p == null) {
                false to "${strings.skipped}: ${strings.getAction("ImageParams")} ${strings.failedPrefix} code=${getR.code}, msg=${getR.message}"
            } else {
                val setR = UVCManager.setImageParams(dev, p)
                val readBack = UVCManager.getImageParams(dev).getOrNull() ?: p
                val syncR = frameProcessor.applyImageParams(readBack)
                setR.isSuccess to "code=${setR.code}, msg=${setR.message}, FileSDK=${syncR.message}"
            }
        }
        runCase("GetImageFlip") {
            val r = UVCManager.getImageFlip(dev)
            val d = r.getOrNull()
            (r.isSuccess && d != null) to "flipMode=${d?.flipMode}"
        }
        runCase("SetImageFlip") {
            val getR = UVCManager.getImageFlip(dev)
            val mode = getR.getOrNull()
            if (!getR.isSuccess || mode == null) {
                false to "${strings.skipped}: ${strings.getAction("ImageFlip")} ${strings.failedPrefix} code=${getR.code}, msg=${getR.message}"
            } else {
                val setR = UVCManager.setImageFlip(dev, mode)
                val readBack = UVCManager.getImageFlip(dev).getOrNull() ?: mode
                val syncR = frameProcessor.applyImageFlipMode(readBack.flipMode)
                setR.isSuccess to "code=${setR.code}, msg=${setR.message}, flipMode=${readBack.flipMode}, ${syncR.message}"
            }
        }
        runCase("GetThermalSR") {
            val r = UVCManager.getThermalSR(dev)
            val d = r.getOrNull()
            (r.isSuccess && d != null) to "srMode=${d?.srMode}"
        }
        runCase("SetThermalSR") {
            val getR = UVCManager.getThermalSR(dev)
            val mode = getR.getOrNull()
            if (!getR.isSuccess || mode == null) {
                false to "${strings.skipped}: ${strings.getAction("ThermalSR")} ${strings.failedPrefix} code=${getR.code}, msg=${getR.message}"
            } else {
                val setR = UVCManager.setThermalSR(dev, mode)
                val readBack = UVCManager.getThermalSR(dev).getOrNull() ?: mode
                setR.isSuccess to "code=${setR.code}, msg=${setR.message}, srMode=${readBack.srMode}"
            }
        }
    }
    if (dev.isAcousticDevice()) {
        runCase("GetAcousticFreqRange") {
            val (code, range) = AcousticPreview.readFreqRange(dev)
            if (code == 0 && range != null) {
                true to describeAcousticFreq(range)
            } else {
                false to "code=$code"
            }
        }
        runCase("SetAcousticFreqRange") {
            val (code, range) = AcousticPreview.readFreqRange(dev)
            if (code != 0 || range == null) {
                false to "${strings.skipped}: ${strings.getAction(strings.acousticFreq)} ${strings.failedPrefix} code=$code"
            } else {
                val setCode = AcousticPreview.writeFreqRange(dev, range.minHz, range.maxHz)
                (setCode == 0) to "code=$setCode, ${describeAcousticFreq(range)}"
            }
        }
        runCase("GetAcousticDbScale") {
            val r = UVCManager.getAcousticDbScale(dev)
            val d = r.getOrNull()
            if (r.isSuccess && d != null) {
                true to describeAcousticScale(d)
            } else {
                false to "code=${r.code}, msg=${r.message}"
            }
        }
        runCase("SetAcousticDbScale") {
            val getR = UVCManager.getAcousticDbScale(dev)
            val scale = getR.getOrNull()
            if (!getR.isSuccess || scale == null) {
                false to "${strings.skipped}: ${strings.getAction(strings.acousticScale)} ${strings.failedPrefix} code=${getR.code}, msg=${getR.message}"
            } else {
                val setR = UVCManager.setAcousticDbScale(dev, scale)
                setR.isSuccess to "code=${setR.code}, msg=${setR.message}, ${describeAcousticScale(scale)}"
            }
        }
    }
    return passCount to totalCount
}
