package dev.kicker.hometiles.settings

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import dev.kicker.hometiles.data.ConfigStore
import dev.kicker.hometiles.R
import dev.kicker.hometiles.notify.NotificationRepository
import dev.kicker.hometiles.phone.PhoneNumbers
import dev.kicker.hometiles.notify.SystemPackagesReader
import dev.kicker.hometiles.safety.CrashRecorder

/**
 * what the app knows about its own situation. there is no adb on that phone, and
 * "it does not work" without numbers cannot be answered.
 */
object Diagnostics {

    /**
     * what is left after the system bars, in dp.
     *
     * the trap sits in the starting number: `resources.displayMetrics` gives the window
     * *without* the gesture bar (480 x 832 instead of 480 x 854 on the jelly 2), so
     * subtracting the insets from it removes that bar twice and reports 565 dp where 581
     * are usable. the *full* screen size goes in here, and the insets come off once.
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
        val width = (fullWidthPx - left - right).coerceAtLeast(0)
        val height = (fullHeightPx - top - bottom).coerceAtLeast(0)
        return (width / density).toInt() to (height / density).toInt()
    }

    /**
     * @param usableDp what is left after the status and gesture bars, `null` if unknown.
     * @param text resolves a string; hard-written labels made exactly the page one opens
     *   when something is wrong unreadable on a phone in another language.
     */
    fun collect(
        context: Context,
        usableDp: Pair<Int, Int>?,
        text: (Int) -> String,
    ): List<Pair<String, String>> {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val windowWidthDp = (metrics.widthPixels * 160f / metrics.densityDpi).toInt()
        val windowHeightDp = (metrics.heightPixels * 160f / metrics.densityDpi).toInt()

        return buildList {
            add(text(R.string.diag_window) to "${metrics.widthPixels} × ${metrics.heightPixels} px")
            add(
                text(R.string.diag_density) to
                    "${metrics.densityDpi} dpi (${"%.3f".format(metrics.density)}×)",
            )
            add(text(R.string.diag_window_dp) to "$windowWidthDp × $windowHeightDp dp")
            // the line that matters when designing: the window is not what a tile gets.
            usableDp?.let { (width, height) ->
                add(text(R.string.diag_usable) to "$width × $height dp")
            }
            add(text(R.string.diag_font_scale) to "%.2f".format(context.resources.configuration.fontScale))
            add(text(R.string.diag_android) to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            add(text(R.string.diag_device) to "${Build.MANUFACTURER} ${Build.MODEL}")
            // whether the system spelling is hooked up shows on a number only if its
            // country is known: "+436 641 110 01" against "+43 664 111001" is the difference
            // between copying it right and wrong. see SystemNumbers.install.
            add(
                // the country's name, not its code: the code does not answer the question
                // someone has on this page.
                //
                // without a country there is no spelling. `region` is null without a sim,
                // and then that is what stands there rather than the sample number this
                // check uses.
                text(R.string.diag_number_format) to (
                    PhoneNumbers.region
                        ?.takeIf { PhoneNumbers.systemFormat("+15550100", it) != null }
                        ?.let { code ->
                            java.util.Locale("", code)
                                .getDisplayCountry(context.resources.configuration.locales[0])
                                .takeIf { name -> name.isNotBlank() }
                                ?: code
                        }
                        ?.let { country -> String.format(text(R.string.diag_number_country), country) }
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
            // read and write apart: the call log was visible but nothing in it could be
            // deleted, because only the write right was missing.
            add(text(R.string.diag_read_call_log) to yesNo(granted(context, Manifest.permission.READ_CALL_LOG), text))
            add(text(R.string.diag_write_call_log) to yesNo(granted(context, Manifest.permission.WRITE_CALL_LOG), text))
            add(text(R.string.diag_location) to yesNo(granted(context, Manifest.permission.ACCESS_FINE_LOCATION), text))
            // the signal bars hang on READ_PHONE_STATE, and this line was missing: anyone
            // asking why the signal tile stays empty found nothing here.
            add(text(R.string.diag_signal) to yesNo(granted(context, Manifest.permission.READ_PHONE_STATE), text))
            // why does the unread reminder come late? the alarm runs over
            // `setAndAllowWhileIdle` and is throttled in doze, which at short intervals
            // looks like a fault and is none. see MessageReminderReceiver.
            val saving = runCatching {
                context.getSystemService(android.os.PowerManager::class.java)
                    ?.isIgnoringBatteryOptimizations(context.packageName)
            }.getOrNull()
            add(
                text(R.string.diag_battery_saving) to when (saving) {
                    true -> text(R.string.diag_battery_saving_off)
                    false -> text(R.string.diag_battery_saving_on)
                    // not none: here the answer failed to arrive, the thing is not absent.
                    null -> text(R.string.diag_unknown)
                },
            )
            // a rescued config file is announced by the wizard exactly once; after that
            // nobody knows about it although the old settings are still there.
            add(text(R.string.diag_rescued) to yesNo(ConfigStore.get(context).hasRescuedFile, text))
            val recorder = CrashRecorder.get(context)
            // is the net still hanging? a net nobody can tell is hanging is no net.
            add(text(R.string.diag_crash_net) to yesNo(recorder.armed(), text))
            add(text(R.string.diag_failed_starts) to recorder.failedStarts.toString())
            add(
                text(R.string.diag_last_crash) to
                    (recorder.lastCrash()?.lineSequence()?.firstOrNull() ?: text(R.string.diag_crash_none)),
            )
        }
    }

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** does HomeTiles hold the home role? the settings ask this too. */
    fun isDefaultHome(context: Context): Boolean = holdsHomeRole(context)

    private fun holdsHomeRole(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roles = context.getSystemService(RoleManager::class.java) ?: return false
        return roles.isRoleAvailable(RoleManager.ROLE_HOME) && roles.isRoleHeld(RoleManager.ROLE_HOME)
    }

    private fun yesNo(value: Boolean, text: (Int) -> String) =
        text(if (value) R.string.diag_yes else R.string.diag_no)
}
