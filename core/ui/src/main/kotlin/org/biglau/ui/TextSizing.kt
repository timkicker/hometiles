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
 * Wandelt eine in dp gedachte Groesse in eine Schriftgroesse um, die von der
 * Systemschriftskalierung unberuehrt bleibt.
 *
 * Warum das noetig ist (PLAN.md 3.2): unsere Kachel- und Kopfzeilenschriften werden aus der
 * verfuegbaren Flaeche gerechnet - die Flaeche ist bereits die Antwort auf "wie gross darf
 * das sein". Wuerde die Systemskalierung obendrauf multiplizieren, liefe der Text ueber:
 * auf dem Zielgeraet steht sie auf 1,35, aus 26 werden also 35, und die zweite Zeile fiel
 * unten aus der Kopfzeile heraus.
 *
 * Fuer Fliesstext in den Einstellungen gilt das nicht - dort folgt die App der
 * Systemeinstellung wie jede andere App auch.
 */
@Composable
@ReadOnlyComposable
fun dpSp(value: Float): TextUnit = with(LocalDensity.current) { value.dp.toSp() }

/**
 * Tabellenziffern - `PLAN.md` 3.7: „Zahlen (Anrufliste, Waehltastatur, Dauer) mit
 * Tabellenziffern […], damit Spalten nicht springen."
 *
 * **Vom Stil der Oberflaeche aus, nicht frisch gebaut.** Hier stand ein eigenstaendiges
 * `TextStyle`, und `Text(style = …)` *ersetzt* den Stil der Umgebung, statt ihn zu
 * ergaenzen: Uhr, Ladestand, Waehltastatur und Gespraechsdauer standen damit in der
 * Systemschrift, waehrend alles daneben in der eingestellten Schrift stand. Der Absatz
 * darunter sagt „die mitgelieferte Schrift kann tnum" - genau die war an diesen Stellen
 * nicht im Einsatz.
 *
 * Gemessen, bevor es das gab: „11 %" in der Kopfzeile war 64 Pixel breit, „88 %" 74. Die
 * Anzeige rutschte also bei jedem Prozent hin und her, und dasselbe tat die Uhr zur vollen
 * Minute und die Gespraechsdauer im Sekundentakt. Die mitgelieferte Schrift kann `tnum`;
 * sie wurde nur nie danach gefragt.
 */
@Composable
@ReadOnlyComposable
fun tabellenZiffern(): TextStyle =
    LocalTextStyle.current.copy(fontFeatureSettings = "tnum")

/**
 * Fliesstext in der eingestellten Textgroesse.
 *
 * Die Einstellung heisst schlicht **„Textgroesse"** - und hielt nur zur Haelfte, was sie
 * versprach: Kacheln, Zeilen und Ueberschriften wuchsen mit, der erklaerende Text daneben
 * nicht. Bei 200 % standen also grosse Knoepfe neben kleiner Schrift, und zwar bei genau
 * den Saetzen, die man am ehesten vergroessert lesen will.
 *
 * Der Unterschied zu [dpSp]: dort ist die Groesse aus der Flaeche gerechnet und darf
 * **nicht** noch einmal skaliert werden, sonst laeuft sie ueber. Hier ist sie gesetzt, und
 * die Einstellung des Nutzers gehoert obendrauf - zusaetzlich zur Systemschrift, der diese
 * Texte wie in jeder anderen App folgen.
 */
@Composable
@ReadOnlyComposable
fun bigSp(value: Float): TextUnit = (value * org.biglau.ui.theme.LocalTextScale.current).sp

/**
 * Die eingestellte Textgroesse, nach oben begrenzt.
 *
 * Fuer Bildschirme, die **nicht scrollen** und deren Knopfhoehe fest ist: dort frisst jede
 * weitere Vergroesserung die Flaeche, auf die man tippen muss, oder schneidet die
 * Beschriftung ab. Zweimal am Emulator gesehen - die PIN-Tasten waren bei 200 % noch 21 dp
 * hoch, und im Gespraech stand auf dem Knopf "Lautsprec…".
 *
 * Nach unten wird nie begrenzt: wer 75 % einstellt, bekommt 75 %.
 */
fun cappedTextScale(current: Float, max: Float): Float = minOf(current, max)

/**
 * Das laengste Wort eines Textes - das, an dem die Zeile bricht.
 *
 * Compose trennt ein Wort mitten hindurch, wenn es allein nicht in die Zeile passt. Bei
 * 200 % stand ueber der Ruecksetzen-Seite „Alles zuruecksetze / n". Ob eine Ueberschrift
 * passt, entscheidet also nicht ihre Laenge, sondern ihr laengstes Wort.
 */
fun longestWord(text: String): String =
    text.split(' ', '\n', '\t').maxByOrNull { it.length }.orEmpty().ifEmpty { text }

/**
 * Die groesste Schriftgroesse bis [wunschDp], bei der [text] in **eine** Zeile passt.
 *
 * `singleLineSizeSp` rechnet mit einer mittleren Zeichenbreite von 0,60 - eine Schaetzung,
 * und sie lag daneben: auf dem Telefon des Nutzers (03.09.2026) (Hyperlegible, Textgroesse 200 %) stand
 * auf der Uhr-Kachel **„2:33"**. Das „AM" war abgeschnitten, lautlos, weil die Zeile
 * `softWrap = false` und kein Kuerzungszeichen hat. Eine Uhrzeit ohne AM/PM ist auf einem
 * Zwoelfstundentelefon keine Uhrzeit.
 *
 * Gemessen wird mit dem Stil, der auch gezeichnet wird - dieselbe Lehre wie bei den
 * Kachelbeschriftungen: mit einer fremden Schrift gemessen kommt die falsche Stufe heraus.
 */
@Composable
fun fittedSingleLineDp(
    text: String,
    stil: TextStyle,
    wunschDp: Float,
    maxWidth: Dp,
    minDp: Float = 12f,
): Float {
    val messer = rememberTextMeasurer()
    val dichte = LocalDensity.current
    return remember(text, stil, wunschDp, maxWidth, minDp) {
        val breite = with(dichte) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
        largestFitting(wunschDp, minDp) { groesse ->
            !messer.measure(
                text = text,
                style = stil.copy(fontSize = with(dichte) { groesse.dp.toSp() }),
                maxLines = 1,
                softWrap = false,
                constraints = Constraints(maxWidth = breite),
            ).hasVisualOverflow
        }
    }
}

/**
 * Die groesste ganze dp-Stufe zwischen [minDp] und [wunschDp], fuer die [passt] gilt.
 *
 * Halbierend statt Schritt fuer Schritt: die Uhr wechselt jede Minute den Text, und von 64
 * dp abwaerts waeren das bis zu zweiundfuenfzig Messungen - jede eine Textvermessung mit
 * Schriftladen. So sind es sieben. Auf einem Telefon, dessen Kaltstart eine Sekunde dauert,
 * ist das kein Feilen an Nachkommastellen.
 *
 * Vorausgesetzt ist, dass groessere Schrift breiter ist: passt eine Stufe, passen alle
 * kleineren. Fuer eine feste Zeichenkette gilt das.
 */
internal fun largestFitting(wunschDp: Float, minDp: Float, passt: (Float) -> Boolean): Float {
    if (wunschDp <= minDp) return minDp
    if (passt(wunschDp)) return wunschDp
    var unten = minDp.toInt()
    var oben = wunschDp.toInt()
    while (unten + 1 < oben) {
        val mitte = (unten + oben) / 2
        if (passt(mitte.toFloat())) unten = mitte else oben = mitte
    }
    return unten.toFloat().coerceAtLeast(minDp)
}
