package org.biglau.tiles

import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.ScreenKind
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.1: „Swipe-Kette: … in welcher Reihenfolge".
 *
 * `swipeOrder` stand von Anfang an im Modell und wurde von [ScreenOrder] gelesen — aber
 * von keiner Oberfläche je geschrieben. Es blieb immer leer, die Reihenfolge war immer die
 * Anlegereihenfolge. Eine Zusage, die nichts tat.
 */
class SwipeChainTest {

    private fun screen(id: String, kind: ScreenKind = ScreenKind.SCREEN) =
        Screen(id = id, name = id.uppercase(), cols = 2, rows = 3, kind = kind)

    private val config = LauncherConfig(
        screens = listOf(
            screen("a"),
            screen("b"),
            screen("mehr", ScreenKind.FOLDER),
            screen("c"),
        ),
        homeScreenId = "a",
    )

    @Test
    fun `ohne eigene ordnung gilt die anlegereihenfolge`() {
        assertEquals(listOf("a", "b", "c"), SwipeChain.explicit(config))
    }

    // Ordner gehoeren ihrer Kachel, nicht der Reihe. Wer "weiter" tippt, erwartet den
    // naechsten Bildschirm, nicht den Inhalt eines Ordners.
    @Test
    fun `ordner kommen nie in die reihe`() {
        val bewegt = SwipeChain.moveDown(config, "a")
        assertEquals(listOf("b", "a", "c"), bewegt.swipeOrder)
    }

    @Test
    fun `nach oben und nach unten sind umkehrbar`() {
        val runter = SwipeChain.moveDown(config, "a")
        val wiederHoch = SwipeChain.moveUp(runter, "a")
        assertEquals(listOf("a", "b", "c"), SwipeChain.explicit(wiederHoch))
    }

    /**
     * Am Rand passiert nichts, und zwar ohne Umlauf: wer den obersten Bildschirm „nach
     * oben" tippt, erwartet nicht, dass er unten herauskommt. Die Ringform gilt fürs
     * Wischen, nicht fürs Sortieren.
     */
    @Test
    fun `am rand bewegt sich nichts`() {
        assertEquals(config, SwipeChain.moveUp(config, "a"))
        assertEquals(config, SwipeChain.moveDown(config, "c"))
    }

    @Test
    fun `ein unbekannter screen aendert nichts`() {
        assertEquals(config, SwipeChain.moveUp(config, "gibtsnicht"))
    }

    // Die Reihenfolge muss auch wirken, nicht nur gespeichert werden.
    @Test
    fun `die neue reihenfolge gilt fuer naechster und voriger`() {
        val bewegt = SwipeChain.moveDown(config, "a")
        assertEquals(listOf("b", "a", "c"), ScreenOrder.ordered(bewegt).map { it.id })
        assertEquals("a", ScreenOrder.next(bewegt, "b"))
        assertEquals("b", ScreenOrder.previous(bewegt, "a"))
    }

    @Test
    fun `die position wird richtig gemeldet`() {
        assertEquals(0, SwipeChain.position(config, "a"))
        assertEquals(2, SwipeChain.position(config, "c"))
        assertEquals(-1, SwipeChain.position(config, "mehr"))
    }

    // Beim Loeschen wird die Ordnung mitgepflegt - sonst zeigte sie auf einen Screen, den
    // es nicht mehr gibt, und der naechste Wisch fiele ins Leere.
    @Test
    fun `ein geloeschter screen faellt aus der ordnung`() {
        val geordnet = SwipeChain.moveDown(config, "a")
        val ohneB = ScreenEdits.delete(geordnet, "b")
        assertEquals(false, ohneB.swipeOrder.contains("b"))
        assertEquals(listOf("a", "c"), ScreenOrder.ordered(ohneB).map { it.id })
    }
}

/**
 * PLAN.md 4.1: „welche Screens per Wischen erreichbar sind".
 *
 * Ein Screen darf die Kette nur verlassen, wenn er danach noch anders zu erreichen ist.
 * Sonst wäre das Herausnehmen der schnellste Weg, einen eingerichteten Screen unauffindbar
 * zu machen — er stünde weiter in der Konfiguration, und kein Weg führte mehr hin.
 */
class SwipeMembershipTest {

    private fun screen(id: String, cells: List<Cell> = emptyList()) =
        Screen(id = id, name = id.uppercase(), cols = 2, rows = 3, cells = cells)

    private val sprung = Cell(0, 0, button = Button(action = ButtonAction.GoToScreen("b")))

    private val config = LauncherConfig(
        screens = listOf(screen("a", listOf(sprung)), screen("b"), screen("c")),
        homeScreenId = "a",
    )

    @Test
    fun `mit sprungkachel darf er raus`() {
        assertEquals(true, SwipeChain.mayLeave(config, "b"))
        val ohne = SwipeChain.exclude(config, "b")
        assertEquals(setOf("b"), ohne.swipeExcluded)
        assertEquals(listOf("a", "c"), ScreenOrder.ordered(ohne).map { it.id })
    }

    @Test
    fun `ohne weg zurueck bleibt er drin`() {
        assertEquals(false, SwipeChain.mayLeave(config, "c"))
        assertEquals(config, SwipeChain.exclude(config, "c"))
    }

    // Der Startbildschirm ist immer erreichbar - zu ihm fuehrt die Zurueck-Geste.
    @Test
    fun `der startbildschirm darf immer raus`() {
        assertEquals(true, SwipeChain.mayLeave(config, "a"))
    }

    @Test
    fun `zurueckholen geht immer`() {
        val ohne = SwipeChain.exclude(config, "b")
        assertEquals(config.swipeExcluded, SwipeChain.include(ohne, "b").swipeExcluded)
        assertEquals(listOf("a", "b", "c"), ScreenOrder.ordered(SwipeChain.include(ohne, "b")).map { it.id })
    }

    // Ausnahmeliste statt Mitgliederliste: ein spaeter angelegter Screen ist von selbst
    // dabei, statt still zu fehlen.
    @Test
    fun `ein neuer screen ist von selbst in der kette`() {
        val ohne = SwipeChain.exclude(config, "b")
        val mitNeu = ohne.copy(screens = ohne.screens + screen("d"))
        assertEquals(listOf("a", "c", "d"), ScreenOrder.ordered(mitNeu).map { it.id })
    }

    /**
     * Und die Warnung „kein Weg führt hierher" muss das Wischen mitzählen, sonst warnt sie
     * vor Screens, die man mit einer Handbewegung erreicht — und eine Warnung, die nicht
     * stimmt, nimmt man auch dort nicht mehr ernst, wo sie stimmt.
     */
    @Test
    fun `mit eingeschaltetem wischen zaehlt die kette als weg`() {
        val ohneWischen = config
        assertEquals(listOf("c"), ScreenEdits.unreachable(ohneWischen).map { it.id })

        val mitWischen = config.copy(
            behaviour = config.behaviour.copy(swipeBetweenScreens = true),
        )
        assertEquals(emptyList<String>(), ScreenEdits.unreachable(mitWischen).map { it.id })
    }

    @Test
    fun `aus der kette genommen zaehlt das wischen nicht mehr`() {
        val mitWischen = config.copy(
            behaviour = config.behaviour.copy(swipeBetweenScreens = true),
        )
        // "b" hat eine Sprungkachel, darf also raus - und bleibt erreichbar.
        val ohneB = SwipeChain.exclude(mitWischen, "b")
        assertEquals(emptyList<String>(), ScreenEdits.unreachable(ohneB).map { it.id })
    }
}
