package org.biglau

import java.io.File

/**
 * where BigLau's source lies, for the rules that read it.
 *
 * nearly half the tests here check source rather than behaviour, and they all ran over one
 * hard-written path. `walkTopDown()` on a directory that does not exist returns no file and
 * no error, so a rule would stay green and check nothing. hence one list of roots, and a
 * test that looks whether it is complete.
 */
object Quelltext {

    /** the main source of `:app` alone, so a rule needing only it need not write the path. */
    val appRoot: File = File("src/main/java")

    /**
     * every root with main source, relative to the tests' working directory (`app/`).
     * new modules belong here; `QuelltextTest` falls over when one is missing.
     */
    val roots: List<File> = listOf(
        appRoot,
        File("../core/model/src/main/kotlin"),
        File("../core/data/src/main/kotlin"),
        File("../core/system/src/main/kotlin"),
        File("../core/ui/src/main/kotlin"),
    )

    /**
     * the resource roots of every module: when `:core:ui` moved, the two font files went
     * with it and two rules kept looking in the old place.
     */
    val resRoots: List<File> = listOf(
        File("src/main/res"),
        File("../core/ui/src/main/res"),
        // the six words for a call's direction live beside `CallDirection`, so they are not
        // kept twice.
        File("../core/system/src/main/res"),
    )

    /**
     * every text file of one language directory, across all modules.
     *
     * ten rules read one module's `strings.xml` and meant all texts by it; a text moving
     * with its module would silently leave their reach. a module without the file drops out
     * rather than failing: not every module has texts.
     */
    fun texts(directory: String, name: String = "strings.xml"): List<File> =
        resRoots.map { File(it, "$directory/$name") }.filter { it.isFile }
            // empty does not mean nothing to check but something is wrong here: a rule
            // running over zero files is green and has looked at nothing.
            .also {
                if (it.isEmpty()) {
                    throw AssertionError(
                        "no module has $directory/$name. every rule looking here would be " +
                            "green from now on without checking anything.",
                    )
                }
            }

