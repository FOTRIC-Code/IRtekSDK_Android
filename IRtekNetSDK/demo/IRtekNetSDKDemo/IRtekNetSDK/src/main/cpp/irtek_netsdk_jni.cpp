#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <climits>
#include <cstdint>
#include <cstring>
#include <limits>
#include <mutex>
#include <sstream>
#include <string>
#include "IRtekNetSDK.h"

#define LOG_TAG "IRtekNetSDK-JNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static JavaVM* g_jvm = nullptr;
static jclass g_native_class = nullptr;
static jmethodID g_on_stream_frame_method = nullptr;
static jmethodID g_on_exception_method = nullptr;
static jmethodID g_on_thermal_data_method = nullptr;
static jmethodID g_on_alarm_method = nullptr;

static std::mutex g_stream_mutex;
static NET_HANDLE g_play_stream_handle = nullptr;

static std::mutex g_alarm_mutex;
static NET_HANDLE g_alarm_listener_handle = nullptr;

static std::mutex g_thermal_mutex;
static NET_HANDLE g_thermal_data_handle = nullptr;

static NET_HANDLE g_exception_device_handle = nullptr;

static std::string bytes_to_string(const BYTE* arr, size_t max_len) {
    size_t len = strnlen(reinterpret_cast<const char*>(arr), max_len);
    return {reinterpret_cast<const char*>(arr), len};
}

static void string_to_bytes(const std::string& str, BYTE* arr, size_t max_len) {
    std::memset(arr, 0, max_len);
    size_t n = std::min(str.size(), max_len - 1);
    std::memcpy(arr, str.c_str(), n);
}

static std::pair<JNIEnv*, bool> get_env() {
    JNIEnv* env = nullptr;
    bool attached = false;
    int status = g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (status == JNI_EDETACHED) {
        if (g_jvm->AttachCurrentThread(&env, nullptr) == JNI_OK) attached = true;
        else return {nullptr, false};
    } else if (status != JNI_OK) {
        return {nullptr, false};
    }
    return {env, attached};
}

