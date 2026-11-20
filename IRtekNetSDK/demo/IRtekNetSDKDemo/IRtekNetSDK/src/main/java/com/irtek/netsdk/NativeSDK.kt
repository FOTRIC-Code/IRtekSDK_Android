package com.irtek.netsdk

object NativeSDK {
    init {
        try { System.loadLibrary("avutil") } catch (_: UnsatisfiedLinkError) {}
        try { System.loadLibrary("swresample") } catch (_: UnsatisfiedLinkError) {}
        try { System.loadLibrary("swscale") } catch (_: UnsatisfiedLinkError) {}
        try { System.loadLibrary("avcodec") } catch (_: UnsatisfiedLinkError) {}
        try { System.loadLibrary("avformat") } catch (_: UnsatisfiedLinkError) {}
        try { System.loadLibrary("c++_shared") } catch (_: UnsatisfiedLinkError) {}
        System.loadLibrary("irtek_netsdk")
        System.loadLibrary("irtek_netsdk_jni")
    }

    // SDK lifecycle
    @JvmStatic external fun nativeInit(): Int
    @JvmStatic external fun nativeCleanup(): Int
    @JvmStatic external fun nativeEnableLog(enable: Boolean): Int
    @JvmStatic external fun nativeLogin(ip: String, port: Int, username: String, password: String, timeoutMs: Int): Long
    @JvmStatic external fun nativeLogout(handle: Long): Int
    @JvmStatic external fun nativeGetSdkVersion(): String

    // Device search
    @JvmStatic external fun nativeSearchDevices(timeoutMs: Int): String

    // Device management
    @JvmStatic external fun nativeGetDeviceInfo(handle: Long): String
    @JvmStatic external fun nativeSetDeviceInfo(handle: Long, json: String): Int
    @JvmStatic external fun nativeDeviceRestart(handle: Long): Int
    @JvmStatic external fun nativeDeviceReset(handle: Long): Int
    @JvmStatic external fun nativeDeviceUpgrade(handle: Long, json: String): Int
    @JvmStatic external fun nativeGetTime(handle: Long): String
    @JvmStatic external fun nativeSetTime(handle: Long, json: String): Int
    @JvmStatic external fun nativeGetPort(handle: Long): String
    @JvmStatic external fun nativeSetPort(handle: Long, json: String): Int
    @JvmStatic external fun nativeGetNtpServers(handle: Long): String
    @JvmStatic external fun nativeGetNtpServer(handle: Long, id: Int): String
    @JvmStatic external fun nativeSetNtpServer(handle: Long, id: Int, json: String): Int
    @JvmStatic external fun nativeGetNetworks(handle: Long): String
    @JvmStatic external fun nativeGetNetwork(handle: Long, id: Int): String
    @JvmStatic external fun nativeSetNetwork(handle: Long, id: Int, json: String): Int
    @JvmStatic external fun nativeGetFtp(handle: Long): String
    @JvmStatic external fun nativeSetFtp(handle: Long, json: String): Int
    @JvmStatic external fun nativeTestFtp(handle: Long, json: String): Int
    @JvmStatic external fun nativeGetWebhook(handle: Long): String?
    @JvmStatic external fun nativeSetWebhook(handle: Long, json: String): Int
    @JvmStatic external fun nativeGetThermalTempAlarmInterval(handle: Long): String?
    @JvmStatic external fun nativeSetThermalTempAlarmInterval(handle: Long, json: String): Int

    // User management
    @JvmStatic external fun nativeGetUsers(handle: Long): String
    @JvmStatic external fun nativeGetUser(handle: Long, id: Int): String
    @JvmStatic external fun nativeUpdateUser(handle: Long, json: String): Int
    @JvmStatic external fun nativeDeleteUser(handle: Long, id: Int): Int

    // Streaming
    @JvmStatic external fun nativeGetStreams(handle: Long): String
    @JvmStatic external fun nativeGetStream(handle: Long, streamId: Int): String
    @JvmStatic external fun nativeGetStreamStatus(handle: Long, streamId: Int): String
    @JvmStatic external fun nativeStartStream(handle: Long, streamId: Int, streamType: Int): Int
    @JvmStatic external fun nativeStopStream(handle: Long): Int
    @JvmStatic external fun nativeStartStreamPlay(handle: Long, streamId: Int, streamType: Int): Long
    @JvmStatic external fun nativeStopStreamPlay(streamHandle: Long): Int
    @JvmStatic external fun nativeStartStreamRecord(streamHandle: Long, localFile: String): Int
    @JvmStatic external fun nativeStopStreamRecord(streamHandle: Long): Int

    // Display
    @JvmStatic external fun nativeGetChannels(handle: Long): String
    @JvmStatic external fun nativeGetChannel(handle: Long, channelId: Int): String
    @JvmStatic external fun nativeSetChannel(handle: Long, channelId: Int, json: String): Int
    @JvmStatic external fun nativeGetOverlay(handle: Long, channelId: Int): String
    @JvmStatic external fun nativeSetOverlay(handle: Long, channelId: Int, json: String): Int
    @JvmStatic external fun nativeGetImageParams(handle: Long, channelId: Int): String
    @JvmStatic external fun nativeSetImageParams(handle: Long, channelId: Int, json: String): Int
    @JvmStatic external fun nativeGetImageFlip(handle: Long, channelId: Int): Int
    @JvmStatic external fun nativeSetImageFlip(handle: Long, channelId: Int, mode: Int): Int

