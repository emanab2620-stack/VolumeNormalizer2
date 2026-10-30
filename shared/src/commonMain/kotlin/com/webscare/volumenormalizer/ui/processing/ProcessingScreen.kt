package com.webscare.volumenormalizer.ui.processing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webscare.volumenormalizer.ProcessingState
import com.webscare.volumenormalizer.platform.ProcessFailure
import com.webscare.volumenormalizer.theme.AppColors
import com.webscare.volumenormalizer.ui.components.AppCard
import com.webscare.volumenormalizer.ui.components.FileInfoRow
import com.webscare.volumenormalizer.ui.components.ToolTopBar
import io.github.hlcaptain.symbols.Symbols
import io.github.hlcaptain.symbols.material.Material
import io.github.hlcaptain.symbols.material.Rounded
import io.github.hlcaptain.symbols.material.rounded.vectors.CheckCircle
import io.github.hlcaptain.symbols.material.rounded.vectors.Close
import org.jetbrains.compose.resources.stringResource
import volumenormalizer.shared.generated.resources.Res
import volumenormalizer.shared.generated.resources.processing_back
import volumenormalizer.shared.generated.resources.processing_cancel
import volumenormalizer.shared.generated.resources.processing_failed_space
import volumenormalizer.shared.generated.resources.processing_failed_title
import volumenormalizer.shared.generated.resources.processing_failed_unreadable
import volumenormalizer.shared.generated.resources.processing_failed_unsupported
import volumenormalizer.shared.generated.resources.processing_failed_write
import volumenormalizer.shared.generated.resources.processing_keep_open
import volumenormalizer.shared.generated.resources.processing_step_analyze
import volumenormalizer.shared.generated.resources.processing_step_analyze_desc
import volumenormalizer.shared.generated.resources.processing_step_apply
import volumenormalizer.shared.generated.resources.processing_step_apply_desc
import volumenormalizer.shared.generated.resources.processing_step_finish
import volumenormalizer.shared.generated.resources.processing_step_finish_desc
import volumenormalizer.shared.generated.resources.processing_title
import volumenormalizer.shared.generated.resources.processing_working

private enum class StepState { Done, Running, Pending }

@Composable
fun ProcessingScreen(
    fileName: String,
    fileDetails: String,
    state: ProcessingState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ToolTopBar(
            title = stringResource(Res.string.processing_title),
            onBack = onCancel
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FileInfoRow(
                fileName = fileName,
                details = fileDetails,
                onMoreClick = { }
            )

            when (state) {
                is ProcessingState.Running -> RunningContent(state.progress)
                is ProcessingState.Failed -> FailedContent(state.reason)
            }
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, AppColors.Divider),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextPrimary)
        ) {
            if (state is ProcessingState.Running) {
                Icon(
                    imageVector = Symbols.Material.Rounded.Close,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = if (state is ProcessingState.Running) {
                    stringResource(Res.string.processing_cancel)
                } else {
                    stringResource(Res.string.processing_back)
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun RunningContent(progress: Float?) {
    ProgressRing(progress = progress)

    AppCard(modifier = Modifier.fillMaxWidth()) {
        StepRow(
            title = stringResource(Res.string.processing_step_analyze),
            description = stringResource(Res.string.processing_step_analyze_desc),
            state = StepState.Done
        )
        StepRow(
            title = stringResource(Res.string.processing_step_apply),
            description = stringResource(Res.string.processing_step_apply_desc),
            state = StepState.Running
        )
        StepRow(
            title = stringResource(Res.string.processing_step_finish),
            description = stringResource(Res.string.processing_step_finish_desc),
            state = StepState.Pending
        )
    }

    if (progress != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(AppColors.Divider)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(AppColors.Primary)
            )
        }
    }

    Text(
        text = stringResource(Res.string.processing_keep_open),
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.TextSecondary,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ProgressRing(progress: Float?) {
    val trackColor = AppColors.Divider
    val ringColor = AppColors.Primary
    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            if (progress != null) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (progress != null) "${(progress * 100).toInt()}%" else "…",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = stringResource(Res.string.processing_working),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun StepRow(
    title: String,
    description: String,
    state: StepState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                StepState.Done -> Icon(
                    imageVector = Symbols.Material.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(26.dp)
                )
                StepState.Running -> CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = AppColors.Primary,
                    strokeWidth = 2.dp
                )
                StepState.Pending -> Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(2.dp, AppColors.Divider, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (state == StepState.Pending) AppColors.TextSecondary else AppColors.TextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun FailedContent(reason: ProcessFailure) {
    val message = when (reason) {
        ProcessFailure.Unsupported -> stringResource(Res.string.processing_failed_unsupported)
        ProcessFailure.Unreadable -> stringResource(Res.string.processing_failed_unreadable)
        ProcessFailure.CannotWrite -> stringResource(Res.string.processing_failed_write)
        ProcessFailure.NotEnoughSpace -> stringResource(Res.string.processing_failed_space)
    }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.processing_failed_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.Balance
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary
        )
    }
}