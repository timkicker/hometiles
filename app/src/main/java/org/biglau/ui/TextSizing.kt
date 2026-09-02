package org.biglau.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
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
 * Gemessen, bevor es das gab: „11 %" in der Kopfzeile war 64 Pixel breit, „88 %" 74. Die
 * Anzeige rutschte also bei jedem Prozent hin und her, und dasselbe tat die Uhr zur vollen
 * Minute und die Gespraechsdauer im Sekundentakt. Die mitgelieferte Schrift kann `tnum`;
 * sie wurde nur nie danach gefragt.
 */
val TabellenZiffern = TextStyle(fontFeatureSettings = "tnum")

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
