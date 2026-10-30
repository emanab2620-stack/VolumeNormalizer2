package com.webscare.volumenormalizer.platform

import android.content.Context
import android.content.ContextWrapper
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

private class AndroidAudioPlayer(
    context: Context,
    uri: Uri,
    initialDurationMs: Long
) : AudioPlayer {

    override var isPlaying by mutableStateOf(false)
        private set
    override var positionMs by mutableStateOf(0L)
        private set
    override var durationMs by mutableStateOf(initialDurationMs)
        private set
    override var hasError by mutableStateOf(false)
        private set

    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    private var focusRequest: AudioFocusRequest? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()


    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        ) {
            pause()
        }
    }

    init {
        try {
            val player = MediaPlayer()
            mediaPlayer = player
            player.setAudioAttributes(attributes)
            player.setDataSource(context, uri)
            player.setOnPreparedListener { ready ->
                isPrepared = true
                if (ready.duration > 0) durationMs = ready.duration.toLong()
            }
            player.setOnCompletionListener { finished ->
                isPlaying = false
                positionMs = 0L
                finished.seekTo(0)
                abandonFocus()
            }
            player.setOnErrorListener { _, _, _ ->
                hasError = true
                isPlaying = false
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            hasError = true
        }
    }

    override fun togglePlayPause() {
        if (isPlaying) pause() else play()
    }

    private fun play() {
        val player = mediaPlayer ?: return
        if (!isPrepared || hasError) return
        if (!requestFocus()) return
        try {
            player.start()
            isPlaying = true
        } catch (e: IllegalStateException) {
            hasError = true
        }
    }

    override fun pause() {
        val player = mediaPlayer
        if (player != null && isPrepared && isPlaying) {
            try {
                player.pause()
                positionMs = player.currentPosition.toLong()
            } catch (e: IllegalStateException) {
                // player was already released
            }
        }
        isPlaying = false
        abandonFocus()
    }

    override fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        if (!isPrepared) return
        val target = positionMs.coerceIn(0L, durationMs)
        try {
            player.seekTo(target.toInt())
            this.positionMs = target
        } catch (e: IllegalStateException) {
            // player was already released
        }
    }

    override fun skipBy(deltaMs: Long) {
        seekTo(positionMs + deltaMs)
    }


    fun updatePosition() {
        val player = mediaPlayer ?: return
        if (!isPrepared || !isPlaying) return
        try {
            positionMs = player.currentPosition.toLong()
        } catch (e: IllegalStateException) {
            // player was already released
        }
    }

    fun release() {
        isPlaying = false
        abandonFocus()
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
    }

    private fun requestFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            requestFocusLegacy()
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    @Suppress("DEPRECATION")
    private fun requestFocusLegacy(): Int =
        audioManager.requestAudioFocus(
            focusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            abandonFocusLegacy()
        }
    }

    @Suppress("DEPRECATION")
    private fun abandonFocusLegacy() {
        audioManager.abandonAudioFocus(focusListener)
    }
}

private fun Context.findActivity(): ComponentActivity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}

@Composable
actual fun rememberAudioPlayer(uri: String, durationMs: Long): AudioPlayer {
    val context = LocalContext.current
    val player = remember(uri) { AndroidAudioPlayer(context, Uri.parse(uri), durationMs) }

    // Stop and free the player when the screen closes, and pause when the app goes to background
    DisposableEffect(player) {
        val lifecycle = context.findActivity()?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
            player.release()
        }
    }


    LaunchedEffect(player) {
        while (true) {
            player.updatePosition()
            delay(250)
        }
    }

    return player
}