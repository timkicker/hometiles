package dev.kicker.hometiles.apps

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    /** serial of the profile, null for the main one. see AppKey. */
    val profile: Long? = null,
    /** its profile is switched off (work apps paused); a tap asks to switch it on. */
    val paused: Boolean = false,
) {
    val component: ComponentName get() = ComponentName(packageName, activityName)
}

/** what a tap on an app came to. */
enum class LaunchResult {
    STARTED,

    /** nothing to start any more: removed, or its profile is gone. */
    GONE,

    /** its profile is paused. see [AppRepository.unpause]. */
    PAUSED,
}

/**
 * the launchable apps via [LauncherApps]; the launcher path, free of QUERY_ALL_PACKAGES.
 *
 * every profile, not only the main one: the work profile's apps were simply missing until
 * issue #6, because every call asked for [main] alone.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val launcherApps =
        appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val users = appContext.getSystemService(Context.USER_SERVICE) as UserManager
    private val main: UserHandle = Process.myUserHandle()

    /** the main profile first, then work profiles. */
    private fun profiles(): List<UserHandle> =
        runCatching { users.userProfiles }.getOrDefault(listOf(main))
            .sortedBy { it != main }

    private fun serialOf(user: UserHandle): Long? =
        if (user == main) null else users.getSerialNumberForUser(user)

    private fun paused(user: UserHandle): Boolean =
        user != main && runCatching { users.isQuietModeEnabled(user) }.getOrDefault(false)

    /**
     * the profile behind a serial.
     *
     * a config moved to another phone names a serial that phone never had. then the app is
     * looked for in the other profiles, so a work tile finds the work app again, and the main
     * profile is never the answer: a work tile must not quietly open the private app.
     */
    private fun userFor(packageName: String, profile: Long?): UserHandle? {
        if (profile == null) return main
        users.getUserForSerialNumber(profile)?.let { return it }
        return profiles().filter { it != main }.firstOrNull { user ->
            runCatching { launcherApps.getActivityList(packageName, user).isNotEmpty() }.getOrDefault(false)
        }
    }

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
        // a work profile switched on or off, added or removed: its apps come, go or pause.
        val profileChanges = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { tick() }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        }
        // system broadcasts only, so nobody else needs to reach this receiver.
        ContextCompat.registerReceiver(appContext, profileChanges, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        tick()
        awaitClose {
            launcherApps.unregisterCallback(callback)
            runCatching { appContext.unregisterReceiver(profileChanges) }
        }
    }

    @Volatile
    private var lastApps: List<LaunchableApp>? = null

    /**
     * the list of the last [loadApps], at once; `null` before the first one.
     *
     * the app list showed nothing and stood still each time it opened and each time it was
     * turned, because it asked the system anew on the main thread (issue #3). it now shows
     * this and asks again in the background; an app removed in between is caught at the tap.
     */
    fun cachedApps(): List<LaunchableApp>? = lastApps

    /** [loadApps] off the main thread, for the lists that show [cachedApps] meanwhile. */
    suspend fun freshApps(): List<LaunchableApp> = withContext(Dispatchers.IO) { loadApps() }

    /**
     * asks the system; it takes a while with many apps, so not on the main thread.
     *
     * a paused profile may report no apps at all. then the ones seen last stay in the list,
     * marked [LaunchableApp.paused], instead of vanishing until the profile is back on.
     */
    fun loadApps(): List<LaunchableApp> {
        val before = lastApps.orEmpty()
        return profiles().flatMap { user ->
            val profile = serialOf(user)
            val paused = paused(user)
            val listed = runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
                .map {
                    LaunchableApp(
                        label = it.label.toString(),
                        packageName = it.componentName.packageName,
                        activityName = it.componentName.className,
                        profile = profile,
                        paused = paused,
                    )
                }
            if (paused && listed.isEmpty()) {
                before.filter { it.profile == profile }.map { it.copy(paused = true) }
            } else {
                listed
            }
        }
            .sortedWith(compareBy({ it.label.lowercase() }, { it.profile != null }))
            .also { lastApps = it }
    }

    /** the app's icon, with android's work badge when it lives in a work profile. */
    fun iconFor(packageName: String, activityName: String, profile: Long? = null): Drawable? = runCatching {
        val user = userFor(packageName, profile) ?: return null
        val component = ComponentName(packageName, activityName)
        launcherApps.getActivityList(packageName, user)
            .firstOrNull { it.componentName == component }
            ?.getBadgedIcon(0)
    }.getOrNull()

    fun labelFor(packageName: String, activityName: String, profile: Long? = null): String? = runCatching {
        val user = userFor(packageName, profile) ?: return null
        val component = ComponentName(packageName, activityName)
        launcherApps.getActivityList(packageName, user)
            .firstOrNull { it.componentName == component }
            ?.label
            ?.toString()
    }.getOrNull()

    /**
     * asks android to switch a paused profile back on.
     *
     * from android 9 on a launcher may do that itself, and the system asks for the work pin
     * where one is set. false means it could not: then the settings are the way.
     */
    fun unpause(profile: Long?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || profile == null) return false
        val user = users.getUserForSerialNumber(profile) ?: return false
        return runCatching {
            users.requestQuietModeEnabled(false, user)
            true
        }.getOrDefault(false)
    }

    /**
     * starts the app behind a tile.
     *
     * the activity is looked up first: `startMainActivity` does not always throw for an
     * unknown component, it sometimes does nothing at all, and then so does the tile. this
     * happens whenever an app update renames its launch class.
     */
    fun launch(packageName: String, activityName: String, profile: Long? = null): LaunchResult {
        val user = userFor(packageName, profile) ?: return LaunchResult.GONE
        if (paused(user)) return LaunchResult.PAUSED
        val activities = runCatching { launcherApps.getActivityList(packageName, user) }
            .getOrDefault(emptyList())
        val known = activities.any { it.componentName.className == activityName }

        if (known && start(ComponentName(packageName, activityName), user)) return LaunchResult.STARTED

        if (user != main) {
            // the system path below only knows the main profile and would open the private
            // app from a work tile. a renamed launch class is found in the profile's own list.
            val first = activities.firstOrNull()?.componentName ?: return LaunchResult.GONE
            return if (start(first, user)) LaunchResult.STARTED else LaunchResult.GONE
        }

        // otherwise the system path, which finds a renamed launch class too.
        val intent = appContext.packageManager.getLaunchIntentForPackage(packageName)
            ?: return LaunchResult.GONE
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (runCatching { appContext.startActivity(intent) }.isSuccess) {
            LaunchResult.STARTED
        } else {
            LaunchResult.GONE
        }
    }

    private fun start(component: ComponentName, user: UserHandle): Boolean = runCatching {
        launcherApps.startMainActivity(component, user, null, null)
        true
    }.getOrDefault(false)

    companion object {
        @Volatile
        private var instance: AppRepository? = null

        fun get(context: Context): AppRepository =
            instance ?: synchronized(this) {
                instance ?: AppRepository(context).also { instance = it }
            }
    }
}