static std::string jstring_to_string(JNIEnv* env, jstring jstr) {
    if (!jstr) return {};
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

static jstring string_to_jstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

static std::string json_escape(const std::string& s) {
    std::ostringstream oss;
    for (char c : s) {
        switch (c) {
            case '"': oss << "\\\""; break;
            case '\\': oss << "\\\\"; break;
            case '\n': oss << "\\n"; break;
            case '\r': oss << "\\r"; break;
            case '\t': oss << "\\t"; break;
            default: oss << c; break;
        }
    }
    return oss.str();
}

static bool ensure_native_callbacks(JNIEnv* env) {
    if (g_native_class) return true;
    jclass cls = env->FindClass("com/irtek/netsdk/NativeSDK");
    if (!cls) {
        LOGE("NativeSDK class not found");
        return false;
    }
    g_native_class = static_cast<jclass>(env->NewGlobalRef(cls));
    env->DeleteLocalRef(cls);

    g_on_stream_frame_method = env->GetStaticMethodID(
        g_native_class, "onNativeStreamFrame", "(JIIII[BJZ)V");
    g_on_exception_method = env->GetStaticMethodID(
        g_native_class, "onNativeException", "(JIILjava/lang/String;)V");
    g_on_thermal_data_method = env->GetStaticMethodID(
        g_native_class, "onNativeThermalData", "(JLjava/lang/String;)V");
    g_on_alarm_method = env->GetStaticMethodID(
        g_native_class, "onNativeAlarm", "(JILjava/lang/String;)V");
    return g_native_class != nullptr;
}

static std::string temp_point_info_to_json(const net_temp_point_info& info) {
    std::ostringstream oss;
    oss << "{\"max\":{\"temperature\":" << info.max.temperature
        << ",\"point\":{\"x\":" << info.max.point.x << ",\"y\":" << info.max.point.y << "}}"
        << ",\"min\":{\"temperature\":" << info.min.temperature
        << ",\"point\":{\"x\":" << info.min.point.x << ",\"y\":" << info.min.point.y << "}}"
        << ",\"avg_temp\":" << info.avg_temp << "}";
    return oss.str();
}

static std::string marker_alarm_info_to_json(const net_marker_alarm_info& info) {
    std::ostringstream oss;
    oss << "{\"marker_name\":\"" << json_escape(bytes_to_string(info.marker_name, sizeof(info.marker_name)))
        << "\",\"alarm_level\":" << static_cast<int>(info.alarm_level) << "}";
    return oss.str();
}

static std::string marker_temp_info_to_json(const net_marker_temp_info& info) {
    std::ostringstream oss;
    oss << "{\"marker_id\":" << info.marker_id
        << ",\"type\":" << static_cast<int>(info.type)
        << ",\"marker_name\":\"" << json_escape(bytes_to_string(info.marker_name, sizeof(info.marker_name)))
        << "\",\"temp_point_info\":" << temp_point_info_to_json(info.temp_point_info) << "}";
    return oss.str();
}

static std::string raw_frame_to_json(const net_raw_frame& frame) {
    std::ostringstream oss;
    oss << "{\"width\":" << frame.width
        << ",\"height\":" << frame.height
        << ",\"range_id\":" << frame.range_id
        << ",\"points_count\":" << frame.points_count
        << ",\"lut_len\":" << frame.lut_len
        << ",\"lut_env_temp\":" << frame.lut_env_temp
        << ",\"sensor_temp\":" << frame.sensor_temp
        << ",\"shutter_state\":" << frame.shutter_state
        << ",\"frame_no\":" << frame.frame_no
        << ",\"pts\":" << frame.pts
        << ",\"raw_type\":" << frame.raw_type
        << ",\"shutter_adv\":" << frame.shutter_adv << "}";
    return oss.str();
}

static std::string thermal_data_to_json(const net_thermal_data& d) {
    std::ostringstream oss;
    oss << "{\"data_types\":" << static_cast<int>(d.data_types)
        << ",\"timestamp_us\":" << d.timestamp_us
        << ",\"raw_frame\":" << raw_frame_to_json(d.raw_frame)
        << ",\"global_temp_info\":" << temp_point_info_to_json(d.global_temp_info)
        << ",\"markers_count\":" << d.markers_count
        << ",\"marker_temps\":[";
    for (USHORT i = 0; i < d.markers_count && i < NET_MAX_MARKERS; ++i) {
        if (i > 0) oss << ',';
        oss << marker_temp_info_to_json(d.marker_temps[i]);
    }
    oss << "]}";
    return oss.str();
}

static std::string alarm_device_info_to_json(const net_alarm_device_info& info) {
    std::ostringstream oss;
    oss << "{\"ip\":\"" << json_escape(bytes_to_string(info.ip, sizeof(info.ip)))
        << "\",\"port_no\":" << info.port_no
        << ",\"channel_id\":" << info.channel_id << "}";
    return oss.str();
}

static std::string thermal_temp_alarm_info_to_json(const net_alarm_thermal_temp_info& info) {
    std::ostringstream oss;
    oss << "{\"timestamp_us\":" << info.timestamp_us
        << ",\"dev_info\":" << alarm_device_info_to_json(info.dev_info)
        << ",\"raw_frame\":" << raw_frame_to_json(info.raw_frame)
        << ",\"global_temp_info\":" << temp_point_info_to_json(info.global_temp_info)
        << ",\"marker_alarms_count\":" << info.marker_alarms_count
        << ",\"markers_count\":" << info.markers_count
        << ",\"marker_alarms\":[";
    for (USHORT i = 0; i < info.marker_alarms_count && i < NET_MAX_MARKERS; ++i) {
        if (i > 0) oss << ',';
        oss << marker_alarm_info_to_json(info.marker_alarms[i]);
    }
    oss << "],\"marker_temps\":[";
    for (USHORT i = 0; i < info.markers_count && i < NET_MAX_MARKERS; ++i) {
        if (i > 0) oss << ',';
        oss << marker_temp_info_to_json(info.marker_temps[i]);
    }
    oss << "],\"message\":\"";
    if (info.message != nullptr && info.message_length > 0) {
        oss << json_escape(std::string(reinterpret_cast<const char*>(info.message), info.message_length));
    }
    oss << "\"}";
    return oss.str();
}

static void IRTEK_NETSDK_CALL native_stream_callback(const net_stream_data* data) {
    if (!g_jvm || !data || !data->data) return;
    auto [env, attached] = get_env();
    if (!env || !ensure_native_callbacks(env)) {
        if (attached) g_jvm->DetachCurrentThread();
        return;
    }
    jbyteArray arr = env->NewByteArray(static_cast<jsize>(data->data_length));
    if (!arr) {
        if (attached) g_jvm->DetachCurrentThread();
        return;
    }
    env->SetByteArrayRegion(arr, 0, static_cast<jsize>(data->data_length),
                            reinterpret_cast<const jbyte*>(data->data));
    jlong handle = 0;
    {
        std::lock_guard<std::mutex> lock(g_stream_mutex);
        handle = static_cast<jlong>(reinterpret_cast<intptr_t>(g_play_stream_handle));
    }
    env->CallStaticVoidMethod(
        g_native_class, g_on_stream_frame_method,
        handle,
        static_cast<jint>(data->stream_id),
        static_cast<jint>(data->stream_type),
        static_cast<jint>(data->width),
        static_cast<jint>(data->height),
        arr,
        static_cast<jlong>(data->timestamp_us),
        static_cast<jboolean>(data->key_frame));
    env->DeleteLocalRef(arr);
    if (attached) g_jvm->DetachCurrentThread();
}

static void IRTEK_NETSDK_CALL native_exception_callback(const net_exception_info* info) {
    if (!g_jvm || !info) return;
    auto [env, attached] = get_env();
    if (!env || !ensure_native_callbacks(env)) {
        if (attached) g_jvm->DetachCurrentThread();
        return;
    }
    jlong handle = static_cast<jlong>(reinterpret_cast<intptr_t>(g_exception_device_handle));
    jstring msg = string_to_jstring(env, bytes_to_string(info->message, sizeof(info->message)));
    env->CallStaticVoidMethod(
        g_native_class, g_on_exception_method,
        handle,
        static_cast<jint>(info->exception_type),
        static_cast<jint>(info->stream_id),
        msg);
    if (msg) env->DeleteLocalRef(msg);
    if (attached) g_jvm->DetachCurrentThread();
}

static void IRTEK_NETSDK_CALL native_thermal_data_callback(const net_thermal_data* frame) {
    if (!g_jvm || !frame) return;
    auto [env, attached] = get_env();
    if (!env || !ensure_native_callbacks(env)) {
        if (attached) g_jvm->DetachCurrentThread();
        return;
    }
    jlong handle = 0;
    {
        std::lock_guard<std::mutex> lock(g_thermal_mutex);
        handle = static_cast<jlong>(reinterpret_cast<intptr_t>(g_thermal_data_handle));
    }
    jstring json = string_to_jstring(env, thermal_data_to_json(*frame));
    env->CallStaticVoidMethod(g_native_class, g_on_thermal_data_method, handle, json);
    if (json) env->DeleteLocalRef(json);
    if (attached) g_jvm->DetachCurrentThread();
}

static void IRTEK_NETSDK_CALL native_alarm_callback(const net_alarm_info* info) {
    if (!g_jvm || !info) return;
    auto [env, attached] = get_env();
    if (!env || !ensure_native_callbacks(env)) {
        if (attached) g_jvm->DetachCurrentThread();
        return;
    }
    std::ostringstream payload;
    payload << "{\"type\":" << static_cast<int>(info->type);
    if (info->type == NET_ALARM_THERMAL_TEMP && info->alarm_info != nullptr &&
        info->alarm_info_length >= sizeof(net_alarm_thermal_temp_info)) {
        const auto* thermal = reinterpret_cast<const net_alarm_thermal_temp_info*>(info->alarm_info);
        payload << ",\"alarm_info\":" << thermal_temp_alarm_info_to_json(*thermal);
    }
    payload << "}";
    jlong listenerHandle = 0;
    {
        std::lock_guard<std::mutex> lock(g_alarm_mutex);
        listenerHandle = static_cast<jlong>(reinterpret_cast<intptr_t>(g_alarm_listener_handle));
    }
    jstring json = string_to_jstring(env, payload.str());
    env->CallStaticVoidMethod(g_native_class, g_on_alarm_method, listenerHandle,
                              static_cast<jint>(info->type), json);
    if (json) env->DeleteLocalRef(json);
    if (attached) g_jvm->DetachCurrentThread();
}

static void ftp_info_from_json(const std::string& json, net_ftp_info& info) {
    std::memset(&info, 0, sizeof(info));
    auto extract_string = [&](const char* key, BYTE* dest, size_t max_len) {
        const auto pos = json.find(key);
        if (pos == std::string::npos) return;
        const auto start = json.find('"', json.find(':', pos) + 1);
        const auto end = json.find('"', start + 1);
        if (start != std::string::npos && end != std::string::npos && end > start) {
            string_to_bytes(json.substr(start + 1, end - start - 1), dest, max_len);
        }
    };
    extract_string("\"ip\"", info.ip, sizeof(info.ip));
    extract_string("\"user_name\"", info.user_name, sizeof(info.user_name));
    extract_string("\"pwd\"", info.pwd, sizeof(info.pwd));
    extract_string("\"path_template\"", info.path_template, sizeof(info.path_template));
    const auto portPos = json.find("\"port_no\"");
    if (portPos != std::string::npos) {
        const auto colon = json.find(':', portPos);
        if (colon != std::string::npos) info.port_no = static_cast<USHORT>(std::stoi(json.substr(colon + 1)));
    }
    const auto sslPos = json.find("\"use_ssl\"");
    if (sslPos != std::string::npos) {
        const auto colon = json.find(':', sslPos);
        if (colon != std::string::npos) info.use_ssl = static_cast<BYTE>(std::stoi(json.substr(colon + 1)));
    }
}

extern "C" jint JNI_OnLoad(JavaVM* vm, void*) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

extern "C" void JNI_OnUnload(JavaVM* vm, void*) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_OK) {
        if (g_native_class) {
            env->DeleteGlobalRef(g_native_class);
            g_native_class = nullptr;
        }
    }
    g_jvm = nullptr;
}

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeGetWebhook(JNIEnv* env, jclass, jlong handle) {
    try {
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(handle));
        net_webhook_info info{};
        if (net_get_webhook(h, &info) != NET_EC_OK) return nullptr;
        std::ostringstream oss;
        oss << "{\"url\":\"" << json_escape(bytes_to_string(info.url, sizeof(info.url))) << "\"}";
        return string_to_jstring(env, oss.str());
    } catch (...) { LOGE("nativeGetWebhook"); return nullptr; }
}

