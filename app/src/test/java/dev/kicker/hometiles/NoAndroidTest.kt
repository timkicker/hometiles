package dev.kicker.hometiles

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * what needs no android does not belong in `:app`.
 *
 * such files hold up the module cut from `PLAN.md` 2.1, they make `:app`'s test runs longer
 * than necessary, and they can bind themselves to android unnoticed because nothing in
 * `:app` stops them. in `core:model` the compiler forbids it.
 *
 * so a new file without android belongs in a core module. if there is a reason it has to
 * stay here, it stands below - by name.
 */
class NoAndroidTest {

    /** file -> why it stays in `:app` anyway. */
    private val mayStay = mapOf<String, String>()

    @Test
    fun `no file without android stays in app`() {
        // only what is **android**. `java.*` and `kotlin.*` exist in `core:model` just the
        // same - the first version of this rule counted them and thereby missed
        // `MessageStamps`, 31 lines of date arithmetic that needs only `java.util`.
        val foreign = listOf("android", "androidx", "coil")
        val clean = Quelltext.appRoot.walkTopDown()
            .filter { it.extension == "kt" }
            .filter { file ->
                file.readLines()
                    .filter { it.startsWith("import ") }
                    .none { line -> foreign.any { line.removePrefix("import ").startsWith(it) } }
            }
            .map { it.name }
            .toSortedSet()

        assertEquals(
            "this file needs no android and belongs in a core module - keep the package, " +
                "change the module, and not a single import changes. if it has to stay, " +
                "into the list in NoAndroidTest with a reason.",
            mayStay.keys.toSortedSet(),
            clean,
        )
    }
}
