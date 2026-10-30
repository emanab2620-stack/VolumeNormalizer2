package com.webscare.volumenormalizer

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

data class EqualizeDspParams(
    val gains: List<Double> // 5 bands: 60Hz, 230Hz, 910Hz, 4000Hz, 14000Hz
)

/**
 * Professional Biquad Peaking EQ Filter
 * - Gain limited to ±10dB for stability
 * - Q factor 1.4 (musical standard)
 * - NaN/Infinity safety checks
 */
class BiquadPeakingEQ(
    private val sampleRate: Int,
    private val freq: Double,
    private val q: Double,
    gainDb: Double
) {
    private val b0: Double
    private val b1: Double
    private val b2: Double
    private val a1: Double
    private val a2: Double

    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    init {
        // ✅ Gain ko ±10dB tak limit kiya (professional standard)
        val safeGain = gainDb.coerceIn(-10.0, 10.0)
        val a = 10.0.pow(safeGain / 40.0)
        val w0 = 2.0 * PI * freq / sampleRate
        val alpha = sin(w0) / (2.0 * q)

        val b0_raw = 1.0 + alpha * a
        val b1_raw = -2.0 * cos(w0)
        val b2_raw = 1.0 - alpha * a
        val a0_raw = 1.0 + alpha / a
        val a1_raw = -2.0 * cos(w0)
        val a2_raw = 1.0 - alpha / a

        val invA0 = 1.0 / a0_raw
        this.b0 = b0_raw * invA0
        this.b1 = b1_raw * invA0
        this.b2 = b2_raw * invA0
        this.a1 = a1_raw * invA0
        this.a2 = a2_raw * invA0
    }

    fun process(input: Double): Double {
        val y = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = input
        y2 = y1
        y1 = y

        // ✅ NaN ya Infinity ko 0 kar do
        return if (y.isNaN() || y.isInfinite()) 0.0 else y
    }

    fun reset() {
        x1 = 0.0; x2 = 0.0; y1 = 0.0; y2 = 0.0
    }
}

class EqualizeProcessor(
    private val sampleRate: Int,
    private val channels: Int,
    private val params: EqualizeDspParams
) {
    private val frequencies = listOf(60.0, 230.0, 910.0, 4000.0, 14000.0)
    private val qFactor = 1.4 // Musical standard, stable for all frequencies

    private val filters = List(channels) { ch ->
        List(5) { band ->
            BiquadPeakingEQ(sampleRate, frequencies[band], qFactor, params.gains[band])
        }
    }

    private var pcm = ShortArray(0)
    private var meterInput = FloatArray(0)

    private val loudnessMeter = LoudnessMeter(sampleRate, channels)
    private val peakMeter = PeakMeter(channels)
    var clippedSamples = 0L
        private set

    val pcmBuffer: ShortArray get() = pcm

    fun process(samples: FloatArray, count: Int): Int {
        ensureCapacity(count)
        var validCount = 0

        for (i in 0 until count step channels) {
            for (ch in 0 until channels) {
                val sampleIdx = i + ch
                var value = samples[sampleIdx].toDouble()

                // 5 bands ko cascade kiya
                for (band in 0 until 5) {
                    value = filters[ch][band].process(value)
                }

                // ✅ Final NaN/Infinity safety check
                if (value.isNaN() || value.isInfinite()) {
                    value = 0.0
                }

                // ✅ Soft clipping (headroom ke liye 0.99)
                val clamped = value.coerceIn(-0.99, 0.99)
                if (value > 0.99 || value < -0.99) clippedSamples++

                pcm[validCount] = (clamped * 32767.0).toInt().toShort()
                meterInput[validCount] = clamped.toFloat()
                validCount++
            }
        }

        loudnessMeter.process(meterInput, validCount)
        peakMeter.process(meterInput, validCount)

        return validCount
    }

    private fun ensureCapacity(count: Int) {
        if (pcm.size < count) {
            pcm = ShortArray(count)
            meterInput = FloatArray(count)
        }
    }

    fun metrics(): AudioMetrics = AudioMetrics(
        integratedLufs = loudnessMeter.integratedLufs(),
        samplePeakDbfs = amplitudeToDb(peakMeter.samplePeak),
        truePeakDbtp = amplitudeToDb(peakMeter.truePeak),
        sampleRateHz = sampleRate,
        channels = channels
    )
}