package org.biglau.data

import android.content.Context
import org.biglau.tiles.CellLayout
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

    /**
     * Liegt eine gerettete Einstellungsdatei daneben?
     *
     * `ConfigFile` legt die unlesbare Datei als `config.json.unreadable` beiseite, statt sie
     * wegzuwerfen, und der Assistent sagt beim nächsten Start einmal Bescheid. Danach
     * erwähnt sie **nie wieder** jemand — sie liegt im privaten Speicher der App, wo weder
     * der Nutzer noch jemand, der ihm hilft, sie findet. Die Diagnoseseite zeigt sie jetzt
     * an; das ist die Seite, die man aufschlägt, wenn Einstellungen verschwunden sind.
     */
    val hasRescuedFile: Boolean get() = storage.rescueFile.exists()

    private val _config = MutableStateFlow(storage.read())
    val config: StateFlow<LauncherConfig> = _config.asStateFlow()

    /**
     * War die gespeicherte Einrichtung beim Start unlesbar?
     *
     * Dann steht hier eine Vorgabe statt der eigenen Belegung, und das darf nicht stumm
     * geschehen: von aussen sieht es aus wie ein frisch installiertes BigLau. Die alte
     * Datei liegt daneben, siehe [ConfigFile.rescueFile].
     */
    val startedFromBrokenFile: Boolean = storage.rescuedBroken

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

    // Die beiden Rechnungen dahinter stehen in `CellLayout`, im Modul ohne Android: dieses
    // Modul hier hatte als einziges keinen Test, und es sind die Rechnungen, die eine Kachel
    // des Nutzers ueberschreiben oder loeschen.

    /** Setzt die Zelle, die (x, y) ueberdeckt, oder legt eine neue 1x1-Zelle dort an. */
    fun setButton(screenId: String, x: Int, y: Int, button: Button) = updateScreen(screenId) {
        CellLayout.withButton(it, x, y, button)
    }

    /** Leert einen Platz. */
    fun clearButton(screenId: String, x: Int, y: Int) = updateScreen(screenId) {
        CellLayout.withoutButton(it, x, y)
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
