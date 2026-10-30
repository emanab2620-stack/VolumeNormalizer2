package com.webscare.volumenormalizer

import com.webscare.volumenormalizer.platform.ProcessFailure
import com.webscare.volumenormalizer.platform.ProcessedAudio
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** What Peak Protection promises: the estimated true peak stays at or below this level. */
const val PEAK_CEILING_DBTP = -1.0

/** The limiter works slightly lower, because the true peak is an estimate (up to about 0.2 dB off). */
const val LIMITER_CEILING_DBTP = PEAK_CEILING_DBTP - 0.2

/**
 * [appliedGainDb] is the gain that is applied (the same as requested).
 * [limitedByPeak] says that the loudest peaks are limited (or were limited).
 */
data class GainPlan(
    val requestedGainDb: Double,
    val appliedGainDb: Double,
    val limitedByPeak: Boolean
)

/**
 * Normalize: works out the gain from the target. Returns null when the file has
 * no measurable loudness (silent file). [limitedByPeak] is a prediction: the peaks would pass
 * the ceiling, so the limiter will have to work.
 */
fun planGain(
    targetLufs: Double,
    metrics: AudioMetrics,
    peakProtection: Boolean,
    ceilingDbtp: Double = PEAK_CEILING_DBTP
): GainPlan? {
    val current = metrics.integratedLufs ?: return null
    val requested = targetLufs - current
    val peak = metrics.truePeakDbtp
    val willLimit = peakProtection && peak != null && peak + requested > ceilingDbtp
    return GainPlan(requested, requested, willLimit)
}

/** Boost: the user chooses the gain in dB. [limitedByPeak] is a prediction, as above. */
fun planBoost(
    requestedGainDb: Double,
    metrics: AudioMetrics,
    peakProtection: Boolean,
    ceilingDbtp: Double = PEAK_CEILING_DBTP
): GainPlan {
    val peak = metrics.truePeakDbtp
    val willLimit = peakProtection && peak != null && peak + requestedGainDb > ceilingDbtp
    return GainPlan(requestedGainDb, requestedGainDb, willLimit)
}

/**
 * Lightweight Dynamics Compressor for smooth loudness increase.
 * It gently tames the highest peaks, creating room for the overall gain to be applied
 * without triggering the hard limiter immediately. This makes voice notes sound
 * fuller, smoother, and noticeably louder.
 */
class DynamicsCompressor(
    private val channels: Int,
    private val sampleRate: Int,
    private val thresholdDb: Double = -15.0,
    private val ratio: Double = 3.0,
    attackMs: Double = 5.0,
    releaseMs: Double = 150.0
) {
    private val thresholdLinear = 10.0.pow(thresholdDb / 20.0)
    private val attackCoef = 1.0 - exp(-1.0 / (attackMs * 0.001 * sampleRate))
    private val releaseCoef = 1.0 - exp(-1.0 / (releaseMs * 0.001 * sampleRate))
    private val compressionFactor = 1.0 - (1.0 / ratio)

    private val envelopes = DoubleArray(channels)
    private val currentGainDb = DoubleArray(channels)

    fun process(input: FloatArray, count: Int, output: FloatArray) {
        for (i in 0 until count step channels) {
            for (ch in 0 until channels) {
                val sampleIdx = i + ch
                val sample = abs(input[sampleIdx].toDouble())

                // 1. Envelope Follower (Track the volume level)
                if (sample > envelopes[ch]) {
                    envelopes[ch] += (sample - envelopes[ch]) * attackCoef
                } else {
                    envelopes[ch] += (sample - envelopes[ch]) * releaseCoef
                }

                // 2. Calculate Gain Reduction
                var targetGainDb = 0.0
                if (envelopes[ch] > thresholdLinear) {
                    val envelopeDb = 20.0 * log10(envelopes[ch])
                    val overshootDb = envelopeDb - thresholdDb
                    targetGainDb = -overshootDb * compressionFactor
                }

                // 3. Smooth Gain Transition (prevents audible pumping/breathing)
                currentGainDb[ch] += (targetGainDb - currentGainDb[ch]) * releaseCoef

                // 4. Apply Compressed Gain
                val gainLinear = 10.0.pow(currentGainDb[ch] / 20.0)
                output[sampleIdx] = (input[sampleIdx] * gainLinear).toFloat()
            }
        }
    }
}

