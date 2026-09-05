package org.biglau.actions

import android.content.ActivityNotFoundException
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import org.biglau.core.ui.R
import org.biglau.ui.Notice

/** one place for every system intent a tile can trigger. */
object Intents {

    fun openCamera(context: Context) = start(context) {
        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
    }

    fun openClock(context: Context) = start(context) {
        Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
    }

    /** by category, not by package name: the calculator is called something else on every other phone. */
    fun openCalculator(context: Context) = start(context) {
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
    }

    fun call(context: Context, number: String) = start(context) {
        Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null))
    }

    /**
     * a keypad that is explicitly *not* BigLau, without a number. `ACTION_DIAL` never dials
     * by itself, it only opens.
     *
     * this is the safety net for two failed starts in a row, and a bare `ACTION_DIAL` goes
     * to the default phone app: once that is BigLau, the emergency exit led back into the
     * app that had just crashed twice. without another one the bare intent stays, since a
     * keypad that might be BigLau still beats none.
     */
    fun openDialer(context: Context) = start(context) {
        val bare = Intent(Intent.ACTION_DIAL)
        val other = context.packageManager
            .queryIntentActivities(bare, 0)
            .firstOrNull { it.activityInfo?.packageName != context.packageName }
            ?.activityInfo
        if (other != null) {
            Intent(bare).setClassName(other.packageName, other.name)
        } else {
            bare
        }
    }

    /** the system's contacts, for the same reason as [openDialer]. */
    fun openContacts(context: Context) = start(context) {
        Intent(Intent.ACTION_VIEW, android.provider.ContactsContract.Contacts.CONTENT_URI)
    }

    fun dial(context: Context, number: String) = start(context) {
        Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
    }

    fun sms(context: Context, number: String) = start(context) {
        Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null))
    }

    /** opens a page in whatever the user set as their browser. */
    fun openLink(context: Context, url: String) = start(context) {
        Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
    }

    /** android's own settings, not ours. */
    fun androidSettings(context: Context) = start(context) {
        Intent(android.provider.Settings.ACTION_SETTINGS)
    }

    /** this app's page in the system settings: the only way back once android stops asking. */
    fun appSettings(context: Context) = start(context) {
        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", context.packageName, null))
    }

    fun notificationListenerSettings(context: Context) = start(context) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    /**
     * the intent for the home role, so the caller can await the result: android answers the
     * new state from a cache within the running process, so only a rebuild shows it.
     */
    fun homeRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
            ?: return null
        if (!roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) return null
        if (roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) return null
        return roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
    }

    /**
     * like [homeRoleIntent]. a role dialog must be opened with `startActivityForResult`, or
     * there is no caller and it aborts unseen: the log says "Package name cannot be null or
     * empty: null" and the screen shows nothing, as if the button had been missed.
     */
    fun dialerRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
            ?: return null
        if (!roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER)) return null
        return roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
    }

    /**
     * the sms role. same as [dialerRoleIntent], and the same restraint: taking the role
     * means storing incoming messages ourselves, and unwritten they are nobody's. see
     * `SmsDeliverReceiver`.
     */
    fun smsRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
            ?: return null
        if (!roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_SMS)) return null
        return roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_SMS)
    }

    /** the dialog for choosing the default launcher. */
    fun chooseHomeApp(context: Context) {
        start(context) { Intent(Settings.ACTION_HOME_SETTINGS) }
    }

    /** only on an explicit move: the role hands BigLau the call screen, and a fault there ends calling. */
    fun chooseDialerApp(context: Context) {
        start(context) { Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS) }
    }

    /** the fallback when there is no role dialog: the list of default apps. */
    fun chooseSmsApp(context: Context) {
        start(context) { Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS) }
    }

    /**
     * starts an intent built elsewhere, for the cases where building belongs there: the
     * contact editor comes from `ContactRepository`. it is started here because the safety
     * net hangs here. the exception, not the way.
     */
    fun open(context: Context, intent: Intent) = start(context) { intent }

    /**
     * the activity behind a context, or `null`.
     *
     * in a composable `LocalContext.current` is rarely the activity itself but a wrapper
     * around it, so `context is Activity` would always be false there.
     */
    fun activityBehind(context: Context): Activity? {
        var here: Context? = context
        while (here is ContextWrapper) {
            if (here is Activity) return here
            here = here.baseContext
        }
        return null
    }

    private inline fun start(context: Context, build: () -> Intent) {
        // a new task only when there is none: started from a screen, the foreign app
        // belongs *in* BigLau's task, or back does not lead back and one ends up in the
        // system's contact list instead of in the large type.
        val intent = build().let {
            if (activityBehind(context) != null) it else it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Notice.show(context, R.string.intent_no_app)
        } catch (e: SecurityException) {
            Notice.show(context, R.string.intent_no_permission)
        }
    }
}
