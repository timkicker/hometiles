package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein abgebrochener Schritt hinterlaesst nichts.
 *
 * Wer auf einer **Ordner**kachel „Ordner anlegen" waehlt, bekommt eine Rueckfrage: der alte
 * Ordner samt Inhalt ginge verloren. Antwortet er mit „Behalten", soll alles sein wie
 * vorher.
 *
 * Bis zum 04.09.2026 war es das nicht. `onNewFolder` legte den neuen Ordner-Screen an,
 * **bevor** die Rueckfrage kam; sagte man „Behalten", blieb er liegen - leer, unerreichbar,
 * in jeder Sicherung. Am Geraet erzeugt und in `config.json` gesehen: `folder4`, null
 * Kacheln, kein Weg hin.
 *
 * Aufgefallen ist es nur, weil die Einstellungen ihn danach auflisteten („These folders have
 * no tile leading to them"). Der Kommentar ueber dieser Liste behauptete zugleich, neue
 * entstuenden nicht mehr - er beschrieb die Absicht, nicht den Zustand.
 *
 * Die Regel: das Anlegen haengt an derselben Bedingung wie das Schreiben.
 */
class OrdnerErstSpaeterTest {

    private val editor = Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt")

    @Test
    fun `der Ordner entsteht erst beim Schreiben`() {
        val ab = editor.indexOf("onNewFolder = {")
        assertTrue("onNewFolder gibt es nicht mehr", ab > 0)
        val rumpf = editor.substring(ab, minOf(editor.length, ab + 400))
        assertTrue(
            "onNewFolder legt den Ordner selbst an. Dann bleibt er liegen, wenn die " +
                "Rueckfrage mit Behalten beantwortet wird.",
            "FolderEdits.newFolder" !in rumpf,
        )

        val beimSchreiben = editor.indexOf("fun writeNow(")
        assertTrue("writeNow gibt es nicht mehr", beimSchreiben > 0)
        val schreiben = editor.substring(beimSchreiben, minOf(editor.length, beimSchreiben + 800))
        assertTrue(
            "Niemand legt den Ordner an, wenn die Kachel geschrieben wird - dann zeigt " +
                "die Kachel auf einen Ordner, den es nicht gibt.",
            "FolderEdits.newFolder" in schreiben,
        )
    }

    /** Und nur, wenn es ihn noch nicht gibt - sonst waere jedes Schreiben ein neuer Ordner. */
    @Test
    fun `ein vorhandener Ordner wird nicht zweimal angelegt`() {
        val ab = editor.indexOf("fun writeNow(")
        val rumpf = editor.substring(ab, minOf(editor.length, ab + 800))
        assertTrue(
            "Das Anlegen fragt nicht, ob der Ordner schon da ist.",
            "screens.none" in rumpf,
        )
    }

    /**
     * Und die Rueckfrage selbst gibt es weiterhin.
     *
     * Sie zaehlt, was verloren ginge: „5 tiles inside go with it. That cannot be undone."
     */
    @Test
    fun `das Ersetzen einer Ordnerkachel fragt vorher`() {
        val ab = editor.indexOf("fun write(")
        assertTrue("write gibt es nicht mehr", ab > 0)
        val rumpf = editor.substring(ab, minOf(editor.length, ab + 300))
        assertTrue(
            "Eine Ordnerkachel wird wieder ohne Rueckfrage ueberschrieben - dann ist der " +
                "Ordner samt Inhalt weg, ohne dass jemand gefragt wurde.",
            "replacingFolder" in rumpf,
        )
    }
}
