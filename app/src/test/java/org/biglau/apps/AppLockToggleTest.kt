package org.biglau.apps

import org.biglau.data.AppsConfig
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Security
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Der Schalter der App-Sperre.
 *
 * Am Emulator ist er mit einem Tipp in einen Zustand gekippt, in dem keine einzige App mehr
 * ohne PIN aufging: der Startbildschirm trug nur eingebaute Kacheln, ein Widget und eine
 * Verknüpfung — also nichts, was [AppLock.initialAllowance] hätte erlauben können. Darunter
 * stand trotzdem „Apps auf Deinen Kacheln sind von Anfang an erlaubt".
 */
class AppLockToggleTest {

    private fun konfig(
        gesperrt: Boolean = false,
        erlaubt: Set<String> = emptySet(),
        kachelApps: List<String> = emptyList(),
    ): LauncherConfig {
        val basis = LauncherConfig()
        val screen = basis.screens.first()
        val zellen = kachelApps.mapIndexed { index, paket ->
            Cell(
                x = index,
                y = 0,
                button = Button(action = ButtonAction.App(paket, "$paket.Main")),
            )
        }
        return basis.copy(
            screens = listOf(screen.copy(cells = zellen)),
            apps = AppsConfig(lockOthers = gesperrt, allowed = erlaubt),
            security = Security(pin = "egal"),
        )
    }

    @Test
    fun `ohne Kachel-Apps und ohne Erlaubnisliste wird nicht eingeschaltet`() {
        assertEquals(AppLock.Step.CHOOSE_FIRST, AppLock.toggle(konfig()))
    }

    @Test
    fun `mit einer App auf einer Kachel geht der Schalter an`() {
        assertEquals(
            AppLock.Step.TURN_ON,
            AppLock.toggle(konfig(kachelApps = listOf("com.example.karte"))),
        )
    }

    @Test
    fun `eine vorbereitete Erlaubnisliste reicht auch ohne Kachel-App`() {
        // Wer die Liste erst zusammenstellt und dann einschaltet, soll nicht auf die
        // Kacheln angewiesen sein.
        assertEquals(
            AppLock.Step.TURN_ON,
            AppLock.toggle(konfig(erlaubt = setOf("com.example.karte/Main"))),
        )
    }

    @Test
    fun `ausschalten geht immer`() {
        // Auch aus dem gefaehrlichen Zustand heraus: sonst waere er nicht zu verlassen.
        assertEquals(AppLock.Step.TURN_OFF, AppLock.toggle(konfig(gesperrt = true)))
    }

    @Test
    fun `die Zeile unter dem Schalter zaehlt, was offen bliebe`() {
        assertEquals(0, AppLock.wouldAllow(konfig()))
        assertEquals(1, AppLock.wouldAllow(konfig(kachelApps = listOf("com.example.karte"))))
        // Eine von Hand zusammengestellte Liste schlaegt die Kacheln - sie ist die
        // spaetere Entscheidung.
        assertEquals(
            2,
            AppLock.wouldAllow(
                konfig(
                    erlaubt = setOf("a/Main", "b/Main"),
                    kachelApps = listOf("com.example.karte"),
                ),
            ),
        )
    }

    @Test
    fun `eine erlaubte App genuegt, damit der Schalter angeht`() {
        // Der Fall, in dem die Zeile darunter zuerst weiter "keine App liegt auf einer
        // Kachel" sagte, obwohl schon eine erlaubt war.
        val konfig = konfig(erlaubt = setOf("com.android.camera2/Main"))
        assertEquals(1, AppLock.wouldAllow(konfig))
        assertEquals(AppLock.Step.TURN_ON, AppLock.toggle(konfig))
    }

    @Test
    fun `die Kachel-Apps landen als Schluessel in der Erlaubnis`() {
        assertEquals(
            setOf("com.example.karte/com.example.karte.Main"),
            AppLock.initialAllowance(konfig(kachelApps = listOf("com.example.karte"))),
        )
    }

    @Test
    fun `ein Startbildschirm ohne App-Kacheln erlaubt nichts`() {
        // Genau der Fall vom Emulator.
        assertEquals(emptySet<String>(), AppLock.initialAllowance(konfig()))
    }
}
