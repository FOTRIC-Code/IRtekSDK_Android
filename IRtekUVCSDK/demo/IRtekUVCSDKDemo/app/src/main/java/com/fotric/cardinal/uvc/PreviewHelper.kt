package com.fotric.cardinal.uvc

import android.graphics.Bitmap

/**
 * 原始帧转 Bitmap。预览画面改由 UVC 出图接口生成，这里不再做本地转换。
 */
fun yuy2ToBitmap(data: ByteArray, length: Int, width: Int, height: Int): Bitmap? = null

fun adToBitmap(data: ByteArray, length: Int, width: Int, height: Int): Bitmap? = null

/**
 * 帧数据转 Bitmap（根据格式自动选择转换方法）。
 */
fun frameDataToBitmap(
    data: ByteArray,
    length: Int,
    width: Int,
    height: Int,
    format: String?
): Bitmap? {
    val fmt = format?.uppercase()?.trim() ?: "YUY2"
    return when {
        fmt == "YUY2" -> adToBitmap(data, length, width, height)
        else -> yuy2ToBitmap(data, length, width, height)
    }
}
