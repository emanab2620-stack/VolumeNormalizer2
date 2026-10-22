package com.webscare.volumenormalizer.platform

import androidx.compose.runtime.Composable

/** Gets the decoded audio, one small piece at a time. */
interface PcmSink {
    /** Called once, before the first piece of audio. */
    fun onFormat(sampleRate: Int, channels: Int)

    /** [interleaved]: left, right, left, right... as numbers from -1.0 to 1.0. [count] values are valid. */
    fun onSamples(interleaved: FloatArray, count: Int)
}

enum class DecodeOutcome { Success, Unsupported, Unreadable }

/** Opens an audio file and turns it into raw audio numbers. Each platform provides its own. */
interface AudioDecoder {
    suspend fun decode(
        uri: String,
        sink: PcmSink,
        onProgress: (Float) -> Unit
    ): DecodeOutcome
}

@Composable
expect fun rememberAudioDecoder(): AudioDecoder