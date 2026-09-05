package org.biglau.a11y

import org.biglau.data.Accessibility
import org.biglau.data.PressMode
import org.biglau.data.ConfigTransfer
import org.biglau.data.LauncherConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LongPressTest {

    private val off = Accessibility()
    private val speak = Accessibility(speakOnLongPress = true)
    private val popup = Accessibility(popupOnLongPress = true)
    private val both = Accessibility(speakOnLongPress = true, popupOnLongPress = true)

    @Test
    fun `without accessibility the long press opens the editor`() {
        assertEquals(listOf(LongPressAction.EDIT), LongPress.decide(off, editMode = false))
    }

    @Test
    fun `with reading aloud it reads instead of editing`() {
        // whoever has tiles read to them because they cannot read them must not end up in
        // the editor by accident.
        assertEquals(listOf(LongPressAction.SPEAK), LongPress.decide(speak, editMode = false))
    }

    @Test
    fun `with the popup it shows instead of editing`() {
        assertEquals(listOf(LongPressAction.POPUP), LongPress.decide(popup, editMode = false))
    }

    @Test
    fun `both together do not contradict each other`() {
        assertEquals(
            listOf(LongPressAction.SPEAK, LongPressAction.POPUP),
            LongPress.decide(both, editMode = false),
        )
    }

    @Test
    fun `in edit mode the editor always wins`() {
        listOf(off, speak, popup, both).forEach { config ->
            assertEquals(listOf(LongPressAction.EDIT), LongPress.decide(config, editMode = true))
        }
    }

    @Test
    fun `without accessibility the editor stays reachable by long press`() {
        assertTrue(LongPress.editorReachableByLongPress(off, editMode = false))
    }

    @Test
    fun `with accessibility the editor needs another way`() {
        // otherwise assigning tiles becomes unreachable the moment someone switches reading
        // aloud on - the same trap as the hidden app and the screen without a home tile.
        assertTrue(LongPress.needsEditModeEntry(speak))
        assertTrue(LongPress.needsEditModeEntry(popup))
        assertTrue(LongPress.needsEditModeEntry(both))
    }

    @Test
    fun `without accessibility no second way is needed`() {
        assertTrue(!LongPress.needsEditModeEntry(off))
    }

    @Test
    fun `edit mode always makes the editor reachable`() {
        listOf(off, speak, popup, both).forEach { config ->
            assertTrue(LongPress.editorReachableByLongPress(config, editMode = true))
        }
    }
}

/**
 * the reading aid has to survive a change of phone and must not tear an older configuration
 * apart - or someone stands in front of silent tiles again after moving.
 */
class AccessibilityConfigTest {

    @Test
    fun `the reading aid survives export and import`() {
        val config = LauncherConfig().let {
            it.copy(
                behaviour = it.behaviour.copy(
                    accessibility = Accessibility(speakOnLongPress = true, popupOnLongPress = true),
                ),
            )
        }
        val back = ConfigTransfer.import(ConfigTransfer.export(config))
        assertNotNull(back)
        assertTrue(back!!.behaviour.accessibility.speakOnLongPress)
        assertTrue(back.behaviour.accessibility.popupOnLongPress)
    }

    @Test
    fun `an old configuration without the reading aid loads with both switches off`() {
        val old = """{"screens":[{"id":"home","name":"Start","cells":[]}],"behaviour":{}}"""
        val config = ConfigTransfer.import(old)
        assertNotNull(config)
        assertFalse(config!!.behaviour.accessibility.speakOnLongPress)
        assertFalse(config.behaviour.accessibility.popupOnLongPress)
        // and so the editor stays where it always was.
        assertTrue(LongPress.editorReachableByLongPress(config.behaviour.accessibility, editMode = false))
    }
}

/**
 * triggering by a long press.
 *
 * for shaky hands the most important setting in the app: brushing a tile by accident then
 * starts nothing. it collides with everything else hanging on the long press - editor and
 * reading aloud - and that collision is decided here rather than postponed.
 */
class PressModeTest {

    private val off = Accessibility()
    private val speak = Accessibility(speakOnLongPress = true)

    @Test
    fun `with a short press everything stays as it was`() {
        assertEquals(
            listOf(LongPressAction.EDIT),
            LongPress.decide(off, editMode = false, pressMode = PressMode.SHORT),
        )
    }

    @Test
    fun `with a long press the long press triggers`() {
        assertEquals(
            listOf(LongPressAction.ACTIVATE),
            LongPress.decide(off, editMode = false, pressMode = PressMode.LONG),
        )
    }

    @Test
    fun `triggering comes before reading aloud`() {
        // whoever chose the long press to start things wants to start. reading aloud stays
        // reachable through the settings; a tile that reads instead of starting would be
        // useless to this user.
        assertEquals(
            listOf(LongPressAction.ACTIVATE),
            LongPress.decide(speak, editMode = false, pressMode = PressMode.LONG),
        )
    }

    @Test
    fun `edit mode still wins`() {
        assertEquals(
            listOf(LongPressAction.EDIT),
            LongPress.decide(speak, editMode = true, pressMode = PressMode.LONG),
        )
    }

    @Test
    fun `with a long press the editor needs another way`() {
        // the same trap as with reading aloud: assigning tiles would be unreachable.
        assertTrue(LongPress.needsEditModeEntry(off, PressMode.LONG))
        assertFalse(LongPress.needsEditModeEntry(off, PressMode.SHORT))
    }
}

/**
 * a tile's second action. `PLAN.md` 4.3: every action assignable to the long press as well,
 * independent of the short press. the field stood in the model from the first day and was
 * only written, never read - the editor could set it and pressing did nothing.
 *
 * it comes before everything else because it is a decision for this one tile, while reading
 * aloud and the press mode are general settings. whoever sets it wants to trigger it.
 */
class SecondActionTest {

    private val off = Accessibility()
    private val speak = Accessibility(speakOnLongPress = true)

    @Test
    fun `without a second action everything stays as it was`() {
        assertEquals(
            listOf(LongPressAction.EDIT),
            LongPress.decide(off, editMode = false, hasSecondAction = false),
        )
    }

    @Test
    fun `with a second action it is triggered`() {
        assertEquals(
            listOf(LongPressAction.SECOND_ACTION),
            LongPress.decide(off, editMode = false, hasSecondAction = true),
        )
    }

    @Test
    fun `it comes before reading aloud`() {
        assertEquals(
            listOf(LongPressAction.SECOND_ACTION),
            LongPress.decide(speak, editMode = false, hasSecondAction = true),
        )
    }

    @Test
    fun `it comes before the press mode too`() {
        assertEquals(
            listOf(LongPressAction.SECOND_ACTION),
            LongPress.decide(off, editMode = false, PressMode.LONG, hasSecondAction = true),
        )
    }

    @Test
    fun `edit mode beats it anyway`() {
        // or a tile with a second action could never be changed again.
        assertEquals(
            listOf(LongPressAction.EDIT),
            LongPress.decide(off, editMode = true, hasSecondAction = true),
        )
    }

    @Test
    fun `with a second action the editor needs another way`() {
        assertFalse(
            LongPress.editorReachableByLongPress(off, editMode = false, hasSecondAction = true),
        )
    }
}
