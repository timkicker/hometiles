package dev.kicker.hometiles.ui

import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import dev.kicker.hometiles.ui.theme.LocalBigPalette
import dev.kicker.hometiles.ui.theme.contrastRatio

/**
 * width of the focus border, on tiles and everywhere else. clearly more than the blink
 * border of a tile, so two borders on one screen never look alike. `PLAN.md` 10.3.1.
 */
const val FOCUS_BORDER_DP = 8f

/**
 * the focus border for everything that is not a tile: rows, buttons, keypad keys.
 *
 * compose moves the focus with the d-pad by itself but draws nothing for it. a keypad
 * phone then shows a list that seems only to scroll (qin f21, reported 19.10.2026).
 *
 * goes directly in front of the `clickable` it belongs to: `onFocusChanged` sees the focus
 * target that follows it in the chain.
 *
 * [fill] is the surface the border lies on, for rows that paint their own. the preview of the
 * light theme is light grey, and the ink of the dark theme on it was measured by
 * `tools/focus-visible.py` as no border at all (19.10.2026). see [focusRingsFor].
 */
fun Modifier.focusBorder(shape: Shape, fill: Color? = null): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val palette = LocalBigPalette.current
    val rings = focusRingsFor(
        focusArgb(palette.onBackground),
        focusArgb(palette.background),
        fill?.let(::focusArgb),
    )
    this
        .then(if (focused) Modifier.focusRings(rings, shape) else Modifier)
        .onFocusChanged { focused = it.isFocused }
}

/** [outer] lies against the page, [inner] against the fill; null means one ring of [outer]. */
data class FocusRings(val outer: Long, val inner: Long? = null)

/**
 * the focus border has to stand out from the row and from the page around it.
 *
 * one ring of the theme's ink where that reaches 3:1 on the fill: against the page it always
 * does. where it does not, two half rings, the ink outside against the page and the theme's
 * background inside against the fill. the first rule only looked at the fill and took the
 * background there, which is the page's own colour: around a red row in the light theme the
 * border measured 1.00:1 against the page and the row merely looked smaller (issue #2).
 * `FocusInkTest` runs every fill of every theme.
 */
fun focusRingsFor(onBackground: Long, background: Long, fill: Long?): FocusRings = when {
    fill == null || contrastRatio(onBackground, fill) >= 3.0 -> FocusRings(onBackground)
    else -> FocusRings(onBackground, background)
}

/**
 * the same for a tile: its own ink, which is drawn for the tile's colours, against the fill.
 * where that ink would melt into the page, the theme's ink goes around it.
 */
fun tileFocusRingsFor(onTile: Long, onBackground: Long, background: Long): FocusRings = when {
    contrastRatio(onTile, background) >= 3.0 -> FocusRings(onTile)
    else -> FocusRings(onBackground, onTile)
}

/**
 * one border of [width], or two of half of it: the inner one is drawn first, the outer over
 * it. the blink border of a tile passes its own width.
 */
fun Modifier.focusRings(rings: FocusRings, shape: Shape, width: Float = FOCUS_BORDER_DP): Modifier {
    val inner = rings.inner ?: return border(width.dp, Color(rings.outer), shape)
    return border((width / 2).dp, Color(rings.outer), shape)
        .border(width.dp, Color(inner), shape)
}

fun focusArgb(colour: Color): Long = colour.toArgb().toLong() and 0xFFFFFFFFL
