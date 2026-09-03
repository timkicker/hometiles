package org.biglau.apps

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aus der App-Liste kommt man immer in die Einstellungen.
 *
 * Am 3.9.2026 am Gerät des Nutzers nachgesehen, weil ich die Diagnoseseite aufschlagen
 * wollte und den Weg nicht fand: acht belegte Kacheln, keine davon die Einstellungen,
 * Wischen zwischen den Screens aus, kein freies Feld zum Langdrücken. Die App-Liste bot
 * nichts an — die Zeile dorthin gab es nur, **wenn** Apps ausgeblendet waren.
 *
 * Es blieb genau ein Weg: eine vorhandene Kachel lange drücken und umbelegen. Also eine App
 * aufgeben, und erst einmal darauf kommen. Für einen Startbildschirm, der für Menschen mit
 * schlechten Augen und zittrigen Händen gebaut ist, ist das kein Weg.
 *
 * `PLAN.md` 5 sagt es als Regel: keine Phase darf das Gerät in einen Zustand bringen, aus
 * dem man ohne Rechner nicht mehr herauskommt. Streng genommen kam man heraus — aber nur
 * über eine Tür, die niemand als Tür erkennt.
 */
class SettingsReachableFromDrawerTest {

    private val datei = Quelltext.datei("org/biglau/apps/AppDrawerActivity.kt")
    private val liste = datei.readText()
    private val zeilen = datei.readLines()

    @Test
    fun `die app-liste bietet den weg in die einstellungen an`() {
        assertTrue(
            "Die App-Liste ruft SettingsLink.toRoot nicht mehr. Wer seine " +
                "Einstellungs-Kachel weggibt, hat dann keinen Weg mehr dorthin.",
            "SettingsLink.toRoot" in liste,
        )
    }

    /**
     * Die Bedingung davor darf **nur** von der Suche handeln.
     *
     * Die erste Fassung dieser Regel verlangte wörtlich `query.isEmpty()` - sie schrieb
     * damit die Umsetzung fest statt der Absicht. Am 3.9.2026 wurde die Zeile verbessert
     * (sie steht jetzt auch da, wenn die **Suche sie trifft**), und die Regel fiel um,
     * obwohl die Sache besser geworden war. Jetzt prüft sie, worum es geht: die Bedingung
     * darf von `query` handeln und von sonst nichts - nicht von ausgeblendeten Apps, nicht
     * von einer Rolle, nicht von etwas, das die meisten Leute nie haben.
     */
    @Test
    fun `der weg haengt an keiner bedingung ausser der suche`() {
        // Gefragt ist die **Zeile**, nicht irgendein Aufruf von `SettingsLink.toRoot` -
        // seit dem 03.09.2026 gibt es einen zweiten (die Lupentaste), und `indexOf` fand
        // ihn zuerst. Damit las die Regel eine Bedingung, die gar nicht zur Zeile gehoerte.
        val beiZeile = zeilen.indexOfFirst { "label = einstellungen" in it }
        assertTrue("Die Zeile in die Einstellungen gibt es nicht mehr", beiZeile > 0)
        val bedingung = zeilen.subList(maxOf(0, beiZeile - 8), beiZeile)
            .reversed()
            .firstNotNullOfOrNull { Regex("""if \((.+?)\) \{""").find(it)?.groupValues?.get(1) }
        assertTrue("Die Zeile haengt an keiner Bedingung mehr", bedingung != null)

        // Ein Name ist keine Bedingung, sondern zeigt auf eine. Die zweite Fassung dieser
        // Regel las den Wortlaut und fiel um, als die Bedingung einen Namen bekam - die
        // Sache war dieselbe geblieben. Also wird der Name jetzt aufgeloest.
        val wortlaut = if (Regex("""^\w+$""").matches(bedingung!!)) {
            zeilen.firstOrNull { it.trim().startsWith("val $bedingung ") }
                ?: bedingung
        } else {
            bedingung
        }
        assertTrue(
            "Der Weg in die Einstellungen hängt an einer Bedingung, die nicht von der " +
                "Suche handelt: \"if ($bedingung)\" → $wortlaut. Er soll immer da sein; " +
                "nur eine Suche, die ihn nicht trifft, darf ihn weglassen.",
            "query" in wortlaut,
        )
    }
}
