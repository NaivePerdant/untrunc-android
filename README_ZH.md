[English](README.md) | 中文

---

<div align="center">

# Untrunc for Android

[![Version](https://img.shields.io/github/v/release/NaivePerdant/untrunc-android?color=blue&label=version)](https://github.com/NaivePerdant/untrunc-android/releases)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://github.com/NaivePerdant/untrunc-android/releases)
[![Built with](https://img.shields.io/badge/built%20with-FFmpeg-blue.svg)](https://ffmpeg.org/)
[![Downloads](https://img.shields.io/github/downloads/NaivePerdant/untrunc-android/total)](https://github.com/NaivePerdant/untrunc-android/releases/latest)
[![License](https://img.shields.io/badge/license-GPL%20v2+-red.svg)](LICENSE)

一款修复损坏/截断音视频文件的 Android 应用。  
基于 ponchio/anthwlock 的 [untrunc](https://github.com/anthwlock/untrunc) 项目。

</div>

---

## 功能介绍

当录制过程意外中断（崩溃、断电等）时，生成的文件通常因缺少 `moov` 原子（元数据）而无法播放。本应用利用同一设备/相同设置录制的正常参考文件来重建元数据，从而修复损坏的文件。

## 支持格式

- **音频**: M4A、AAC、MP3、WAV
- **视频**: MP4、MOV、3GP

## 使用方法

1. 选择一个**参考文件** — 用相同设备和设置录制的正常文件
2. 选择**损坏文件** — 需要修复的截断/损坏文件
3. 选择输出位置
4. 等待修复完成

## 编译构建

### 环境要求

- Android Studio（或 Gradle CLI）
- Android NDK 27+
- CMake 3.22.1+

### 编译 FFmpeg（仅首次需要）

```bash
./build_ffmpeg_android.sh [NDK路径]
```

为 arm64-v8a 和 armeabi-v7a 架构编译最小化的 FFmpeg 共享库（libavformat、libavcodec、libavutil）。

### 编译 APK

```bash
./gradlew assembleDebug
```

输出路径: `app/build/outputs/apk/debug/app-debug.apk`

## 项目架构

```
app/src/main/
├── java/com/untrunc/android/
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── native/UntruncEngine.kt    # JNI 接口
│   │   ├── repository/RepairRepository.kt
│   │   └── model/
│   └── ui/
│       ├── screen/HomeScreen.kt        # Compose UI
│       └── viewmodel/RepairViewModel.kt
└── cpp/
    ├── jni_bridge.cpp                  # JNI ↔ C++ 桥接层
    └── untrunc_core/                   # 核心修复引擎 (C++)
```

## 许可证

- **untrunc 核心**: GPL v2+（Copyright 2010 Federico Ponchio）
- **Android 封装层**: GPL v2+
- **FFmpeg**: LGPL v2.1+

## 致谢

- [untrunc](https://github.com/anthwlock/untrunc) — 原始修复引擎
- [FFmpeg](https://ffmpeg.org/) — 音视频编解码解析