    // Thermal
    @JvmStatic external fun nativeGetThermalLensInfo(handle: Long): String
    @JvmStatic external fun nativeThermalCalibrate(handle: Long): Int
    @JvmStatic external fun nativeGetThermalShutterMode(handle: Long): Int
    @JvmStatic external fun nativeSetThermalShutterMode(handle: Long, mode: Int): Int
    @JvmStatic external fun nativeGetThermalShutterTime(handle: Long): Int
    @JvmStatic external fun nativeSetThermalShutterTime(handle: Long, interval: Int): Int
    @JvmStatic external fun nativeGetThermalResolution(handle: Long): String
    @JvmStatic external fun nativeGetThermalRange(handle: Long): Int
    @JvmStatic external fun nativeSetThermalRange(handle: Long, rangeId: Int): Int
    @JvmStatic external fun nativeGetThermalParams(handle: Long): String
    @JvmStatic external fun nativeSetThermalParams(handle: Long, json: String): Int
    @JvmStatic external fun nativeGetThermalColorDist(handle: Long): Int
    @JvmStatic external fun nativeSetThermalColorDist(handle: Long, type: Int): Int
    @JvmStatic external fun nativeGetThermalGain(handle: Long): String
    @JvmStatic external fun nativeSetThermalGain(handle: Long, json: String): Int
    @JvmStatic external fun nativeGetThermalPresetPalettes(handle: Long): String
    @JvmStatic external fun nativeGetThermalPalette(handle: Long): String
    @JvmStatic external fun nativeSetThermalPalette(handle: Long, json: String): Int
    @JvmStatic external fun nativeGetThermalOverlay(handle: Long): String
    @JvmStatic external fun nativeSetThermalOverlay(handle: Long, json: String): Int
    @JvmStatic external fun nativeGetThermalUnit(handle: Long): String
    @JvmStatic external fun nativeSetThermalUnit(handle: Long, json: String): Int
    @JvmStatic external fun nativeGetThermalCapture(handle: Long, mode: Int, localFile: String): String
    @JvmStatic external fun nativeGetThermalMarkers(handle: Long): String
    @JvmStatic external fun nativeSetThermalMarkers(handle: Long, json: String): Int
    @JvmStatic external fun nativeGetThermalAlarms(handle: Long): String
    @JvmStatic external fun nativeSetThermalAlarms(handle: Long, json: String): Int
    @JvmStatic external fun nativeSetThermalDpc(handle: Long, json: String): Int
    @JvmStatic external fun nativeGetThermalSr(handle: Long): Int
    @JvmStatic external fun nativeSetThermalSr(handle: Long, mode: Int): Int
    @JvmStatic external fun nativeGetThermalData(handle: Long, dataTypes: Int): String
    @JvmStatic external fun nativeStartThermalDataStream(handle: Long, configJson: String): Long
    @JvmStatic external fun nativeStopThermalDataStream(thermalDataHandle: Long): Int

    // Alarm trigger / listener
    @JvmStatic external fun nativeGetAlarmTrigger(handle: Long, type: Int): String
    @JvmStatic external fun nativeSetAlarmTrigger(handle: Long, type: Int, json: String): Int
    @JvmStatic external fun nativeStartAlarmListener(handle: Long): Long
    @JvmStatic external fun nativeStopAlarmListener(listenerHandle: Long): Int

    // Exception callback
    @JvmStatic external fun nativeSetExceptionCallback(handle: Long, enable: Boolean): Int

    private var streamFrameListener: StreamFrameListener? = null
    private var exceptionListener: ExceptionListener? = null
    private var thermalDataListener: ThermalDataListener? = null
    private var alarmListener: AlarmListener? = null

    fun setStreamFrameListener(listener: StreamFrameListener?) { streamFrameListener = listener }
    fun setExceptionListener(listener: ExceptionListener?) { exceptionListener = listener }
    fun setThermalDataListener(listener: ThermalDataListener?) { thermalDataListener = listener }
    fun setAlarmListener(listener: AlarmListener?) { alarmListener = listener }

    @JvmStatic
    fun onNativeStreamFrame(
        handle: Long, streamId: Int, streamType: Int,
        width: Int, height: Int, data: ByteArray,
        timestampUs: Long, keyFrame: Boolean
    ) {
        streamFrameListener?.onStreamFrame(handle, streamId, streamType, width, height, data, timestampUs, keyFrame)
    }

    @JvmStatic
    fun onNativeException(handle: Long, exceptionType: Int, streamId: Int, message: String) {
        exceptionListener?.onException(handle, exceptionType, streamId, message)
    }

    @JvmStatic
    fun onNativeThermalData(handle: Long, json: String) {
        thermalDataListener?.onThermalData(handle, json)
    }

    @JvmStatic
    fun onNativeEvent(handle: Long, eventType: Int, code: Int, json: String) {
        alarmListener?.onAlarm(handle, eventType, json)
    }

    @JvmStatic
    fun onNativeAlarm(listenerHandle: Long, alarmType: Int, json: String) {
        alarmListener?.onAlarm(listenerHandle, alarmType, json)
    }

    interface StreamFrameListener {
        fun onStreamFrame(
            handle: Long, streamId: Int, streamType: Int,
            width: Int, height: Int, data: ByteArray,
            timestampUs: Long, keyFrame: Boolean
        )
    }

    interface ExceptionListener {
        fun onException(handle: Long, exceptionType: Int, streamId: Int, message: String)
    }

    interface ThermalDataListener {
        fun onThermalData(handle: Long, json: String)
    }

    interface AlarmListener {
        fun onAlarm(listenerHandle: Long, alarmType: Int, json: String)
    }
}
