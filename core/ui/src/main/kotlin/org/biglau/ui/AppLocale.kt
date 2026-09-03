package org.biglau.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale
import org.biglau.data.ConfigStore
import org.biglau.data.Language

/**
 * Die Sprache der App, unabhaengig von der des Telefons. PLAN.md 4.9.
 *
 * Der Grund ist nicht Bequemlichkeit: viele Telefone stehen auf einer Sprache, die
 * jemand anderes eingestellt hat - der Sohn beim Einrichten, der Haendler, die
 * Werkseinstellung. Wer das System umstellen wollte, muesste sich erst durch die
 * Systemeinstellungen in einer Sprache arbeiten, die er nicht liest. Genau die Falle,
 * gegen die diese App antritt.
 *
 * Ab Android 13 gibt es dafuer eine Systemfunktion. Dieses Geraet laeuft auf Android 11,
 * also wird der Context beim Start umgehaengt - der Weg, der ueberall funktioniert.
 */
object AppLocale {

    /** null heisst: die Sprache des Telefons, was immer sie ist. */
    fun localeFor(language: Language): Locale? = when (language) {
        Language.SYSTEM -> null
        Language.GERMAN -> Locale.GERMAN
        Language.ENGLISH -> Locale.ENGLISH
    }

    /** Die drei Stufen im Kreis, wie bei der Haptik. */
    fun next(current: Language): Language = when (current) {
        Language.SYSTEM -> Language.GERMAN
        Language.GERMAN -> Language.ENGLISH
        Language.ENGLISH -> Language.SYSTEM
    }

    fun wrap(base: Context, language: Language): Context {
        val locale = localeFor(language) ?: return base
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return base.createConfigurationContext(configuration)
    }

    fun needsRecreate(attached: Language, current: Language): Boolean = attached != current

    /**
     * Der Context in der Sprache der **App** - fuer alles ausserhalb einer Activity.
     *
     * [wrap] hing bis hierher nur an `BigLauActivity`. Jede Meldung, jeder Wecker und der
     * Vorgabetext des Notrufs holten ihre Texte dagegen aus dem rohen Anwendungs-Context -
     * also in der Sprache des **Telefons**. Am Emulator gesehen: die Oberflaeche auf
     * Deutsch, die Meldung darueber auf Englisch.
     *
     * Das trifft genau den Fall, fuer den es diese Einstellung ueberhaupt gibt: ein Telefon,
     * dessen Systemsprache jemand anderes gesetzt hat.
     */
    fun forApp(context: Context): Context =
        wrap(context, ConfigStore.get(context).current.appearance.language)
}

/**
 * Die Sprache, in der gerade gemalt wird - aus der Konfiguration des Contexts, nicht aus
 * [Locale.getDefault].
 *
 * Der Unterschied faellt beim Datum auf: nach dem Umstellen auf Deutsch stand ueber dem
 * Startbildschirm weiter "Tue, 1. Sep". Die Texte kamen aus den Ressourcen und waren
 * deutsch, der Wochentag kam aus der Standardsprache des Prozesses und blieb englisch.
 */
@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
