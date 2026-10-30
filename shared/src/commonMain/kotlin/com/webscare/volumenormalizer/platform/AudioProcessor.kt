package com.webscare.volumenormalizer.platform

import androidx.compose.runtime.Composable
import com.webscare.volumenormalizer.AudioMetrics
import com.webscare.volumenormalizer.BalanceDspParams
import com.webscare.volumenormalizer.EqualizeDspParams // <-- NAYA: Equalize import

data class ProcessedAudio(
    val filePath: String,
    val metrics: AudioMetrics,
    val appliedGainDb: Double,
    val clippedSamples: Long,
    val maxLimiterReductionDb: Double
)

enum class ProcessFailure { Unsupported, Unreadable, CannotWrite, NotEnoughSpace }

sealed interface ProcessResult {
    data class Success(val audio: ProcessedAudio) : ProcessResult
    data class Failure(val reason: ProcessFailure) : ProcessResult
}

interface AudioProcessor {

    suspend fun applyGain(
        sourceUri: String,
        gainDb: Double,
        limiterCeilingDbtp: Double?,
        onProgress: (Float) -> Unit
    ): ProcessResult

    suspend fun applyBalance(
        sourceUri: String,
        params: BalanceDspParams,
        onProgress: (Float) -> Unit
    ): ProcessResult

    // <-- NAYA: Equalize function interface mein add kiya
    suspend fun applyEqualize(
        sourceUri: String,
        params: EqualizeDspParams,
        onProgress: (Float) -> Unit
    ): ProcessResult

    fun freeSpaceBytes(): Long

    fun clearTemporaryFiles()
}

@Composable
expect fun rememberAudioProcessor(): AudioProcessor