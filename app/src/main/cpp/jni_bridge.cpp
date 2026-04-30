#include <jni.h>
#include <android/log.h>
#include <string>
#include <unistd.h>
#include "jni_bridge.h"
#include "untrunc_core/mp4.h"
#include "untrunc_core/common.h"
#include "untrunc_core/file.h"

#define LOG_TAG "untrunc"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static JavaVM* g_jvm = nullptr;
static volatile bool g_cancelled_jni = false;

// Store callback info
struct JniCallbackInfo {
    jobject callback;
    jmethodID onProgressMethod;
    jmethodID onStatusMethod;
};
static JniCallbackInfo g_cb_info = {};

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

// Helper: get JNIEnv for current thread
static JNIEnv* getEnv() {
    JNIEnv* env = nullptr;
    g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    return env;
}

// Progress callback that routes to Java
static void nativeProgressCallback(int percentage) {
    JNIEnv* env = getEnv();
    if (env && g_cb_info.callback) {
        env->CallVoidMethod(g_cb_info.callback, g_cb_info.onProgressMethod, percentage);
    }
}

static void nativeStatusCallback(const std::string& msg) {
    JNIEnv* env = getEnv();
    if (env && g_cb_info.callback) {
        jstring jmsg = env->NewStringUTF(msg.c_str());
        env->CallVoidMethod(g_cb_info.callback, g_cb_info.onStatusMethod, jmsg);
        env->DeleteLocalRef(jmsg);
    }
}

// Setup callback from Java object
static void setupCallback(JNIEnv* env, jobject callback) {
    if (g_cb_info.callback) {
        env->DeleteGlobalRef(g_cb_info.callback);
    }
    g_cb_info.callback = env->NewGlobalRef(callback);
    jclass clazz = env->GetObjectClass(callback);
    g_cb_info.onProgressMethod = env->GetMethodID(clazz, "onProgress", "(I)V");
    g_cb_info.onStatusMethod = env->GetMethodID(clazz, "onStatus", "(Ljava/lang/String;)V");
    env->DeleteLocalRef(clazz);
}

static void cleanupCallback(JNIEnv* env) {
    if (g_cb_info.callback) {
        env->DeleteGlobalRef(g_cb_info.callback);
        g_cb_info.callback = nullptr;
    }
}

JNIEXPORT jstring JNICALL
Java_com_untrunc_android_data_native_UntruncEngine_repairVideo(
    JNIEnv* env, jobject thiz,
    jint referenceFd, jint brokenFd, jint outputFd,
    jstring configJson, jobject callback)
{
    resetGlobalState();
    g_is_gui = true;  // Use exception mode
    g_cancelled = false;

    setupCallback(env, callback);
    g_onProgress = nativeProgressCallback;
    g_onStatus = nativeStatusCallback;

    // Get cache path from config
    const char* configStr = env->GetStringUTFChars(configJson, nullptr);
    std::string cachePath;
    std::string config(configStr);
    env->ReleaseStringUTFChars(configJson, configStr);

    // Parse cachePath from JSON (simple extraction)
    auto pos = config.find("\"cachePath\":\"");
    if (pos != std::string::npos) {
        auto start = pos + 13;
        auto end = config.find("\"", start);
        if (end != std::string::npos) cachePath = config.substr(start, end - start);
    }

    std::string refCachePath;

    try {
        LOGI("Starting repair...");
        LOGI("Reference fd=%d, broken fd=%d, output fd=%d", (int)referenceFd, (int)brokenFd, (int)outputFd);

        // Copy reference fd to cache so ffmpeg can open it by path
        if (!cachePath.empty()) {
            refCachePath = cachePath + "/ref_tmp";
            LOGI("Copying reference fd to cache: %s", refCachePath.c_str());
            FILE* src = fdopen(dup(referenceFd), "rb");
            FILE* dst = fopen(refCachePath.c_str(), "wb");
            if (src && dst) {
                char buf[65536];
                size_t n;
                while ((n = fread(buf, 1, sizeof(buf), src)) > 0) fwrite(buf, 1, n, dst);
                fclose(dst);
                fclose(src);
                LOGI("Reference copied to cache, using path for ffmpeg");
            } else {
                if (src) fclose(src);
                if (dst) fclose(dst);
                LOGE("Failed to copy reference to cache");
                refCachePath.clear();
            }
        }

        // Parse reference file
        Mp4 mp4;
        g_mp4 = &mp4;

        FileRead refFile(referenceFd, "reference");
        // Set the cache path so ffmpeg can open it
        if (!refCachePath.empty()) {
            refFile.filename_ = refCachePath;
        }
        LOGI("Reference file opened, size=%lld, filename=%s", (long long)refFile.length(), refFile.filename_.c_str());
        mp4.parseOk(refFile);

        LOGI("Reference parsed, starting repair...");

        // Open broken file and repair with FD-based overloads
        FileRead brokenFile(brokenFd, "broken");
        mp4.repair(brokenFile, outputFd);

        cleanupCallback(env);
        g_mp4 = nullptr;
        if (!refCachePath.empty()) remove(refCachePath.c_str());
        return env->NewStringUTF("{\"success\":true}");

    } catch (const std::exception& e) {
        LOGE("Repair failed: %s", e.what());
        cleanupCallback(env);
        g_mp4 = nullptr;
        if (!refCachePath.empty()) remove(refCachePath.c_str());
        std::string result = "{\"success\":false,\"error\":\"" + std::string(e.what()) + "\"}";
        return env->NewStringUTF(result.c_str());
    } catch (const std::string& e) {
        LOGE("Repair failed: %s", e.c_str());
        cleanupCallback(env);
        g_mp4 = nullptr;
        if (!refCachePath.empty()) remove(refCachePath.c_str());
        std::string result = "{\"success\":false,\"error\":\"" + e + "\"}";
        return env->NewStringUTF(result.c_str());
    } catch (const char* e) {
        LOGE("Repair failed: %s", e);
        cleanupCallback(env);
        g_mp4 = nullptr;
        if (!refCachePath.empty()) remove(refCachePath.c_str());
        std::string result = std::string("{\"success\":false,\"error\":\"") + e + "\"}";
        return env->NewStringUTF(result.c_str());
    }
}

JNIEXPORT jstring JNICALL
Java_com_untrunc_android_data_native_UntruncEngine_analyzeFile(
    JNIEnv* env, jobject thiz, jint fd)
{
    try {
        // Basic file analysis - detect format, codecs, duration
        // For now return a placeholder
        return env->NewStringUTF("{\"format\":\"mp4\",\"analyzed\":true}");
    } catch (...) {
        return env->NewStringUTF("{\"error\":\"analysis failed\"}");
    }
}

JNIEXPORT void JNICALL
Java_com_untrunc_android_data_native_UntruncEngine_cancel(
    JNIEnv* env, jobject thiz)
{
    g_cancelled = true;
    LOGI("Repair cancelled by user");
}

} // extern "C"
