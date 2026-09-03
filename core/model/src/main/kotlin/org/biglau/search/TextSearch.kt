package org.biglau.search

import java.text.Normalizer

/**
 * Suche ueber beschriftete Eintraege - Apps, Kontakte, spaeter Einstellungen. Auf drei Zoll
 * ist Scrollen durch hunderte Eintraege keine Bedienung, also muss die Suche mehr koennen als
 * Praefixe.
 *
 * Reine Funktionen, damit die Rangfolge pruefbar bleibt: sie entscheidet, was der Nutzer
 * nach zwei Buchstaben als Erstes sieht.
 */
object TextSearch {

    /** Kleinschreibung ohne diakritische Zeichen, damit "muller" auch "Müller" findet. */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .replace("ß", "ss")
            .lowercase()

    /**
     * Guete eines Treffers, kleiner ist besser; null heisst kein Treffer.
     * 0 = der Name beginnt damit, 1 = ein Wort im Namen beginnt damit, 2 = kommt irgendwo vor.
     */
    fun rank(label: String, token: String): Int? {
        if (token.isEmpty()) return 0
        val haystack = normalize(label)
        val needle = normalize(token)
        return when {
            haystack.startsWith(needle) -> 0
            haystack.split(WORD_BREAK).any { it.startsWith(needle) } -> 1
            haystack.contains(needle) -> 2
            else -> null
        }
    }

    /**
     * Filtert und sortiert. Alle Wortteile der Anfrage muessen zutreffen - "goog map" findet
     * "Google Maps", "goog zzz" nichts. Sortiert wird nach dem besten Treffer, bei Gleichstand
     * alphabetisch, damit die Reihenfolge zwischen zwei Tastendruecken nicht springt.
     */
    fun <T> filter(items: List<T>, query: String, label: (T) -> String): List<T> {
        val tokens = query.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return items

        return items
            .mapNotNull { item ->
                val text = label(item)
                val ranks = tokens.map { rank(text, it) }
                if (ranks.any { it == null }) null else item to ranks.filterNotNull().min()
            }
            .sortedWith(compareBy({ it.second }, { normalize(label(it.first)) }))
            .map { it.first }
    }

    private val DIACRITICS = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    private val WORD_BREAK = "[\\s._\\-/]+".toRegex()
    private val WHITESPACE = "\\s+".toRegex()
}