/**
 * Applies a gain to audio pieces, optionally limits the peaks, and turns the result into
 * 16-bit values. The meters look at exactly the values that will be written, so the final
 * measurement is the true result of the new file.
 */
class GainProcessor(
    private val sampleRate: Int,
    private val channels: Int,
    gainDb: Double,
    limiterCeilingDbtp: Double? = null
) {
    private val gain = 10.0.pow(gainDb / 20.0).toFloat()

    // Add Compressor ONLY if we are boosting/normalizing and need to create headroom.
    // We activate it if the requested gain is > 3dB, as lower gains don't need it.
    private val compressor: DynamicsCompressor? =
        if (gainDb > 3.0) DynamicsCompressor(channels, sampleRate) else null

    private val limiter: PeakLimiter? =
        if (limiterCeilingDbtp != null) PeakLimiter(sampleRate, channels, limiterCeilingDbtp) else null
    private val loudnessMeter = LoudnessMeter(sampleRate, channels)
    private val peakMeter = PeakMeter(channels)

    private var compressed = FloatArray(0)
    private var boosted = FloatArray(0)
    private var limited = FloatArray(0)
    private var pcm = ShortArray(0)
    private var meterInput = FloatArray(0)

    /** Number of values that were too loud for 16 bits and had to be cut. */
    var clippedSamples = 0L
        private set

    /** The 16-bit result of the last [process] or [flush] call. Only the valid values count. */
    val pcmBuffer: ShortArray
        get() = pcm

    /** Gain, then limiter. Returns how many values of [pcmBuffer] are valid. */
    fun process(samples: FloatArray, count: Int): Int {
        ensureCapacity(count)
        val currentLimiter = limiter
        val valid: Int

        var workingArray = samples

        // Stage 1: Compression (Smooth out high peaks to create headroom)
        if (compressor != null) {
            compressor.process(samples, count, compressed)
            workingArray = compressed
        }

        // Stage 2 & 3: Apply User Gain, then Limiter (Safety net)
        if (currentLimiter == null) {
            for (i in 0 until count) limited[i] = workingArray[i] * gain
            valid = count
        } else {
            for (i in 0 until count) boosted[i] = workingArray[i] * gain
            valid = currentLimiter.process(boosted, count, limited)
        }
        return finishChunk(valid)
    }

    /** Call once at the end. Returns how many values of [pcmBuffer] are valid. */
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

    /** How many dB the limiter had to turn the volume down at its strongest (0 = never). */
    fun maxLimiterReductionDb(): Double {
        val smallest = limiter?.smallestGain ?: 1.0
        return if (smallest < 1.0) -20.0 * log10(smallest) else 0.0
    }

    /** What the new audio measures like. */
    fun metrics(): AudioMetrics = AudioMetrics(
        integratedLufs = loudnessMeter.integratedLufs(),
        samplePeakDbfs = amplitudeToDb(peakMeter.samplePeak),
        truePeakDbtp = amplitudeToDb(peakMeter.truePeak),
        sampleRateHz = sampleRate,
        channels = channels
    )
}

/** Everything the Result screen needs. [targetLufs] is null for a Boost. */
data class NormalizeOutput(
    val before: AudioMetrics,
    val processed: ProcessedAudio,
    val plan: GainPlan,
    val targetLufs: Int?
)

/** The state of the processing job. */
sealed interface ProcessingState {
    /** [progress] is 0..1, or null when it cannot be known yet. */
    data class Running(val progress: Float?) : ProcessingState

    data class Failed(val reason: ProcessFailure) : ProcessingState
}