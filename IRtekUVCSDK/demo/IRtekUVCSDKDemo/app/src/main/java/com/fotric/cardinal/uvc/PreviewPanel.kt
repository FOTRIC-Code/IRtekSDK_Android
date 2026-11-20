package com.fotric.cardinal.uvc

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 预览面板：帧画面 + 叠加在画面底部的状态条（帧信息 / 错误）。
 * 尺寸由父布局决定——主屏幕中占满全部可用空间，视频流为视觉主体。
 */
@Composable
internal fun PreviewPanel(
    bitmap: Bitmap?,
    isAcoustic: Boolean,
    centerTemp: Float?,
    deviceConnected: Boolean,
    isStreaming: Boolean,
    streamError: String?,
    frameText: String,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                UVCPreviewSurface(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = bitmap,
                    centerTemp = if (isAcoustic) null else centerTemp,
                    applyThermalOrientation = !isAcoustic
                )
            } else {
                Text(
                    text = when {
                        !deviceConnected -> strings.previewNeedConnect
                        !isStreaming -> strings.previewNeedStart
                        else -> strings.previewWaiting
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (streamError != null || frameText.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (streamError != null) {
                        Text(
                            text = streamError,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF8A80),
                            maxLines = 1
                        )
                    }
                    if (frameText.isNotEmpty()) {
                        Text(
                            text = frameText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 4
                        )
                    }
                }
            }
        }
    }
}
