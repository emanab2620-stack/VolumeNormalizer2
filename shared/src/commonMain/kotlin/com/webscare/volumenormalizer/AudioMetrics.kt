package com.webscare.volumenormalizer


import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt

/** What we measured in a file. A value is null when it could not be measured. */
data class AudioMetrics(
    val integratedLufs: Double?,
    val samplePeakDbfs: Double?,
    val truePeakDbtp: Double?,
    val sampleRateHz: Int,
    val channels: Int
)

/** The state of the measurement of one file. */
sealed interface AnalysisState {
    /** [progress] is 0..1, or null when it cannot be known. */
    data class Analyzing(val progress: Float?) : AnalysisState

    data class Done(val metrics: AudioMetrics) : AnalysisState

    data object Failed : AnalysisState
}

/** Linear amplitude (1.0 = full scale) to decibels. Null for silence. */
fun amplitudeToDb(amplitude: Double): Double? =
    if (amplitude <= 0.0) null else 20.0 * log10(amplitude)

/** Example: -20.84 becomes "-20.8" */
fun formatLufs(value: Double): String {
    val tenths = (value * 10.0).roundToInt()
    val sign = if (tenths < 0) "-" else ""
    val magnitude = abs(tenths)
    return "$sign${magnitude / 10}.${magnitude % 10}"
}