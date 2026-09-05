package org.biglau.apps

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Zeile zu den ausgeblendeten Apps steht auch da, wenn jemand sucht.
 *
 * Sie hing an `query.isEmpty()` - also war sie genau dann weg, wenn sie gebraucht wird. Wer
 * „brave" tippt und die App vor Monaten ausgeblendet hat, las „Keine App passt dazu" und
 * hatte keinen Anhaltspunkt mehr.
 *
 * Die Einstellungszeile eine Zeile darueber hatte denselben Fehler und ist am 03.09.2026
 * dafuer geaendert worden; die Begruendung steht dort im Quelltext. Diese Regel haelt fest,
 * dass es nicht wieder auseinanderlaeuft.
 */
class VersteckteZeileTest {

    private val liste = Quelltext.withoutComments("org/biglau/apps/AppDrawerActivity.kt")

    @Test
    fun `beim Suchen werden passende ausgeblendete Apps genannt`() {
        assertTrue(
            "Die App-Liste fragt nicht nach ausgeblendeten Treffern - dann sagt sie bei " +
                "einer ausgeblendeten App, es passe keine.",
            "AppDrawer.hiddenMatches(" in liste,
        )
        assertTrue(
            "Die passenden ausgeblendeten Apps werden nicht angezeigt.",
            "R.plurals.apps_hidden_match" in liste,
        )
        // Die erste Fassung dieser Regel sah nur nach, dass die Woerter irgendwo in der
        // Datei stehen. Die Gegenprobe - den Suchfall auf `null` gesetzt - lief damit
        // durch: die Zeile war wieder weg, die Regel gruen. Jetzt wird die Entscheidung
        // selbst gelesen.
        val entscheidung = Quelltext.cut(liste, "val hiddenRow", "}")
        assertTrue(
            "Die Zeile haengt nicht am Suchfall - sie waere beim Suchen wieder weg, also " +
                "genau dann, wenn sie gebraucht wird: $entscheidung",
            "query.isNotEmpty()" in entscheidung && "hiddenHits" in entscheidung,
        )
    }

    @Test
    fun `kein Widerspruch zwischen beiden Saetzen`() {
        val leer = Quelltext.cut(liste, "R.string.search_no_match", atMost = 0)
        // Der Satz steht in einem `if`, das ein paar Zeilen darueber beginnt.
        val bedingung = Quelltext.cut(liste, "", "R.string.search_no_match").takeLast(300)
        assertTrue(
            "\"Keine App passt dazu\" steht auch dann da, wenn eine ausgeblendete App " +
                "passt - daneben widersprechen sich die beiden Zeilen: $bedingung$leer",
            "hiddenHits.isEmpty()" in bedingung,
        )
    }
}
