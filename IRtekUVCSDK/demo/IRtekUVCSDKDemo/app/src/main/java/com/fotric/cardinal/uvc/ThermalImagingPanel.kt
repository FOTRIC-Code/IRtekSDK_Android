package com.fotric.cardinal.uvc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal const val GAIN_FIXED: Byte = 1
internal const val GAIN_AUTO: Byte = 2
internal const val GAIN_TOUCH: Byte = 3

internal const val COLOR_T_TWB: Byte = 1
internal const val COLOR_TEMP_LINEAR: Byte = 2
internal const val COLOR_AD_LINEAR: Byte = 3

internal const val ALARM_NORMAL: Byte = 0
internal const val ALARM_BELOW: Byte = 1
internal const val ALARM_ABOVE: Byte = 2
internal const val ALARM_BETWEEN: Byte = 3
internal const val ALARM_MIX: Byte = 4
internal const val ALARM_OUTSIDE: Byte = 5

internal const val SR_NONE: Short = 0
internal const val SR_ENABLE: Short = 1

@Composable
internal fun ThermalImagingPanel(
    language: AppLanguage,
    gainMode: Byte,
    onGainModeChange: (Byte) -> Unit,
    gainMinText: String,
    onGainMinTextChange: (String) -> Unit,
    gainMaxText: String,
    onGainMaxTextChange: (String) -> Unit,
    gainFixedSpan: Boolean,
    onGainFixedSpanChange: (Boolean) -> Unit,
    gainSpanText: String,
    onGainSpanTextChange: (String) -> Unit,
    colorDist: Byte,
    onColorDistChange: (Byte) -> Unit,
    srMode: Short,
    onSrModeChange: (Short) -> Unit,
    alarmType: Byte,
    onAlarmTypeChange: (Byte) -> Unit,
    alarmCustom: Boolean,
    onAlarmCustomChange: (Boolean) -> Unit,
    alarmColorText: String,
    onAlarmColorTextChange: (String) -> Unit,
    alarmMinText: String,
    onAlarmMinTextChange: (String) -> Unit,
    alarmMaxText: String,
    onAlarmMaxTextChange: (String) -> Unit,
    onGet: () -> Unit,
    onSet: () -> Unit
) {
    val chinese = language == AppLanguage.Chinese
    fun text(english: String, chineseText: String) = if (chinese) chineseText else english
    val gainModes = listOf(
        GAIN_FIXED to text("Fixed", "固定"),
        GAIN_AUTO to text("Auto", "自动"),
        GAIN_TOUCH to text("Touch", "触摸")
    )
    val colorModes = listOf(
        COLOR_T_TWB to "T_TWB",
        COLOR_TEMP_LINEAR to text("Temp linear", "温度线性"),
        COLOR_AD_LINEAR to text("AD linear", "AD 线性")
    )
    val srModes = listOf(
        SR_NONE to text("Off", "关闭"),
        SR_ENABLE to text("Super resolution", "超分")
    )
    val alarmModes = listOf(
        ALARM_NORMAL to text("Normal", "正常"),
        ALARM_BELOW to text("Below", "低于"),
        ALARM_ABOVE to text("Above", "高于"),
        ALARM_BETWEEN to text("Between", "区间内"),
        ALARM_MIX to text("Mix palette", "混合调色板"),
        ALARM_OUTSIDE to text("Outside", "区间外")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onGet, modifier = Modifier.weight(1f)) {
                Text(text("Get", "获取"))
            }
            Button(onClick = onSet, modifier = Modifier.weight(1f)) {
                Text(text("Set", "设置"))
            }
        }
        Text(text("Gain (°C)", "温宽 (°C)"))
        ChoiceRow(text("Mode", "模式"), gainModes, gainMode, onGainModeChange)
        AcousticNumberField(text("Min (°C)", "下限 (°C)"), gainMinText, onGainMinTextChange)
        AcousticNumberField(text("Max (°C)", "上限 (°C)"), gainMaxText, onGainMaxTextChange)
        AcousticSwitchRow(text("Fixed span", "固定温宽"), gainFixedSpan, onGainFixedSpanChange)
        AcousticNumberField(text("Span (°C)", "温宽跨度 (°C)"), gainSpanText, onGainSpanTextChange)

        Text(text("Color distribution", "颜色分布"))
        ChoiceRow(text("Type", "类型"), colorModes, colorDist, onColorDistChange)

        Text(text("Thermal imaging", "热像成像"))
        ChoiceRow(text("Super resolution", "超分"), srModes, srMode, onSrModeChange)

        Text(text("Alarm (°C)", "报警 (°C)"))
        ChoiceRow(text("Type", "类型"), alarmModes, alarmType, onAlarmTypeChange)
        AcousticSwitchRow(text("Custom color", "自定义颜色"), alarmCustom, onAlarmCustomChange)
        AcousticNumberField(text("Color ARGB", "颜色 ARGB"), alarmColorText, onAlarmColorTextChange)
        AcousticNumberField(text("Min (°C)", "下限 (°C)"), alarmMinText, onAlarmMinTextChange)
        AcousticNumberField(text("Max (°C)", "上限 (°C)"), alarmMaxText, onAlarmMaxTextChange)
    }
}

@Composable
private fun <T> ChoiceRow(
    title: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelected: (T) -> Unit
) {
    AcousticChoiceRow(title, options, selectedValue, onSelected)
}
