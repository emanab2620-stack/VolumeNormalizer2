package com.webscare.volumenormalizer.ui.editor

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webscare.volumenormalizer.platform.PickedAudio
import com.webscare.volumenormalizer.platform.detailsLine
import com.webscare.volumenormalizer.platform.formatDuration
import com.webscare.volumenormalizer.platform.formatName
import com.webscare.volumenormalizer.platform.formatSize
import com.webscare.volumenormalizer.platform.rememberAudioPlayer
import com.webscare.volumenormalizer.theme.AppColors
import com.webscare.volumenormalizer.ui.components.AppCard
import com.webscare.volumenormalizer.ui.components.FileInfoRow
import com.webscare.volumenormalizer.ui.components.ToolTopBar
import io.github.hlcaptain.symbols.Symbols
import io.github.hlcaptain.symbols.material.Material
import io.github.hlcaptain.symbols.material.Rounded
import io.github.hlcaptain.symbols.material.rounded.vectors.Forward10
import io.github.hlcaptain.symbols.material.rounded.vectors.GraphicEq
import io.github.hlcaptain.symbols.material.rounded.vectors.Info
import io.github.hlcaptain.symbols.material.rounded.vectors.Pause
import io.github.hlcaptain.symbols.material.rounded.vectors.PlayArrow
import io.github.hlcaptain.symbols.material.rounded.vectors.Replay10
import io.github.hlcaptain.symbols.material.rounded.vectors.SignalCellularAlt
import org.jetbrains.compose.resources.stringResource
import volumenormalizer.shared.generated.resources.Res
import volumenormalizer.shared.generated.resources.editor_audio_info
import volumenormalizer.shared.generated.resources.editor_error
import volumenormalizer.shared.generated.resources.editor_forward
import volumenormalizer.shared.generated.resources.editor_info_duration
import volumenormalizer.shared.generated.resources.editor_info_format
import volumenormalizer.shared.generated.resources.editor_info_size
import volumenormalizer.shared.generated.resources.editor_pause
import volumenormalizer.shared.generated.resources.editor_play
import volumenormalizer.shared.generated.resources.editor_rewind
import volumenormalizer.shared.generated.resources.editor_seek_description
import volumenormalizer.shared.generated.resources.editor_title
import volumenormalizer.shared.generated.resources.editor_tool_boost_subtitle
import volumenormalizer.shared.generated.resources.editor_tool_normalize_subtitle
import volumenormalizer.shared.generated.resources.tool_boost
import volumenormalizer.shared.generated.resources.tool_normalize

private const val SKIP_MS = 10_000L

/** Holds the player and passes plain values to the screen. */
@Composable
fun EditorRoute(
    audio: PickedAudio,
    onBack: () -> Unit,
    onNormalizeClick: () -> Unit,
    onBoostClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val player = rememberAudioPlayer(audio.uri, audio.durationMs)

    EditorScreen(
        fileName = audio.displayName,
        fileDetails = audio.detailsLine(),
        formatName = audio.formatName,
        sizeText = audio.sizeBytes?.let { formatSize(it) },
        isPlaying = player.isPlaying,
        positionMs = player.positionMs,
        durationMs = player.durationMs,
        hasError = player.hasError,
        onTogglePlay = { player.togglePlayPause() },
        onSeek = { player.seekTo(it) },
        onSkipBack = { player.skipBy(-SKIP_MS) },
        onSkipForward = { player.skipBy(SKIP_MS) },
        onBack = onBack,
        onNormalizeClick = onNormalizeClick,
        onBoostClick = onBoostClick,
        modifier = modifier
    )
}

