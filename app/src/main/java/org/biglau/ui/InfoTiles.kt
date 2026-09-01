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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import java.util.Date
import java.util.Locale

/**
 * Uhrzeit auf einer Kachel. Aktualisiert sich zur vollen Minute, nicht sekuendlich -
 * auf einem 2000-mAh-Akku ist das ein Unterschied, und Sekunden zeigt sie ohnehin nicht.
 */
@Composable
fun ClockContent(
    cellWidth: Dp,
    cellHeight: Dp,
    showDate: Boolean,
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

    val locale = Locale.getDefault()
    val timeFormat = remember(twentyFourHour, locale) {
        SimpleDateFormat(if (twentyFourHour) "HH:mm" else "h:mm a", locale)
    }
    val dateFormat = remember(locale) { SimpleDateFormat("EEEE, d. MMMM", locale) }

    val timeText = timeFormat.format(Date(now))
    val timeSize = singleLineSizeSp(timeText, cellWidth.value, cellHeight.value, scale)

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = timeText,
            color = palette.onTile,
            fontSize = dpSp(timeSize),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
        if (showDate) {
            Text(
                text = dateFormat.format(Date(now)),
                color = palette.onTile,
                fontSize = dpSp((timeSize * 0.3f).coerceAtLeast(12f)),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

/**
 * Ladestand als Zahl plus Balken. Bei unbekanntem Stand ein Fragezeichen statt eines
 * leeren Balkens - der saehe aus wie "leer" und waere eine falsche Aussage.
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
    val low = BatteryInfo.isLow(percent)
    val percentText = if (percent == null) "?" else "$percent %"
    val numberSize = singleLineSizeSp(percentText, cellWidth.value, cellHeight.value, scale, maxSp = 64f)

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = percentText,
            color = if (low) palette.danger else palette.onTile,
            fontSize = dpSp(numberSize),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
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
                        .background(if (low) palette.danger else palette.onTile),
                )
            }
        }
    }
}

/**
 * Der Inhalt einer Empfangskachel: vier Balken, darunter Netzart und gegebenenfalls Roaming.
 *
 * Vier Zustände, weil sie zu verschiedenen Handlungen führen - keine Karte, kein Netz,
 * schwach, in Ordnung. Leere Balken für alle drei ersten Fälle sagten nicht, was zu tun ist,
 * deshalb steht bei den ersten beiden ein Wort statt einer Zahl.
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
    val zustand = reading?.let { SignalInfo.stateOf(it) } ?: SignalInfo.State.NO_SIM
    val balken = reading?.let { SignalInfo.bars(it) } ?: 0
    val zusatz = reading?.let { SignalInfo.caption(it) }.orEmpty()
    val wort = when (zustand) {
        SignalInfo.State.NO_PERMISSION -> stringResource(R.string.signal_no_permission)
        SignalInfo.State.NO_SIM -> stringResource(R.string.signal_no_sim)
        SignalInfo.State.NO_SERVICE -> stringResource(R.string.signal_no_service)
        else -> zusatz
    }
    val schwach = zustand == SignalInfo.State.WEAK || zustand == SignalInfo.State.NO_SERVICE

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Steigende Balken, wie man sie kennt. Leere bleiben als Umriss stehen, damit
            // man sieht, wie viel fehlt - eine verschwundene Stufe zeigt nur die halbe Lage.
            repeat(SignalInfo.MAX_LEVEL) { index ->
                val hoehe = (cellHeight.value * (0.08f + 0.045f * index)).coerceAtMost(48f).dp
                Box(
                    Modifier
                        .width((cellWidth.value * 0.09f).coerceIn(6f, 16f).dp)
                        .height(hoehe)
                        // Vollrund wie der Ladebalken: eine Anzeige, keine Flaeche. Ein
                        // eigener kleiner Radius waere ein zweiter Radius in der App.
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (index < balken) {
                                if (schwach) palette.danger else palette.onTile
                            } else {
                                palette.onTile.copy(alpha = 0.25f)
                            },
                        ),
                )
            }
        }
        if (wort.isNotEmpty()) {
            Text(
                text = wort,
                color = if (schwach) palette.danger else palette.onTile,
                fontSize = dpSp(singleLineSizeSp(wort, cellWidth.value, cellHeight.value, scale, maxSp = 22f)),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
