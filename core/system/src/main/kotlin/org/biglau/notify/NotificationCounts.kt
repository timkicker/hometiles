package org.biglau.notify

/** Eine Benachrichtigung, so weit sie uns interessiert - ohne Android-Typen, damit pruefbar. */
data class NotificationRow(
    val packageName: String,
    val clearable: Boolean,
    val ongoing: Boolean,
    val groupSummary: Boolean,
    val number: Int = 0,
    /** Was die App selbst sagt, wofuer die Anzeige da ist. Die meisten sagen nichts. */
    val category: String? = null,
    /** Traegt die Anzeige die Medien-Vorlage? Dann ist sie eine Wiedergabe, keine Nachricht. */
    val mediaStyle: Boolean = false,
)

/**
 * Zaehlt Benachrichtigungen pro Paket.
 *
 * Was hier falsch gezaehlt wird, blinkt spaeter grundlos - und eine Kachel, die immer blinkt,
 * ist schlimmer als gar kein Hinweis, weil man sie zu ignorieren lernt. Deshalb fliegen
 * dauerhafte Anzeigen raus (Musikwiedergabe, USB-Debugging, Ladeanzeige) und
 * Gruppenzusammenfassungen, die dieselben Nachrichten ein zweites Mal melden.
 */
object NotificationCounts {

    fun summarise(rows: List<NotificationRow>): Map<String, Int> = rows
        .filter { counts(it) }
        .groupingBy { it.packageName }
        .eachCount()

    /**
     * Anzeigen, die keine Nachricht an den Nutzer sind.
     *
     * Android laesst die App selbst sagen, wofuer eine Anzeige da ist. Was hier steht, ist
     * eine Anzeige **ueber etwas**, das laeuft oder gilt, und keine Nachricht, auf die man
     * antworten wuerde: die Wiedergabe, ein Dienst im Hintergrund, ein Fortschritt, eine
     * Navigation, das laufende Gespraech, der Wecker, die Stoppuhr, eine Statuszeile.
     */
    private val NICHT_GEMEINT = setOf(
        "transport", "service", "progress", "navigation", "call", "alarm", "stopwatch",
        "sys", "status",
    )

    /**
     * Zaehlt diese eine Benachrichtigung?
     *
     * `ongoing` allein reicht nicht, und das ist am 04.09.2026 am Jelly 2 aufgefallen:
     * **Spotify blinkte.** Eine Medienanzeige ist nur waehrend der Wiedergabe `ongoing`;
     * pausiert laesst sie sich wegwischen und ist damit nach der alten Regel eine
     * Nachricht. Sie ist aber weiter dieselbe Anzeige und meldet nichts Neues.
     *
     * Eine Kachel, die immer blinkt, ist schlimmer als gar kein Hinweis: man lernt, sie zu
     * ignorieren, und uebersieht dann die eine, die zaehlt.
     */
    fun counts(row: NotificationRow): Boolean = when {
        row.ongoing -> false        // laufende Wiedergabe, Navigation, Dateiuebertragung
        row.mediaStyle -> false     // Medien-Vorlage: eine Wiedergabe, auch pausiert
        row.category in NICHT_GEMEINT -> false
        !row.clearable -> false     // laesst sich nicht wegwischen, also keine Nachricht an den Nutzer
        row.groupSummary -> false   // meldet nur, was die Einzeleintraege schon melden
        else -> true
    }

    /** Anzahl fuer die Anzeige auf der Kachel; ueber 9 wird nicht mehr gezaehlt, sondern gestapelt. */
    fun badgeText(count: Int): String? = when {
        count <= 0 -> null
        count > 9 -> "9+"
        else -> count.toString()
    }
}
