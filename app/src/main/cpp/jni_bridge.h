#ifndef JNI_BRIDGE_H
#define JNI_BRIDGE_H

#include <jni.h>

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved);

JNIEXPORT jstring JNICALL
Java_com_untrunc_android_data_native_UntruncEngine_repairVideo(
    JNIEnv* env, jobject thiz,
    jint referenceFd, jint brokenFd, jint outputFd,
    jstring configJson, jobject callback);

JNIEXPORT jstring JNICALL
Java_com_untrunc_android_data_native_UntruncEngine_analyzeFile(
    JNIEnv* env, jobject thiz, jint fd);

JNIEXPORT void JNICALL
Java_com_untrunc_android_data_native_UntruncEngine_cancel(
    JNIEnv* env, jobject thiz);

} // extern "C"

#endif // JNI_BRIDGE_H
