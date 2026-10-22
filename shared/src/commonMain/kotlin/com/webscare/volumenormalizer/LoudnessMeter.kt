package com.webscare.volumenormalizer
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan

private const val ABSOLUTE_GATE_LUFS = -70.0
private const val RELATIVE_GATE_LU = -10.0
private const val LUFS_OFFSET = -0.691

private fun energyToLufs(energy: Double): Double = LUFS_OFFSET + 10.0 * log10(energy)

private fun lufsToEnergy(lufs: Double): Double = 10.0.pow((lufs - LUFS_OFFSET) / 10.0)

/** One filter stage (transposed direct form II). */
private class Biquad(coefficients: DoubleArray) {
    private val b0 = coefficients[0]
    private val b1 = coefficients[1]
    private val b2 = coefficients[2]
    private val a1 = coefficients[3]
    private val a2 = coefficients[4]
    private var z1 = 0.0
    private var z2 = 0.0

    fun process(x: Double): Double {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }
}

/** High-shelf stage of the K-weighting filter (ITU-R BS.1770), for any sample rate. */
private fun shelfCoefficients(sampleRate: Double): DoubleArray {
    val f0 = 1681.974450955533
    val gain = 3.999843853973347
    val q = 0.7071752369554196
    val k = tan(PI * f0 / sampleRate)
    val vh = 10.0.pow(gain / 20.0)
    val vb = vh.pow(0.4996667741545416)
    val a0 = 1.0 + k / q + k * k
    return doubleArrayOf(
        (vh + vb * k / q + k * k) / a0,
        2.0 * (k * k - vh) / a0,
        (vh - vb * k / q + k * k) / a0,
        2.0 * (k * k - 1.0) / a0,
        (1.0 - k / q + k * k) / a0
    )
}

/** High-pass stage of the K-weighting filter (ITU-R BS.1770), for any sample rate. */
private fun highPassCoefficients(sampleRate: Double): DoubleArray {
    val f0 = 38.13547087602444
    val q = 0.5003270373238773
    val k = tan(PI * f0 / sampleRate)
    val a0 = 1.0 + k / q + k * k
    return doubleArrayOf(
        1.0,
        -2.0,
        1.0,
        2.0 * (k * k - 1.0) / a0,
        (1.0 - k / q + k * k) / a0
    )
}

/** Surround channels count a bit more, the LFE channel is ignored (5.1 order: L R C LFE Ls Rs). */
private fun channelWeight(index: Int, channels: Int): Double = when {
    channels == 6 && index == 3 -> 0.0
    channels == 6 && (index == 4 || index == 5) -> 1.41
    else -> 1.0
}

/**
 * Measures integrated loudness (LUFS) as described in ITU-R BS.1770 / EBU R128.
 * Audio is fed in small pieces with [process]; read the result with [integratedLufs].
 */
class LoudnessMeter(sampleRate: Int, private val channels: Int) {

    private val shelfFilters = Array(channels) { Biquad(shelfCoefficients(sampleRate.toDouble())) }
    private val highPassFilters = Array(channels) { Biquad(highPassCoefficients(sampleRate.toDouble())) }
    private val weights = DoubleArray(channels) { channelWeight(it, channels) }

    // The audio is cut into 100 ms pieces. 4 pieces in a row make one 400 ms block.
    private val subBlockLength = (sampleRate * 0.1).roundToInt().coerceAtLeast(1)
    private val subBlockSums = DoubleArray(channels)
    private var subBlockFill = 0
    private val recentEnergies = DoubleArray(4)
    private var recentIndex = 0
    private var subBlocksSeen = 0
    private val blockEnergies = ArrayList<Double>()

    /** [interleaved] holds whole frames: left, right, left, right... [count] is the number of values. */
    fun process(interleaved: FloatArray, count: Int) {
        var index = 0
        val frames = count / channels
        for (frame in 0 until frames) {
            for (channel in 0 until channels) {
                val x = interleaved[index++].toDouble()
                val y = highPassFilters[channel].process(shelfFilters[channel].process(x))
                subBlockSums[channel] += y * y
            }
            subBlockFill++
            if (subBlockFill == subBlockLength) finishSubBlock()
        }
    }

    private fun finishSubBlock() {
        var energy = 0.0
        for (channel in 0 until channels) {
            energy += weights[channel] * (subBlockSums[channel] / subBlockLength)
            subBlockSums[channel] = 0.0
        }
        subBlockFill = 0

        recentEnergies[recentIndex] = energy
        recentIndex = (recentIndex + 1) % 4
        subBlocksSeen++
        if (subBlocksSeen >= 4) {
            blockEnergies.add(
                (recentEnergies[0] + recentEnergies[1] + recentEnergies[2] + recentEnergies[3]) / 4.0
            )
        }
    }

    /** The loudness of the whole file, or null when it is silent or shorter than 400 ms. */
    fun integratedLufs(): Double? {
        if (blockEnergies.isEmpty()) return null

        // Gate 1: ignore blocks quieter than -70 LUFS (silence)
        val absoluteGate = lufsToEnergy(ABSOLUTE_GATE_LUFS)
        var sum = 0.0
        var count = 0
        for (energy in blockEnergies) {
            if (energy > absoluteGate) {
                sum += energy
                count++
            }
        }
        if (count == 0) return null

        // Gate 2: ignore blocks more than 10 LU below the average of the rest
        val relativeGate = lufsToEnergy(energyToLufs(sum / count) + RELATIVE_GATE_LU)
        var gatedSum = 0.0
        var gatedCount = 0
        for (energy in blockEnergies) {
            if (energy > absoluteGate && energy > relativeGate) {
                gatedSum += energy
                gatedCount++
            }
        }
        if (gatedCount == 0) return null

        return energyToLufs(gatedSum / gatedCount)
    }
}