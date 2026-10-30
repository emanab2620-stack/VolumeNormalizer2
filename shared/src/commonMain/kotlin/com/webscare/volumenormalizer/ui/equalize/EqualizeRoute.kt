package com.webscare.volumenormalizer.ui.equalize

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webscare.volumenormalizer.theme.AppColors
import com.webscare.volumenormalizer.ui.components.AppCard
import com.webscare.volumenormalizer.ui.components.FileInfoRow
import com.webscare.volumenormalizer.ui.components.PrimaryButton
import com.webscare.volumenormalizer.ui.components.ToolTopBar
import io.github.hlcaptain.symbols.Symbols
import io.github.hlcaptain.symbols.material.Material
import io.github.hlcaptain.symbols.material.Rounded
import io.github.hlcaptain.symbols.material.rounded.vectors.Equalizer
import io.github.hlcaptain.symbols.material.rounded.vectors.Mic
import io.github.hlcaptain.symbols.material.rounded.vectors.MusicNote
import io.github.hlcaptain.symbols.material.rounded.vectors.Tune
import io.github.hlcaptain.symbols.material.rounded.vectors.WaterDrop
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import volumenormalizer.shared.generated.resources.Res
import volumenormalizer.shared.generated.resources.equalize_apply
import volumenormalizer.shared.generated.resources.equalize_bands_title
import volumenormalizer.shared.generated.resources.equalize_preset_bass_boost
import volumenormalizer.shared.generated.resources.equalize_preset_custom
import volumenormalizer.shared.generated.resources.equalize_preset_flat
import volumenormalizer.shared.generated.resources.equalize_preset_treble
import volumenormalizer.shared.generated.resources.equalize_preset_vocal
import volumenormalizer.shared.generated.resources.equalize_presets_title
import volumenormalizer.shared.generated.resources.equalize_title

private val EqualizeColor = Color(0xFFA45AFF)
private val BAND_FREQUENCIES = listOf(60, 230, 910, 4000, 14000)
private const val MIN_GAIN = -12.0
private const val MAX_GAIN = 12.0

enum class EqualizePreset { Flat, BassBoost, Vocal, Treble, Custom }

data class EqualizeSettings(
    val gains: List<Double> = List(5) { 0.0 },
    val preset: EqualizePreset = EqualizePreset.Flat
)

@Composable
fun EqualizeRoute(
    fileName: String,
    fileDetails: String,
    settings: EqualizeSettings,
    onSettingsChange: (EqualizeSettings) -> Unit,
    onBack: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    EqualizeScreen(
        fileName = fileName,
        fileDetails = fileDetails,
        settings = settings,
        onSettingsChange = onSettingsChange,
        onBack = onBack,
        onApplyClick = onApply,
        modifier = modifier
    )
}

