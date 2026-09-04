package com.baltajmn.test4test

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Un solo acento y el resto neutros. El violeta es el del icono y el grafico de
// funciones: si se toca aqui, hay que tocar branding/*.svg y el fondo del
// adaptive icon, que no salen de este fichero.
private val Violet = Color(0xFF5330D9)
private val VioletSoft = Color(0xFFEDE8FF)
private val Ink = Color(0xFF181425)
private val Muted = Color(0xFF6B6480)
private val Line = Color(0xFFE7E4EE)
private val Border = Color(0xFF8B8399)
private val Paper = Color(0xFFFBFAFC)
private val Card = Color(0xFFFFFFFF)
private val Danger = Color(0xFFB4241C)

private val colors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = VioletSoft,
    onPrimaryContainer = Ink,
    secondary = Muted,
    onSecondary = Color.White,
    secondaryContainer = VioletSoft,
    onSecondaryContainer = Ink,
    tertiary = Violet,
    onTertiary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Card,
    onSurfaceVariant = Muted,
    surfaceContainerLowest = Card,
    surfaceContainerLow = Card,
    surfaceContainer = Card,
    surfaceContainerHigh = Card,
    surfaceContainerHighest = Card,
    // Sin sombras ni tinte de elevacion: la jerarquia la hacen el filete y el
    // blanco sobre el papel, no una pila de superficies.
    surfaceTint = Color.Transparent,
    // Border para lo que hay que poder tocar (bordes de campo), Line para lo que
    // solo separa. Un unico gris para las dos cosas deja los campos invisibles.
    outline = Border,
    outlineVariant = Line,
    error = Danger,
    onError = Color.White,
    errorContainer = Color(0xFFFDE7E5),
    onErrorContainer = Color(0xFF5C120D),
)

// Sin fontFamily a proposito: la del sistema es la unica que trae todos los
// alfabetos. Aqui se publican apps de cualquier pais, y una fuente empaquetada
// solo cubre lo que le quepa. Schibsted Grotesk traia 497 glifos, todos latinos,
// asi que un nombre japones o el idioma hindi entero salian en cuadritos.
@Composable
private fun typography(): Typography {
    fun style(size: Int, line: Int, weight: FontWeight, tracking: Double) = TextStyle(
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = weight,
        letterSpacing = tracking.sp,
    )
    return Typography(
        displaySmall = style(40, 44, FontWeight.Bold, -1.2),
        headlineLarge = style(32, 38, FontWeight.Bold, -0.8),
        headlineMedium = style(28, 34, FontWeight.Bold, -0.6),
        headlineSmall = style(24, 30, FontWeight.Bold, -0.4),
        titleLarge = style(20, 26, FontWeight.Medium, -0.2),
        titleMedium = style(17, 22, FontWeight.Medium, -0.1),
        titleSmall = style(15, 20, FontWeight.Medium, 0.0),
        bodyLarge = style(16, 25, FontWeight.Normal, 0.0),
        bodyMedium = style(15, 23, FontWeight.Normal, 0.0),
        bodySmall = style(13, 19, FontWeight.Normal, 0.1),
        labelLarge = style(14, 18, FontWeight.Medium, 0.1),
        labelMedium = style(12, 16, FontWeight.Medium, 0.3),
        labelSmall = style(11, 14, FontWeight.Medium, 0.5),
    )
}

// Radios cortos en las superficies y pastilla en los botones: lo que separa
// informacion queda recto, lo que se pulsa queda blando.
private val shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun Test4TestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        typography = typography(),
        shapes = shapes,
        content = content,
    )
}