JNIEXPORT jint JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeSetWebhook(JNIEnv* env, jclass, jlong handle, jstring jsonStr) {
    try {
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(handle));
        net_webhook_info info{};
        std::string json = jstring_to_string(env, jsonStr);
        const auto pos = json.find("\"url\"");
        if (pos != std::string::npos) {
            const auto start = json.find('"', json.find(':', pos) + 1);
            const auto end = json.find('"', start + 1);
            if (start != std::string::npos && end != std::string::npos && end > start) {
                string_to_bytes(json.substr(start + 1, end - start - 1), info.url, sizeof(info.url));
            }
        }
        return net_set_webhook(h, &info);
    } catch (...) { LOGE("nativeSetWebhook"); return NET_EC_FAIL; }
}

JNIEXPORT jstring JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeGetThermalTempAlarmInterval(JNIEnv* env, jclass, jlong handle) {
    try {
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(handle));
        net_thermal_temp_alarm_interval interval{};
        if (net_get_thermal_temp_alarm_interval(h, &interval) != NET_EC_OK) return nullptr;
        std::ostringstream oss;
        oss << "{\"interval\":" << interval.interval << "}";
        return string_to_jstring(env, oss.str());
    } catch (...) { LOGE("nativeGetThermalTempAlarmInterval"); return nullptr; }
}

