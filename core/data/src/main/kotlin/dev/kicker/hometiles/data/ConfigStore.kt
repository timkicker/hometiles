package dev.kicker.hometiles.data

import android.content.Context
import dev.kicker.hometiles.tiles.CellLayout
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

/** the launcher config as one json file in internal storage. one file, one flow, no room. */
class ConfigStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val storage = ConfigFile(File(appContext.filesDir, "config.json"))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** writes go one after another; two at once tore the file apart. */
    private val writeLock = Mutex()

    /**
     * is a rescued config file lying beside it?
     *
     * the wizard mentions it once at the next start and then never again, and the file lives
     * in the app's private storage where neither the user nor whoever helps them can find it.
     * the diagnostics page shows it, because that is the page one opens when settings have
     * disappeared.
     */
    val hasRescuedFile: Boolean get() = storage.rescueFile.exists()

    private val _config = MutableStateFlow(storage.read())
    val config: StateFlow<LauncherConfig> = _config.asStateFlow()

    /**
     * was the stored setup unreadable at startup?
     *
     * then these are defaults rather than the user's layout, and that must not happen
     * quietly: from outside it looks like a freshly installed hometiles.
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

    // the arithmetic behind both lives in `CellLayout`, in the module without android: this
    // one had no tests, and these are the calculations that overwrite or delete a user's tile.

    fun setButton(screenId: String, x: Int, y: Int, button: Button) = updateScreen(screenId) {
        CellLayout.withButton(it, x, y, button)
    }

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
