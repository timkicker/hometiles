package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a message naming a way also shows it.
 *
 * two tiles can lead nowhere: an uninstalled app and a vanished shortcut. both said the same
 * sentence, and only the app tile opened the editor afterwards. half a way out is none, and
 * worse than none: the second time nobody believes the sentence.
 *
 * the third case shows why a text has to fit its place: the same message stood in the **app
 * list**, where there is no tile at all.
 */
class WayOutTest {

    private val home = Quelltext.file("dev/kicker/hometiles/MainActivity.kt").readText()

    /**
     * **every** occurrence, not the first: `substringAfter` checked one and missed that
     * `app_gone` stands in three places - on tapping the tile, in the app list, and after
     * unlocking a locked app. the third was the one without the way out.
     */
    @Test
    fun `both dead tiles lead into the editor`() {
        listOf("R.string.app_gone", "R.string.shortcut_gone").forEach { message ->
            var from = 0
            var found = 0
            while (true) {
                val place = home.indexOf(message, from)
                if (place < 0) break
                found++
                assertTrue(
                    "$message (occurrence $found in MainActivity) is not followed by a " +
                        "TileEditorActivity.intent - the message advises reassigning the tile " +
                        "and leaves the advice hanging.",
                    "TileEditorActivity.intent(" in home.substring(place).take(600),
                )
                from = place + message.length
            }
            assertTrue("$message no longer appears in MainActivity at all", found > 0)
        }
    }

    @Test
    fun `the app list does not advise a tile that is not there`() {
        val list = Quelltext.file("dev/kicker/hometiles/apps/AppDrawerActivity.kt").readText()
        assertEquals(
            "the app list shows `app_gone`, which advises reassigning a tile - there is no " +
                "tile there. use `app_gone_list`.",
            false,
            "R.string.app_gone)" in list,
        )
        assertTrue("app_gone_list is not used", "R.string.app_gone_list" in list)
        assertTrue(
            "the list is not reloaded after the message - the dead entry would stay and do " +
                "nothing again on the next tap.",
            "loadApps()" in Quelltext.cut(list, "R.string.app_gone_list").take(200),
        )
    }

    /**
     * nailing the sentence down made the rule hang on the wording: rephrasing the message
     * turned it red although the way out was there, and rephrasing **one** of the two turned
     * it green although they had drifted apart. so equality is checked, not the wording.
     */
    @Test
    fun `the advice stands in both messages and is the same`() {
        listOf("values", "values-de").forEach { language ->
            val advice = listOf("app_gone", "shortcut_gone").map { name ->
                name to lastSentence(language, name)
            }
            advice.forEach { (name, text) ->
                assertTrue(
                    "$language/$name names only the problem and no way out - then one stands " +
                        "in front of it not knowing what now.",
                    text.isNotBlank(),
                )
            }
            assertEquals(
                "the two dead tiles advise different things. the same case, the same way " +
                    "out - or the second time neither is believed.",
                advice[0].second,
                advice[1].second,
            )
            assertTrue(
                "the app list gives the same advice as the tile, but there is no tile there " +
                    "to reassign.",
                lastSentence(language, "app_gone_list") != advice[0].second,
            )
        }
    }

    /**
     * the last sentence of a text - that is where the way out stands if one does. across all
     * text files, so a text moving with its module is still found.
     */
    private fun lastSentence(language: String, name: String): String {
        val value = Quelltext.textValue(name, language)
        val sentences = value.split(". ").filter { it.isNotBlank() }
        return if (sentences.size < 2) "" else sentences.last().trim()
    }
}
