package com.fotric.cardinal.uvc

import android.hardware.usb.UsbManager
import com.fotric.holley.uvc.UVCDeviceInfo
import com.fotric.holley.uvc.UVCManager

/**
 * 扩展函数：从 UsbManager 获取 UVC 设备列表（Android 推荐方式）
 */
fun UVCManager.getUVCList(usbManager: UsbManager): List<UVCDeviceInfo> {
    return usbManager.deviceList.values.map { d ->
        UVCDeviceInfo(
            path = d.deviceName,
            vid = d.vendorId.toShort(),
            pid = d.productId.toShort()
        )
    }
}
