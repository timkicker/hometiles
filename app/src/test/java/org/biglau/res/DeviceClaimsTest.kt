package org.biglau.res

import org.biglau.Quelltext
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

    private fun quelle(pfad: String) = Quelltext.datei(pfad).readText()

    @Test
    fun `der Hinweis zur Ausblendliste fragt nach der SMS-Rolle`() {
        // Die Nachrichten-Einstellungen sind am 3.9.2026 in den Bereich `sms` gezogen -
        // der Hinweis mit ihnen. Diese Regel fiel beim Umzug laut auf, wie sie soll.
        val stelle = quelle("org/biglau/sms/MessagesSettingsList.kt")
            .let { Quelltext.ausschnitt(it, "sms_filter_numbers_heading", "OutlinedTextField") }
        // Am 3.9.2026 wanderte der Aufruf aus der Seite heraus: die SMS-Rolle vergibt das
        // System, und wer sie erteilt und zurueckkommt, soll nicht denselben Satz noch
        // einmal lesen - der Wert kommt jetzt als `holdsSmsRole` von der Activity. Die
        // Regel prueft weiter dasselbe: der Satz haengt an der Rolle. Nur eben nicht mehr
        // an einer bestimmten Schreibweise.
        assertTrue(
            "sieht nicht nach der Rolle: $stelle",
            "isDefaultSmsApp()" in stelle || "holdsSmsRole" in stelle,
        )
        assertTrue("zweite Fassung fehlt: $stelle", "sms_filter_hint_default" in stelle)
    }

    @Test
    fun `der Text vor der Telefonstatus-Frage sieht nach CALL_PHONE`() {
        val stelle = quelle("org/biglau/MainActivity.kt")
            .let { Quelltext.ausschnitt(it, "R.string.signal_permission_title", "BigRow") }
        assertTrue("sieht nicht nach CALL_PHONE: $stelle", "CALL_PHONE" in stelle)
        assertTrue("zweite Fassung fehlt: $stelle", "signal_permission_body_may_call" in stelle)
    }

    /**
     * Die Nummernsperre versprach „werden abgewiesen, ohne zu klingeln". Eingehende Anrufe
     * sieht aber nur die Standard-Telefon-App - ohne die Rolle wirkt die Sperre allein nach
     * außen. Am 02.09.2026 hielt die Rolle auf dem Telefon des Nutzers ein anderes
     * Programm; dort war der Satz falsch.
     *
     * Seit dem 04.09.2026 hält BigLau die Rolle, und der erste Satz stimmt dort wieder.
     * Genau deshalb bleibt die Regel: die Seite darf nicht davon ausgehen, sondern muss
     * fragen — auf einem anderen Gerät ist es wieder anders, und auf diesem war es zweimal
     * verschieden.
     */
    @Test
    fun `der Hinweis zur Nummernsperre fragt nach der Telefon-Rolle`() {
        val quelltext = quelle("org/biglau/settings/SettingsActivity.kt")
        val stelle = quelltext
            .let { Quelltext.ausschnitt(it, "R.string.blocked_numbers)", "OutlinedTextField") }
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
        val logik = quelle("org/biglau/notify/TileNotifications.kt")
        assertTrue("verpasste Anrufe werden nicht selbst gezaehlt", "Builtin.MISSED_CALLS) return missed" in logik)
        assertTrue("ungelesene Nachrichten werden nicht selbst gezaehlt", "unread != null) return unread" in logik)
        listOf(
            "values-de" to listOf("verpasste anrufe", "ungelesene nachrichten"),
            "values" to listOf("missed calls", "unread messages"),
        ).forEach { (verzeichnis, woerter) ->
            // Klein verglichen: ob die Wendung am Satzanfang steht oder mittendrin, ist
            // Grammatik und nicht die Behauptung. Am 04.09.2026 rutschte "unread messages"
            // beim Kuerzen nach vorn, wurde dadurch gross geschrieben, und die Regel meldete
            // einen Fehler, den es nicht gab.
            val satz = Quelltext.texte(verzeichnis).joinToString("\n") { it.readText() }
                .let { Quelltext.ausschnitt(it, "name=\"blink_explainer\"", "</string>") }
                .lowercase()
            woerter.forEach { wort ->
                assertTrue("$verzeichnis: $wort fehlt im Satz: $satz", wort in satz)
            }
        }
    }

    @Test
    fun `beide Fassungen stehen in beiden Sprachen`() {
        // Je Sprache, nicht je Datei - die Texte liegen in mehreren Modulen.
        listOf("values", "values-de").forEach { sprache ->
            val texte = Quelltext.texte(sprache).joinToString("\n") { it.readText() }
            listOf(
                "sms_filter_hint",
                "sms_filter_hint_default",
                "signal_permission_body",
                "signal_permission_body_may_call",
                "blocked_numbers_hint",
                "blocked_numbers_hint_outgoing",
            ).forEach { name ->
                assertTrue("$sprache: $name fehlt", "\"$name\"" in texte)
            }
        }
    }
}
