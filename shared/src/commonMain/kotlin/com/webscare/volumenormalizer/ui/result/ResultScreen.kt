package com.webscare.volumenormalizer.ui.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webscare.volumenormalizer.NormalizeOutput
import com.webscare.volumenormalizer.formatLufs
import com.webscare.volumenormalizer.platform.AudioPlayer
import com.webscare.volumenormalizer.platform.formatDuration
import com.webscare.volumenormalizer.platform.rememberAudioPlayer
import com.webscare.volumenormalizer.theme.AppColors
import com.webscare.volumenormalizer.ui.components.AppCard
import com.webscare.volumenormalizer.ui.components.FileInfoRow
import com.webscare.volumenormalizer.ui.components.PrimaryButton
import com.webscare.volumenormalizer.ui.components.ToolTopBar
import io.github.hlcaptain.symbols.Symbols
import io.github.hlcaptain.symbols.material.Material
import io.github.hlcaptain.symbols.material.Rounded
import io.github.hlcaptain.symbols.material.rounded.vectors.ArrowForward
import io.github.hlcaptain.symbols.material.rounded.vectors.CheckCircle
import io.github.hlcaptain.symbols.material.rounded.vectors.ChevronRight
import io.github.hlcaptain.symbols.material.rounded.vectors.Pause
import io.github.hlcaptain.symbols.material.rounded.vectors.PlayArrow
import io.github.hlcaptain.symbols.material.rounded.vectors.Refresh
import org.jetbrains.compose.resources.stringResource
import volumenormalizer.shared.generated.resources.Res
import volumenormalizer.shared.generated.resources.dbtp_text
import volumenormalizer.shared.generated.resources.editor_pause
import volumenormalizer.shared.generated.resources.editor_play
import volumenormalizer.shared.generated.resources.editor_seek_description
import volumenormalizer.shared.generated.resources.lufs_not_measured
import volumenormalizer.shared.generated.resources.lufs_text
import volumenormalizer.shared.generated.resources.result_after
import volumenormalizer.shared.generated.resources.result_before
import volumenormalizer.shared.generated.resources.result_clipped_note
import volumenormalizer.shared.generated.resources.result_continue
import volumenormalizer.shared.generated.resources.result_current
import volumenormalizer.shared.generated.resources.result_current_peak
import volumenormalizer.shared.generated.resources.result_edit_again
import volumenormalizer.shared.generated.resources.result_gain
import volumenormalizer.shared.generated.resources.result_limited_note
import volumenormalizer.shared.generated.resources.result_limited_note_boost
import volumenormalizer.shared.generated.resources.result_loudness_title
import volumenormalizer.shared.generated.resources.result_output
import volumenormalizer.shared.generated.resources.result_output_peak
import volumenormalizer.shared.generated.resources.result_peak_note
import volumenormalizer.shared.generated.resources.result_peak_title
import volumenormalizer.shared.generated.resources.result_preview_title
import volumenormalizer.shared.generated.resources.result_ready_description
import volumenormalizer.shared.generated.resources.result_ready_title
import volumenormalizer.shared.generated.resources.result_ready_title_boost
import volumenormalizer.shared.generated.resources.result_target
import volumenormalizer.shared.generated.resources.result_title
import volumenormalizer.shared.generated.resources.value_not_measured

