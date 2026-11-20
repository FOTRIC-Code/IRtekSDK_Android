package com.fotric.cardinal.uvc

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * UVC 热像与竖屏预览坐标系不一致：
 * 先垂直翻转，再逆时针旋转 90°，校正方向与左右。
 */
private const val PREVIEW_ROTATION_DEGREES = -90f

/**
 * Fit 先把整帧放进模块，再按旋转后的宽高放大到铺满，超出部分由父布局裁切。
 */
private fun GraphicsLayerScope.coverPreview(bitmap: Bitmap, applyThermalOrientation: Boolean) {
    val viewW = size.width
    val viewH = size.height
    if (viewW <= 0f || viewH <= 0f || bitmap.width <= 0 || bitmap.height <= 0) return
    val fit = min(viewW / bitmap.width, viewH / bitmap.height)
    val drawnW = bitmap.width * fit
    val drawnH = bitmap.height * fit
    val cover = if (applyThermalOrientation) {
        max(viewW / drawnH, viewH / drawnW)
    } else {
        max(viewW / drawnW, viewH / drawnH)
    }
    scaleX = cover
    scaleY = if (applyThermalOrientation) -cover else cover
    if (applyThermalOrientation) {
        rotationZ = PREVIEW_ROTATION_DEGREES
    }
}

@Composable
fun UVCPreviewSurface(
    modifier: Modifier = Modifier,
    bitmap: Bitmap?,
    centerTemp: Float? = null,
    applyThermalOrientation: Boolean = true
) {
    Box(
        modifier = modifier.clip(RectangleShape),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "UVC preview",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        coverPreview(it, applyThermalOrientation)
                    },
                contentScale = ContentScale.Fit
            )
            centerTemp?.let { temp ->
                Text(
                    text = String.format(Locale.US, "CENTER %.2f °C", temp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-24).dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = Color.Yellow,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }
    }
}
