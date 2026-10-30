package com.webscare.volumenormalizer.platform

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.arthenica.ffmpegkit.FFmpegKit
import com.webscare.volumenormalizer.AudioMetrics
import com.webscare.volumenormalizer.BalanceDspParams
import com.webscare.volumenormalizer.BalanceProcessor
import com.webscare.volumenormalizer.EqualizeDspParams
import com.webscare.volumenormalizer.EqualizeProcessor
import com.webscare.volumenormalizer.GainProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TEMP_FOLDER = "processed"
private const val WAV_HEADER_SIZE = 44
private const val MAX_WAV_DATA_BYTES = 4_000_000_000L
private const val TAG = "AudioProcessor"

// ============================================================================
// 1. WAV GAIN SINK (Normalize / Boost ke liye)
// ============================================================================
private class WavGainSink(
    private val file: File,
    private val gainDb: Double,
    private val limiterCeilingDbtp: Double?
) : PcmSink {

    private var processor: GainProcessor? = null
    private var output: RandomAccessFile? = null
    private var sampleRate = 0
    private var channels = 0
    private var dataBytes = 0L
    private var buffer = ByteArray(0)

    var writeFailed = false
        private set

    override fun onFormat(sampleRate: Int, channels: Int) {
        this.sampleRate = sampleRate
        this.channels = channels
        processor = GainProcessor(sampleRate, channels, gainDb, limiterCeilingDbtp)
        try {
            val raf = RandomAccessFile(file, "rw")
            output = raf
            raf.setLength(0)
            raf.write(ByteArray(WAV_HEADER_SIZE))
        } catch (e: IOException) {
            writeFailed = true
            throw e
        }
    }

    override fun onSamples(interleaved: FloatArray, count: Int) {
        val gainProcessor = processor ?: return
        val valid = gainProcessor.process(interleaved, count)
        writePcm(gainProcessor.pcmBuffer, valid)
    }

    private fun writePcm(pcm: ShortArray, valueCount: Int) {
        if (valueCount <= 0) return
        val raf = output ?: return

        val byteCount = valueCount * 2
        if (dataBytes + byteCount > MAX_WAV_DATA_BYTES) {
            writeFailed = true
            throw IOException("The WAV file would be too large")
        }
        if (buffer.size < byteCount) buffer = ByteArray(byteCount)
        for (i in 0 until valueCount) {
            val value = pcm[i].toInt()
            buffer[2 * i] = (value and 0xFF).toByte()
            buffer[2 * i + 1] = ((value shr 8) and 0xFF).toByte()
        }
        try {
            raf.write(buffer, 0, byteCount)
        } catch (e: IOException) {
            writeFailed = true
            throw e
        }
        dataBytes += byteCount
    }

    fun finish(): ProcessedAudio? {
        val gainProcessor = processor ?: return null
        val raf = output ?: return null

        val tail = gainProcessor.flush()
        writePcm(gainProcessor.pcmBuffer, tail)
        if (dataBytes == 0L) return null

        val header = ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt((36 + dataBytes).toInt())
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1.toShort())
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(sampleRate * channels * 2)
        header.putShort((channels * 2).toShort())
        header.putShort(16.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataBytes.toInt())

        raf.seek(0)
        raf.write(header.array())

        return ProcessedAudio(
            filePath = file.absolutePath,
            metrics = gainProcessor.metrics(),
            appliedGainDb = gainDb,
            clippedSamples = gainProcessor.clippedSamples,
            maxLimiterReductionDb = gainProcessor.maxLimiterReductionDb()
        )
    }

    fun close() {
        try {
            output?.close()
        } catch (ignored: IOException) {
        }
    }
}


