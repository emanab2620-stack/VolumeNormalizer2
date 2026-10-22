package com.webscare.volumenormalizer.platform


import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.webscare.volumenormalizer.platform.AudioDecoder
import com.webscare.volumenormalizer.platform.DecodeOutcome
import com.webscare.volumenormalizer.platform.PcmSink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteOrder

private const val TIMEOUT_US = 10_000L

private class AndroidAudioDecoder(private val context: Context) : AudioDecoder {

    override suspend fun decode(
        uri: String,
        sink: PcmSink,
        onProgress: (Float) -> Unit
    ): DecodeOutcome = withContext(Dispatchers.Default) {
        decodeBlocking(this, Uri.parse(uri), sink, onProgress)
    }

    private fun decodeBlocking(
        scope: CoroutineScope,
        uri: Uri,
        sink: PcmSink,
        onProgress: (Float) -> Unit
    ): DecodeOutcome {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)

            // Find the first audio track in the file
            var trackFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val candidate = extractor.getTrackFormat(i)
                val mime = candidate.getString(MediaFormat.KEY_MIME)
                if (mime != null && mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    trackFormat = candidate
                    break
                }
            }
            if (trackFormat == null) return DecodeOutcome.Unsupported
            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: return DecodeOutcome.Unsupported

            val durationUs = if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
                trackFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }

            val decoder = MediaCodec.createDecoderByType(mime)
            codec = decoder
            decoder.configure(trackFormat, null, null, 0)
            decoder.start()

            return runDecodeLoop(scope, extractor, decoder, trackFormat, durationUs, sink, onProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return DecodeOutcome.Unreadable
        } finally {
            try {
                codec?.stop()
            } catch (ignored: Exception) {
                // nothing to do
            }
            codec?.release()
            extractor.release()
        }
    }

    private fun runDecodeLoop(
        scope: CoroutineScope,
        extractor: MediaExtractor,
        decoder: MediaCodec,
        trackFormat: MediaFormat,
        durationUs: Long,
        sink: PcmSink,
        onProgress: (Float) -> Unit
    ): DecodeOutcome {
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        var sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var encoding = AudioFormat.ENCODING_PCM_16BIT

        var formatSent = false
        var sentSampleRate = 0
        var sentChannels = 0
        var scratch = FloatArray(0)
        var lastReported = -1f

        while (!outputDone) {
            scope.ensureActive()

            // 1) Give the decoder more compressed data
            if (!inputDone) {
                val inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                if (inputIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputIndex)
                    val size = if (inputBuffer != null) extractor.readSampleData(inputBuffer, 0) else -1
                    if (size < 0) {
                        decoder.queueInputBuffer(
                            inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputDone = true
                    } else {
                        decoder.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            // 2) Take decoded audio out
            val outputIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val outputFormat = decoder.outputFormat
                sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                encoding = if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                    outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                } else {
                    AudioFormat.ENCODING_PCM_16BIT
                }
                if (formatSent && (sampleRate != sentSampleRate || channels != sentChannels)) {
                    return DecodeOutcome.Unsupported
                }
            } else if (outputIndex >= 0) {
                val buffer = decoder.getOutputBuffer(outputIndex)
                if (buffer != null && info.size > 0) {
                    if (!formatSent) {
                        sink.onFormat(sampleRate, channels)
                        formatSent = true
                        sentSampleRate = sampleRate
                        sentChannels = channels
                    }

                    buffer.position(info.offset)
                    buffer.limit(info.offset + info.size)
                    buffer.order(ByteOrder.nativeOrder())

                    val valueCount: Int
                    when (encoding) {
                        AudioFormat.ENCODING_PCM_FLOAT -> {
                            val floats = buffer.asFloatBuffer()
                            valueCount = floats.remaining()
                            if (scratch.size < valueCount) scratch = FloatArray(valueCount)
                            floats.get(scratch, 0, valueCount)
                        }
                        AudioFormat.ENCODING_PCM_16BIT -> {
                            val shorts = buffer.asShortBuffer()
                            valueCount = shorts.remaining()
                            if (scratch.size < valueCount) scratch = FloatArray(valueCount)
                            for (i in 0 until valueCount) {
                                scratch[i] = shorts.get(i) / 32768f
                            }
                        }
                        else -> return DecodeOutcome.Unsupported
                    }

                    // Only whole frames (one value per channel) are passed on
                    val wholeFrames = valueCount - (valueCount % channels)
                    if (wholeFrames > 0) sink.onSamples(scratch, wholeFrames)

                    if (durationUs > 0) {
                        val progress = (info.presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 1f)
                        if (progress - lastReported >= 0.01f) {
                            onProgress(progress)
                            lastReported = progress
                        }
                    }
                }
                decoder.releaseOutputBuffer(outputIndex, false)
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
            }
        }

        return if (formatSent) DecodeOutcome.Success else DecodeOutcome.Unreadable
    }
}

@Composable
actual fun rememberAudioDecoder(): AudioDecoder {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) { AndroidAudioDecoder(appContext) }
}