package com.webscare.volumenormalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.webscare.volumenormalizer.platform.PickedAudio
import com.webscare.volumenormalizer.platform.ProcessFailure
import com.webscare.volumenormalizer.platform.ProcessResult
import com.webscare.volumenormalizer.platform.RejectedAudio
import com.webscare.volumenormalizer.platform.detailsLine
import com.webscare.volumenormalizer.platform.rememberAudioDecoder
import com.webscare.volumenormalizer.platform.rememberAudioPicker
import com.webscare.volumenormalizer.platform.rememberAudioProcessor
import com.webscare.volumenormalizer.theme.AppColors
import com.webscare.volumenormalizer.theme.AppTheme
import com.webscare.volumenormalizer.ui.balance.BalanceRoute
import com.webscare.volumenormalizer.ui.batch.BatchRoute
import com.webscare.volumenormalizer.ui.boost.BoostScreen
import com.webscare.volumenormalizer.ui.boost.BoostSettings
import com.webscare.volumenormalizer.ui.components.BottomTab
import com.webscare.volumenormalizer.ui.components.PickProblemsDialog
import com.webscare.volumenormalizer.ui.editor.EditorRoute
import com.webscare.volumenormalizer.ui.equalize.EqualizeRoute
import com.webscare.volumenormalizer.ui.equalize.EqualizeSettings
import com.webscare.volumenormalizer.ui.home.HomeScreen
import com.webscare.volumenormalizer.ui.home.HomeTool
import com.webscare.volumenormalizer.ui.normalize.NormalizeScreen
import com.webscare.volumenormalizer.ui.normalize.NormalizeSettings
import com.webscare.volumenormalizer.ui.processing.ProcessingScreen
import com.webscare.volumenormalizer.ui.result.ResultScreen
import com.webscare.volumenormalizer.ui.settings.SettingsRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private enum class AppScreen {
    Home, Editor, Normalize, Boost, Balance, Equalize, Batch, Processing, Result,
    Settings, MyFiles
}

private const val SPACE_MARGIN_BYTES = 20_000_000L
private const val LIMITER_NOTICE_DB = 0.1

