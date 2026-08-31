package org.biglau.notify

/** Eine Benachrichtigung, so weit sie uns interessiert - ohne Android-Typen, damit pruefbar. */
data class NotificationRow(
    val packageName: String,
    val clearable: Boolean,
    val ongoing: Boolean,
    val groupSummary: Boolean,
    val number: Int = 0,
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

    /** Zaehlt diese eine Benachrichtigung? */
    fun counts(row: NotificationRow): Boolean = when {
        row.ongoing -> false        // laufende Wiedergabe, Navigation, Dateiuebertragung
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
