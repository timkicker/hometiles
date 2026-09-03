package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.biglau.phone.probe.Probeanruf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Probeanruf bleibt eine Probe.
 *
 * **Diese Datei liegt in `src/testDebug`, nicht in `src/test`.** `src/test` wird fuer
 * beide Varianten uebersetzt, `src/debug` gibt es nur in einer - ein Test dort, der die
 * Probe beim Namen nennt, bricht den Release-Bau. Ein Test fuer etwas, das es nur im
 * Debug-Bau gibt, gehoert in den Testbereich, den es auch nur dort gibt.
 *
 * Er stellt sich selbst einen Anruf, damit der Anrufbildschirm durchgespielt werden kann,
 * ohne dass jemand angerufen wird. Zwei Dinge muessen dabei gelten, und beide sind hier
 * festgehalten: er darf nie eine Notrufnummer anfassen, und er darf nie in einem
 * ausgelieferten Programm landen.
 */
class ProbeanrufTest {

    private val debugWurzel = File("src/debug/java/org/biglau/phone/probe")

    @Test
    fun `mit einer Notrufnummer wird nicht geprobt`() {
        listOf("112", "911", "+112", "110", "999").forEach { nummer ->
            assertFalse(
                "Mit $nummer liesse sich proben - und aus einer Probe waere ein Notruf geworden.",
                Probeanruf.erlaubt(nummer),
            )
        }
    }

    @Test
    fun `die Probenummer gehoert niemandem`() {
        assertTrue("Die Probenummer ist nicht erlaubt", Probeanruf.erlaubt(Probeanruf.NUMMER))
        assertTrue(
            "Die Probenummer liegt nicht mehr im reservierten Bereich fuer Film und " +
                "Fernsehen (07700 900000-900999). Eine erfundene Nummer kann es " +
                "irgendwann wirklich geben.",
            Probeanruf.NUMMER.startsWith("+447700900"),
        )
    }

    @Test
    fun `ohne Nummer wird nicht geprobt`() {
        assertFalse(Probeanruf.erlaubt(""))
        assertFalse(Probeanruf.erlaubt("   "))
    }

    @Test
    fun `die Probe liegt nur im Debug-Bau`() {
        assertTrue(
            "Die Probe liegt nicht mehr in src/debug - dann kommt sie mit dem " +
                "ausgelieferten Programm mit.",
            debugWurzel.isDirectory && debugWurzel.listFiles().orEmpty().isNotEmpty(),
        )
        val imHauptbau = Quelltext.dateien()
            .filter { datei ->
                val text = datei.readText()
                "phone.probe" in text || "ProbeConnectionService" in text
            }
            .map { it.name }
        assertEquals(
            "Der Hauptbau kennt die Probe. Eine Moeglichkeit, einen Anruf vorzutaeuschen, " +
                "gehoert nicht in die Hand von jemandem, der die App bloss installiert hat.",
            emptyList<String>(),
            imHauptbau,
        )
    }

    /**
     * Und sie raeumt nur **ihre** Spur.
     *
     * Ein Loeschbefehl auf `CallLog.Calls.CONTENT_URI` ohne Bedingung leert die ganze
     * Anrufliste. Die Probe loescht dort, und sie tut es in einem Debug-Bau, der auf dem
     * Alltagstelefon des Nutzers laeuft - hier ist die Bedingung kein Detail, sondern der
     * Unterschied zwischen Aufraeumen und Datenverlust.
     */
    @Test
    fun `die Probe raeumt nur ihre eigene Spur`() {
        val quelle = File("src/debug/java/org/biglau/phone/probe/ProbeConnectionService.kt")
        assertTrue("Die ConnectionService gibt es nicht mehr", quelle.isFile)
        val text = quelle.readText()
        assertTrue(
            "Die Probe raeumt ihre Spur nicht mehr aus der Anrufliste - dann sammeln sich " +
                "die Proben oben in der Liste des Nutzers.",
            "CallLog.Calls.CONTENT_URI" in text,
        )
        val loeschen = text.substringAfter("contentResolver.delete(").substringBefore(")")
        assertTrue(
            "Der Loeschbefehl nennt die Probenummer nicht: $loeschen - ohne Bedingung " +
                "leert er die ganze Anrufliste.",
            "Probeanruf.NUMMER" in loeschen && "LIKE" in loeschen,
        )
        assertFalse(
            "Der Loeschbefehl hat keine Bedingung.",
            Regex("""CONTENT_URI,\s*null,\s*null""").containsMatchIn(text),
        )
    }

    @Test
    fun `die ConnectionService steht nur im Debug-Manifest`() {
        val debug = File("src/debug/AndroidManifest.xml")
        assertTrue("Es gibt kein Debug-Manifest mehr", debug.isFile)
        assertTrue(
            "Die ConnectionService steht nicht mehr im Debug-Manifest",
            "ProbeConnectionService" in debug.readText(),
        )
        assertFalse(
            "Die ConnectionService steht im Haupt-Manifest - damit waere sie in jedem Bau.",
            "probe" in File("src/main/AndroidManifest.xml").readText(),
        )
    }
}
