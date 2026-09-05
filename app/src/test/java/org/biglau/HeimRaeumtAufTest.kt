package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Heim-Taste räumt alles weg, was über dem Startbildschirm liegt.
 *
 * Fünf Überlagerungen gibt es: der offene Ordner, die grosse Kachelbeschriftung, die Frage
 * „anrufen oder schreiben?", die PIN-Sperre vor einer gesperrten App und die Erklärung zur
 * Empfangs-Berechtigung. Bis zum 04.09.2026 räumte `onNewIntent` **nur** den Ordner weg —
 * mit einer Begründung, die für alle fünf gilt: „wer heim tippt, will nicht weiter darin
 * stehen."
 *
 * Am Emulator nachgestellt: die Frage „anrufen oder schreiben?" geöffnet, Heim gedrückt —
 * die Frage stand weiter da. Dasselbe mit der grossen Beschriftung. Eine Frage, die eine
 * Heim-Taste überlebt, ist ein Riegel.
 *
 * Die Regel zählt die Zustände, nicht die Wörter: wer eine sechste Überlagerung einführt,
 * muss sie hier eintragen, und dabei fällt die Frage an, ob die Heim-Taste sie wegräumt.
 */
class HeimRaeumtAufTest {

    private val haupt = Quelltext.ohneKommentare("org/biglau/MainActivity.kt")

    /** Zustand → was er über den Startbildschirm legt. */
    private val ueberlagerungen = mapOf(
        "openFolder" to "der offene Ordner",
        "popupLabelState" to "die grosse Kachelbeschriftung",
        "contactChoice" to "anrufen oder schreiben?",
        "lockedApp" to "die PIN-Sperre vor einer gesperrten App",
        "phoneStateAsked" to "die Erklaerung zur Empfangs-Berechtigung",
        // Am 04.09.2026 dazugekommen, mit der Menuetaste aus PLAN.md 10.3.4. Die Regel hat
        // sie sofort gemeldet, wie ihr eigener Kommentar es angekuendigt hat.
        "kachelMenue" to "die Liste der Menuetaste",
    )

    private val raeumen = Quelltext.ausschnitt(
        haupt,
        von = "private fun closeOverlays() {",
        bis = "\n    }",
    )

    @Test
    fun `heim raeumt jede ueberlagerung weg`() {
        val vergessen = ueberlagerungen.filterKeys { it !in raeumen }
        assertEquals(
            "closeOverlays() laesst etwas stehen: " +
                vergessen.values.joinToString(", ") +
                ". Wer heim tippt, will den Startbildschirm.",
            emptyMap<String, String>(),
            vergessen,
        )
    }

    @Test
    fun `die heim-taste ruft das auch auf`() {
        val neuerIntent = Quelltext.ausschnitt(
            haupt,
            von = "override fun onNewIntent(",
            bis = "\n    }",
        )
        assertTrue(
            "onNewIntent raeumt die Ueberlagerungen nicht: $neuerIntent",
            "closeOverlays()" in neuerIntent,
        )
    }

    @Test
    fun `keine ueberlagerung raeumt sich nur einzeln weg`() {
        // Der Zaehler ist die eigentliche Regel: so viele Zustaende, so viele Zeilen.
        // Eine weitere Ueberlagerung faellt hier auf, bevor sie eine Heim-Taste ueberlebt.
        // Am 04.09.2026 hat sie genau das getan, als die Menuetaste die sechste brachte.
        val zeilen = raeumen.lines().count { it.contains(".value") }
        assertEquals(
            "closeOverlays() raeumt $zeilen Zustaende weg, erwartet werden " +
                "${ueberlagerungen.size}: ${ueberlagerungen.values.joinToString(", ")}",
            ueberlagerungen.size,
            zeilen,
        )
    }
}
