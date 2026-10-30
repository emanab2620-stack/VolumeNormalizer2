package com.webscare.volumenormalizer.ui.boost

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webscare.volumenormalizer.AnalysisState
import com.webscare.volumenormalizer.formatLufs
import com.webscare.volumenormalizer.planBoost
import com.webscare.volumenormalizer.theme.AppColors
import com.webscare.volumenormalizer.ui.components.ActionRowCard
import com.webscare.volumenormalizer.ui.components.AppCard
import com.webscare.volumenormalizer.ui.components.FileInfoRow
import com.webscare.volumenormalizer.ui.components.PrimaryButton
import com.webscare.volumenormalizer.ui.components.ToggleCard
import com.webscare.volumenormalizer.ui.components.ToolTopBar
import io.github.hlcaptain.symbols.Symbols
import io.github.hlcaptain.symbols.material.Material
import io.github.hlcaptain.symbols.material.Rounded
import io.github.hlcaptain.symbols.material.rounded.vectors.ChevronRight
import io.github.hlcaptain.symbols.material.rounded.vectors.Info
import io.github.hlcaptain.symbols.material.rounded.vectors.PlayArrow
import io.github.hlcaptain.symbols.material.rounded.vectors.Shield
import io.github.hlcaptain.symbols.material.rounded.vectors.SignalCellularAlt
import org.jetbrains.compose.resources.stringResource
import volumenormalizer.shared.generated.resources.Res
import volumenormalizer.shared.generated.resources.boost_analysis_failed
import volumenormalizer.shared.generated.resources.boost_analyzing
import volumenormalizer.shared.generated.resources.boost_apply
import volumenormalizer.shared.generated.resources.boost_caption_protected
import volumenormalizer.shared.generated.resources.boost_caption_unprotected
import volumenormalizer.shared.generated.resources.boost_clipping_warning
import volumenormalizer.shared.generated.resources.boost_headroom_limited
import volumenormalizer.shared.generated.resources.boost_level_title
import volumenormalizer.shared.generated.resources.boost_max
import volumenormalizer.shared.generated.resources.boost_medium
import volumenormalizer.shared.generated.resources.boost_no_headroom
import volumenormalizer.shared.generated.resources.boost_slider_description
import volumenormalizer.shared.generated.resources.boost_slider_description_zero
import volumenormalizer.shared.generated.resources.boost_strong
import volumenormalizer.shared.generated.resources.boost_subtle
import volumenormalizer.shared.generated.resources.boost_tick_max
import volumenormalizer.shared.generated.resources.boost_title
import volumenormalizer.shared.generated.resources.boost_value
import volumenormalizer.shared.generated.resources.boost_value_zero
import volumenormalizer.shared.generated.resources.boost_warning_protected
import volumenormalizer.shared.generated.resources.boost_warning_unprotected
import volumenormalizer.shared.generated.resources.peak_protection_description
import volumenormalizer.shared.generated.resources.peak_protection_title
import volumenormalizer.shared.generated.resources.preview_before_after
import kotlin.math.roundToInt

private const val MIN_GAIN_DB = 0
private const val MAX_GAIN_DB = 12
private const val STRONG_WARNING_DB = 9

/** Shortcut levels. The dB values are an assumption until confirmed (they match the slider ticks). */
enum class BoostLevel(val gainDb: Int, val filledBars: Int) {
    Subtle(3, 1),
    Medium(6, 2),
    Strong(9, 3),
    Max(12, 4)
}

/** What the user chose on the Boost screen. Kept in App.kt so Cancel does not lose it. */
data class BoostSettings(
    val gainDb: Int = BoostLevel.Medium.gainDb,
    val peakProtection: Boolean = true
)

@Composable
fun BoostScreen(
    fileName: String,
    fileDetails: String,
    gainDb: Int,
    peakProtection: Boolean,
    analysis: AnalysisState,
    onBack: () -> Unit,
    onGainChanged: (Int) -> Unit,
    onLevelSelected: (BoostLevel) -> Unit,
    onPeakProtectionChange: (Boolean) -> Unit,
    onPreviewClick: () -> Unit,
    onApplyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The boost needs the measurement (for the peak room) and a gain above zero
    val canApply = analysis is AnalysisState.Done && gainDb > 0

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        ToolTopBar(
            title = stringResource(Res.string.boost_title),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FileInfoRow(
                fileName = fileName,
                details = fileDetails,
                onMoreClick = { }
            )

            BoostLevelCard(
                gainDb = gainDb,
                peakProtection = peakProtection,
                analysis = analysis,
                onGainChanged = onGainChanged
            )

            BoostLevelRow(
                gainDb = gainDb,
                onLevelSelected = onLevelSelected
            )

            ToggleCard(
                icon = Symbols.Material.Rounded.Shield,
                iconTint = AppColors.Boost,
                title = stringResource(Res.string.peak_protection_title),
                description = stringResource(Res.string.peak_protection_description),
                checked = peakProtection,
                onCheckedChange = onPeakProtectionChange
            )

            ActionRowCard(
                icon = Symbols.Material.Rounded.PlayArrow,
                text = stringResource(Res.string.preview_before_after),
                onClick = onPreviewClick
            )

            Spacer(Modifier.height(4.dp))
        }

        PrimaryButton(
            text = stringResource(Res.string.boost_apply),
            onClick = onApplyClick,
            enabled = canApply,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            leadingContent = {
                Icon(
                    imageVector = Symbols.Material.Rounded.SignalCellularAlt,
                    contentDescription = null,
                    tint = AppColors.Boost
                )
                Spacer(Modifier.width(8.dp))
            },
            trailingContent = {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Symbols.Material.Rounded.ChevronRight,
                    contentDescription = null
                )
            }
        )
    }
}

