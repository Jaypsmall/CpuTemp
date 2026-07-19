package com.jaylizapp.cputemp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DemoniacColorScheme = darkColorScheme(
    primary = Color(0xFF8B0000), // Blood Red
    secondary = Color(0xFF4B0082), // Indigo/Dark Purple
    background = Color.Black,
    surface = Color(0xFF1A1A1A),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    outline = Color(0xFF333333),
    secondaryContainer = Color(0xFF2A2A2A) // Terminal-like background
)

private val LightProfessionalColorScheme = lightColorScheme(
    primary = Color(0xFF0066FF), // Electric Blue
    secondary = Color(0xFF0056D2), // Navy Blue
    background = Color(0xFFF4F7FA), // Soft Blueish White
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF333333), // Dark Gray
    onSurface = Color(0xFF333333),
    outline = Color(0xFFD0E0FF), // Light Pastel Blue
    secondaryContainer = Color(0xFFE9EDF0), // Light blueish gray (Terminal background)
    tertiary = Color(0xFF008800) // OK Text
)

@Composable
fun CpuTempTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DemoniacColorScheme else LightProfessionalColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
