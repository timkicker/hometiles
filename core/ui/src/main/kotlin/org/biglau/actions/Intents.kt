package org.biglau.actions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import org.biglau.core.ui.R
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

    /**
     * Eine Wähltastatur, die **nicht** BigLau ist - ohne Nummer.
     *
     * Fuer den Notfall-Auffang: wenn BigLau zweimal hintereinander nicht startet, ist die
     * eigene Wähltastatur genau das, worauf man sich nicht verlassen sollte. `ACTION_DIAL`
     * waehlt von sich aus nie - es oeffnet nur.
     *
     * **Bis zum 03.09.2026 stand hier ein nacktes `ACTION_DIAL`**, und darueber genau
     * dieser Satz. Ein nacktes `ACTION_DIAL` geht an die Standard-Telefon-App - und sobald
     * BigLau die ist, fuehrte der Notausgang zurueck in die App, die gerade zweimal
     * abgestuerzt war. Am Geraet fiel es nur deshalb nicht auf, weil das System an dem Tag
     * seinen eigenen Dialer nahm; verlassen kann man sich darauf nicht.
     *
     * Also wird ausdruecklich eine andere App gesucht. Gibt es keine, bleibt das nackte
     * Intent - eine Waehltastatur, die vielleicht BigLau ist, ist immer noch besser als
     * keine.
     */
    fun openDialer(context: Context) = start(context) {
        val nackt = Intent(Intent.ACTION_DIAL)
        val fremd = context.packageManager
            .queryIntentActivities(nackt, 0)
            .firstOrNull { it.activityInfo?.packageName != context.packageName }
            ?.activityInfo
        if (fremd != null) {
            Intent(nackt).setClassName(fremd.packageName, fremd.name)
        } else {
            nackt
        }
    }

    /** Die Kontakte des Systems - aus demselben Grund wie [openDialer]. */
    fun openContacts(context: Context) = start(context) {
        Intent(Intent.ACTION_VIEW, android.provider.ContactsContract.Contacts.CONTENT_URI)
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

    /**
     * Der Absichtsaufruf fuer die Telefon-Rolle - wie [homeRoleIntent], und aus demselben
     * Grund als Absicht statt als fertiger Start.
     *
     * **Ein Rollendialog muss mit `startActivityForResult` geoeffnet werden.** Sonst steht
     * dort kein Aufrufer, und der Dialog bricht ab, bevor er zu sehen ist: im Protokoll
     * „RequestRoleActivity: Package name cannot be null or empty: null", auf dem Bildschirm
     * gar nichts. Der Knopf sah aus, als haette man danebengetippt - und genau so war er
     * seit dem ersten Tag. Am Emulator gefunden.
     */
    fun dialerRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
            ?: return null
        if (!roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER)) return null
        return roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
    }

    /**
     * Fragt die Nachrichten-Rolle an.
     *
     * Dieselbe Ueberlegung wie bei [dialerRoleIntent]: der Dialog braucht einen Aufrufer.
     * Und dieselbe Zurueckhaltung - wer die Rolle annimmt, uebernimmt die Verantwortung,
     * eingehende Nachrichten selbst zu speichern; schreibt BigLau sie nicht, hat sie
     * niemand. Siehe `SmsDeliverReceiver`.
     */
    fun smsRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
            ?: return null
        if (!roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_SMS)) return null
        return roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_SMS)
    }

    /** Oeffnet den Dialog zur Wahl des Standard-Launchers. */
    fun chooseHomeApp(context: Context) {
        start(context) { Intent(Settings.ACTION_HOME_SETTINGS) }
    }

    /**
     * Fragt die Telefon-Rolle an. Bewusst nur auf ausdrueckliche Handlung: wer sie annimmt,
     * gibt BigLau die Gespraechsansicht - und ein Fehler darin macht Telefonieren unmoeglich.
     */
    fun chooseDialerApp(context: Context) {
        start(context) { Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS) }
    }

    /** Der Rueckfall, wenn es den Rollendialog nicht gibt: die Liste der Standard-Apps. */
    fun chooseSmsApp(context: Context) {
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
