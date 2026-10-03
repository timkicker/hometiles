package dev.kicker.hometiles.notify

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a notice carries the symbol of the thing it is about.
 *
 * until 03.10.2026 there was exactly one status bar icon in the project, an envelope, and
 * all five notices used it - the one about an incoming call too. seen on android 8 and on
 * android 15: the phone rings and an envelope appears. for someone who reads symbols faster
 * than words, and this app is built for exactly those people, that is the wrong word.
 *
 * the rule is deliberately narrow. it does not demand an icon per notice, only that the one
 * about a call does not use the one about a message.
 */
class NoticeIconTest {

    private val calls = "dev/kicker/hometiles/phone/CallNotifications.kt"

    @Test
    fun `the call notice does not use the message icon`() {
        val icons = Regex("""setSmallIcon\(R\.drawable\.(\w+)\)""")
            .findAll(Quelltext.withoutComments(calls))
            .map { it.groupValues[1] }
            .toList()
        assertTrue("CallNotifications sets no small icon at all.", icons.isNotEmpty())
        assertEquals(
            "the notice about a call carries the message icon. whoever sees it in the status " +
                "bar reads a message, not a call.",
            emptyList<String>(),
            icons.filter { "message" in it },
        )
    }

    @Test
    fun `every icon a notice names exists as a drawable`() {
        val named = Quelltext.files()
            .flatMap { file ->
                Regex("""setSmallIcon\(R\.drawable\.(\w+)\)""")
                    .findAll(file.readText())
                    .map { it.groupValues[1] }
            }
            .toSet()
        val missing = named.filterNot { File("src/main/res/drawable/$it.xml").isFile }
        assertEquals(
            "a notice names a drawable that does not exist; it would crash when posted.",
            emptyList<String>(),
            missing,
        )
    }
}
