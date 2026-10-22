package com.webscare.volumenormalizer.platform

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val AUDIO_MIME_TYPES = arrayOf("audio/*")
private val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "m4a", "aac", "flac")

@Composable
actual fun rememberAudioPicker(
    allowMultiple: Boolean,
    onResult: (PickResult) -> Unit
): AudioPicker {
    val context = LocalContext.current
    val latestOnResult by rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()

    fun handle(uris: List<Uri>) {
        scope.launch {
            val result = withContext(Dispatchers.IO) { readAudio(context, uris) }
            latestOnResult(result)
        }
    }

    val singleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) handle(listOf(uri))
    }
    val multiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) handle(uris)
    }

    return remember(allowMultiple, singleLauncher, multiLauncher) {
        AudioPicker(onLaunch = {
            if (allowMultiple) {
                multiLauncher.launch(AUDIO_MIME_TYPES)
            } else {
                singleLauncher.launch(AUDIO_MIME_TYPES)
            }
        })
    }
}

private class FileInfo(
    val name: String,
    val sizeBytes: Long?,
    val mimeType: String?
)

private fun readAudio(context: Context, uris: List<Uri>): PickResult {
    val accepted = mutableListOf<PickedAudio>()
    val rejected = mutableListOf<RejectedAudio>()

    for (uri in uris) {
        // Keep access to the file after the app restarts, when the provider allows it
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (ignored: Exception) {
            // Not every provider supports persistable access
        }

        val info = try {
            readInfo(context, uri)
        } catch (ignored: Exception) {
            null
        }
        if (info == null) {
            rejected += RejectedAudio(uri.lastPathSegment ?: "Audio", PickFailure.Unreadable)
            continue
        }

        val extension = info.name.substringAfterLast('.', "").lowercase()
        val looksLikeAudio = info.mimeType?.startsWith("audio/") == true ||
                extension in SUPPORTED_EXTENSIONS
        if (!looksLikeAudio) {
            rejected += RejectedAudio(info.name, PickFailure.NotAudio)
            continue
        }

        val durationMs = readDurationMs(context, uri)
        if (durationMs == null || durationMs <= 0L) {
            rejected += RejectedAudio(info.name, PickFailure.Unreadable)
            continue
        }

        accepted += PickedAudio(
            uri = uri.toString(),
            displayName = info.name,
            mimeType = info.mimeType,
            sizeBytes = info.sizeBytes,
            durationMs = durationMs
        )
    }

    return PickResult(accepted = accepted, rejected = rejected)
}

private fun readInfo(context: Context, uri: Uri): FileInfo {
    val resolver = context.contentResolver
    var name: String? = null
    var size: Long? = null

    resolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }

    return FileInfo(
        name = name ?: uri.lastPathSegment ?: "Audio",
        sizeBytes = size,
        mimeType = resolver.getType(uri)
    )
}

private fun readDurationMs(context: Context, uri: Uri): Long? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    } catch (ignored: Exception) {
        null
    } finally {
        try {
            retriever.release()
        } catch (ignored: Exception) {
            // nothing to do
        }
    }
}