JNIEXPORT jint JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeSetThermalTempAlarmInterval(JNIEnv* env, jclass, jlong handle, jstring jsonStr) {
    try {
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(handle));
        net_thermal_temp_alarm_interval interval{};
        std::string json = jstring_to_string(env, jsonStr);
        const auto pos = json.find("\"interval\"");
        if (pos != std::string::npos) {
            const auto colon = json.find(':', pos);
            if (colon != std::string::npos) interval.interval = static_cast<USHORT>(std::stoi(json.substr(colon + 1)));
        }
        return net_set_thermal_temp_alarm_interval(h, &interval);
    } catch (...) { LOGE("nativeSetThermalTempAlarmInterval"); return NET_EC_FAIL; }
}

JNIEXPORT jlong JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeStartAlarmListener(JNIEnv* /*env*/, jclass, jlong handle) {
    try {
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(handle));
        std::lock_guard<std::mutex> lock(g_alarm_mutex);
        if (g_alarm_listener_handle != nullptr) {
            net_stop_alarm_listener(g_alarm_listener_handle);
            g_alarm_listener_handle = nullptr;
        }
        INT32 ret = net_start_alarm_listener(h, native_alarm_callback);
        if (ret <= 0) return static_cast<jlong>(ret);
        g_alarm_listener_handle = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(ret));
        return static_cast<jlong>(ret);
    } catch (...) { LOGE("nativeStartAlarmListener"); return NET_EC_FAIL; }
}

