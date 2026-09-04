package org.biglau.settings

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Sprung auf eine Unterseite - und was ihn am Emulator lange nicht ankommen liess.
 *
 * Gemessen: kalt gestartet sprang er, warm nicht. Android liefert einer schon laufenden
 * Activity den neuen Intent nur ueber `onNewIntent`, und `onNewIntent` kommt nur bei einem
 * Startmodus, der die vorhandene Instanz wiederverwendet. Ohne `singleTop` verwarf das
 * System den Aufruf mit "intent has been delivered to currently running top-most instance"
 * und die Seite von vorhin blieb stehen - ein Schalter, der einmal wirkt und danach nie
 * wieder.
 */
class SettingsDeepLinkTest {

    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun `bekannter Name ergibt die Seite`() {
        assertEquals(Page.CALL_TYPES, SettingsDeepLink.ziel("CALL_TYPES"))
        assertEquals(Page.CONTACTS, SettingsDeepLink.ziel("CONTACTS"))
    }

    /** Eine Sicherung aus einer spaeteren Fassung darf die Einstellungen nicht abschiessen. */
    @Test
    fun `unbekannter oder fehlender Name ergibt nichts`() {
        assertNull(SettingsDeepLink.ziel("GIBTSNICHT"))
        assertNull(SettingsDeepLink.ziel(null))
        assertNull(SettingsDeepLink.ziel(""))
    }

    @Test
    fun `ohne Ziel oeffnet die Hauptseite`() {
        assertEquals(Page.MAIN, SettingsDeepLink.start(locked = false, ziel = null))
    }

    @Test
    fun `mit Ziel oeffnet die Unterseite`() {
        assertEquals(Page.MESSAGES, SettingsDeepLink.start(locked = false, ziel = Page.MESSAGES))
    }

    /** Sonst waere die PIN mit einem Aufruf von aussen umgangen. */
    @Test
    fun `das Schloss geht dem Ziel vor`() {
        assertEquals(Page.GATE, SettingsDeepLink.start(locked = true, ziel = Page.MESSAGES))
        assertNull(SettingsDeepLink.sprung(Page.GATE, Page.MESSAGES))
    }

    @Test
    fun `eine spaetere Anfrage springt`() {
        assertEquals(Page.CALL_TYPES, SettingsDeepLink.sprung(Page.MESSAGES, Page.CALL_TYPES))
    }

    @Test
    fun `ohne Anfrage und auf der eigenen Seite bleibt alles stehen`() {
        assertNull(SettingsDeepLink.sprung(Page.MESSAGES, null))
        assertNull(SettingsDeepLink.sprung(Page.MESSAGES, Page.MESSAGES))
    }

    /**
     * Ohne wiederverwendenden Startmodus kommt die zweite Anfrage nie an. Das war der Fehler
     * am Emulator: der Code stimmte, nur rief ihn niemand auf.
     */
    @Test
    fun `die Einstellungen nehmen einen zweiten Aufruf entgegen`() {
        val block = Quelltext.ausschnitt(manifest, ".settings.SettingsActivity", "/>")
        assertTrue(
            "SettingsActivity nimmt einen Deep-Link entgegen und braucht darum einen " +
                "Startmodus, bei dem onNewIntent ankommt: $block",
            "singleTop" in block || "singleTask" in block,
        )
    }

    /**
     * Ein Name, den keine Seite traegt, fuehrt stumm nirgendwohin.
     *
     * Gelesen wird `SettingsLink` - dort stehen die Kennungen, seit der Sprung ueber eine
     * Absicht laeuft. In `SettingsActivity` stehen sie nur noch als Verweis, und ein Test,
     * der dort nach Zeichenketten sucht, faende **nichts** und waere gruen, ohne etwas zu
     * pruefen.
     */
    @Test
    fun `jede angebotene Kennung gehoert zu einer Seite`() {
        val namen = Page.entries.map { it.name }
        val treffer = Regex("""const val PAGE_[A-Z_]+ = "([A-Z_]+)"""")
            .findAll(Quelltext.datei("org/biglau/ui/SettingsLink.kt").readText())
            .toList()
        assertTrue("keine einzige Kennung gefunden - liest der Test die richtige Datei?", treffer.isNotEmpty())
        treffer.forEach {
            val wert = it.groupValues[1]
            assertTrue("PAGE-Kennung \"$wert\" gehoert zu keiner Seite", wert in namen)
        }
    }
}
