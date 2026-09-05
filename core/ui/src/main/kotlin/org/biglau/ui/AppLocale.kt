package org.biglau.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale
import org.biglau.data.ConfigStore
import org.biglau.data.Language

/**
 * the app's language, apart from the phone's. `PLAN.md` 4.9.
 *
 * many phones stand in a language someone else set, and changing that means working
 * through system settings one cannot read. android 13 has a system function for this; the
 * device runs 11, so the context is wrapped at start instead.
 */
object AppLocale {

    /** null means the phone's language, whatever it is. */
    fun localeFor(language: Language): Locale? = when (language) {
        Language.SYSTEM -> null
        Language.GERMAN -> Locale.GERMAN
        Language.ENGLISH -> Locale.ENGLISH
        Language.FRENCH -> Locale.FRENCH
        Language.SPANISH -> Locale.forLanguageTag("es")
        Language.ITALIAN -> Locale.ITALIAN
    }

    fun wrap(base: Context, language: Language): Context {
        val locale = localeFor(language) ?: return base
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return base.createConfigurationContext(configuration)
    }

    fun needsRecreate(attached: Language, current: Language): Boolean = attached != current

    /**
     * the context in the *app's* language, for everything outside an activity.
     *
     * notices, alarms and the sos default text otherwise read the raw application context
     * and come out in the phone's language, over a screen in the app's.
     */
    fun forApp(context: Context): Context =
        wrap(context, ConfigStore.get(context).current.appearance.language)
}

/**
 * the language currently being drawn, from the context configuration and not from
 * [Locale.getDefault], which keeps the process language: the date read "Tue, 1. Sep".
 */
@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