private class BalanceGainSink(
    private val file: File,
    private val params: BalanceDspParams
) : PcmSink {

    private var processor: BalanceProcessor? = null
    private var output: RandomAccessFile? = null
    private var sampleRate = 0
    private var channels = 0
    private var dataBytes = 0L
    private var buffer = ByteArray(0)

    var writeFailed = false
        private set

    override fun onFormat(sampleRate: Int, channels: Int) {
        this.sampleRate = sampleRate
        this.channels = channels
        processor = BalanceProcessor(sampleRate, channels, params)
        try {
            val raf = RandomAccessFile(file, "rw")
            output = raf
            raf.setLength(0)
            raf.write(ByteArray(WAV_HEADER_SIZE))
        } catch (e: IOException) {
            writeFailed = true
            throw e
        }
    }

    override fun onSamples(interleaved: FloatArray, count: Int) {
        val balanceProcessor = processor ?: return
        val valid = balanceProcessor.process(interleaved, count)
        writePcm(balanceProcessor.pcmBuffer, valid)
    }

    private fun writePcm(pcm: ShortArray, valueCount: Int) {
        if (valueCount <= 0) return
        val raf = output ?: return

        val byteCount = valueCount * 2
        if (dataBytes + byteCount > MAX_WAV_DATA_BYTES) {
            writeFailed = true
            throw IOException("The WAV file would be too large")
        }
        if (buffer.size < byteCount) buffer = ByteArray(byteCount)
        for (i in 0 until valueCount) {
            val value = pcm[i].toInt()
            buffer[2 * i] = (value and 0xFF).toByte()
            buffer[2 * i + 1] = ((value shr 8) and 0xFF).toByte()
        }
        try {
            raf.write(buffer, 0, byteCount)
        } catch (e: IOException) {
            writeFailed = true
            throw e
        }
        dataBytes += byteCount
    }

    fun finish(): ProcessedAudio? {
        val balanceProcessor = processor ?: return null
        val raf = output ?: return null

        val tail = balanceProcessor.flush()
        writePcm(balanceProcessor.pcmBuffer, tail)
        if (dataBytes == 0L) return null

        val header = ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt((36 + dataBytes).toInt())
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1.toShort())
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(sampleRate * channels * 2)
        header.putShort((channels * 2).toShort())
        header.putShort(16.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataBytes.toInt())

        raf.seek(0)
        raf.write(header.array())

        return ProcessedAudio(
            filePath = file.absolutePath,
            metrics = balanceProcessor.metrics(),
            appliedGainDb = params.makeupGainDb,
            clippedSamples = balanceProcessor.clippedSamples,
            maxLimiterReductionDb = balanceProcessor.maxLimiterReductionDb()
        )
    }

    fun close() {
        try {
            output?.close()
        } catch (ignored: IOException) {
        }
    }
}

// ============================================================================
// 3. EQUALIZE GAIN SINK (Equalize tool ke liye)
// ============================================================================
private class EqualizeGainSink(
    private val file: File,
    private val params: EqualizeDspParams
) : PcmSink {

    private var processor: EqualizeProcessor? = null
    private var output: RandomAccessFile? = null
    private var sampleRate = 0
    private var channels = 0
    private var dataBytes = 0L
    private var buffer = ByteArray(0)

    var writeFailed = false
        private set

    override fun onFormat(sampleRate: Int, channels: Int) {
        this.sampleRate = sampleRate
        this.channels = channels
        processor = EqualizeProcessor(sampleRate, channels, params)
        try {
            val raf = RandomAccessFile(file, "rw")
            output = raf
            raf.setLength(0)
            raf.write(ByteArray(WAV_HEADER_SIZE))
        } catch (e: IOException) {
            writeFailed = true
            throw e
        }
    }

    override fun onSamples(interleaved: FloatArray, count: Int) {
        val eqProcessor = processor ?: return
        val valid = eqProcessor.process(interleaved, count)
        writePcm(eqProcessor.pcmBuffer, valid)
    }

    private fun writePcm(pcm: ShortArray, valueCount: Int) {
        if (valueCount <= 0) return
        val raf = output ?: return

        val byteCount = valueCount * 2
        if (dataBytes + byteCount > MAX_WAV_DATA_BYTES) {
            writeFailed = true
            throw IOException("The WAV file would be too large")
        }
        if (buffer.size < byteCount) buffer = ByteArray(byteCount)
        for (i in 0 until valueCount) {
            val value = pcm[i].toInt()
            buffer[2 * i] = (value and 0xFF).toByte()
            buffer[2 * i + 1] = ((value shr 8) and 0xFF).toByte()
        }
        try {
            raf.write(buffer, 0, byteCount)
        } catch (e: IOException) {
            writeFailed = true
            throw e
        }
        dataBytes += byteCount
    }

    fun finish(): ProcessedAudio? {
        val eqProcessor = processor ?: return null
        val raf = output ?: return null

        if (dataBytes == 0L) return null

        val header = ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt((36 + dataBytes).toInt())
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1.toShort())
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(sampleRate * channels * 2)
        header.putShort((channels * 2).toShort())
        header.putShort(16.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataBytes.toInt())

        raf.seek(0)
        raf.write(header.array())

        // ✅ REAL METRICS ab processor se aayengi
        return ProcessedAudio(
            filePath = file.absolutePath,
            metrics = eqProcessor.metrics(),
            appliedGainDb = 0.0,
            clippedSamples = eqProcessor.clippedSamples,
            maxLimiterReductionDb = 0.0
        )
    }

    fun close() {
        try {
            output?.close()
        } catch (ignored: IOException) {
        }
    }
}

