package org.biglau.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactMergeTest {

    private fun row(
        id: Long,
        name: String,
        number: String,
        label: String? = null,
        photo: String? = null,
        starred: Boolean = false,
    ) = ContactRow(id, name, number, label, photo, starred)

    @Test
    fun `Schreibweisen derselben Nummer werden als gleich erkannt`() {
        assertEquals(
            ContactMerge.normalizeNumber("+43 660 123 45 67"),
            ContactMerge.normalizeNumber("+436601234567"),
        )
        assertEquals(
            ContactMerge.normalizeNumber("0660/123-4567"),
            ContactMerge.normalizeNumber("0660 123 4567"),
        )
    }

    @Test
    fun `ein fuehrendes Plus bleibt unterscheidend`() {
        // Ohne Landesvorwahl laesst sich nicht sicher sagen, ob 0660... dieselbe
        // Nummer ist wie +43660... - also wird hier nicht geraten.
        assertTrue(
            ContactMerge.normalizeNumber("+436601234567") !=
                ContactMerge.normalizeNumber("06601234567"),
        )
    }

    @Test
    fun `dieselbe Nummer aus zwei Konten erscheint nur einmal`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "Anna Berger", "+43 660 1234567"),
                row(1, "Anna Berger", "+436601234567"),
            ),
        )
        assertEquals(1, merged.size)
        assertEquals(1, merged.first().numbers.size)
    }

    @Test
    fun `die erste Schreibweise bleibt stehen`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "Anna", "+43 660 1234567"),
                row(1, "Anna", "+436601234567"),
            ),
        )
        assertEquals("+43 660 1234567", merged.first().numbers.first().number)
    }

    @Test
    fun `eine Bezeichnung ersetzt eine zuvor gesehene ohne`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "Anna", "+436601234567", label = null),
                row(1, "Anna", "+436601234567", label = "Mobil"),
            ),
        )
        assertEquals("Mobil", merged.first().numbers.first().label)
    }

    @Test
    fun `verschiedene Nummern bleiben alle erhalten`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "Anna", "+436601234567", label = "Mobil"),
                row(1, "Anna", "+43512999888", label = "Arbeit"),
            ),
        )
        assertEquals(2, merged.first().numbers.size)
        assertTrue(merged.first().hasChoice)
    }

    @Test
    fun `ein Kontakt mit einer Nummer stellt keine Frage`() {
        val merged = ContactMerge.merge(listOf(row(1, "Anna", "+436601234567")))
        assertTrue(!merged.first().hasChoice)
        assertEquals("+436601234567", merged.first().primaryNumber)
    }

    @Test
    fun `namenlose Eintraege und Nummern ohne Ziffern fliegen raus`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "", "+436601234567"),
                row(2, "   ", "+436601234567"),
                row(3, "Ohne Nummer", "---"),
                row(4, "Anna", "+436601234567"),
            ),
        )
        assertEquals(listOf("Anna"), merged.map { it.name })
    }

    @Test
    fun `Favoriten stehen oben`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "Anna", "+431"),
                row(2, "Bertha", "+432", starred = true),
                row(3, "Carl", "+433"),
            ),
        )
        assertEquals(listOf("Bertha", "Anna", "Carl"), merged.map { it.name })
    }

    @Test
    fun `ein Favoritenkennzeichen an einer Zeile gilt fuer den Kontakt`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "Anna", "+431", starred = false),
                row(1, "Anna", "+432", starred = true),
            ),
        )
        assertTrue(merged.first().starred)
    }

    @Test
    fun `das erste vorhandene Foto gewinnt`() {
        val merged = ContactMerge.merge(
            listOf(
                row(1, "Anna", "+431", photo = null),
                row(1, "Anna", "+432", photo = "content://foto"),
            ),
        )
        assertEquals("content://foto", merged.first().photoUri)
    }

    @Test
    fun `ohne Foto bleibt es null`() {
        val merged = ContactMerge.merge(listOf(row(1, "Anna", "+431")))
        assertNull(merged.first().photoUri)
    }

    @Test
    fun `Namen werden getrimmt`() {
        assertEquals("Anna Berger", ContactMerge.merge(listOf(row(1, "  Anna Berger  ", "+431"))).first().name)
    }

    @Test
    fun `eine leere Liste ergibt eine leere Liste`() {
        assertTrue(ContactMerge.merge(emptyList()).isEmpty())
    }
}

/**
 * Ein Kontakt ohne Rufnummer.
 *
 * Heute liefert die Abfrage über Phone.CONTENT_URI keine Zeile für so jemanden, die Liste
 * enthält ihn also gar nicht. Aber `primaryNumber` war ein `first()` auf einer Liste, die
 * leer sein kann - ein Absturz, eine Umbaurunde entfernt. Und ein abgestürzter Launcher ist
 * ein schwarzes Telefon: genau so hat Android schon einmal die Startbildschirm-Rolle wieder
 * entzogen.
 */
class ContactWithoutNumberTest {

    private val ohneNummer = PhoneContact(
        id = 1L,
        name = "Nur E-Mail",
        photoUri = null,
        numbers = emptyList(),
    )

    private val mitNummer = PhoneContact(
        id = 2L,
        name = "Mit Nummer",
        photoUri = null,
        numbers = listOf(PhoneNumber("+436601234567", null)),
    )

    @Test
    fun `ohne Nummer stuerzt nichts ab`() {
        assertNull(ohneNummer.primaryNumber)
        assertFalse(ohneNummer.isCallable)
        assertFalse(ohneNummer.hasChoice)
    }

    @Test
    fun `mit einer Nummer gibt es nichts zu waehlen`() {
        assertEquals("+436601234567", mitNummer.primaryNumber)
        assertTrue(mitNummer.isCallable)
        assertFalse(mitNummer.hasChoice)
    }

    @Test
    fun `mit zwei Nummern wird gefragt`() {
        val zwei = mitNummer.copy(
            numbers = mitNummer.numbers + PhoneNumber("+436809876543", null),
        )
        assertTrue(zwei.hasChoice)
        assertTrue(zwei.isCallable)
    }
}
