package com.webscare.volumenormalizer

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val TAPS = 16
private const val SKIP_RATIO = 0.3

private fun windowedSinc(t: Double): Double {
    val halfWidth = 8.0
    if (abs(t) >= halfWidth) return 0.0
    val sinc = if (abs(t) < 1e-12) 1.0 else sin(PI * t) / (PI * t)
    val window = 0.42 + 0.5 * cos(PI * t / halfWidth) + 0.08 * cos(2.0 * PI * t / halfWidth)
    return sinc * window
}

/** Weights to estimate the signal at 1/4, 2/4 and 3/4 of the way between two samples. */
private val INTERPOLATION_COEFFS: Array<DoubleArray> = Array(3) { phase ->
    val fraction = (phase + 1) / 4.0
    val raw = DoubleArray(TAPS) { j -> windowedSinc((j - 7) - fraction) }
    val total = raw.sum()
    DoubleArray(TAPS) { j -> raw[j] / total }
}

/**
 * Measures the sample peak and an estimate of the true peak (4x oversampling).
 * Audio is fed in small pieces with [process].
 */
class PeakMeter(private val channels: Int) {

    /** Largest sample seen, as a linear value (1.0 = full scale). */
    var samplePeak = 0.0
        private set

    /** Largest value seen between samples too, as a linear value. */
    var truePeak = 0.0
        private set

    // Each channel keeps its last 16 samples. Every sample is stored twice so that the
    // 16 newest samples are always one straight run, starting at [position].
    private val history = Array(channels) { DoubleArray(2 * TAPS) }
    private var position = 0
    private var framesSeen = 0

    fun process(interleaved: FloatArray, count: Int) {
        var index = 0
        val frames = count / channels
        for (frame in 0 until frames) {
            for (channel in 0 until channels) {
                val x = interleaved[index++].toDouble()
                val magnitude = abs(x)
                if (magnitude > samplePeak) samplePeak = magnitude
                if (magnitude > truePeak) truePeak = magnitude
                val h = history[channel]
                h[position] = x
                h[position + TAPS] = x
            }
            position = (position + 1) % TAPS
            framesSeen++
            if (framesSeen >= TAPS) checkBetweenSamples()
        }
    }

    private fun checkBetweenSamples() {
        for (channel in 0 until channels) {
            val h = history[channel]
            // Between the two middle samples. Skip when both are far below the current peak.
            val nearby = maxOf(abs(h[position + 7]), abs(h[position + 8]))
            if (nearby < truePeak * SKIP_RATIO) continue

            for (phase in 0 until 3) {
                val coeffs = INTERPOLATION_COEFFS[phase]
                var sum = 0.0
                for (j in 0 until TAPS) sum += h[position + j] * coeffs[j]
                val magnitude = abs(sum)
                if (magnitude > truePeak) truePeak = magnitude
            }
        }
    }
}