

package com.webscare.volumenormalizer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webscare.volumenormalizer.theme.AppColors
import com.webscare.volumenormalizer.ui.components.AppCard
import com.webscare.volumenormalizer.ui.components.ToolTopBar
import io.github.hlcaptain.symbols.Symbols
import io.github.hlcaptain.symbols.material.Material
import io.github.hlcaptain.symbols.material.Rounded
import io.github.hlcaptain.symbols.material.rounded.vectors.Brightness4
import io.github.hlcaptain.symbols.material.rounded.vectors.ChevronRight
import io.github.hlcaptain.symbols.material.rounded.vectors.Delete
import io.github.hlcaptain.symbols.material.rounded.vectors.Description
import io.github.hlcaptain.symbols.material.rounded.vectors.Folder
import io.github.hlcaptain.symbols.material.rounded.vectors.Info
import io.github.hlcaptain.symbols.material.rounded.vectors.MusicNote
import io.github.hlcaptain.symbols.material.rounded.vectors.Security
import io.github.hlcaptain.symbols.material.rounded.vectors.Tune

private val ScreenBackground = Color(0xFF080F11)
private val CardBackground = Color(0xFF151E20)
private val PrimaryText = Color(0xFFF4F5F5)
private val SecondaryText = Color(0xFF929C9E)
private val DividerColor = Color(0xFF263033)

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScreen(
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showOnboarding by remember { mutableStateOf(true) }
    var isDarkTheme by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(scrollState)
    ) {

        // CENTERED HEADER WITH BACK BUTTON

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(top = 18.dp, bottom = 18.dp),
            contentAlignment = Alignment.Center
        ) {

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
            ) {
                Text(
                    text = "‹",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Light,
                    color = PrimaryText
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                            modifier = Modifier.offset(y = 10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Personalize and manage your app.",
                    fontSize = 10.sp,
                    color = SecondaryText,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // AUDIO SETTINGS

            SettingsSectionTitle("AUDIO SETTINGS")

            SettingsCard {

                SettingsItem(
                    icon = Symbols.Material.Rounded.MusicNote,
                    title = "Default Output Format",
                    subtitle = "MP3",
                    iconTint = Color(0xFF8BEA35),
                    onClick = {}
                )

                SettingsDivider()

                SettingsItem(
                    icon = Symbols.Material.Rounded.Tune,
                    title = "Output Quality",
                    subtitle = "High (320 kbps)",
                    iconTint = Color(0xFF24A9FF),
                    onClick = {}
                )

                SettingsDivider()

                SettingsItem(
                    icon = Symbols.Material.Rounded.Folder,
                    title = "Save Location",
                    subtitle = "Internal Storage / VolumeNormalizer",
                    iconTint = Color(0xFFFFA51F),
                    onClick = {}
                )
            }

            // APP PREFERENCES

            SettingsSectionTitle("APP PREFERENCES")

            SettingsCard {

                SettingsItem(
                    icon = Symbols.Material.Rounded.Brightness4,
                    title = "Theme",
                    subtitle = "Dark",
                    iconTint = Color(0xFF9B65FF),
                    onClick = {}
                )

                SettingsDivider()

                SettingsSwitchItem(
                    icon = Symbols.Material.Rounded.Info,
                    title = "Show Onboarding Again",
                    checked = showOnboarding,
                    onCheckedChange = {
                        showOnboarding = it
                    }
                )

                SettingsDivider()

                SettingsItem(
                    icon = Symbols.Material.Rounded.Delete,
                    title = "Clear App Data",
                    subtitle = "Free up space",
                    iconTint = Color(0xFFFF4081),
                    onClick = {}
                )
            }

            // ABOUT

            SettingsSectionTitle("ABOUT")

            SettingsCard {

                SettingsItem(
                    icon = Symbols.Material.Rounded.Security,
                    title = "Privacy Policy",
                    subtitle = "Read our privacy policy",
                    iconTint = Color(0xFF75E638),
                    onClick = {}
                )

                SettingsDivider()

                SettingsItem(
                    icon = Symbols.Material.Rounded.Description,
                    title = "Terms of Use",
                    subtitle = "Read our terms of use",
                    iconTint = Color(0xFFFFC52E),
                    onClick = {}
                )

                SettingsDivider()

                SettingsItem(
                    icon = Symbols.Material.Rounded.Info,
                    title = "App Version",
                    subtitle = "10.0",
                    iconTint = Color(0xFF20A9FF),
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    title: String
) {
    Text(
        text = title,
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        color = SecondaryText,
        modifier = Modifier.padding(
            start = 5.dp,
            bottom = 8.dp
        )
    )
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = CardBackground,
                shape = RoundedCornerShape(15.dp)
            )
            .padding(
                horizontal = 12.dp,
                vertical = 5.dp
            ),
        content = content
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 36.dp),
        thickness = 0.5.dp,
        color = DividerColor
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = SecondaryText,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        onClick()
                    }
                } else {
                    Modifier
                }
            )
            .defaultMinSize(minHeight = 43.dp)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(21.dp)
        )

        Spacer(modifier = Modifier.width(13.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryText
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = SecondaryText
                )
            }
        }

        Icon(
            imageVector = Symbols.Material.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFADB7B9),
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SecondaryText,
            modifier = Modifier.size(21.dp)
        )

        Spacer(modifier = Modifier.width(13.dp))

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = PrimaryText,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF8BEA35),
                checkedTrackColor = Color(0xFF8BEA35).copy(alpha = 0.3f)
            )
        )
    }
}
