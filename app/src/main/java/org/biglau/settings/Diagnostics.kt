package org.biglau.settings

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import org.biglau.R
import org.biglau.notify.NotificationRepository
import org.biglau.notify.SystemPackagesReader
import org.biglau.safety.CrashRecorder

/**
 * Was die App ueber ihre eigene Lage weiss. Beim Bauen kostete jede dieser Zahlen einen
 * adb-Aufruf; auf dem Geraet des Nutzers gibt es kein adb, und "es geht nicht" ohne Zahlen
 * ist nicht zu beantworten.
 */
object Diagnostics {

    /**
     * @param usableDp was nach Statusleiste und Gestenleiste uebrig bleibt - `null`, wenn
     *   es nicht zu ermitteln war.
     * @param text loest eine Zeichenkette auf. Die Beschriftungen standen hier einmal fest
     *   auf Deutsch; auf einem englischen Geraet war ausgerechnet die Seite unlesbar, die
     *   man aufschlaegt, wenn etwas nicht geht.
     */
    fun collect(
        context: Context,
        usableDp: Pair<Int, Int>?,
        text: (Int) -> String,
    ): List<Pair<String, String>> {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val fensterBreiteDp = (metrics.widthPixels * 160f / metrics.densityDpi).toInt()
        val fensterHoeheDp = (metrics.heightPixels * 160f / metrics.densityDpi).toInt()

        return buildList {
            add(text(R.string.diag_window) to "${metrics.widthPixels} × ${metrics.heightPixels} px")
            add(
                text(R.string.diag_density) to
                    "${metrics.densityDpi} dpi (${"%.3f".format(metrics.density)}×)",
            )
            add(text(R.string.diag_window_dp) to "$fensterBreiteDp × $fensterHoeheDp dp")
            // Die Zeile, auf die es beim Entwerfen ankommt: das Fenster ist nicht das,
            // was eine Kachel bekommt. Statusleiste und Gestenleiste gehen noch ab.
            usableDp?.let { (breite, hoehe) ->
                add(text(R.string.diag_usable) to "$breite × $hoehe dp")
            }
            add(text(R.string.diag_font_scale) to "%.2f".format(context.resources.configuration.fontScale))
            add(text(R.string.diag_android) to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            add(text(R.string.diag_device) to "${Build.MANUFACTURER} ${Build.MODEL}")
            add(text(R.string.diag_home_role) to yesNo(holdsHomeRole(context), text))
            add(text(R.string.diag_notification_access) to yesNo(NotificationRepository.isEnabled(context), text))
            val system = SystemPackagesReader.read(context)
            add(text(R.string.diag_default_sms) to (system.sms ?: text(R.string.diag_none)))
            add(text(R.string.diag_default_dialer) to (system.dialer ?: text(R.string.diag_none)))
            add(text(R.string.diag_contacts) to yesNo(granted(context, Manifest.permission.READ_CONTACTS), text))
            add(text(R.string.diag_calls) to yesNo(granted(context, Manifest.permission.CALL_PHONE), text))
            add(text(R.string.diag_send_sms) to yesNo(granted(context, Manifest.permission.SEND_SMS), text))
            add(text(R.string.diag_read_sms) to yesNo(granted(context, Manifest.permission.READ_SMS), text))
            // Lesen und Schreiben getrennt: genau daran hing die Sackgasse vom 02.09.2026 -
            // die Anrufliste war zu sehen, aber nichts daraus zu loeschen, weil das
            // Schreibrecht fehlte. Wer diese Seite aufschlaegt, soll den Unterschied sehen.
            add(text(R.string.diag_read_call_log) to yesNo(granted(context, Manifest.permission.READ_CALL_LOG), text))
            add(text(R.string.diag_write_call_log) to yesNo(granted(context, Manifest.permission.WRITE_CALL_LOG), text))
            add(text(R.string.diag_location) to yesNo(granted(context, Manifest.permission.ACCESS_FINE_LOCATION), text))
            val recorder = CrashRecorder.get(context)
            add(text(R.string.diag_failed_starts) to recorder.failedStarts.toString())
            add(
                text(R.string.diag_last_crash) to
                    (recorder.lastCrash()?.lineSequence()?.firstOrNull() ?: text(R.string.diag_none)),
            )
        }
    }

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** Haelt BigLau die Startbildschirm-Rolle? Auch die Einstellungen fragen danach. */
    fun isDefaultHome(context: Context): Boolean = holdsHomeRole(context)

    private fun holdsHomeRole(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roles = context.getSystemService(RoleManager::class.java) ?: return false
        return roles.isRoleAvailable(RoleManager.ROLE_HOME) && roles.isRoleHeld(RoleManager.ROLE_HOME)
    }

    private fun yesNo(value: Boolean, text: (Int) -> String) =
        text(if (value) R.string.diag_yes else R.string.diag_no)
}
