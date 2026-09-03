package org.biglau.a11y

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Liest Beschriftungen vor.
 *
 * Bewusst eine eigene Sprachausgabe und nicht TalkBack: TalkBack aendert die gesamte
 * Bedienung des Telefons - jeder Tipp wird zum Doppeltipp. Wer nur wissen will, was auf
 * einer Kachel steht, soll dafuer nicht die Bedienung des ganzen Geraets umstellen muessen.
 *
 * Die Sprache wird **mitgegeben**, nicht geholt. Vorher fragte diese Datei selbst beim
 * `ConfigStore` und bei `AppLocale` nach - und hing damit an `core:ui`, obwohl an ihr nichts
 * Oberflaeche ist. Wer vorlesen laesst, weiss ohnehin, in welcher Sprache.
 */
object Speaker {

    private var engine: TextToSpeech? = null
    private var ready = false

    fun warmUp(context: Context, sprache: Locale) {
        if (engine != null) return
        engine = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            // Die Stimme spricht die Sprache der App, nicht die des Telefons. Sonst
            // liest sie eine deutsche Kachelbeschriftung englisch vor, und heraus kommt
            // Kauderwelsch - ausgerechnet fuer den, der aufs Vorlesen angewiesen ist.
            if (ready) runCatching { engine?.language = sprache }
        }
    }

    fun say(context: Context, text: String, sprache: Locale) {
        if (text.isBlank()) return
        warmUp(context, sprache)
        val instance = engine ?: return
        runCatching { instance.speak(text, TextToSpeech.QUEUE_FLUSH, null, "biglau") }
    }

    fun shutdown() {
        runCatching { engine?.stop(); engine?.shutdown() }
        engine = null
        ready = false
    }
}
