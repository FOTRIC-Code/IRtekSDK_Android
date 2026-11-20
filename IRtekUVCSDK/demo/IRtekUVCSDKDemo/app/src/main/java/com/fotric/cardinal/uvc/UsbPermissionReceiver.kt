package com.fotric.cardinal.uvc

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log

/**
 * USB 权限广播接收器
 * 当用户授予或拒绝 USB 权限时接收广播
 */
class UsbPermissionReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "UsbPermissionReceiver"
        const val ACTION_USB_PERMISSION = "com.fotric.cardinal.uvc.USB_PERMISSION"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        when (action) {
            ACTION_USB_PERMISSION -> {
                synchronized(this) {
                    // 打印所有 extras，便于排查 EXTRA_PERMISSION_GRANTED 为 false 的原因
                    intent.extras?.keySet()?.forEach { key ->
                        val v = intent.extras?.get(key)
                        Log.d(TAG, "USB_PERMISSION extra: key=$key type=${v?.javaClass?.simpleName} value=$v")
                    }
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    val grantedCompat = intent.extras?.get(UsbManager.EXTRA_PERMISSION_GRANTED)?.let {
                        when (it) { is Boolean -> it; is Int -> it != 0; else -> granted }
                    } ?: granted
                    Log.d(TAG, "EXTRA_PERMISSION_GRANTED: raw=$granted resolved=$grantedCompat")
                    if (grantedCompat) {
                        device?.apply {
                            Log.d(TAG, "USB permission granted for device: VID=0x${vendorId.toString(16)}, PID=0x${productId.toString(16)}")
                        }
                    } else {
                        Log.w(TAG, "USB permission denied for device: ${device?.deviceName}")
                    }
                }
            }
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                device?.apply {
                    Log.d(TAG, "USB device attached: VID=0x${vendorId.toString(16)}, PID=0x${productId.toString(16)}")
                    // 设备已插入，可以请求权限
                    requestUsbPermission(context, this)
                }
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                device?.apply {
                    Log.d(TAG, "USB device detached: VID=0x${vendorId.toString(16)}, PID=0x${productId.toString(16)}")
                }
            }
        }
    }
    
    /**
     * 请求 USB 设备权限
     */
    private fun requestUsbPermission(context: Context, device: UsbDevice) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        if (usbManager == null) {
            Log.e(TAG, "UsbManager not available")
            return
        }
        
        // 检查是否已有权限
        if (usbManager.hasPermission(device)) {
            Log.d(TAG, "Already has permission for device: ${device.deviceName}")
            return
        }
        
        // 创建权限请求的 PendingIntent
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // 请求权限
        usbManager.requestPermission(device, permissionIntent)
        Log.d(TAG, "Requesting USB permission for device: ${device.deviceName}")
    }
}
