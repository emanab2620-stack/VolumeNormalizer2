package com.webscare.volumenormalizer.ui.normalize



data class NormalizeSettings(
    val preset: NormalizePreset = NormalizePreset.Podcast,
    val targetLufs: Int = NormalizePreset.Podcast.defaultTargetLufs ?: -16,
    val peakProtection: Boolean = true
) {
    fun withPreset(selected: NormalizePreset): NormalizeSettings =
        copy(preset = selected, targetLufs = selected.defaultTargetLufs ?: targetLufs)

    fun withTarget(newTarget: Int): NormalizeSettings =
        if (newTarget == targetLufs) this else copy(targetLufs = newTarget, preset = NormalizePreset.Custom)
}