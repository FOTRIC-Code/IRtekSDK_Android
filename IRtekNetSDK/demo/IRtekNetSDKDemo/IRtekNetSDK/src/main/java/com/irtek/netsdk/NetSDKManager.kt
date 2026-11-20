package com.irtek.netsdk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class NetSDKResult<T>(
    val code: Int,
    val data: T? = null,
    val message: String = ""
) {
    /** 厂商约定：接口返回值 >= 0 表示成功 */
    val isSuccess: Boolean get() = code >= 0
}

object NetSDKManager {
    private var handle: Long = 0L

    private fun parseObj(json: String?): NetSDKResult<JSONObject> {
        if (json == null) return NetSDKResult(-1, message = "Native returned null")
        return NetSDKResult(0, JSONObject(json))
    }

    private fun parseArr(json: String?): NetSDKResult<JSONArray> {
        if (json == null) return NetSDKResult(-1, message = "Native returned null")
        return NetSDKResult(0, JSONArray(json))
    }

    fun init(): Int = NativeSDK.nativeInit()

    fun cleanup(): Int = NativeSDK.nativeCleanup()

    fun enableLog(enable: Boolean): Int = NativeSDK.nativeEnableLog(enable)

    suspend fun login(
        ip: String,
        port: Int,
        username: String,
        password: String,
        timeoutMs: Int = 5000
    ): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val h = NativeSDK.nativeLogin(ip, port, username, password, timeoutMs)
            if (h != 0L) {
                handle = h
                NetSDKResult(0, Unit)
            } else {
                NetSDKResult(-1, message = "Login failed")
            }
        } catch (e: Exception) {
            NetSDKResult(-1, message = e.message ?: "Login failed")
        }
    }

    suspend fun logout(): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeLogout(handle)
            if (code >= 0) {
                handle = 0L
                NetSDKResult(0, Unit)
            } else {
                NetSDKResult(code, message = "Error code: $code")
            }
        } catch (e: Exception) {
            NetSDKResult(-1, message = e.message ?: "Logout failed")
        }
    }

    fun getSdkVersion(): NetSDKResult<JSONObject> {
        return try {
            parseObj(NativeSDK.nativeGetSdkVersion())
        } catch (e: Exception) {
            NetSDKResult(-1, message = e.message ?: "Failed")
        }
    }

    suspend fun searchDevices(timeoutMs: Int = 3000): NetSDKResult<JSONArray> = withContext(Dispatchers.IO) {
        try {
            parseArr(NativeSDK.nativeSearchDevices(timeoutMs))
        } catch (e: Exception) {
            NetSDKResult(-1, message = e.message ?: "Failed")
        }
    }

    suspend fun getDeviceInfo(): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetDeviceInfo(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setDeviceInfo(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetDeviceInfo(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun deviceRestart(): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeDeviceRestart(handle)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun deviceReset(): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeDeviceReset(handle)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun deviceUpgrade(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeDeviceUpgrade(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getTime(): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetTime(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setTime(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetTime(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getPort(): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetPort(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setPort(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetPort(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getNtpServers(): NetSDKResult<JSONArray> = withContext(Dispatchers.IO) {
        try { parseArr(NativeSDK.nativeGetNtpServers(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getNtpServer(id: Int): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetNtpServer(handle, id)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setNtpServer(id: Int, json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetNtpServer(handle, id, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getNetworks(): NetSDKResult<JSONArray> = withContext(Dispatchers.IO) {
        try { parseArr(NativeSDK.nativeGetNetworks(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getNetwork(id: Int): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetNetwork(handle, id)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setNetwork(id: Int, json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetNetwork(handle, id, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getFtp(): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetFtp(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setFtp(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetFtp(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun testFtp(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeTestFtp(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getWebhook(): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetWebhook(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setWebhook(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetWebhook(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalTempAlarmInterval(): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetThermalTempAlarmInterval(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setThermalTempAlarmInterval(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetThermalTempAlarmInterval(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getUsers(): NetSDKResult<JSONArray> = withContext(Dispatchers.IO) {
        try { parseArr(NativeSDK.nativeGetUsers(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getUser(id: Int): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetUser(handle, id)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun updateUser(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeUpdateUser(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun deleteUser(id: Int): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeDeleteUser(handle, id)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getStreams(): NetSDKResult<JSONArray> = withContext(Dispatchers.IO) {
        try { parseArr(NativeSDK.nativeGetStreams(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getStream(streamId: Int): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetStream(handle, streamId)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getStreamStatus(streamId: Int): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetStreamStatus(handle, streamId)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    /** @deprecated Use [startStream] which returns stream handle. */
    suspend fun startStreamLegacy(streamId: Int, streamType: Int = 4): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeStartStream(handle, streamId, streamType)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun startStream(streamId: Int, streamType: Int = 4): NetSDKResult<Long> = withContext(Dispatchers.IO) {
        try {
            val streamHandle = NativeSDK.nativeStartStreamPlay(handle, streamId, streamType)
            if (streamHandle >= 0) NetSDKResult(0, streamHandle)
            else NetSDKResult(streamHandle.toInt(), message = "Error code: $streamHandle")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun stopStream(streamHandle: Long): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeStopStreamPlay(streamHandle)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun startStreamRecord(streamHandle: Long, path: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeStartStreamRecord(streamHandle, path)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun stopStreamRecord(streamHandle: Long): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeStopStreamRecord(streamHandle)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getChannels(): NetSDKResult<JSONArray> = withContext(Dispatchers.IO) {
        try { parseArr(NativeSDK.nativeGetChannels(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getChannel(channelId: Int): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetChannel(handle, channelId)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setChannel(channelId: Int, json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetChannel(handle, channelId, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getOverlay(channelId: Int): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetOverlay(handle, channelId)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setOverlay(channelId: Int, json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetOverlay(handle, channelId, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getImageParams(channelId: Int): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetImageParams(handle, channelId)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setImageParams(channelId: Int, json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetImageParams(handle, channelId, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getImageFlip(channelId: Int): NetSDKResult<Int> = withContext(Dispatchers.IO) {
        try {
            val value = NativeSDK.nativeGetImageFlip(handle, channelId)
            if (value >= 0) NetSDKResult(0, value) else NetSDKResult(value, message = "Error code: $value")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setImageFlip(channelId: Int, mode: Int): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetImageFlip(handle, channelId, mode)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalLensInfo(): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetThermalLensInfo(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun thermalCalibrate(): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeThermalCalibrate(handle)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalShutterMode(): NetSDKResult<Int> = withContext(Dispatchers.IO) {
        try {
            val value = NativeSDK.nativeGetThermalShutterMode(handle)
            if (value >= 0) NetSDKResult(0, value) else NetSDKResult(value, message = "Error code: $value")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setThermalShutterMode(mode: Int): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetThermalShutterMode(handle, mode)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalShutterTime(): NetSDKResult<Int> = withContext(Dispatchers.IO) {
        try {
            val value = NativeSDK.nativeGetThermalShutterTime(handle)
            if (value >= 0) NetSDKResult(0, value) else NetSDKResult(value, message = "Error code: $value")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setThermalShutterTime(interval: Int): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetThermalShutterTime(handle, interval)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalResolution(): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetThermalResolution(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalRange(): NetSDKResult<Int> = withContext(Dispatchers.IO) {
        try {
            val value = NativeSDK.nativeGetThermalRange(handle)
            if (value >= 0) NetSDKResult(0, value) else NetSDKResult(value, message = "Error code: $value")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setThermalRange(rangeId: Int): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetThermalRange(handle, rangeId)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalParams(): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetThermalParams(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setThermalParams(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetThermalParams(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalColorDist(): NetSDKResult<Int> = withContext(Dispatchers.IO) {
        try {
            val value = NativeSDK.nativeGetThermalColorDist(handle)
            if (value >= 0) NetSDKResult(0, value) else NetSDKResult(value, message = "Error code: $value")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setThermalColorDist(type: Int): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetThermalColorDist(handle, type)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalGain(): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetThermalGain(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setThermalGain(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetThermalGain(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalPresetPalettes(): NetSDKResult<JSONArray> = withContext(Dispatchers.IO) {
        try { parseArr(NativeSDK.nativeGetThermalPresetPalettes(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalPalette(): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetThermalPalette(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setThermalPalette(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetThermalPalette(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalOverlay(): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetThermalOverlay(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setThermalOverlay(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetThermalOverlay(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalUnit(): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetThermalUnit(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setThermalUnit(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetThermalUnit(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalCapture(mode: Int, localFile: String): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetThermalCapture(handle, mode, localFile)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalMarkers(): NetSDKResult<JSONArray> = withContext(Dispatchers.IO) {
        try { parseArr(NativeSDK.nativeGetThermalMarkers(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setThermalMarkers(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetThermalMarkers(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalAlarms(): NetSDKResult<JSONArray> = withContext(Dispatchers.IO) {
        try { parseArr(NativeSDK.nativeGetThermalAlarms(handle)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setThermalAlarms(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetThermalAlarms(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setThermalDpc(json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetThermalDpc(handle, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalSr(): NetSDKResult<Int> = withContext(Dispatchers.IO) {
        try {
            val value = NativeSDK.nativeGetThermalSr(handle)
            if (value >= 0) NetSDKResult(0, value) else NetSDKResult(value, message = "Error code: $value")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setThermalSr(mode: Int): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetThermalSr(handle, mode)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getThermalData(dataTypes: Int): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetThermalData(handle, dataTypes)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun startThermalDataStream(configJson: String): NetSDKResult<Long> = withContext(Dispatchers.IO) {
        try {
            val h = NativeSDK.nativeStartThermalDataStream(handle, configJson)
            if (h >= 0) NetSDKResult(0, h) else NetSDKResult(h.toInt(), message = "Error code: $h")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun stopThermalDataStream(thermalDataHandle: Long): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeStopThermalDataStream(thermalDataHandle)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun getAlarmTrigger(type: Int): NetSDKResult<JSONObject> = withContext(Dispatchers.IO) {
        try { parseObj(NativeSDK.nativeGetAlarmTrigger(handle, type)) }
        catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun setAlarmTrigger(type: Int, json: String): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeSetAlarmTrigger(handle, type, json)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun startAlarmListener(): NetSDKResult<Long> = withContext(Dispatchers.IO) {
        try {
            val listenerHandle = NativeSDK.nativeStartAlarmListener(handle)
            if (listenerHandle >= 0) NetSDKResult(0, listenerHandle)
            else NetSDKResult(listenerHandle.toInt(), message = "Error code: $listenerHandle")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    suspend fun stopAlarmListener(listenerHandle: Long): NetSDKResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = NativeSDK.nativeStopAlarmListener(listenerHandle)
            if (code >= 0) NetSDKResult(0, Unit) else NetSDKResult(code, message = "Error code: $code")
        } catch (e: Exception) { NetSDKResult(-1, message = e.message ?: "Failed") }
    }

    fun setStreamFrameListener(listener: NativeSDK.StreamFrameListener?) {
        NativeSDK.setStreamFrameListener(listener)
    }

    fun setExceptionListener(listener: NativeSDK.ExceptionListener?) {
        NativeSDK.setExceptionListener(listener)
    }

    fun enableExceptionCallback(enable: Boolean): Int {
        return try {
            NativeSDK.nativeSetExceptionCallback(handle, enable)
        } catch (e: Exception) {
            -1
        }
    }

    fun setThermalDataListener(listener: NativeSDK.ThermalDataListener?) {
        NativeSDK.setThermalDataListener(listener)
    }

    fun setAlarmListener(listener: NativeSDK.AlarmListener?) {
        NativeSDK.setAlarmListener(listener)
    }
}
