package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * „Kein Treffer" nur, wenn es etwas zu treffen gab.
 *
 * Vier Listen hat BigLau: Kontakte, Nachrichten, Anrufliste, Apps. Drei sagten am 3.9.2026
 * das Richtige, wenn sie leer sind — die Anrufliste „noch keine Anrufe", die Nachrichten
 * „noch keine Nachrichten", die App-Liste „kein Treffer" **nur wenn es Apps gibt**
 * (`all.isNotEmpty() && shown.isEmpty()`).
 *
 * Die Kontaktliste sagte immer „Kein Kontakt passt dazu." — auch bei leerem Suchfeld auf
 * einem Telefon ohne Kontakte. Dann bezieht sich „dazu" auf nichts, und wer es liest, sucht
 * den Fehler bei sich oder hält die App für kaputt. Es ist dieselbe Sorte Fehler wie „Kachel
 * neu belegen" in der App-Liste: ein richtiger Satz am falschen Ort.
 */
class LeereListeTest {

    private val kontakte = Quelltext.datei("org/biglau/contacts/ContactsActivity.kt").readText()

    @Test
    fun `die Kontaktliste unterscheidet leer von kein Treffer`() {
        assertTrue(
            "contacts_none fehlt - dann sagt eine leere Kontaktliste weiter „kein Treffer\", " +
                "auch wenn niemand gesucht hat.",
            "R.string.contacts_none" in kontakte,
        )
        val stelle = Quelltext.ausschnitt(kontakte, "R.string.contacts_no_match")
            .let { Quelltext.ausschnitt(kontakte, "", "R.string.contacts_no_match").takeLast(300) + it.take(100) }
        assertTrue(
            "Die Auswahl zwischen den beiden Sätzen hängt an nichts - `hatKontakte` fehlt.",
            "hatKontakte" in stelle,
        )
    }

    @Test
    fun `die App-Liste macht es weiterhin richtig`() {
        val apps = Quelltext.datei("org/biglau/apps/AppDrawerActivity.kt").readText()
        assertTrue(
            "Der Schutz `all.isNotEmpty() && shown.isEmpty()` ist weg - dann sagt die " +
                "App-Liste „kein Treffer\", bevor überhaupt Apps geladen sind.",
            "all.isNotEmpty() && shown.isEmpty()" in apps,
        )
    }

    /** Und die beiden Sätze sagen wirklich Verschiedenes. */
    @Test
    fun `leer und kein Treffer sind nicht derselbe Satz`() {
        listOf("values", "values-de").forEach { sprache ->
            fun wert(name: String) = Quelltext.textWert(name, sprache)
            assertEquals(
                "$sprache: contacts_none und contacts_no_match sagen dasselbe - dann war " +
                    "die Unterscheidung umsonst.",
                false,
                wert("contacts_none") == wert("contacts_no_match"),
            )
        }
    }
}
