package org.biglau.ui

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Plan nennt die Zahl der mitgelieferten Icons — und zwar die echte.
 *
 * `PLAN.md` 1.1 begründet eine Entscheidung damit: das Original hat nur ~11 eingebaute Icons
 * und lädt den Rest aus Theme-APKs nach; wir liefern alles mit, damit niemand etwas
 * installieren muss. Bis zum 3.9.2026 stand dort „~120 Vektor-Icons". Gebaut sind **56**.
 *
 * Die Zahl trägt das Argument, also muss sie stimmen. Sie steht jetzt im Plan, und diese
 * Regel zählt sie nach: Zahl im Plan gegen Einträge in [IconCatalogue.GROUPS].
 */
class IconZahlTest {

    private val plan = File("../PLAN.md").readText()

    private fun katalog(): List<String> {
        val quelle = Quelltext.datei("org/biglau/ui/IconCatalogue.kt").readText()
        val ab = Quelltext.ausschnitt(quelle, "val GROUPS")
        return Regex("""listOf\(([^)]*)\)""").findAll(ab)
            .flatMap { Regex(""""([A-Za-z]+)"""").findAll(it.groupValues[1]) }
            .map { it.groupValues[1] }
            .toList()
    }

    @Test
    fun `die Zahl im Plan stimmt mit dem Katalog ueberein`() {
        val genannt = Regex("""Gebaut sind \*\*(\d+)\*\* in (\w+) Gruppen""").find(plan)
            ?: throw AssertionError("Die Icon-Zeile in PLAN.md 1.1 nennt keine Zahl mehr")
        assertEquals(
            "PLAN.md nennt eine andere Zahl als IconCatalogue.GROUPS hergibt. Die Zahl " +
                "trägt dort ein Argument (keine Theme-APKs nötig) - sie muss stimmen.",
            katalog().size,
            genannt.groupValues[1].toInt(),
        )
    }

    @Test
    fun `kein Icon steht zweimal im Katalog`() {
        val alle = katalog()
        val doppelt = alle.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.sorted()
        assertEquals(
            "Ein Icon steht in zwei Gruppen. Wer es sucht, findet es zweimal und weiss " +
                "nicht, ob es dasselbe ist.",
            emptyList<String>(),
            doppelt,
        )
    }

    @Test
    fun `es sind deutlich mehr als die elf des Originals`() {
        assertTrue(
            "Nur ${katalog().size} Icons - das Argument aus PLAN.md 1.1 (das Original hat " +
                "~11 und laedt nach) traegt dann nicht mehr.",
            katalog().size >= 40,
        )
    }
}
