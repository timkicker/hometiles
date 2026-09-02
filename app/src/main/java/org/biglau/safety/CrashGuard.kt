package org.biglau.safety

enum class StartMode { NORMAL, SAFE }

/**
 * Merkt sich, ob der letzte Start durchgekommen ist.
 *
 * Warum es das gibt: ein Absturz in einer Nebenansicht hat BigLau am 31.08.2026 die
 * Launcher-Rolle gekostet - Android raeumt bei wiederholten Abstuerzen die bevorzugten
 * Aktivitaeten. Wer dann kein zweites Telefon und kein adb hat, steht ohne Homescreen da.
 *
 * Die Regel: zwei aufeinanderfolgende Starts, die nie beim Zeichnen ankamen, schalten in
 * einen abgespeckten Modus. Der zeigt nur, was man zum Zurueckkommen braucht - und zwar
 * ohne die Konfiguration zu laden, denn die koennte ja gerade das Problem sein.
 *
 * Bewusst *zwei*: ein einzelner Absturz kann ein Ausrutscher sein, und wer nach jedem
 * Ausrutscher im Notmodus landet, traut der App nicht mehr.
 */
object CrashGuard {

    const val THRESHOLD = 2

    fun modeFor(consecutiveFailedStarts: Int): StartMode =
        if (consecutiveFailedStarts >= THRESHOLD) StartMode.SAFE else StartMode.NORMAL

    /** Beim Start hochzaehlen - heruntergesetzt wird erst, wenn wirklich gezeichnet wurde. */
    fun onStart(previous: Int): Int = (previous + 1).coerceAtMost(THRESHOLD * 5)

    /** Erfolgreich gezeichnet: der Zaehler faellt zurueck auf null. */
    fun onRendered(): Int = 0
}
