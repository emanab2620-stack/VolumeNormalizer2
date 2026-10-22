package com.webscare.volumenormalizer.ui.balance

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webscare.volumenormalizer.theme.AppColors
import com.webscare.volumenormalizer.ui.components.ActionRowCard
import com.webscare.volumenormalizer.ui.components.AppCard
import com.webscare.volumenormalizer.ui.components.FileInfoRow
import com.webscare.volumenormalizer.ui.components.PrimaryButton
import com.webscare.volumenormalizer.ui.components.ToggleRow
import com.webscare.volumenormalizer.ui.components.ToolTopBar
import io.github.hlcaptain.symbols.Symbols
import io.github.hlcaptain.symbols.material.Material
import io.github.hlcaptain.symbols.material.Rounded
import io.github.hlcaptain.symbols.material.rounded.vectors.ChevronRight
import io.github.hlcaptain.symbols.material.rounded.vectors.GraphicEq
import io.github.hlcaptain.symbols.material.rounded.vectors.Info
import io.github.hlcaptain.symbols.material.rounded.vectors.Mic
import io.github.hlcaptain.symbols.material.rounded.vectors.MusicNote
import io.github.hlcaptain.symbols.material.rounded.vectors.PlayArrow
import io.github.hlcaptain.symbols.material.rounded.vectors.Tune
import org.jetbrains.compose.resources.stringResource
import volumenormalizer.shared.generated.resources.Res
import volumenormalizer.shared.generated.resources.balance_after
import volumenormalizer.shared.generated.resources.balance_apply
import volumenormalizer.shared.generated.resources.balance_before
import volumenormalizer.shared.generated.resources.balance_help
import volumenormalizer.shared.generated.resources.balance_intensity_description
import volumenormalizer.shared.generated.resources.balance_intensity_high
import volumenormalizer.shared.generated.resources.balance_intensity_low
import volumenormalizer.shared.generated.resources.balance_intensity_medium
import volumenormalizer.shared.generated.resources.balance_intensity_slider_description
import volumenormalizer.shared.generated.resources.balance_intensity_title
import volumenormalizer.shared.generated.resources.balance_natural_dynamics_description
import volumenormalizer.shared.generated.resources.balance_natural_dynamics_title
import volumenormalizer.shared.generated.resources.balance_profile_mixed
import volumenormalizer.shared.generated.resources.balance_profile_music
import volumenormalizer.shared.generated.resources.balance_profile_speech
import volumenormalizer.shared.generated.resources.balance_protect_peaks_description
import volumenormalizer.shared.generated.resources.balance_protect_peaks_title
import volumenormalizer.shared.generated.resources.balance_title
import volumenormalizer.shared.generated.resources.balance_type_title
import volumenormalizer.shared.generated.resources.balance_visual_description
import volumenormalizer.shared.generated.resources.preview_before_after
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private const val BAR_COUNT_PER_SIDE = 22
private const val TARGET_LEVEL = 0.55f   // loudness that the "After" bars move toward

// TODO: mock values until the importer exists
private const val MOCK_FILE_NAME = "Podcast_Interview.mp3"
private const val MOCK_FILE_DETAILS = "MP3 · 12.4 MB · 03:42"

enum class BalanceProfile { Speech, Music, Mixed }

private enum class IntensityLevel { Low, Medium, High }

private fun intensityLevel(intensity: Float): IntensityLevel = when {
    intensity < 0.34f -> IntensityLevel.Low
    intensity < 0.67f -> IntensityLevel.Medium
    else -> IntensityLevel.High
}

