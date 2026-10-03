package dev.kicker.hometiles.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val activityName: String,
) {
    val component: ComponentName get() = ComponentName(packageName, activityName)
}

/** the launchable apps via [LauncherApps]; the launcher path, free of QUERY_ALL_PACKAGES. */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val launcherApps =
        appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val user: UserHandle = Process.myUserHandle()

    /** shared by every listener, so a new one never starts at a number an old one had. */
    private val ticks = AtomicInteger()

    /**
     * ticks when an app is installed, removed or changed, and once whenever someone starts
     * listening. the once matters most: an app is usually removed while the home screen sits
     * in the background, where nobody listens, and the way back is when it has to ask again.
     */
    fun changes(): Flow<Int> = callbackFlow {
        val tick = { trySend(ticks.incrementAndGet()) }
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) { tick() }
            override fun onPackageAdded(packageName: String, user: UserHandle) { tick() }
            override fun onPackageChanged(packageName: String, user: UserHandle) { tick() }
            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) { tick() }
            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) { tick() }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        tick()
        awaitClose { launcherApps.unregisterCallback(callback) }
    }

    fun loadApps(): List<LaunchableApp> =
        launcherApps.getActivityList(null, user)
            .map {
                LaunchableApp(
                    label = it.label.toString(),
                    packageName = it.componentName.packageName,
                    activityName = it.componentName.className,
                )
            }
            .sortedBy { it.label.lowercase() }

    fun iconFor(packageName: String, activityName: String): Drawable? = runCatching {
        val component = ComponentName(packageName, activityName)
        launcherApps.getActivityList(packageName, user)
            .firstOrNull { it.componentName == component }
            ?.getBadgedIcon(0)
    }.getOrNull()

    fun labelFor(packageName: String, activityName: String): String? = runCatching {
        val component = ComponentName(packageName, activityName)
        launcherApps.getActivityList(packageName, user)
            .firstOrNull { it.componentName == component }
            ?.label
            ?.toString()
    }.getOrNull()

    /**
     * starts the app behind a tile.
     *
     * the activity is looked up first: `startMainActivity` does not always throw for an
     * unknown component, it sometimes does nothing at all, and then so does the tile. this
     * happens whenever an app update renames its launch class.
     */
    fun launch(packageName: String, activityName: String): Boolean {
        val known = runCatching {
            launcherApps.getActivityList(packageName, user)
                .any { it.componentName.className == activityName }
        }.getOrDefault(false)

        if (known) {
            val started = runCatching {
                launcherApps.startMainActivity(
                    ComponentName(packageName, activityName),
                    user,
                    null,
                    null,
                )
                true
            }.getOrDefault(false)
            if (started) return true
        }

        // otherwise the system path, which finds a renamed launch class too.
        val intent = appContext.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { appContext.startActivity(intent) }.isSuccess
    }

    companion object {
        @Volatile
        private var instance: AppRepository? = null

        fun get(context: Context): AppRepository =
            instance ?: synchronized(this) {
                instance ?: AppRepository(context).also { instance = it }
            }
    }
}
