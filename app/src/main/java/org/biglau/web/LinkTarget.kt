package org.biglau.web

/**
 * Die Adresse einer Webseiten-Kachel.
 *
 * Getippt wird auf drei Zoll mit großen Tasten, und niemand tippt „https://" freiwillig.
 * Die Eingabe wird deshalb ergänzt statt abgelehnt - eine Kachel, die „ungültige Adresse"
 * sagt, weil das Vorwort fehlt, ist schlechter als eine, die es hinzufügt.
 */
object LinkTarget {

    /**
     * Macht aus einer Eingabe eine brauchbare Adresse, oder `null`.
     *
     * Ergänzt `https://`, wenn kein Schema dasteht - bewusst https und nicht http: wer
     * heute eine Adresse ohne Vorwort tippt, meint die verschlüsselte Fassung, und der
     * Server leitet notfalls selbst um.
     */
    fun normalise(input: String): String? {
        val roh = input.trim()
        if (roh.isEmpty()) return null
        // Leerzeichen mitten in einer Adresse heißen: das war keine Adresse.
        if (roh.any { it.isWhitespace() }) return null

        val mitSchema = when {
            roh.startsWith("https://", ignoreCase = true) -> roh
            roh.startsWith("http://", ignoreCase = true) -> roh
            // Andere Schemata bleiben, wie sie sind - tel:, mailto:, geo: haben ihren Sinn.
            SCHEMA.containsMatchIn(roh) -> roh
            else -> "https://$roh"
        }
        return if (hostOf(mitSchema).isNullOrEmpty() && !SCHEMA.containsMatchIn(roh)) null else mitSchema
    }

    /** Der Rechnername, wie er auf der Kachel steht, wenn kein eigener Name gesetzt ist. */
    fun hostOf(url: String): String? {
        val ohneSchema = url.substringAfter("://", missingDelimiterValue = "")
        if (ohneSchema.isEmpty()) return null
        val host = ohneSchema.substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfter('@')
            .substringBefore(':')
        if (host.isEmpty() || !host.contains('.')) return null
        return host.removePrefix("www.").lowercase()
    }

    /** Was ohne eigene Beschriftung auf der Kachel steht. */
    fun labelFor(url: String): String = hostOf(url) ?: url

    private val SCHEMA = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
}
