package com.webscare.volumenormalizer.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.webscare.volumenormalizer.platform.PickFailure
import com.webscare.volumenormalizer.platform.RejectedAudio
import com.webscare.volumenormalizer.theme.AppColors
import org.jetbrains.compose.resources.stringResource
import volumenormalizer.shared.generated.resources.Res
import volumenormalizer.shared.generated.resources.action_ok
import volumenormalizer.shared.generated.resources.pick_error_not_audio
import volumenormalizer.shared.generated.resources.pick_error_title
import volumenormalizer.shared.generated.resources.pick_error_unreadable

/** Explains which chosen files cannot be used. Shows nothing when the list is empty. */
@Composable
fun PickProblemsDialog(
    rejected: List<RejectedAudio>,
    onDismiss: () -> Unit
) {
    if (rejected.isEmpty()) return

    val message = rejected.map { item ->
        when (item.reason) {
            PickFailure.NotAudio -> stringResource(Res.string.pick_error_not_audio, item.displayName)
            PickFailure.Unreadable -> stringResource(Res.string.pick_error_unreadable, item.displayName)
        }
    }.joinToString(separator = "\n\n")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Surface,
        titleContentColor = AppColors.TextPrimary,
        textContentColor = AppColors.TextSecondary,
        title = { Text(stringResource(Res.string.pick_error_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.action_ok),
                    color = AppColors.Primary
                )
            }
        }
    )
}