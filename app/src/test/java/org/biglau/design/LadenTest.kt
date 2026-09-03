package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Solange gelesen wird, sagt keine Liste, sie sei leer.
 *
 * „Noch keine Nachrichten" ist ein wahrer Satz — aber nicht, während die Nachrichten gerade
 * gelesen werden. Bis zum 3.9.2026 stand er in der Nachrichtenliste vom ersten Bild an da,
 * und danach sprang die volle Liste hinein. Auf dem Telefon des Nutzers dauert das sichtbar
 * lange, denn der Ladevorgang holt die Nachrichten **und alle 338 Kontakte** für die Namen.
 *
 * **Ein wahrer Satz zum falschen Zeitpunkt ist eine Falschaussage** — dieselbe Sorte wie
 * „kein Kontakt passt dazu" ohne Suche und „Kachel neu belegen" in der App-Liste.
 *
 * Die Kontaktliste machte es von Anfang an richtig (`contacts_loading`), die Anrufliste
 * unterscheidet sogar drei Gründe fürs Leersein. Die App-Liste zeigt während des Ladens gar
 * nichts — kein schöner, aber auch kein falscher Zustand; sie lädt aus `LauncherApps` und ist
 * schnell da.
 */
class LadenTest {

    private val sms = Quelltext.datei("org/biglau/sms/SmsActivity.kt").readText()

    @Test
    fun `die Nachrichtenliste unterscheidet laden von leer`() {
        assertTrue("sms_loading fehlt", "R.string.sms_loading" in sms)
        assertTrue(
            "Der leere Zustand hängt an nichts - dann steht „noch keine Nachrichten\" auch " +
                "während des Lesens da.",
            "if (laedt) R.string.sms_loading else R.string.sms_empty" in sms,
        )
    }

    /** Und der Ladezustand wird auch wieder abgeschaltet - in **beiden** Ausgängen. */
    @Test
    fun `der Ladezustand endet, auch ohne Berechtigung`() {
        val ladevorgang = sms.substringAfter("var laedt by remember").substringBefore("val threads")
        assertEquals(
            "`laedt = false` kommt nicht zweimal vor: einmal nach dem Lesen und einmal im " +
                "Zweig ohne Leseberechtigung. Fehlt der zweite, steht dort für immer " +
                "„Nachrichten werden gelesen…\".",
            2,
            Regex("""laedt = false""").findAll(ladevorgang).count(),
        )
    }

    @Test
    fun `die Kontaktliste hat ihren Ladehinweis behalten`() {
        val kontakte = Quelltext.datei("org/biglau/contacts/ContactsActivity.kt").readText()
        assertTrue("contacts_loading ist weg", "R.string.contacts_loading" in kontakte)
    }
}
