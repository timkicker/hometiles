package org.biglau.settings

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import org.biglau.data.ConfigStore
import org.biglau.R
import org.biglau.notify.NotificationRepository
import org.biglau.phone.PhoneNumbers
import org.biglau.notify.SystemPackagesReader
import org.biglau.safety.CrashRecorder

/**
 * Was die App ueber ihre eigene Lage weiss. Beim Bauen kostete jede dieser Zahlen einen
 * adb-Aufruf; auf dem Geraet des Nutzers gibt es kein adb, und "es geht nicht" ohne Zahlen
 * ist nicht zu beantworten.
 */
object Diagnostics {

    /**
     * Was nach den Systemleisten uebrig bleibt, in dp.
     *
     * **Die Falle steckt in der Ausgangszahl.** `resources.displayMetrics` liefert das
     * Fenster *ohne* die Gestenleiste (am Jelly 2 480 x 832 statt 480 x 854). Zieht man
     * davon die Einblendungen ab, geht die Gestenleiste **zweimal** weg, und die Seite
     * meldet 565 dp, wo 581 nutzbar sind. Genau diese 565 standen deshalb auch in
     * `PLAN.md` 3.2 - eine Zahl, die sich selbst bestaetigt hat, weil sie an beiden Stellen
     * gleich falsch gerechnet war.
     *
     * Hier gehen deshalb die **ganzen** Bildschirmmasse hinein, und die Einblendungen
     * genau einmal ab.
     */
    fun usableDp(
        fullWidthPx: Int,
        fullHeightPx: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        density: Float,
    ): Pair<Int, Int>? {
        if (density <= 0f) return null
        val breite = (fullWidthPx - left - right).coerceAtLeast(0)
        val hoehe = (fullHeightPx - top - bottom).coerceAtLeast(0)
        return (breite / density).toInt() to (hoehe / density).toInt()
    }

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
            // Ob die Systemschreibweise eingehaengt ist, sieht man einer Nummer nur an,
            // wenn man ihr Land kennt: „+436 804 …" statt „+43 680 1234567" ist der
            // Unterschied zwischen richtig und falsch abgeschrieben. Ohne diese Zeile ist
            // das ein stiller Rueckschritt - siehe SystemNumbers.install.
            add(
                // Nicht die Landeskennung ("at"), sondern der Name des Landes: die Kennung
                // beantwortet die Frage nicht, die jemand auf dieser Seite hat, und gross
                // geschrieben waere sie schlecht zu lesen (PlainLanguageTest hat genau das
                // abgefangen).
                //
                // **Ohne Land keine Schreibweise.** Ohne SIM ist `region` null; die
                // Systemformatierung kann dann nichts ausrichten, und genau das steht dann
                // auch da. Die erste Fassung haette in diesem Fall die Beispielnummer
                // hingeschrieben, mit der hier geprueft wird - eine amerikanische, die mit
                // dem Telefon nichts zu tun hat.
                text(R.string.diag_number_format) to (
                    PhoneNumbers.region
                        ?.takeIf { PhoneNumbers.systemFormat("+15550100", it) != null }
                        ?.let { kennung ->
                            java.util.Locale("", kennung)
                                .getDisplayCountry(context.resources.configuration.locales[0])
                                .takeIf { name -> name.isNotBlank() }
                                ?: kennung
                        }
                        ?.let { land -> String.format(text(R.string.diag_number_country), land) }
                        ?: text(R.string.diag_number_plain)
                    ),
            )
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
            // Warum kommt die Erinnerung an ungelesene Nachrichten spät? Der Wecker läuft
            // über `setAndAllowWhileIdle` und wird im Doze deshalb zwar geweckt, aber
            // gedrosselt - bei kurzen Abständen sieht das nach einem Fehler aus und ist
            // keiner. Siehe MessageReminderReceiver.
            val sparen = runCatching {
                context.getSystemService(android.os.PowerManager::class.java)
                    ?.isIgnoringBatteryOptimizations(context.packageName)
            }.getOrNull()
            add(
                text(R.string.diag_battery_saving) to when (sparen) {
                    true -> text(R.string.diag_battery_saving_off)
                    false -> text(R.string.diag_battery_saving_on)
                    null -> text(R.string.diag_none)
                },
            )
            // Gehört zu den Fehlerspuren: wenn eine unlesbare Einstellungsdatei beiseite
            // gelegt wurde, sagt das der Assistent genau einmal - danach weiss niemand
            // mehr davon, obwohl die alten Einstellungen noch da sind.
            add(text(R.string.diag_rescued) to yesNo(ConfigStore.get(context).hasRescuedFile, text))
            val recorder = CrashRecorder.get(context)
            // Haengt das Netz ueberhaupt noch? Am 3.9.2026 stand hier „letzter Absturz:
            // keiner", obwohl es Stunden vorher einen gab. Ein Netz, von dem niemand weiss,
            // ob es haengt, ist kein Netz - deshalb steht es jetzt daneben.
            add(text(R.string.diag_crash_net) to yesNo(recorder.armed(), text))
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
