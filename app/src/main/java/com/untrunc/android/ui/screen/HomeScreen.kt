package com.untrunc.android.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.untrunc.android.ui.viewmodel.RepairViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: RepairViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val refPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.setReferenceFile(it)
        }
    }

    val brokenPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.setBrokenFile(it)
        }
    }

    val outputPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        uri?.let { viewModel.setOutputUri(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Untrunc") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reference file card
            FileSelectionCard(
                title = "Reference File (healthy)",
                fileName = uiState.referenceFile?.name,
                fileSize = uiState.referenceFile?.size,
                format = uiState.referenceFile?.format,
                onClick = { refPicker.launch(arrayOf("video/*", "audio/*")) }
            )

            // Broken file card
            FileSelectionCard(
                title = "Broken File",
                fileName = uiState.brokenFile?.name,
                fileSize = uiState.brokenFile?.size,
                format = uiState.brokenFile?.format,
                onClick = { brokenPicker.launch(arrayOf("video/*", "audio/*")) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Repair button
            if (!uiState.isRepairing) {
                Button(
                    onClick = {
                        if (uiState.outputUri == null) {
                            val brokenName = uiState.brokenFile?.name ?: "repaired"
                            val ext = brokenName.substringAfterLast(".", "mp4")
                            val baseName = brokenName.substringBeforeLast(".")
                            outputPicker.launch("${baseName}_fixed.$ext")
                        } else {
                            viewModel.startRepair()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.referenceFile != null && uiState.brokenFile != null
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (uiState.outputUri == null) "Choose Output & Repair" else "Start Repair"
                    )
                }
            }

            // Auto-start repair after output is selected
            LaunchedEffect(uiState.outputUri) {
                if (uiState.outputUri != null && !uiState.isRepairing && !uiState.isComplete
                    && uiState.referenceFile != null && uiState.brokenFile != null
                ) {
                    viewModel.startRepair()
                }
            }

            // Progress
            if (uiState.isRepairing) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = uiState.progress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${uiState.progress}%")
                    if (uiState.statusMessage.isNotEmpty()) {
                        Text(uiState.statusMessage, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.cancelRepair() }) {
                        Text("Cancel")
                    }
                }
            }

            // Result
            if (uiState.isComplete) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.success)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (uiState.success) "Repair Successful!" else "Repair Failed",
                            style = MaterialTheme.typography.titleMedium
                        )
                        uiState.error?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Repair Another File")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSelectionCard(
    title: String,
    fileName: String?,
    fileSize: Long?,
    format: String?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (fileName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(fileName, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    format?.let {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(it) }
                        )
                    }
                    fileSize?.let {
                        Text(
                            formatFileSize(it),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Tap to select file",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}
