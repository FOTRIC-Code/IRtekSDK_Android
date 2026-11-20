package com.fotric.cardinal.uvc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fotric.holley.uvc.AcousticDbScale
import com.fotric.holley.uvc.UVCDeviceInfo
import java.util.Locale

internal const val ACOUSTIC_MODE_MONO: Byte = 1
internal const val ACOUSTIC_MODE_MULTI: Byte = 2
internal const val ACOUSTIC_MODE_FIXED: Byte = 3
internal const val ACOUSTIC_MODE_HOLO: Byte = 4

private const val ACOUSTIC_VID = 0x03FD
private const val ACOUSTIC_PID = 0x0100
private const val ACOUSTIC_SCALE_MAX = 1000f
private const val ACOUSTIC_SCALE_MIN = -100f

internal fun UVCDeviceInfo.isAcousticDevice(): Boolean {
    val vendorId = vid.toInt() and 0xFFFF
    val productId = pid.toInt() and 0xFFFF
    return vendorId == ACOUSTIC_VID && productId == ACOUSTIC_PID
}

internal fun normalizedAcousticMode(mode: Byte): Byte = when (mode) {
    ACOUSTIC_MODE_MULTI, ACOUSTIC_MODE_FIXED, ACOUSTIC_MODE_HOLO -> mode
    else -> ACOUSTIC_MODE_MONO
}

internal fun defaultAcousticScale(): AcousticDbScale = AcousticDbScale(
    ACOUSTIC_MODE_MONO,
    ACOUSTIC_SCALE_MAX,
    ACOUSTIC_SCALE_MIN,
    0.toByte(),
    0.1f,
    3.2f,
    3,
    ACOUSTIC_SCALE_MIN,
    0.toByte()
)

internal fun acousticScaleFromFields(
    mode: Byte,
    fixedSpan: Boolean,
    spanText: String,
    sensitivityText: String,
    sourceSizeText: String,
    limitText: String,
    maxText: String,
    minText: String,
    focusEnabled: Boolean
): AcousticDbScale? {
    val span = spanText.toFloatOrNull() ?: return null
    val sensitivity = sensitivityText.toFloatOrNull() ?: return null
    val sourceSize = sourceSizeText.toIntOrNull() ?: return null
    val limit = limitText.toFloatOrNull() ?: return null
    val max = if (mode == ACOUSTIC_MODE_FIXED) maxText.toFloatOrNull() ?: return null else ACOUSTIC_SCALE_MAX
    val min = if (mode == ACOUSTIC_MODE_FIXED) minText.toFloatOrNull() ?: return null else ACOUSTIC_SCALE_MIN
    if (mode == ACOUSTIC_MODE_FIXED &&
        (max !in ACOUSTIC_SCALE_MIN..ACOUSTIC_SCALE_MAX ||
            min !in ACOUSTIC_SCALE_MIN..ACOUSTIC_SCALE_MAX ||
            min >= max)
    ) {
        return null
    }
    return AcousticDbScale(
        mode,
        max,
        min,
        if (mode == ACOUSTIC_MODE_HOLO && fixedSpan) 1.toByte() else 0.toByte(),
        span,
        sensitivity,
        sourceSize,
        limit,
        if (focusEnabled) 1.toByte() else 0.toByte()
    )
}

internal fun formatAcousticFloat(value: Float): String {
    val text = String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
    return text.ifEmpty { "0" }
}

internal data class AcousticFreqRange(val minHz: Float, val maxHz: Float)

internal fun acousticFreqFromKhz(minText: String, maxText: String): AcousticFreqRange? {
    val minKhz = minText.toFloatOrNull() ?: return null
    val maxKhz = maxText.toFloatOrNull() ?: return null
    if (minKhz !in 0.001f..1000f || maxKhz !in 0.001f..1000f || minKhz >= maxKhz) return null
    return AcousticFreqRange(minKhz * 1000f, maxKhz * 1000f)
}

internal fun formatAcousticKhz(hz: Float): String = formatAcousticFloat(hz / 1000f)

internal fun describeAcousticFreq(range: AcousticFreqRange): String =
    "min=${formatAcousticKhz(range.minHz)} kHz (${formatAcousticFloat(range.minHz)} Hz), " +
        "max=${formatAcousticKhz(range.maxHz)} kHz (${formatAcousticFloat(range.maxHz)} Hz)"

