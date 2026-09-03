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

    private val liste = Quelltext.datei("org/biglau/apps/AppDrawerActivity.kt").readText()

    @Test
    fun `die app-liste bietet den weg in die einstellungen an`() {
        assertTrue(
            "Die App-Liste ruft SettingsLink.toRoot nicht mehr. Wer seine " +
                "Einstellungs-Kachel weggibt, hat dann keinen Weg mehr dorthin.",
            "SettingsLink.toRoot" in liste,
        )
    }

    @Test
    fun `der weg haengt an keiner bedingung ausser der suche`() {
        val stelle = liste.indexOf("SettingsLink.toRoot")
        assertTrue("SettingsLink.toRoot nicht gefunden", stelle > 0)
        // Der Block davor: die Zeile darf höchstens beim Suchen verschwinden, nicht an
        // etwas hängen, das die meisten Leute nie haben - wie ausgeblendete Apps.
        val davor = liste.substring(maxOf(0, stelle - 900), stelle)
        val letzteBedingung = davor.substringAfterLast("if (")
        assertTrue(
            "Der Weg in die Einstellungen hängt an einer Bedingung: " +
                "\"if (${letzteBedingung.substringBefore(')')})\". Er soll immer da sein; " +
                "nur während einer laufenden Suche darf er weichen.",
            letzteBedingung.startsWith("query.isEmpty()"),
        )
    }
}
