package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Zahl der Kanten im Plan ist nachgezaehlt, nicht erinnert.
 *
 * `PLAN.md` 2.1 traegt eine Tabelle der Kanten zwischen den kuenftigen `feature:*`-Modulen.
 * Sie ist das Mass fuer den Modulschnitt: solange dort Kanten stehen, geht er nicht.
 *
 * Am 04.09.2026 nachgemessen und dabei gemerkt, dass die Tabelle veraltet war. Sie nannte
 * `settings → home` (`WidgetHostController`) und `settings → sos` - beides gab es nicht
 * mehr: die eine Datei war nach `core:system` gezogen, der andere Bereich heisst inzwischen
 * `toggles`. Zwei von vier Zeilen waren falsch, und niemand hat es gemerkt, weil eine
 * Tabelle nicht faellt.
 *
 * Jetzt faellt sie. Dieselbe Uebung wie bei den 56 Icons in [org.biglau.ui.IconZahlTest]:
 * eine Zahl, die ein Argument traegt, muss stimmen.
 */
class KantenZahlTest {

    /**
     * Ohne Backticks gelesen.
     *
     * Der Plan setzt Namen in `…`, die Messung liefert sie nackt - die erste Fassung suchte
     * `. → sms` und fand `` `.` → sms ``. Eine Regel, die an der Auszeichnung scheitert,
     * prueft die Auszeichnung.
     */
    private val plan = File("../PLAN.md").readText().replace("`", "")

    /**
     * `MainActivity` bleibt aussen vor.
     *
     * Sie ist der Startbildschirm und ruft jede Kachel-Aktion auf - sie kennt notwendig
     * jeden Bereich. Waere sie dabei, zaehlte man nicht den Modulschnitt, sondern sie.
     */
    private fun kanten() = Bereiche.kanten(ohne = setOf("MainActivity.kt"))

    @Test
    fun `der Plan nennt die Zahl, die gemessen wird`() {
        // Die Zahl steht **nur** im Plan, nicht auch hier. Die erste Fassung schrieb sie
        // an beiden Stellen hin; eine Stunde spaeter fiel eine Kante weg, und ich musste
        // sie zweimal aendern. Eine Zahl, die zweimal dasteht, ist eine Zahl zu viel.
        val imPlan = Regex("""Übrig sind (\d+) Kanten""").find(plan)?.groupValues?.get(1)
        assertTrue(
            "PLAN.md nennt die Zahl der uebrigen Kanten nicht mehr - dann steht die " +
                "Tabelle ohne Mass da.",
            imPlan != null,
        )
        assertEquals(
            "Die Zahl im Plan stimmt nicht mehr. Entweder ist eine Kante dazugekommen " +
                "(dann gehoert sie mit Urteil in die Tabelle) oder eine ist weg (dann ist " +
                "der Modulschnitt naeher, als der Plan sagt).",
            imPlan!!.toInt(),
            kanten().size,
        )
    }

    @Test
    fun `jede gemessene Kante steht auch in der Tabelle`() {
        val fehlt = kanten().keys
            .map { (von, nach) -> "$von → $nach" }
            .filterNot { it in plan }
        assertEquals(
            "Diese Kante wird gemessen, steht aber in keiner Zeile der Tabelle. Eine " +
                "Kante ohne Urteil ist eine, ueber die niemand entschieden hat.",
            emptyList<String>(),
            fehlt,
        )
    }
}
