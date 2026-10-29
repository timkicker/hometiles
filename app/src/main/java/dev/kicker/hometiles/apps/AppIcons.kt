package dev.kicker.hometiles.apps

import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    fun of(repository: AppRepository, app: LaunchableApp): AppIcon {
        val key = AppDrawer.keyOf(app)
        var icon by remember(key) {
            val known = cache.get(key)
            mutableStateOf(
                when {
                    known != null -> AppIcon(known, loaded = true)
                    synchronized(none) { key in none } -> AppIcon(null, loaded = true)
                    else -> AppIcon(null, loaded = false)
                },
            )
        }
        LaunchedEffect(key) {
            if (!icon.loaded) icon = load(repository, app, key)
        }
        return icon
    }

    private suspend fun load(repository: AppRepository, app: LaunchableApp, key: String): AppIcon {
        val bitmap = withContext(Dispatchers.IO) {
            repository.iconFor(app.packageName, app.activityName, app.profile)
                ?.toBitmap(SIZE_PX, SIZE_PX)
                ?.asImageBitmap()
        }
        if (bitmap != null) cache.put(key, bitmap) else synchronized(none) { none.add(key) }
        return AppIcon(bitmap, loaded = true)
    }
}
