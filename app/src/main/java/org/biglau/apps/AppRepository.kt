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

    fun launch(packageName: String, activityName: String): Boolean = runCatching {
        launcherApps.startMainActivity(
            ComponentName(packageName, activityName),
            user,
            null,
            null,
        )
        true
    }.getOrElse {
        // Fallback, falls die Activity umbenannt wurde: normaler Launch-Intent.
        val intent = appContext.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }.isSuccess
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
