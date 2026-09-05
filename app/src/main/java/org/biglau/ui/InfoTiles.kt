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
 * Uhrzeit auf einer Kachel. Aktualisiert sich zur vollen Minute, nicht sekuendlich -
 * auf einem 2000-mAh-Akku ist das ein Unterschied, und Sekunden zeigt sie ohnehin nicht.
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
        // Gemessen statt geschaetzt: `singleLineSizeSp` rechnet mit einer mittleren
        // Zeichenbreite, und bei 200 % Textgroesse und Hyperlegible stand hier „2:33" -
        // das „AM" war abgeschnitten. Siehe fittedSingleLineDp.
        val zeitStil = tabularFigures().copy(fontWeight = FontWeight.Bold)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val zeitSp = fittedSingleLineDp(
                text = timeText,
                style = zeitStil,
                desiredDp = timeSize,
                maxWidth = maxWidth,
            )
            Text(
                text = timeText,
                style = zeitStil,
                color = palette.onTile,
                fontSize = dpSp(zeitSp),
                maxLines = 1,
                softWrap = false,
            )
        }
        // Erst kuerzen, dann umbrechen: „Wednesday, September 2" passte auf der Kachel
        // nicht in eine Zeile, und in der zweiten stand die 2 allein. Gemessen wird gegen
        // die Breite, die die Kachel wirklich hergibt - `cellWidth` ist die Zelle, nicht
        // der Platz darin -, und mit dem laengsten Datum des Jahres statt dem heutigen.
        val stufen = ClockFormat.dateSkeletons(clock, onTile = true)
        if (stufen.isNotEmpty()) {
            val messer = rememberTextMeasurer()
            val dichte = LocalDensity.current
            val dateSp = dpSp(dateSize)
            // Mit dem Stil messen, der auch gezeichnet wird: die Schrift des Nutzers ist
            // breiter als die Standardschrift, und ein frisch gebautes TextStyle haette
            // sie nicht. Genau daran ist der erste Versuch gescheitert - die Messung sagte
            // „passt", und auf dem Geraet brach es trotzdem um.
            val grundstil = LocalTextStyle.current
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val breite = with(dichte) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
                val stil = grundstil.copy(fontSize = dateSp)
                val formate = remember(locale, stufen) {
                    stufen.mapNotNull { bestDatePattern(it, locale) }
                        .map { SimpleDateFormat(it, locale) }
                }
                val dateFormat = remember(formate, breite, dateSp) {
                    formate.firstOrNull { format ->
                        !messer.measure(
                            text = ClockFormat.longestDate(format),
                            style = stil,
                            maxLines = 1,
                            constraints = Constraints(maxWidth = breite),
                        ).hasVisualOverflow
                    } ?: formate.lastOrNull()
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
    val percentText = if (percent == null) "?" else "$percent %"
    val numberSize = singleLineSizeSp(percentText, cellWidth.value, cellHeight.value, scale, maxSp = 64f)

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Gemessen statt geschaetzt - siehe fittedSingleLineDp. Bei „100 %" und grosser
        // Schrift waere sonst das Prozentzeichen der erste Kandidat zum Abschneiden.
        val zahlStil = tabularFigures().copy(fontWeight = FontWeight.Bold)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Text(
                text = percentText,
                // Nicht in Warnfarbe: gemessen kommt das Rot auf jedem der sechs Kacheltoene
                // auf 1,4 bis 1,8 zu 1 - unter jeder Schwelle aus PLAN.md 3.3, und das
                // ausgerechnet bei neun Prozent. Die Zahl selbst ist die Warnung.
                style = zahlStil,
                color = palette.onTile,
                fontSize = dpSp(
                    fittedSingleLineDp(percentText, zahlStil, numberSize, maxWidth),
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
                                palette.onTile
                            } else {
                                palette.onTile.copy(alpha = 0.25f)
                            },
                        ),
                )
            }
        }
        if (wort.isNotEmpty()) {
            val wortStil = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)
            BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    text = wort,
                    color = palette.onTile,
                    style = wortStil,
                    fontSize = dpSp(
                        fittedSingleLineDp(
                            text = wort,
                            style = wortStil,
                            desiredDp = singleLineSizeSp(
                                wort, cellWidth.value, cellHeight.value, scale, maxSp = 22f,
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
