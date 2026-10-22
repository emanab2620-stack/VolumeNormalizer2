package com.webscare.volumenormalizer.ui.batch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webscare.volumenormalizer.platform.RejectedAudio
import com.webscare.volumenormalizer.platform.detailsLine
import com.webscare.volumenormalizer.platform.rememberAudioPicker
import com.webscare.volumenormalizer.theme.AppColors
import com.webscare.volumenormalizer.ui.components.ActionRowCard
import com.webscare.volumenormalizer.ui.components.PickProblemsDialog
import com.webscare.volumenormalizer.ui.components.PrimaryButton
import com.webscare.volumenormalizer.ui.components.ToolTopBar
import io.github.hlcaptain.symbols.Symbols
import io.github.hlcaptain.symbols.material.Material
import io.github.hlcaptain.symbols.material.Rounded
import io.github.hlcaptain.symbols.material.rounded.vectors.Add
import io.github.hlcaptain.symbols.material.rounded.vectors.ChevronRight
import io.github.hlcaptain.symbols.material.rounded.vectors.Close
import io.github.hlcaptain.symbols.material.rounded.vectors.MusicNote
import io.github.hlcaptain.symbols.material.rounded.vectors.PlayArrow
import io.github.hlcaptain.symbols.material.rounded.vectors.Settings
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import volumenormalizer.shared.generated.resources.Res
import volumenormalizer.shared.generated.resources.batch_add_files
import volumenormalizer.shared.generated.resources.batch_add_files_hint
import volumenormalizer.shared.generated.resources.batch_description
import volumenormalizer.shared.generated.resources.batch_empty
import volumenormalizer.shared.generated.resources.batch_files_count
import volumenormalizer.shared.generated.resources.batch_remove_file
import volumenormalizer.shared.generated.resources.batch_settings_note
import volumenormalizer.shared.generated.resources.batch_settings_summary
import volumenormalizer.shared.generated.resources.batch_settings_title
import volumenormalizer.shared.generated.resources.batch_start
import volumenormalizer.shared.generated.resources.batch_title

// Spec default for voice/podcast. Will come from the batch settings screen later.
private const val DEFAULT_TARGET_LUFS = -16

/** One file in the batch list. [id] is the file's durable reference (its URI). */
data class BatchFile(
    val id: String,
    val name: String,
    val details: String
)

/** Holds the screen state. Rotation and ViewModel handling come in a later step. */
@Composable
fun BatchRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var files by remember { mutableStateOf(emptyList<BatchFile>()) }
    var problems by remember { mutableStateOf(emptyList<RejectedAudio>()) }

    val audioPicker = rememberAudioPicker(allowMultiple = true) { result ->
        val added = result.accepted.map { audio ->
            BatchFile(
                id = audio.uri,
                name = audio.displayName,
                details = audio.detailsLine()
            )
        }
        files = (files + added).distinctBy { it.id }
        if (result.rejected.isNotEmpty()) problems = result.rejected
    }

    BatchScreen(
        files = files,
        targetLufs = DEFAULT_TARGET_LUFS,
        onBack = onBack,
        onAddFilesClick = { audioPicker.launch() },
        onRemoveFile = { id -> files = files.filterNot { it.id == id } },
        onSettingsClick = { /* batch settings screen: later step */ },
        onStartClick = { /* processing: later step */ },
        modifier = modifier
    )

    PickProblemsDialog(
        rejected = problems,
        onDismiss = { problems = emptyList() }
    )
}

@Composable
fun BatchScreen(
    files: List<BatchFile>,
    targetLufs: Int,
    onBack: () -> Unit,
    onAddFilesClick: () -> Unit,
    onRemoveFile: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        ToolTopBar(
            title = stringResource(Res.string.batch_title),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.batch_description),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )

            AddFilesCard(onClick = onAddFilesClick)

            if (files.isEmpty()) {
                Text(
                    text = stringResource(Res.string.batch_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    files.forEach { file ->
                        key(file.id) {
                            BatchFileRow(
                                file = file,
                                onRemove = { onRemoveFile(file.id) }
                            )
                        }
                    }
                }
            }

            ActionRowCard(
                icon = Symbols.Material.Rounded.Settings,
                text = stringResource(Res.string.batch_settings_title),
                subtitle = stringResource(Res.string.batch_settings_summary, targetLufs),
                onClick = onSettingsClick
            )

            Text(
                text = stringResource(Res.string.batch_settings_note),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }

        PrimaryButton(
            text = stringResource(Res.string.batch_start),
            subtitle = pluralStringResource(Res.plurals.batch_files_count, files.size, files.size),
            onClick = onStartClick,
            enabled = files.isNotEmpty(),
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            leadingContent = {
                Icon(
                    imageVector = Symbols.Material.Rounded.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
            }
        )
    }
}

@Composable
private fun AddFilesCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = AppColors.Primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Symbols.Material.Rounded.Add,
                    contentDescription = null,
                    tint = AppColors.BackgroundTop
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.batch_add_files),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = stringResource(Res.string.batch_add_files_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
            Icon(
                imageVector = Symbols.Material.Rounded.ChevronRight,
                contentDescription = null,
                tint = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun BatchFileRow(
    file: BatchFile,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AppColors.Divider.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 6.dp, end = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Divider.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Symbols.Material.Rounded.MusicNote,
                    contentDescription = null,
                    tint = AppColors.TextPrimary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Symbols.Material.Rounded.Close,
                    contentDescription = stringResource(Res.string.batch_remove_file, file.name),
                    tint = AppColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}