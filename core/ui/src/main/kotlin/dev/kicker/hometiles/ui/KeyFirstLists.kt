package dev.kicker.hometiles.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import dev.kicker.hometiles.data.KeyFirst
import androidx.compose.runtime.withFrameNanos

/**
 * does the phone have keys to work lists with: a number keypad, or a d-pad.
 *
 * a flip phone like the cat s22 reports the keypad; a touch phone such as the jelly 2 reports
 * neither, and a phone with a full keyboard and no d-pad stays off, it is worked by touch.
 */
fun phoneHasKeys(configuration: Configuration): Boolean =
    configuration.keyboard == Configuration.KEYBOARD_12KEY ||
        configuration.navigation == Configuration.NAVIGATION_DPAD

/** whether the lists of this screen follow [KeyFirst]. */
@Composable
fun keyFirstOn(mode: KeyFirst): Boolean = mode.isOn(phoneHasKeys(LocalConfiguration.current))

/** where a KeyFirst list wants the focus: [entry] on its first entry, [screen] around it all. */
class FirstEntry(val entry: Modifier, val screen: Modifier)

/**
 * the focus on a list's first entry, once the list has one.
 *
 * opened by key, android hands the window's first focus to the first input field it finds,
 * and that came after this list was drawn: the search field took it and opened its keyboard
 * over half the list (emulator, 25.10.2026). [screen] makes everything one focus group whose
 * way in leads to the entry, so the window's focus lands there and the field stays closed.
 *
 * after a tap android is in touch mode and drops a focus request without a word, so on a
 * touch phone this does nothing even when switched on. one frame first: the entry has to be
 * laid out before it can take anything.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun firstEntryFocus(
    active: Boolean,
    ready: Boolean,
    /**
     * the list is still being read. the window's focus arrives about then, and with nowhere
     * to go it fell through to the search field; it waits instead. a list that stays empty
     * lets the focus in as usual, or the keys could never reach the field.
     */
    loading: Boolean = false,
): FirstEntry {
    val first = remember { FocusRequester() }
    LaunchedEffect(active, ready) {
        if (active && ready) {
            withFrameNanos { }
            runCatching { first.requestFocus() }
        }
    }
    if (!active) return FirstEntry(Modifier, Modifier)
    return FirstEntry(
        entry = Modifier.focusRequester(first),
        screen = Modifier
            .focusProperties {
                enter = {
                    when {
                        ready -> first
                        loading -> FocusRequester.Cancel
                        else -> FocusRequester.Default
                    }
                }
            }
            .focusGroup(),
    )
}
