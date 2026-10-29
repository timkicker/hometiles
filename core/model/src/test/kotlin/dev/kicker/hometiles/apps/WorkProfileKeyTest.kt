package dev.kicker.hometiles.apps

import dev.kicker.hometiles.data.AppsConfig
import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.Cell
import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.data.Screen
import dev.kicker.hometiles.data.Security
import dev.kicker.hometiles.security.Pin
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * an app in the work profile is another app than the same one in the main profile (issue #6).
 *
 * until 29.10.2026 both were named `package/activity`. hiding, allowing or counting one did the
 * same to the other, and the work profile's apps were not listed at all.
 */
class WorkProfileKeyTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `the main profile keeps the key it always had`() {
        assertEquals("com.chrome/Main", AppKey.of("com.chrome", "Main", null))
        assertNull(AppKey.profileOf("com.chrome/Main"))
    }

    @Test
    fun `a work app has a key of its own`() {
        val work = AppKey.of("com.chrome", "Main", 11)
        assertNotEquals(AppKey.of("com.chrome", "Main", null), work)
        assertEquals(11L, AppKey.profileOf(work))
        assertEquals("com.chrome/Main", AppKey.withoutProfile(work))
    }

    @Test
    fun `notices are counted per profile`() {
        assertEquals("com.chat", AppKey.notices("com.chat", null))
        assertNotEquals(AppKey.notices("com.chat", null), AppKey.notices("com.chat", 11))
    }

    /** a config written before issue #6 has no profile and must read as the main one. */
    @Test
    fun `an old app tile reads as the main profile`() {
        val old = """{"type":"app","packageName":"com.chrome","activityName":"Main"}"""
        val action = json.decodeFromString(ButtonAction.serializer(), old) as ButtonAction.App
        assertNull(action.profile)
        assertEquals("com.chrome/Main", action.key)
    }

    @Test
    fun `a work tile keeps its profile through the file`() {
        val tile: ButtonAction = ButtonAction.App("com.chrome", "Main", 11)
        val back = json.decodeFromString(ButtonAction.serializer(), json.encodeToString(ButtonAction.serializer(), tile))
        assertEquals(tile, back)
    }

    private val locked = LauncherConfig(
        screens = listOf(
            Screen(
                "home", "Start", 2, 3,
                cells = listOf(Cell(0, 0, button = Button(action = ButtonAction.App("com.mail", "Main", 11)))),
            ),
        ),
        security = Security(pin = Pin.hash("1234")),
        apps = AppsConfig(lockOthers = true, allowed = setOf("com.chrome")),
    )

    /** a bare package comes from a shortcut tile, which only reaches the main profile. */
    @Test
    fun `allowing a shortcut does not open the work app`() {
        assertEquals(false, AppLock.needsPin(locked, "com.chrome/Main", "com.chrome"))
        assertEquals(true, AppLock.needsPin(locked, AppKey.of("com.chrome", "Main", 11), "com.chrome"))
    }

    @Test
    fun `a work tile is allowed as the work app`() {
        assertEquals(setOf("com.mail/Main@11"), AppLock.initialAllowance(locked))
    }
}
