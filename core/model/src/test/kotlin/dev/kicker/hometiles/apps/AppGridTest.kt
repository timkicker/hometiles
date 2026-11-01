package dev.kicker.hometiles.apps

import dev.kicker.hometiles.data.AppListView
import dev.kicker.hometiles.data.AppsConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** the app list as a grid (issue #7). */
class AppGridTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** the jelly 2 leaves 349 dp, 333 beside the list's padding. */
    @Test
    fun `three apps side by side on the jelly 2`() {
        assertEquals(3, AppGrid.columns(333, 1.0f))
    }

    /** its system text size is 1.35; three cells of 111 dp would break the names apart. */
    @Test
    fun `larger text means fewer and wider cells`() {
        assertEquals(2, AppGrid.columns(333, 1.35f))
    }

    @Test
    fun `never a single column and never a wall of icons`() {
        assertEquals(2, AppGrid.columns(150, 1.0f))
        assertEquals(6, AppGrid.columns(1200, 1.0f))
    }

    @Test
    fun `the last row may be short`() {
        assertEquals(listOf(listOf(1, 2, 3), listOf(4)), AppGrid.rows(listOf(1, 2, 3, 4), 3))
    }

    /** a config from before issue #7 keeps its list. */
    @Test
    fun `the list stays the default`() {
        assertEquals(AppListView.LIST, AppsConfig().view)
        assertEquals(AppListView.LIST, json.decodeFromString(AppsConfig.serializer(), """{"hidden":[]}""").view)
    }

    @Test
    fun `the grid survives the file`() {
        val grid = AppsConfig(view = AppListView.GRID)
        assertEquals(grid, json.decodeFromString(AppsConfig.serializer(), json.encodeToString(AppsConfig.serializer(), grid)))
    }
}
