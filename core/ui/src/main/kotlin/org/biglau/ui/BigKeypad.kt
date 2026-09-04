package org.biglau.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.biglau.ui.theme.LocalCornerRadius
import org.biglau.core.ui.R
import org.biglau.ui.theme.LocalBigPalette
import org.biglau.ui.theme.tileBorder

/**
 * Zifferntastatur in der Formsprache der Kacheln. Die Tasten fuellen die verfuegbare Flaeche
 * aus, statt eine feste Groesse zu haben - auf drei Zoll ist jede ungenutzte Flaeche eine
 * verpasste Trefferflaeche.
 *
 * Wird spaeter die Grundlage der Waehltastatur.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BigKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    onLongDigit: ((Char) -> Unit)? = null,
    /** Kurzhinweis unter der Ziffer, etwa der Name der Kurzwahl. */
    hintFor: (Char) -> String? = { null },
    /**
     * Zusatztaste unten links. Die Waehltastatur setzt dort das Plus fuer Auslandsnummern;
     * die PIN-Eingabe laesst den Platz leer, weil dort kein Plus hingehoert.
     */
    extraKey: Char? = null,
    /**
     * Soll die Tastatur beim Erscheinen den Fokus holen?
     *
     * Nur, wo sie die Hauptsache des Bildschirms ist - bei der PIN-Eingabe. Die
     * Waehltastatur hat ein Nummernfeld darueber und soll es ihm nicht wegnehmen.
     */
    holtFokus: Boolean = false,
    /**
     * Wohin der Fokus geht, wenn er unten aus der Tastatur hinauslaeuft.
     *
     * Unter der PIN-Tastatur steht `Fertig`. Ohne diesen Anker waere die Zeile mit Tasten
     * unerreichbar - derselbe Fall wie der Streifen unter dem Rasterrahmen, und dieselbe
     * Antwort: ein benannter Anker statt einer blinden Suche.
     */
    unten: FocusRequester? = null,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(extraKey?.toString() ?: "", "0", "⌫"),
    )

    // Die Bewegung wird gefuehrt, nicht gesucht.
    //
    // Am 04.09.2026 gemessen: ueber dem Startbildschirm bekam die Tastatur den Fokus zwar
    // auf die Eins, aber jede Richtungstaste lief ins Leere - Compose sucht das naechste
    // Ziel und findet es unter der Ueberlagerung, auf Kacheln, die niemand sieht. Nach
    // zehnmal hoch war der Fokus ganz weg, und damit die gesperrte App nicht mehr zu
    // oeffnen.
    //
    // Ein `moveFocus` in die Richtung der Taste war der erste Versuch und half nicht: am
    // Rand verliess es die Tastatur genauso. Deshalb je Taste ein Anker und eine eigene
    // Rechnung, wie im Rasterrahmen. Leere Plaetze werden uebersprungen; am Rand passiert
    // nichts, ausser unten, wo [unten] steht.
    val anker = remember(rows.size) { rows.map { zeile -> zeile.map { FocusRequester() } } }
    var wo by remember { mutableStateOf(0 to 0) }

    fun belegt(zeile: Int, spalte: Int): Boolean =
        rows.getOrNull(zeile)?.getOrNull(spalte)?.isNotEmpty() == true

    /** Die naechste belegte Taste in dieser Richtung, oder null am Rand. */
    fun nachbar(zeile: Int, spalte: Int, dz: Int, ds: Int): Pair<Int, Int>? {
        var z = zeile + dz
        var sp = spalte + ds
        while (z in rows.indices && sp in 0..2) {
            if (belegt(z, sp)) return z to sp
            z += dz
            sp += ds
        }
        return null
    }

    fun geheZu(ziel: Pair<Int, Int>?): Boolean {
        if (ziel == null) return false
        wo = ziel
        return runCatching { anker[ziel.first][ziel.second].requestFocus() }.isSuccess
    }

    LaunchedEffect(holtFokus) { if (holtFokus) geheZu(0 to 0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { taste ->
                if (taste.type != KeyEventType.KeyDown) {
                    false
                } else {
                    val (z, sp) = wo
                    when (taste.key) {
                        Key.DirectionUp -> {
                            geheZu(nachbar(z, sp, -1, 0))
                            true
                        }
                        Key.DirectionDown -> {
                            val ziel = nachbar(z, sp, 1, 0)
                            if (ziel != null) {
                                geheZu(ziel)
                            } else {
                                // Unter der letzten Reihe steht, was der Aufrufer angibt.
                                unten?.let { runCatching { it.requestFocus() } }
                            }
                            true
                        }
                        Key.DirectionLeft -> {
                            geheZu(nachbar(z, sp, 0, -1))
                            true
                        }
                        Key.DirectionRight -> {
                            geheZu(nachbar(z, sp, 0, 1))
                            true
                        }
                        else -> false
                    }
                }
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEachIndexed { zeile, row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEachIndexed { spalte, key ->
                    val platz = Modifier
                        .focusRequester(anker[zeile][spalte])
                        .onFocusChanged { if (it.isFocused) wo = zeile to spalte }
                    when (key) {
                        "" -> Box(Modifier.weight(1f))
                        "⌫" -> KeypadKey(
                            Modifier.weight(1f).then(platz),
                            onClick = onBackspace,
                            // Ohne Beschreibung ist die Taste fuer TalkBack stumm - und
                            // im Baum ueberhaupt nicht auffindbar.
                            description = stringResource(R.string.keypad_backspace),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = null,
                                tint = LocalBigPalette.current.onBackground,
                            )
                        }
                        else -> KeypadKey(
                            Modifier.weight(1f).then(platz),
                            onClick = { onDigit(key[0]) },
                            onLongClick = onLongDigit?.let { handler -> { handler(key[0]) } },
                            description = key,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = key,
                                    color = LocalBigPalette.current.onBackground,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                hintFor(key[0])?.let { hint ->
                                    Text(
                                        text = hint,
                                        color = LocalBigPalette.current.accent,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadKey(
    modifier: Modifier,
    onClick: () -> Unit,
    description: String,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val palette = LocalBigPalette.current
    val border = palette.tileBorder()
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(LocalCornerRadius.current))
            .background(palette.emptyTile)
            .then(if (border != null) Modifier.border(3.dp, border, RoundedCornerShape(LocalCornerRadius.current)) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/** Wie deutlich der Rand eines leeren Punktes ist - siehe die Begruendung an [PinDots]. */
internal const val PUNKTRAND = 0.55f

/**
 * Punktreihe fuer eine verdeckte Eingabe.
 *
 * Wie viele Ziffern schon dastehen, sagten bis zum 04.09.2026 allein die gefuellten Punkte.
 * Wer sie nicht sieht, bekam auf diesem Bildschirm ueberhaupt keine Rueckmeldung: BigLau
 * macht absichtlich keinen Ton, und ob eine Taste angekommen ist, war nur zu sehen. Die
 * Zahl der Ziffern verraet die PIN nicht; sie zu verschweigen hilft niemandem.
 *
 * `liveRegion`, damit es beim Tippen gesagt wird und nicht erst beim Antasten.
 */
@Composable
fun PinDots(length: Int, total: Int = 8, modifier: Modifier = Modifier) {
    val palette = LocalBigPalette.current
    val ansage = if (length == 0) {
        stringResource(R.string.a11y_pin_empty)
    } else {
        pluralStringResource(R.plurals.a11y_pin_digits, length, length)
    }
    Row(
        modifier = modifier.semantics {
            contentDescription = ansage
            liveRegion = LiveRegionMode.Polite
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (index < length) palette.accent else palette.emptyTile)
                    // Der Rand traegt den leeren Punkt allein: seine Fuellung ist die
                    // stille Kachelfarbe und steht mit 1,09:1 (dunkel) bis 1,16:1 (hell)
                    // praktisch auf dem Hintergrund. Bei 0,35 kam der Rand im hellen Thema
                    // auf 2,23:1 und im Kontrastthema auf 2,64 - unter der Flaechenschwelle
                    // von 3,0. Am 04.09.2026 am Emulator im Bildpunkt bestaetigt: (157,158,160)
                    // auf (232,234,236). Mit 0,55 sind es 6,12 / 3,94 / 5,28.
                    //
                    // Das ist dieselbe Sache wie beim Rahmen der leeren Kachel: wo die
                    // Fuellung absichtlich still ist, muss der Rand die Auffindbarkeit tragen.
                    .border(2.dp, palette.onBackground.copy(alpha = PUNKTRAND), RoundedCornerShape(50))
                    .size(18.dp)
            )
        }
    }
}
