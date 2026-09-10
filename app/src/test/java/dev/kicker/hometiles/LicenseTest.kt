package dev.kicker.hometiles

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the licence the readme names is the licence the project actually grants.
 *
 * until 07.09.2026 the readme said `GPL-3.0-or-later` and nothing in the repository said
 * "or any later version". The `LICENSE` file is the plain GPLv3 text; the only place that
 * wording appeared was inside the GPL's own appendix, which tells an author what to write
 * and grants nothing.
 *
 * so what was shipped was GPLv3, and what was claimed was more. an F-Droid reviewer found
 * it on the submission, not us. "or later" is not a label, it is a grant, and a grant has to
 * be written down somewhere a reader can find it.
 */
class LicenseTest {

    private val readme = File("../README.md").readText()
    private val license = File("../LICENSE").readText()

    /** the wording the FSF prescribes for the grant, cut short enough to survive rewrapping. */
    private val grant = "or (at your option) any later"

    @Test
    fun `the readme names one spdx identifier`() {
        val named = Regex("""GPL-3\.0-(?:only|or-later)""").findAll(readme)
            .map { it.value }.toSet()
        assertEquals(
            "the README names no GPL identifier, or two different ones. whoever reads it has " +
                "to know under which licence they may pass the app on.",
            1,
            named.size,
        )
    }

    @Test
    fun `or-later is granted and not only claimed`() {
        val claimsLater = "GPL-3.0-or-later" in readme
        val grantsLater = grant in readme
        assertEquals(
            "the README says GPL-3.0-or-later but nowhere grants it. The `LICENSE` file is " +
                "the bare GPLv3 text - the sentence in its appendix is an instruction to the " +
                "author, not a grant. Either write the notice out, or name GPL-3.0-only.",
            claimsLater,
            grantsLater,
        )
    }

    @Test
    fun `the license file is the gpl version three`() {
        assertTrue(
            "the LICENSE is no longer the GPLv3 - then the identifier in the README is wrong too.",
            "GNU GENERAL PUBLIC LICENSE" in license.take(200) && "Version 3" in license.take(200),
        )
    }

    /**
     * counter-check: without it the second rule would pass on a readme that names no licence
     * at all, because false equals false.
     */
    @Test
    fun `the rule would notice a claim without a grant`() {
        val invented = "License: GPL-3.0-or-later, and nothing else."
        assertTrue("GPL-3.0-or-later" in invented)
        assertTrue(grant !in invented)
    }
}
