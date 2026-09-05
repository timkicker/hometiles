package org.biglau.toggles

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Wo „keine Notfallkontakte" steht, steht auch der Weg dorthin.
 *
 * Der Notruf-Bildschirm sagt an zwei Stellen, dass keine Kontakte hinterlegt sind: bevor er
 * losläuft, und nach der **Probe**. Die erste bot seit Langem einen Knopf in die
 * Notruf-Einstellungen an — „der Weg dorthin statt der Wegbeschreibung", wie der Kommentar
 * daneben sagt. Die zweite bot ihn nicht, und das ist die Stelle, an der jemand den Notruf
 * gerade einrichtet und ausprobiert.
 *
 * Geprüft wird deshalb: **beide** Zweige, die den Satz zeigen, bieten den Sprung an.
 */
class SosSetupReachableTest {

    private val quelle = Quelltext.withoutComments("org/biglau/toggles/SosActivity.kt")

    @Test
    fun `zu jedem Hinweis auf fehlende Kontakte gehoert der Sprung in die Einstellungen`() {
        val hinweise = Regex("""R\.string\.sos_not_configured""").findAll(quelle).count()
        // Die Zeile steht einmal als Funktion da und wird zweimal gerufen; gezählt werden
        // die Aufrufe, nicht die Erklärung.
        val wege = Regex("""(?<!fun )NotrufEinrichtenZeile\(\)""").findAll(quelle).count()
        assertEquals(
            "Es gibt $hinweise Hinweise auf fehlende Notfallkontakte, aber $wege Wege in " +
                "die Einstellungen. Wer den Satz liest, soll ihn nicht nur lesen.",
            hinweise,
            wege,
        )
    }
}