@Composable
fun ResultScreen(
    fileName: String,
    fileDetails: String,
    originalUri: String,
    originalDurationMs: Long,
    output: NormalizeOutput,
    onBack: () -> Unit,
    onEditAgain: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    isBoost: Boolean = false
) {
    // Two players, one for the original and one for the new file. Only one plays at a time.
    val beforePlayer = rememberAudioPlayer(originalUri, originalDurationMs)

    // KMP safe path formatting without using android.net.Uri or java.io.File
    val rawPath = output.processed.filePath
    val processedPath = when {
        rawPath.startsWith("content://") || rawPath.startsWith("file://") -> rawPath
        else -> "file://$rawPath"
    }
    val afterPlayer = rememberAudioPlayer(processedPath, originalDurationMs)

    var showAfter by remember { mutableStateOf(true) }
    val player = if (showAfter) afterPlayer else beforePlayer

    // Switching keeps the same position and keeps playing if it was playing
    fun select(after: Boolean) {
        if (after == showAfter) return
        val from = if (showAfter) afterPlayer else beforePlayer
        val to = if (after) afterPlayer else beforePlayer
        val position = from.positionMs
        val wasPlaying = from.isPlaying
        from.pause()
        to.seekTo(position)
        if (wasPlaying) to.togglePlayPause()
        showAfter = after
    }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ToolTopBar(
            title = stringResource(Res.string.result_title),
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

            ReadyBanner(
                title = if (isBoost) {
                    stringResource(Res.string.result_ready_title_boost)
                } else {
                    stringResource(Res.string.result_ready_title)
                }
            )

            LoudnessCard(output = output)

            PeakCard(output = output)

            PreviewCard(
                player = player,
                showAfter = showAfter,
                onSelect = { select(it) }
            )

            if (output.plan.limitedByPeak) {
                Text(
                    text = if (isBoost) {
                        stringResource(Res.string.result_limited_note_boost)
                    } else {
                        stringResource(Res.string.result_limited_note)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Balance
                )
            }
            if (output.processed.clippedSamples > 0L) {
                Text(
                    text = stringResource(Res.string.result_clipped_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Balance
                )
            }

            Spacer(Modifier.height(4.dp))
        }

        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onEditAgain,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, AppColors.Divider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextPrimary)
            ) {
                Icon(
                    imageVector = Symbols.Material.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(Res.string.result_edit_again),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
            PrimaryButton(
                text = stringResource(Res.string.result_continue),
                onClick = onContinue,
                modifier = Modifier.weight(1.2f),
                trailingContent = {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Symbols.Material.Rounded.ChevronRight,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
private fun ReadyBanner(title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = AppColors.Primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Symbols.Material.Rounded.CheckCircle,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = stringResource(Res.string.result_ready_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun lufsString(value: Double?): String =
    if (value != null) {
        stringResource(Res.string.lufs_text, formatLufs(value))
    } else {
        stringResource(Res.string.lufs_not_measured)
    }

@Composable
private fun dbtpString(value: Double?): String =
    if (value != null) {
        stringResource(Res.string.dbtp_text, formatLufs(value))
    } else {
        stringResource(Res.string.value_not_measured)
    }

private fun signedOneDecimal(value: Double): String =
    if (value >= 0.0) "+" + formatLufs(value) else formatLufs(value)

@Composable
private fun LoudnessCard(output: NormalizeOutput) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.result_loudness_title),
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
            ValueBox(
                label = stringResource(Res.string.result_current),
                value = lufsString(output.before.integratedLufs),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Symbols.Material.Rounded.ArrowForward,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            ValueBox(
                label = stringResource(Res.string.result_output),
                value = lufsString(output.processed.metrics.integratedLufs),
                highlight = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        val target = output.targetLufs
        if (target != null) {
            Text(
                text = stringResource(Res.string.result_target, target),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
        Text(
            text = stringResource(
                Res.string.result_gain,
                signedOneDecimal(output.processed.appliedGainDb)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
private fun PeakCard(output: NormalizeOutput) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.result_peak_title),
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
            ValueBox(
                label = stringResource(Res.string.result_current_peak),
                value = dbtpString(output.before.truePeakDbtp),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Symbols.Material.Rounded.ArrowForward,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            ValueBox(
                label = stringResource(Res.string.result_output_peak),
                value = dbtpString(output.processed.metrics.truePeakDbtp),
                highlight = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.result_peak_note),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
private fun ValueBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(AppColors.BackgroundTop)
            .border(
                1.dp,
                if (highlight) AppColors.Primary.copy(alpha = 0.6f) else AppColors.Divider,
                shape
            )
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
            color = if (highlight) AppColors.Primary else AppColors.TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun PreviewCard(
    player: AudioPlayer,
    showAfter: Boolean,
    onSelect: (Boolean) -> Unit
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val safeDurationMs = player.durationMs.coerceAtLeast(1L)
    val playedFraction = (player.positionMs.toFloat() / safeDurationMs).coerceIn(0f, 1f)
    val shownFraction = dragFraction ?: playedFraction
    val shownPositionMs = (shownFraction * safeDurationMs).toLong()
    val seekDescription = stringResource(Res.string.editor_seek_description)

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.result_preview_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleChip(
                label = stringResource(Res.string.result_before),
                selected = !showAfter,
                onClick = { onSelect(false) },
                modifier = Modifier.weight(1f)
            )
            ToggleChip(
                label = stringResource(Res.string.result_after),
                selected = showAfter,
                onClick = { onSelect(true) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val enabled = true
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled) AppColors.Primary else AppColors.Primary.copy(alpha = 0.3f)
                    )
                    .clickable(enabled = enabled, role = Role.Button) { player.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (player.isPlaying) {
                        Symbols.Material.Rounded.Pause
                    } else {
                        Symbols.Material.Rounded.PlayArrow
                    },
                    contentDescription = if (player.isPlaying) {
                        stringResource(Res.string.editor_pause)
                    } else {
                        stringResource(Res.string.editor_play)
                    },
                    tint = AppColors.BackgroundTop,
                    modifier = Modifier.size(26.dp)
                )
            }

            Slider(
                value = shownFraction,
                onValueChange = { dragFraction = it },
                onValueChangeFinished = {
                    dragFraction?.let { fraction ->
                        player.seekTo((fraction * safeDurationMs).toLong())
                    }
                    dragFraction = null
                },
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = AppColors.Primary,
                    activeTrackColor = AppColors.Primary,
                    inactiveTrackColor = AppColors.Divider
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .semantics { contentDescription = seekDescription }
            )

            Text(
                text = formatDuration(shownPositionMs),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun ToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(if (selected) AppColors.Primary else AppColors.Surface)
            .border(1.dp, if (selected) AppColors.Primary else AppColors.Divider, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) AppColors.BackgroundTop else AppColors.TextPrimary
        )
    }
}