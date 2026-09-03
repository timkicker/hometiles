package org.biglau

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine Kachel, deren App weg ist, führt zum Editor — nicht nur zu einem Satz.
 *
 * Wird eine App deinstalliert, bleibt ihre Kachel stehen. Beim Antippen sagte BigLau „Die
 * App gibt es nicht mehr. Kachel neu belegen." und liess es dabei. Wie man sie neu belegt,
 * stand nicht da: der Langdruck, auf den man erst kommen muss — und der ist abschaltbar
 * (`a11y`), dann führte gar nichts mehr hin.
 *
 * Seit dem 3.9.2026 steht der Kachel-Editor gleich hinter der Meldung, für **diese** Zelle.
 * Wer ihn nicht will, geht mit der Zurück-Geste heraus.
 *
 * Dieselbe Regel wie beim Notruf ohne Kontakte, bei der Anrufliste und beim unerreichbaren
 * Bildschirm: der Weg dorthin statt der Wegbeschreibung.
 */
class DeadTileTest {

    private val quelle = Quelltext.ohneKommentare("org/biglau/MainActivity.kt")

    /**
     * Die Stelle, die den Start behandelt.
     *
     * `app_gone` stand in dieser Datei zweimal - einmal beim Tipp, einmal im PIN-Ablauf der
     * App-Sperre. Die erste Fassung dieser Regel suchte das erste Vorkommen und fiel prompt
     * über die falsche Stelle. Seit dem 03.09.2026 gibt es die Meldung nur noch **einmal**:
     * beide Wege gehen durch `starten`, und dorthin kommt die Zelle als Parameter. Also
     * sucht die Regel jetzt nicht mehr eine Funktion, die sie beim Namen kennt, sondern die
     * Stelle, an der die Meldung steht.
     */
    private val nachDerMeldung = quelle.substringAfter("R.string.app_gone", "")

    @Test
    fun `nach der Meldung ueber die fehlende App oeffnet sich der Editor`() {
        assertTrue("die Meldung gibt es nicht mehr", nachDerMeldung.isNotEmpty())
        assertTrue(
            "Auf die Meldung folgt kein Weg zum Neubelegen - dann steht der Nutzer wieder " +
                "vor einem Satz statt vor einer Handlung.",
            "TileEditorActivity.intent(" in nachDerMeldung.take(400),
        )
    }

    /**
     * Und der Editor muss **die** Zelle bekommen, auf die getippt wurde.
     *
     * Welche Namen die Koordinaten unterwegs tragen, geht die Regel nichts an - sie hiessen
     * `cell.x`/`cell.y`, bis der Start eine eigene Funktion bekam, und die Regel fiel um,
     * obwohl dieselbe Zelle ankam. Gefragt ist deshalb der **Anfang** des Weges: wer den
     * Start ruft, muss die angetippte Zelle mitgeben.
     */
    @Test
    fun `der Editor bekommt die Zelle, auf die getippt wurde`() {
        val rufe = Quelltext.datei("org/biglau/MainActivity.kt").readLines()
            .filter { it.trim().startsWith("starten(") }
        assertTrue("Niemand ruft den Start - liest die Regel noch, was sie meint?", rufe.isNotEmpty())
        val ohneZelle = rufe.filterNot { "cell.x" in it || "wartend.x" in it }
        assertTrue(
            "Hier wird gestartet, ohne die angetippte Zelle mitzugeben: $ohneZelle - " +
                "dann landet man nach der Meldung auf irgendeiner Kachel.",
            ohneZelle.isEmpty(),
        )
    }
}
