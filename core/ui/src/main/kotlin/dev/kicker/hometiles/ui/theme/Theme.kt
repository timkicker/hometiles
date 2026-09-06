package dev.kicker.hometiles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.kicker.hometiles.data.ThemeName
import dev.kicker.hometiles.data.FontChoice
import androidx.compose.ui.unit.dp
import dev.kicker.hometiles.data.HapticStrength
import dev.kicker.hometiles.data.IconVisibility
import dev.kicker.hometiles.ui.LocalHaptics

/**
 * a surface together with its ink. kept apart, the two drifted three times: white text on a
 * bright accent and on a bright red, both under 3:1.
 */
@Immutable
data class BigSurface(val fill: Color, val ink: Color)

/** background, ink and a palette for the tiles; the tile colours are strong and distinct. */
@Immutable
data class BigPalette(
    val background: Color,
    val onBackground: Color,
    val tiles: List<Color>,
    val onTile: Color,
    /** an unassigned tile: visible, but clearly held back. */
    val emptyTile: Color,
    /** border of the empty tile; it carries the findability, not the fill. */
    val emptyTileBorder: Color,
    val accent: Color,
    /** text and icons on the accent surface. */
    val onAccent: Color,
    val danger: Color,
    val onDanger: Color,
    /**
     * warning text on the background. not [danger], which is an area colour carrying
     * [onDanger] and misses its own threshold as text; see `Tokens.DARK_DANGER_TEXT`.
     */
    val dangerText: Color,
) {
    val surfaceDefault: BigSurface get() = BigSurface(emptyTile, onBackground)
    val surfaceAccent: BigSurface get() = BigSurface(accent, onAccent)
    val surfaceDanger: BigSurface get() = BigSurface(danger, onDanger)
    fun surfaceTile(index: Int): BigSurface = BigSurface(tiles[index.mod(tiles.size)], onTile)

    /** every pair there is; the contrast test runs over exactly this list. */
    fun allSurfaces(): List<BigSurface> =
        listOf(surfaceDefault, surfaceAccent, surfaceDanger) + tiles.indices.map(::surfaceTile)
}

private fun c(argb: Long) = Color(argb.toInt())

/** the main theme; every value comes from [Tokens], the thresholds are nailed in ContrastTest. */
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
    dangerText = c(Tokens.DARK_DANGER_TEXT),
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
    dangerText = c(Tokens.LIGHT_DANGER_TEXT),
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
    dangerText = c(Tokens.CONTRAST_DANGER_TEXT),
)

/**
 * the palette of a theme.
 *
 * [systemIsDark] is needed only for [ThemeName.SYSTEM] and still has no default: a silent
 * one would paint the wrong theme wherever it was forgotten.
 */
fun paletteFor(theme: ThemeName, systemIsDark: Boolean): BigPalette = when (theme) {
    ThemeName.DARK -> Dark
    ThemeName.HIGH_CONTRAST -> HighContrast
    ThemeName.LIGHT -> Light
    ThemeName.SYSTEM -> if (systemIsDark) Dark else Light
}

/** tile border: visible only in the high contrast theme, but load-bearing there. */
fun BigPalette.tileBorder(): Color? =
    if (this === HighContrast) Color(0xFFFFEB3B) else null

val LocalBigPalette = staticCompositionLocalOf { Dark }
val LocalTextScale = staticCompositionLocalOf { 1.0f }

/** label on the tile, on top of the global text size. `PLAN.md` 4.2. */
val LocalLabelScale = staticCompositionLocalOf { 1.0f }

/** icon size as a percent of the shorter cell edge. `PLAN.md` 4.2. */
val LocalIconPercent = staticCompositionLocalOf { 40 }

/** whether an icon stands on the tile: always, never, or only if there is room. `PLAN.md` 4.2. */
val LocalIconVisibility = staticCompositionLocalOf { IconVisibility.ALWAYS }

/** drop the label when it would be cut off. `PLAN.md` 3.2. */
val LocalHideCutLabels = staticCompositionLocalOf { false }

/**
 * the one corner radius for every surface. `PLAN.md` 3.7 forbids a second one beside it,
 * which is what fifteen hard-written 12.dp produced: square tiles beside round rows.
 */
val LocalCornerRadius = staticCompositionLocalOf { 12.dp }

@Composable
fun HomeTilesTheme(
    theme: ThemeName = ThemeName.DARK,
    textScale: Float = 1.0f,
    /** haptic answer on a tap, from the `Behaviour` setting. */
    haptics: HapticStrength = HapticStrength.LIGHT,
    font: FontChoice = FontChoice.HYPERLEGIBLE,
    labelScale: Float = 1.0f,
    iconPercent: Int = 40,
    icons: IconVisibility = IconVisibility.ALWAYS,
    hideCutLabels: Boolean = false,
    cornerRadiusDp: Int = 12,
    content: @Composable () -> Unit,
) {
    val palette = paletteFor(theme, isSystemInDarkTheme())
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
        LocalHaptics provides haptics,
        LocalBigPalette provides palette,
        LocalTextScale provides textScale,
        LocalLabelScale provides labelScale,
        LocalIconPercent provides iconPercent,
        LocalIconVisibility provides icons,
        LocalHideCutLabels provides hideCutLabels,
        LocalCornerRadius provides cornerRadiusDp.dp,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typographyFor(font),
            content = content,
        )
    }
}

private fun Color.luminanceIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f
