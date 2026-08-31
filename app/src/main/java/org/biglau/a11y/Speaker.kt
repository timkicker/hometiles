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
 */
object Speaker {

    private var engine: TextToSpeech? = null
    private var ready = false

    fun warmUp(context: Context) {
        if (engine != null) return
        engine = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) runCatching { engine?.language = Locale.getDefault() }
        }
    }

    fun say(context: Context, text: String) {
        if (text.isBlank()) return
        warmUp(context)
        val instance = engine ?: return
        runCatching { instance.speak(text, TextToSpeech.QUEUE_FLUSH, null, "biglau") }
    }

    fun shutdown() {
        runCatching { engine?.stop(); engine?.shutdown() }
        engine = null
        ready = false
    }
}
