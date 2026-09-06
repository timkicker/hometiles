package dev.kicker.hometiles.res

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * a quotation mark that never arrives.
 *
 * android swallows a **straight** quotation mark (`"`) in a string unless it is written
 * `\"`. the screen therefore showed an opening quote with no closing one. ten german texts
 * were affected, the oldest for weeks.
 *
 * it showed only on looking at the call log in german on the screen. in the source such a
 * string looks perfectly right - exactly the kind of fault one finds only on the device.
 *
 * the rule: texts carry **typographic** quotation marks. they are not swallowed and look
 * better.
 */
class QuotesTest {

    private val files = Quelltext.allTexts()

    @Test
    fun `no straight quotation mark in a text`() {
        val hits = files.flatMap { file ->
            Regex("""<(string |string>|item)[^>]*>((?:(?!</).)*)<""", RegexOption.DOT_MATCHES_ALL)
                .findAll(file.readText())
                .filter { '"' in it.groupValues[2] }
                .map { "${file.name}: ${it.groupValues[2].take(40)}" }
        }
        assertEquals(emptyList<String>(), hits)
    }
}
