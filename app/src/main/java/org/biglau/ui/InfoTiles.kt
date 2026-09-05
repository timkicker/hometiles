package org.biglau.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import org.biglau.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.biglau.info.BatteryInfo
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import org.biglau.info.SignalReading
import org.biglau.info.SignalInfo
import org.biglau.info.BatteryReading
import org.biglau.info.ClockTick
import org.biglau.ui.theme.LocalBigPalette
import org.biglau.ui.theme.LocalTextScale
import java.text.SimpleDateFormat
import org.biglau.data.ClockDisplay
import java.util.Date
import java.util.Locale

/**
 * the time on a tile. updates on the full minute and not every second: on a 2000 mAh
 * battery that is a difference, and it shows no seconds anyway.
 */
@Composable
fun ClockContent(
    cellWidth: Dp,
    cellHeight: Dp,
    clock: ClockDisplay,
    twentyFourHour: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBigPalette.current
    val scale = LocalTextScale.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(ClockTick.millisUntilNextMinute(System.currentTimeMillis()))
            now = System.currentTimeMillis()
        }
    }

    val locale = currentLocale()
    val timeFormat = remember(twentyFourHour, locale) {
        SimpleDateFormat(ClockFormat.timePattern(twentyFourHour), locale)
    }
    val timeText = timeFormat.format(Date(now))
    val timeSize = singleLineSizeSp(timeText, cellWidth.value, cellHeight.value, scale)
    val dateSize = (timeSize * 0.3f).coerceAtLeast(12f)

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // measured, not estimated: `singleLineSizeSp` works from an average character
        // width, and at 200 % with Hyperlegible this read "2:33" with the AM cut off.
        val timeStyle = tabularFigures().copy(fontWeight = FontWeight.Bold)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val timeSp = fittedSingleLineDp(
                text = timeText,
                style = timeStyle,
                desiredDp = timeSize,
                maxWidth = maxWidth,
            )
            Text(
                text = timeText,
                style = timeStyle,
                color = palette.onTile,
                fontSize = dpSp(timeSp),
                maxLines = 1,
                softWrap = false,
            )
        }
        // shorten first, then wrap: "Wednesday, September 2" did not fit one line and left
        // the 2 alone on the second. measured against the width the tile really gives -
        // `cellWidth` is the cell, not the room in it - and with the year's longest date.
        val steps = ClockFormat.dateSkeletons(clock, onTile = true)
        if (steps.isNotEmpty()) {
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val dateSp = dpSp(dateSize)
            // measured with the style that is drawn: the user's font is wider than the
            // default, and a freshly built TextStyle would not carry it.
            val baseStyle = LocalTextStyle.current
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val widthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
                val style = baseStyle.copy(fontSize = dateSp)
                val formats = remember(locale, steps) {
                    steps.mapNotNull { bestDatePattern(it, locale) }
                        .map { SimpleDateFormat(it, locale) }
                }
                val dateFormat = remember(formats, widthPx, dateSp) {
                    formats.firstOrNull { format ->
                        !measurer.measure(
                            text = ClockFormat.longestDate(format),
                            style = style,
                            maxLines = 1,
                            constraints = Constraints(maxWidth = widthPx),
                        ).hasVisualOverflow
                    } ?: formats.lastOrNull()
                }
                if (dateFormat != null) {
                    Text(
                        text = dateFormat.format(Date(now)),
                        color = palette.onTile,
                        fontSize = dateSp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

/**
 * battery as a number plus a bar. a question mark for an unknown level: an empty bar would
 * read as empty and say something untrue.
 */
@Composable
fun BatteryContent(
    reading: BatteryReading?,
    cellWidth: Dp,
    cellHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBigPalette.current
    val scale = LocalTextScale.current
    val percent = reading?.let { BatteryInfo.percent(it) }
    val charging = reading?.let { BatteryInfo.isCharging(it) } ?: false
    val fraction = BatteryInfo.fraction(percent)
    val percentText = if (percent == null) "?" else "$percent %"
    val numberSize = singleLineSizeSp(percentText, cellWidth.value, cellHeight.value, scale, maxSp = 64f)

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // measured, not estimated: at 100 % and a large font the percent sign would be the
        // first candidate to be cut.
        val numberStyle = tabularFigures().copy(fontWeight = FontWeight.Bold)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Text(
                text = percentText,
                // not in the danger colour: measured, the red reaches 1.4 to 1.8 to one on
                // each of the six tile hues, under every threshold in `PLAN.md` 3.3, and
                // that at nine percent. the number itself is the warning.
                style = numberStyle,
                color = palette.onTile,
                fontSize = dpSp(
                    fittedSingleLineDp(percentText, numberStyle, numberSize, maxWidth),
                ),
                maxLines = 1,
                softWrap = false,
            )
        }
        if (charging) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = stringResource(R.string.battery_charging),
                tint = palette.onTile,
                modifier = Modifier.size(dpSp(numberSize * 0.5f).value.dp),
            )
        }
        if (fraction != null) {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.onTile.copy(alpha = 0.25f)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(50))
                        .background(palette.onTile),
                )
            }
        }
    }
}

/**
 * a signal tile: four bars, below them the network type and roaming if any.
 *
 * four states, because each asks for a different move. empty bars said nothing about what
 * to do, so the first two carry a word instead of a number.
 */
@Composable
fun SignalContent(
    reading: SignalReading?,
    cellWidth: Dp,
    cellHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBigPalette.current
    val scale = LocalTextScale.current
    val state = reading?.let { SignalInfo.stateOf(it) } ?: SignalInfo.State.NO_SIM
    val bars = reading?.let { SignalInfo.bars(it) } ?: 0
    val caption = reading?.let { SignalInfo.caption(it) }.orEmpty()
    val word = when (state) {
        SignalInfo.State.NO_PERMISSION -> stringResource(R.string.signal_no_permission)
        SignalInfo.State.NO_SIM -> stringResource(R.string.signal_no_sim)
        SignalInfo.State.NO_SERVICE -> stringResource(R.string.signal_no_service)
        else -> caption
    }

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // rising bars. empty ones stay as an outline so one sees how much is missing.
            repeat(SignalInfo.MAX_LEVEL) { index ->
                val height = (cellHeight.value * (0.08f + 0.045f * index)).coerceAtMost(48f).dp
                Box(
                    Modifier
                        .width((cellWidth.value * 0.09f).coerceIn(6f, 16f).dp)
                        .height(height)
                        // fully rounded like the battery bar: a gauge, not a surface. a
                        // small radius of its own would be a second radius in the app.
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (index < bars) {
                                palette.onTile
                            } else {
                                palette.onTile.copy(alpha = 0.25f)
                            },
                        ),
                )
            }
        }
        if (word.isNotEmpty()) {
            val wordStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)
            BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    text = word,
                    color = palette.onTile,
                    style = wordStyle,
                    fontSize = dpSp(
                        fittedSingleLineDp(
                            text = word,
                            style = wordStyle,
                            desiredDp = singleLineSizeSp(
                                word, cellWidth.value, cellHeight.value, scale, maxSp = 22f,
                            ),
                            maxWidth = maxWidth,
                        ),
                    ),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
