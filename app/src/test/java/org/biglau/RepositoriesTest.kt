package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * every repository lives in `core:system`.
 *
 * nine files are called `…Repository`: they fetch something from the device - apps, contacts,
 * battery, signal, call log, shortcuts, messages, the running call. one lay in `:app` without
 * a reason: `InCallRepository` held our own service class in its type although it uses only
 * `setMuted` and `setAudioRoute`, both from the system. a type chosen too narrowly, no more.
 *
 * why it counts: a repository lying in `:app` can touch an activity, and then it is not a
 * repository any more. in `core:system` there are none.
 *
 * the first version looked at **file names** only and missed one that stood in the middle of
 * another file. a rule that reads the envelope finds nothing anyone put inside; now it
 * searches the declaration.
 */
class RepositoriesTest {

    /**
     * repository to why it stays in `:app` after all.
     *
     * **empty since 04.09.2026.** the one entry had the right reason and answered the wrong
     * question: not the repository had to stay, the **service** was allowed to move. an empty
     * exception list is the aim of every exception list.
     */
    private val mayStay = emptyMap<String, String>()

    private fun repositoriesIn(files: Sequence<java.io.File>): Set<String> =
        files.flatMap { file ->
            Regex("""^(?:internal )?(?:object|class) (\w*Repository)\b""", RegexOption.MULTILINE)
                .findAll(file.readText())
                .map { it.groupValues[1] }
        }.toSortedSet()

    @Test
    fun `no repository stays in the app module`() {
        val inApp = repositoriesIn(
            Quelltext.appRoot.walkTopDown().filter { it.extension == "kt" },
        )
        assertEquals(
            "a repository belongs in core:system. if it hangs on a class from :app the type " +
                "is usually chosen too narrowly. if it really has to stay, into the list in " +
                "RepositoriesTest with a reason.",
            mayStay.keys.toSortedSet(),
            inApp,
        )
    }

    @Test
    fun `every exception names its reason`() {
        mayStay.forEach { (name, reason) ->
            assertTrue("$name: the reason is missing or too short", reason.length > 40)
        }
    }

    @Test
    fun `all nine are still there`() {
        val all = repositoriesIn(Quelltext.files().asSequence())
        assertEquals(
            "a repository has vanished or arrived - then update this note, so the rule above " +
                "does not check into nothing.",
            sortedSetOf(
                "AppRepository", "BatteryRepository", "CallLogRepository",
                "ContactRepository", "InCallRepository", "NotificationRepository",
                "ShortcutRepository", "SignalRepository", "SmsRepository",
            ),
            all,
        )
    }
}
