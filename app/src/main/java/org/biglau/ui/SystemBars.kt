package org.biglau.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Statusleiste und Navigationsleiste verstecken. PLAN.md 4.2.
 *
 * Auf diesem Geraet sind das zusammen rund vierzig von 605 dp Hoehe - sieben Prozent, die
 * den Kacheln fehlen. Auf einem grossen Telefon waere das Zierrat; auf drei Zoll ist es
 * eine Zeile mehr.
 */
object SystemBars {

    /**
     * Wie sich die Leisten verhalten sollen.
     *
     * Es gibt bewusst kein hartes Verstecken. Wer die Leisten wegnimmt und dabei auch das
     * Herunterziehen sperrt, sperrt die Benachrichtigungen weg - und wer nicht weiss, dass
     * man dafuer wischen kann, kommt nie wieder an sie heran. Ein paar dp sind das nicht
     * wert.
     */
    enum class Behaviour { VISIBLE, HIDDEN_SWIPE_SHOWS }

    fun behaviourFor(fullScreen: Boolean): Behaviour =
        if (fullScreen) Behaviour.HIDDEN_SWIPE_SHOWS else Behaviour.VISIBLE
}

/** Setzt [SystemBars.behaviourFor] auf das Fenster um. */
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
