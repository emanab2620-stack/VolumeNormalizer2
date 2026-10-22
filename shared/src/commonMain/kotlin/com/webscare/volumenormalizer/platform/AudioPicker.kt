package com.webscare.volumenormalizer.platform



import androidx.compose.runtime.Composable

/** Opens the platform's audio file picker. Each platform provides its own implementation. */
class AudioPicker(private val onLaunch: () -> Unit) {
    fun launch() = onLaunch()
}

/**
 * [onResult] is called only after the user picked something.
 * If the user cancels the picker, nothing is called.
 */
@Composable
expect fun rememberAudioPicker(
    allowMultiple: Boolean,
    onResult: (PickResult) -> Unit
): AudioPicker