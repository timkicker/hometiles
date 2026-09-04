package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ein Schalter sagt „aus" nur, wo die Beschriftung es nicht schon sagt.
 *
 * In BigLau springt fast jede Schalterzeile mit der Beschriftung um. Ob dazu noch „an" oder
 * „aus" gehoert, haengt daran, was die **ausgeschaltete** Fassung sagt:
 *
 * * Eine **Einladung** - „Vorlesen", „Lassen Sie Kacheln den ganzen Bildschirm nutzen" -
 *   nennt den Zustand nicht. Dort hilft „aus", zumal die eingeschaltete Fassung sich im Ohr
 *   oft nur um einen Buchstaben unterscheidet („Vorlesen" / „Liest vor").
 * * Eine **Aussage** - „Kein PIN vor der App-Liste", „Jede App startet ohne PIN",
 *   „Sofort senden" - nennt ihn schon. „Sofort senden, aus" liest sich wie das Gegenteil,
 *   und genau das stand am 04.09.2026 auf dem Geraet.
 *
 * Das laesst sich nicht ausrechnen, nur entscheiden. Deshalb steht hier jede Zeile mit
 * `checked` namentlich und mit ihrer ausgeschalteten Beschriftung. Kommt eine dazu, faellt
 * diese Regel um und verlangt die Entscheidung - statt sie zu vergessen.
 */
class SchalterSatzTest {

    /** Der Schalter → was seine ausgeschaltete Beschriftung sagt. */
    private val begruendet = mapOf(
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
        // Die beiden Mehrfachlisten: dort ist jede Zeile fuer sich an oder aus, und eine
        // ausgeschaltete sagt sonst gar nichts.
        "erlaubt" to "eine von mehreren erlaubten Apps",
        "sichtbar" to "eine von mehreren sichtbaren Anrufarten",
    )

    @Test
    fun `jeder Schalter mit Ansage ist begruendet`() {
        val gefunden = Quelltext.dateien()
            .flatMap { datei ->
                Regex("""checked = (.+),""").findAll(datei.readText()).map { it.groupValues[1] }
            }
            .toSortedSet()
        assertEquals(
            "Eine Zeile sagt „an\"/\"aus\", ohne dass jemand entschieden haette, ob ihre " +
                "ausgeschaltete Beschriftung den Zustand nicht schon nennt. Bei einer " +
                "Aussage („Jede App startet ohne PIN\") liest sich der Zusatz wie das " +
                "Gegenteil - dann gehoert die Zeile nicht hierher, sondern zurueck auf eine " +
                "gefaerbte Flaeche.",
            begruendet.keys.toSortedSet(),
            gefunden,
        )
    }
}
