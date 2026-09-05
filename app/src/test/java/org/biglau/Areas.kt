package org.biglau

import java.io.File

/**
 * who knows whom inside `:app` - computed once, for the rules that need it.
 *
 * two rules read the same thing: `BereichsZyklenTest` looks for cycles, `FeatureEdgesTest`
 * for edges between future modules. written twice, the day one gets fixed and the other does
 * not is only a matter of time.
 *
 * the important part is in the resolution: an import is traced back to the **file** the
 * symbol really stands in. `org.biglau.phone.PhoneNumbers` lies in `:app` by its package name
 * and in truth in `core:system`; comparing package names alone counts every move twice - two
 * of six reported cycles were exactly that at the first measurement.
 */
object Areas {

    /** area to area, only where the target really stands in `:app`. value: file + symbol. */
    fun edges(without: Set<String> = emptySet()): Map<Pair<String, String>, List<String>> {
        val root = Quelltext.appRoot
        val symbols = mutableMapOf<String, String>()
        files().forEach { file ->
            val text = file.readText()
            val pkg = packageOf(text) ?: return@forEach
            declarations(text).forEach { name ->
                symbols.putIfAbsent("$pkg.$name", areaOf(file, root))
            }
        }
        val edges = mutableMapOf<Pair<String, String>, MutableList<String>>()
        files().filterNot { it.name in without }.forEach { file ->
            val from = areaOf(file, root)
            file.readLines().mapNotNull { line ->
                Regex("""^import (org\.biglau\.[\w.]+)""").find(line.trim())?.groupValues?.get(1)
            }.forEach { full ->
                val to = symbols[full] ?: symbols[full.substringBeforeLast('.')] ?: return@forEach
                if (to != from) {
                    edges.getOrPut(from to to) { mutableListOf() }
                        .add("${file.name}: ${full.substringAfterLast('.')}")
                }
            }
        }
        return edges
    }

    fun files(): List<File> =
        Quelltext.appRoot.walkTopDown().filter { it.extension == "kt" }.toList()

    fun areaOf(file: File, root: File = Quelltext.appRoot): String {
        val parts = file.relativeTo(File(root, "org/biglau")).path.split(File.separator)
        return if (parts.size > 1) parts.first() else "."
    }

    private fun packageOf(text: String): String? =
        Regex("""^package ([\w.]+)""", RegexOption.MULTILINE).find(text)?.groupValues?.get(1)

    private fun declarations(text: String): List<String> = Regex(
        """^(?:internal |private )?(?:object|class|interface|enum class|data class|""" +
            """sealed class|sealed interface|value class|fun|val|const val|typealias) (\w+)""",
        RegexOption.MULTILINE,
    ).findAll(text).map { it.groupValues[1] }.toList()
}
