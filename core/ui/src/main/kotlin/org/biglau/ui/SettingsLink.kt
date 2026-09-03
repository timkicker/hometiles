package org.biglau.ui

import android.content.Context
import android.content.Intent

/**
 * Der Weg in die Einstellungen - ueber eine Absicht, nicht ueber eine Klasse.
 *
 * Vier Bildschirme aus vier verschiedenen Ecken springen auf eine Unterseite der
 * Einstellungen: die Waehltastatur zu den Anrufarten, die Kontakte zu ihrer Sortierung, die
 * App-Liste zu den ausgeblendeten Apps, der Notruf zu den Notfallkontakten. Jeder von ihnen
 * nannte dafuer `SettingsActivity` beim Namen - und damit haengt in `PLAN.md` 2.1 jedes
 * kuenftige `feature:*` am Einstellungsbaum, obwohl die Regel dort sagt, dass ein Feature nie
 * ein anderes importiert.
 *
 * Hier steht deshalb nur, **was** man will („die Einstellungen, Seite X"), und die
 * Einstellungen selbst sagen im Manifest, dass sie das beantworten. `SettingsLinkTest` haelt
 * beide Seiten zusammen: gaebe es den Filter nicht, fuehre der Sprung ins Leere - und zwar
 * still.
 */
object SettingsLink {

    /** Die Absicht. Steht wortgleich im Manifest von `SettingsActivity`. */
    const val ACTION = "org.biglau.action.SETTINGS"

    /** Die Unterseite, als Name eines `Page`-Werts. */
    const val EXTRA_PAGE = "biglau.settings.page"

    /** Welche Anrufarten in der Liste erscheinen. */
    const val PAGE_CALL_TYPES = "CALL_TYPES"

    /** Sortierung, Nummernsuche und Favoriten der Kontakte. */
    const val PAGE_CONTACTS = "CONTACTS"

    /** Die Notfallkontakte. */
    const val PAGE_SOS = "SOS"

    /** Die ausgeblendeten Apps. */
    const val PAGE_HIDDEN_APPS = "HIDDEN_APPS"

    /**
     * Die Einstellungen von vorn, ohne Unterseite.
     *
     * Gebraucht von der App-Liste: wer seine Einstellungs-Kachel weggibt, hat sonst keinen
     * Weg mehr dorthin. Am 3.9.2026 am Geraet des Nutzers gesehen - acht belegte Kacheln,
     * keine davon die Einstellungen, Wischen aus, und die App-Liste bot nichts an.
     */
    fun toRoot(context: Context): Intent =
        Intent(ACTION).setPackage(context.packageName)

    /**
     * Die Absicht auf eine Unterseite. `setPackage` haelt sie im eigenen Programm - sonst
     * bekaeme sie ein fremdes Programm zu sehen, das denselben Namen anbietet.
     */
    fun toPage(context: Context, page: String): Intent =
        Intent(ACTION).setPackage(context.packageName).putExtra(EXTRA_PAGE, page)
}
