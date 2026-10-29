package dev.kicker.hometiles.apps

import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.notify.SystemPackages
import dev.kicker.hometiles.notify.TileNotifications
import org.junit.Assert.assertEquals
import org.junit.Test

/** the app list with a work profile (issue #6): the same app twice, kept apart. */
class WorkProfileDrawerTest {

    private val chrome = LaunchableApp("Chrome", "com.chrome", "Main")
    private val workChrome = chrome.copy(profile = 11)
    private val maps = LaunchableApp("Maps", "com.maps", "Main")
    private val all = listOf(chrome, workChrome, maps)

    @Test
    fun `hiding the work app leaves the private one`() {
        assertEquals(listOf(chrome, maps), AppDrawer.visible(all, setOf(AppDrawer.keyOf(workChrome))))
    }

    @Test
    fun `hiding the private app leaves the work one`() {
        assertEquals(listOf(workChrome, maps), AppDrawer.visible(all, setOf(AppDrawer.keyOf(chrome))))
    }

    /** a bare package was hidden before work profiles were known, meaning the app one saw. */
    @Test
    fun `an old hidden package hides the main profile only`() {
        assertEquals(listOf(workChrome, maps), AppDrawer.visible(all, setOf("com.chrome")))
    }

    @Test
    fun `recently used keeps both apart`() {
        val recent = listOf(AppDrawer.keyOf(workChrome), AppDrawer.keyOf(maps))
        assertEquals(listOf(workChrome, maps), AppDrawer.recents(all, recent, emptySet(), 4))
    }

    @Test
    fun `the work tile counts the work notices only`() {
        val counts = mapOf("com.chat" to 2, AppKey.notices("com.chat", 11) to 5)
        val private = Button(action = ButtonAction.App("com.chat", "Main"), blink = true)
        val work = Button(action = ButtonAction.App("com.chat", "Main", 11), blink = true)
        assertEquals(2, TileNotifications.badgeFor(private, counts, SystemPackages()))
        assertEquals(5, TileNotifications.badgeFor(work, counts, SystemPackages()))
    }
}
