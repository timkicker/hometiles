package org.biglau

import java.io.File

/**
 * Wer in `:app` wen kennt — einmal gerechnet, für die Regeln, die es brauchen.
 *
 * Zwei Regeln lesen dasselbe: `BereichsZyklenTest` sucht Kreise, `FeatureKantenTest` sucht
 * Kanten zwischen künftigen Modulen. Doppelt geschrieben wäre der Tag absehbar, an dem die
 * eine repariert wird und die andere nicht.
 *
 * Die wichtigste Regel dabei steht in der Auflösung: ein Import wird auf die **Datei**
 * zurückgeführt, in der das Symbol wirklich steht. `org.biglau.phone.PhoneNumbers` liegt dem
 * Paketnamen nach in `:app` und in Wahrheit in `core:system`. Wer nur Paketnamen vergleicht,
 * zählt jeden Umzug doppelt — beim ersten Messen am 3.9.2026 waren zwei von sechs
 * gemeldeten Kreisen genau das.
 */
object Bereiche {

    /** Bereich → Bereich, nur wo das Ziel wirklich in `:app` steht. Wert: Datei + Symbol. */
    fun kanten(ohne: Set<String> = emptySet()): Map<Pair<String, String>, List<String>> {
        val wurzel = Quelltext.appWurzel
        val symbole = mutableMapOf<String, String>()
        dateien().forEach { datei ->
            val text = datei.readText()
            val paket = paketVon(text) ?: return@forEach
            deklarationen(text).forEach { name ->
                symbole.putIfAbsent("$paket.$name", bereich(datei, wurzel))
            }
        }
        val kanten = mutableMapOf<Pair<String, String>, MutableList<String>>()
        dateien().filterNot { it.name in ohne }.forEach { datei ->
            val von = bereich(datei, wurzel)
            datei.readLines().mapNotNull { zeile ->
                Regex("""^import (org\.biglau\.[\w.]+)""").find(zeile.trim())?.groupValues?.get(1)
            }.forEach { voll ->
                val nach = symbole[voll] ?: symbole[voll.substringBeforeLast('.')] ?: return@forEach
                if (nach != von) {
                    kanten.getOrPut(von to nach) { mutableListOf() }
                        .add("${datei.name}: ${voll.substringAfterLast('.')}")
                }
            }
        }
        return kanten
    }

    fun dateien(): List<File> =
        Quelltext.appWurzel.walkTopDown().filter { it.extension == "kt" }.toList()

    fun bereich(datei: File, wurzel: File = Quelltext.appWurzel): String {
        val teile = datei.relativeTo(File(wurzel, "org/biglau")).path.split(File.separator)
        return if (teile.size > 1) teile.first() else "."
    }

    private fun paketVon(text: String): String? =
        Regex("""^package ([\w.]+)""", RegexOption.MULTILINE).find(text)?.groupValues?.get(1)

    private fun deklarationen(text: String): List<String> = Regex(
        """^(?:internal |private )?(?:object|class|interface|enum class|data class|""" +
            """sealed class|sealed interface|value class|fun|val|const val|typealias) (\w+)""",
        RegexOption.MULTILINE,
    ).findAll(text).map { it.groupValues[1] }.toList()
}
