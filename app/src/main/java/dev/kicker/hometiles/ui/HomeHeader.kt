package dev.kicker.hometiles.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import dev.kicker.hometiles.ui.theme.LocalCornerRadius
import dev.kicker.hometiles.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import dev.kicker.hometiles.info.BatteryInfo
import dev.kicker.hometiles.info.BatteryReading
import dev.kicker.hometiles.info.ClockTick
import dev.kicker.hometiles.ui.theme.LocalBigPalette
import dev.kicker.hometiles.ui.theme.LocalTextScale
import java.text.SimpleDateFormat
import dev.kicker.hometiles.data.ClockDisplay
import java.util.Date
import java.util.Locale

/**
 * a bar over the grid with time, date and battery.
 *
 * one wants these permanently, but two whole tiles for them is a third of a 2x3 home
 * screen; as a row they cost about 70 dp. both blocks are left-aligned inside themselves,
 * so the row has the same reading edge as the tiles below (`PLAN.md` 3.0).
 */
@Composable
fun HomeHeader(
    battery: BatteryReading?,
    clock: ClockDisplay,
    clockScale: Float = 1.0f,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBigPalette.current
    val scale = LocalTextScale.current
    val context = LocalContext.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(ClockTick.millisUntilNextMinute(System.currentTimeMillis()))
            now = System.currentTimeMillis()
        }
    }

    val locale = currentLocale()
    val twentyFourHour = remember(context) { DateFormat.is24HourFormat(context) }
    val timeFormat = remember(twentyFourHour, locale) {
        SimpleDateFormat(ClockFormat.timePattern(twentyFourHour), locale)
    }
    val datePattern = bestDatePattern(ClockFormat.dateSkeleton(clock, onTile = false), locale)
    val dateFormat = remember(locale, datePattern) {
        datePattern?.let { SimpleDateFormat(it, locale) }
    }

    val percent = battery?.let { BatteryInfo.percent(it) }
    val charging = battery?.let { BatteryInfo.isCharging(it) } ?: false
    val fraction = BatteryInfo.fraction(percent)
    val low = BatteryInfo.isLow(percent)

    // what the left column really keeps: screen minus outer and inner padding minus the
    // battery display on the right.
    val leftWidth = (
        LocalConfiguration.current.screenWidthDp - 2 * 8f - 2 * 12f -
            ClockFormat.batteryWidthDp(scale)
        ).coerceAtLeast(40f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            // the height grows with both the text size and the clock's own scale, or the
            // header cuts off its own line.
            .height(
                ClockFormat.headerHeightDp(
                    hasDate = ClockFormat.dateSkeleton(clock, onTile = false) != null,
                    textScale = scale,
                    clockScale = clockScale,
                ).dp,
            )
            .clip(RoundedCornerShape(LocalCornerRadius.current))
            .background(palette.emptyTile)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (ClockFormat.showsTime(clock, onTile = false)) {
                val time = timeFormat.format(Date(now))
                // the wish comes from the arithmetic, the measurement has the last word:
                // `clockSizeSp` estimates with an average character width, and that estimate
                // cut off the AM on the clock tile at 200 %.
                val clockStyle = tabularFigures().copy(fontWeight = FontWeight.Bold)
                Text(
                    text = time,
                    color = palette.onBackground,
                    fontSize = dpSp(
                        fittedSingleLineDp(
                            text = time,
                            style = clockStyle,
                            desiredDp = ClockFormat.clockSizeSp(
                                text = time,
                                availableDp = leftWidth,
                                textScale = scale,
                                clockScale = clockScale,
                            ),
                            maxWidth = leftWidth.dp,
                            minDp = 14f,
                        ),
                    ),
                    style = clockStyle,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            if (dateFormat != null) {
                Text(
                    text = dateFormat.format(Date(now)),
                    color = palette.onBackground,
                    fontSize = dpSp(14f * scale * ClockFormat.scale(clockScale)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // measured here too: the space on the right is reserved with the same
                // average character width as everywhere (`batteryWidthDp`), and 100 % is the
                // longest case. cut off, a number is no longer a number.
                val levelText = if (percent == null) "?" else "$percent %"
                val levelStyle = tabularFigures().copy(fontWeight = FontWeight.Bold)
                Text(
                    text = levelText,
                    color = if (low) palette.dangerText else palette.onBackground,
                    fontSize = dpSp(
                        fittedSingleLineDp(
                            text = levelText,
                            style = levelStyle,
                            desiredDp = 20f * scale,
                            maxWidth = (ClockFormat.batteryWidthDp(scale) - 20f * scale - 8f).dp,
                        ),
                    ),
                    style = levelStyle,
                    maxLines = 1,
                    softWrap = false,
                )
                // the bolt used to be an emoji and came from the system emoji font: a second
                // typeface in the middle of the header, in a colour of its own.
                if (charging) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = stringResource(R.string.battery_charging),
                        tint = if (low) palette.dangerText else palette.onBackground,
                        modifier = Modifier.padding(start = 4.dp).size(dpSp(20f * scale).value.dp),
                    )
                }
            }
            if (fraction != null) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(72.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        // from the tokens and not a fixed black, which would be a foreign
                        // body in the light theme.
                        .background(palette.onBackground.copy(alpha = 0.25f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RoundedCornerShape(50))
                            .background(if (low) palette.danger else palette.onBackground),
                    )
                }
            }
        }
    }
}
