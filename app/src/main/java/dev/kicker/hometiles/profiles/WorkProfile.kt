package dev.kicker.hometiles.profiles

import android.content.Context
import android.content.Intent
import android.provider.Settings
import dev.kicker.hometiles.R
import dev.kicker.hometiles.actions.Intents
import dev.kicker.hometiles.apps.AppRepository
import dev.kicker.hometiles.apps.LaunchableApp
import dev.kicker.hometiles.ui.AppLocale
import dev.kicker.hometiles.ui.Notice

/**
 * apps in a work profile (issue #6).
 *
 * the badge android draws on the icon is not enough on its own: icons can be switched off, a
 * screen reader does not see it, and two rows reading "Chrome" one under the other say
 * nothing about which is which. so the name says it too.
 */
object WorkProfile {

    fun title(context: Context, label: String, profile: Long?, paused: Boolean = false): String = when {
        profile == null -> label
        paused -> AppLocale.forApp(context).getString(R.string.app_in_paused_work_profile, label)
        else -> AppLocale.forApp(context).getString(R.string.app_in_work_profile, label)
    }

    fun title(context: Context, app: LaunchableApp): String =
        title(context, app.label, app.profile, app.paused)

    /**
     * a tap on an app whose profile is paused.
     *
     * the tap is the request: from android 9 on the launcher switches the profile on itself,
     * and the system asks for the work pin where there is one. below, or when android says
     * no, the settings open, rather than a tap that does nothing.
     */
    fun askToUnpause(context: Context, apps: AppRepository, profile: Long?) {
        if (apps.unpause(profile)) {
            Notice.show(context, R.string.work_apps_turning_on)
            return
        }
        Notice.show(context, R.string.work_apps_paused)
        // the page with the switch for work apps on android 8.
        Intents.open(context, Intent(Settings.ACTION_SYNC_SETTINGS))
    }
}
