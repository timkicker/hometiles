package org.biglau.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * a size thought of in dp, turned into a font size the system font scale does not touch.
 *
 * tile and header sizes are computed from the available area (`PLAN.md` 3.2), which is
 * already the answer to how big this may be; the device font scale sits at 1.35, so 26
 * would become 35 and the second line would fall out of the header.
 */
@Composable
@ReadOnlyComposable
fun dpSp(value: Float): TextUnit = with(LocalDensity.current) { value.dp.toSp() }

/**
 * tabular figures, `PLAN.md` 3.7, so columns do not jump.
 *
 * built *from* the surrounding style: `Text(style = ...)` replaces the ambient style
 * instead of adding to it, and a fresh `TextStyle` here put the clock, the battery, the
 * keypad and the call duration in the system font while everything beside them was not.
 *
 * measured before: 11 % was 64 pixels wide in the header, 88 % was 74.
 */
@Composable
@ReadOnlyComposable
fun tabularFigures(): TextStyle =
    LocalTextStyle.current.copy(fontFeatureSettings = "tnum")

/**
 * running text in the chosen text size.
 *
 * the setting is called text size and kept only half of it: tiles, rows and headings grew,
 * the explaining text beside them did not.
 *
 * unlike [dpSp], where the size comes from the area and must not be scaled again.
 */
@Composable
@ReadOnlyComposable
fun bigSp(value: Float): TextUnit = (value * org.biglau.ui.theme.LocalTextScale.current).sp

/**
 * the chosen text size, capped.
 *
 * for screens that do not scroll and whose button height is fixed: at 200 % the pin keys
 * were 21 dp high, and the call screen offered a button reading "Lautsprec...". never
 * capped downwards: 75 percent stays 75 percent.
 */
fun cappedTextScale(current: Float, max: Float): Float = minOf(current, max)

/**
 * the longest word of a text, the one the line breaks at.
 *
 * compose splits a word that does not fit on its own, so at 200 % a heading read
 * "Alles zuruecksetze / n". whether a heading fits is decided by its longest word.
 */
fun longestWord(text: String): String =
    text.split(' ', '\n', '\t').maxByOrNull { it.length }.orEmpty().ifEmpty { text }

/**
 * the largest size up to [desiredDp] at which [text] fits on *one* line.
 *
 * an estimate of 0.60 average character width was wrong on the device: the clock tile read
 * "2:33" with the AM cut off silently, the line having `softWrap = false` and no ellipsis.
 *
 * measured with the style that is actually drawn: a foreign font gives the wrong step.
 */
@Composable
fun fittedSingleLineDp(
    text: String,
    style: TextStyle,
    desiredDp: Float,
    maxWidth: Dp,
    minDp: Float = 12f,
): Float {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, style, desiredDp, maxWidth, minDp) {
        val width = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
        largestFitting(desiredDp, minDp) { size ->
            !measurer.measure(
                text = text,
                style = style.copy(fontSize = with(density) { size.dp.toSp() }),
                maxLines = 1,
                softWrap = false,
                constraints = Constraints(maxWidth = width),
            ).hasVisualOverflow
        }
    }
}

/**
 * the largest whole dp step between [minDp] and [desiredDp] for which [fits] holds.
 *
 * halving instead of stepping: the clock changes its text every minute, and from 64 dp
 * downwards that would be up to fifty-two text measurements. this way it is seven.
 *
 * assumes larger text is wider, so if one step fits, every smaller one does.
 */
internal fun largestFitting(desiredDp: Float, minDp: Float, fits: (Float) -> Boolean): Float {
    if (desiredDp <= minDp) return minDp
    if (fits(desiredDp)) return desiredDp
    var low = minDp.toInt()
    var high = desiredDp.toInt()
    while (low + 1 < high) {
        val middle = (low + high) / 2
        if (fits(middle.toFloat())) low = middle else high = middle
    }
    return low.toFloat().coerceAtLeast(minDp)
}
