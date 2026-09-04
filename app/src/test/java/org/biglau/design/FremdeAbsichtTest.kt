package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Was BigLau nicht selbst gebaut hat, startet es ueber [org.biglau.actions.Intents].
 *
 * `startActivity` auf eine fremde App wirft `ActivityNotFoundException`, wenn es die App
 * nicht gibt - und `SecurityException`, wenn die Berechtigung fehlt. Beides faengt `Intents`
 * ab und sagt einen Satz dazu. Zwei Zeilen in der Kontaktliste taten es selbst: wer ohne
 * Adressbuch-App auf "Im Adressbuch bearbeiten" tippte, sah BigLau abstuerzen.
 *
 * Eigene Bildschirme sind davon ausgenommen - eine Klasse aus demselben Programm gibt es
 * immer.
 */
class FremdeAbsichtTest {

    /** Woran man erkennt, dass das Ziel BigLau selbst ist. */
    private val eigenes = listOf("::class.java", "SettingsLink.", ".intent(", "Intents.")

    /**
     * Zwei Stellen duerfen selbst starten, und warum.
     *
     * Der Notruf-Zweig uebergibt an eine fremde Waehltastatur und will dabei ausdruecklich
     * **kein** BigLau treffen - das ist ein eigener Fall, der in `STATUS.md` als Frage
     * steht. Der Anruf selbst braucht seinen eigenen Auffang: eine `SecurityException`
     * heisst dort "Berechtigung fehlt" und fuehrt zur Nachfrage, nicht zu einem Hinweis.
     *
     * `Notice` startet einen eigenen Bildschirm, aber ueber eine Absicht statt ueber die
     * Klasse - damit das Design-System nicht von der App abhaengt. Steht dort so
     * begruendet.
     */
    private val erlaubt = listOf("DialerActivity.kt", "Notice.kt")

    @Test
    fun `fremde Absichten laufen ueber Intents`() {
        val nackt = mutableListOf<String>()
        Quelltext.dateien().forEach { datei ->
            if (datei.name == "Intents.kt" || datei.name in erlaubt) return@forEach
            val zeilen = datei.readLines()
            zeilen.forEachIndexed { index, zeile ->
                if (!zeile.contains("startActivity(")) return@forEachIndexed
                // Das Ziel steht oft in der naechsten Zeile, wenn der Aufruf umbricht.
                val fenster = zeilen.subList(index, minOf(zeilen.size, index + 4)).joinToString(" ")
                // Ein eigener Auffang ist auch einer. Drei Stellen fangen selbst ab und
                // werten das Ergebnis aus - die Regel meint die **ungefangenen**.
                val gefangen = zeile.contains("runCatching") ||
                    zeilen.subList(maxOf(0, index - 3), index).any { it.trim() == "try {" }
                if (!gefangen && eigenes.none { it in fenster }) {
                    nackt += "${datei.name}:${index + 1}: ${zeile.trim()}"
                }
            }
        }
        assertTrue(
            "Diese Aufrufe starten eine fremde App ohne Auffang - fehlt sie, stuerzt BigLau " +
                "ab, statt einen Satz zu sagen:\n" + nackt.joinToString("\n"),
            nackt.isEmpty(),
        )
    }

    /**
     * Die Kontaktliste liest neu, wenn sie wiederkommt.
     *
     * Sie schickt in den Editor des Adressbuchs, und von dort kommt kein Ergebnis zurueck.
     * Wer die Nummer aendert, sah danach weiter die alte - und die Zeile "Sofort anrufen"
     * haette sie gewaehlt.
     */
    @Test
    fun `die Kontaktliste liest nach dem fremden Editor neu`() {
        val quelle = Quelltext.ohneKommentare("org/biglau/contacts/ContactsActivity.kt")
        val laden = Quelltext.ausschnitt(quelle, "LaunchedEffect(granted", "}")
        assertTrue(
            "Die Kontakte werden nur einmal gelesen. Nach dem Bearbeiten im Adressbuch " +
                "stuende hier weiter die alte Nummer.",
            quelle.contains("LaunchedEffect(granted, fortsetzungen.intValue)"),
        )
        // Ein festes Fenster statt einer Grenze an einem Variablennamen: `substringBefore`
        // auf einen Namen, den es eines Tages nicht mehr gibt, liefert **den ganzen Rest**
        // der Datei - und die Regel waere gruen, ohne noch etwas zu pruefen. Genau das ist
        // heute Nacht schon einmal passiert.
        val fenster = quelle
            .let { Quelltext.ausschnitt(it, "LaunchedEffect(granted, fortsetzungen.intValue)") }
            .take(800)
        assertTrue(
            "Der geoeffnete Kontakt wird beim Neulesen nicht mitgenommen: $laden",
            fenster.contains("selected = selected?.let"),
        )
    }

    /**
     * Die Zurueck-Taste fuehrt zurueck.
     *
     * `FLAG_ACTIVITY_NEW_TASK` haengt die fremde App in **ihre** Aufgabe. Am 04.09.2026
     * gemessen: "Im Adressbuch bearbeiten" landete in Aufgabe t995 - der des Adressbuchs -,
     * und einmal Zurueck stellte einen in dessen Kontaktliste, nicht wieder zu BigLau. Wer
     * dort weiterkommt, ist nicht mehr in der grossen Schrift. Ohne die Flagge: t1030,
     * dieselbe Aufgabe wie BigLau, und Zurueck fuehrt zurueck.
     *
     * Von einem Dienst oder der Application aus gibt es keine Aufgabe, in die etwas
     * hineinkoennte - dort bleibt die Flagge noetig. Deshalb haengt sie an der Frage, ob
     * ein Bildschirm dahintersteht, und nicht an einem festen Wert.
     */
    @Test
    fun `eine fremde App startet in BigLaus Aufgabe, wenn es eine gibt`() {
        val intents = Quelltext.ohneKommentare("org/biglau/actions/Intents.kt")
        val start = Quelltext.ausschnitt(intents, "private inline fun start(", "\n    }")
        assertTrue(
            "Intents haengt jede fremde App in eine eigene Aufgabe; die Zurueck-Taste " +
                "fuehrt dann nicht zu BigLau zurueck:\n$start",
            start.contains("bildschirmHinter(context) != null"),
        )
        val kontakte = Quelltext.ohneKommentare("org/biglau/contacts/ContactRepository.kt")
        assertTrue(
            "Der Kontakt-Editor bringt die Flagge selbst mit - dann hilft der Auffang in " +
                "Intents nichts.",
            !kontakte.contains("FLAG_ACTIVITY_NEW_TASK"),
        )
    }
}
