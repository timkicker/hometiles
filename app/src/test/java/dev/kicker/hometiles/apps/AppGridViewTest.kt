package dev.kicker.hometiles.apps

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the grid of issue #7 is a choice and stays one: offered in the settings, read by the list,
 * and the list keeps its rows for everyone who did not choose.
 */
class AppGridViewTest {

    private val drawer = Quelltext.withoutComments("dev/kicker/hometiles/apps/AppDrawerActivity.kt")
    private val settings = Quelltext.withoutComments("dev/kicker/hometiles/settings/SettingsActivity.kt")

    @Test
    fun `the list reads the choice`() {
        assertTrue("the app list no longer asks for the grid", "config.apps.view == AppListView.GRID" in drawer)
        assertTrue("the columns no longer follow the width", "AppGrid.columns(LocalConfiguration.current.screenWidthDp" in drawer)
    }

    /** recently used and all apps both, or half the list would stay rows. */
    @Test
    fun `both parts of the list become a grid`() {
        assertTrue("AppGrid.rows(recents, columns)" in drawer)
        assertTrue("AppGrid.rows(listed, columns)" in drawer)
    }

    /** hiding by long press is how an app leaves the list; the grid must not lose it. */
    @Test
    fun `a long press hides in the grid too`() {
        assertTrue("onHide = ::hide" in drawer)
    }

    @Test
    fun `the settings offer both views`() {
        assertTrue("items(AppListView.entries)" in settings)
        assertTrue("it.apps.copy(view = choice)" in settings)
    }
}
