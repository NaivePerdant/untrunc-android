package com.untrunc.android.data.`native`

object UntruncEngine {
    init {
        System.loadLibrary("untrunc_android")
    }

    external fun repairVideo(
        referenceFd: Int,
        brokenFd: Int,
        outputFd: Int,
        configJson: String,
        callback: NativeCallback
    ): String

    external fun analyzeFile(fd: Int): String

    external fun cancel()

    interface NativeCallback {
        fun onProgress(percentage: Int)
        fun onStatus(message: String)
    }
}
