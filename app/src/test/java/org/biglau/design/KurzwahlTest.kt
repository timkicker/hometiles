package org.biglau.design

import org.biglau.Quelltext
import org.biglau.data.PhoneConfig
import org.biglau.phone.SpeedDial
import org.biglau.data.SpeedDialTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Hinweis sagt, was das Halten **jetzt** tut.
 *
 * Ein Langdruck auf eine leere Ziffer fuehrt ins Belegen; auf eine belegte waehlt er
 * **sofort**, ohne Rueckfrage. Ueber der Tastatur stand bis zum 04.09.2026 in beiden
 * Faellen derselbe Satz: „Halten fuer Kurzwahl". Im harmlosen Zustand derselbe wie im
 * gefaehrlichen - und der Satz nannte den Namen der Funktion, nicht die Folge der Geste.
 *
 * Das entscheidet die offene Frage nicht, ob die Kurzwahl vor dem Waehlen fragen soll (die
 * gehoert dem Nutzer, siehe `STATUS.md`). Es sorgt nur dafuer, dass er sie mit dem
 * richtigen Wissen entscheidet: die Anrufliste fragt vor dem Waehlen, die Kurzwahl nicht.
 */
class KurzwahlTest {

    private val waehler = Quelltext.withoutComments("org/biglau/phone/DialerActivity.kt")

    @Test
    fun `leer und belegt bekommen verschiedene Saetze`() {
        assertTrue(
            "Der Hinweis ueber der Tastatur ist wieder fest - dann steht im harmlosen " +
                "Zustand derselbe Satz wie im gefaehrlichen.",
            "R.string.dialer_speeddial_hint_assign" in waehler &&
                "R.string.dialer_speeddial_hint_call" in waehler,
        )
        assertTrue(
            "Der Hinweis fragt nicht nach, ob ueberhaupt eine Taste belegt ist.",
            "kurzwahlBelegt" in waehler,
        )
    }

    @Test
    fun `anyAssigned zaehlt nur die belegbaren Tasten`() {
        val leer = PhoneConfig()
        assertFalse("Ohne Kurzwahl darf nichts belegt sein", SpeedDial.anyAssigned(leer))

        val belegt = SpeedDial.assign(leer, '3', SpeedDialTarget(name = "Mama", number = "+430000"))
        assertTrue("Nach dem Belegen muss es auffallen", SpeedDial.anyAssigned(belegt))

        val wieder = SpeedDial.clear(belegt, '3')
        assertFalse("Nach dem Leeren nicht mehr", SpeedDial.anyAssigned(wieder))
    }

    /**
     * Und der alte, feste Satz ist wirklich weg - nicht nur ungenutzt liegengeblieben.
     */
    @Test
    fun `der feste Satz steht nirgends mehr`() {
        val uebrig = Quelltext.allTexts()
            .filter { "dialer_speeddial_hint\"" in it.readText() }
            .map { it.parentFile.name + "/" + it.name }
        assertEquals("Der alte Hinweis liegt noch herum", emptyList<String>(), uebrig)
    }
}
