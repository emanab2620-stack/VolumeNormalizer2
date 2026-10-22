package com.webscare.volumenormalizer.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AppDarkColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.BackgroundTop,
    background = AppColors.BackgroundTop,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.Divider,
    outlineVariant = AppColors.Divider
)

// PDF: cards ka corner radius 14-20 dp
private val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography = appTypography(),
        shapes = AppShapes,
        content = content
    )
}