internal fun describeAcousticScale(scale: AcousticDbScale): String =
    "mode=${scale.mode}, max=${scale.max}, min=${scale.min}, fixedSpan=${scale.fixedSpan}, " +
        "span=${scale.span}, sensitivity=${scale.sensitivity}, sourceSize=${scale.sourceSize}, " +
        "limit=${scale.limit}, focus=${scale.focusEnabled}"

@Composable
internal fun AcousticFreqPanel(
    strings: AppStrings,
    minText: String,
    onMinTextChange: (String) -> Unit,
    maxText: String,
    onMaxTextChange: (String) -> Unit,
    onGet: () -> Unit,
    onSet: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onGet, modifier = Modifier.weight(1f)) {
                Text(strings.get)
            }
            Button(onClick = onSet, modifier = Modifier.weight(1f)) {
                Text(strings.set)
            }
        }
        Text(strings.acousticFreq)
        AcousticNumberField(strings.acousticFreqMin, minText, onMinTextChange)
        AcousticNumberField(strings.acousticFreqMax, maxText, onMaxTextChange)
    }
}

@Composable
internal fun AcousticScalePanel(
    strings: AppStrings,
    mode: Byte,
    onModeChange: (Byte) -> Unit,
    fixedSpan: Boolean,
    onFixedSpanChange: (Boolean) -> Unit,
    spanText: String,
    onSpanTextChange: (String) -> Unit,
    sensitivityText: String,
    onSensitivityTextChange: (String) -> Unit,
    sourceSizeText: String,
    onSourceSizeTextChange: (String) -> Unit,
    limitText: String,
    onLimitTextChange: (String) -> Unit,
    maxText: String,
    onMaxTextChange: (String) -> Unit,
    minText: String,
    onMinTextChange: (String) -> Unit,
    focusEnabled: Boolean,
    onFocusEnabledChange: (Boolean) -> Unit,
    onGet: () -> Unit,
    onSet: () -> Unit,
    onRestore: () -> Unit
) {
    val holo = mode == ACOUSTIC_MODE_HOLO
    val fixed = mode == ACOUSTIC_MODE_FIXED
    val source = mode == ACOUSTIC_MODE_MONO || mode == ACOUSTIC_MODE_MULTI
    val modeOptions = listOf(
        ACOUSTIC_MODE_MONO to strings.acousticModeMono,
        ACOUSTIC_MODE_MULTI to strings.acousticModeMulti,
        ACOUSTIC_MODE_FIXED to strings.acousticModeFixed,
        ACOUSTIC_MODE_HOLO to strings.acousticModeHolo
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onGet, modifier = Modifier.weight(1f)) {
                Text(strings.get)
            }
            Button(onClick = onSet, modifier = Modifier.weight(1f)) {
                Text(strings.set)
            }
        }
        OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
            Text(strings.acousticRestore)
        }

        AcousticChoiceRow(
            title = strings.acousticScale,
            options = modeOptions,
            selectedValue = mode,
            onSelected = onModeChange
        )
        Text(if (holo || fixed) strings.acousticDbSection else strings.acousticSourceSection)
        if (source) {
            AcousticNumberField(strings.acousticSensitivity, sensitivityText, onSensitivityTextChange)
            AcousticNumberField(strings.acousticSourceSize, sourceSizeText, onSourceSizeTextChange)
        }
        if (fixed) {
            AcousticNumberField(strings.acousticDbMax, maxText, onMaxTextChange)
            AcousticNumberField(strings.acousticDbMin, minText, onMinTextChange)
        }
        if (holo) {
            AcousticSwitchRow(strings.acousticFixedSpan, fixedSpan, onFixedSpanChange)
            if (fixedSpan) {
                AcousticNumberField(strings.acousticSpan, spanText, onSpanTextChange)
            }
        }
        Text(strings.acousticImaging)
        AcousticNumberField(strings.acousticLimit, limitText, onLimitTextChange)
        AcousticSwitchRow(strings.acousticFocus, focusEnabled, onFocusEnabledChange)
    }
}

@Composable
internal fun <T> AcousticChoiceRow(
    title: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = options.firstOrNull { it.first == selectedValue }?.second
        ?: selectedValue.toString()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(selectedText) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun AcousticNumberField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(title) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun AcousticSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
