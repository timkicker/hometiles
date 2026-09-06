package dev.kicker.hometiles.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * exporting and importing the config, for moving to another phone.
 *
 * the file has to stand on its own: there is no old hometiles on the target device to fill in
 * gaps. so everything is written, defaults included, with the schema version.
 */
object ConfigTransfer {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /**
     * widget ids come from the *old* device's widget host; on the new one they point at
     * nothing or, worse, at somebody else's widget. those tiles are emptied rather than
     * transferred broken. recently used apps are device-bound too and add nothing.
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
     * a backup from a newer release carries fields this one does not know, and
     * `ignoreUnknownKeys` drops them without a word. that is what the `version` field is for.
     */
    fun isFromNewerVersion(text: String): Boolean = runCatching {
        val root = json.parseToJsonElement(text) as? JsonObject
        val version = (root?.get("version") as? JsonPrimitive)?.content?.toIntOrNull()
        version != null && version > CONFIG_VERSION
    }.getOrDefault(false)

    /**
     * null when the text is no usable config.
     *
     * parsing alone is not enough: every field has a default, so `{}` would yield the
     * factory setting without complaint, and the import would wipe the user's whole layout
     * while looking as if it had worked. the file must bring its screens explicitly.
     */
    fun import(text: String): LauncherConfig? = runCatching {
        val root = json.parseToJsonElement(text) as? JsonObject ?: return null
        val screens = root["screens"] as? JsonArray ?: return null
        if (screens.isEmpty()) return null

        val parsed = json.decodeFromString<LauncherConfig>(text)
        if (parsed.screens.isEmpty()) return null
        // the version is set to our own: what arrived is from now on exactly what this
        // release understands. leaving a higher number would make **every later backup**
        // claim it came from a newer hometiles, and the warning would show for good.
        val loaded = parsed.copy(version = CONFIG_VERSION)
        // a home screen pointing at nothing would start nothing.
        if (loaded.screenById(loaded.homeScreenId) == null) {
            loaded.copy(homeScreenId = loaded.screens.first().id)
        } else {
            loaded
        }
    }.getOrNull()

    /** dated, so several backups can lie side by side. */
    fun suggestedFileName(epochMillis: Long): String {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(epochMillis))
        return "hometiles-$date.json"
    }
}
