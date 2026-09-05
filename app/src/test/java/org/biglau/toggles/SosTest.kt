package org.biglau.toggles

import org.biglau.actions.SosMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the german message texts here are test data: the check compares them literally on both
 * sides, and they are what a german user's emergency message looks like.
 */
class SosTest {

    @Test
    fun `without a location only the text stands in the message`() {
        // a message that looks as if it had a location and has none would be worse in earnest
        // than no place at all.
        assertEquals("Hilfe!", SosMessage.compose("Hilfe!", null, null, "Notfall"))
        assertEquals("Hilfe!", SosMessage.compose("Hilfe!", 47.26, null, "Notfall"))
        assertEquals("Hilfe!", SosMessage.compose("Hilfe!", null, 11.39, "Notfall"))
    }

    @Test
    fun `with a location a tappable link is appended`() {
        val message = SosMessage.compose("Hilfe!", 48.20849, 16.37208, "Notfall")
        assertTrue(message.startsWith("Hilfe!\n"))
        assertTrue(message.contains("https://maps.google.com/?q=48.20849,16.37208"))
    }

    @Test
    fun `coordinates are written with a full stop`() {
        // with a german comma the link would be broken - hence a fixed locale.
        assertEquals("https://maps.google.com/?q=48.20849,16.37208", SosMessage.mapsLink(48.20849, 16.37208))
        assertTrue(!SosMessage.mapsLink(48.20849, 16.37208).contains(","+"26"))
    }

    @Test
    fun `negative coordinates stay negative`() {
        assertEquals("https://maps.google.com/?q=-33.86880,151.20930", SosMessage.mapsLink(-33.8688, 151.2093))
    }

    @Test
    fun `an empty text falls back to the default`() {
        assertEquals("Notfall", SosMessage.compose("", null, null, "Notfall"))
        assertEquals("Notfall", SosMessage.compose("   ", null, null, "  Notfall  "))
    }

    @Test
    fun `the number of message parts is computed correctly`() {
        assertEquals(1, SosMessage.partsNeeded(""))
        assertEquals(1, SosMessage.partsNeeded("a".repeat(160)))
        assertEquals(2, SosMessage.partsNeeded("a".repeat(161)))
        assertEquals(2, SosMessage.partsNeeded("a".repeat(306)))
        assertEquals(3, SosMessage.partsNeeded("a".repeat(307)))
    }

    @Test
    fun `the countdown counts down`() {
        assertEquals(5, SosCountdown.remaining(1000L, 1000L, 5))
        assertEquals(3, SosCountdown.remaining(1000L, 3000L, 5))
        assertEquals(0, SosCountdown.remaining(1000L, 6000L, 5))
    }

    @Test
    fun `the countdown does not go negative`() {
        assertEquals(0, SosCountdown.remaining(1000L, 60000L, 5))
    }

    @Test
    fun `a clock jumping back breaks nothing`() {
        // a time zone change or a clock correction during the countdown.
        assertEquals(5, SosCountdown.remaining(5000L, 1000L, 5))
    }

    @Test
    fun `zero seconds means at once`() {
        assertEquals(0, SosCountdown.remaining(1000L, 1000L, 0))
    }

    @Test
    fun `the waiting time is bounded above`() {
        assertEquals(SosCountdown.MAX_SECONDS, SosCountdown.clamp(99))
        assertEquals(0, SosCountdown.clamp(-5))
    }

    @Test
    fun `without numbers nothing is set up`() {
        assertTrue(!SosCountdown.isConfigured(emptyList()))
        assertTrue(!SosCountdown.isConfigured(listOf("", "   ")))
        assertTrue(SosCountdown.isConfigured(listOf("+43660")))
    }
}

/**
 * the coordinates in the emergency message.
 *
 * the link has to open for the receiver whatever language the sending phone runs in. on a
 * german device the default locale formats "47,26543" with a comma - and a map link with a
 * comma is no map link.
 */
class SosLocaleTest {

