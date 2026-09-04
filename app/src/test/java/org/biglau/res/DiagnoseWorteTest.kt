package org.biglau.res

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein Wort für vier Zeilen passt in zweien.
 *
 * Die Diagnoseseite setzt hinter jede Zeile einen Wert. Für „nichts da" stand überall
 * derselbe String: „keine". Das passt zu „Standard-Nachrichten-App: keine" und war an den
 * beiden anderen Stellen falsch — am 04.09.2026 am Emulator gelesen:
 *
 * * **„Letzter Absturz: keine"** — der Absturz ist männlich, es heisst „keiner".
 * * **„Akku-Sparen: keine"** (wenn die Abfrage fehlschlägt) — dort ist nicht *nichts* da,
 *   sondern die **Auskunft** ausgeblieben. „unbekannt" sagt das.
 *
 * Dazu eine Ungleichheit im selben Abschnitt: der Aus-Fall hiess „Für BigLau aus", der
 * An-Fall nur „An". Wer die Zeile „Akku-Sparen: An" liest, denkt an den Sparmodus des
 * Telefons — der war am Emulator ausgeschaltet, während die Zeile „An" sagte. Gemeint ist
 * die Akku-Optimierung **für BigLau**; jetzt steht das in beiden Fällen.
 */
class DiagnoseWorteTest {

    private val quelle = Quelltext.ohneKommentare("org/biglau/settings/Diagnostics.kt")

    @Test
    fun `der letzte absturz hat sein eigenes wort`() {
        assertTrue(
            "Die Absturzzeile benutzt das allgemeine \"keine\" - das ist falsches Deutsch",
            "diag_crash_none" in quelle,
        )
    }

    @Test
    fun `eine ausgebliebene auskunft heisst nicht nichts`() {
        assertTrue(
            "Wenn die Akku-Abfrage fehlschlaegt, steht dort \"keine\" statt \"unbekannt\"",
            "diag_unknown" in quelle,
        )
    }

    @Test
    fun `beide seiten der akku-zeile sind gleich weit gefasst`() {
        listOf("values-de", "values").forEach { sprache ->
            val an = Quelltext.textWert("diag_battery_saving_on", sprache)
            val aus = Quelltext.textWert("diag_battery_saving_off", sprache)
            val nenntApp = { text: String -> "BigLau" in text }
            assertTrue(
                "Nur eine der beiden Antworten nennt BigLau (Sprache \"$sprache\"): " +
                    "an=\"$an\", aus=\"$aus\" - dann liest sich die andere als Aussage " +
                    "ueber das ganze Telefon",
                nenntApp(an) == nenntApp(aus),
            )
        }
    }
}
