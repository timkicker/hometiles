package org.biglau.actions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import org.biglau.R
import org.biglau.ui.Notice

/** Zentrale Stelle fuer alle System-Intents, die eine Kachel ausloesen kann. */
object Intents {

    fun openCamera(context: Context) = start(context) {
        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
    }

    fun openClock(context: Context) = start(context) {
        Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
    }

    /**
     * Der Rechner - ueber die Kategorie, nicht ueber einen Namen.
     *
     * Eine feste Anwendungskennung waere herstellerabhaengig: der Rechner heisst auf jedem
     * zweiten Telefon anders. `CATEGORY_APP_CALCULATOR` fragt das System, welche App diese
     * Rolle ausfuellt - dieselbe Art, wie Kamera und Wecker schon geoeffnet werden. Gibt es
     * keine, sagt [start] das, statt still nichts zu tun.
     */
    fun openCalculator(context: Context) = start(context) {
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
    }

    fun call(context: Context, number: String) = start(context) {
        Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null))
    }

    fun dial(context: Context, number: String) = start(context) {
        Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
    }

    fun sms(context: Context, number: String) = start(context) {
        Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null))
    }

    /** Eine Webseite oeffnen - was der Nutzer als Standardbrowser gesetzt hat. */
    fun openLink(context: Context, url: String) = start(context) {
        Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
    }

    /** Die Systemeinstellungen von Android - nicht unsere. */
    fun androidSettings(context: Context) = start(context) {
        Intent(android.provider.Settings.ACTION_SETTINGS)
    }

    /** Die Seite dieser App in den Systemeinstellungen - der einzige Weg zurueck, wenn
     *  Android eine Berechtigung nicht mehr abfragt. */
    fun appSettings(context: Context) = start(context) {
        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", context.packageName, null))
    }

    fun notificationListenerSettings(context: Context) = start(context) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    /**
     * Der Absichtsaufruf fuer die Startbildschirm-Frage, damit der Aufrufer auf das
     * Ergebnis warten kann. Wer den Balken antippt und zurueckkommt, muss den neuen
     * Zustand sehen - und den beantwortet Android im laufenden Prozess aus dem
     * Zwischenspeicher, also hilft nur ein Neuaufbau nach der Rueckkehr.
     */
    fun homeRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
            ?: return null
        if (!roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) return null
        if (roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) return null
        return roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
    }

    /** Oeffnet den Dialog zur Wahl des Standard-Launchers. */
    fun chooseHomeApp(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)
            ) {
                val intent = roleManager
                    .createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (runCatching { context.startActivity(intent) }.isSuccess) return
            }
        }
        start(context) { Intent(Settings.ACTION_HOME_SETTINGS) }
    }

    /**
     * Fragt die Telefon-Rolle an. Bewusst nur auf ausdrueckliche Handlung: wer sie annimmt,
     * gibt BigLau die Gespraechsansicht - und ein Fehler darin macht Telefonieren unmoeglich.
     */
    fun chooseDialerApp(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER)
            ) {
                val intent = roleManager
                    .createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (runCatching { context.startActivity(intent) }.isSuccess) return
            }
        }
        start(context) { Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS) }
    }

    private inline fun start(context: Context, build: () -> Intent) {
        val intent = build().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Notice.show(context, R.string.intent_no_app)
        } catch (e: SecurityException) {
            Notice.show(context, R.string.intent_no_permission)
        }
    }
}