    /**
     * a text's value across all modules, with a loud no when it does not exist. five rules
     * took the first module only, and texts have moved between modules three times.
     */
    fun textValue(name: String, language: String): String =
        texts(language).firstNotNullOfOrNull { file ->
            Regex("""<string name="$name">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
                .find(file.readText())?.groupValues?.get(1)
        } ?: throw AssertionError("the text $name exists in $language in no module.")

    /**
     * the language directories as they stand in the tree: `values` and every `values-xx`.
     *
     * searched instead of listed, and that is the point: with two names written down, a
     * third language would have been created and looked at by not a single rule. green, and
     * nothing checked.
     */
    fun languages(): List<String> =
        resRoots.flatMap { root ->
            (root.listFiles() ?: emptyArray()).filter { folder ->
                folder.isDirectory &&
                    (folder.name == "values" || folder.name.startsWith("values-")) &&
                    (File(folder, "strings.xml").isFile || File(folder, "plurals.xml").isFile)
            }.map { it.name }
        }.distinct().sorted()

    /** the languages without the base one: what wants translating. */
    fun translations(): List<String> = languages().filter { it != "values" }

    /**
     * the languages actually shipped, read from `resourceConfigurations`, which is the only
     * place that knows whether a language is finished or still in progress.
     *
     * the difference counts: android falls back to `values` for a missing text, so a
     * half-translated language *works* and is partly english. that is the normal state while
     * working, and not a shippable one.
     */
    fun shipped(): List<String> =
        Regex("""resourceConfigurations\s*\+=\s*listOf\(([^)]*)\)""")
            .find(File("build.gradle.kts").readText())
            ?.groupValues?.get(1)
            ?.let { Regex(""""([^"]+)"""").findAll(it).map { m -> m.groupValues[1] }.toList() }
            ?.map { if (it == "en") "values" else "values-$it" }
            ?: throw AssertionError(
                "resourceConfigurations is gone from app/build.gradle.kts. without it no rule " +
                    "knows which language ships.",
            )

    /** every text file across all modules and languages. */
    fun allTexts(): List<File> =
        languages().flatMap { v ->
            listOf("strings.xml", "plurals.xml").flatMap { name ->
                resRoots.map { File(it, "$v/$name") }.filter { it.isFile }
            }
        }

    /** a resource by its path below `res/`, such as `font/atkinson_bold.ttf`. */
    fun resource(path: String): File =
        resRoots.map { File(it, path) }.firstOrNull { it.exists() }
            ?: throw AssertionError("resource not found: $path")

    /** the roots with test source. */
    val testRoots: List<File> = listOf(
        File("src/test/java"),
        File("../core/system/src/test/kotlin"),
        File("../core/model/src/test/kotlin"),
        File("../core/ui/src/test/kotlin"),
    )

    /** every kotlin file of the main source, across all modules. */
    fun files(): List<File> = kt(roots)

    /** every kotlin file of the test source. */
    fun testFiles(): List<File> = kt(testRoots)

    /**
     * the same list with a promise that something is in it: a selection that matches nothing
     * makes the loop not run and the rule green. asking here states a minimum.
     */
    fun atLeast(hits: List<*>, howMany: Int, what: String): List<*> {
        if (hits.size < howMany) {
            throw AssertionError(
                "$what: $howMany expected, ${hits.size} found. the rule would run over a list " +
                    "too short and stay green.",
            )
        }
        return hits
    }

    /**
     * a single file by its package path, such as `org/biglau/data/Model.kt`. writing a
     * module path instead ties the rule to that module.
     */
    fun file(path: String): File =
        // a path that already exists is taken as it is: the resources still live in :app,
        // and one rule often reads both source and strings.xml.
        File(path).takeIf { it.isFile }
            ?: (roots + testRoots).map { File(it, path) }.firstOrNull { it.isFile }
            ?: throw AssertionError("source not found: $path")

    /**
     * a file without its comment lines, for rules searching for *calls* with `indexOf`.
     *
     * a comment that merely mentions the same name shifts the found place: with one above a
     * branch, the rule that keeps the sos alarm silent during a preview would have waved a
     * real fault through.
     *
     * only whole comment lines go; a trailing `//` after source stays, since the place
     * really is there.
     */
    fun withoutComments(path: String): String = file(path)
        .readLines()
        .filterNot { isCommentLine(it) }
        .joinToString("\n")

    /**
     * a line that is only comment. the third form is the one always forgotten, the one-line
     * doc comment: fourteen rules knew only the first two and took one for source.
     */
    fun isCommentLine(line: String): Boolean {
        val bare = line.trim()
        return bare.startsWith("//") || bare.startsWith("*") || bare.startsWith("/*")
    }

    /**
     * the cut between two marks, and a loud no when one is missing.
     *
     * `substringAfter` and `substringBefore` return the *whole* text for a missing mark, so
     * a rule cutting that way then checks something else and stays green. it happened twice
     * in one night.
     *
     * an empty [from] means from the start, [to] `null` means to the end, and [atMost]
     * bounds it further: a window that does not hang on a name.
     */
    fun cut(
        text: String,
        from: String,
        to: String? = null,
        atMost: Int = Int.MAX_VALUE,
        /**
         * the start mark may occur more than once and the first is meant. set only when that
         * is really intended, or the order in the source decides what gets checked.
         */
        repeated: Boolean = false,
    ): String {
        val start = text.indexOf(from)
        if (from.isNotEmpty() && !repeated) {
            val howOften = Regex(Regex.escape(from)).findAll(text).count()
            if (howOften > 1) {
                throw AssertionError(
                    "the mark \"$from\" stands in the text ${howOften} times. which place the " +
                        "rule looks at is then decided by the order. cut more precisely, " +
                        "or set repeated = true.",
                )
            }
        }
        if (start < 0) {
            throw AssertionError(
                "the mark \"$from\" is gone from the text. the rule would cut into nothing " +
                    "and stay green without checking anything.",
            )
        }
        val rest = text.substring(start + from.length)
        val end = if (to == null) {
            rest.length
        } else {
            rest.indexOf(to).also {
                if (it < 0) {
                    throw AssertionError(
                        "the end mark \"$to\" no longer stands behind \"$from\". the cut " +
                            "would run to the end of the file.",
                    )
                }
            }
        }
        return rest.take(minOf(end, atMost))
    }

    private fun kt(places: List<File>): List<File> =
        places.flatMap { it.walkTopDown().filter { file -> file.extension == "kt" } }
}
