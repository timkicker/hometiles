package org.biglau.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

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
