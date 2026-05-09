<div align="center">

# Untrunc for Android

### Repair broken/truncated audio and video files on Android

[![Version](https://img.shields.io/github/v/release/NaivePerdant/untrunc-android?color=blue&label=version)](https://github.com/NaivePerdant/untrunc-android/releases)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://github.com/NaivePerdant/untrunc-android/releases)
[![Built with](https://img.shields.io/badge/built%20with-FFmpeg-blue.svg)](https://ffmpeg.org/)
[![Downloads](https://img.shields.io/github/downloads/NaivePerdant/untrunc-android/total)](https://github.com/NaivePerdant/untrunc-android/releases/latest)
[![License](https://img.shields.io/badge/license-GPL%20v2+-red.svg)](LICENSE)

English | [中文](README_ZH.md)

</div>

---

Based on [untrunc](https://github.com/anthwlock/untrunc) by ponchio/anthwlock.

## What it does

When a recording is interrupted unexpectedly (crash, power loss, etc.), the resulting file is often unplayable because it's missing the `moov` atom (metadata). This app reconstructs the metadata using a healthy reference file recorded with the same device/settings.

## Supported formats

- **Audio**: M4A, AAC, MP3, WAV
- **Video**: MP4, MOV, 3GP

## How to use

1. Select a **reference file** — a healthy file recorded with the same device and settings
2. Select the **broken file** — the truncated/corrupted file you want to repair
3. Choose an output location
4. Wait for the repair to complete

## Building

### Prerequisites

- Android Studio (or Gradle CLI)
- Android NDK 27+
- CMake 3.22.1+

### Build FFmpeg (first time only)

```bash
./build_ffmpeg_android.sh [path_to_ndk]
```

This builds minimal FFmpeg shared libraries (libavformat, libavcodec, libavutil) for arm64-v8a and armeabi-v7a.

### Build APK

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Architecture

```
app/src/main/
├── java/com/untrunc/android/
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── native/UntruncEngine.kt    # JNI interface
│   │   ├── repository/RepairRepository.kt
│   │   └── model/
│   └── ui/
│       ├── screen/HomeScreen.kt        # Compose UI
│       └── viewmodel/RepairViewModel.kt
└── cpp/
    ├── jni_bridge.cpp                  # JNI ↔ C++ bridge
    └── untrunc_core/                   # Core repair engine (C++)
```

## License

- **untrunc core**: GPL v2+ (Copyright 2010 Federico Ponchio)
- **Android wrapper**: GPL v2+
- **FFmpeg**: LGPL v2.1+

## Credits

- [untrunc](https://github.com/anthwlock/untrunc) — the original repair engine
- [FFmpeg](https://ffmpeg.org/) — audio/video codec parsing
