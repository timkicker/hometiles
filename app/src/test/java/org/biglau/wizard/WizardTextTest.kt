package org.biglau.wizard

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Im Assistenten wächst der Text mit — dort wird die Größe ja gerade eingestellt.
 *
 * Schritt 2 heißt „Wählen Sie, was sich für Sie gut liest. Die Änderung wirkt sofort hier."
 * Der erklärende Satz daneben machte die Änderung **nicht** mit: er hing an `dpSp` und blieb
 * gleich, während Überschrift und Auswahlzeilen wuchsen. Ein Satz, der die Wirkung zeigen
 * soll und sie nicht zeigt, ist ein schlechter Beweis.
 *
 * Damit der wachsende Text die Knöpfe nie hinausschiebt, blättert er in seinem eigenen Feld;
 * die Knöpfe stehen darunter fest — dieselbe Aufteilung, die die Auswahlschritte schon
 * hatten. Bei 200 % am Emulator nachgesehen: „Los geht's" bleibt unten stehen.
 */
class WizardTextTest {

    private val quelle = Quelltext.datei("org/biglau/wizard/WizardActivity.kt").readText()

    @Test
    fun `der Erklaertext folgt der eingestellten Groesse`() {
        assertTrue("bigSp fehlt im Assistenten", "bigSp(" in quelle)
        assertTrue(
            "Der Erklaertext haengt noch an dpSp - er wuerde die Aenderung nicht zeigen",
            "fontSize = dpSp(17f)" !in quelle && "fontSize = dpSp(16f)" !in quelle,
        )
    }

    @Test
    fun `der Text blaettert, damit die Knoepfe stehenbleiben`() {
        assertTrue("kein eigenes Blaetterfeld fuer den Text", "verticalScroll" in quelle)
        assertTrue("das Feld gibt keinen Platz ab", "weight(1f, fill = false)" in quelle)
    }
}
