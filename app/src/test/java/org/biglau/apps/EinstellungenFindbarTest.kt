package org.biglau.apps

import org.biglau.Quelltext
import org.biglau.search.TextSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * „BigLau-Einstellungen" ist auch beim Suchen zu finden.
 *
 * Die Zeile am Ende der App-Liste ist der letzte Weg in die Einstellungen — auf dem Gerät
 * des Nutzers war sie am 3.9.2026 der **einzige**. Bis heute stand sie nur da, wenn das
 * Suchfeld leer war (`if (query.isEmpty())`). Wer „einstell" tippte, fand nur die
 * Einstellungen von Android.
 *
 * Das ist die Sorte Fehler, die eine Zusage kaputtmacht, ohne dass jemand sie bemerkt: das
 * README sagt „ganz am Ende steht **immer**", und ein Test dafür gab es nicht.
 */
class EinstellungenFindbarTest {

    private fun wert(name: String): String = Quelltext.textValue(name, "values")

    @Test
    fun `die Zeile haengt nicht mehr an einem leeren Suchfeld`() {
        val quelle = Quelltext.file("org/biglau/apps/AppDrawerActivity.kt").readText()
        val stelle = Quelltext.cut(quelle, "apps_open_settings")
        assertEquals(
            "Die Einstellungszeile darf nicht wieder an `query.isEmpty()` hängen - " +
                "sie ist der letzte Weg dorthin, und der Suchende sucht.",
            false,
            "if (query.isEmpty())" in Quelltext.cut(quelle, "", "apps_open_settings").takeLast(400),
        )
        assertEquals(true, "TextSearch.rank(settingsLabel" in quelle || "TextSearch.rank(\n" in stelle)
    }

    /**
     * Und die Suche trifft sie auch wirklich - **in der jeweiligen Sprache**.
     *
     * Der erste Anlauf dieser Regel suchte „einstell" auch im englischen Text und fiel
     * prompt um. Ein Test, der die falsche Sprache erwartet, sagt nichts über die App aus.
     */
    @Test
    fun `wer nach den Einstellungen sucht, findet sie`() {
        listOf("values" to "setting", "values-de" to "einstell").forEach { (sprache, wort) ->
            val label = Quelltext.textValue("apps_open_settings", sprache)
            assertNotNull("$sprache: $label wird von $wort nicht getroffen",
                TextSearch.rank(label, wort))
            assertNotNull("$sprache: $label wird von biglau nicht getroffen",
                TextSearch.rank(label, "biglau"))
        }
    }

    @Test
    fun `ein leeres Suchfeld trifft weiterhin alles`() {
        assertNotNull(TextSearch.rank(wert("apps_open_settings"), ""))
        assertNull(TextSearch.rank(wert("apps_open_settings"), "zzzz"))
    }
}
