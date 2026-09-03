package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Plan nennt keine Klasse, die es nicht gibt.
 *
 * `PLAN.md` setzt Typnamen in Backticks — 92 davon am 3.9.2026. Drei zeigten ins Leere:
 *
 * * **`HomeViewModel` und `HomeUiState`** (Abschnitt 2.3): eine ganze Architektur, samt
 *   Begründung („jede zusätzliche Recomposition-Quelle ist spürbar"), die nie gebaut wurde.
 *   Wer den Abschnitt liest, sucht danach im Quelltext und findet nichts.
 * * **`NotificationsListenerService`** — ein Buchstabe zu viel; die Android-Klasse heisst
 *   `NotificationListenerService`. Ein Tippfehler in einem API-Namen schickt den Leser in
 *   die Suchmaschine und von dort ins Nichts.
 *
 * Nicht jeder Name muss uns gehören: `FastOutSlowIn` kommt aus Compose, `BadgesProvider` ist
 * das, was das Original macht, und `LazyGrid` steht ausdrücklich als „nicht verwendet" da.
 * Die stehen unten, mit Grund.
 */
class NamenImPlanTest {

    /** Name → warum er im Plan stehen darf, obwohl er nicht im Quelltext vorkommt. */
    private val vonAussen = mapOf(
        "BadgesProvider" to
            "die Lösung des Originals für blinkende Kacheln - eigener Provider mit eigener " +
            "Berechtigung für die Schwester-Apps. Wir machen es ausdrücklich anders.",
        "FastOutSlowIn" to
            "eine Beschleunigungskurve aus Compose selbst, genannt als Vorgabe für die " +
            "Bewegung beim Screenwechsel.",
        "LazyGrid" to
            "steht als ausdrückliche Absage da: das Raster ist ein eigenes `Layout`, weil " +
            "spannende Zellen in einem LazyGrid nicht gehen.",
        "SPALTENxZEILEN" to
            "kein Typ, sondern ein Platzhalter in der Beschreibung dessen, was das Original " +
            "als Rastereingabe anbietet.",
    )

    private val plan = File("../PLAN.md").readText()

    private fun genannt(): List<String> =
        Regex("""`([A-Z][A-Za-z0-9]{3,})`""").findAll(plan).map { it.groupValues[1] }
            .distinct().sorted().toList()

    @Test
    fun `jeder genannte Typ kommt im Quelltext vor`() {
        val alles = (Quelltext.dateien() + Quelltext.testDateien()).joinToString("\n") { it.readText() }
        val geister = genannt().filterNot { it in vonAussen }.filterNot { it in alles }
        assertEquals(
            "PLAN.md nennt einen Namen, den es im Quelltext nicht gibt. Entweder ist er " +
                "veraltet, ein Tippfehler, oder er gehört jemand anderem - dann mit Grund " +
                "in die Liste in NamenImPlanTest.",
            emptyList<String>(),
            geister,
        )
    }

    @Test
    fun `jede Ausnahme nennt ihren Grund und steht auch im Plan`() {
        vonAussen.forEach { (name, grund) ->
            assertTrue("$name: Grund fehlt oder ist zu knapp", grund.length > 40)
            assertTrue("$name steht gar nicht mehr im Plan - dann kann die Ausnahme weg", name in plan)
        }
    }

    @Test
    fun `die Regel findet ueberhaupt Namen`() {
        assertTrue("nur ${genannt().size} Namen gefunden - sucht sie noch?", genannt().size >= 50)
    }
}
