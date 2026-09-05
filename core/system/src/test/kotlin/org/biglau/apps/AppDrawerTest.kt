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
    fun `a key holds package and activity`() {
        assertEquals("com.brave/com.brave.Main", AppDrawer.keyOf(brave))
    }

    @Test
    fun `hidden apps vanish from the list`() {
        val hidden = setOf(AppDrawer.keyOf(calc))
        assertEquals(listOf(brave, maps), AppDrawer.visible(all, hidden))
    }

    @Test
    fun `a whole package can be hidden too`() {
        assertEquals(listOf(calc, maps), AppDrawer.visible(all, setOf("com.brave")))
    }

    @Test
    fun `without hiding everything stays`() {
        assertEquals(all, AppDrawer.visible(all, emptySet()))
    }

    @Test
    fun `recently used keeps the stored order`() {
        val keys = listOf(AppDrawer.keyOf(maps), AppDrawer.keyOf(brave))
        assertEquals(listOf(maps, brave), AppDrawer.recents(all, keys, emptySet(), limit = 4))
    }

    @Test
    fun `an uninstalled app leaves no gap`() {
        // otherwise an empty spot would stand in the row of recently used apps.
        val keys = listOf("com.gone/com.gone.Main", AppDrawer.keyOf(brave))
        assertEquals(listOf(brave), AppDrawer.recents(all, keys, emptySet(), limit = 4))
    }

    @Test
    fun `hidden apps do not turn up among the recently used either`() {
        val keys = listOf(AppDrawer.keyOf(brave), AppDrawer.keyOf(calc))
        val hidden = setOf(AppDrawer.keyOf(brave))
        assertEquals(listOf(calc), AppDrawer.recents(all, keys, hidden, limit = 4))
    }

    @Test
    fun `the number of recently used ones is bounded`() {
        val keys = all.map(AppDrawer::keyOf)
        assertEquals(2, AppDrawer.recents(all, keys, emptySet(), limit = 2).size)
        assertTrue(AppDrawer.recents(all, keys, emptySet(), limit = 0).isEmpty())
    }

    @Test
    fun `a launch moves the app to the front`() {
        val keys = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), AppDrawer.remember(keys, "b"))
    }

    @Test
    fun `a key never stands twice in the list`() {
        val once = AppDrawer.remember(listOf("a", "b"), "a")
        assertEquals(listOf("a", "b"), once)
        assertEquals(once.size, once.toSet().size)
    }

    @Test
    fun `a new app joins at the front`() {
        assertEquals(listOf("new", "a", "b"), AppDrawer.remember(listOf("a", "b"), "new"))
    }

    @Test
    fun `the list does not grow without bound`() {
        var keys = emptyList<String>()
        repeat(50) { keys = AppDrawer.remember(keys, "app$it", cap = 12) }
        assertEquals(12, keys.size)
        assertEquals("app49", keys.first())
    }

    @Test
    fun `the search leaves hidden apps out`() {
        val hidden = setOf(AppDrawer.keyOf(calc))
        assertTrue(AppDrawer.search(all, hidden, "calc").isEmpty())
        assertEquals(listOf(brave), AppDrawer.search(all, hidden, "brave"))
    }

    @Test
    fun `hiding can be toggled`() {
        val once = AppDrawer.toggleHidden(emptySet(), brave)
        assertTrue(AppDrawer.keyOf(brave) in once)
        assertTrue(AppDrawer.toggleHidden(once, brave).isEmpty())
    }
}

/**
 * how many recently used apps are shown.
 *
 * from use: after one day the stored list stood at twelve entries - twelve different apps -
 * four were shown, and the number could be set nowhere.
 */
class RecentCountTest {

    private fun app(name: String) = LaunchableApp(name, "$name.Main", name)

    private val all = (1..15).map { app("app$it") }
    private val keys = all.map { AppDrawer.keyOf(it) }

    @Test
    fun `the choice never goes beyond what is stored`() {
        // otherwise a number could be chosen that is never reached.
        assertTrue(AppDrawer.RECENT_CHOICES.max() <= AppDrawer.STORAGE_CAP)
    }

    @Test
    fun `zero means no suggestions at all`() {
        assertTrue(AppDrawer.recents(all, keys, emptySet(), 0).isEmpty())
        assertTrue(AppDrawer.RECENT_CHOICES.contains(0))
    }

    @Test
    fun `the chosen number is kept to`() {
        AppDrawer.RECENT_CHOICES.filter { it > 0 }.forEach { count ->
            assertEquals(count, AppDrawer.recents(all, keys, emptySet(), count).size)
        }
    }

    @Test
    fun `no more is stored than the cap allows`() {
        var list = emptyList<String>()
        keys.forEach { list = AppDrawer.remember(list, it) }
        assertEquals(AppDrawer.STORAGE_CAP, list.size)
    }

    @Test
    fun `with fewer there than wanted there are simply fewer`() {
        val few = keys.take(3)
        assertEquals(3, AppDrawer.recents(all, few, emptySet(), 12).size)
    }
}
