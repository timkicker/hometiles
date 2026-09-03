package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jede fremde Bibliothek steht namentlich da, mit Grund.
 *
 * Der README verspricht: „quelloffen, ohne Konto, ohne Werbung, ohne Netzwerkzugriff". Die
 * fehlende `INTERNET`-Berechtigung sichert den letzten Teil ab (`SlopRulesTest`) — für die
 * ersten beiden stand nichts im Quelltext. Eine Bibliothek für Absturzberichte oder
 * Nutzungszahlen kommt aber nicht über eine Berechtigung ins Programm, sondern über eine
 * Zeile in `libs.versions.toml`, und die liest niemand nach.
 *
 * Deshalb hier die vollständige Liste. Was dazukommt, fällt um — und wer es aufnimmt,
 * schreibt daneben, wofür.
 *
 * Am 3.9.2026 hat die Regel gleich beim Schreiben etwas gefunden:
 * `androidx-datastore-preferences` stand seit Monaten im Katalog und wurde von keinem Modul
 * benutzt. BigLau schreibt seine Konfiguration selbst (`ConfigFile`); der Eintrag war ein
 * Überbleibsel einer verworfenen Richtung. Entfernt.
 */
class AbhaengigkeitenTest {

    private val katalog = File("../gradle/libs.versions.toml").readText()

    /** Alias → wofür. */
    private val erlaubt = mapOf(
        "androidx-core-ktx" to "Grundlagen des Android-Frameworks",
        "androidx-lifecycle-runtime-ktx" to "Lebenszyklus der Bildschirme",
        "androidx-lifecycle-viewmodel-compose" to "Zustand über eine Drehung hinweg",
        "androidx-activity-compose" to "Compose in einer Activity",
        "androidx-compose-bom" to "hält die Compose-Fassungen zusammen",
        "androidx-ui" to "Compose selbst - Layout, Eingaben, Zeichnen",
        "androidx-ui-graphics" to "Farben und Formen",
        "androidx-ui-tooling" to "nur im Debug-Bau: Vorschau im Studio",
        "androidx-ui-tooling-preview" to "die @Preview-Annotation",
        "androidx-material3" to "Material 3 - das Design-System darunter",
        "androidx-material-icons-extended" to "die Symbole auf den Kacheln",
        "kotlinx-coroutines-core" to "Nebenläufigkeit ohne Threads von Hand",
        "kotlinx-serialization-json" to "die Konfigurationsdatei lesen und schreiben",
        "coil-compose" to "Kontaktfotos; bringt OkHttp mit, siehe PLAN.md P8",
        "androidx-profileinstaller" to "spielt das Startprofil ein, siehe PLAN.md P8",
        "junit" to "die Tests - über elfhundert Stück",
    )

    /** Was in dieser App nie vorkommen darf. */
    private val verboten = listOf(
        "firebase", "crashlytics", "analytics", "admob", "ads", "gms", "facebook",
        "sentry", "appcenter", "mixpanel", "amplitude", "adjust", "onesignal",
    )

    private fun aliase(): List<String> {
        val teil = katalog.substringAfter("[libraries]").substringBefore("[plugins]")
        return Regex("""^([a-z0-9-]+)\s*=\s*\{""", RegexOption.MULTILINE)
            .findAll(teil).map { it.groupValues[1] }.toList()
    }

    @Test
    fun `es steht keine bibliothek im katalog, die nicht hier steht`() {
        assertEquals(
            "Eine fremde Bibliothek ist dazugekommen. In dieser App ist das eine " +
                "Entscheidung, keine Kleinigkeit: mit Grund in die Liste in " +
                "AbhaengigkeitenTest, oder wieder heraus.",
            erlaubt.keys.sorted(),
            aliase().sorted(),
        )
    }

    @Test
    fun `nichts fuer Werbung, Konten oder Nutzungszahlen`() {
        val treffer = verboten.filter { it in katalog.lowercase() }
        assertEquals(
            "Der README verspricht: ohne Konto, ohne Werbung. Das hier bricht es.",
            emptyList<String>(),
            treffer,
        )
    }

    @Test
    fun `jede bibliothek wird auch benutzt`() {
        val bauDateien = File("..").walkTopDown()
            .onEnter { it.name != "build" && it.name != ".git" && it.name != ".gradle" }
            .filter { it.name == "build.gradle.kts" }
            .joinToString("\n") { it.readText() }
        val ungenutzt = aliase().filterNot { alias ->
            "libs.${alias.replace('-', '.')}" in bauDateien
        }
        assertEquals(
            "Steht im Katalog und wird von keinem Modul benutzt - ein Überbleibsel",
            emptyList<String>(),
            ungenutzt,
        )
    }

    @Test
    fun `jeder grund ist einer`() {
        erlaubt.forEach { (alias, grund) ->
            assertTrue("$alias: Grund zu knapp", grund.length > 15)
        }
    }
}
