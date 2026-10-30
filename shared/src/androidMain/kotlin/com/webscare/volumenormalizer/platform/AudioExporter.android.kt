
package com.webscare.volumenormalizer.platform

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

@Composable
actual fun rememberAudioExporter(
    sourceFilePath: String,
    onResult: (ExportResult) -> Unit
): AudioExporter {
    val context = LocalContext.current
    val latestOnResult by rememberUpdatedState(onResult)
    val latestSourcePath by rememberUpdatedState(sourceFilePath)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/wav")
    ) { destination: Uri? ->
        if (destination == null) {
            latestOnResult(ExportResult.Cancelled)
        } else {
            val sourcePath = latestSourcePath

            CoroutineScope(Dispatchers.Main).launch {
                val result = withContext(Dispatchers.IO) {
                    copyWavToDestination(
                        context = context,
                        sourcePath = sourcePath,
                        destination = destination
                    )
                }
                latestOnResult(result)
            }
        }
    }

    return remember(launcher) {
        AudioExporter { requestedName ->
            launcher.launch(normalizeWavFileName(requestedName))
        }
    }
}

private fun normalizeWavFileName(name: String): String {
    val cleaned = name.trim()
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .trimEnd('.', ' ')

    val baseName = cleaned.ifBlank { "Normalized Audio" }

    return if (baseName.endsWith(".wav", ignoreCase = true)) {
        baseName
    } else {
        "$baseName.wav"
    }
}

private fun copyWavToDestination(
    context: Context,
    sourcePath: String,
    destination: Uri
): ExportResult {
    val source = File(sourcePath)

    if (!source.exists() || !source.isFile) {
        return ExportResult.Failure(
            "The processed audio file could not be found."
        )
    }

    return try {
        val output = context.contentResolver.openOutputStream(destination, "w")
            ?: return ExportResult.Failure(
                "Could not open the selected save location."
            )

        output.use { destinationStream ->
            source.inputStream().use { sourceStream ->
                sourceStream.copyTo(destinationStream)
            }
        }

        ExportResult.Success
    } catch (e: IOException) {
        ExportResult.Failure(
            e.message ?: "The audio file could not be saved."
        )
    } catch (e: SecurityException) {
        ExportResult.Failure(
            "Permission to save the file was denied."
        )
    }
}
