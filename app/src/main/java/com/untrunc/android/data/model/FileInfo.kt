package com.untrunc.android.data.model

import android.net.Uri

data class FileInfo(
    val uri: Uri,
    val name: String,
    val size: Long,
    val format: String = "unknown",
    val mimeType: String = ""
)
