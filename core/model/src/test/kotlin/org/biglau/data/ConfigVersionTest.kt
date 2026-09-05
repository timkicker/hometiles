package org.biglau.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * the field `version` stood in every saved file and was read by nobody.
 *
 * its purpose is exactly one case: playing a backup from the new phone back onto an old one.
 * then the file holds fields the old version does not know, and `ignoreUnknownKeys` throws
 * them away without a word - one loses settings and nothing says so.
 */
class ConfigVersionTest {

    private fun file(version: Int): String =
        """{"version":$version,"screens":[{"id":"a","name":"A","cols":2,"rows":3,"cells":[]}]}"""

    @Test
    fun `a file from this version is not newer`() {
        assertEquals(false, ConfigTransfer.isFromNewerVersion(file(CONFIG_VERSION)))
    }

    @Test
    fun `a file from a newer version is recognised`() {
        assertEquals(true, ConfigTransfer.isFromNewerVersion(file(CONFIG_VERSION + 1)))
    }

    @Test
    fun `an older file is no reason to warn`() {
        assertEquals(false, ConfigTransfer.isFromNewerVersion(file(CONFIG_VERSION - 1)))
    }

    // no warning without a version and none on nonsense: a warning that appears on every
    // crooked file says nothing any more.
    @Test
    fun `without a version there is no warning`() {
        assertEquals(
            false,
            ConfigTransfer.isFromNewerVersion("""{"screens":[{"id":"a","name":"A","cols":2,"rows":3,"cells":[]}]}"""),
        )
        assertEquals(false, ConfigTransfer.isFromNewerVersion("no json"))
    }

    // and the file must stay readable all the same - warning is not refusing.
    @Test
    fun `a newer file is read all the same`() {
        val loaded = ConfigTransfer.import(file(CONFIG_VERSION + 1))
        assertEquals(1, loaded?.screens?.size)
    }
}