JNIEXPORT jint JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeStopAlarmListener(JNIEnv* /*env*/, jclass, jlong listenerHandle) {
    try {
        std::lock_guard<std::mutex> lock(g_alarm_mutex);
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(listenerHandle));
        INT32 ret = net_stop_alarm_listener(h);
        if (ret == NET_EC_OK && g_alarm_listener_handle == h) g_alarm_listener_handle = nullptr;
        return ret;
    } catch (...) { LOGE("nativeStopAlarmListener"); return NET_EC_FAIL; }
}

JNIEXPORT jint JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeDeviceUpgrade(JNIEnv* env, jclass, jlong handle, jstring jsonStr) {
    try {
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(handle));
        net_upgrade_info info{};
        std::string json = jstring_to_string(env, jsonStr);
        const auto pos = json.find("\"path\"");
        if (pos != std::string::npos) {
            const auto start = json.find('"', json.find(':', pos) + 1);
            const auto end = json.find('"', start + 1);
            if (start != std::string::npos && end != std::string::npos && end > start) {
                string_to_bytes(json.substr(start + 1, end - start - 1), info.path, sizeof(info.path));
            }
        }
        return net_device_upgrade(h, &info);
    } catch (...) { LOGE("nativeDeviceUpgrade"); return NET_EC_FAIL; }
}

JNIEXPORT jint JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeTestFtp(JNIEnv* env, jclass, jlong handle, jstring jsonStr) {
    try {
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(handle));
        net_ftp_info info{};
        ftp_info_from_json(jstring_to_string(env, jsonStr), info);
        return net_test_ftp(h, &info);
    } catch (...) { LOGE("nativeTestFtp"); return NET_EC_FAIL; }
}

JNIEXPORT jlong JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeStartStreamPlay(JNIEnv* /*env*/, jclass,
        jlong deviceHandle, jint streamId, jint streamType) {
    try {
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(deviceHandle));
        net_stream_info stream{};
        if (net_get_stream(h, static_cast<UINT32>(streamId), &stream) != NET_EC_OK) return NET_EC_FAIL;
        INT32 ret = net_start_stream_play(h, &stream,
            static_cast<net_enum_stream_type>(streamType), native_stream_callback);
        if (ret <= 0) return static_cast<jlong>(ret);
        std::lock_guard<std::mutex> lock(g_stream_mutex);
        g_play_stream_handle = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(ret));
        return static_cast<jlong>(ret);
    } catch (...) { LOGE("nativeStartStreamPlay"); return NET_EC_FAIL; }
}

