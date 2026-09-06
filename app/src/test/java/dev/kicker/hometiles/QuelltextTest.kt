package dev.kicker.hometiles

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the list of source roots is complete.
 *
 * without this rule the module split of `PLAN.md` 2.1 would be a quiet devaluation: moving
 * files to `:core:model` leaves the same number of green tests, only they no longer check
 * the moved part. so this test looks for the modules itself and compares.
 */
class QuelltextTest {

    /** every directory with a `build.gradle.kts`, without the build directories. */
    private fun modules(): List<File> = File("..").walkTopDown()
        .onEnter { it.name != "build" && it.name != ".git" && it.name != ".gradle" }
        .filter { it.name == "build.gradle.kts" }
        .map { it.parentFile }
        .toList()

    private fun sourcePlaces(subdirectory: String): List<File> = modules()
        .flatMap { module -> listOf("java", "kotlin").map { File(module, "src/$subdirectory/$it") } }
        .filter { it.isDirectory }

    private fun keys(files: List<File>) = files.map { it.canonicalPath }.sorted()

    @Test
    fun `every module with main source stands in the roots`() {
        assertEquals(
            "a module is missing from Quelltext.roots - no rule checked its files any more",
            keys(sourcePlaces("main")),
            keys(Quelltext.roots),
        )
    }

    @Test
    fun `every module with test source stands in the test roots`() {
        assertEquals(
            "a module is missing from Quelltext.testRoots",
            keys(sourcePlaces("test")),
            keys(Quelltext.testRoots),
        )
    }

    /** a root that does not exist does not stand out on its own. so it stands out here. */
    @Test
    fun `no root points at nothing`() {
        (Quelltext.roots + Quelltext.testRoots).forEach {
            assertTrue("the root does not exist: ${it.path}", it.isDirectory)
        }
    }

    @Test
    fun `the roots really carry source`() {
        assertTrue("no main source found", Quelltext.files().size > 50)
        assertTrue("no test source found", Quelltext.testFiles().size > 50)
    }

    /**
     * the root list existed and `SlopRulesTest` still ran over `File("src/main/java/dev/kicker/hometiles")`
     * of its own. after the move to `:core:system` it simply stopped seeing thirty-five
     * files, without a red test: `walkTopDown` on a path holding less returns less.
     */
    @Test
    fun `no rule builds its own source path`() {
        val hits = Quelltext.testFiles()
            .filterNot { it.name == "Quelltext.kt" || it.name == "QuelltextTest.kt" }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { line ->
                        // the resources stay in :app - only the source directories move, and
                        // only they are meant here.
                        listOf("File(\"src/main/java", "File(\"src/test/java")
                            .any { it in line.value }
                    }
                    .map { "${file.name}:${it.index + 1}  ${it.value.trim()}" }
            }
        assertEquals("reads past the directory: $hits", emptyList<String>(), hits)
    }

    /** the resources lie in several modules too; `:core:ui` brings the font files along. */
    @Test
    fun `every module with resources stands in the resource roots`() {
        val present = modules().map { File(it, "src/main/res") }.filter { it.isDirectory }
        assertEquals(
            "a module is missing from Quelltext.resRoots",
            keys(present),
            keys(Quelltext.resRoots),
        )
    }

    /**
     * ten rules read `src/main/res/values/strings.xml` and meant all texts by it. once a text
     * moves with its module they stop checking it, silently, because a file that exists still
     * reads fine. `themes.xml` stays exempt: the application's theme lies in `:app` and
     * nowhere else.
     */
    @Test
    fun `no rule looks for the texts itself`() {
        val hits = Quelltext.testFiles()
            .filterNot { it.name == "Quelltext.kt" || it.name == "QuelltextTest.kt" }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { "src/main/res/values" in it.value && "themes.xml" !in it.value }
                    .map { "${file.name}:${it.index + 1}  ${it.value.trim()}" }
            }
        assertEquals("reads texts past the directory: $hits", emptyList<String>(), hits)
    }

    /**
     * around thirty rules start with `Quelltext.files().filter { … }` - by `Activity.kt`, by
     * `@Composable`, by `Repository`. a filter whose pattern no longer matches returns
     * nothing, and the rule above it is quietly green.
     *
     * the bounds are deliberately generous: they report a **collapse**, not every tidy-up.
     */
    @Test
    fun `the filters of the other rules still catch something`() {
        val all = Quelltext.files()
        assertTrue("only ${all.size} kotlin files found", all.size >= 100)
        val activities = all.filter { it.name.endsWith("Activity.kt") }
        assertTrue("only ${activities.size} activities found", activities.size >= 10)
        val composables = all.filter { "@Composable" in it.readText() }
        assertTrue("only ${composables.size} files with @Composable", composables.size >= 20)
    }
}
