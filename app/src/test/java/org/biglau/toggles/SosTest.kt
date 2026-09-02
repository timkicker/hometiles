package org.biglau.toggles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SosTest {

    @Test
    fun `ohne Standort steht nur der Text in der Nachricht`() {
        // Eine Nachricht, die so aussieht als haette sie einen Standort, aber keinen hat,
        // waere im Ernstfall schlimmer als gar keine Ortsangabe.
        assertEquals("Hilfe!", SosMessage.compose("Hilfe!", null, null, "Notfall"))
        assertEquals("Hilfe!", SosMessage.compose("Hilfe!", 47.26, null, "Notfall"))
        assertEquals("Hilfe!", SosMessage.compose("Hilfe!", null, 11.39, "Notfall"))
    }

    @Test
    fun `mit Standort haengt ein anklickbarer Link an`() {
        val message = SosMessage.compose("Hilfe!", 48.20849, 16.37208, "Notfall")
        assertTrue(message.startsWith("Hilfe!\n"))
        assertTrue(message.contains("https://maps.google.com/?q=48.20849,16.37208"))
    }

    @Test
    fun `Koordinaten werden mit Punkt geschrieben`() {
        // Mit deutschem Komma waere der Link kaputt - deshalb feste Locale.
        assertEquals("https://maps.google.com/?q=48.20849,16.37208", SosMessage.mapsLink(48.20849, 16.37208))
        assertTrue(!SosMessage.mapsLink(48.20849, 16.37208).contains(","+"26"))
    }

    @Test
    fun `negative Koordinaten bleiben negativ`() {
        assertEquals("https://maps.google.com/?q=-33.86880,151.20930", SosMessage.mapsLink(-33.8688, 151.2093))
    }

    @Test
    fun `ein leerer Text faellt auf die Vorgabe zurueck`() {
        assertEquals("Notfall", SosMessage.compose("", null, null, "Notfall"))
        assertEquals("Notfall", SosMessage.compose("   ", null, null, "  Notfall  "))
    }

    @Test
    fun `die Zahl der Teil-SMS wird richtig gerechnet`() {
        assertEquals(1, SosMessage.partsNeeded(""))
        assertEquals(1, SosMessage.partsNeeded("a".repeat(160)))
        assertEquals(2, SosMessage.partsNeeded("a".repeat(161)))
        assertEquals(2, SosMessage.partsNeeded("a".repeat(306)))
        assertEquals(3, SosMessage.partsNeeded("a".repeat(307)))
    }

    @Test
    fun `der Countdown zaehlt herunter`() {
        assertEquals(5, SosCountdown.remaining(1000L, 1000L, 5))
        assertEquals(3, SosCountdown.remaining(1000L, 3000L, 5))
        assertEquals(0, SosCountdown.remaining(1000L, 6000L, 5))
    }

    @Test
    fun `der Countdown geht nicht ins Negative`() {
        assertEquals(0, SosCountdown.remaining(1000L, 60000L, 5))
    }

    @Test
    fun `eine Uhr die zurueckspringt bricht nichts`() {
        // Zeitzonenwechsel oder Zeitkorrektur waehrend des Countdowns.
        assertEquals(5, SosCountdown.remaining(5000L, 1000L, 5))
    }

    @Test
    fun `null Sekunden heisst sofort`() {
        assertEquals(0, SosCountdown.remaining(1000L, 1000L, 0))
    }

    @Test
    fun `die Wartezeit ist nach oben begrenzt`() {
        assertEquals(SosCountdown.MAX_SECONDS, SosCountdown.clamp(99))
        assertEquals(0, SosCountdown.clamp(-5))
    }

    @Test
    fun `ohne Nummern ist nichts eingerichtet`() {
        assertTrue(!SosCountdown.isConfigured(emptyList()))
        assertTrue(!SosCountdown.isConfigured(listOf("", "   ")))
        assertTrue(SosCountdown.isConfigured(listOf("+43660")))
    }
}