/** Holds the screen state. Rotation and ViewModel handling come in a later step. */
@Composable
fun BalanceRoute(
    fileName: String,
    fileDetails: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var profile by remember { mutableStateOf(BalanceProfile.Speech) }
    var intensity by remember { mutableStateOf(0.5f) }
    var protectPeaks by remember { mutableStateOf(true) }
    var naturalDynamics by remember { mutableStateOf(true) }

    BalanceScreen(
        fileName = fileName,
        fileDetails = fileDetails,
        profile = profile,
        intensity = intensity,
        protectPeaks = protectPeaks,
        naturalDynamics = naturalDynamics,
        onBack = onBack,
        onProfileSelected = { profile = it },
        onIntensityChanged = { intensity = it },
        onProtectPeaksChange = { protectPeaks = it },
        onNaturalDynamicsChange = { naturalDynamics = it },
        onPreviewClick = { /* preview: later step */ },
        onApplyClick = { /* processing: later step */ },
        modifier = modifier
    )
}

@Composable
fun BalanceScreen(
    fileName: String,
    fileDetails: String,
    profile: BalanceProfile,
    intensity: Float,
    protectPeaks: Boolean,
    naturalDynamics: Boolean,
    onBack: () -> Unit,
    onProfileSelected: (BalanceProfile) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    onProtectPeaksChange: (Boolean) -> Unit,
    onNaturalDynamicsChange: (Boolean) -> Unit,
    onPreviewClick: () -> Unit,
    onApplyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        ToolTopBar(
            title = stringResource(Res.string.balance_title),
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

            BalanceTypeCard(
                profile = profile,
                intensity = intensity,
                onProfileSelected = onProfileSelected
            )

            IntensityCard(
                intensity = intensity,
                onIntensityChanged = onIntensityChanged
            )

            AppCard(modifier = Modifier.fillMaxWidth()) {
                ToggleRow(
                    icon = Symbols.Material.Rounded.GraphicEq,
                    iconTint = AppColors.Balance,
                    title = stringResource(Res.string.balance_protect_peaks_title),
                    description = stringResource(Res.string.balance_protect_peaks_description),
                    checked = protectPeaks,
                    onCheckedChange = onProtectPeaksChange
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AppColors.Divider)
                )
                Spacer(Modifier.height(12.dp))
                ToggleRow(
                    icon = Symbols.Material.Rounded.Tune,
                    iconTint = AppColors.Balance,
                    title = stringResource(Res.string.balance_natural_dynamics_title),
                    description = stringResource(Res.string.balance_natural_dynamics_description),
                    checked = naturalDynamics,
                    onCheckedChange = onNaturalDynamicsChange
                )
            }

            ActionRowCard(
                icon = Symbols.Material.Rounded.PlayArrow,
                text = stringResource(Res.string.preview_before_after),
                onClick = onPreviewClick
            )

            Spacer(Modifier.height(4.dp))
        }

        PrimaryButton(
            text = stringResource(Res.string.balance_apply),
            onClick = onApplyClick,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            leadingContent = {
                Icon(
                    imageVector = Symbols.Material.Rounded.Tune,
                    contentDescription = null,
                    tint = AppColors.Balance
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
private fun BalanceTypeCard(
    profile: BalanceProfile,
    intensity: Float,
    onProfileSelected: (BalanceProfile) -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.balance_type_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Symbols.Material.Rounded.Info,
                contentDescription = stringResource(Res.string.balance_help),
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BalanceProfile.entries.forEach { item ->
                ProfileTile(
                    label = profileLabel(item),
                    icon = profileIcon(item),
                    isSelected = item == profile,
                    onClick = { onProfileSelected(item) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        BeforeAfterWave(
            intensity = intensity,
            description = stringResource(Res.string.balance_visual_description)
        )

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.balance_before),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(Res.string.balance_after),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Balance,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun profileLabel(profile: BalanceProfile): String = when (profile) {
    BalanceProfile.Speech -> stringResource(Res.string.balance_profile_speech)
    BalanceProfile.Music -> stringResource(Res.string.balance_profile_music)
    BalanceProfile.Mixed -> stringResource(Res.string.balance_profile_mixed)
}

private fun profileIcon(profile: BalanceProfile): ImageVector = when (profile) {
    BalanceProfile.Speech -> Symbols.Material.Rounded.Mic
    BalanceProfile.Music -> Symbols.Material.Rounded.MusicNote
    BalanceProfile.Mixed -> Symbols.Material.Rounded.GraphicEq
}

@Composable
private fun ProfileTile(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val contentColor = if (isSelected) AppColors.Balance else AppColors.TextPrimary
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(if (isSelected) AppColors.Balance.copy(alpha = 0.12f) else AppColors.Surface)
            .border(1.dp, if (isSelected) AppColors.Balance else AppColors.Divider, shape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1
        )
    }
}

/**
 * Illustration only (not the real audio): left = uneven loud/quiet bars (Before),
 * right = the same bars pulled toward one level (After). A higher intensity pulls them closer.
 */
@Composable
private fun BeforeAfterWave(
    intensity: Float,
    description: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .semantics { contentDescription = description }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val half = size.width / 2f
            val centerY = size.height / 2f
            val slot = half / BAR_COUNT_PER_SIDE
            val barWidth = slot * 0.5f
            val maxHalfHeight = size.height / 2f - barWidth / 2f - 1.dp.toPx()
            val beforeColor = AppColors.TextSecondary.copy(alpha = 0.8f)
            val afterColor = AppColors.Balance

            for (i in 0 until BAR_COUNT_PER_SIDE) {
                val before = barPattern(i)
                val after = (TARGET_LEVEL + (before - TARGET_LEVEL) * (1f - 0.85f * intensity))
                    .coerceIn(0.1f, 1f)

                val xBefore = slot * (i + 0.5f)
                val xAfter = half + slot * (i + 0.5f)

                drawLine(
                    color = beforeColor,
                    start = Offset(xBefore, centerY - before * maxHalfHeight),
                    end = Offset(xBefore, centerY + before * maxHalfHeight),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = afterColor,
                    start = Offset(xAfter, centerY - after * maxHalfHeight),
                    end = Offset(xAfter, centerY + after * maxHalfHeight),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }

            drawLine(
                color = AppColors.Balance,
                start = Offset(half, 0f),
                end = Offset(half, size.height),
                strokeWidth = 2.dp.toPx()
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(28.dp)
                .clip(CircleShape)
                .background(AppColors.BackgroundTop)
                .border(1.dp, AppColors.Balance, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Symbols.Material.Rounded.ChevronRight,
                contentDescription = null,
                tint = AppColors.Balance,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** A fixed pattern of loud and quiet bars (0.14 to 1.0). */
private fun barPattern(index: Int): Float {
    val value = abs(sin(index * 0.9f) * sin(index * 0.37f + 1.3f)) +
            0.15f * abs(sin(index * 2.3f))
    return (0.14f + 0.86f * value).coerceIn(0.14f, 1f)
}

@Composable
private fun IntensityCard(
    intensity: Float,
    onIntensityChanged: (Float) -> Unit
) {
    val level = intensityLevel(intensity)
    val levelText = intensityLabel(level)
    val sliderDescription = stringResource(Res.string.balance_intensity_slider_description)

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.balance_intensity_title),
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
            Spacer(Modifier.weight(1f))
            Text(
                text = levelText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Balance
            )
        }

        Spacer(Modifier.height(8.dp))

        Slider(
            value = intensity,
            onValueChange = { onIntensityChanged((it * 10f).roundToInt() / 10f) },
            valueRange = 0f..1f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = AppColors.Balance,
                activeTrackColor = AppColors.Balance,
                inactiveTrackColor = AppColors.Divider,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = sliderDescription
                    stateDescription = levelText
                }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = stringResource(Res.string.balance_intensity_low),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(Res.string.balance_intensity_medium),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(Res.string.balance_intensity_high),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(Res.string.balance_intensity_description),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
private fun intensityLabel(level: IntensityLevel): String = when (level) {
    IntensityLevel.Low -> stringResource(Res.string.balance_intensity_low)
    IntensityLevel.Medium -> stringResource(Res.string.balance_intensity_medium)
    IntensityLevel.High -> stringResource(Res.string.balance_intensity_high)
}