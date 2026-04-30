# Untrunc Android ProGuard Rules

# Keep JNI bridge
-keep class com.untrunc.android.data.native.UntruncEngine { *; }
-keep class com.untrunc.android.data.native.UntruncEngine$NativeCallback { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
