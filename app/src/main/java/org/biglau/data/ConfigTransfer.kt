package org.biglau.data

import kotlinx.serialization.json.Json

/**
 * Konfiguration aus- und wieder einlesen - gedacht fuer den Wechsel auf ein anderes Telefon.
 *
 * Die Datei muss fuer sich stehen: auf dem Zielgeraet gibt es kein altes BigLau, das etwas
 * ergaenzen koennte. Deshalb wird alles geschrieben, auch die Vorgabewerte, und die
 * Schema-Version steht mit drin.
 *
 * Was bewusst *nicht* mitwandert, steht in [strippedForTransfer]: Dinge, die auf dem neuen
 * Geraet ohnehin nicht gelten.
 */
object ConfigTransfer {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /**
     * Bereinigt die Konfiguration fuers Ausschreiben.
     *
     * Widget-Kennungen sind vom AppWidgetHost des *alten* Geraets vergeben - auf dem neuen
     * zeigen sie auf nichts oder, schlimmer, auf ein fremdes Widget. Solche Kacheln werden
     * geleert statt kaputt uebertragen. Die zuletzt benutzten Apps sind ebenfalls
     * geraetegebunden und tragen nichts bei.
     */
    fun strippedForTransfer(config: LauncherConfig): LauncherConfig = config.copy(
        screens = config.screens.map { screen ->
            screen.copy(
                cells = screen.cells.map { cell ->
                    if (cell.button.action is ButtonAction.Widget) {
                        cell.copy(button = Button())
                    } else {
                        cell
                    }
                },
            )
        },
        apps = config.apps.copy(recent = emptyList()),
    )

    fun export(config: LauncherConfig): String = json.encodeToString(
        LauncherConfig.serializer(),
        strippedForTransfer(config),
    )

    /**
     * Stammt diese Sicherung aus einer neueren Fassung von BigLau?
     *
     * Dann enthaelt sie Felder, die diese Fassung nicht kennt - und `ignoreUnknownKeys`
     * wirft sie beim Einlesen wortlos weg. Wer eine Sicherung vom neuen Telefon auf ein
     * altes zurueckspielt, verliert also Einstellungen, ohne dass irgendetwas es sagt. Das
     * ist genau der Fall, fuer den das Feld `version` in jeder Datei steht; bis hierher
     * hat es niemand gelesen.
     */
    fun isFromNewerVersion(text: String): Boolean = runCatching {
        val root = json.parseToJsonElement(text) as? kotlinx.serialization.json.JsonObject
        val version = (root?.get("version") as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toIntOrNull()
        version != null && version > CONFIG_VERSION
    }.getOrDefault(false)

    /**
     * Null, wenn der Text keine brauchbare Konfiguration ist - der Aufrufer sagt das dann.
     *
     * Wichtig: es reicht nicht, dass sich der Text irgendwie einlesen laesst. Weil jedes Feld
     * einen Vorgabewert hat, ergaebe `{}` klaglos die Werkseinstellung - der Import wuerde
     * die gesamte Belegung des Nutzers loeschen und dabei aussehen, als haette er geklappt.
     * Die Datei muss ihre Screens deshalb ausdruecklich mitbringen.
     */
    fun import(text: String): LauncherConfig? = runCatching {
        val root = json.parseToJsonElement(text) as? kotlinx.serialization.json.JsonObject
            ?: return null
        val screens = root["screens"] as? kotlinx.serialization.json.JsonArray ?: return null
        if (screens.isEmpty()) return null

        val loaded = json.decodeFromString<LauncherConfig>(text)
        if (loaded.screens.isEmpty()) return null
        // Zeigt der Startscreen ins Leere, nimm den ersten - sonst startet nichts.
        if (loaded.screenById(loaded.homeScreenId) == null) {
            loaded.copy(homeScreenId = loaded.screens.first().id)
        } else {
            loaded
        }
    }.getOrNull()

    /** Dateiname mit Datum, damit mehrere Sicherungen nebeneinander liegen koennen. */
    fun suggestedFileName(epochMillis: Long): String {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(epochMillis))
        return "biglau-$date.json"
    }
}
