package org.biglau.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * hides the status and navigation bars. `PLAN.md` 4.2.
 *
 * on this device that is about forty of 605 dp, seven percent, which is one more tile row.
 */
object SystemBars {

    /** no hard hiding: locking the pull-down would lock away the notifications for good. */
    enum class Behaviour { VISIBLE, HIDDEN_SWIPE_SHOWS }

    fun behaviourFor(fullScreen: Boolean): Behaviour =
        if (fullScreen) Behaviour.HIDDEN_SWIPE_SHOWS else Behaviour.VISIBLE
}

/** applies [SystemBars.behaviourFor] to the window. */
@Composable
fun SystemBarsEffect(fullScreen: Boolean) {
    val view = LocalView.current
    LaunchedEffect(fullScreen, view) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        when (SystemBars.behaviourFor(fullScreen)) {
            SystemBars.Behaviour.VISIBLE -> {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }

            SystemBars.Behaviour.HIDDEN_SWIPE_SHOWS -> {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
