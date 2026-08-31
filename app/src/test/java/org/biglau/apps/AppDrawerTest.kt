package org.biglau.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDrawerTest {

    private fun app(label: String, pkg: String = label.lowercase()) =
        LaunchableApp(label, "com.$pkg", "com.$pkg.Main")

    private val brave = app("Brave")
    private val calc = app("Calculator")
    private val maps = app("Maps")
    private val all = listOf(brave, calc, maps)

    @Test
    fun `ein Schluessel enthaelt Paket und Activity`() {
        assertEquals("com.brave/com.brave.Main", AppDrawer.keyOf(brave))
    }

    @Test
    fun `ausgeblendete Apps verschwinden aus der Liste`() {
        val hidden = setOf(AppDrawer.keyOf(calc))
        assertEquals(listOf(brave, maps), AppDrawer.visible(all, hidden))
    }

    @Test
    fun `auch ein ganzes Paket laesst sich ausblenden`() {
        assertEquals(listOf(calc, maps), AppDrawer.visible(all, setOf("com.brave")))
    }

    @Test
    fun `ohne Ausblendung bleibt alles stehen`() {
        assertEquals(all, AppDrawer.visible(all, emptySet()))
    }

    @Test
    fun `zuletzt benutzt behaelt die gespeicherte Reihenfolge`() {
        val keys = listOf(AppDrawer.keyOf(maps), AppDrawer.keyOf(brave))
        assertEquals(listOf(maps, brave), AppDrawer.recents(all, keys, emptySet(), limit = 4))
    }

    @Test
    fun `eine deinstallierte App hinterlaesst keine Luecke`() {
        // Sonst stuende in der Reihe der zuletzt benutzten ein leerer Platz.
        val keys = listOf("com.weg/com.weg.Main", AppDrawer.keyOf(brave))
        assertEquals(listOf(brave), AppDrawer.recents(all, keys, emptySet(), limit = 4))
    }

    @Test
    fun `ausgeblendete Apps tauchen auch bei zuletzt benutzt nicht auf`() {
        val keys = listOf(AppDrawer.keyOf(brave), AppDrawer.keyOf(calc))
        val hidden = setOf(AppDrawer.keyOf(brave))
        assertEquals(listOf(calc), AppDrawer.recents(all, keys, hidden, limit = 4))
    }

    @Test
    fun `die Zahl der zuletzt benutzten ist begrenzt`() {
        val keys = all.map(AppDrawer::keyOf)
        assertEquals(2, AppDrawer.recents(all, keys, emptySet(), limit = 2).size)
        assertTrue(AppDrawer.recents(all, keys, emptySet(), limit = 0).isEmpty())
    }

    @Test
    fun `ein Start rueckt die App nach vorn`() {
        val keys = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), AppDrawer.remember(keys, "b"))
    }

    @Test
    fun `ein Schluessel steht nie doppelt in der Liste`() {
        val once = AppDrawer.remember(listOf("a", "b"), "a")
        assertEquals(listOf("a", "b"), once)
        assertEquals(once.size, once.toSet().size)
    }

    @Test
    fun `eine neue App kommt vorn dazu`() {
        assertEquals(listOf("neu", "a", "b"), AppDrawer.remember(listOf("a", "b"), "neu"))
    }

    @Test
    fun `die Liste waechst nicht unbegrenzt`() {
        var keys = emptyList<String>()
        repeat(50) { keys = AppDrawer.remember(keys, "app$it", cap = 12) }
        assertEquals(12, keys.size)
        assertEquals("app49", keys.first())
    }

    @Test
    fun `die Suche laesst ausgeblendete Apps aus`() {
        val hidden = setOf(AppDrawer.keyOf(calc))
        assertTrue(AppDrawer.search(all, hidden, "calc").isEmpty())
        assertEquals(listOf(brave), AppDrawer.search(all, hidden, "brave"))
    }

    @Test
    fun `Ausblenden laesst sich umschalten`() {
        val once = AppDrawer.toggleHidden(emptySet(), brave)
        assertTrue(AppDrawer.keyOf(brave) in once)
        assertTrue(AppDrawer.toggleHidden(once, brave).isEmpty())
    }
}
