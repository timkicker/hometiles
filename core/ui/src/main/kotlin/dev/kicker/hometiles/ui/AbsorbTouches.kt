package dev.kicker.hometiles.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * swallows every tap a tile inside has not already consumed.
 *
 * for screens that lie *over* another instead of replacing it: a tap into the margin or a
 * gap otherwise reaches through. measured on the jelly 2: with a folder open, a tap at
 * (14, 250) started the contacts tile of the home screen behind it.
 */
fun Modifier.absorbTouches(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
            if (event.changes.all { !it.pressed }) break
        }
    }
}
