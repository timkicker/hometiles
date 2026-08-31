package org.biglau.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Haelt die Launcher-Konfiguration als JSON-Datei im internen Speicher.
 * Bewusst simpel: eine Datei, ein StateFlow, kein Room.
 */
class ConfigStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val storage = ConfigFile(File(appContext.filesDir, "config.json"))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Schreibvorgaenge nacheinander - zwei gleichzeitige zerlegten die Datei. */
    private val writeLock = Mutex()

    private val _config = MutableStateFlow(storage.read())
    val config: StateFlow<LauncherConfig> = _config.asStateFlow()

    val current: LauncherConfig get() = _config.value

    fun update(block: (LauncherConfig) -> LauncherConfig) {
        val next = block(_config.value)
        _config.value = next
        scope.launch {
            writeLock.withLock { runCatching { storage.write(next) } }
        }
    }

    fun updateScreen(id: String, block: (Screen) -> Screen) = update { cfg ->
        val index = cfg.screens.indexOfFirst { it.id == id }
        if (index < 0) return@update cfg
        cfg.copy(screens = cfg.screens.toMutableList().also { it[index] = block(it[index]) })
    }

    /** Setzt die Zelle, die (x, y) ueberdeckt, oder legt eine neue 1x1-Zelle dort an. */
    fun setButton(screenId: String, x: Int, y: Int, button: Button) = updateScreen(screenId) { screen ->
        val existing = screen.cellAt(x, y)
        val cells = screen.cells.toMutableList()
        if (existing == null) {
            cells.add(Cell(x = x, y = y, button = button))
        } else {
            cells[cells.indexOf(existing)] = existing.copy(button = button)
        }
        screen.copy(cells = cells)
    }

    /**
     * Leert einen Platz, indem die Zelle verschwindet - nicht, indem sie eine Zelle ohne
     * Aktion zuruecklaesst. Sonst gaebe es "leer" zweimal im Modell, und die zweite Sorte
     * blockiert stillschweigend das Vergroessern der Nachbarn.
     */
    fun clearButton(screenId: String, x: Int, y: Int) = updateScreen(screenId) { screen ->
        val existing = screen.cellAt(x, y) ?: return@updateScreen screen
        screen.copy(cells = screen.cells - existing)
    }

    companion object {
        @Volatile
        private var instance: ConfigStore? = null

        fun get(context: Context): ConfigStore =
            instance ?: synchronized(this) {
                instance ?: ConfigStore(context).also { instance = it }
            }
    }
}
