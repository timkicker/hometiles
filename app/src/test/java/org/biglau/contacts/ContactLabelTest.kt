package org.biglau.contacts

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Bezeichnung einer Nummer kommt vom System, nicht von uns.
 *
 * Am Telefon des Nutzers gesehen (03.09.2026): unter der Nummer seines Vaters stand **„Mobil"**,
 * während der Knopf daneben „Call straight away" hieß. Das Gerät steht auf Englisch; drei
 * deutsche Wörter waren fest im Quelltext einer Zuordnung eingetragen, und alles außerhalb
 * dieser drei - Fax, Pager, Hauptanschluss, eigene Bezeichnungen - hatte gar keine.
 *
 * `Phone.getTypeLabel` kennt alle Arten und übersetzt in die Sprache der übergebenen
 * Ressourcen. Genau deshalb bekommt `load` sie jetzt vom Aufrufer: eine Activity gibt ihre
 * eigenen, und die stehen schon in der Sprache der **App** - nicht in der des Telefons.
 * Ohne diesen Umweg wäre der Fehler bloß von Deutsch nach Englisch gewandert.
 */
class ContactLabelTest {

    private val quelle = Quelltext.file("org/biglau/contacts/ContactRepository.kt").readText()

    @Test
    fun `die Bezeichnung kommt aus getTypeLabel`() {
        assertTrue("getTypeLabel wird nicht benutzt", "getTypeLabel(" in quelle)
    }

    @Test
    fun `keine eigene Zuordnung mit festen Woertern`() {
        val verboten = listOf("\"Mobil\"", "\"Privat\"", "\"Arbeit\"", "\"Mobile\"", "\"Home\"", "\"Work\"")
        assertEquals(
            "feste Bezeichnung im Quelltext",
            emptyList<String>(),
            verboten.filter { it in quelle },
        )
    }

    /**
     * Und die Ressourcen kommen von außen. Nähme das Lesen die des Anwendungs-Contexts,
     * stünde die Bezeichnung in der Sprache des Telefons statt in der der App - derselbe
     * Fehler, nur unauffälliger.
     */
    @Test
    fun `die Sprache gibt der Aufrufer vor`() {
        assertTrue("load nimmt keine Ressourcen", "fun load(resources: Resources" in quelle)
        val rufer = listOf(
            "org/biglau/contacts/ContactsActivity.kt",
            "org/biglau/sms/SmsActivity.kt",
            "org/biglau/phone/DialerActivity.kt",
        )
        val ohne = rufer.filterNot { "load(resources)" in Quelltext.file(it).readText() }
        assertEquals("liest Kontakte ohne eigene Sprache: $ohne", emptyList<String>(), ohne)
    }
}
