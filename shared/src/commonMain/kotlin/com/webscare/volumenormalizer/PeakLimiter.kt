package com.webscare.volumenormalizer

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private const val DETECTOR_TAPS = 16
private const val DETECTOR_CENTRE = 7   // the frame we judge sits at position 7 of the last 16
private const val DETECTOR_LAG = 8      // frames we look ahead to see what happens between samples
private const val NEAR_RATIO = 0.3      // far below the ceiling: no need to look between samples

private fun detectorSinc(t: Double): Double {
    val halfWidth = 8.0
    if (abs(t) >= halfWidth) return 0.0
    val sinc = if (abs(t) < 1e-12) 1.0 else sin(PI * t) / (PI * t)
    val window = 0.42 + 0.5 * cos(PI * t / halfWidth) + 0.08 * cos(2.0 * PI * t / halfWidth)
    return sinc * window
}

/** Weights to estimate the signal at 1/4, 2/4 and 3/4 of the way between two samples. */
private val DETECTOR_COEFFS: Array<DoubleArray> = Array(3) { phase ->
    val fraction = (phase + 1) / 4.0
    val raw = DoubleArray(DETECTOR_TAPS) { j -> detectorSinc((j - DETECTOR_CENTRE) - fraction) }
    val total = raw.sum()
    DoubleArray(DETECTOR_TAPS) { j -> raw[j] / total }
}

/**
 * Keeps the loudest peaks below a ceiling by turning the volume down for a very short time.
 *
 * Stage A looks at every frame and at the 3 points between it and the next frame (true peak).
 * Stage B knows 5 ms ahead when a peak is coming, starts turning the volume down smoothly
 * before it, and lets it come back up slowly afterwards, so it does not sound like a cut.
 * Audio goes in with [process] and comes out slightly later; [flush] gives the last frames.
 * The same number of frames come out as went in.
 */
