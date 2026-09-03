package org.biglau.notify

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Name des Benachrichtigungs-Dienstes darf sich nicht ändern.
 *
 * Die Erlaubnis für den Benachrichtigungszugriff steht **ausserhalb der App**: Android
 * merkt sie sich in `Settings.Secure.enabled_notification_listeners`, und zwar als
 * vollständigen Klassennamen. Wandert die Klasse in ein anderes Paket, zeigt der Eintrag
 * ins Leere — die Erlaubnis ist still weg, die Kacheln blinken nie wieder, und niemand
 * bekommt eine Meldung. Wiederherstellen kann sie nur der Nutzer selbst, in den
 * Systemeinstellungen.
 *
 * Am 3.9.2026 sind in dieser Nacht vier Dateien aus `notify` nach `sms` gewandert, darunter
 * ein Empfänger, der im Manifest steht. Der Zuhörer war nicht dabei — geprüft hat das
 * niemand, es war Glück. `ManifestKlassenTest` hätte den Umzug bemerkt, aber nur, weil das
 * Manifest mitgezogen worden wäre; die Zeile in den Systemeinstellungen des Nutzers zieht
 * niemand mit.
 *
 * Am Gerät nachgesehen: nach rund fünfzehn Neuinstallationen dieser Nacht steht der Eintrag
 * unverändert und der Dienst ist verbunden.
 */
class ListenerNameTest {

    private val name = "org.biglau.notify.BigNotificationListener"

    @Test
    fun `der Zuhoerer heisst noch genauso`() {
        // Über Quelltext.datei, nicht über einen selbst gebauten Pfad: sonst hängt die
        // Regel am Modul, und genau das verbietet QuelltextTest - beim Schreiben prompt
        // hineingelaufen.
        val gefunden = runCatching { Quelltext.datei("${name.replace('.', '/')}.kt") }.isSuccess
        assertTrue(
            "Der Benachrichtigungs-Dienst ist umgezogen oder umbenannt. Die Erlaubnis des " +
                "Nutzers steht in den Systemeinstellungen unter dem alten Namen und ist " +
                "damit still verloren - er müsste sie von Hand neu erteilen.",
            gefunden,
        )
    }

    @Test
    fun `das Manifest nennt genau diesen Namen`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(
            "Manifest und Klassenname gehen auseinander",
            ".notify.BigNotificationListener" in manifest,
        )
    }
}
