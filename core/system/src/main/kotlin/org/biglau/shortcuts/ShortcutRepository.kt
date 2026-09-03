package org.biglau.shortcuts

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserHandle

/**
 * Verknuepfungen einer App.
 *
 * Android gibt sie nur an den Standard-Launcher heraus. Solange BigLau das nicht ist, liefert
 * [available] false und die Oberflaeche sagt das - statt eine leere Liste zu zeigen, aus der
 * niemand schliessen kann, ob die App keine hat oder wir nicht fragen duerfen.
 */
class ShortcutRepository(context: Context) {

    private val appContext = context.applicationContext
    private val launcherApps =
        appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val user: UserHandle = Process.myUserHandle()

    /** Duerfen wir ueberhaupt fragen? */
    fun available(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 &&
            runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)

    /**
     * Die Verknuepfungen einer App - oder [ShortcutAnswer.Failed], wenn wir nicht fragen
     * durften oder Android nichts geantwortet hat. `getShortcuts` liefert selbst null,
     * wenn die Berechtigung fehlt; auch das ist keine Antwort, sondern keine.
     */
    fun forPackage(packageName: String): ShortcutAnswer {
        if (!available()) return ShortcutAnswer.Failed
        val query = LauncherApps.ShortcutQuery()
            .setPackage(packageName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        val infos = runCatching { launcherApps.getShortcuts(query, user) }.getOrNull()
            ?: return ShortcutAnswer.Failed
        return ShortcutAnswer.Rows(Shortcuts.usable(infos.map { it.toRow() }))
    }

    fun launch(packageName: String, shortcutId: String): Boolean = runCatching {
        launcherApps.startShortcut(packageName, shortcutId, null, null, user)
        true
    }.getOrDefault(false)

    fun iconFor(packageName: String, shortcutId: String, density: Int): Drawable? {
        if (!available()) return null
        val query = LauncherApps.ShortcutQuery()
            .setPackage(packageName)
            .setShortcutIds(listOf(shortcutId))
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        val info = runCatching { launcherApps.getShortcuts(query, user) }.getOrNull()?.firstOrNull()
            ?: return null
        return runCatching { launcherApps.getShortcutIconDrawable(info, density) }.getOrNull()
    }

    private fun ShortcutInfo.toRow() = ShortcutRow(
        packageName = `package`,
        id = id,
        shortLabel = shortLabel?.toString().orEmpty(),
        longLabel = longLabel?.toString(),
        enabled = isEnabled,
        rank = rank,
        kind = when {
            isPinned -> ShortcutKind.PINNED
            isDynamic -> ShortcutKind.DYNAMIC
            else -> ShortcutKind.STATIC
        },
    )

    companion object {
        @Volatile
        private var instance: ShortcutRepository? = null

        fun get(context: Context): ShortcutRepository =
            instance ?: synchronized(this) {
                instance ?: ShortcutRepository(context).also { instance = it }
            }
    }
}