class PeakLimiter(
    sampleRate: Int,
    private val channels: Int,
    ceilingDbtp: Double,
    lookaheadSeconds: Double = 0.005,
    releaseSeconds: Double = 0.08
) {
    private val ceiling = 10.0.pow(ceilingDbtp / 20.0)

    // ----- Stage A: biggest value around each frame, including between samples -----
    private val history = Array(channels) { DoubleArray(2 * DETECTOR_TAPS) }
    private var historyPos = 0
    private var framesPushed = 0
    private val centreFrame = DoubleArray(channels)
    private var centreMagnitude = 0.0

    // ----- Stage B: gain that starts early and comes back slowly -----
    private val window = maxOf((sampleRate * lookaheadSeconds).roundToInt(), 8)
    private val delay = window - 1
    private val releaseCoef = 1.0 - exp(-1.0 / (releaseSeconds * sampleRate))

    // Smallest gain needed over the last [window] frames (a queue kept in a ring)
    private val queueCapacity = window + 1
    private val queueIndex = IntArray(queueCapacity)
    private val queueValue = DoubleArray(queueCapacity)
    private var queueHead = 0
    private var queueSize = 0
    private var frameNumber = 0

    private var envelope = 1.0
    private val ring = DoubleArray(window) { 1.0 }
    private var ringPos = 0
    private var ringSum = window.toDouble()

    private val delayLine = Array(channels) { DoubleArray(delay) }
    private var delayPos = 0
    private val silence = DoubleArray(channels)

    /** The strongest turn-down that happened (1.0 = never turned down). */
    var smallestGain = 1.0
        private set

    /** The most values that [flush] can write. */
    val flushValueCount: Int = (DETECTOR_LAG + delay) * channels

    /**
     * [input] holds whole frames (left, right, left, right...). Writes into [output]
     * and returns how many values were written (it is smaller at the very start).
     */
    fun process(input: FloatArray, count: Int, output: FloatArray): Int {
        var written = 0
        var inputPos = 0
        val frames = count / channels
        for (frame in 0 until frames) {
            if (pushToDetector(input, inputPos)) {
                if (limitFrame(centreMagnitude, centreFrame, output, written)) written += channels
            }
            inputPos += channels
        }
        return written
    }

    /** Call once at the end. Writes the frames that are still inside and returns the value count. */
    fun flush(output: FloatArray): Int {
        var written = 0
        repeat(DETECTOR_LAG) {
            if (pushToDetector(null, 0)) {
                if (limitFrame(centreMagnitude, centreFrame, output, written)) written += channels
            }
        }
        repeat(delay) {
            if (limitFrame(0.0, silence, output, written)) written += channels
        }
        return written
    }

    /** Adds one frame (or silence when [source] is null). True when an older frame is ready. */
    private fun pushToDetector(source: FloatArray?, sourcePos: Int): Boolean {
        for (channel in 0 until channels) {
            val value = if (source != null) source[sourcePos + channel].toDouble() else 0.0
            val h = history[channel]
            h[historyPos] = value
            h[historyPos + DETECTOR_TAPS] = value
        }
        historyPos = (historyPos + 1) % DETECTOR_TAPS
        framesPushed++
        if (framesPushed <= DETECTOR_LAG) return false

        var magnitude = 0.0
        var nearby = 0.0
        for (channel in 0 until channels) {
            val h = history[channel]
            val centre = h[historyPos + DETECTOR_CENTRE]
            val next = h[historyPos + DETECTOR_CENTRE + 1]
            centreFrame[channel] = centre
            val own = abs(centre)
            if (own > magnitude) magnitude = own
            val close = maxOf(own, abs(next))
            if (close > nearby) nearby = close
        }

        // Only look between the samples when they are close to the ceiling
        if (nearby >= ceiling * NEAR_RATIO) {
            for (channel in 0 until channels) {
                val h = history[channel]
                for (phase in 0 until 3) {
                    val coeffs = DETECTOR_COEFFS[phase]
                    var sum = 0.0
                    for (j in 0 until DETECTOR_TAPS) sum += h[historyPos + j] * coeffs[j]
                    val value = abs(sum)
                    if (value > magnitude) magnitude = value
                }
            }
        }
        centreMagnitude = magnitude
        return true
    }

    /** Stage B for one frame. True when a finished frame was written to [output] at [outputPos]. */
    private fun limitFrame(
        magnitude: Double,
        frame: DoubleArray,
        output: FloatArray,
        outputPos: Int
    ): Boolean {
        val required = if (magnitude > ceiling) ceiling / magnitude else 1.0

        // Smallest required gain over the last [window] frames
        while (queueSize > 0 &&
            queueValue[(queueHead + queueSize - 1) % queueCapacity] >= required
        ) {
            queueSize--
        }
        val tail = (queueHead + queueSize) % queueCapacity
        queueIndex[tail] = frameNumber
        queueValue[tail] = required
        queueSize++
        while (queueIndex[queueHead] <= frameNumber - window) {
            queueHead = (queueHead + 1) % queueCapacity
            queueSize--
        }
        val lowest = queueValue[queueHead]

        // Turn down at once, come back up slowly
        envelope = if (lowest < envelope) lowest else envelope + (lowest - envelope) * releaseCoef

        // Smooth the turn-down over the look-ahead time
        ringSum += envelope - ring[ringPos]
        ring[ringPos] = envelope
        ringPos = (ringPos + 1) % window
        val gain = minOf(ringSum / window, 1.0)

        var wrote = false
        if (frameNumber >= delay) {
            for (channel in 0 until channels) {
                output[outputPos + channel] = (delayLine[channel][delayPos] * gain).toFloat()
            }
            if (gain < smallestGain) smallestGain = gain
            wrote = true
        }
        for (channel in 0 until channels) {
            delayLine[channel][delayPos] = frame[channel]
        }
        delayPos = (delayPos + 1) % delay
        frameNumber++
        return wrote
    }
}