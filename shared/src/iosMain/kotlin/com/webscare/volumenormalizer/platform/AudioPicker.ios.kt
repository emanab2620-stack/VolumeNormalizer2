package com.webscare.volumenormalizer.platform



import androidx.compose.runtime.Composable

@Composable
actual fun rememberAudioPicker(
    allowMultiple: Boolean,
    onResult: (PickResult) -> Unit
): AudioPicker {
    // TODO iOS: document picker comes in the iOS step
    return AudioPicker(onLaunch = { })
}