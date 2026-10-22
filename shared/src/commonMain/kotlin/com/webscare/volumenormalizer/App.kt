package com.webscare.volumenormalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

import com.webscare.volumenormalizer.platform.PickedAudio
import com.webscare.volumenormalizer.platform.RejectedAudio
import com.webscare.volumenormalizer.platform.detailsLine
import com.webscare.volumenormalizer.platform.rememberAudioDecoder
import com.webscare.volumenormalizer.platform.rememberAudioPicker
import com.webscare.volumenormalizer.theme.AppColors
import com.webscare.volumenormalizer.theme.AppTheme
import com.webscare.volumenormalizer.ui.balance.BalanceRoute
import com.webscare.volumenormalizer.ui.batch.BatchRoute
import com.webscare.volumenormalizer.ui.boost.BoostRoute
import com.webscare.volumenormalizer.ui.components.PickProblemsDialog
import com.webscare.volumenormalizer.ui.editor.EditorRoute
import com.webscare.volumenormalizer.ui.home.HomeScreen
import com.webscare.volumenormalizer.ui.home.HomeTool
import com.webscare.volumenormalizer.ui.normalize.NormalizeRoute

// Temporary screen switch for the UI phase. Replaced by real navigation in the navigation step.
private enum class AppScreen { Home, Editor, Normalize, Boost, Balance, Batch }

@Composable
fun App() {
    AppTheme {
        var screen by remember { mutableStateOf(AppScreen.Home) }
        var selectedAudio by remember { mutableStateOf<PickedAudio?>(null) }
        var pendingTool by remember { mutableStateOf<HomeTool?>(null) }
        var problems by remember { mutableStateOf(emptyList<RejectedAudio>()) }

        // Measurement of the chosen file. It starts as soon as a file is chosen,
        // and stops by itself if another file is chosen or the app closes.
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

        val audioPicker = rememberAudioPicker(allowMultiple = false) { result ->
            val audio = result.accepted.firstOrNull()
            if (audio != null) {
                selectedAudio = audio
                // Quick tool: go straight to that tool. Select Audio: open the Editor.
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
                    onTabSelected = { }
                )

                AppScreen.Editor -> selectedAudio?.let { audio ->
                    EditorRoute(
                        audio = audio,
                        onBack = { screen = AppScreen.Home },
                        onNormalizeClick = { screen = AppScreen.Normalize },
                        onBoostClick = { screen = AppScreen.Boost }
                    )
                }

                AppScreen.Normalize -> selectedAudio?.let { audio ->
                    NormalizeRoute(
                        fileName = audio.displayName,
                        fileDetails = audio.detailsLine(),
                        analysis = analysis,
                        onBack = { screen = AppScreen.Editor }
                    )
                }

                AppScreen.Boost -> selectedAudio?.let { audio ->
                    BoostRoute(
                        fileName = audio.displayName,
                        fileDetails = audio.detailsLine(),
                        onBack = { screen = AppScreen.Editor }
                    )
                }

                AppScreen.Balance -> selectedAudio?.let { audio ->
                    BalanceRoute(
                        fileName = audio.displayName,
                        fileDetails = audio.detailsLine(),
                        onBack = { screen = AppScreen.Editor }
                    )
                }

                AppScreen.Batch -> BatchRoute(onBack = { screen = AppScreen.Home })
            }

            PickProblemsDialog(
                rejected = problems,
                onDismiss = { problems = emptyList() }
            )
        }
    }
}