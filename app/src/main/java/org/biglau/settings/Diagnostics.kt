package org.biglau.settings

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import org.biglau.notify.NotificationRepository
import org.biglau.notify.SystemPackagesReader

/**
 * Was die App ueber ihre eigene Lage weiss. Beim Bauen kostete jede dieser Zahlen einen
 * adb-Aufruf; auf dem Geraet des Nutzers gibt es kein adb, und "es geht nicht" ohne Zahlen
 * ist nicht zu beantworten.
 */
object Diagnostics {

    fun collect(context: Context): List<Pair<String, String>> {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val widthDp = metrics.widthPixels * 160f / metrics.densityDpi
        val heightDp = metrics.heightPixels * 160f / metrics.densityDpi

        return buildList {
            add("Fensterfläche" to "${metrics.widthPixels} × ${metrics.heightPixels} px")
            add("Dichte" to "${metrics.densityDpi} dpi (Faktor ${"%.3f".format(metrics.density)})")
            add("Nutzbar" to "${widthDp.toInt()} × ${heightDp.toInt()} dp")
            add("Systemschrift" to "%.2f".format(context.resources.configuration.fontScale))
            add("Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            add("Gerät" to "${Build.MANUFACTURER} ${Build.MODEL}")
            add("Startbildschirm" to yesNo(holdsHomeRole(context)))
            add("Benachrichtigungszugriff" to yesNo(NotificationRepository.isEnabled(context)))
            val system = SystemPackagesReader.read(context)
            add("Standard-SMS-App" to (system.sms ?: "keine"))
            add("Standard-Telefon-App" to (system.dialer ?: "keine"))
            add("Kontakte" to yesNo(granted(context, Manifest.permission.READ_CONTACTS)))
            add("Anrufe" to yesNo(granted(context, Manifest.permission.CALL_PHONE)))
            add("SMS senden" to yesNo(granted(context, Manifest.permission.SEND_SMS)))
            add("Standort" to yesNo(granted(context, Manifest.permission.ACCESS_FINE_LOCATION)))
        }
    }

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun holdsHomeRole(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roles = context.getSystemService(RoleManager::class.java) ?: return false
        return roles.isRoleAvailable(RoleManager.ROLE_HOME) && roles.isRoleHeld(RoleManager.ROLE_HOME)
    }

    private fun yesNo(value: Boolean) = if (value) "ja" else "nein"
}
