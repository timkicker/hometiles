package org.biglau.a11y

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import org.biglau.data.ConfigStore
import org.biglau.ui.AppLocale

/**
 * Liest Beschriftungen vor.
 *
 * Bewusst eine eigene Sprachausgabe und nicht TalkBack: TalkBack aendert die gesamte
 * Bedienung des Telefons - jeder Tipp wird zum Doppeltipp. Wer nur wissen will, was auf
 * einer Kachel steht, soll dafuer nicht die Bedienung des ganzen Geraets umstellen muessen.
 */
object Speaker {

    private var engine: TextToSpeech? = null
    private var ready = false

    fun warmUp(context: Context) {
        if (engine != null) return
        engine = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            // Die Stimme spricht die Sprache der App, nicht die des Telefons. Sonst
            // liest sie eine deutsche Kachelbeschriftung englisch vor, und heraus kommt
            // Kauderwelsch - ausgerechnet fuer den, der aufs Vorlesen angewiesen ist.
            if (ready) runCatching { engine?.language = spoken(context) }
        }
    }

    fun say(context: Context, text: String) {
        if (text.isBlank()) return
        warmUp(context)
        val instance = engine ?: return
        runCatching { instance.speak(text, TextToSpeech.QUEUE_FLUSH, null, "biglau") }
    }

    /** Die App-Sprache, sonst die des Telefons. */
    private fun spoken(context: Context): Locale =
        AppLocale.localeFor(ConfigStore.get(context).current.appearance.language)
            ?: Locale.getDefault()

    fun shutdown() {
        runCatching { engine?.stop(); engine?.shutdown() }
        engine = null
        ready = false
    }
}
