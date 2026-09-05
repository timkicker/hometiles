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
 * Die Kontaktliste machte es von Anfang an richtig (`contacts_loading`). Die App-Liste zeigt
 * während des Ladens gar nichts — kein schöner, aber auch kein falscher Zustand; sie lädt aus
 * `LauncherApps` und ist schnell da.
 *
 * **Die Anrufliste stand hier als Vorbild** — sie unterscheidet drei Gründe fürs Leersein,
 * und das klang nach genug. Am 04.09.2026 nachgesehen: keiner der drei hieß „wird gerade
 * gelesen". `groups` fängt leer an, `CallLogEmpty.reason` macht daraus „Noch keine Anrufe",
 * und genau dieser Satz stand da, solange der Anbieter las. Ein Lob im Kommentar ist keine
 * Prüfung; jetzt steht die Anrufliste hier als Regel und nicht als Beispiel.
 */
class LadenTest {

    private val sms = Quelltext.datei("org/biglau/sms/SmsActivity.kt").readText()

    private val anrufe = Quelltext.datei("org/biglau/phone/DialerActivity.kt").readText()

    /**
     * Und dasselbe ueberall, nicht nur an den drei gefundenen Stellen.
     *
     * Dreimal an einem Tag dieselbe Sache: die Nachrichtenliste, die Anrufliste, die
     * Favoriten. Jedes Mal ein wahrer Satz, jedes Mal zu frueh, jedes Mal einzeln
     * gefunden. Beim dritten Mal ist eine Einzelregel nicht mehr die richtige Antwort.
     *
     * **Es geht nur um Listen, die nachtraeglich gefuellt werden.** Eine Liste, die beim
     * Zeichnen schon fertig ist - die Verknuepfungen einer App, die vorhandenen Widgets,
     * die ausgeblendeten Apps aus der Konfiguration - ist nie faelschlich leer. Deshalb
     * fragt die Regel, ob die geprüfte Sammlung ein `var` mit `mutableStateOf` ist: nur
     * dann gibt es einen Zeitpunkt, zu dem sie leer ist, ohne leer zu sein.
     *
     * Eine Auswahlbeschriftung wie „keine Gruppierung" faellt ebenfalls heraus - dort wird
     * gar nichts auf Leere geprueft.
     */
    @Test
    fun `kein Satz ueber Leere haengt allein an einer leeren Liste`() {
        val leerSaetze = Regex("""R\.string\.[a-z_]*(none|empty|no_missed|no_match)\b""")
        val belege = listOf("laedt", "loading", "isNotEmpty()")
        val ungeschuetzt = Quelltext.dateien()
            .filter { it.name.endsWith("Activity.kt") }
            .flatMap { datei ->
                val zeilen = datei.readLines()
                val text = datei.readText()
                zeilen.withIndex()
                    .filter { (_, z) ->
                        leerSaetze.containsMatchIn(z) && !Quelltext.istKommentarzeile(z)
                    }
                    .filterNot { (i, _) ->
                        zeilen.subList(maxOf(0, i - 12), i + 1)
                            .any { zeile -> belege.any { it in zeile } }
                    }
                    .filter { (i, _) ->
                        // Woran haengt der Satz? Und wird diese Sammlung erst nachtraeglich
                        // gefuellt?
                        val geprueft = zeilen.subList(maxOf(0, i - 12), i + 1)
                            .firstNotNullOfOrNull {
                                Regex("""(\w+)\.isEmpty\(\)""").find(it)?.groupValues?.get(1)
                            }
                        geprueft != null &&
                            Regex("""var $geprueft by remember""").containsMatchIn(text)
                    }
                    .map { (i, z) -> "${datei.name}:${i + 1}: ${z.trim()}" }
            }
        assertEquals(
            "Hier steht ein Satz ueber Leere, der an nichts haengt ausser der leeren Liste " +
                "selbst - und die wird erst nachtraeglich gefuellt. Solange gelesen wird, " +
                "ist sie leer, und dann ist der Satz eine Falschaussage.",
            emptyList<String>(),
            ungeschuetzt,
        )
    }

    @Test
    fun `die Anrufliste unterscheidet laden von leer`() {
        assertTrue("calllog_loading fehlt", "R.string.calllog_loading" in anrufe)
        val ab = anrufe.indexOf("leerWeil != null")
        assertTrue("Der leere Zustand der Anrufliste ist weg", ab > 0)
        val block = anrufe.substring(ab, minOf(anrufe.length, ab + 700))
        assertTrue(
            "Der Grund fuers Leersein wird gezeigt, ohne vorher zu fragen, ob ueberhaupt " +
                "schon gelesen wurde. Dann steht dort waehrend des Lesens: keine Anrufe.",
            "if (loading)" in block,
        )
    }

    /** Auch hier: der Ladezustand muss in **beiden** Ausgaengen enden. */
    @Test
    fun `der Ladezustand der Anrufliste endet auch ohne Berechtigung`() {
        val treffer = Regex("""loading = false""").findAll(anrufe).count()
        assertEquals(
            "loading wird nicht in beiden Ausgaengen zurueckgesetzt. Ein Ladezustand, der " +
                "nie endet, ist schlimmer als gar keiner.",
            2,
            treffer,
        )
    }

    @Test
    fun `die Nachrichtenliste unterscheidet laden von leer`() {
        assertTrue("sms_loading fehlt", "R.string.sms_loading" in sms)
        assertTrue(
            "Der leere Zustand hängt an nichts - dann steht „noch keine Nachrichten\" auch " +
                "während des Lesens da.",
            "if (loading) R.string.sms_loading else R.string.sms_empty" in sms,
        )
    }

    /** Und der Ladezustand wird auch wieder abgeschaltet - in **beiden** Ausgängen. */
    @Test
    fun `der Ladezustand endet, auch ohne Berechtigung`() {
        val ladevorgang = Quelltext.ausschnitt(sms, "var loading by remember", "val threads")
        assertEquals(
            "`loading = false` kommt nicht zweimal vor: einmal nach dem Lesen und einmal im " +
                "Zweig ohne Leseberechtigung. Fehlt der zweite, steht dort für immer " +
                "„Nachrichten werden gelesen…\".",
            2,
            Regex("""loading = false""").findAll(ladevorgang).count(),
        )
    }

    @Test
    fun `die Kontaktliste hat ihren Ladehinweis behalten`() {
        val kontakte = Quelltext.datei("org/biglau/contacts/ContactsActivity.kt").readText()
        assertTrue("contacts_loading ist weg", "R.string.contacts_loading" in kontakte)
    }
}
