package com.fotric.cardinal.uvc

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.fotric.holley.uvc.ImageFlipMode
import com.fotric.holley.uvc.ImageParam
import com.fotric.holley.uvc.RangeIndex
import com.fotric.holley.uvc.ShutterMode
import com.fotric.holley.uvc.ThermalAlarm
import com.fotric.holley.uvc.ThermalGain
import com.fotric.holley.uvc.ThermalParam
import com.fotric.holley.uvc.ThermalSRMode
import com.fotric.holley.uvc.UVCDeviceInfo
import com.fotric.holley.uvc.UVCManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 设备控制面板（主 Tab 1）。
 * 原来多达 10 个的二级 ScrollableTabRow 改为下拉选择，
 * 节省一行 Tab 的高度，也避免在手机上反复横滑查找指令。
 */
@Composable
internal fun DeviceControlPanel(
    state: UvcUiState,
    strings: AppStrings,
    frameProcessor: IRtekFileFrameProcessor,
    onRunCommand: (String, suspend (UVCDeviceInfo) -> String) -> Unit,
    modifier: Modifier = Modifier
) {
    val commandTabs = if (state.isAcoustic) {
        listOf(
            9 to strings.tabAcousticFreq,
            8 to strings.tabAcousticScale
        )
    } else {
        strings.deviceCommandTabs.dropLast(2).mapIndexed { index, title -> index to title }
    }
    val rangeOptions = remember(strings, state.rangeItems) {
        state.rangeItems?.map { (id, min, max) -> id to strings.rangeNamed(id, min, max) }
            ?: listOf(
                0.toShort() to strings.rangeFallback(0),
                1.toShort() to strings.rangeFallback(1)
            )
    }
    val thermalParamOptions = remember(strings) {
        listOf(
            strings.thermalDefault to ThermalParam(1.0f, 1.0f, 25.0f, 25.0f, 0.5f),
            strings.thermalTypical to ThermalParam(0.95f, 1.0f, 25.0f, 25.0f, 0.5f),
            strings.thermalLowEmissivity to ThermalParam(0.80f, 2.0f, 25.0f, 25.0f, 0.5f)
        )
    }
    val imageParamOptions = remember(strings) {
        listOf(
            strings.imageDefault to ImageParam(50.toShort(), 50.toShort(), 0.toShort(), 0.toByte()),
            strings.imageEnhanced to ImageParam(60.toShort(), 50.toShort(), 50.toShort(), 1.toByte()),
            strings.imageSoft to ImageParam(40.toShort(), 50.toShort(), 0.toShort(), 0.toByte())
        )
    }

    PanelCard(modifier = modifier) {
        PanelTitle(strings.deviceCommands)
        PanelDropdown(
            options = commandTabs,
            selectedValue = state.selectedDeviceCommandTab,
            onSelected = { state.selectedDeviceCommandTab = it }
        )
        when (state.selectedDeviceCommandTab) {
            0 -> {
                Button(
                    onClick = {
                        onRunCommand(strings.runCalibrate) { dev ->
                            val r = UVCManager.thermalCalibrate(dev)
                            "code=${r.code}, msg=${r.message}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(strings.runCalibrate, style = MaterialTheme.typography.labelMedium) }
            }

            1 -> {
                DeviceSettingRow(
                    title = strings.shutterMode,
                    options = listOf(0.toByte() to strings.shutterAuto, 1.toByte() to strings.shutterManual),
                    selectedValue = state.selectedShutterMode,
                    onSelected = { state.selectedShutterMode = it },
                    getLabel = strings.get,
                    setLabel = strings.set,
                    onGet = {
                        onRunCommand(strings.getAction(strings.shutterMode)) { dev ->
                            val r = UVCManager.getThermalShutterMode(dev)
                            val mode = r.getOrNull()
                            if (r.isSuccess && mode != null) {
                                state.selectedShutterMode = mode.mode
                                "mode=${mode.mode}"
                            } else {
                                "${strings.failedPrefix} code=${r.code}, msg=${r.message}"
                            }
                        }
                    },
                    onSet = {
                        onRunCommand(strings.setAction(strings.shutterMode)) { dev ->
                            val r = UVCManager.setThermalShutterMode(dev, ShutterMode(state.selectedShutterMode))
                            "mode=${state.selectedShutterMode}, code=${r.code}, msg=${r.message}"
                        }
                    }
                )
            }

            2 -> {
                DeviceSettingRow(
                    title = strings.shutterTime,
                    options = listOf(
                        1.toShort() to strings.minutes(1),
                        5.toShort() to strings.minutes(5),
                        10.toShort() to strings.minutes(10),
                        15.toShort() to strings.minutes(15),
                        30.toShort() to strings.minutes(30),
                        60.toShort() to strings.minutes(60)
                    ),
                    selectedValue = state.selectedShutterTime,
                    onSelected = { state.selectedShutterTime = it },
                    getLabel = strings.get,
                    setLabel = strings.set,
                    onGet = {
                        onRunCommand(strings.getAction(strings.shutterTime)) { dev ->
                            val r = UVCManager.getThermalShutterTime(dev)
                            val minutes = r.getOrNull()
                            if (r.isSuccess && minutes != null) {
                                state.selectedShutterTime = minutes
                                "minutes=$minutes"
                            } else {
                                "${strings.failedPrefix} code=${r.code}, msg=${r.message}"
                            }
                        }
                    },
                    onSet = {
                        onRunCommand(strings.setAction(strings.shutterTime)) { dev ->
                            val r = UVCManager.setThermalShutterTime(dev, state.selectedShutterTime)
                            "minutes=${state.selectedShutterTime}, code=${r.code}, msg=${r.message}"
                        }
                    }
                )
            }

            3 -> {
                DeviceSettingRow(
                    title = strings.tempRange,
                    options = rangeOptions,
                    selectedValue = state.selectedRangeIndex,
                    onSelected = { state.selectedRangeIndex = it },
                    getLabel = strings.get,
                    setLabel = strings.set,
                    onGet = {
                        onRunCommand(strings.getAction(strings.tempRange)) { dev ->
                            val lensR = UVCManager.getLensInfo(dev)
                            val lens = lensR.getOrNull()
                            val indexR = UVCManager.getThermalRangeIndex(dev)
                            val index = indexR.getOrNull()
                            if (lensR.isSuccess && lens != null && indexR.isSuccess && index != null) {
                                state.selectedRangeIndex = index.rangeIndex
                                val nextItems = lens.ranges
                                    .take(lens.rangesCount.toInt())
                                    .map { Triple(it.id, it.min, it.max) }
                                if (nextItems.isNotEmpty()) state.rangeItems = nextItems
                                val nextOptions = nextItems.map { (id, min, max) ->
                                    strings.rangeNamed(id, min, max)
                                }
                                "current=${index.rangeIndex}, ranges=${nextOptions.joinToString()}"
                            } else {
                                "${strings.failedPrefix} lens=${lensR.code}/${lensR.message}, index=${indexR.code}/${indexR.message}"
                            }
                        }
                    },
                    onSet = {
                        onRunCommand(strings.setAction(strings.tempRange)) { dev ->
                            val r = UVCManager.setThermalRangeIndex(dev, RangeIndex(state.selectedRangeIndex))
                            "rangeIndex=${state.selectedRangeIndex}, code=${r.code}, msg=${r.message}"
                        }
                    }
                )
            }

            4 -> {
                DeviceSettingRow(
                    title = strings.tempParams,
                    options = thermalParamOptions.mapIndexed { index, item -> index to item.first },
                    selectedValue = state.selectedThermalParamIndex,
                    onSelected = { state.selectedThermalParamIndex = it },
                    getLabel = strings.get,
                    setLabel = strings.set,
                    onGet = {
                        onRunCommand(strings.getAction(strings.tempParams)) { dev ->
                            val r = UVCManager.getThermalParams(dev)
                            val params = r.getOrNull()
                            if (r.isSuccess && params != null) {
                                val sync = frameProcessor.applyThermalParams(params)
                                "emissivity=${params.emissivity}, distance=${params.distance}, refl=${params.reflTemp}, atm=${params.atmTemp}, FileSDK=${sync.message}"
                            } else {
                                "${strings.failedPrefix} code=${r.code}, msg=${r.message}"
                            }
                        }
                    },
                    onSet = {
                        onRunCommand(strings.setAction(strings.tempParams)) { dev ->
                            val params = thermalParamOptions[state.selectedThermalParamIndex].second
                            val r = UVCManager.setThermalParams(dev, params)
                            val readBack = UVCManager.getThermalParams(dev).getOrNull() ?: params
                            val sync = frameProcessor.applyThermalParams(readBack)
                            "preset=${thermalParamOptions[state.selectedThermalParamIndex].first}, code=${r.code}, msg=${r.message}, FileSDK=${sync.message}"
                        }
                    }
                )
            }

            5 -> {
                DeviceSettingRow(
                    title = strings.imageParams,
                    options = imageParamOptions.mapIndexed { index, item -> index to item.first },
                    selectedValue = state.selectedImageParamIndex,
                    onSelected = { state.selectedImageParamIndex = it },
                    getLabel = strings.get,
                    setLabel = strings.set,
                    onGet = {
                        onRunCommand(strings.getAction(strings.imageParams)) { dev ->
                            val r = withContext(Dispatchers.IO) {
                                UVCManager.getImageParams(dev)
                            }
                            val params = r.getOrNull()
                            if (r.isSuccess && params != null) {
                                "contrast=${params.contrast}, brightness=${params.brightness}, dde=${params.ddeEnable}/${params.ddeValue}"
                            } else {
                                "${strings.failedPrefix} code=${r.code}, msg=${r.message}"
                            }
                        }
                    },
                    onSet = {
                        onRunCommand(strings.setAction(strings.imageParams)) { dev ->
                            val params = imageParamOptions[state.selectedImageParamIndex].second
                            val (r, readBack) = withContext(Dispatchers.IO) {
                                val setR = UVCManager.setImageParams(dev, params)
                                val deviceParams = UVCManager.getImageParams(dev).getOrNull() ?: params
                                setR to deviceParams
                            }
                            val sync = withContext(Dispatchers.IO) {
                                frameProcessor.applyImageParams(readBack)
                            }
                            "preset=${imageParamOptions[state.selectedImageParamIndex].first}, code=${r.code}, msg=${r.message}, FileSDK=${sync.message}"
                        }
                    }
                )
            }

            6 -> {
                DeviceSettingRow(
                    title = strings.imageFlip,
                    options = listOf(
                        0.toShort() to strings.flipNone,
                        1.toShort() to strings.flipUpDown,
                        2.toShort() to strings.flipLeftRight,
                        3.toShort() to strings.flipCenter
                    ),
                    selectedValue = state.selectedFlipMode,
                    onSelected = { state.selectedFlipMode = it },
                    getLabel = strings.get,
                    setLabel = strings.set,
                    onGet = {
                        onRunCommand(strings.getAction(strings.imageFlip)) { dev ->
                            val r = UVCManager.getImageFlip(dev)
                            val mode = r.getOrNull()
                            if (r.isSuccess && mode != null) {
                                state.selectedFlipMode = mode.flipMode
                                val sync = frameProcessor.applyImageFlipMode(mode.flipMode)
                                "flipMode=${mode.flipMode}, ${sync.message}"
                            } else {
                                "${strings.failedPrefix} code=${r.code}, msg=${r.message}"
                            }
                        }
                    },
                    onSet = {
                        onRunCommand(strings.setAction(strings.imageFlip)) { dev ->
                            val r = UVCManager.setImageFlip(dev, ImageFlipMode(state.selectedFlipMode))
                            val readBack = UVCManager.getImageFlip(dev).getOrNull() ?: ImageFlipMode(state.selectedFlipMode)
                            val sync = frameProcessor.applyImageFlipMode(readBack.flipMode)
                            "flipMode=${readBack.flipMode}, code=${r.code}, msg=${r.message}, ${sync.message}"
                        }
                    }
                )
            }

            9 -> {
                AcousticFreqPanel(
                    strings = strings,
                    minText = state.acousticFreqMinText,
                    onMinTextChange = { state.acousticFreqMinText = it },
                    maxText = state.acousticFreqMaxText,
                    onMaxTextChange = { state.acousticFreqMaxText = it },
                    onGet = {
                        onRunCommand(strings.getAction(strings.acousticFreq)) { dev ->
                            val (code, range) = withContext(Dispatchers.IO) {
                                AcousticPreview.readFreqRange(dev)
                            }
                            if (code == 0 && range != null) {
                                state.acousticFreqMinText = formatAcousticKhz(range.minHz)
                                state.acousticFreqMaxText = formatAcousticKhz(range.maxHz)
                                describeAcousticFreq(range)
                            } else {
                                "${strings.failedPrefix} code=$code"
                            }
                        }
                    },
                    onSet = {
                        val minKhz = state.acousticFreqMinText.toFloatOrNull()
                        val maxKhz = state.acousticFreqMaxText.toFloatOrNull()
                        val range = acousticFreqFromKhz(state.acousticFreqMinText, state.acousticFreqMaxText)
                        val message = when {
                            minKhz == null || maxKhz == null -> strings.acousticInvalidNumber
                            minKhz !in 0.001f..1000f || maxKhz !in 0.001f..1000f -> strings.acousticFreqBounds
                            minKhz >= maxKhz -> strings.acousticFreqOrder
                            range == null -> strings.acousticInvalidNumber
                            else -> null
                        }
                        if (message != null || range == null) {
                            state.appendApiResult("${strings.setAction(strings.acousticFreq)}: $message")
                        } else {
                            onRunCommand(strings.setAction(strings.acousticFreq)) { dev ->
                                val code = withContext(Dispatchers.IO) {
                                    AcousticPreview.writeFreqRange(dev, range.minHz, range.maxHz)
                                }
                                val readBack = withContext(Dispatchers.IO) {
                                    AcousticPreview.readFreqRange(dev).second
                                }
                                if (readBack != null) {
                                    state.acousticFreqMinText = formatAcousticKhz(readBack.minHz)
                                    state.acousticFreqMaxText = formatAcousticKhz(readBack.maxHz)
                                }
                                "code=$code, ${describeAcousticFreq(readBack ?: range)}"
                            }
                        }
                    }
                )
            }

            7 -> {
                ThermalImagingPanel(
                    language = state.language,
                    gainMode = state.gainMode,
                    onGainModeChange = { state.gainMode = it },
                    gainMinText = state.gainMinText,
                    onGainMinTextChange = { state.gainMinText = it },
                    gainMaxText = state.gainMaxText,
                    onGainMaxTextChange = { state.gainMaxText = it },
                    gainFixedSpan = state.gainFixedSpan,
                    onGainFixedSpanChange = { state.gainFixedSpan = it },
                    gainSpanText = state.gainSpanText,
                    onGainSpanTextChange = { state.gainSpanText = it },
                    colorDist = state.colorDist,
                    onColorDistChange = { state.colorDist = it },
                    srMode = state.selectedSrMode,
                    onSrModeChange = { state.selectedSrMode = it },
                    alarmType = state.alarmType,
                    onAlarmTypeChange = { state.alarmType = it },
                    alarmCustom = state.alarmCustom,
                    onAlarmCustomChange = { state.alarmCustom = it },
                    alarmColorText = state.alarmColorText,
                    onAlarmColorTextChange = { state.alarmColorText = it },
                    alarmMinText = state.alarmMinText,
                    onAlarmMinTextChange = { state.alarmMinText = it },
                    alarmMaxText = state.alarmMaxText,
                    onAlarmMaxTextChange = { state.alarmMaxText = it },
                    onGet = {
                        onRunCommand(strings.getAction(strings.tabImaging)) { dev ->
                            val lut = loadThermalLut(dev, state.thermalLut)
                            if (lut == null) {
                                missingThermalLut(state.language)
                            } else {
                                state.thermalLut = lut
                                val gainR = UVCManager.getThermalGain(dev)
                                gainR.getOrNull()?.let { gain ->
                                    state.gainMode = gain.mode
                                    state.gainMinText = formatAcousticFloat(lut.adToCelsius(gain.min))
                                    state.gainMaxText = formatAcousticFloat(lut.adToCelsius(gain.max))
                                    state.gainFixedSpan = gain.fixedSpan.toInt() != 0
                                    state.gainSpanText = formatAcousticFloat(lut.spanCelsius(gain.min, gain.span))
                                }
                                AcousticPreview.readColorDist(dev)?.let { state.colorDist = it }
                                val srR = UVCManager.getThermalSR(dev)
                                srR.getOrNull()?.let { state.selectedSrMode = it.srMode }
                                val alarmR = UVCManager.getThermalAlarm(dev)
                                alarmR.getOrNull()?.let { alarm ->
                                    state.alarmType = alarm.type
                                    state.alarmCustom = alarm.custom.toInt() != 0
                                    state.alarmColorText = "%08X".format(alarm.color)
                                    state.alarmMinText = formatAcousticFloat(lut.adToCelsius(alarm.min))
                                    state.alarmMaxText = formatAcousticFloat(lut.adToCelsius(alarm.max))
                                }
                                "gain=${gainR.code}, color=${state.colorDist}, sr=${srR.code}/${state.selectedSrMode}, alarm=${alarmR.code}"
                            }
                        }
                    },
                    onSet = {
                        val min = state.gainMinText.toFloatOrNull()
                        val max = state.gainMaxText.toFloatOrNull()
                        val span = state.gainSpanText.toFloatOrNull()
                        val alarmMin = state.alarmMinText.toFloatOrNull()
                        val alarmMax = state.alarmMaxText.toFloatOrNull()
                        val colorText = state.alarmColorText.trim()
                        val color = colorText.toLongOrNull(16)
                        val message = when {
                            min == null || max == null || span == null || alarmMin == null || alarmMax == null || span <= 0f ->
                                strings.acousticInvalidNumber
                            color == null || colorText.length != 8 ->
                                if (state.language == AppLanguage.Chinese) "报警颜色需为 8 位 ARGB 十六进制" else "Alarm color must be an 8-digit ARGB hex value"
                            else -> null
                        }
                        if (message != null) {
                            state.appendApiResult("${strings.setAction(strings.tabImaging)}: $message")
                        } else {
                            onRunCommand(strings.setAction(strings.tabImaging)) { dev ->
                                val lut = loadThermalLut(dev, state.thermalLut)
                                if (lut == null) {
                                    missingThermalLut(state.language)
                                } else {
                                    state.thermalLut = lut
                                    val gain = ThermalGain(
                                        state.gainMode,
                                        lut.celsiusToAd(max!!),
                                        lut.celsiusToAd(min!!),
                                        if (state.gainFixedSpan) 1.toByte() else 0.toByte(),
                                        lut.spanAd(min!!, span!!)
                                    )
                                    val gainR = UVCManager.setThermalGain(dev, gain)
                                    val srR = UVCManager.setThermalSR(dev, ThermalSRMode(state.selectedSrMode))
                                    val distR = AcousticPreview.writeColorDist(dev, state.colorDist)
                                    val alarm = ThermalAlarm(
                                        state.alarmType,
                                        if (state.alarmCustom) 1.toByte() else 0.toByte(),
                                        color!!.toInt(),
                                        lut.celsiusToAd(alarmMax!!),
                                        lut.celsiusToAd(alarmMin!!)
                                    )
                                    val alarmR = UVCManager.setThermalAlarm(dev, alarm)
                                    "gain=${gainR.code}, sr=${srR.code}, color=$distR, alarm=${alarmR.code}"
                                }
                            }
                        }
                    }
                )
            }

            8 -> {
                AcousticScalePanel(
                    strings = strings,
                    mode = state.acousticMode,
                    onModeChange = { state.acousticMode = it },
                    fixedSpan = state.acousticFixedSpan,
                    onFixedSpanChange = { state.acousticFixedSpan = it },
                    spanText = state.acousticSpanText,
                    onSpanTextChange = { state.acousticSpanText = it },
                    sensitivityText = state.acousticSensitivityText,
                    onSensitivityTextChange = { state.acousticSensitivityText = it },
                    sourceSizeText = state.acousticSourceSizeText,
                    onSourceSizeTextChange = { state.acousticSourceSizeText = it },
                    limitText = state.acousticLimitText,
                    onLimitTextChange = { state.acousticLimitText = it },
                    maxText = state.acousticMaxText,
                    onMaxTextChange = { state.acousticMaxText = it },
                    minText = state.acousticMinText,
                    onMinTextChange = { state.acousticMinText = it },
                    focusEnabled = state.acousticFocusEnabled,
                    onFocusEnabledChange = { state.acousticFocusEnabled = it },
                    onGet = {
                        onRunCommand(strings.getAction(strings.acousticScale)) { dev ->
                            val r = withContext(Dispatchers.IO) {
                                UVCManager.getAcousticDbScale(dev)
                            }
                            val scale = r.getOrNull()
                            if (r.isSuccess && scale != null) {
                                state.applyAcousticScale(scale)
                                describeAcousticScale(scale)
                            } else {
                                "${strings.failedPrefix} code=${r.code}, msg=${r.message}"
                            }
                        }
                    },
                    onSet = {
                        val scale = acousticScaleFromFields(
                            state.acousticMode,
                            state.acousticFixedSpan,
                            state.acousticSpanText,
                            state.acousticSensitivityText,
                            state.acousticSourceSizeText,
                            state.acousticLimitText,
                            state.acousticMaxText,
                            state.acousticMinText,
                            state.acousticFocusEnabled
                        )
                        if (scale == null) {
                            state.appendApiResult("${strings.setAction(strings.acousticScale)}: ${strings.acousticInvalidNumber}")
                        } else {
                            onRunCommand(strings.setAction(strings.acousticScale)) { dev ->
                                val r = withContext(Dispatchers.IO) {
                                    UVCManager.setAcousticDbScale(dev, scale)
                                }
                                val readBack = withContext(Dispatchers.IO) {
                                    UVCManager.getAcousticDbScale(dev).getOrNull()
                                }
                                if (readBack != null) state.applyAcousticScale(readBack)
                                "code=${r.code}, msg=${r.message}, ${describeAcousticScale(readBack ?: scale)}"
                            }
                        }
                    },
                    onRestore = { state.applyAcousticScale(defaultAcousticScale()) }
                )
            }
        }
    }
}
