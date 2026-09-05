package org.biglau.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * the address of a web tile.
 *
 * on three inches nobody types "https://" willingly. the input is therefore completed instead
 * of refused - a tile that says the address is invalid because the prefix is missing is worse
 * than one that adds it.
 */
class LinkTargetTest {

    @Test
    fun `without a prefix https is added`() {
        assertEquals("https://orf.at", LinkTarget.normalise("orf.at"))
        assertEquals("https://www.wien.gv.at", LinkTarget.normalise("www.wien.gv.at"))
    }

    @Test
    fun `an existing prefix stays`() {
        assertEquals("http://old-site.at", LinkTarget.normalise("http://old-site.at"))
        assertEquals("https://orf.at", LinkTarget.normalise("https://orf.at"))
    }

    @Test
    fun `other schemes stay untouched`() {
        // tel: and mailto: have their purpose; a prefixed https would be nonsense.
        assertEquals("mailto:hello@example.at", LinkTarget.normalise("mailto:hello@example.at"))
        assertEquals("geo:12.3,45.6", LinkTarget.normalise("geo:12.3,45.6"))
    }

    @Test
    fun `space at the edges does not disturb`() {
        assertEquals("https://orf.at", LinkTarget.normalise("  orf.at  "))
    }

    @Test
    fun `a space in the middle means no address`() {
        assertNull(LinkTarget.normalise("this is no address"))
    }

    @Test
    fun `empty stays empty`() {
        assertNull(LinkTarget.normalise(""))
        assertNull(LinkTarget.normalise("   "))
    }

    @Test
    fun `something without a dot is no host name`() {
        assertNull(LinkTarget.normalise("orf"))
    }

    @Test
    fun `the host name stands on the tile`() {
        assertEquals("orf.at", LinkTarget.labelFor("https://orf.at/news/wetter"))
        assertEquals("wien.gv.at", LinkTarget.labelFor("https://www.wien.gv.at/"))
    }

    @Test
    fun `path, query and anchor do not belong to the name`() {
        assertEquals("example.at", LinkTarget.hostOf("https://example.at/path?a=1#here"))
    }

    @Test
    fun `a port does not belong either`() {
        assertEquals("example.at", LinkTarget.hostOf("https://example.at:8443/x"))
    }

    @Test
    fun `without a host name the address itself stands there`() {
        assertEquals("mailto:hello@example.at", LinkTarget.labelFor("mailto:hello@example.at"))
    }
}
