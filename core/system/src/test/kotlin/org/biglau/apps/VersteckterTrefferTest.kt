package org.biglau.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine ausgeblendete App ist nicht weg, und die Suche sagt das.
 *
 * „Keine App passt dazu" stimmt ueber die sichtbare Liste und stimmt nicht ueber das
 * Telefon. Wer eine App vor Monaten ausgeblendet hat und sie sucht, steht sonst vor einem
 * Satz, der ihn in die falsche Richtung schickt - und die Zeile zu den ausgeblendeten Apps
 * stand ausgerechnet dann nicht da, weil sie an `query.isEmpty()` hing. Genau denselben
 * Fehler hatte die Einstellungszeile eine Zeile darueber schon einmal.
 */
class VersteckterTrefferTest {

    private fun app(paket: String, name: String) =
        LaunchableApp(packageName = paket, activityName = "$paket.Main", label = name)

    private val alle = listOf(
        app("com.brave.browser", "Brave"),
        app("com.example.bank", "Bank"),
        app("com.example.karte", "Karten"),
    )

    @Test
    fun `eine ausgeblendete App zaehlt als Treffer, aber nicht als sichtbarer`() {
        val versteckt = setOf(AppDrawer.keyOf(alle[0]))
        assertTrue(
            "Eine ausgeblendete App darf in der Liste nicht auftauchen",
            AppDrawer.search(alle, versteckt, "brave").isEmpty(),
        )
        assertEquals(
            "…aber die Suche muss wissen, dass es sie gibt",
            listOf("Brave"),
            AppDrawer.hiddenMatches(alle, versteckt, "brave").map { it.label },
        )
    }

    @Test
    fun `ohne Suche gibt es keine Treffer zu melden`() {
        val versteckt = setOf(AppDrawer.keyOf(alle[0]))
        assertEquals(emptyList<String>(), AppDrawer.hiddenMatches(alle, versteckt, "").map { it.label })
        assertEquals(emptyList<String>(), AppDrawer.hiddenMatches(alle, versteckt, "   ").map { it.label })
    }

    @Test
    fun `was sichtbar ist, zaehlt hier nicht mit`() {
        assertEquals(
            "Eine sichtbare App darf nicht als ausgeblendeter Treffer gelten - sonst " +
                "stuende die Zeile neben der App, die man gerade sieht.",
            emptyList<String>(),
            AppDrawer.hiddenMatches(alle, emptySet(), "bank").map { it.label },
        )
    }
}
