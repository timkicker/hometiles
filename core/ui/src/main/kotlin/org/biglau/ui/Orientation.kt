package org.biglau.ui

import android.content.pm.ActivityInfo
import org.biglau.data.ScreenOrientation

/**
 * Die Drehung des Bildschirms. PLAN.md 4.2.
 *
 * Bisher stand `portrait` zwoelfmal im Manifest - eine Entscheidung, die im Plan als
 * Einstellung zugesagt war und nirgends zu aendern. Jetzt entscheidet die Konfiguration,
 * und zwar an einer Stelle statt zwoelfmal.
 */
object Orientation {

    fun requested(orientation: ScreenOrientation): Int = when (orientation) {
        ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        // Bewusst SENSOR und nicht UNSPECIFIED: "automatisch" soll sich auch dann drehen,
        // wenn im System die Drehsperre aus ist - sonst waere die Wahl folgenlos, und der
        // Nutzer suchte den Fehler bei uns.
        ScreenOrientation.AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }
}
