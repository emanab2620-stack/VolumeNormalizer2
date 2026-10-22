package com.webscare.volumenormalizer



import com.webscare.volumenormalizer.platform.AudioDecoder
import com.webscare.volumenormalizer.platform.DecodeOutcome
import com.webscare.volumenormalizer.platform.PcmSink

/** Receives the decoded audio piece by piece and feeds both meters. */
private class AnalysisSink : PcmSink {
    private var loudnessMeter: LoudnessMeter? = null
    private var peakMeter: PeakMeter? = null
    private var sampleRate = 0
    private var channels = 0

    override fun onFormat(sampleRate: Int, channels: Int) {
        this.sampleRate = sampleRate
        this.channels = channels
        loudnessMeter = LoudnessMeter(sampleRate, channels)
        peakMeter = PeakMeter(channels)
    }

    override fun onSamples(interleaved: FloatArray, count: Int) {
        loudnessMeter?.process(interleaved, count)
        peakMeter?.process(interleaved, count)
    }

    fun metrics(): AudioMetrics? {
        val loudness = loudnessMeter ?: return null
        val peaks = peakMeter ?: return null
        return AudioMetrics(
            integratedLufs = loudness.integratedLufs(),
            samplePeakDbfs = amplitudeToDb(peaks.samplePeak),
            truePeakDbtp = amplitudeToDb(peaks.truePeak),
            sampleRateHz = sampleRate,
            channels = channels
        )
    }
}

/**
 * Reads the whole file once and measures it. Returns null when the file cannot be decoded.
 * Cancelling the coroutine stops the work.
 */
suspend fun analyzeAudio(
    decoder: AudioDecoder,
    uri: String,
    onProgress: (Float) -> Unit = {}
): AudioMetrics? {
    val sink = AnalysisSink()
    val outcome = decoder.decode(uri, sink, onProgress)
    return if (outcome == DecodeOutcome.Success) sink.metrics() else null
}