package org.biglau.search

import java.text.Normalizer

/**
 * search over labelled entries: apps, contacts, later settings.
 *
 * scrolling through hundreds of entries is no way to operate three inches, so the search has
 * to do more than prefixes. pure functions, because the ranking decides what someone sees
 * first after two letters.
 */
object TextSearch {

    /** lowercase without diacritics, so "muller" finds "Müller". */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .replace("ß", "ss")
            .lowercase()

    /**
     * match quality, lower is better, null means no match.
     * 0 = the name starts with it, 1 = a word in the name does, 2 = it occurs somewhere.
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
     * every token must match: "goog map" finds "Google Maps", "goog zzz" nothing. ties break
     * alphabetically so the order does not jump between two key presses.
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
