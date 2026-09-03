package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine Meldung, die einen Weg nennt, zeigt ihn auch.
 *
 * Zwei Kacheln können ins Leere führen: eine deinstallierte App und eine verschwundene
 * Verknüpfung. Beide Meldungen sagen denselben Satz — „Kachel neu belegen". Seit dem
 * 3.9.2026 früh öffnete **nur die App-Kachel** danach den Editor; die Verknüpfung liess den
 * Rat im Raum stehen. Ein halber Ausweg ist keiner, und er ist schlechter als gar keiner:
 * beim zweiten Mal glaubt man dem Satz nicht mehr.
 *
 * Und der dritte Fall zeigt, warum ein Text zu seinem Ort passen muss: dieselbe Meldung
 * stand in der **App-Liste**, wo es gar keine Kachel gibt. „Kachel neu belegen" war dort ein
 * Rat ins Leere. Sie hat jetzt einen eigenen Satz — und die Liste lädt sich neu, damit der
 * tote Eintrag verschwindet, statt beim nächsten Tipp wieder nichts zu tun.
 */
class AuswegTest {

    private val start = Quelltext.datei("org/biglau/MainActivity.kt").readText()

    /**
     * **Jedes** Vorkommen, nicht das erste.
     *
     * Die erste Fassung prüfte mit `substringAfter` nur den ersten Treffer — und übersah
     * damit, dass `app_gone` an **drei** Stellen steht: beim Tipp auf die Kachel, in der
     * App-Liste, und nach dem Entsperren einer gesperrten App. Ausgerechnet die dritte hatte
     * den Ausweg nicht, weil der gemerkte Zustand nur die App enthielt und nicht die Kachel.
     */
    @Test
    fun `beide toten Kacheln fuehren in den Editor`() {
        listOf("R.string.app_gone", "R.string.shortcut_gone").forEach { meldung ->
            var ab = 0
            var gefunden = 0
            while (true) {
                val stelle = start.indexOf(meldung, ab)
                if (stelle < 0) break
                gefunden++
                assertTrue(
                    "Auf $meldung (Vorkommen $gefunden in MainActivity) folgt kein " +
                        "TileEditorActivity.intent - die Meldung rät zum Neubelegen und " +
                        "lässt den Rat im Raum stehen.",
                    "TileEditorActivity.intent(" in start.substring(stelle).take(600),
                )
                ab = stelle + meldung.length
            }
            assertTrue("$meldung kommt in MainActivity gar nicht mehr vor", gefunden > 0)
        }
    }

    @Test
    fun `die App-Liste rät nicht zu einer Kachel, die es dort nicht gibt`() {
        val liste = Quelltext.datei("org/biglau/apps/AppDrawerActivity.kt").readText()
        assertEquals(
            "Die App-Liste zeigt `app_gone` („Kachel neu belegen\") - dort gibt es keine " +
                "Kachel. `app_gone_list` nehmen.",
            false,
            "R.string.app_gone)" in liste,
        )
        assertTrue("app_gone_list wird nicht benutzt", "R.string.app_gone_list" in liste)
        assertTrue(
            "Nach der Meldung wird die Liste nicht neu geladen - der tote Eintrag bliebe " +
                "stehen und täte beim nächsten Tipp wieder nichts.",
            "loadApps()" in liste.substringAfter("R.string.app_gone_list").take(200),
        )
    }

    /** Und die beiden Sätze sagen dasselbe, in beiden Sprachen. */
    @Test
    fun `der Rat zum Neubelegen steht in beiden Meldungen`() {
        listOf("values" to "Assign the tile again", "values-de" to "Kachel neu belegen")
            .forEach { (sprache, satz) ->
                val texte = Quelltext.texte(sprache).first().readText()
                listOf("app_gone", "shortcut_gone").forEach { name ->
                    val wert = Regex("""<string name="$name">(.*?)</string>""").find(texte)
                        ?.groupValues?.get(1)
                        ?: throw AssertionError("$sprache: $name fehlt")
                    assertTrue("$sprache/$name rät nicht zum Neubelegen: $wert", satz in wert)
                }
            }
    }
}