@Composable
fun App() {
    AppTheme {
        var screen by remember { mutableStateOf(AppScreen.Home) }
        var selectedAudio by remember { mutableStateOf<PickedAudio?>(null) }
        var pendingTool by remember { mutableStateOf<HomeTool?>(null) }
        var problems by remember { mutableStateOf(emptyList<RejectedAudio>()) }

        val scope = rememberCoroutineScope()
        val processor = rememberAudioProcessor()

        var normalizeSettings by remember { mutableStateOf(NormalizeSettings()) }
        var boostSettings by remember { mutableStateOf(BoostSettings()) }
        var balanceSettings by remember { mutableStateOf(BalanceSettings()) }
        var equalizeSettings by remember { mutableStateOf(EqualizeSettings()) }

        var processingState by remember {
            mutableStateOf<ProcessingState>(ProcessingState.Running(null))
        }
        var processingJob by remember { mutableStateOf<Job?>(null) }
        var processingSource by remember { mutableStateOf(AppScreen.Normalize) }
        var processingOutput by remember { mutableStateOf<NormalizeOutput?>(null) }

        LaunchedEffect(Unit) {
            processor.clearTemporaryFiles()
        }

        val decoder = rememberAudioDecoder()
        var analysis by remember { mutableStateOf<AnalysisState>(AnalysisState.Analyzing(null)) }

        LaunchedEffect(selectedAudio?.uri) {
            val audio = selectedAudio ?: return@LaunchedEffect
            analysis = AnalysisState.Analyzing(null)
            val metrics = analyzeAudio(decoder, audio.uri)
            analysis = if (metrics != null) {
                AnalysisState.Done(metrics)
            } else {
                AnalysisState.Failed
            }
        }

        val canNormalize =
            (analysis as? AnalysisState.Done)?.metrics?.integratedLufs != null
        val canBoost = analysis is AnalysisState.Done && boostSettings.gainDb > 0

        fun runProcessing(
            source: AppScreen,
            audio: PickedAudio,
            before: AudioMetrics,
            plan: GainPlan,
            targetLufs: Int?,
            peakProtection: Boolean
        ) {
            processor.clearTemporaryFiles()
            processingOutput = null
            processingSource = source
            processingState = ProcessingState.Running(null)
            screen = AppScreen.Processing

            val requiredBytes =
                (audio.durationMs / 1000.0 * before.sampleRateHz * before.channels * 2).toLong()
            if (processor.freeSpaceBytes() < requiredBytes + SPACE_MARGIN_BYTES) {
                processingState = ProcessingState.Failed(ProcessFailure.NotEnoughSpace)
                return
            }

            val limiterCeiling = if (peakProtection) LIMITER_CEILING_DBTP else null

            processingJob = scope.launch {
                val outcome = processor.applyGain(
                    sourceUri = audio.uri,
                    gainDb = plan.appliedGainDb,
                    limiterCeilingDbtp = limiterCeiling
                ) { progress ->
                    processingState = ProcessingState.Running(progress)
                }
                when (outcome) {
                    is ProcessResult.Success -> {
                        val limited = outcome.audio.maxLimiterReductionDb > LIMITER_NOTICE_DB
                        processingOutput = NormalizeOutput(
                            before = before,
                            processed = outcome.audio,
                            plan = plan.copy(limitedByPeak = limited),
                            targetLufs = targetLufs
                        )
                        screen = AppScreen.Result
                    }
                    is ProcessResult.Failure -> {
                        processingState = ProcessingState.Failed(outcome.reason)
                    }
                }
            }
        }

        fun startNormalize() {
            val audio = selectedAudio ?: return
            val before = (analysis as? AnalysisState.Done)?.metrics ?: return
            val settings = normalizeSettings
            val plan = planGain(
                targetLufs = settings.targetLufs.toDouble(),
                metrics = before,
                peakProtection = settings.peakProtection
            ) ?: return
            runProcessing(
                source = AppScreen.Normalize,
                audio = audio,
                before = before,
                plan = plan,
                targetLufs = settings.targetLufs,
                peakProtection = settings.peakProtection
            )
        }

        fun startBoost() {
            val audio = selectedAudio ?: return
            val before = (analysis as? AnalysisState.Done)?.metrics ?: return
            val settings = boostSettings
            if (settings.gainDb <= 0) return
            val plan = planBoost(
                requestedGainDb = settings.gainDb.toDouble(),
                metrics = before,
                peakProtection = settings.peakProtection
            )
            runProcessing(
                source = AppScreen.Boost,
                audio = audio,
                before = before,
                plan = plan,
                targetLufs = null,
                peakProtection = settings.peakProtection
            )
        }

        fun startBalance() {
            val audio = selectedAudio ?: return
            val before = (analysis as? AnalysisState.Done)?.metrics ?: return
            val settings = balanceSettings

            processor.clearTemporaryFiles()
            processingOutput = null
            processingSource = AppScreen.Balance
            processingState = ProcessingState.Running(null)
            screen = AppScreen.Processing

            val requiredBytes = (audio.durationMs / 1000.0 * before.sampleRateHz * before.channels * 2).toLong()
            if (processor.freeSpaceBytes() < requiredBytes + SPACE_MARGIN_BYTES) {
                processingState = ProcessingState.Failed(ProcessFailure.NotEnoughSpace)
                return
            }

            processingJob = scope.launch {
                val outcome = processor.applyBalance(
                    sourceUri = audio.uri,
                    params = settings.toDspParams()
                ) { progress ->
                    processingState = ProcessingState.Running(progress)
                }

                when (outcome) {
                    is ProcessResult.Success -> {
                        val limited = outcome.audio.maxLimiterReductionDb > LIMITER_NOTICE_DB
                        processingOutput = NormalizeOutput(
                            before = before,
                            processed = outcome.audio,
                            plan = GainPlan(0.0, settings.toDspParams().makeupGainDb, limited),
                            targetLufs = null
                        )
                        screen = AppScreen.Result
                    }
                    is ProcessResult.Failure -> {
                        processingState = ProcessingState.Failed(outcome.reason)
                    }
                }
            }
        }

        fun startEqualize() {
            val audio = selectedAudio ?: return
            val before = (analysis as? AnalysisState.Done)?.metrics ?: return
            val settings = equalizeSettings

            processor.clearTemporaryFiles()
            processingOutput = null
            processingSource = AppScreen.Equalize
            processingState = ProcessingState.Running(null)
            screen = AppScreen.Processing

            val requiredBytes = (audio.durationMs / 1000.0 * before.sampleRateHz * before.channels * 2).toLong()
            if (processor.freeSpaceBytes() < requiredBytes + SPACE_MARGIN_BYTES) {
                processingState = ProcessingState.Failed(ProcessFailure.NotEnoughSpace)
                return
            }

            processingJob = scope.launch {
                val outcome = processor.applyEqualize(
                    sourceUri = audio.uri,
                    params = EqualizeDspParams(settings.gains),
                    onProgress = { progress ->
                        processingState = ProcessingState.Running(progress)
                    }
                )

                when (outcome) {
                    is ProcessResult.Success -> {
                        processingOutput = NormalizeOutput(
                            before = before,
                            processed = outcome.audio,
                            plan = GainPlan(0.0, 0.0, false),
                            targetLufs = null
                        )
                        screen = AppScreen.Result
                    }
                    is ProcessResult.Failure -> {
                        processingState = ProcessingState.Failed(outcome.reason)
                    }
                }
            }
        }

        fun cancelProcessing() {
            processingJob?.cancel()
            processingJob = null
            screen = processingSource
        }

        fun leaveResult() {
            processor.clearTemporaryFiles()
            processingOutput = null
            screen = processingSource
        }

        val audioPicker = rememberAudioPicker(allowMultiple = false) { result ->
            val audio = result.accepted.firstOrNull()
            if (audio != null) {
                selectedAudio = audio
                processingOutput = null
                processor.clearTemporaryFiles()

                // FIXED: HomeTool.Equalize hata diya kyunke wo HomeScreen par nahi hai
                screen = when (pendingTool) {
                    HomeTool.Normalize -> AppScreen.Normalize
                    HomeTool.Boost -> AppScreen.Boost
                    HomeTool.Balance -> AppScreen.Balance
                    else -> AppScreen.Editor
                }
            }
            if (result.rejected.isNotEmpty()) problems = result.rejected
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(AppColors.BackgroundTop, AppColors.BackgroundBottom)
                    )
                )
        ) {
            when (screen) {
                AppScreen.Home -> HomeScreen(
                    onSelectAudio = {
                        pendingTool = null
                        audioPicker.launch()
                    },
                    onToolClick = { tool ->
                        if (tool == HomeTool.Batch) {
                            screen = AppScreen.Batch
                        } else {
                            pendingTool = tool
                            audioPicker.launch()
                        }
                    },
                    onTabSelected = { tab ->
                        screen = when (tab) {
                            BottomTab.Home -> AppScreen.Home
                            BottomTab.MyFiles -> AppScreen.MyFiles
                            BottomTab.Settings -> AppScreen.Settings
                        }
                    }
                )

                AppScreen.Editor -> selectedAudio?.let { audio ->
                    EditorRoute(
                        audio = audio,
                        onBack = { screen = AppScreen.Home },
                        onNormalizeClick = { screen = AppScreen.Normalize },
                        onBoostClick = { screen = AppScreen.Boost },
                        onBalanceClick = { screen = AppScreen.Balance },
                        onEqualizeClick = { screen = AppScreen.Equalize }
                    )
                }

                AppScreen.Normalize -> selectedAudio?.let { audio ->
                    NormalizeScreen(
                        fileName = audio.displayName,
                        fileDetails = audio.detailsLine(),
                        preset = normalizeSettings.preset,
                        targetLufs = normalizeSettings.targetLufs,
                        analysis = analysis,
                        peakProtection = normalizeSettings.peakProtection,
                        onBack = { screen = AppScreen.Editor },
                        onPresetSelected = {
                            normalizeSettings = normalizeSettings.withPreset(it)
                        },
                        onTargetChanged = {
                            normalizeSettings = normalizeSettings.withTarget(it)
                        },
                        onPeakProtectionChange = {
                            normalizeSettings = normalizeSettings.copy(peakProtection = it)
                        },
                        onPreviewClick = { if (canNormalize) startNormalize() },
                        onNormalizeClick = { startNormalize() },
                        canStart = canNormalize
                    )
                }

                AppScreen.Boost -> selectedAudio?.let { audio ->
                    BoostScreen(
                        fileName = audio.displayName,
                        fileDetails = audio.detailsLine(),
                        gainDb = boostSettings.gainDb,
                        peakProtection = boostSettings.peakProtection,
                        analysis = analysis,
                        onBack = { screen = AppScreen.Editor },
                        onGainChanged = {
                            boostSettings = boostSettings.copy(gainDb = it)
                        },
                        onLevelSelected = {
                            boostSettings = boostSettings.copy(gainDb = it.gainDb)
                        },
                        onPeakProtectionChange = {
                            boostSettings = boostSettings.copy(peakProtection = it)
                        },
                        onPreviewClick = { if (canBoost) startBoost() },
                        onApplyClick = { startBoost() }
                    )
                }

                AppScreen.Processing -> selectedAudio?.let { audio ->
                    ProcessingScreen(
                        fileName = audio.displayName,
                        fileDetails = audio.detailsLine(),
                        state = processingState,
                        onCancel = { cancelProcessing() }
                    )
                }

                AppScreen.Result -> selectedAudio?.let { audio ->
                    processingOutput?.let { output ->
                        ResultScreen(
                            fileName = audio.displayName,
                            fileDetails = audio.detailsLine(),
                            originalUri = audio.uri,
                            originalDurationMs = audio.durationMs,
                            output = output,
                            isBoost = processingSource == AppScreen.Boost,
                            onBack = { leaveResult() },
                            onEditAgain = { leaveResult() },
                            onContinue = { /* Save screen: next step */ }
                        )
                    }
                }

                AppScreen.Balance -> selectedAudio?.let { audio ->
                    BalanceRoute(
                        fileName = audio.displayName,
                        fileDetails = audio.detailsLine(),
                        settings = balanceSettings,
                        onSettingsChange = { balanceSettings = it },
                        onBack = { screen = AppScreen.Editor },
                        onApply = { startBalance() }
                    )
                }

                AppScreen.Equalize -> selectedAudio?.let { audio ->
                    EqualizeRoute(
                        fileName = audio.displayName,
                        fileDetails = audio.detailsLine(),
                        settings = equalizeSettings,
                        onSettingsChange = { equalizeSettings = it },
                        onBack = { screen = AppScreen.Editor },
                        onApply = { startEqualize() }
                    )
                }

                AppScreen.Batch -> BatchRoute(onBack = { screen = AppScreen.Home })

                AppScreen.Settings -> SettingsRoute(
                    onBack = { screen = AppScreen.Home }
                )

                AppScreen.MyFiles -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.Text(
                            text = "My Files - Coming Soon",
                            color = AppColors.TextPrimary
                        )
                    }
                }
            }

            PickProblemsDialog(
                rejected = problems,
                onDismiss = { problems = emptyList() }
            )
        }
    }
}