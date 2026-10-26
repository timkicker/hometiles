package dev.kicker.hometiles.apps

import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** an app's icon in a list: [loaded] false while it is still being fetched. */
class AppIcon(val bitmap: ImageBitmap?, val loaded: Boolean)

/**
 * the icons of the app lists, fetched off the main thread and kept.
 *
 * each row asked the system for its icon on the main thread while the list was drawn, and
 * scrolling or turning asked again (issue #3). 72 px at 4 bytes is 20 KB an icon, so the
 * cache holds a few hundred apps in a few megabytes.
 */
object AppIcons {

    private const val SIZE_PX = 72
    private val cache = LruCache<String, ImageBitmap>(300)

    /** apps without an icon, so they are not asked again either. */
    private val none = mutableSetOf<String>()

    @Composable
    fun remember(repository: AppRepository, app: LaunchableApp): AppIcon {
        val key = app.packageName + "/" + app.activityName
        val known = cache.get(key)
        val initial = when {
            known != null -> AppIcon(known, loaded = true)
            synchronized(none) { key in none } -> AppIcon(null, loaded = true)
            else -> AppIcon(null, loaded = false)
        }
        return produceState(initial, key) {
            if (value.loaded) return@produceState
            val bitmap = withContext(Dispatchers.IO) {
                repository.iconFor(app.packageName, app.activityName)
                    ?.toBitmap(SIZE_PX, SIZE_PX)
                    ?.asImageBitmap()
            }
            if (bitmap != null) cache.put(key, bitmap) else synchronized(none) { none.add(key) }
            value = AppIcon(bitmap, loaded = true)
        }.value
    }
}
