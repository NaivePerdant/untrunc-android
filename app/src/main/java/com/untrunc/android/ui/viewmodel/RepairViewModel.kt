package com.untrunc.android.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.untrunc.android.data.model.FileInfo
import com.untrunc.android.data.model.RepairProgress
import com.untrunc.android.data.`native`.UntruncEngine
import com.untrunc.android.data.repository.RepairRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RepairUiState(
    val referenceFile: FileInfo? = null,
    val brokenFile: FileInfo? = null,
    val outputUri: Uri? = null,
    val progress: Int = 0,
    val statusMessage: String = "",
    val isRepairing: Boolean = false,
    val isComplete: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

class RepairViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepairRepository(application)

    private val _uiState = MutableStateFlow(RepairUiState())
    val uiState: StateFlow<RepairUiState> = _uiState.asStateFlow()

    fun setReferenceFile(uri: Uri) {
        val info = getFileInfo(uri)
        _uiState.value = _uiState.value.copy(referenceFile = info)
    }

    fun setBrokenFile(uri: Uri) {
        val info = getFileInfo(uri)
        _uiState.value = _uiState.value.copy(brokenFile = info)
    }

    fun setOutputUri(uri: Uri) {
        _uiState.value = _uiState.value.copy(outputUri = uri)
    }

    fun startRepair() {
        val state = _uiState.value
        val refUri = state.referenceFile?.uri ?: return
        val brokenUri = state.brokenFile?.uri ?: return
        val outputUri = state.outputUri ?: return

        _uiState.value = state.copy(isRepairing = true, progress = 0, error = null, isComplete = false)

        viewModelScope.launch {
            repository.repairVideo(refUri, brokenUri, outputUri).collect { progress ->
                when (progress) {
                    is RepairProgress.InProgress -> {
                        _uiState.value = _uiState.value.copy(progress = progress.percentage)
                    }
                    is RepairProgress.Status -> {
                        _uiState.value = _uiState.value.copy(statusMessage = progress.message)
                    }
                    is RepairProgress.Complete -> {
                        _uiState.value = _uiState.value.copy(
                            isRepairing = false,
                            isComplete = true,
                            success = progress.result.success,
                            error = progress.result.error
                        )
                    }
                    is RepairProgress.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isRepairing = false,
                            isComplete = true,
                            success = false,
                            error = progress.message
                        )
                    }
                }
            }
        }
    }

    fun cancelRepair() {
        UntruncEngine.cancel()
        _uiState.value = _uiState.value.copy(isRepairing = false, error = "Cancelled")
    }

    fun reset() {
        _uiState.value = RepairUiState()
    }

    private fun getFileInfo(uri: Uri): FileInfo {
        val context = getApplication<Application>()
        var name = "unknown"
        var size = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = cursor.getString(nameIdx)
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }

        val mimeType = context.contentResolver.getType(uri) ?: ""
        val format = when {
            name.endsWith(".mp4", true) || name.endsWith(".m4v", true) -> "MP4"
            name.endsWith(".m4a", true) -> "M4A"
            name.endsWith(".mov", true) -> "MOV"
            name.endsWith(".3gp", true) -> "3GP"
            else -> mimeType.substringAfterLast("/", "unknown").uppercase()
        }

        return FileInfo(uri = uri, name = name, size = size, format = format, mimeType = mimeType)
    }
}