    private fun withLocale(locale: java.util.Locale, block: () -> Unit) {
        val before = java.util.Locale.getDefault()
        java.util.Locale.setDefault(locale)
        try {
            block()
        } finally {
            java.util.Locale.setDefault(before)
        }
    }

    @Test
    fun `a german system language still writes a full stop`() {
        withLocale(java.util.Locale.GERMANY) {
            val message = SosMessage.compose("Hilfe!", 48.20849, 16.37208, "Notfall")
            assertTrue("a full stop instead of a comma", message.contains("48.20849,16.37208"))
            assertFalse("no comma as the decimal separator", message.contains("47,26543"))
        }
    }

    @Test
    fun `the link is the same in every language`() {
        var german = ""
        var english = ""
        withLocale(java.util.Locale.GERMANY) { german = SosMessage.mapsLink(48.20849, 16.37208) }
        withLocale(java.util.Locale.US) { english = SosMessage.mapsLink(48.20849, 16.37208) }
        assertEquals(english, german)
    }

    // --- how old the location is (02.09.2026) ---

    /**
     * a location without an age reads like "here it is now". if it is in truth from
     * yesterday, help drives to the wrong place and searches there. since the countdown looks
     * for a fresh position the normal case is fresh - but when none arrives (a cellar, a
     * train) the old one goes out, and then it has to say so.
     */
    @Test
    fun `a fresh location needs no note`() {
        assertEquals(null, SosMessage.ageNote(0L))
        assertEquals(null, SosMessage.ageNote(4L))
        assertEquals(null, SosMessage.ageNote(null))
    }

    @Test
    fun `from five minutes on the age stands with it`() {
        assertEquals(SosMessage.AgeUnit.MINUTES to 5, SosMessage.ageNote(5L))
        assertEquals(SosMessage.AgeUnit.MINUTES to 119, SosMessage.ageNote(119L))
    }

    @Test
    fun `from two hours on in hours`() {
        // "location from 180 minutes ago" is something the receiver has to convert first.
        assertEquals(SosMessage.AgeUnit.HOURS to 2, SosMessage.ageNote(120L))
        assertEquals(SosMessage.AgeUnit.HOURS to 25, SosMessage.ageNote(1500L))
    }

    @Test
    fun `the note stands on a line of its own behind the link`() {
        val message = SosMessage.compose(
            text = "Hilfe!",
            latitude = 48.20849,
            longitude = 16.37208,
            fallback = "Notfall",
            ageNote = "Standort von vor 3 Stunden",
        )
        val lines = message.lines()
        assertEquals("Hilfe!", lines[0])
        assertTrue(lines[1].startsWith("https://"))
        assertEquals("Standort von vor 3 Stunden", lines[2])
    }

    @Test
    fun `without a location no age stands there either`() {
        // otherwise the message would carry a note about something that is not in it.
        val message = SosMessage.compose(
            text = "Hilfe!",
            latitude = null,
            longitude = null,
            fallback = "Notfall",
            ageNote = "Standort von vor 3 Stunden",
        )
        assertEquals("Hilfe!", message)
    }

    /**
     * the cost figure in the settings computes with example coordinates - and since the
     * message names the age of an old location, it has to compute with that too. otherwise it
     * would say "costs one message" and in earnest it would be two. too low is the wrong
     * direction for a cost.
     */
    @Test
    fun `the age line can cost a second message`() {
        val text = "Bitte kommt schnell, mir ist schwindlig und ich kann nicht mehr aufstehen."
        val without = SosMessage.compose(text, 48.20849, 16.37208, "Notfall")
        val with = SosMessage.compose(
            text, 48.20849, 16.37208, "Notfall", ageNote = "Standort von vor 24 Stunden",
        )
        assertTrue("the line does not make the message longer", with.length > without.length)
        assertTrue(
            "the calculation needs the longer version",
            SosMessage.partsNeeded(with) >= SosMessage.partsNeeded(without),
        )
    }

    @Test
    fun `a short message with a location stays one message`() {
        val short = SosMessage.compose("Hilfe!", 48.20849, 16.37208, "Notfall")
        assertEquals(1, SosMessage.partsNeeded(short))
    }
}
