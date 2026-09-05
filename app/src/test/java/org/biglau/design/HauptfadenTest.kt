package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kein Bildschirm liest von der Platte, waehrend er gezeichnet wird.
 *
 * Jedes Verzeichnis im Programm liest ueber `withContext(Dispatchers.IO)` - die
 * Nachrichten, die Kontakte, die Anrufliste, die Konfiguration. `ImportActivity` war die
 * Ausnahme: sie rief `contentResolver.openInputStream` direkt in `onCreate`.
 *
 * Bei einer Datei auf dem Geraet faellt das nicht auf. Der Weg aber, den ihr eigener
 * Klassenkopf beschreibt - eine Sicherung aus einer Cloud-App oder einem Mailanhang -,
 * geht ueber einen fremden Anbieter, und der holt sie unter Umstaenden erst aus dem Netz.
 * Dann steht der Bildschirm, und Android erklaert die App fuer haengend. Ausgerechnet beim
 * Wechsel auf ein neues Telefon, den man genau einmal geht.
 *
 * Die Regel gilt fuer die Oberflaeche, nicht fuer die Verzeichnisse: die duerfen lesen, sie
 * tun es ja im richtigen Faden.
 */
class HauptfadenTest {

    /** Zugriffe, die auf die Platte oder ins Netz gehen koennen. */
    private val langsam = listOf(
        "contentResolver.openInputStream(",
        "contentResolver.openOutputStream(",
        "contentResolver.query(",
    )

    @Test
    fun `keine Activity greift ausserhalb eines Effekts auf einen Anbieter zu`() {
        val stellen = Quelltext.dateien()
            .filter { it.name.endsWith("Activity.kt") }
            .flatMap { datei ->
                val zeilen = datei.readLines()
                zeilen.withIndex()
                    .filter { (_, z) ->
                        val nackt = z.trim()
                        langsam.any { it in nackt } && !Quelltext.istKommentarzeile(z)
                    }
                    .filterNot { (i, _) ->
                        // Im richtigen Faden, oder in einer Funktion, die selbst nur aus
                        // einem Effekt gerufen wird - beides erkennt man daran, dass in den
                        // zwanzig Zeilen darueber `Dispatchers.IO` steht.
                        zeilen.subList(maxOf(0, i - 20), i).any { "Dispatchers.IO" in it }
                    }
                    .map { (i, _) -> "${datei.name}:${i + 1}" }
            }
        assertEquals(
            "Hier liest ein Bildschirm von einem Anbieter, ohne den Faden zu wechseln. " +
                "Bei einer Datei aus einer Cloud-App ist das Netzverkehr im Hauptthread - " +
                "der Bildschirm steht, bis Android die App fuer haengend erklaert.",
            emptyList<String>(),
            stellen,
        )
    }

    /** Und wer laedt, sagt es. */
    @Test
    fun `der Import sagt, dass er liest`() {
        val quelle = Quelltext.datei("org/biglau/settings/ImportActivity.kt").readText()
        assertTrue(
            "Der Import liest ohne Ladezustand - dann steht der Bildschirm leer da, und " +
                "wer nichts sieht, tippt noch einmal.",
            "R.string.transfer_reading" in quelle && "loading" in quelle,
        )
    }
}
