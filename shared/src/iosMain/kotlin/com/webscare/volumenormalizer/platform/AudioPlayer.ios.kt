package com.webscare.volumenormalizer.platform


import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private class UnsupportedAudioPlayer(
    override val durationMs: Long
) : AudioPlayer {
    override val isPlaying: Boolean = false
    override val positionMs: Long = 0L
    override val hasError: Boolean = true

    override fun togglePlayPause() {}
    override fun pause() {}
    override fun seekTo(positionMs: Long) {}
    override fun skipBy(deltaMs: Long) {}
}

@Composable
actual fun rememberAudioPlayer(uri: String, durationMs: Long): AudioPlayer =
    remember(uri) { UnsupportedAudioPlayer(durationMs) }