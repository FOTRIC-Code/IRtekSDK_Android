package com.fotric.cardinal.uvc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** USB 授权引导面板：未授权时居中展示说明与操作按钮。 */
@Composable
internal fun UsbPermissionPanel(
    strings: AppStrings,
    permissionDeniedHint: Boolean,
    onRequestPermission: () -> Unit,
    onEnterDevicePage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        PanelCard {
            Text(
                text = strings.requestUsbPermissionTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = strings.requestUsbPermissionHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (permissionDeniedHint) {
                Text(
                    text = strings.requestUsbPermissionDeniedHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.requestUsbPermissionButton) }
            Button(
                onClick = onEnterDevicePage,
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.useDevice) }
        }
    }
}

/**
 * 设备与连接卡片。
 * 按钮行位置固定：连接/断开是同一位置的切换按钮，
 * 不再按条件在行内插入/移除按钮，避免布局跳动。
 */
@Composable
internal fun ConnectionPanel(
    deviceCount: Int,
    deviceConnected: Boolean,
    isAcoustic: Boolean,
    isStreaming: Boolean,
    strings: AppStrings,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onTogglePreview: () -> Unit,
    onGetResolution: () -> Unit,
    modifier: Modifier = Modifier
) {
    PanelCard(modifier = modifier) {
        PanelTitle(strings.deviceAndConnection)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                Text(strings.refresh, style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(onClick = onRequestPermission, modifier = Modifier.weight(1f)) {
                Text(strings.requestPermission, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (!deviceConnected) {
            Button(
                onClick = onConnect,
                enabled = deviceCount > 0,
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.connect, style = MaterialTheme.typography.labelMedium) }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onDisconnect, modifier = Modifier.weight(1f)) {
                    Text(strings.disconnect, style = MaterialTheme.typography.labelMedium)
                }
                Button(onClick = onTogglePreview, modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isStreaming) strings.stopPreview else strings.startPreview,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            if (!isAcoustic) {
                OutlinedButton(
                    onClick = onGetResolution,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(strings.getRes, style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}