@Composable
fun EditorScreen(
    fileName: String,
    fileDetails: String,
    formatName: String,
    sizeText: String?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    hasError: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onBack: () -> Unit,
    onNormalizeClick: () -> Unit,
    onBoostClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ToolTopBar(
            title = stringResource(Res.string.editor_title),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FileInfoRow(
                fileName = fileName,
                details = fileDetails,
                onMoreClick = { }
            )

            SeekBar(
                positionMs = positionMs,
                durationMs = durationMs,
                enabled = !hasError,
                onSeek = onSeek
            )

            PlaybackControls(
                isPlaying = isPlaying,
                enabled = !hasError,
                onTogglePlay = onTogglePlay,
                onSkipBack = onSkipBack,
                onSkipForward = onSkipForward
            )

            if (hasError) {
                Text(
                    text = stringResource(Res.string.editor_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Balance,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ToolTile(
                    icon = Symbols.Material.Rounded.GraphicEq,
                    tint = AppColors.Primary,
                    title = stringResource(Res.string.tool_normalize),
                    subtitle = stringResource(Res.string.editor_tool_normalize_subtitle),
                    onClick = onNormalizeClick,
                    modifier = Modifier.weight(1f)
                )
                ToolTile(
                    icon = Symbols.Material.Rounded.SignalCellularAlt,
                    tint = AppColors.Boost,
                    title = stringResource(Res.string.tool_boost),
                    subtitle = stringResource(Res.string.editor_tool_boost_subtitle),
                    onClick = onBoostClick,
                    modifier = Modifier.weight(1f)
                )
            }

            AudioInfoCard(
                formatName = formatName,
                durationText = formatDuration(durationMs),
                sizeText = sizeText
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    enabled: Boolean,
    onSeek: (Long) -> Unit
) {
    // While the finger is dragging we show the dragged value; the player is told only at the end
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    val safeDurationMs = durationMs.coerceAtLeast(1L)
    val playedFraction = (positionMs.toFloat() / safeDurationMs).coerceIn(0f, 1f)
    val shownFraction = dragFraction ?: playedFraction
    val shownPositionMs = (shownFraction * safeDurationMs).toLong()
    val description = stringResource(Res.string.editor_seek_description)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDuration(shownPositionMs),
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.TextSecondary
        )
        Slider(
            value = shownFraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { fraction -> onSeek((fraction * safeDurationMs).toLong()) }
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
                .semantics { contentDescription = description }
        )
        Text(
            text = formatDuration(durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    enabled: Boolean,
    onTogglePlay: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkipButton(
            icon = Symbols.Material.Rounded.Replay10,
            description = stringResource(Res.string.editor_rewind),
            enabled = enabled,
            onClick = onSkipBack
        )

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) AppColors.Primary else AppColors.Primary.copy(alpha = 0.3f)
                )
                .clickable(enabled = enabled, role = Role.Button, onClick = onTogglePlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) {
                    Symbols.Material.Rounded.Pause
                } else {
                    Symbols.Material.Rounded.PlayArrow
                },
                contentDescription = if (isPlaying) {
                    stringResource(Res.string.editor_pause)
                } else {
                    stringResource(Res.string.editor_play)
                },
                tint = AppColors.BackgroundTop,
                modifier = Modifier.size(36.dp)
            )
        }

        SkipButton(
            icon = Symbols.Material.Rounded.Forward10,
            description = stringResource(Res.string.editor_forward),
            enabled = enabled,
            onClick = onSkipForward
        )
    }
}

@Composable
private fun SkipButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Divider, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) AppColors.TextPrimary else AppColors.TextSecondary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun ToolTile(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AppColors.Divider.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AudioInfoCard(
    formatName: String,
    durationText: String,
    sizeText: String?
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Symbols.Material.Rounded.Info,
                contentDescription = null,
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.editor_audio_info),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
        }
        Spacer(Modifier.height(8.dp))
        InfoRow(stringResource(Res.string.editor_info_format), formatName)
        InfoRow(stringResource(Res.string.editor_info_duration), durationText)
        if (sizeText != null) {
            InfoRow(stringResource(Res.string.editor_info_size), sizeText)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextPrimary
        )
    }
}