package org.biglau.web

/**
 * the address behind a website tile.
 *
 * typing happens on three inches with big keys, and nobody types "https://" willingly. the
 * input is completed rather than rejected: a tile saying "invalid address" because the
 * prefix is missing is worse than one that adds it.
 */
object LinkTarget {

    /**
     * turns an input into a usable address, or `null`.
     *
     * adds `https://`, deliberately not `http://`: whoever types a bare address today means
     * the encrypted one, and the server redirects if it must.
     */
    fun normalise(input: String): String? {
        val raw = input.trim()
        if (raw.isEmpty()) return null
        // whitespace in the middle means it was not an address.
        if (raw.any { it.isWhitespace() }) return null

        val withScheme = when {
            raw.startsWith("https://", ignoreCase = true) -> raw
            raw.startsWith("http://", ignoreCase = true) -> raw
            // other schemes stay as they are; tel:, mailto: and geo: have their point.
            SCHEME.containsMatchIn(raw) -> raw
            else -> "https://$raw"
        }
        return if (hostOf(withScheme).isNullOrEmpty() && !SCHEME.containsMatchIn(raw)) null else withScheme
    }

    /** the host name, which is what the tile shows when no label is set. */
    fun hostOf(url: String): String? {
        val withoutScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (withoutScheme.isEmpty()) return null
        val host = withoutScheme.substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfter('@')
            .substringBefore(':')
        if (host.isEmpty() || !host.contains('.')) return null
        return host.removePrefix("www.").lowercase()
    }

    fun labelFor(url: String): String = hostOf(url) ?: url

    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
}
