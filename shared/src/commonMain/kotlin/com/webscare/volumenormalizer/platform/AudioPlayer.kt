package com.webscare.volumenormalizer.platform

import androidx.compose.runtime.Composable


interface AudioPlayer {
    val isPlaying: Boolean
    val positionMs: Long
    val durationMs: Long
    val hasError: Boolean

    fun togglePlayPause()
    fun pause()
    fun seekTo(positionMs: Long)
    fun skipBy(deltaMs: Long)
}

/** Creates a player for the file at [uri]. [durationMs] is the length we already know. */
@Composable
expect fun rememberAudioPlayer(uri: String, durationMs: Long): AudioPlayer