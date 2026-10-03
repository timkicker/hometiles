package dev.kicker.hometiles

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a tile whose app is gone says so on the home screen, before anyone taps it.
 *
 * found by the user on 07.09.2026: whatsapp uninstalled and installed again under another
 * package name. the tile stayed and looked usable. a tile with its own label kept that label,
 * because the label is taken before the app is asked, and only the missing icon told it
 * apart from a healthy one. [DeadTileTest] covers the tap; this covers the look.
 *
 * nothing is deleted. a package can be gone for a moment, during an update, and a tile the
 * user set does not vanish without them.
 */
class MissingAppTileTest {

    /** the branch exists twice in the file; the home screen tile is the one in `TileFor`. */
    private val appBranch = Quelltext.cut(
        Quelltext.cut(
            Quelltext.withoutComments("dev/kicker/hometiles/ui/HomeScreenView.kt"),
            "private fun TileFor(",
            "is ButtonAction.Contact ->",
        ),
        "is ButtonAction.App ->",
    )

    @Test
    fun `the label says the app is missing`() {
        assertTrue(
            "an app tile has no wording for a missing app. it keeps its name and looks like " +
                "it would still open something.",
            "R.string.tile_app_missing" in appBranch,
        )
    }

    @Test
    fun `a label of its own does not hide the missing app`() {
        assertTrue(
            "the own label is taken without asking whether the app is still there. that is " +
                "exactly the case that was found: called whatsapp, pointing at nothing.",
            "button.label ?: appLabel(" !in appBranch,
        )
    }

    @Test
    fun `the missing app has an icon of its own`() {
        assertTrue(
            "with the app gone the tile loses its icon and keeps an empty corner. an empty " +
                "corner is not a sign anyone reads.",
            "Icons.Filled.ErrorOutline" in appBranch,
        )
    }

    /**
     * seen on 03.10.2026 on android 15: calendar switched off, back to the home screen, and the
     * tile still said "Kalender". only a cold start showed "Kalender fehlt". the mark is worth
     * nothing if the screen does not ask again: an app is removed while the home screen is in
     * the background, so asking again on the way back counts as much as listening.
     */
    @Test
    fun `the home screen asks again when apps come or go`() {
        val repository = Quelltext.withoutComments("dev/kicker/hometiles/apps/AppRepository.kt")
        assertTrue(
            "AppRepository has no way to tell anyone that an app came or went.",
            "LauncherApps.Callback" in repository && "fun changes(" in repository,
        )
        val main = Quelltext.withoutComments("dev/kicker/hometiles/MainActivity.kt")
        assertTrue(
            "the home screen does not listen to app changes, so a tile only notices a " +
                "missing app after the next cold start.",
            "apps.changes()" in main,
        )
    }
}
