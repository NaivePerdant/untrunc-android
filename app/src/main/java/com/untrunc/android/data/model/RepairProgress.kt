package com.untrunc.android.data.model

sealed class RepairProgress {
    data class InProgress(val percentage: Int) : RepairProgress()
    data class Status(val message: String) : RepairProgress()
    data class Complete(val result: RepairResult) : RepairProgress()
    data class Error(val message: String) : RepairProgress()
}
