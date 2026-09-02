package org.biglau.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val activityName: String,
) {
    val component: ComponentName get() = ComponentName(packageName, activityName)
}

/**
 * Liest die startbaren Apps ueber [LauncherApps]. Das ist der von Android
 * vorgesehene Weg fuer Launcher und kommt ohne QUERY_ALL_PACKAGES aus.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val launcherApps =
        appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val user: UserHandle = Process.myUserHandle()

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
     * Startet die App hinter einer Kachel.
     *
     * **Erst nachsehen, ob es die Activity ueberhaupt noch gibt.** `startMainActivity` wirft
     * bei einer unbekannten Komponente nicht immer - manchmal passiert einfach *nichts*, und
     * dann tut die Kachel nichts, ohne dass jemand etwas erfaehrt. Am Emulator gesehen: eine
     * Kachel mit dem Alias-Namen von Chrome blieb stumm, waehrend derselbe Name ueber einen
     * gewoehnlichen Intent startete.
     *
     * Der Fall tritt im Alltag ein, wenn eine App sich aktualisiert und ihre Startklasse
     * umbenennt. Die Kachel steht dann weiter da und fuehrt nirgendwohin.
     */
    fun launch(packageName: String, activityName: String): Boolean {
        val bekannt = runCatching {
            launcherApps.getActivityList(packageName, user)
                .any { it.componentName.className == activityName }
        }.getOrDefault(false)

        if (bekannt) {
            val gestartet = runCatching {
                launcherApps.startMainActivity(
                    ComponentName(packageName, activityName),
                    user,
                    null,
                    null,
                )
                true
            }.getOrDefault(false)
            if (gestartet) return true
        }

        // Sonst der gewoehnliche Startweg des Systems - der findet auch eine umbenannte
        // Startklasse.
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
