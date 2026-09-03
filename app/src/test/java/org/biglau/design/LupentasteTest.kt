package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Lupentaste ist kein Seiteneingang.
 *
 * In der App-Liste steht seit langem der Satz: „Die Sperre gilt auch hier, nicht nur auf
 * den Kacheln - sonst waere sie ueber die Liste in einem Tipp zu umgehen." Die Zeilen der
 * Liste hielten sich daran und gingen ueber `open`, das die PIN abfragt. Die Lupentaste
 * daneben rief `launch` und startete direkt - eine gesperrte App war also ueber das
 * Suchfeld in einem Tastendruck zu oeffnen. Der Kommentar stand da, die Ausnahme auch.
 *
 * Dazu die kleinere Haelfte derselben Stelle: der Zaehler ueber dem Suchfeld zaehlte nur
 * die Apps, obwohl die Einstellungszeile mit in der Liste steht. Bei „big" standen zwei
 * Zeilen da und darueber „1 Treffer" - und die Lupentaste oeffnete die eine, ohne dass zu
 * sehen war, welche.
 */
class LupentasteTest {

    private val datei = Quelltext.datei("org/biglau/apps/AppDrawerActivity.kt")
    private val zeilen = datei.readLines()
    private val quelle = datei.readText()

    @Test
    fun `keine App startet, ohne dass die Sperre gefragt wurde`() {
        val ohnePruefung = zeilen.withIndex()
            .filter { (_, z) ->
                val nackt = z.trim()
                Regex("""(^|[^.\w])launch\(""").containsMatchIn(nackt) &&
                    !nackt.startsWith("//") &&
                    !nackt.startsWith("fun launch(") &&
                    "repository.launch(" !in nackt
            }
            .filter { (i, _) ->
                // Der Blick auf die Sperre darf ueber dem Start stehen - entweder als
                // Abfrage (`open`) oder als das Ja danach (die eingegebene PIN).
                zeilen.subList(maxOf(0, i - 12), i)
                    .none { "AppLock.needsPin" in it || "onAccept" in it }
            }
            .map { it.index + 1 }
        assertEquals(
            "Hier startet eine App, ohne dass die App-Sperre gefragt wurde. Damit ist sie " +
                "ueber diesen Weg zu umgehen - und genau das begruendet der Kommentar " +
                "ueber `open` als Grund, warum es sie gibt.",
            emptyList<Int>(),
            ohnePruefung,
        )
    }

    @Test
    fun `der Zaehler meint dieselbe Menge wie die Liste`() {
        val zaehler = Regex("""R\.plurals\.search_matches,\s*(\w+)""")
            .find(quelle)?.groupValues?.get(1)
        assertNotNull("Der Trefferzaehler ist weg - wandert die Regel mit?", zaehler)

        // Woran haengt die Einstellungszeile? Das steht ueber ihr, nicht in dieser Regel.
        val beiZeile = zeilen.indexOfFirst { "label = einstellungen" in it }
        assertTrue("Die Einstellungszeile gibt es nicht mehr", beiZeile > 0)
        val bedingung = zeilen.subList(maxOf(0, beiZeile - 8), beiZeile)
            .reversed()
            .firstNotNullOfOrNull { Regex("""if \((\w+)\)""").find(it)?.groupValues?.get(1) }
        assertNotNull("Die Einstellungszeile haengt an keiner Bedingung mehr", bedingung)

        val erklaerung = zeilen.firstOrNull { it.trim().startsWith("val $zaehler ") }
        assertNotNull("$zaehler wird nirgends erklaert", erklaerung)
        assertTrue(
            "Der Zaehler ($zaehler) weiss nichts von der Einstellungszeile ($bedingung). " +
                "Dann steht ueber einer Liste mit zwei Zeilen die Zahl 1.",
            bedingung!! in erklaerung!!,
        )
    }

    @Test
    fun `die Lupentaste rechnet auf derselben Menge wie der Zaehler`() {
        val zaehler = Regex("""R\.plurals\.search_matches,\s*(\w+)""")
            .find(quelle)!!.groupValues[1]
        val beiSuche = zeilen.indexOfFirst { "onSearch = " in it }
        assertTrue("onSearch gibt es nicht mehr", beiSuche > 0)
        val gerufen = Regex("""(\w+)[?.]""").find(zeilen[beiSuche].substringAfter("onSearch = "))
            ?.groupValues?.get(1)
        assertNotNull("onSearch ruft nichts Benanntes", gerufen)

        val beiErklaerung = zeilen.indexOfFirst { it.trim().startsWith("val $gerufen") }
        assertTrue("$gerufen wird nirgends erklaert", beiErklaerung > 0)
        assertTrue(
            "Die Lupentaste ($gerufen) rechnet nicht mit dem Zaehler ($zaehler). Dann " +
                "verspricht der Satz daneben etwas Eindeutiges ueber einen zweideutigen " +
                "Zustand: zwei Zeilen, und die Taste oeffnet eine davon.",
            zeilen.subList(beiErklaerung, minOf(zeilen.size, beiErklaerung + 6))
                .any { zaehler in it },
        )
    }
}
