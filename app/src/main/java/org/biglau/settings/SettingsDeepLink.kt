package org.biglau.settings

/**
 * Der Sprung auf eine Unterseite der Einstellungen.
 *
 * Andere Bildschirme schicken einen dorthin, wo die passende Einstellung steht - aus der
 * Anrufliste zu den Anrufarten, aus den Kontakten zu deren Sortierung. Ohne diesen Weg
 * muesste man den Pfad durch die Einstellungen selbst finden, und der Hinweis "das laesst
 * sich einstellen" waere ein Versprechen ohne Weg.
 */
internal object SettingsDeepLink {

    /** Die zu [name] gehoerende Seite. Unbekannte Namen ergeben null statt einer Ausnahme. */
    fun ziel(name: String?): Page? = name?.let { gesucht ->
        Page.entries.firstOrNull { it.name == gesucht }
    }

    /** Die Seite, auf der die Einstellungen aufgehen. Das Schloss geht jedem Ziel vor. */
    fun start(locked: Boolean, ziel: Page?): Page = when {
        locked -> Page.GATE
        ziel != null -> ziel
        else -> Page.MAIN
    }

    /**
     * Wohin eine spaetere Anfrage fuehrt, oder null, wenn alles stehen bleibt.
     *
     * Am Schloss bleibt es stehen: sonst brauchte es nur einen Aufruf von aussen, um an der
     * PIN vorbei in die Einstellungen zu kommen.
     */
    fun sprung(aktuell: Page, ziel: Page?): Page? = when {
        ziel == null || aktuell == Page.GATE || aktuell == ziel -> null
        else -> ziel
    }
}
