package org.biglau.actions

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a role dialog needs a caller.
 *
 * android reads the caller through `startActivityForResult`; opened with `startActivity`
 * (and all the more with `FLAG_ACTIVITY_NEW_TASK`) the dialog breaks off before it is
 * visible - nothing happens on screen, and the user takes it for a misplaced tap. that hit
 * both roles, phone **and** home screen.
 */
class RoleRequestTest {

    private val intents = Quelltext.withoutComments("org/biglau/actions/Intents.kt")

    private fun rolePart(): String =
        Quelltext.cut(intents, "fun homeRoleIntent", "private inline fun start")

    /**
     * `createRequestRoleIntent` may only be **returned as an intent** in this file, so the
     * caller starts it through a launcher.
     */
    @Test
    fun `the role intent is not started here`() {
        val lines = intents.lines()
        val started = lines.indices.filter { index ->
            "createRequestRoleIntent" in lines[index] &&
                (index until minOf(index + 6, lines.size)).any { "startActivity" in lines[it] }
        }
        assertEquals(emptyList<Int>(), started)
        assertTrue("NEW_TASK on a role intent", "FLAG_ACTIVITY_NEW_TASK" !in rolePart())
    }

    /**
     * "use as phone app" stood there even when BigLau already was one - a tap did visibly
     * nothing because the role dialog closed again at once.
     */
    @Test
    fun `the role rows name the state`() {
        val settings = Quelltext.file("org/biglau/settings/SettingsActivity.kt").readText()
        listOf("R.string.is_home", "R.string.is_dialer", "isHomeScreen", "isDialerApp")
            .forEach { assertTrue("$it is missing", it in settings) }
        // per language, not per file - the texts lie in several modules.
        listOf("values", "values-de").forEach { language ->
            val texts = Quelltext.texts(language).joinToString("\n") { it.readText() }
            listOf("is_home", "is_dialer", "role_change_hint").forEach { name ->
                assertTrue("$language: $name is missing", "\"$name\"" in texts)
            }
        }
    }

    @Test
    fun `both roles are asked for through a launcher`() {
        val settings = Quelltext.file("org/biglau/settings/SettingsActivity.kt").readText()
        assertTrue("phone role without a launcher", "askDialerRole.launch(" in settings)
        assertTrue("home screen role without a launcher", "Intents.homeRoleIntent(" in settings)
        val wizard = Quelltext.file("org/biglau/wizard/WizardActivity.kt").readText()
        assertTrue("wizard without a launcher", "askHomeRole.launch(" in wizard)
    }
}
