package org.biglau.design

import org.biglau.Quelltext
import org.biglau.ui.Notice
import org.biglau.ui.SettingsLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the jump into the settings runs through an intent, not through the class.
 *
 * four screens from four corners jump to a subpage: keypad to call types, contacts to
 * sorting, app list to hidden apps, emergency call to emergency contacts. each named
 * `SettingsActivity` by name, while `PLAN.md` 2.1 says a `feature:*` never imports another -
 * with four such references the split would not be possible at all.
 *
 * the price of an intent is that the compiler no longer checks it: manifest and source can
 * drift apart, and the jump then leads **silently** nowhere. that is what this rule stands
 * against.
 */
class SettingsLinkTest {

    private val manifest = Quelltext.file("src/main/AndroidManifest.xml").readText()

    @Test
    fun `the settings answer the intent`() {
        val block = Quelltext.cut(manifest, ".settings.SettingsActivity", "</activity>")
        assertTrue(
            "SettingsActivity has no filter for ${SettingsLink.ACTION}: $block",
            SettingsLink.ACTION in block,
        )
        assertTrue("without the DEFAULT category no implicit intent starts", "category.DEFAULT" in block)
    }

    /**
     * only the shell may know the settings by name. `MainActivity` is that shell: it holds the
     * home role and the emergency mode and knows every screen anyway.
     */
    @Test
    fun `apart from the shell nobody names SettingsActivity`() {
        val namers = Quelltext.files()
            .filter { "org.biglau.settings.SettingsActivity" in it.readText() }
            .map { it.name }
            .filterNot { it == "MainActivity.kt" || it == "SettingsActivity.kt" }
        assertEquals("jumps past SettingsLink: $namers", emptyList<String>(), namers)
    }

    /** and the way there is used - otherwise the rule would check a dead class. */
    @Test
    fun `the four jumpers use the way`() {
        val jumpers = listOf(
            "org/biglau/toggles/SosActivity.kt",
            "org/biglau/phone/DialerActivity.kt",
            "org/biglau/contacts/ContactsActivity.kt",
            "org/biglau/apps/AppDrawerActivity.kt",
        )
        val without = jumpers.filterNot { "SettingsLink.toPage(" in Quelltext.file(it).readText() }
        assertEquals("does not jump through SettingsLink: $without", emptyList<String>(), without)
    }

    /** the intent stays inside our own program - or a foreign one could answer it. */
    @Test
    fun `the intent stays inside our own program`() {
        val source = Quelltext.file("org/biglau/ui/SettingsLink.kt").readText()
        assertTrue("without setPackage the intent would be open", "setPackage(" in source)
    }

    /**
     * the same for the waiting notice: `Notice` belongs to the design system and is used
     * everywhere, while its screen is an activity of the application. naming the class would
     * pull half the app into the design system.
     */
    @Test
    fun `the waiting notice is opened through an intent`() {
        val block = Quelltext.cut(manifest, ".ui.NoticeActivity", "</activity>")
        assertTrue("NoticeActivity has no filter for ${Notice.ACTION}: $block", Notice.ACTION in block)
        assertTrue("without the DEFAULT category no implicit intent starts", "category.DEFAULT" in block)
        val source = Quelltext.file("org/biglau/ui/Notice.kt").readLines()
        // code only: the comment may name the activity - that is where it says who answers.
        // a test that reads comments along teaches silence.
        val code = source.filterNot { Quelltext.isCommentLine(it) }
        assertTrue(
            "Notice names the activity in code: ${code.filter { "NoticeActivity" in it }}",
            code.none { "NoticeActivity" in it },
        )
        assertTrue("without setPackage the intent would be open", code.any { "setPackage(" in it })
    }
}
