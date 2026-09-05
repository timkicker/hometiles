package org.biglau.a11y

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * reads labels aloud.
 *
 * deliberately our own speech and not talkback: talkback changes how the whole phone is
 * operated, turning every tap into a double tap. wanting to know what a tile says should not
 * cost that.
 *
 * the language is **passed in**, not fetched: this file used to ask the config store and
 * `AppLocale` itself and hung off `core:ui` for it, although nothing here is surface.
 */
object Speaker {

    private var engine: TextToSpeech? = null
    private var ready = false

    fun warmUp(context: Context, language: Locale) {
        if (engine != null) return
        engine = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            // the voice speaks the app's language, not the phone's, or it reads a german
            // label in english - to the very person who depends on being read to.
            if (ready) runCatching { engine?.language = language }
        }
    }

    fun say(context: Context, text: String, language: Locale) {
        if (text.isBlank()) return
        warmUp(context, language)
        val instance = engine ?: return
        runCatching { instance.speak(text, TextToSpeech.QUEUE_FLUSH, null, "biglau") }
    }

    fun shutdown() {
        runCatching { engine?.stop(); engine?.shutdown() }
        engine = null
        ready = false
    }
}
