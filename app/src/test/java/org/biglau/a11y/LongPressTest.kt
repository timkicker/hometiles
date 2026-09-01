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
    fun `ohne Barrierefreiheit oeffnet der Langdruck den Editor`() {
        assertEquals(listOf(LongPressAction.EDIT), LongPress.decide(off, editMode = false))
    }

    @Test
    fun `mit Vorlesen wird vorgelesen statt bearbeitet`() {
        // Wer sich Kacheln vorlesen laesst, weil er sie nicht liest, darf nicht
        // versehentlich im Editor landen.
        assertEquals(listOf(LongPressAction.SPEAK), LongPress.decide(speak, editMode = false))
    }

    @Test
    fun `mit Popup wird angezeigt statt bearbeitet`() {
        assertEquals(listOf(LongPressAction.POPUP), LongPress.decide(popup, editMode = false))
    }

    @Test
    fun `beides zusammen widerspricht sich nicht`() {
        assertEquals(
            listOf(LongPressAction.SPEAK, LongPressAction.POPUP),
            LongPress.decide(both, editMode = false),
        )
    }

    @Test
    fun `im Bearbeitungsmodus gewinnt immer der Editor`() {
        listOf(off, speak, popup, both).forEach { config ->
            assertEquals(listOf(LongPressAction.EDIT), LongPress.decide(config, editMode = true))
        }
    }

    @Test
    fun `der Editor bleibt ohne Barrierefreiheit per Langdruck erreichbar`() {
        assertTrue(LongPress.editorReachableByLongPress(off, editMode = false))
    }

    @Test
    fun `mit Barrierefreiheit braucht der Editor einen anderen Weg`() {
        // Sonst waere die Belegung unerreichbar, sobald jemand das Vorlesen einschaltet -
        // dieselbe Falle wie bei der ausgeblendeten App und beim Screen ohne Heim-Kachel.
        assertTrue(LongPress.needsEditModeEntry(speak))
        assertTrue(LongPress.needsEditModeEntry(popup))
        assertTrue(LongPress.needsEditModeEntry(both))
    }

    @Test
    fun `ohne Barrierefreiheit braucht es keinen zweiten Weg`() {
        assertTrue(!LongPress.needsEditModeEntry(off))
    }

    @Test
    fun `der Bearbeitungsmodus macht den Editor immer erreichbar`() {
        listOf(off, speak, popup, both).forEach { config ->
            assertTrue(LongPress.editorReachableByLongPress(config, editMode = true))
        }
    }
}

/**
 * Die Lesehilfe muss einen Telefonwechsel ueberleben und darf eine aeltere Konfiguration
 * nicht zerreissen - sonst steht jemand nach dem Umzug wieder vor stummen Kacheln.
 */
class AccessibilityConfigTest {

    @Test
    fun `Lesehilfe ueberlebt Export und Import`() {
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
    fun `alte Konfiguration ohne Lesehilfe laedt mit beiden Schaltern aus`() {
        val old = """{"screens":[{"id":"home","name":"Start","cells":[]}],"behaviour":{}}"""
        val config = ConfigTransfer.import(old)
        assertNotNull(config)
        assertFalse(config!!.behaviour.accessibility.speakOnLongPress)
        assertFalse(config.behaviour.accessibility.popupOnLongPress)
        // Und damit bleibt der Editor da, wo er immer war.
        assertTrue(LongPress.editorReachableByLongPress(config.behaviour.accessibility, editMode = false))
    }
}

/**
 * Auslösen per langem Druck.
 *
 * Für zittrige Hände die wichtigste Einstellung der App: ein versehentliches Streifen
 * startet dann nichts mehr. Sie kollidiert aber mit allem anderen, was am langen Druck
 * hängt - Editor und Vorlesen -, und diese Kollision wird hier entschieden statt vertagt.
 */
class PressModeTest {

    private val aus = Accessibility()
    private val vorlesen = Accessibility(speakOnLongPress = true)

