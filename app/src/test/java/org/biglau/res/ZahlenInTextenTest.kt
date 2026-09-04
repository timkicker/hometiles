package org.biglau.res

import org.biglau.Quelltext
import org.biglau.phone.SpeedDial
import org.biglau.security.Pin
import org.biglau.ui.EMERGENCY_HOLD_MILLIS
import org.biglau.ui.theme.FreeTileColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine Zahl im Text ist ein Versprechen.
 *
 * Vier Texte nennen eine Zahl, die anderswo im Quelltext steht: „24 Toene zur Wahl",
 * „30 Sekunden halten", „4 bis 8 Ziffern", „2-9 halten". Alle vier stimmen heute. Keine
 * einzige haengt an ihrer Quelle - wer `HUE_COUNT` auf 18 setzt, aendert die Farben und
 * laesst den Satz stehen.
 *
 * Dieselbe Uebung wie bei den 56 Icons in [org.biglau.ui.IconZahlTest] und den Kanten in
 * [org.biglau.KantenZahlTest]: eine Zahl, die ein Argument oder eine Anleitung traegt, muss
 * nachgezaehlt sein. Am 04.09.2026 aufgeschrieben, waehrend die Farbwahl durchgesehen wurde.
 *
 * Die Regel prueft **beide Sprachen** - eine Zahl, die nur im Deutschen mitwandert, ist
 * dieselbe Falle mit halber Reichweite.
 */
class ZahlenInTextenTest {

    private fun text(name: String, sprache: String): String =
        Quelltext.texte(sprache)
            .firstNotNullOfOrNull { datei ->
                Regex("""<string name="$name">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
                    .find(datei.readText())?.groupValues?.get(1)
            }
            ?: throw AssertionError("$sprache: $name fehlt")

    private fun beideSprachen(name: String, zahl: String, wofuer: String) {
        listOf("values", "values-de").forEach { sprache ->
            val wert = text(name, sprache)
            assertTrue(
                "$sprache/$name nennt nicht mehr $zahl ($wofuer): $wert",
                zahl in wert,
            )
        }
    }

    @Test
    fun `die Zahl der freien Farbtoene stimmt`() {
        beideSprachen(
            "editor_color_free_hint",
            FreeTileColor.HUE_COUNT.toString(),
            "FreeTileColor.HUE_COUNT",
        )
    }

    @Test
    fun `die Haltedauer des Notausstiegs stimmt`() {
        val sekunden = (EMERGENCY_HOLD_MILLIS / 1000).toString()
        listOf("security_explainer", "security_forgot", "editor_locked_hint").forEach {
            beideSprachen(it, sekunden, "EMERGENCY_HOLD_MILLIS")
        }
    }

    @Test
    fun `die Laenge der PIN stimmt`() {
        listOf("security_new_pin", "security_pin_rules").forEach { name ->
            beideSprachen(name, Pin.MIN_LENGTH.toString(), "Pin.MIN_LENGTH")
            beideSprachen(name, Pin.MAX_LENGTH.toString(), "Pin.MAX_LENGTH")
        }
    }

    @Test
    fun `die belegbaren Kurzwahltasten stimmen`() {
        val erste = SpeedDial.ASSIGNABLE.first().toString()
        val letzte = SpeedDial.ASSIGNABLE.last().toString()
        beideSprachen("dialer_speeddial_hint_assign", erste, "SpeedDial.ASSIGNABLE.first")
        beideSprachen("dialer_speeddial_hint_assign", letzte, "SpeedDial.ASSIGNABLE.last")
    }

    /** Und die Regel findet ueberhaupt etwas - sonst prueft sie Luft. */
    @Test
    fun `es sind wirklich vier verschiedene Zahlen`() {
        assertEquals(
            "Eine der vier Quellen ist weg - dann gehoert die Regel nachgezogen.",
            listOf(24, 30, 4, 8),
            listOf(
                FreeTileColor.HUE_COUNT,
                (EMERGENCY_HOLD_MILLIS / 1000).toInt(),
                Pin.MIN_LENGTH,
                Pin.MAX_LENGTH,
            ),
        )
    }
}
