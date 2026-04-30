package com.untrunc.android.data.repository

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.untrunc.android.data.model.RepairProgress
import com.untrunc.android.data.model.RepairResult
import com.untrunc.android.data.`native`.UntruncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

class RepairRepository(private val context: Context) {

    fun repairVideo(
        referenceUri: Uri,
        brokenUri: Uri,
        outputUri: Uri,
        config: String = "{}"
    ): Flow<RepairProgress> = callbackFlow {
        withContext(Dispatchers.IO) {
            var refPfd: ParcelFileDescriptor? = null
            var brokenPfd: ParcelFileDescriptor? = null
            var outputPfd: ParcelFileDescriptor? = null

            try {
                refPfd = context.contentResolver.openFileDescriptor(referenceUri, "r")
                    ?: throw Exception("Cannot open reference file")
                brokenPfd = context.contentResolver.openFileDescriptor(brokenUri, "r")
                    ?: throw Exception("Cannot open broken file")
                outputPfd = context.contentResolver.openFileDescriptor(outputUri, "w")
                    ?: throw Exception("Cannot create output file")

                val callback = object : UntruncEngine.NativeCallback {
                    override fun onProgress(percentage: Int) {
                        trySend(RepairProgress.InProgress(percentage))
                    }
                    override fun onStatus(message: String) {
                        trySend(RepairProgress.Status(message))
                    }
                }

                val resultJson = UntruncEngine.repairVideo(
                    refPfd.fd,
                    brokenPfd.fd,
                    outputPfd.fd,
                    "{\"cachePath\":\"${context.cacheDir.absolutePath}\"}",
                    callback
                )

                val json = JSONObject(resultJson)
                val success = json.optBoolean("success", false)
                val error: String? = if (json.has("error")) json.getString("error") else null

                trySend(
                    RepairProgress.Complete(
                        RepairResult(success = success, error = error)
                    )
                )
            } catch (e: Exception) {
                trySend(RepairProgress.Error(e.message ?: "Unknown error"))
            } finally {
                refPfd?.close()
                brokenPfd?.close()
                outputPfd?.close()
            }
        }

        awaitClose { UntruncEngine.cancel() }
    }
}
