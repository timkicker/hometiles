package org.biglau.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import org.biglau.ui.theme.LocalCornerRadius
import org.biglau.R
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
import org.biglau.info.BatteryInfo
import org.biglau.info.BatteryReading
import org.biglau.info.ClockTick
import org.biglau.ui.theme.LocalBigPalette
import org.biglau.ui.theme.LocalTextScale
import java.text.SimpleDateFormat
import org.biglau.data.ClockDisplay
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
        SimpleDateFormat(if (twentyFourHour) "HH:mm" else "h:mm a", locale)
    }
    val datePattern = ClockFormat.datePattern(clock, onTile = false)
    val dateFormat = remember(locale, datePattern) {
        datePattern?.let { SimpleDateFormat(it, locale) }
    }

    val percent = battery?.let { BatteryInfo.percent(it) }
    val charging = battery?.let { BatteryInfo.isCharging(it) } ?: false
    val fraction = BatteryInfo.fraction(percent)
    val low = BatteryInfo.isLow(percent)

    // Was der linken Spalte wirklich bleibt: Bildschirm minus Aussen- und Innenabstand
    // minus die Ladestandsanzeige rechts.
    val breiteLinks = (
        LocalConfiguration.current.screenWidthDp - 2 * 8f - 2 * 12f -
            ClockFormat.batteryWidthDp(scale)
        ).coerceAtLeast(40f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Die Hoehe waechst mit beidem mit: der Textgroesse und der eigenen Groesse
            // der Uhr. Sonst schneidet die Kopfzeile ihre eigene Zeile ab.
            .height(
                ClockFormat.headerHeightDp(
                    hasDate = ClockFormat.datePattern(clock, onTile = false) != null,
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
                val uhrzeit = timeFormat.format(Date(now))
                Text(
                    text = uhrzeit,
                    color = palette.onBackground,
                    fontSize = dpSp(
                        ClockFormat.clockSizeSp(
                            text = uhrzeit,
                            availableDp = breiteLinks,
                            textScale = scale,
                            clockScale = clockScale,
                        ),
                    ),
                    fontWeight = FontWeight.Bold,
                    style = TabellenZiffern,
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
                Text(
                    text = if (percent == null) "?" else "$percent %",
                    color = if (low) palette.danger else palette.onBackground,
                    fontSize = dpSp(20f * scale),
                    fontWeight = FontWeight.Bold,
                    style = TabellenZiffern,
                    maxLines = 1,
                    softWrap = false,
                )
                // Das Blitzzeichen war ein Emoji und kam damit aus der Emoji-Schrift des
                // Systems - eine zweite Schriftart mitten in der Kopfzeile, in fremder Farbe.
                // Jetzt dasselbe Symbol-Set wie ueberall sonst, in unserer Tinte.
                if (charging) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = stringResource(R.string.battery_charging),
                        tint = if (low) palette.danger else palette.onBackground,
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
                        // Aus dem Token-System abgeleitet statt aus einem festen Schwarz:
                        // im hellen Thema waere ein schwarzer Balken ein Fremdkoerper.
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