// ============================================================================
// 4. ANDROID AUDIO PROCESSOR
// ============================================================================
private class AndroidAudioProcessor(
    private val context: Context,
    private val decoder: AudioDecoder
) : AudioProcessor {

    private val folder: File
        get() = File(context.cacheDir, TEMP_FOLDER)

    /**
     * ✅ FIXED: WAV files ko directly use karein (fast aur reliable)
     * Non-WAV files (MP3, M4A, OPUS, OGG) ko FFmpeg se WAV mein convert karein
     */
    private fun getResolvedInputPath(sourceUri: String): String {
        val cleanPath = sourceUri.removePrefix("file://")
        val sourceFile = File(cleanPath)
        val extension = sourceFile.extension.lowercase()

        val inputPath = if (sourceFile.exists()) {
            sourceFile.absolutePath
        } else {
            val tempInput = File(folder, "input_${System.currentTimeMillis()}.$extension")
            try {
                val uri = sourceUri.toUri()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempInput.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            tempInput.absolutePath
        }

        // ✅ WAV files ko directly use karein (FFmpeg skip)
        if (extension == "wav" || extension == "wave") {
            Log.d(TAG, "WAV file detected, using directly: $inputPath")
            return inputPath
        }

        // ✅ Non-WAV files ko FFmpeg se WAV mein convert karein
        val decodedWav = File(folder, "decoded_${System.currentTimeMillis()}.wav")
        val convertCommand = "-y -i \"$inputPath\" -c:a pcm_s16le -ar 48000 -ac 2 \"${decodedWav.absolutePath}\""
        Log.d(TAG, "Executing Conversion Command: $convertCommand")

        val session = FFmpegKit.execute(convertCommand)
        if (session.returnCode.isValueSuccess && decodedWav.exists() && decodedWav.length() > 0) {
            Log.d(TAG, "Successfully converted to 48000Hz stereo WAV: ${decodedWav.absolutePath}")
            return decodedWav.absolutePath
        }

        Log.w(TAG, "FFmpeg conversion failed, falling back to original: $inputPath")
        return inputPath
    }

    override suspend fun applyGain(
        sourceUri: String,
        gainDb: Double,
        limiterCeilingDbtp: Double?,
        onProgress: (Float) -> Unit
    ): ProcessResult = withContext(Dispatchers.IO) {
        runGain(sourceUri, gainDb, limiterCeilingDbtp, onProgress)
    }

    private suspend fun runGain(
        sourceUri: String,
        gainDb: Double,
        limiterCeilingDbtp: Double?,
        onProgress: (Float) -> Unit
    ): ProcessResult {
        val dir = folder
        dir.mkdirs()

        val resolvedPath = getResolvedInputPath(sourceUri)
        onProgress(0.4f)

        val outputFile = File(dir, "processed_${System.currentTimeMillis()}.wav")
        val sink = WavGainSink(outputFile, gainDb, limiterCeilingDbtp)
        var finished = false
        try {
            val outcome = decoder.decode(resolvedPath, sink, onProgress)
            if (outcome != DecodeOutcome.Success) {
                val reason = when {
                    sink.writeFailed -> ProcessFailure.CannotWrite
                    outcome == DecodeOutcome.Unsupported -> ProcessFailure.Unsupported
                    else -> ProcessFailure.Unreadable
                }
                return ProcessResult.Failure(reason)
            }
            val processed = sink.finish() ?: return ProcessResult.Failure(ProcessFailure.Unreadable)
            finished = true
            onProgress(1.0f)
            return ProcessResult.Success(processed)
        } catch (e: IOException) {
            return ProcessResult.Failure(ProcessFailure.CannotWrite)
        } finally {
            sink.close()
            if (!finished) outputFile.delete()
        }
    }

    override suspend fun applyBalance(
        sourceUri: String,
        params: BalanceDspParams,
        onProgress: (Float) -> Unit
    ): ProcessResult = withContext(Dispatchers.IO) {
        runBalance(sourceUri, params, onProgress)
    }

    private suspend fun runBalance(
        sourceUri: String,
        params: BalanceDspParams,
        onProgress: (Float) -> Unit
    ): ProcessResult {
        val dir = folder
        dir.mkdirs()

        val resolvedPath = getResolvedInputPath(sourceUri)
        onProgress(0.4f)

        val outputFile = File(dir, "balanced_${System.currentTimeMillis()}.wav")
        val sink = BalanceGainSink(outputFile, params)
        var finished = false
        try {
            val outcome = decoder.decode(resolvedPath, sink, onProgress)
            if (outcome != DecodeOutcome.Success) {
                val reason = when {
                    sink.writeFailed -> ProcessFailure.CannotWrite
                    outcome == DecodeOutcome.Unsupported -> ProcessFailure.Unsupported
                    else -> ProcessFailure.Unreadable
                }
                return ProcessResult.Failure(reason)
            }
            val processed = sink.finish() ?: return ProcessResult.Failure(ProcessFailure.Unreadable)
            finished = true
            onProgress(1.0f)
            return ProcessResult.Success(processed)
        } catch (e: IOException) {
            return ProcessResult.Failure(ProcessFailure.CannotWrite)
        } finally {
            sink.close()
            if (!finished) outputFile.delete()
        }
    }

    override suspend fun applyEqualize(
        sourceUri: String,
        params: EqualizeDspParams,
        onProgress: (Float) -> Unit
    ): ProcessResult = withContext(Dispatchers.IO) {
        runEqualize(sourceUri, params, onProgress)
    }

    private suspend fun runEqualize(
        sourceUri: String,
        params: EqualizeDspParams,
        onProgress: (Float) -> Unit
    ): ProcessResult {
        val dir = folder
        dir.mkdirs()

        val resolvedPath = getResolvedInputPath(sourceUri)
        onProgress(0.4f)

        val outputFile = File(dir, "equalized_${System.currentTimeMillis()}.wav")
        val sink = EqualizeGainSink(outputFile, params)
        var finished = false
        try {
            val outcome = decoder.decode(resolvedPath, sink, onProgress)
            if (outcome != DecodeOutcome.Success) {
                val reason = when {
                    sink.writeFailed -> ProcessFailure.CannotWrite
                    outcome == DecodeOutcome.Unsupported -> ProcessFailure.Unsupported
                    else -> ProcessFailure.Unreadable
                }
                return ProcessResult.Failure(reason)
            }
            val processed = sink.finish() ?: return ProcessResult.Failure(ProcessFailure.Unreadable)
            finished = true
            onProgress(1.0f)
            return ProcessResult.Success(processed)
        } catch (e: IOException) {
            return ProcessResult.Failure(ProcessFailure.CannotWrite)
        } finally {
            sink.close()
            if (!finished) outputFile.delete()
        }
    }

    override fun freeSpaceBytes(): Long = context.cacheDir.usableSpace

    override fun clearTemporaryFiles() {
        folder.listFiles()?.forEach { it.delete() }
    }
}

@Composable
actual fun rememberAudioProcessor(): AudioProcessor {
    val appContext = LocalContext.current.applicationContext
    val decoder = rememberAudioDecoder()
    return remember(appContext, decoder) { AndroidAudioProcessor(appContext, decoder) }
}