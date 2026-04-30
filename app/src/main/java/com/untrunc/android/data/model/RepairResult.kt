package com.untrunc.android.data.model

data class RepairResult(
    val success: Boolean,
    val error: String? = null,
    val outputSize: Long = 0
)
