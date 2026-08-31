package org.biglau.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.biglau.info.BatteryInfo
import org.biglau.info.BatteryReading
import org.biglau.info.ClockTick
import org.biglau.ui.theme.LocalBigPalette
import org.biglau.ui.theme.LocalTextScale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ein Balken ueber dem Raster mit Uhrzeit, Datum und Ladestand.
 *
 * Der Sinn: diese Angaben will man dauerhaft sehen, aber zwei ganze Kacheln dafuer zu
 * verbrauchen ist auf einem 2x3-Raster ein Drittel des Homescreens. Als Zeile kosten sie
 * rund 70 dp - die Kacheln darunter schrumpfen entsprechend, bleiben aber Kacheln.
 *
 * Links die Zeit gross mit dem Datum darunter, rechts der Ladestand mit Balken. Beide
 * Bloecke sind in sich linksbuendig, damit die Zeile dieselbe Lesekante hat wie die
 * Kacheln darunter (PLAN.md 3.0).
 */
@Composable
fun HomeHeader(
    battery: BatteryReading?,
    showDate: Boolean,
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

    val locale = Locale.getDefault()
    val twentyFourHour = remember(context) { DateFormat.is24HourFormat(context) }
    val timeFormat = remember(twentyFourHour, locale) {
        SimpleDateFormat(if (twentyFourHour) "HH:mm" else "h:mm a", locale)
    }
    val dateFormat = remember(locale) { SimpleDateFormat("EEE, d. MMM", locale) }

    val percent = battery?.let { BatteryInfo.percent(it) }
    val charging = battery?.let { BatteryInfo.isCharging(it) } ?: false
    val fraction = BatteryInfo.fraction(percent)
    val low = BatteryInfo.isLow(percent)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (showDate) 78.dp else 54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.emptyTile)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = timeFormat.format(Date(now)),
                color = palette.onBackground,
                fontSize = dpSp(26f * scale),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
            if (showDate) {
                Text(
                    text = dateFormat.format(Date(now)),
                    color = palette.onBackground,
                    fontSize = dpSp(14f * scale),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = buildString {
                    append(if (percent == null) "?" else "$percent %")
                    if (charging) append(" ⚡")
                },
                color = if (low) palette.danger else palette.onBackground,
                fontSize = dpSp(20f * scale),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
            if (fraction != null) {
                Box(
                    Modifier
                        .padding(top = 5.dp)
                        .width(72.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.45f)),
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
