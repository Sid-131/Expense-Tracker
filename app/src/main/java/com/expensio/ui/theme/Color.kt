package com.expensio.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Primary - Deep Blue
val Blue10 = Color(0xFF001945)
val Blue20 = Color(0xFF002E6E)
val Blue30 = Color(0xFF004397)
val Blue40 = Color(0xFF1565C0)
val Blue80 = Color(0xFFADC6FF)
val Blue90 = Color(0xFFD9E2FF)

// Secondary - Teal
val Teal10 = Color(0xFF002019)
val Teal20 = Color(0xFF00382D)
val Teal30 = Color(0xFF005143)
val Teal40 = Color(0xFF00897B)
val Teal80 = Color(0xFF70EFDE)
val Teal90 = Color(0xFFABF5E5)

// Error
val Red10 = Color(0xFF410002)
val Red20 = Color(0xFF690005)
val Red40 = Color(0xFFBA1A1A)
val Red80 = Color(0xFFFFB4AB)
val Red90 = Color(0xFFFFDAD6)

// Neutral
val Grey10 = Color(0xFF1A1C1E)
val Grey20 = Color(0xFF2F3033)
val Grey90 = Color(0xFFE2E2E6)
val Grey95 = Color(0xFFF0F0F4)
val Grey99 = Color(0xFFFCFCFF)

// Neutral Variant
val BlueGrey30 = Color(0xFF44474E)
val BlueGrey50 = Color(0xFF74777F)
val BlueGrey60 = Color(0xFF8E9099)
val BlueGrey80 = Color(0xFFC4C6D0)
val BlueGrey90 = Color(0xFFE0E2EC)

val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal10,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Grey99,
    onBackground = Grey10,
    surface = Grey99,
    onSurface = Grey10,
    surfaceVariant = BlueGrey90,
    onSurfaceVariant = BlueGrey30,
    outline = BlueGrey50
)

val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    secondary = Teal80,
    onSecondary = Teal20,
    secondaryContainer = Teal30,
    onSecondaryContainer = Teal90,
    error = Red80,
    onError = Red20,
    errorContainer = Red40,
    onErrorContainer = Red90,
    background = Grey10,
    onBackground = Grey90,
    surface = Grey10,
    onSurface = Grey90,
    surfaceVariant = BlueGrey30,
    onSurfaceVariant = BlueGrey80,
    outline = BlueGrey60
)