    @Test
    fun `bei kurzem Druck bleibt alles wie bisher`() {
        assertEquals(
            listOf(LongPressAction.EDIT),
            LongPress.decide(aus, editMode = false, pressMode = PressMode.SHORT),
        )
    }

    @Test
    fun `bei langem Druck loest der lange Druck aus`() {
        assertEquals(
            listOf(LongPressAction.ACTIVATE),
            LongPress.decide(aus, editMode = false, pressMode = PressMode.LONG),
        )
    }

    @Test
    fun `Auslösen geht dem Vorlesen vor`() {
        // Wer den Langdruck zum Starten gewaehlt hat, will starten. Vorlesen bleibt ueber
        // die Einstellungen erreichbar - eine Kachel, die statt zu starten vorliest, waere
        // fuer diesen Nutzer unbrauchbar.
        assertEquals(
            listOf(LongPressAction.ACTIVATE),
            LongPress.decide(vorlesen, editMode = false, pressMode = PressMode.LONG),
        )
    }

    @Test
    fun `der Bearbeitungsmodus gewinnt trotzdem`() {
        assertEquals(
            listOf(LongPressAction.EDIT),
            LongPress.decide(vorlesen, editMode = true, pressMode = PressMode.LONG),
        )
    }

    @Test
    fun `bei langem Druck braucht der Editor einen anderen Weg`() {
        // Dieselbe Falle wie beim Vorlesen: sonst waere die Belegung unerreichbar.
        assertTrue(LongPress.needsEditModeEntry(aus, PressMode.LONG))
        assertFalse(LongPress.needsEditModeEntry(aus, PressMode.SHORT))
    }
}

/**
 * Die Zweitbelegung einer Kachel.
 *
 * `PLAN.md` 4.3, Zeile 485: „Jede Aktion zusätzlich auf Langdruck belegbar, unabhängig vom
 * Kurzdruck." Das Feld dafür stand seit dem ersten Tag im Modell und wurde nur geschrieben,
 * nie gelesen - der Editor konnte es setzen, und beim Drücken passierte nichts.
 *
 * Sie geht allem anderen vor, weil sie eine Entscheidung für genau diese eine Kachel ist,
 * während Vorlesen und Druckmodus allgemeine Vorgaben sind. Wer sie setzt, will sie auslösen.
 */
class SecondActionTest {

    private val aus = Accessibility()
    private val vorlesen = Accessibility(speakOnLongPress = true)

    @Test
    fun `ohne Zweitbelegung bleibt alles wie bisher`() {
        assertEquals(
            listOf(LongPressAction.EDIT),
            LongPress.decide(aus, editMode = false, hasSecondAction = false),
        )
    }

    @Test
    fun `mit Zweitbelegung wird sie ausgeloest`() {
        assertEquals(
            listOf(LongPressAction.SECOND_ACTION),
            LongPress.decide(aus, editMode = false, hasSecondAction = true),
        )
    }

    @Test
    fun `sie geht dem Vorlesen vor`() {
        assertEquals(
            listOf(LongPressAction.SECOND_ACTION),
            LongPress.decide(vorlesen, editMode = false, hasSecondAction = true),
        )
    }

    @Test
    fun `sie geht auch dem Druckmodus vor`() {
        assertEquals(
            listOf(LongPressAction.SECOND_ACTION),
            LongPress.decide(aus, editMode = false, PressMode.LONG, hasSecondAction = true),
        )
    }

    @Test
    fun `der Bearbeitungsmodus schlaegt sie trotzdem`() {
        // Sonst koennte man eine Kachel mit Zweitbelegung nie wieder aendern.
        assertEquals(
            listOf(LongPressAction.EDIT),
            LongPress.decide(aus, editMode = true, hasSecondAction = true),
        )
    }

    @Test
    fun `mit Zweitbelegung braucht der Editor einen anderen Weg`() {
        assertFalse(
            LongPress.editorReachableByLongPress(aus, editMode = false, hasSecondAction = true),
        )
    }
}
