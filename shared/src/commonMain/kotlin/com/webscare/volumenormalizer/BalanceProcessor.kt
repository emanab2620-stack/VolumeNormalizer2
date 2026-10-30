package com.webscare.volumenormalizer

import com.webscare.volumenormalizer.ui.balance.BalanceProfile
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

data class BalanceSettings(
    val profile: BalanceProfile = BalanceProfile.Speech,
    val intensity: Float = 0.5f,
    val preservePeaks: Boolean = true,
    val naturalDynamics: Boolean = true
)

data class BalanceDspParams(
    val thresholdDb: Double,
    val ratio: Double,
    val attackMs: Double,
    val releaseMs: Double,
    val makeupGainDb: Double,
    val applyLimiter: Boolean
)

fun BalanceSettings.toDspParams(): BalanceDspParams {
    val baseThreshold: Double
    val baseRatio: Double
    val baseAttack: Double
    val baseRelease: Double

    when (profile) {
        BalanceProfile.Speech -> {
            baseThreshold = -20.0
            baseRatio = 4.0
            baseAttack = 3.0
            baseRelease = 150.0 // Wapis original effective setting par
        }
        BalanceProfile.Music -> {
            baseThreshold = -12.0
            baseRatio = 2.0
            baseAttack = 10.0
            baseRelease = 250.0
        }
        BalanceProfile.Mixed -> {
            baseThreshold = -16.0
            baseRatio = 3.0
            baseAttack = 5.0
            baseRelease = 200.0
        }
    }

    val adjustedRatio = baseRatio - 1.0
    val ratio = if (naturalDynamics) {
        if (adjustedRatio > 1.5) adjustedRatio else 1.5
    } else {
        baseRatio
    }

    // Wapis original effective makeup gain par (jo normal files par farq la raha tha)
    val makeupGainDb = when {
        intensity < 0.34f -> 3.0
        intensity < 0.67f -> 6.0
        else -> 9.0
    }

    return BalanceDspParams(
        thresholdDb = baseThreshold,
        ratio = ratio,
        attackMs = baseAttack,
        releaseMs = baseRelease,
        makeupGainDb = makeupGainDb,
        applyLimiter = preservePeaks
    )
}

class BalanceProcessor(
    private val sampleRate: Int,
    private val channels: Int,
    private val params: BalanceDspParams
) {
    private val thresholdLinear = 10.0.pow(params.thresholdDb / 20.0)
    private val attackCoef = 1.0 - exp(-1.0 / (params.attackMs * 0.001 * sampleRate))
    private val releaseCoef = 1.0 - exp(-1.0 / (params.releaseMs * 0.001 * sampleRate))
    private val compressionFactor = 1.0 - (1.0 / params.ratio)
    private val makeupGain = 10.0.pow(params.makeupGainDb / 20.0).toFloat()

    private val envelopes = DoubleArray(channels)
    private val currentGainDb = DoubleArray(channels)

    private val limiter: PeakLimiter? = if (params.applyLimiter) {
        PeakLimiter(sampleRate, channels, LIMITER_CEILING_DBTP)
    } else null

    private val loudnessMeter = LoudnessMeter(sampleRate, channels)
    private val peakMeter = PeakMeter(channels)

    private var compressed = FloatArray(0)
    private var boosted = FloatArray(0)
    private var limited = FloatArray(0)
    private var pcm = ShortArray(0)
    private var meterInput = FloatArray(0)

    var clippedSamples = 0L
        private set

    val pcmBuffer: ShortArray
        get() = pcm

    fun process(samples: FloatArray, count: Int): Int {
        ensureCapacity(count)

        for (i in 0 until count step channels) {
            for (ch in 0 until channels) {
                val sampleIdx = i + ch
                val sample = abs(samples[sampleIdx].toDouble())

                if (sample > envelopes[ch]) {
                    envelopes[ch] += (sample - envelopes[ch]) * attackCoef
                } else {
                    envelopes[ch] += (sample - envelopes[ch]) * releaseCoef
                }

                var targetGainDb = 0.0
                if (envelopes[ch] > thresholdLinear) {
                    val envelopeDb = 20.0 * log10(envelopes[ch])
                    val overshootDb = envelopeDb - params.thresholdDb
                    targetGainDb = -overshootDb * compressionFactor
                }

                currentGainDb[ch] += (targetGainDb - currentGainDb[ch]) * releaseCoef

                val gainLinear = 10.0.pow(currentGainDb[ch] / 20.0)
                compressed[sampleIdx] = (samples[sampleIdx] * gainLinear).toFloat()
            }
        }

        for (i in 0 until count) {
            boosted[i] = compressed[i] * makeupGain
        }

        val valid: Int
        val currentLimiter = limiter
        if (currentLimiter == null) {
            for (i in 0 until count) limited[i] = boosted[i]
            valid = count
        } else {
            valid = currentLimiter.process(boosted, count, limited)
        }

        return finishChunk(valid)
    }

    fun flush(): Int {
        val currentLimiter = limiter ?: return 0
        ensureCapacity(0)
        return finishChunk(currentLimiter.flush(limited))
    }

    private fun ensureCapacity(count: Int) {
        val flushCount = limiter?.flushValueCount ?: 0
        val needed = if (count > flushCount) count else flushCount
        if (pcm.size < needed) {
            compressed = FloatArray(needed)
            boosted = FloatArray(needed)
            limited = FloatArray(needed)
            pcm = ShortArray(needed)
            meterInput = FloatArray(needed)
        }
    }

    private fun finishChunk(valid: Int): Int {
        for (i in 0 until valid) {
            val rounded = (limited[i] * 32768f).roundToInt()
            val clamped = rounded.coerceIn(-32768, 32767)
            if (clamped != rounded) clippedSamples++
            pcm[i] = clamped.toShort()
            meterInput[i] = clamped / 32768f
        }
        loudnessMeter.process(meterInput, valid)
        peakMeter.process(meterInput, valid)
        return valid
    }

    fun maxLimiterReductionDb(): Double {
        val smallest = limiter?.smallestGain ?: 1.0
        return if (smallest < 1.0) -20.0 * log10(smallest) else 0.0
    }

    fun metrics(): AudioMetrics = AudioMetrics(
        integratedLufs = loudnessMeter.integratedLufs(),
        samplePeakDbfs = amplitudeToDb(peakMeter.samplePeak),
        truePeakDbtp = amplitudeToDb(peakMeter.truePeak),
        sampleRateHz = sampleRate,
        channels = channels
    )
}