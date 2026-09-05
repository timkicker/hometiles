package org.biglau.design

import org.biglau.Quelltext
import java.io.File
import org.biglau.data.ThemeName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Das README zählt die Themen auf - und die Zahl altert.
 *
 * Bis zum 3.9.2026 stand dort „Dunkel als Hauptthema, dazu ein helles und ein
 * Kontrastthema" - drei. `ThemeName` kennt vier: das vierte ist „dem Telefon folgen", und
 * genau das ist die Einstellung, die man erwartet und im Text nicht fand.
 *
 * Diese Regel prüft nicht die Formulierung, sondern hält einen **Merkposten**: kommt ein
 * Thema dazu oder fällt eines weg, fällt sie um und erinnert daran, dass es im README einen
 * Satz gibt, der davon handelt. Ohne so etwas altert eine Aufzählung lautlos - dieselbe
 * Klasse Fehler wie die Prüfsumme ohne Commit.
 */
class ThemenImReadmeTest {

    private val readme = File("../README.md").readText()

    @Test
    fun `es sind weiterhin vier Themen`() {
        assertEquals(
            "Die Zahl der Themen hat sich geändert. Im README steht ein Satz, der sie " +
                "aufzählt (Abschnitt „Gestaltung\") - der will dann mitgezogen werden.",
            4,
            ThemeName.entries.size,
        )
    }

    @Test
    fun `der Satz nennt alle vier`() {
        val stelle = Quelltext.cut(readme, "## Gestaltung", "##")
        listOf("unkel", "hell", "Kontrast", "Telefon folgen").forEach { wort ->
            assertTrue("Im Abschnitt Gestaltung fehlt: $wort", wort in stelle)
        }
    }
}
