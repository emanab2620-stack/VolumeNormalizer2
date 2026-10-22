package com.webscare.volumenormalizer.platform



import androidx.compose.runtime.Composable

private object UnsupportedAudioDecoder : AudioDecoder {
    override suspend fun decode(
        uri: String,
        sink: PcmSink,
        onProgress: (Float) -> Unit
    ): DecodeOutcome = DecodeOutcome.Unsupported
}

@Composable
actual fun rememberAudioDecoder(): AudioDecoder = UnsupportedAudioDecoder