package com.fotric.cardinal.uvc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** 调色板面板（主 Tab 2）。 */
@Composable
internal fun PalettePanel(
    state: UvcUiState,
    strings: AppStrings,
    onGetPalette: () -> Unit,
    onSetPalette: () -> Unit,
    modifier: Modifier = Modifier
) {
    PanelCard(modifier = modifier) {
        PanelTitle(strings.palette)
        DeviceSettingRow(
            title = strings.presetPalette,
            options = state.paletteOptions,
            selectedValue = state.selectedPaletteRefNo,
            onSelected = { state.selectedPaletteRefNo = it },
            getLabel = strings.get,
            setLabel = strings.set,
            onGet = onGetPalette,
            onSet = onSetPalette
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.invertPalette, style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = state.paletteInverted,
                onCheckedChange = { state.paletteInverted = it }
            )
        }
        if (state.isAcoustic) {
            Text(
                text = "${strings.paletteBlend}: ${state.paletteAlpha}",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = state.paletteAlpha.toFloat(),
                onValueChange = { state.paletteAlpha = it.toInt() },
                valueRange = 0f..100f
            )
        }
        Text(
            text = strings.paletteHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
