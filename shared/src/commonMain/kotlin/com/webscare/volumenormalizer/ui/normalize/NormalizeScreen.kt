package com.webscare.volumenormalizer.ui.normalize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webscare.volumenormalizer.AnalysisState
import com.webscare.volumenormalizer.formatLufs
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
import io.github.hlcaptain.symbols.material.rounded.vectors.ArrowForward
import io.github.hlcaptain.symbols.material.rounded.vectors.ChevronRight
import io.github.hlcaptain.symbols.material.rounded.vectors.GraphicEq
import io.github.hlcaptain.symbols.material.rounded.vectors.Info
import io.github.hlcaptain.symbols.material.rounded.vectors.Mic
import io.github.hlcaptain.symbols.material.rounded.vectors.MusicNote
import io.github.hlcaptain.symbols.material.rounded.vectors.Person
import io.github.hlcaptain.symbols.material.rounded.vectors.PlayArrow
import io.github.hlcaptain.symbols.material.rounded.vectors.Settings
import io.github.hlcaptain.symbols.material.rounded.vectors.Shield
import org.jetbrains.compose.resources.stringResource
import volumenormalizer.shared.generated.resources.Res
import volumenormalizer.shared.generated.resources.lufs_not_measured
import volumenormalizer.shared.generated.resources.lufs_text
import volumenormalizer.shared.generated.resources.lufs_value
import volumenormalizer.shared.generated.resources.normalize_analyzing
import volumenormalizer.shared.generated.resources.normalize_caption_custom
import volumenormalizer.shared.generated.resources.normalize_caption_music
import volumenormalizer.shared.generated.resources.normalize_caption_voice
import volumenormalizer.shared.generated.resources.normalize_comparison_title
import volumenormalizer.shared.generated.resources.normalize_cta
import volumenormalizer.shared.generated.resources.normalize_current
import volumenormalizer.shared.generated.resources.normalize_output
import volumenormalizer.shared.generated.resources.normalize_slider_description
import volumenormalizer.shared.generated.resources.normalize_target_loudness
import volumenormalizer.shared.generated.resources.normalize_title
import volumenormalizer.shared.generated.resources.peak_protection_description
import volumenormalizer.shared.generated.resources.peak_protection_title
import volumenormalizer.shared.generated.resources.preset_custom
import volumenormalizer.shared.generated.resources.preset_music
import volumenormalizer.shared.generated.resources.preset_podcast
import volumenormalizer.shared.generated.resources.preset_voice
import volumenormalizer.shared.generated.resources.preview_before_after
import kotlin.math.roundToInt

private const val MIN_LUFS = -30
private const val MAX_LUFS = 0
private val MeterRed = Color(0xFFE5484D)

/** Illustrative starting points from the spec (not platform compliance claims). */
enum class NormalizePreset(val defaultTargetLufs: Int?) {
    Podcast(-16),
    Music(-14),
    Voice(-16),
    Custom(null)
}

/** Holds the screen state. Rotation and ViewModel handling come in a later step. */
@Composable
fun NormalizeRoute(
    fileName: String,
    fileDetails: String,
    analysis: AnalysisState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var preset by remember { mutableStateOf(NormalizePreset.Podcast) }
    var targetLufs by remember { mutableStateOf(NormalizePreset.Podcast.defaultTargetLufs ?: -16) }
    var peakProtection by remember { mutableStateOf(true) }

    NormalizeScreen(
        fileName = fileName,
        fileDetails = fileDetails,
        preset = preset,
        targetLufs = targetLufs,
        analysis = analysis,
        peakProtection = peakProtection,
        onBack = onBack,
        onPresetSelected = { selected ->
            preset = selected
            selected.defaultTargetLufs?.let { targetLufs = it }
        },
        onTargetChanged = { newTarget ->
            if (newTarget != targetLufs) {
                targetLufs = newTarget
                preset = NormalizePreset.Custom
            }
        },
        onPeakProtectionChange = { peakProtection = it },
        onPreviewClick = { /* preview: later step */ },
        onNormalizeClick = { /* processing: next step */ },
        modifier = modifier
    )
}