@Composable
fun EqualizeScreen(
    fileName: String,
    fileDetails: String,
    settings: EqualizeSettings,
    onSettingsChange: (EqualizeSettings) -> Unit,
    onBack: () -> Unit,
    onApplyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        ToolTopBar(title = stringResource(Res.string.equalize_title), onBack = onBack)

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FileInfoRow(fileName = fileName, details = fileDetails, onMoreClick = { })

            PresetSelector(
                selectedPreset = settings.preset,
                onPresetSelected = { preset ->
                    val newGains = when (preset) {
                        EqualizePreset.Flat -> List(5) { 0.0 }
                        EqualizePreset.BassBoost -> listOf(8.0, 4.0, 0.0, 0.0, 0.0)
                        EqualizePreset.Vocal -> listOf(-2.0, -1.0, 4.0, 3.0, 0.0)
                        EqualizePreset.Treble -> listOf(0.0, 0.0, 0.0, 4.0, 8.0)
                        EqualizePreset.Custom -> settings.gains
                    }
                    onSettingsChange(settings.copy(gains = newGains, preset = preset))
                }
            )

            EqGraph(gains = settings.gains)

            BandSliders(
                gains = settings.gains,
                onGainChanged = { index, value ->
                    val newGains = settings.gains.toMutableList()
                    newGains[index] = value
                    onSettingsChange(settings.copy(gains = newGains, preset = EqualizePreset.Custom))
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        PrimaryButton(
            text = stringResource(Res.string.equalize_apply),
            onClick = onApplyClick,
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            leadingContent = {
                Icon(imageVector = Symbols.Material.Rounded.Equalizer, contentDescription = null, tint = EqualizeColor)
                Spacer(Modifier.width(8.dp))
            }
        )
    }
}

@Composable
private fun PresetSelector(selectedPreset: EqualizePreset, onPresetSelected: (EqualizePreset) -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(Res.string.equalize_presets_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EqualizePreset.entries.forEach { preset ->
                PresetChip(
                    label = presetLabel(preset),
                    icon = when (preset) {
                        EqualizePreset.Flat -> Symbols.Material.Rounded.Tune
                        EqualizePreset.BassBoost -> Symbols.Material.Rounded.MusicNote
                        EqualizePreset.Vocal -> Symbols.Material.Rounded.Mic
                        EqualizePreset.Treble -> Symbols.Material.Rounded.WaterDrop
                        EqualizePreset.Custom -> Symbols.Material.Rounded.Equalizer
                    },
                    isSelected = preset == selectedPreset,
                    onClick = { onPresetSelected(preset) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun presetLabel(preset: EqualizePreset): String = when (preset) {
    EqualizePreset.Flat -> stringResource(Res.string.equalize_preset_flat)
    EqualizePreset.BassBoost -> stringResource(Res.string.equalize_preset_bass_boost)
    EqualizePreset.Vocal -> stringResource(Res.string.equalize_preset_vocal)
    EqualizePreset.Treble -> stringResource(Res.string.equalize_preset_treble)
    EqualizePreset.Custom -> stringResource(Res.string.equalize_preset_custom)
}

@Composable
private fun PresetChip(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier.height(64.dp).clip(shape).background(if (isSelected) EqualizeColor.copy(alpha = 0.15f) else AppColors.Surface).border(1.dp, if (isSelected) EqualizeColor else AppColors.Divider, shape).selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) EqualizeColor else AppColors.TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = if (isSelected) EqualizeColor else AppColors.TextPrimary, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EqGraph(gains: List<Double>) {
    val gainsFloat = gains.map { it.toFloat() }
    Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp)).background(AppColors.Surface).border(1.dp, AppColors.Divider, RoundedCornerShape(16.dp))) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val width = size.width; val height = size.height; val centerY = height / 2f; val maxAmplitude = (height / 2f) - 16.dp.toPx()
            drawLine(color = AppColors.Divider, start = Offset(0f, centerY), end = Offset(width, centerY), strokeWidth = 1.dp.toPx())
            val path = Path()
            val points = gainsFloat.mapIndexed { index, gain ->
                val x = (index.toFloat() / (gainsFloat.size - 1)) * width
                val normalizedGain = (gain / 12f).coerceIn(-1f, 1f)
                Offset(x, centerY - (normalizedGain * maxAmplitude))
            }
            if (points.isNotEmpty()) {
                path.moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]; val curr = points[i]; val controlX = (prev.x + curr.x) / 2f
                    path.cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                }
                drawPath(path = path, color = EqualizeColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                points.forEach { point -> drawCircle(color = EqualizeColor, radius = 4.dp.toPx(), center = point) }
            }
        }
    }
}

@Composable
private fun BandSliders(gains: List<Double>, onGainChanged: (Int, Double) -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(Res.string.equalize_bands_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        BAND_FREQUENCIES.forEachIndexed { index, freq ->
            val gain = gains[index]
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (freq >= 1000) "${freq / 1000}k Hz" else "$freq Hz", style = MaterialTheme.typography.labelMedium, color = AppColors.TextPrimary)
                    Text(text = "${gain.roundToInt()} dB", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EqualizeColor)
                }
                Slider(value = gain.toFloat(), onValueChange = { onGainChanged(index, it.toDouble()) }, valueRange = MIN_GAIN.toFloat()..MAX_GAIN.toFloat(), steps = 23, colors = SliderDefaults.colors(thumbColor = EqualizeColor, activeTrackColor = EqualizeColor, inactiveTrackColor = AppColors.Divider), modifier = Modifier.fillMaxWidth())
            }
            if (index < BAND_FREQUENCIES.size - 1) Spacer(Modifier.height(8.dp))
        }
    }
}