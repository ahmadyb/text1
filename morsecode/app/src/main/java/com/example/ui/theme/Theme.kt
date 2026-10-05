package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AccentColor(val id: String, val hex: String, val primary: Color, val secondary: Color, val ink: Color) {
    SUNFLOWER("sunflower", "#FACC15", SunflowerAcc, SunflowerAcc2, SunflowerInk),
    LEAF("leaf", "#84CC16", LeafAcc, LeafAcc2, Color(0xFF0F1A00)),
    EMBER("ember", "#EA580C", EmberAcc, EmberAcc2, Color.White),
    VIOLET("violet", "#8B5CF6", VioletAcc, VioletAcc2, Color.White),
    SKY("sky", "#0EA5E9", SkyAcc, SkyAcc2, Color(0xFF001726));

    companion object {
        fun fromId(id: String): AccentColor = entries.find { it.id == id } ?: SUNFLOWER
    }
}

data class MorsecodeColors(
    val bg: Color,
    val card: Color,
    val raised: Color,
    val pressed: Color,
    val line: Color,
    val t1: Color,
    val t2: Color,
    val t3: Color,
    val acc: Color,
    val acc2: Color,
    val accInk: Color,
    val ok: Color = ColorOk,
    val recv: Color = ColorRecv,
    val recvBg: Color = ColorRecvBg,
    val warn: Color = ColorWarn,
    val err: Color = ColorErr,
    val info: Color = ColorInfo
)

val LocalMorsecodeColors = staticCompositionLocalOf {
    MorsecodeColors(
        bg = DarkBg,
        card = DarkCard,
        raised = DarkRaised,
        pressed = DarkPressed,
        line = DarkLine,
        t1 = DarkT1,
        t2 = DarkT2,
        t3 = DarkT3,
        acc = SunflowerAcc,
        acc2 = SunflowerAcc2,
        accInk = SunflowerInk
    )
}

@Composable
fun MorsecodeTheme(
    darkTheme: Boolean = true,
    accent: AccentColor = AccentColor.SUNFLOWER,
    content: @Composable () -> Unit
) {
    val morseColors = if (darkTheme) {
        MorsecodeColors(
            bg = DarkBg,
            card = DarkCard,
            raised = DarkRaised,
            pressed = DarkPressed,
            line = DarkLine,
            t1 = DarkT1,
            t2 = DarkT2,
            t3 = DarkT3,
            acc = accent.primary,
            acc2 = accent.secondary,
            accInk = accent.ink
        )
    } else {
        MorsecodeColors(
            bg = LightBg,
            card = LightCard,
            raised = LightRaised,
            pressed = LightPressed,
            line = LightLine,
            t1 = LightT1,
            t2 = LightT2,
            t3 = LightT3,
            acc = accent.primary,
            acc2 = accent.secondary,
            accInk = accent.ink
        )
    }

    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = accent.primary,
            onPrimary = accent.ink,
            primaryContainer = accent.primary.copy(alpha = 0.2f),
            onPrimaryContainer = DarkT1,
            background = DarkBg,
            surface = DarkBg,
            surfaceVariant = DarkRaised,
            onSurface = DarkT1,
            onSurfaceVariant = DarkT2,
            outline = DarkLine,
            error = ColorErr
        )
    } else {
        lightColorScheme(
            primary = accent.primary,
            onPrimary = accent.ink,
            primaryContainer = accent.primary.copy(alpha = 0.2f),
            onPrimaryContainer = LightT1,
            background = LightBg,
            surface = LightBg,
            surfaceVariant = LightRaised,
            onSurface = LightT1,
            onSurfaceVariant = LightT2,
            outline = LightLine,
            error = ColorErr
        )
    }

    CompositionLocalProvider(LocalMorsecodeColors provides morseColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content
        )
    }
}
