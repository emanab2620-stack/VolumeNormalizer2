package com.webscare.volumenormalizer.platform

import kotlin.math.roundToInt

private val KNOWN_FORMATS = setOf("MP3", "WAV", "M4A", "AAC", "FLAC")

/** Short format name, for example "MP3". */
val PickedAudio.formatName: String
    get() = formatLabel(displayName, mimeType)

/** Example: "MP3 · 12.4 MB · 03:42" */
fun PickedAudio.detailsLine(): String {
    val parts = listOfNotNull(
        formatName,
        sizeBytes?.let { formatSize(it) },
        formatDuration(durationMs)
    )
    return parts.joinToString(" · ")
}

private fun formatLabel(displayName: String, mimeType: String?): String {
    val extension = displayName.substringAfterLast('.', "").uppercase()
    if (extension in KNOWN_FORMATS) return extension
    return mimeType?.substringAfter('/')?.uppercase() ?: "AUDIO"
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.pad2()}:${seconds.pad2()}"
    } else {
        "${minutes.pad2()}:${seconds.pad2()}"
    }
}

private fun Long.pad2(): String = toString().padStart(2, '0')

fun formatSize(sizeBytes: Long): String {
    val kb = sizeBytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "${oneDecimal(gb)} GB"
        mb >= 100.0 -> "${mb.roundToInt()} MB"
        mb >= 1.0 -> "${oneDecimal(mb)} MB"
        else -> "${kb.roundToInt()} KB"
    }
}

private fun oneDecimal(value: Double): String {
    val tenths = (value * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}