package org.biglau.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * whoever names the search key has to listen for it.
 *
 * `search_matches` says at exactly one hit that the search key opens it. only the enter key
 * was listened for, through `ImeAction.Search`: measured on the emulator on 04.09.2026,
 * `KEYCODE_SEARCH` did nothing and `KEYCODE_ENTER` started the camera.
 *
 * on a phone with a keyboard that is no blemish. the search key really is there, and a text
 * pointing at a key that does nothing is worse than no hint at all: it makes the user believe
 * they did something wrong.
 *
 * the enter key stays - on a screen keyboard it is the way.
 */
class SearchKeyTest {

    private val source = File("src/main/kotlin/org/biglau/ui/BigSearchField.kt").readText()

    @Test
    fun `the search field listens for the search key`() {
        assertTrue(
            "BigSearchField does not react to Key.Search. the text search_matches names the " +
                "search key; on a keyboard phone it thereby points at a key that does nothing.",
            "Key.Search" in source,
        )
    }

    @Test
    fun `the enter key stays as well`() {
        assertTrue(
            "ImeAction.Search is gone. a screen keyboard has no search key.",
            "ImeAction.Search" in source && "onSearch" in source,
        )
    }
}

/**
 * one gets out of the search field with the d-pad again.
 *
 * measured in four lists on 04.09.2026, on the emulator and on the jelly 2. the app list and
 * contacts have a search field at the top, and the focus stayed in it, four presses down
 * without any movement. messages and the call log have none and run through their entries
 * cleanly. so it is not the lists, it is the field.
 *
 * an `EditText` takes the focus and does not pass it on. with a finger that does not show,
 * since one simply taps the entry. by key the whole list is unreachable.
 *
 * down is the only direction that means anything here: the field is one line, a press down
 * can reach no text inside it, so it belongs to the list.
 */
class LeaveSearchFieldTest {

    private val source = File("src/main/kotlin/org/biglau/ui/BigSearchField.kt").readText()

    @Test
    fun `downwards the field passes the focus on`() {
        assertTrue(
            "BigSearchField does not handle Key.DirectionDown. then the focus sits fast in " +
                "the field and the list below is unreachable by key.",
            "Key.DirectionDown" in source,
        )
        assertTrue(
            "the focus is not passed on. consuming the key is not enough, it has to arrive " +
                "somewhere.",
            "FocusDirection.Down" in source,
        )
    }
}
