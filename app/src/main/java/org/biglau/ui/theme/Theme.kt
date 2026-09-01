package org.biglau.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.biglau.data.ThemeName
import org.biglau.ui.LocalHapticsEnabled

/**
 * Ein Theme besteht aus Hintergrund, Textfarbe und einer Palette fuer die Kacheln.
 * Die Kachelfarben sind bewusst kraeftig und untereinander gut unterscheidbar.
 */
/**
 * Eine Flaeche samt ihrer Schriftfarbe. Getrennt waren die beiden dreimal auseinandergelaufen -
 * weisser Text auf hellem Akzent und hellem Rot, jeweils unter 3:1. Als Paar ist das nicht
 * mehr moeglich, ohne es absichtlich zu tun.
 */
@Immutable
data class BigSurface(val fill: Color, val ink: Color)

@Immutable
data class BigPalette(
    val background: Color,
    val onBackground: Color,
    val tiles: List<Color>,
    val onTile: Color,
    /** Kachel ohne Belegung - sichtbar, aber deutlich zurueckgenommen. */
    val emptyTile: Color,
    /** Umrandung der leeren Kachel; traegt die Auffindbarkeit statt der Fuellung. */
    val emptyTileBorder: Color,
    val accent: Color,
    /** Text und Icons auf der Akzentflaeche. */
    val onAccent: Color,
    val danger: Color,
    val onDanger: Color,
) {
    val surfaceDefault: BigSurface get() = BigSurface(emptyTile, onBackground)
    val surfaceAccent: BigSurface get() = BigSurface(accent, onAccent)
    val surfaceDanger: BigSurface get() = BigSurface(danger, onDanger)
    fun surfaceTile(index: Int): BigSurface = BigSurface(tiles[index.mod(tiles.size)], onTile)

    /** Alle Paare, die es gibt - der Kontrasttest laeuft ueber genau diese Liste. */
    fun allSurfaces(): List<BigSurface> =
        listOf(surfaceDefault, surfaceAccent, surfaceDanger) + tiles.indices.map(::surfaceTile)
}

private fun c(argb: Long) = Color(argb.toInt())

/**
 * Hauptthema. Alle Werte kommen aus [Tokens]; die Kontrastschwellen dahinter
 * sind in ContrastTest festgenagelt.
 */
private val Dark = BigPalette(
    background = c(Tokens.DARK_BACKGROUND),
    onBackground = c(Tokens.DARK_ON_BACKGROUND),
    tiles = Tokens.DARK_TILES.map(::c),
    onTile = Color.White,
    emptyTile = c(Tokens.DARK_EMPTY_TILE),
    emptyTileBorder = c(Tokens.DARK_EMPTY_TILE_BORDER),
    accent = c(Tokens.DARK_ACCENT),
    onAccent = c(Tokens.DARK_ON_ACCENT),
    danger = c(Tokens.DARK_DANGER),
    onDanger = c(Tokens.DARK_ON_DANGER),
)

private val Light = BigPalette(
    background = c(Tokens.LIGHT_BACKGROUND),
    onBackground = c(Tokens.LIGHT_ON_BACKGROUND),
    tiles = Tokens.LIGHT_TILES.map(::c),
    onTile = Color.White,
    emptyTile = c(Tokens.LIGHT_EMPTY_TILE),
    emptyTileBorder = c(Tokens.LIGHT_EMPTY_TILE_BORDER),
    accent = c(Tokens.LIGHT_ACCENT),
    onAccent = c(Tokens.LIGHT_ON_ACCENT),
    danger = c(Tokens.LIGHT_DANGER),
    onDanger = c(Tokens.LIGHT_ON_DANGER),
)

private val HighContrast = BigPalette(
    background = c(Tokens.CONTRAST_BACKGROUND),
    onBackground = c(Tokens.CONTRAST_INK),
    tiles = List(6) { c(Tokens.CONTRAST_BACKGROUND) },
    onTile = c(Tokens.CONTRAST_INK),
    emptyTile = c(Tokens.CONTRAST_BACKGROUND),
    emptyTileBorder = c(Tokens.CONTRAST_INK),
    accent = c(Tokens.CONTRAST_INK),
    onAccent = c(Tokens.CONTRAST_ON_ACCENT),
    danger = c(Tokens.CONTRAST_DANGER),
    onDanger = c(Tokens.CONTRAST_ON_DANGER),
)

fun paletteFor(theme: ThemeName): BigPalette = when (theme) {
    ThemeName.DARK -> Dark
    ThemeName.HIGH_CONTRAST -> HighContrast
    ThemeName.LIGHT -> Light
}

/** Kachelrahmen: nur im Hochkontrast-Theme sichtbar, dort aber tragend. */
fun BigPalette.tileBorder(): Color? =
    if (this === HighContrast) Color(0xFFFFEB3B) else null

val LocalBigPalette = staticCompositionLocalOf { Dark }
val LocalTextScale = staticCompositionLocalOf { 1.0f }

@Composable
fun BigLauTheme(
    theme: ThemeName = ThemeName.DARK,
    textScale: Float = 1.0f,
    /** Spuerbare Rueckmeldung beim Antippen - Einstellung aus `Behaviour`. */
    haptics: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = paletteFor(theme)
    val scheme = if (isSystemInDarkTheme() || palette.background.luminanceIsDark()) {
        darkColorScheme(
            primary = palette.accent,
            background = palette.background,
            surface = palette.background,
            onBackground = palette.onBackground,
            onSurface = palette.onBackground,
            error = palette.danger,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            background = palette.background,
            surface = palette.background,
            onBackground = palette.onBackground,
            onSurface = palette.onBackground,
            error = palette.danger,
        )
    }
    CompositionLocalProvider(
        LocalHapticsEnabled provides haptics,
        LocalBigPalette provides palette,
        LocalTextScale provides textScale,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

private fun Color.luminanceIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f
