package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * no sentence consoles with a way the role has just closed.
 *
 * two texts said the phone's messaging app could still open it - the hint when a picture
 * message arrives, and the explainer on the message settings page. both were written as a
 * warning **before** the role was taken and were right exactly until then.
 *
 * only the default sms app fetches an mms: the wap push goes to it alone, the download too.
 * once HomeTiles holds the role and does not fetch it, no other app can either. a consolation
 * that turns false after the decision is worse than none - the user then looks for an app
 * that cannot exist any more.
 *
 * both places now have two wordings, and the rule holds that the choice hangs on the role and
 * not on an assumption.
 */
class RoleInTextTest {

    private val page = Quelltext.withoutComments("dev/kicker/hometiles/sms/MessagesSettingsList.kt")

    @Test
    fun `the mms explainer goes by the role`() {
        assertTrue(
            "there is only one wording of the mms explainer left - then it is right before " +
                "or after the role is taken, but not both.",
            "R.string.sms_no_mms_default" in page && "R.string.sms_no_mms" in page,
        )
        val from = page.indexOf("R.string.sms_no_mms_default")
        assertTrue(
            "the choice does not hang on the role.",
            "holdsSmsRole" in page.substring(maxOf(0, from - 120), from),
        )
    }

    /** and the filter hint likewise - that one had it right before. */
    @Test
    fun `the filter hint goes by the role`() {
        assertTrue(
            "the filter hint has only one wording left.",
            "R.string.sms_filter_hint_default" in page && "R.string.sms_filter_hint" in page,
        )
    }

    /**
     * nailing the sentence down made the rule hang on the wording: a better phrasing of the
     * same wrong consolation would have passed, and a harmless rephrasing of the **right** one
     * would have turned it red.
     *
     * the fault arose from the same sentence standing in both wordings, so what is checked is
     * that the ending differs - what comes after the em dash, else the last sentence.
     */
    @Test
    fun `the wording for the held role says something else`() {
        listOf("values", "values-de").forEach { language ->
            val before = ending(language, "sms_no_mms")
            assertTrue(
                "$language/sms_no_mms has no ending left - then only the problem stands there.",
                before.isNotBlank(),
            )
            listOf("sms_no_mms_default", "mms_arrived_body").forEach { name ->
                assertEquals(
                    "$language/$name ends word for word like sms_no_mms. that sentence is " +
                        "only true while **another** app holds the role; here HomeTiles holds " +
                        "it, and the user then looks for an app that cannot exist any more.",
                    false,
                    ending(language, name) == before,
                )
            }
        }
    }

    /**
     * the ending of a text: what stands after the em dash, else the last sentence. that is
     * where the conclusion sits - the part that differs before and after the role is taken.
     */
    private fun ending(language: String, name: String): String {
        val value = Quelltext.textValue(name, language)
        val afterDash = value.substringAfterLast("\u2014", "")
        if (afterDash.isNotBlank()) return afterDash.trim()
        val sentences = value.split(". ").filter { it.isNotBlank() }
        return if (sentences.size < 2) "" else sentences.last().trim()
    }
}
