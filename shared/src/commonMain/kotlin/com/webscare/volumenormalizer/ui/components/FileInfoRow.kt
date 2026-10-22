package com.webscare.volumenormalizer.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webscare.volumenormalizer.theme.AppColors
import io.github.hlcaptain.symbols.Symbols
import io.github.hlcaptain.symbols.material.Material
import io.github.hlcaptain.symbols.material.Rounded
import io.github.hlcaptain.symbols.material.rounded.vectors.MoreHoriz
import io.github.hlcaptain.symbols.material.rounded.vectors.MusicNote
import org.jetbrains.compose.resources.stringResource
import volumenormalizer.shared.generated.resources.Res
import volumenormalizer.shared.generated.resources.action_more_options

@Composable
fun FileInfoRow(
    fileName: String,
    details: String,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Divider, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Symbols.Material.Rounded.MusicNote,
                contentDescription = null,
                tint = AppColors.TextPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onMoreClick) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppColors.Surface)
                    .border(1.dp, AppColors.Divider, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Symbols.Material.Rounded.MoreHoriz,
                    contentDescription = stringResource(Res.string.action_more_options),
                    tint = AppColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}