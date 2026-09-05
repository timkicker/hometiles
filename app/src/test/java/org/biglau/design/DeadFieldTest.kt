package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein Feld, das nur gefüllt und nie gelesen wird, ist eine vergessene Zusage.
 *
 * Anlass: `CallView.otherCallWaiting`. Der Dienst setzte es bei jedem zweiten Anruf
 * gewissenhaft — und **keine Zeile der Oberfläche las es je**. Sichtbar wurde das erst am
 * Emulator: während eines Gesprächs klingelte es, der Bildschirm zeigte nur den neuen
 * Anrufer, und der erste Anruf war danach weder zu sehen noch zu erreichen.
 *
 * Das ist die Schwester von [DeadLogicTest]: dort Funktionen, die niemand ruft, hier Werte,
 * die niemand liest. Beides sieht im Quelltext nach fertiger Arbeit aus.
 *
 * Geprüft werden nur Felder im Kopf einer `data class` — das sind die Werte, die von einer
 * Stelle zur anderen gereicht werden. Ein Zuweisen (`feld = wert` als benannter Parameter)
 * zählt nicht als Lesen.
 */
class DeadFieldTest {


    /**
     * Was gefüllt werden darf, ohne gelesen zu werden.
     *
     * Nur mit Grund — eine Ausnahme ohne Grund ist bloss ein leiser gestellter Fehler.
     */
    private val begruendeteAusnahmen = mapOf(
        // Steht in jeder gesicherten Datei und wird beim Einlesen aus dem rohen JSON
        // geprueft (ConfigTransfer), nicht ueber dieses Feld.
        "LauncherConfig.version" to "wird beim Einlesen aus dem JSON selbst gelesen",
    )

    private fun dateien(): List<File> =
        Quelltext.files()

    /** Alle Felder im Kopf einer `data class`, als "Klasse.Feld" mit ihrer Fundstelle. */
    private fun felder(): List<Triple<String, String, File>> {
        val gefunden = mutableListOf<Triple<String, String, File>>()
        dateien().forEach { datei ->
            var klasse: String? = null
            datei.readLines().forEach { zeile ->
                Regex("""^(?:@\w+\s+)?data class (\w+)""").find(zeile.trim())?.let {
                    // Eine einzeilige `data class X(val a: Int)` hat keinen mehrzeiligen
                    // Kopf - sonst gehoerte ihr alles, was danach im Rumpf steht.
                    klasse = if (zeile.trimEnd().endsWith(")")) null else it.groupValues[1]
                }
                // Der Kopf endet mit der schliessenden Klammer am Zeilenanfang; danach
                // beginnt der Rumpf oder die naechste Deklaration, und ein `val` dort
                // gehoert nicht mehr dazu.
                if (Regex("""^\)""").containsMatchIn(zeile) ||
                    Regex("""^(object|class|enum|sealed|fun|interface)\b""")
                        .containsMatchIn(zeile)
                ) {
                    klasse = null
                }
                val feld = Regex("""^ {4}val (\w+):""").find(zeile) ?: return@forEach
                klasse?.let { gefunden += Triple(it, feld.groupValues[1], datei) }
            }
        }
        return gefunden
    }

    /** Eine Zeile, die das Feld nur füllt: `feld = wert` als benannter Parameter. */
    private fun nurGefuellt(zeile: String, feld: String): Boolean =
        Regex("""^\s*$feld = """).containsMatchIn(zeile)

    private fun deklaration(zeile: String, feld: String): Boolean =
        Regex("""^\s*(?:val|var) $feld:""").containsMatchIn(zeile)

    @Test
    fun `jedes Feld einer data class wird auch gelesen`() {
        val zeilen = dateien().flatMap { it.readLines() }
        val tot = mutableListOf<String>()
        felder().forEach { (klasse, feld, _) ->
            val name = "$klasse.$feld"
            if (name in begruendeteAusnahmen) return@forEach
            val gelesen = zeilen.any { zeile ->
                Regex("""(^|[^\w.])$feld\b""").containsMatchIn(zeile) &&
                    !deklaration(zeile, feld) &&
                    !nurGefuellt(zeile, feld) ||
                    Regex("""\.$feld\b""").containsMatchIn(zeile)
            }
            if (!gelesen) tot += name
        }
        assertTrue(
            "Diese Felder werden gefuellt und nie gelesen:\n" + tot.joinToString("\n"),
            tot.isEmpty(),
        )
    }
}
