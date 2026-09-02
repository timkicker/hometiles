package org.biglau.res

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Texte, die etwas über *dieses* Telefon behaupten, sehen nach.
 *
 * Zwei Sätze taten es nicht, und beide fielen am Emulator auf, wo BigLau die SMS- und die
 * Telefon-Rolle hält:
 *
 * 1. Über der Ausblendliste stand „BigLau ist nicht die SMS-App dieses Telefons und kann sie
 *    nicht abweisen" — während die App gerade selbst die eingehenden Nachrichten speicherte.
 * 2. Vor der Frage nach dem Telefonstatus stand „BigLau ruft niemanden an **und kann es auch
 *    nicht**: die Berechtigung dafür hat es nicht." Mit der Telefon-Rolle hat es sie.
 *
 * Beide Sätze sollen Vertrauen herstellen. Ein Satz, der Vertrauen herstellt und dabei nicht
 * stimmt, ist schlimmer als keiner — deshalb diese Regel.
 */
class DeviceClaimsTest {

    private fun quelle(pfad: String) = File(pfad).readText()

    @Test
    fun `der Hinweis zur Ausblendliste fragt nach der SMS-Rolle`() {
        val stelle = quelle("src/main/java/org/biglau/settings/SettingsActivity.kt")
            .substringAfter("sms_filter_numbers_heading")
            .substringBefore("OutlinedTextField")
        assertTrue("sieht nicht nach der Rolle: $stelle", "isDefaultSmsApp()" in stelle)
        assertTrue("zweite Fassung fehlt: $stelle", "sms_filter_hint_default" in stelle)
    }

    @Test
    fun `der Text vor der Telefonstatus-Frage sieht nach CALL_PHONE`() {
        val stelle = quelle("src/main/java/org/biglau/MainActivity.kt")
            .substringAfter("R.string.signal_permission_title")
            .substringBefore("BigRow")
        assertTrue("sieht nicht nach CALL_PHONE: $stelle", "CALL_PHONE" in stelle)
        assertTrue("zweite Fassung fehlt: $stelle", "signal_permission_body_may_call" in stelle)
    }

    /**
     * Die Nummernsperre versprach „werden abgewiesen, ohne zu klingeln". Eingehende Anrufe
     * sieht aber nur die Standard-Telefon-App - ohne die Rolle wirkt die Sperre allein nach
     * außen. Auf dem Telefon des Nutzers hält die Rolle ein anderes Programm; dort war der
     * Satz falsch.
     */
    @Test
    fun `der Hinweis zur Nummernsperre fragt nach der Telefon-Rolle`() {
        val quelltext = quelle("src/main/java/org/biglau/settings/SettingsActivity.kt")
        val stelle = quelltext
            .substringAfter("R.string.blocked_numbers)")
            .substringBefore("OutlinedTextField")
        assertTrue("Die Rolle wird nirgends gelesen", "DialerRole.held(" in quelltext)
        assertTrue("sieht nicht nach der Rolle: $stelle", "hatTelefonRolle" in stelle)
        assertTrue("zweite Fassung fehlt: $stelle", "blocked_numbers_hint_outgoing" in stelle)
    }


    /**
     * Der Satz über das Blinken behauptete: „BigLau braucht dafür Zugriff auf die
     * Benachrichtigungen." Seit die Kacheln für verpasste Anrufe und Nachrichten die
     * Anrufliste bzw. den Nachrichtenanbieter zählen, stimmt das **für sie nicht mehr** —
     * die beiden blinken auch ohne diesen Zugriff. Ein Satz, der einen Zugriff verlangt,
     * den es gar nicht braucht, kostet Vertrauen doppelt.
     */
    @Test
    fun `der Satz zum Blinken passt zu dem was ohne Zugriff zaehlt`() {
        val logik = quelle("src/main/java/org/biglau/notify/TileNotifications.kt")
        assertTrue("verpasste Anrufe werden nicht selbst gezaehlt", "Builtin.MISSED_CALLS) return missed" in logik)
        assertTrue("ungelesene Nachrichten werden nicht selbst gezaehlt", "unread != null) return unread" in logik)
        listOf(
            "src/main/res/values-de/strings.xml" to listOf("Verpasste Anrufe", "ungelesene Nachrichten"),
            "src/main/res/values/strings.xml" to listOf("missed calls", "unread messages"),
        ).forEach { (pfad, woerter) ->
            val satz = quelle(pfad)
                .substringAfter("name=\"blink_explainer\"")
                .substringBefore("</string>")
            woerter.forEach { wort ->
                assertTrue("$pfad: $wort fehlt im Satz: $satz", wort in satz)
            }
        }
    }

    @Test
    fun `beide Fassungen stehen in beiden Sprachen`() {
        listOf("src/main/res/values/strings.xml", "src/main/res/values-de/strings.xml").forEach { pfad ->
            val texte = quelle(pfad)
            listOf(
                "sms_filter_hint",
                "sms_filter_hint_default",
                "signal_permission_body",
                "signal_permission_body_may_call",
                "blocked_numbers_hint",
                "blocked_numbers_hint_outgoing",
            ).forEach { name ->
                assertTrue("$pfad: $name fehlt", "\"$name\"" in texte)
            }
        }
    }
}