JNIEXPORT jint JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeStopStreamPlay(JNIEnv* /*env*/, jclass, jlong streamHandle) {
    try {
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(streamHandle));
        INT32 ret = net_stop_stream_play(h);
        if (ret == NET_EC_OK) {
            std::lock_guard<std::mutex> lock(g_stream_mutex);
            if (g_play_stream_handle == h) g_play_stream_handle = nullptr;
        }
        return ret;
    } catch (...) { LOGE("nativeStopStreamPlay"); return NET_EC_FAIL; }
}

JNIEXPORT jint JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeStartStreamRecord(JNIEnv* env, jclass,
        jlong streamHandle, jstring localFile) {
    try {
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(streamHandle));
        std::string path = jstring_to_string(env, localFile);
        return net_start_stream_record(h,
            reinterpret_cast<const BYTE*>(path.c_str()),
            static_cast<USHORT>(std::min(path.size(),
                static_cast<size_t>(std::numeric_limits<uint16_t>::max()))));
    } catch (...) { LOGE("nativeStartStreamRecord"); return NET_EC_FAIL; }
}

JNIEXPORT jint JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeStopStreamRecord(JNIEnv* /*env*/, jclass, jlong streamHandle) {
    try {
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(streamHandle));
        return net_stop_stream_record(h);
    } catch (...) { LOGE("nativeStopStreamRecord"); return NET_EC_FAIL; }
}

JNIEXPORT jint JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeSetExceptionCallback(JNIEnv* /*env*/, jclass,
        jlong handle, jboolean enable) {
    try {
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(handle));
        if (enable) {
            g_exception_device_handle = h;
            return net_set_exception_callback(h, native_exception_callback);
        }
        g_exception_device_handle = nullptr;
        return net_set_exception_callback(h, nullptr);
    } catch (...) { LOGE("nativeSetExceptionCallback"); return NET_EC_FAIL; }
}

JNIEXPORT jlong JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeStartThermalDataStream(JNIEnv* env, jclass,
        jlong deviceHandle, jstring configJson) {
    try {
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(deviceHandle));
        net_thermal_data_config config{};
        std::string json = jstring_to_string(env, configJson);
        const auto dtPos = json.find("\"data_types\"");
        if (dtPos != std::string::npos) {
            const auto colon = json.find(':', dtPos);
            if (colon != std::string::npos) {
                config.data_types = static_cast<net_enum_thermal_data_type>(std::stoi(json.substr(colon + 1)));
            }
        }
        const auto siPos = json.find("\"sample_interval\"");
        if (siPos != std::string::npos) {
            const auto colon = json.find(':', siPos);
            if (colon != std::string::npos) config.sample_interval = static_cast<UINT32>(std::stoi(json.substr(colon + 1)));
        }
        INT32 ret = net_start_thermal_data_stream(h, &config, native_thermal_data_callback);
        if (ret <= 0) return static_cast<jlong>(ret);
        std::lock_guard<std::mutex> lock(g_thermal_mutex);
        g_thermal_data_handle = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(ret));
        return static_cast<jlong>(ret);
    } catch (...) { LOGE("nativeStartThermalDataStream"); return NET_EC_FAIL; }
}

JNIEXPORT jint JNICALL
Java_com_irtek_netsdk_NativeSDK_nativeStopThermalDataStream(JNIEnv* /*env*/, jclass, jlong thermalDataHandle) {
    try {
        NET_HANDLE h = reinterpret_cast<NET_HANDLE>(static_cast<intptr_t>(thermalDataHandle));
        INT32 ret = net_stop_thermal_data_stream(h);
        if (ret == NET_EC_OK) {
            std::lock_guard<std::mutex> lock(g_thermal_mutex);
            if (g_thermal_data_handle == h) g_thermal_data_handle = nullptr;
        }
        return ret;
    } catch (...) { LOGE("nativeStopThermalDataStream"); return NET_EC_FAIL; }
}

} // extern "C"
