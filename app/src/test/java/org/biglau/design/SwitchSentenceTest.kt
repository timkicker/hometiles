package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * a switch says "off" only where the label does not say it already.
 *
 * whether "on"/"off" belongs to a switch row hangs on what its **switched-off** wording
 * says. an invitation ("Vorlesen", "Lassen Sie Kacheln den ganzen Bildschirm nutzen") does
 * not name the state; there "off" helps, all the more since the switched-on wording often
 * differs by a single letter. a statement ("Sofort senden") names it already, and "Sofort
 * senden, aus" reads like the opposite - that stood on the device on 04.09.2026.
 *
 * that cannot be computed, only decided. hence every row with `checked` stands here by name.
 * a new one makes this rule fall over and demands the decision.
 */
class SwitchSentenceTest {

    // the switch -> what its switched-off label says. the labels stay german: they are
    // quotations of the interface, and a translation here would name a text that is nowhere.
    private val decided = mapOf(
        "sms.fullScreenAlert" to "Lassen Sie eine neue Nachricht den ganzen Bildschirm nehmen",
        "config.alarmSound" to "Spielen Sie einen lauten Alarmton",
        "config.alarmFlash" to "Lassen Sie das Licht blinken",
        "appearance.hideCutLabels" to "Abgeschnittene Beschriftungen weglassen",
        "appearance.fullScreen" to "Lassen Sie Kacheln den ganzen Bildschirm nutzen",
        "behaviour.accessibility.speakOnLongPress" to "Kachel vorlesen",
        "behaviour.accessibility.popupOnLongPress" to "Kachelnamen gross zeigen",
        "behaviour.accessibility.scrollButtons" to "Knoepfe fuer lange Listen",
        "behaviour.swipeBetweenScreens" to "Lassen Sie das Wischen den Bildschirm wechseln",
        "protectsEditor" to "Die Kacheln auch schuetzen",
        // the two multiple-choice lists: there each row is on or off for itself, and a
        // switched-off one says nothing else.
        "isAllowed" to "one of several allowed apps",
        "visible" to "one of several visible call kinds",
    )

    @Test
    fun `every switch with an announcement is decided`() {
        val found = Quelltext.files()
            .flatMap { file ->
                Regex("""checked = (.+),""").findAll(file.readText()).map { it.groupValues[1] }
            }
            .toSortedSet()
        assertEquals(
            "a row says on/off without anyone having decided whether its switched-off " +
                "label names the state already. with a statement the addition reads like " +
                "the opposite - then the row belongs on a coloured surface instead.",
            decided.keys.toSortedSet(),
            found,
        )
    }
}
