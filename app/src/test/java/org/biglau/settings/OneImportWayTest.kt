package org.biglau.settings

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a backup is read in at **one** place.
 *
 * two ways lead to the same thing: tapping the file from outside, and "load from a file" in
 * the settings. the first went through `ImportActivity`, which says what is in the file and
 * only then asks. the second read the file itself and replaced the whole setup as soon as one
 * was chosen - the more common way was the less careful one.
 */
class OneImportWayTest {

    private val settings = Quelltext.withoutComments("org/biglau/settings/SettingsActivity.kt")

    @Test
    fun `only one place replaces the setup from a file`() {
        val places = Quelltext.files()
            // both spellings: `ImportActivity` passes the function on as a reference
            // (`ConfigTransfer::import`), and the first version of this rule looked only for
            // the call with brackets - so it found nothing at all.
            .filter { file ->
                val text = file.readText()
                "ConfigTransfer.import(" in text || "ConfigTransfer::import" in text
            }
            .map { it.name }
            .sorted()
        assertEquals(
            "more than one place reads a backup in. then there are two answers to whether it " +
                "asks first - and they drift apart.",
            listOf("ImportActivity.kt"),
            places,
        )
    }

    @Test
    fun `the settings pass the file on`() {
        assertTrue(
            "the settings open the file dialog but do not pass the file on to " +
                "ImportActivity - then the question asked there is missing.",
            "ImportActivity::class.java" in settings,
        )
    }
}
