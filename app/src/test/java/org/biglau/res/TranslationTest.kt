package org.biglau.res

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Deutsch und Englisch müssen zueinander passen.
 *
 * Zwei Fehler, die sonst erst am Gerät auffallen — und der zweite stürzt ab:
 *
 * 1. Ein Text, den es nur auf Englisch gibt, erscheint im deutschen Telefon **auf
 *    Englisch**. Niemand merkt es, solange man die App in der eigenen Sprache nicht ansieht.
 * 2. Fehlt in der Übersetzung ein Platzhalter (`%1$s`), den der Code füllt, wirft Android
 *    beim Anzeigen eine Ausnahme. Das trifft dann **nur** die deutschen Nutzer — also genau
 *    den, für den diese App gebaut wird.
 *
 * Anlass war ein dritter Fund derselben Sorte: zehn deutsche Texte hatten ihr schließendes
 * Anführungszeichen verloren, weil Android gerade Anführungszeichen verschluckt. Siehe
 * [QuotesTest]. Was man nur am Bildschirm sieht, prüft man besser maschinell.
 */
class TranslationTest {

    private val en = File("src/main/res/values/strings.xml").readText()
    private val de = File("src/main/res/values-de/strings.xml").readText()
    private val enPlural = File("src/main/res/values/plurals.xml").readText()
    private val dePlural = File("src/main/res/values-de/plurals.xml").readText()

    /** Der Produktname wird nicht übersetzt - er heißt in jeder Sprache BigLau. */
    private val nurEnglisch = setOf("app_name")

    private fun namen(xml: String, tag: String) =
        Regex("""<$tag name="([^"]+)"""").findAll(xml).map { it.groupValues[1] }.toSet()

    private fun texte(xml: String) = Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        .findAll(xml).associate { it.groupValues[1] to it.groupValues[2] }

    private fun platzhalter(text: String) =
        Regex("""%\d\$[sd]""").findAll(text).map { it.value }.toList().sorted()

    @Test
    fun `jeder Text gibt es auch auf Deutsch`() {
        assertEquals(emptySet<String>(), namen(en, "string") - namen(de, "string") - nurEnglisch)
        assertEquals(emptySet<String>(), namen(de, "string") - namen(en, "string"))
    }

    @Test
    fun `jede Mehrzahlform gibt es in beiden Sprachen`() {
        assertEquals(namen(enPlural, "plurals"), namen(dePlural, "plurals"))
    }

    /** Ein fehlender Platzhalter in der Übersetzung ist ein Absturz, kein Schönheitsfehler. */
    @Test
    fun `die Platzhalter stimmen ueberein`() {
        val english = texte(en)
        val deutsch = texte(de)
        val abweichungen = english.keys.intersect(deutsch.keys).mapNotNull { name ->
            val a = platzhalter(english.getValue(name))
            val b = platzhalter(deutsch.getValue(name))
            if (a != b) "$name: $a gegen $b" else null
        }
        assertEquals(emptyList<String>(), abweichungen)
    }

    /**
     * Ein längerer Text, der in beiden Dateien wortgleich steht, ist fast immer eine
     * vergessene Übersetzung. Kurze Wörter wie „SOS" oder „OK" dürfen gleich sein.
     */
    @Test
    fun `kein laengerer Text steht unuebersetzt da`() {
        val english = texte(en)
        val deutsch = texte(de)
        val gleich = english.keys.intersect(deutsch.keys).filter { name ->
            val text = english.getValue(name)
            text.length > 12 && text == deutsch.getValue(name)
        }
        assertEquals(emptyList<String>(), gleich)
    }
}