@Composable
fun NormalizeScreen(
    fileName: String,
    fileDetails: String,
    preset: NormalizePreset,
    targetLufs: Int,
    analysis: AnalysisState,
    peakProtection: Boolean,
    onBack: () -> Unit,
    onPresetSelected: (NormalizePreset) -> Unit,
    onTargetChanged: (Int) -> Unit,
    onPeakProtectionChange: (Boolean) -> Unit,
    onPreviewClick: () -> Unit,
    onNormalizeClick: () -> Unit,
    canStart: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        ToolTopBar(
            title = stringResource(Res.string.normalize_title),
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

            TargetLoudnessCard(
                preset = preset,
                targetLufs = targetLufs,
                onTargetChanged = onTargetChanged
            )

            PresetRow(
                selected = preset,
                onPresetSelected = onPresetSelected
            )

            ComparisonCard(
                analysis = analysis,
                outputLufs = targetLufs
            )

            ToggleCard(
                icon = Symbols.Material.Rounded.Shield,
                iconTint = AppColors.Primary,
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
            text = stringResource(Res.string.normalize_cta),
            onClick = onNormalizeClick,
            enabled = canStart,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            leadingContent = {
                Icon(
                    imageVector = Symbols.Material.Rounded.GraphicEq,
                    contentDescription = null
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
private fun TargetLoudnessCard(
    preset: NormalizePreset,
    targetLufs: Int,
    onTargetChanged: (Int) -> Unit
) {
    val caption = when (preset) {
        NormalizePreset.Podcast, NormalizePreset.Voice ->
            stringResource(Res.string.normalize_caption_voice)
        NormalizePreset.Music -> stringResource(Res.string.normalize_caption_music)
        NormalizePreset.Custom -> stringResource(Res.string.normalize_caption_custom)
    }
    val lufsText = stringResource(Res.string.lufs_value, targetLufs)
    val sliderDescription = stringResource(Res.string.normalize_slider_description)

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.normalize_target_loudness),
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.Primary.copy(alpha = 0.14f))
                    .border(1.dp, AppColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = lufsText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Primary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = lufsText,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.Primary,
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
            value = targetLufs.toFloat(),
            onValueChange = { onTargetChanged(it.roundToInt()) },
            valueRange = MIN_LUFS.toFloat()..MAX_LUFS.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = AppColors.Primary,
                activeTrackColor = AppColors.Primary,
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
            listOf("-30", "-24", "-18", "-12", "-6", "0").forEach { tick ->
                Text(
                    text = tick,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun PresetRow(
    selected: NormalizePreset,
    onPresetSelected: (NormalizePreset) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NormalizePreset.entries.forEach { preset ->
            PresetTile(
                label = presetLabel(preset),
                icon = presetIcon(preset),
                isSelected = preset == selected,
                onClick = { onPresetSelected(preset) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun presetLabel(preset: NormalizePreset): String = when (preset) {
    NormalizePreset.Podcast -> stringResource(Res.string.preset_podcast)
    NormalizePreset.Music -> stringResource(Res.string.preset_music)
    NormalizePreset.Voice -> stringResource(Res.string.preset_voice)
    NormalizePreset.Custom -> stringResource(Res.string.preset_custom)
}

private fun presetIcon(preset: NormalizePreset): ImageVector = when (preset) {
    NormalizePreset.Podcast -> Symbols.Material.Rounded.Mic
    NormalizePreset.Music -> Symbols.Material.Rounded.MusicNote
    NormalizePreset.Voice -> Symbols.Material.Rounded.Person
    NormalizePreset.Custom -> Symbols.Material.Rounded.Settings
}

@Composable
private fun PresetTile(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val contentColor = if (isSelected) AppColors.BackgroundTop else AppColors.TextPrimary
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(if (isSelected) AppColors.Primary else AppColors.Surface)
            .border(1.dp, if (isSelected) AppColors.Primary else AppColors.Divider, shape)
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

@Composable
private fun ComparisonCard(
    analysis: AnalysisState,
    outputLufs: Int
) {
    val currentLufs = (analysis as? AnalysisState.Done)?.metrics?.integratedLufs
    val currentText = when {
        analysis is AnalysisState.Analyzing -> stringResource(Res.string.normalize_analyzing)
        currentLufs != null -> stringResource(Res.string.lufs_text, formatLufs(currentLufs))
        else -> stringResource(Res.string.lufs_not_measured)
    }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.normalize_comparison_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LufsBox(
                label = stringResource(Res.string.normalize_current),
                value = currentText,
                level = currentLufs?.let { levelFraction(it) },
                meterColor = MeterRed,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Symbols.Material.Rounded.ArrowForward,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            LufsBox(
                label = stringResource(Res.string.normalize_output),
                value = stringResource(Res.string.lufs_value, outputLufs),
                level = levelFraction(outputLufs.toDouble()),
                meterColor = AppColors.Primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun levelFraction(lufs: Double): Float =
    ((lufs - MIN_LUFS) / (MAX_LUFS - MIN_LUFS)).toFloat().coerceIn(0f, 1f)

@Composable
private fun LufsBox(
    label: String,
    value: String,
    level: Float?,
    meterColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(AppColors.BackgroundTop)
            .border(1.dp, AppColors.Divider, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        LevelMeter(level = level, activeColor = meterColor)
    }
}

/** Small decorative level indicator: 10 segments, filled from the left. */
@Composable
private fun LevelMeter(
    level: Float?,
    activeColor: Color
) {
    val total = 10
    val filled = ((level ?: 0f) * total).roundToInt().coerceIn(0, total)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(width = 5.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index < filled) activeColor else AppColors.Divider)
            )
        }
    }
}