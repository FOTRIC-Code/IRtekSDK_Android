package com.fotric.cardinal.uvc

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 设备信息面板（主 Tab 0）。 */
@Composable
internal fun DeviceInfoPanel(
    state: UvcUiState,
    strings: AppStrings,
    onRefreshInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceModeName =
        if (state.isAcoustic) strings.deviceModeAcoustic else strings.deviceModeThermal
    PanelCard(modifier = modifier) {
        PanelTitle(strings.deviceInfo)
        Text(
            text = state.deviceInfo
                ?: if (state.currentDevice == null) strings.deviceNotConnected
                else strings.deviceConnectedHint,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = state.currentDevice?.let {
                "VID=${formatUsbId(it.vid)}, PID=${formatUsbId(it.pid)}  $deviceModeName"
            } ?: "VID=-, PID=-",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(
            onClick = onRefreshInfo,
            modifier = Modifier.fillMaxWidth()
        ) { Text(strings.refreshDeviceInfo, style = MaterialTheme.typography.labelMedium) }
    }
}

/** 版本信息面板（主 Tab 3）。 */
@Composable
internal fun VersionPanel(
    uvcSdkVersion: String?,
    fileSdkVersion: String?,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    PanelCard(modifier = modifier) {
        PanelTitle(strings.versionInfo)
        Text(
            text = "UVC SDK: ${uvcSdkVersion ?: "-"}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "FileSDK: ${fileSdkVersion ?: "-"}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
