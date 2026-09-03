package org.biglau.ui

import android.content.Context
import androidx.activity.ComponentActivity
import android.os.Bundle
import org.biglau.data.ConfigStore
import org.biglau.data.Language

/**
 * Gemeinsame Grundlage aller Bildschirme: haengt den Context auf die eingestellte Sprache
 * um, bevor irgendein Text nachgeschlagen wird.
 *
 * Als Basisklasse und nicht als Aufruf in jedem `onCreate`, weil ein vergessener Aufruf
 * genau einen Bildschirm in der falschen Sprache liesse - und das faellt erst dem auf,
 * der ihn nicht lesen kann.
 */
abstract class BigLauActivity : ComponentActivity() {

    private var attachedLanguage: Language = Language.SYSTEM

    override fun attachBaseContext(newBase: Context) {
        attachedLanguage = ConfigStore.get(newBase).current.appearance.language
        super.attachBaseContext(AppLocale.wrap(newBase, attachedLanguage))
    }

    /**
     * Ein Bildschirm, der schon lief, als die Sprache umgestellt wurde, haelt noch die
     * alten Texte - der Startbildschirm etwa, der die ganze Zeit im Hintergrund steht.
     * Beim Zurueckkommen wird er neu aufgebaut, sonst bliebe ausgerechnet die Seite
     * fremdsprachig, auf der man landet.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Eine Stelle statt zwoelfmal `portrait` im Manifest.
        requestedOrientation =
            Orientation.requested(ConfigStore.get(this).current.appearance.orientation)
    }

    override fun onResume() {
        super.onResume()
        val aussehen = ConfigStore.get(this).current.appearance
        // Auch die Ausrichtung: ein Bildschirm, der schon lief, als sie umgestellt wurde,
        // bliebe sonst hochkant stehen - der Startbildschirm etwa, der die ganze Zeit im
        // Hintergrund ist. Dann sieht die Einstellung aus wie ein Schalter, der klemmt.
        val gewuenscht = Orientation.requested(aussehen.orientation)
        if (requestedOrientation != gewuenscht) requestedOrientation = gewuenscht
        if (AppLocale.needsRecreate(attachedLanguage, aussehen.language)) recreate()
    }
}
