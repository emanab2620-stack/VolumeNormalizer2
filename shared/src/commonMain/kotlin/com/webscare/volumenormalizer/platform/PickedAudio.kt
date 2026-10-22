package com.webscare.volumenormalizer.platform



/** An audio file chosen by the user. [uri] is a durable reference, not a copy of the audio. */
data class PickedAudio(
    val uri: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long?,
    val durationMs: Long
)

enum class PickFailure { NotAudio, Unreadable }

data class RejectedAudio(
    val displayName: String,
    val reason: PickFailure
)

data class PickResult(
    val accepted: List<PickedAudio>,
    val rejected: List<RejectedAudio>
)