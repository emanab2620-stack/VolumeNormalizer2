package com.webscare.volumenormalizer.ui.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webscare.volumenormalizer.theme.AppColors
import com.webscare.volumenormalizer.ui.components.AnimatedWave
import com.webscare.volumenormalizer.ui.components.BottomNavBar
import com.webscare.volumenormalizer.ui.components.BottomTab
import com.webscare.volumenormalizer.ui.components.PrimaryButton
import io.github.hlcaptain.symbols.Symbols
import io.github.hlcaptain.symbols.material.Material
import io.github.hlcaptain.symbols.material.Rounded
import io.github.hlcaptain.symbols.material.rounded.vectors.ChevronRight
import io.github.hlcaptain.symbols.material.rounded.vectors.GraphicEq
import io.github.hlcaptain.symbols.material.rounded.vectors.Layers
import io.github.hlcaptain.symbols.material.rounded.vectors.Settings
import io.github.hlcaptain.symbols.material.rounded.vectors.SignalCellularAlt
import io.github.hlcaptain.symbols.material.rounded.vectors.Tune
import io.github.hlcaptain.symbols.material.rounded.vectors.Upload
import org.jetbrains.compose.resources.stringResource
import volumenormalizer.shared.generated.resources.Res
import volumenormalizer.shared.generated.resources.home_import_hint
import volumenormalizer.shared.generated.resources.home_select_audio
import volumenormalizer.shared.generated.resources.home_supported_formats
import volumenormalizer.shared.generated.resources.home_tagline
import volumenormalizer.shared.generated.resources.home_title
import volumenormalizer.shared.generated.resources.nav_settings
import volumenormalizer.shared.generated.resources.tool_balance
import volumenormalizer.shared.generated.resources.tool_batch
import volumenormalizer.shared.generated.resources.tool_boost
import volumenormalizer.shared.generated.resources.tool_normalize

enum class HomeTool { Normalize, Boost, Balance, Batch }

@Composable
fun HomeScreen(
    onSelectAudio: () -> Unit,
    onToolClick: (HomeTool) -> Unit,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        // Top row: logo + settings gear
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Symbols.Material.Rounded.GraphicEq,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(32.dp)
            )
            IconButton(onClick = { onTabSelected(BottomTab.Settings) }) {
                Icon(
                    imageVector = Symbols.Material.Rounded.Settings,
                    contentDescription = stringResource(Res.string.nav_settings),
                    tint = AppColors.TextPrimary
                )
            }
        }

        // Title + tagline
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = stringResource(Res.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = stringResource(Res.string.home_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.TextSecondary
            )
        }

        // Decorative animated wave takes the remaining space
        AnimatedWave(modifier = Modifier.weight(1f).fillMaxWidth())

        // Select Audio + hint
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton(
                text = stringResource(Res.string.home_select_audio),
                onClick = onSelectAudio,
                leadingContent = {
                    Icon(
                        imageVector = Symbols.Material.Rounded.Upload,
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
            Text(
                text = stringResource(Res.string.home_import_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }

        // 4 quick tools: same 24 dp side space as the Select Audio button,
        // first circle starts at the button's left edge, last circle ends at its right edge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ToolShortcut(
                label = stringResource(Res.string.tool_normalize),
                icon = Symbols.Material.Rounded.GraphicEq,
                tint = AppColors.Primary,
                onClick = { onToolClick(HomeTool.Normalize) }
            )
            ToolShortcut(
                label = stringResource(Res.string.tool_boost),
                icon = Symbols.Material.Rounded.SignalCellularAlt,
                tint = AppColors.Boost,
                onClick = { onToolClick(HomeTool.Boost) }
            )
            ToolShortcut(
                label = stringResource(Res.string.tool_balance),
                icon = Symbols.Material.Rounded.Tune,
                tint = AppColors.Balance,
                onClick = { onToolClick(HomeTool.Balance) }
            )
            ToolShortcut(
                label = stringResource(Res.string.tool_batch),
                icon = Symbols.Material.Rounded.Layers,
                tint = AppColors.Purple,
                onClick = { onToolClick(HomeTool.Batch) }
            )
        }

        // Supported formats with divider lines
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f).height(1.dp).background(AppColors.Divider))
            Text(
                text = stringResource(Res.string.home_supported_formats),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Box(Modifier.weight(1f).height(1.dp).background(AppColors.Divider))
        }
        Spacer(Modifier.height(38.dp))
        BottomNavBar(selected = BottomTab.Home, onTabSelected = onTabSelected)
    }
}

@Composable
private fun ToolShortcut(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    // Fixed 64 dp width = circle width, so the circle edge is exactly the item's edge
    Column(
        modifier = Modifier
            .width(56.dp)

            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Divider, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.wrapContentWidth(unbounded = true)
        )
    }
}