/**
 * Die Koordinaten in der Notruf-SMS.
 *
 * Der Link muss beim Empfaenger aufgehen, egal in welcher Sprache das absendende Telefon
 * laeuft. Auf einem deutschen Geraet formatiert die Standardsprache "47,26543" mit Komma -
 * und ein Kartenlink mit Komma ist kein Kartenlink mehr.
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
    fun `deutsche Systemsprache schreibt trotzdem einen Punkt`() {
        withLocale(java.util.Locale.GERMANY) {
            val message = SosMessage.compose("Hilfe!", 48.20849, 16.37208, "Notfall")
            assertTrue("Punkt statt Komma", message.contains("48.20849,16.37208"))
            assertFalse("kein Komma als Dezimaltrennzeichen", message.contains("47,26543"))
        }
    }

    @Test
    fun `der Link ist in jeder Sprache derselbe`() {
        var deutsch = ""
        var englisch = ""
        withLocale(java.util.Locale.GERMANY) { deutsch = SosMessage.mapsLink(48.20849, 16.37208) }
        withLocale(java.util.Locale.US) { englisch = SosMessage.mapsLink(48.20849, 16.37208) }
        assertEquals(englisch, deutsch)
    }

    // --- Wie alt der Standort ist (02.09.2026) ---

    /**
     * Ein Standort ohne Alter liest sich wie „hier ist er jetzt". Ist er in Wahrheit von
     * gestern, fährt die Hilfe an den falschen Ort und sucht dort. Seit der Countdown nach
     * einer frischen Position sucht, ist der Normalfall frisch — aber wenn keine kommt (im
     * Keller, im Zug), geht die alte hinaus, und dann muss es dabeistehen.
     */
    @Test
    fun `ein frischer Standort braucht keinen Hinweis`() {
        assertEquals(null, SosMessage.ageNote(0L))
        assertEquals(null, SosMessage.ageNote(4L))
        assertEquals(null, SosMessage.ageNote(null))
    }

    @Test
    fun `ab fuenf Minuten steht das Alter dabei`() {
        assertEquals(SosMessage.AgeUnit.MINUTES to 5, SosMessage.ageNote(5L))
        assertEquals(SosMessage.AgeUnit.MINUTES to 119, SosMessage.ageNote(119L))
    }

    @Test
    fun `ab zwei Stunden in Stunden`() {
        // "Standort von vor 180 Minuten" muss der Empfaenger erst umrechnen.
        assertEquals(SosMessage.AgeUnit.HOURS to 2, SosMessage.ageNote(120L))
        assertEquals(SosMessage.AgeUnit.HOURS to 25, SosMessage.ageNote(1500L))
    }

    @Test
    fun `der Hinweis steht in einer eigenen Zeile hinter dem Link`() {
        val nachricht = SosMessage.compose(
            text = "Hilfe!",
            latitude = 48.20849,
            longitude = 16.37208,
            fallback = "Notfall",
            ageNote = "Standort von vor 3 Stunden",
        )
        val zeilen = nachricht.lines()
        assertEquals("Hilfe!", zeilen[0])
        assertTrue(zeilen[1].startsWith("https://"))
        assertEquals("Standort von vor 3 Stunden", zeilen[2])
    }

    @Test
    fun `ohne Standort steht auch kein Alter da`() {
        // Sonst stuende in der Nachricht ein Hinweis auf etwas, das gar nicht drinsteht.
        val nachricht = SosMessage.compose(
            text = "Hilfe!",
            latitude = null,
            longitude = null,
            fallback = "Notfall",
            ageNote = "Standort von vor 3 Stunden",
        )
        assertEquals("Hilfe!", nachricht)
    }

    /**
     * Die Kostenangabe in den Einstellungen rechnet mit Beispielkoordinaten — und seit die
     * Nachricht das Alter eines alten Standorts nennt, muss sie auch damit rechnen. Sonst
     * stünde dort „kostet eine SMS", und im Ernstfall wären es zwei. Zu niedrig ist bei
     * Kosten die falsche Richtung.
     */
    @Test
    fun `die Alterszeile kann eine zweite SMS kosten`() {
        val text = "Bitte kommt schnell, mir ist schwindlig und ich kann nicht mehr aufstehen."
        val ohne = SosMessage.compose(text, 48.20849, 16.37208, "Notfall")
        val mit = SosMessage.compose(
            text, 48.20849, 16.37208, "Notfall", ageNote = "Standort von vor 24 Stunden",
        )
        assertTrue("die Zeile macht die Nachricht nicht laenger", mit.length > ohne.length)
        assertTrue(
            "die Rechnung braucht die laengere Fassung",
            SosMessage.partsNeeded(mit) >= SosMessage.partsNeeded(ohne),
        )
    }

    @Test
    fun `eine kurze Nachricht mit Standort bleibt eine SMS`() {
        val kurz = SosMessage.compose("Hilfe!", 48.20849, 16.37208, "Notfall")
        assertEquals(1, SosMessage.partsNeeded(kurz))
    }
}