@Composable
private fun BoostLevelCard(
    gainDb: Int,
    peakProtection: Boolean,
    analysis: AnalysisState,
    onGainChanged: (Int) -> Unit
) {
    val valueText = if (gainDb == 0) {
        stringResource(Res.string.boost_value_zero)
    } else {
        stringResource(Res.string.boost_value, gainDb)
    }
    val sliderDescription = if (gainDb == 0) {
        stringResource(Res.string.boost_slider_description_zero)
    } else {
        stringResource(Res.string.boost_slider_description, gainDb)
    }
    val caption = if (peakProtection) {
        stringResource(Res.string.boost_caption_protected)
    } else {
        stringResource(Res.string.boost_caption_unprotected)
    }

    // What we know about this file decides which notes are shown
    val metrics = (analysis as? AnalysisState.Done)?.metrics
    val plan = if (metrics != null && gainDb > 0) {
        planBoost(gainDb.toDouble(), metrics, peakProtection)
    } else {
        null
    }

    val statusText: String? = when (analysis) {
        is AnalysisState.Analyzing -> stringResource(Res.string.boost_analyzing)
        is AnalysisState.Failed -> stringResource(Res.string.boost_analysis_failed)
        is AnalysisState.Done -> null
    }

    val limitText: String? = if (plan != null && plan.limitedByPeak) {
        if (plan.appliedGainDb < 0.1) {
            stringResource(Res.string.boost_no_headroom)
        } else {
            stringResource(Res.string.boost_headroom_limited, formatLufs(plan.appliedGainDb))
        }
    } else {
        null
    }

    val samplePeakDb = metrics?.samplePeakDbfs
    val willClip = !peakProtection && samplePeakDb != null && samplePeakDb + gainDb > 0.0

    val notes = listOfNotNull(
        limitText,
        if (willClip) stringResource(Res.string.boost_clipping_warning) else null,
        if (gainDb >= STRONG_WARNING_DB) {
            stringResource(
                if (peakProtection) {
                    Res.string.boost_warning_protected
                } else {
                    Res.string.boost_warning_unprotected
                }
            )
        } else {
            null
        }
    )

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.boost_level_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Symbols.Material.Rounded.Info,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = valueText,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.Boost,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Slider(
            value = gainDb.toFloat(),
            onValueChange = { onGainChanged(it.roundToInt()) },
            valueRange = MIN_GAIN_DB.toFloat()..MAX_GAIN_DB.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = AppColors.Boost,
                activeTrackColor = AppColors.Boost,
                inactiveTrackColor = AppColors.Divider
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = sliderDescription }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("0", "+3", "+6", "+9", stringResource(Res.string.boost_tick_max)).forEach { tick ->
                Text(
                    text = tick,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary
                )
            }
        }

        if (statusText != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (analysis is AnalysisState.Failed) AppColors.Balance else AppColors.TextSecondary
            )
        }

        notes.forEach { note ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Balance
            )
        }
    }
}

@Composable
private fun BoostLevelRow(
    gainDb: Int,
    onLevelSelected: (BoostLevel) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BoostLevel.entries.forEach { level ->
            LevelTile(
                label = levelLabel(level),
                filledBars = level.filledBars,
                isSelected = level.gainDb == gainDb,
                onClick = { onLevelSelected(level) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun levelLabel(level: BoostLevel): String = when (level) {
    BoostLevel.Subtle -> stringResource(Res.string.boost_subtle)
    BoostLevel.Medium -> stringResource(Res.string.boost_medium)
    BoostLevel.Strong -> stringResource(Res.string.boost_strong)
    BoostLevel.Max -> stringResource(Res.string.boost_max)
}

@Composable
private fun LevelTile(
    label: String,
    filledBars: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val iconColor = if (isSelected) AppColors.Boost else AppColors.TextSecondary
    val textColor = if (isSelected) AppColors.Boost else AppColors.TextPrimary
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(if (isSelected) AppColors.Boost.copy(alpha = 0.12f) else AppColors.Surface)
            .border(1.dp, if (isSelected) AppColors.Boost else AppColors.Divider, shape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LevelBarsIcon(filledBars = filledBars, tint = iconColor)
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}

/** 4 rising bars; the first [filledBars] are bright, the rest are dim. */
@Composable
private fun LevelBarsIcon(
    filledBars: Int,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.size(24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (index in 1..4) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((4 + 4 * index).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (index <= filledBars) tint else tint.copy(alpha = 0.3f))
            )
        }
    }
}