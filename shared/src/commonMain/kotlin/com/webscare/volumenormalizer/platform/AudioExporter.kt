package com.webscare.volumenormalizer.platform

import androidx.compose.runtime.Composable

sealed interface ExportResult {
    data object Success : ExportResult
    data object Cancelled : ExportResult
    data class Failure(val message: String) : ExportResult
}

class AudioExporter internal constructor(
    private val onLaunch: (String) -> Unit
) {
    fun save(fileName: String) {
        onLaunch(fileName)
    }
}

@Composable
expect fun rememberAudioExporter(
    sourceFilePath: String,
    onResult: (ExportResult) -> Unit
): AudioExporter
