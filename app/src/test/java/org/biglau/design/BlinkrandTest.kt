package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Rand fuer Neues ist duenn und ruhig.
 *
 * Er pulste einmal. Erst in einer Sekunde von duenn nach dick und zurueck, mit linearem
 * Verlauf; der Nutzer nannte das am Jelly 2 ablenkend, und es wurde auf 1400 ms mit weichem
 * Verlauf gestreckt. Am 04.09.2026, nachdem der Fokusrand fuer die Tastenbedienung dazukam,
 * hat er die Sache anders entschieden: **ein pulsendes Rechteck passt an einem
 * Tastentelefon besser zu "hier steht der Fokus" als zu "hier ist etwas Neues".** Fuer das
 * Neue reicht ein ganz duenner, ruhiger Rand.
 *
 * Das ist keine Nachlaessigkeit, sondern eine Umverteilung. Auf einem Bildschirm, auf dem
 * sich ein Ding bewegt, sieht man dieses Ding an. Auf einem Tastentelefon soll das der Platz
 * sein, den die Auswahltaste trifft, und nicht eine Kachel, die ohnehin ihre Zahl traegt.
 *
 * Verloren geht dabei nichts: die Zahl in der Ecke sagt weiterhin, wie viel wartet, und sie
 * sagt es genauer als jede Bewegung.
 */
class BlinkrandTest {

    private val kachel = Quelltext.withoutComments("org/biglau/ui/BigTile.kt")

    @Test
    fun `nichts an der Kachel pulst mehr`() {
        listOf("rememberInfiniteTransition", "infiniteRepeatable", "RepeatMode.Reverse").forEach {
            assertTrue(
                "$it steht wieder in BigTile. Der Rand fuer Neues soll ruhig sein; wer " +
                    "wieder eine Bewegung einbaut, nimmt sie dem Fokus weg.",
                it !in kachel,
            )
        }
    }

    @Test
    fun `der Rand fuer Neues ist duenn`() {
        val zahl = Regex("""BADGE_BORDER_DP = ([0-9.]+)f""").find(kachel)
            ?: throw AssertionError("BADGE_BORDER_DP steht nicht mehr im Quelltext")
        val dp = zahl.groupValues[1].toFloat()
        assertTrue("Der Rand fuer Neues ist mit $dp dp kein duenner Rand mehr", dp <= 3f)
    }

    @Test
    fun `die Zahl in der Ecke bleibt`() {
        // Sie ist jetzt die genaue Auskunft, nachdem die Bewegung weg ist.
        assertTrue(
            "Die Kachel zeigt die Anzahl nicht mehr - dann sagt nichts mehr, wie viel wartet",
            "badgeCount" in kachel,
        )
    }
}
