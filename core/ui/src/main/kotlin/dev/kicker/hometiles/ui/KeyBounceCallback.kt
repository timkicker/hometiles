package dev.kicker.hometiles.ui

import android.view.KeyEvent
import android.view.Window
import dev.kicker.hometiles.a11y.KeyBounce

/**
 * puts [KeyBounce] in front of a window, ahead of compose, which acts on the release of
 * select. a wrapped window callback and not an activity override: androidx restricts
 * `dispatchKeyEvent` on its activities, see ScreenKeysTest.
 */
internal fun Window.dropKeyBounces(enabled: () -> Boolean) {
    val inner = callback
    val bounce = KeyBounce()
    callback = object : Window.Callback by inner {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            val down = event.action == KeyEvent.ACTION_DOWN
            if (enabled() && !bounce.accept(event.keyCode, down, event.repeatCount, event.eventTime)) {
                return true
            }
            return inner.dispatchKeyEvent(event)
        }
    }
}
