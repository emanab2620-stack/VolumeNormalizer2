package com.webscare.volumenormalizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.webscare.volumenormalizer.theme.AppColors
import io.github.hlcaptain.symbols.Symbols
import io.github.hlcaptain.symbols.material.Filled
import io.github.hlcaptain.symbols.material.Material
import io.github.hlcaptain.symbols.material.Rounded
import io.github.hlcaptain.symbols.material.rounded.vectors.Folder
import io.github.hlcaptain.symbols.material.rounded.vectors.Home
import io.github.hlcaptain.symbols.material.rounded.vectors.Settings
import org.jetbrains.compose.resources.stringResource
import volumenormalizer.shared.generated.resources.Res
import volumenormalizer.shared.generated.resources.nav_home
import volumenormalizer.shared.generated.resources.nav_my_files
import volumenormalizer.shared.generated.resources.nav_settings

enum class BottomTab { Home, MyFiles, Settings }

@Composable
fun BottomNavBar(
    selected: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().background(AppColors.BackgroundTop)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.Divider)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(
                label = stringResource(Res.string.nav_home),
                icon = Symbols.Material.Rounded.Home,
                selectedIcon = Symbols.Material.Rounded.Filled.Home,
                isSelected = selected == BottomTab.Home,
                onClick = { onTabSelected(BottomTab.Home) }
            )
            NavItem(
                label = stringResource(Res.string.nav_my_files),
                icon = Symbols.Material.Rounded.Folder,
                selectedIcon = Symbols.Material.Rounded.Filled.Folder,
                isSelected = selected == BottomTab.MyFiles,
                onClick = { onTabSelected(BottomTab.MyFiles) }
            )
            NavItem(
                label = stringResource(Res.string.nav_settings),
                icon = Symbols.Material.Rounded.Settings,
                selectedIcon = Symbols.Material.Rounded.Filled.Settings,
                isSelected = selected == BottomTab.Settings,
                onClick = { onTabSelected(BottomTab.Settings) }
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) AppColors.Primary else AppColors.TextSecondary
    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 72.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isSelected) selectedIcon else icon,
            contentDescription = null,
            tint = color
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}