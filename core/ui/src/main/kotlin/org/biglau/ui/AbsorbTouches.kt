package org.biglau.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Schluckt jeden Tipp, der nicht schon von einer Kachel darin verbraucht wurde.
 *
 * Fuer Bildschirme, die sich **ueber** einen anderen legen statt ihn zu ersetzen. Ohne das
 * geht ein Tipp neben eine Kachel - in den Rand, in die Fuge - durch auf das, was darunter
 * liegt. Am 04.09.2026 am Jelly 2 gemessen: bei offenem Ordner startete ein Tipp auf
 * (14, 250), also im linken Rand des Ordners, die Kontakte-Kachel des Startbildschirms.
 *
 * Auf drei Zoll ist der Rand ein paar Bildpunkte breit, und die Hand, fuer die BigLau
 * gebaut ist, trifft ihn regelmaessig. Ein Tipp, der etwas Unsichtbares startet, ist der
 * schlimmste Fehlgriff, den ein Startbildschirm anbieten kann.
 *
 * Die Kacheln der Ueberlagerung selbst merken nichts davon: sie bekommen den Tipp zuerst
 * und verbrauchen ihn; hier landet nur, was niemand haben wollte.
 */
fun Modifier.absorbTouches(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        while (true) {
            val ereignis = awaitPointerEvent()
            ereignis.changes.forEach { it.consume() }
            if (ereignis.changes.all { !it.pressed }) break
        }
    }
}
