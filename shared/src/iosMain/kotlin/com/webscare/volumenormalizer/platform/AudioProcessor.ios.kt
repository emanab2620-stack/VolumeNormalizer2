package com.webscare.volumenormalizer.platform

import androidx.compose.runtime.Composable
import com.webscare.volumenormalizer.BalanceDspParams

private object UnsupportedAudioProcessor : AudioProcessor {
    override suspend fun applyGain(
        sourceUri: String,
        gainDb: Double,
        limiterCeilingDbtp: Double?,
        onProgress: (Float) -> Unit
    ): ProcessResult = ProcessResult.Failure(ProcessFailure.Unsupported)

    override suspend fun applyBalance(
        sourceUri: String,
        params: BalanceDspParams,
        onProgress: (Float) -> Unit
    ): ProcessResult = ProcessResult.Failure(ProcessFailure.Unsupported)

    override fun freeSpaceBytes(): Long = Long.MAX_VALUE

    override fun clearTemporaryFiles() {}
}

@Composable
actual fun rememberAudioProcessor(): AudioProcessor = UnsupportedAudioProcessor