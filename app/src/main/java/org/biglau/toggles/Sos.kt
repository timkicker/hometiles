package org.biglau.toggles

import org.biglau.actions.SosMessage
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import org.biglau.ui.AppLocale
import org.biglau.R
import org.biglau.data.SosConfig

/**
 * why nothing went out.
 *
 * one sentence for every case is too little on the screen that is the last one in an
 * emergency: a missing permission, nobody entered and a network that refused are three
 * different things, and only the first can be acted on.
 */
enum class SosFailure { NONE, NO_NUMBERS, NO_PERMISSION, SEND_FAILED }

data class SosResult(
    val sent: Int,
    val failed: Int,
    val hadLocation: Boolean,
    val failure: SosFailure = if (sent > 0) SosFailure.NONE else SosFailure.SEND_FAILED,
) {
    val ok: Boolean get() = sent > 0
}

/**
 * sends an emergency sms with the location to the stored numbers. no play services:
 * LocationManager is enough for the last known position.
 */
object Sos {

    /** a plain mapping so it can be tested: the screen itself is hard to try out in an emergency. */
    fun failureText(failure: SosFailure): Int = when (failure) {
        SosFailure.NO_PERMISSION -> R.string.sos_failed_permission
        SosFailure.NO_NUMBERS -> R.string.sos_not_configured
        else -> R.string.sos_failed
    }


    /**
     * the text that would go out, and whether a location is in it. stands on its own because
     * the preview shows it without sending: setting this up for someone should not require
     * sending it once to find out.
     */
    fun compose(context: Context, config: SosConfig): Pair<String, Boolean> {
        val location = if (config.sendLocation) lastKnownLocation(context) else null
        // built in SosMessage and not here: the coordinates need a dot as the separator,
        // and formatted in the system language a german phone would put "47,26543" in the
        // link, which the recipient could not open.
        val texts = AppLocale.forApp(context)
        // how old the position is decides whether it is mentioned. see SosMessage.ageNote.
        val age = location?.let { (System.currentTimeMillis() - it.time) / 60_000L }
        val note = SosMessage.ageNote(age)?.let { (unit, value) ->
            texts.resources.getQuantityString(
                when (unit) {
                    SosMessage.AgeUnit.MINUTES -> R.plurals.sos_location_age_minutes
                    SosMessage.AgeUnit.HOURS -> R.plurals.sos_location_age_hours
                },
                value,
                value,
            )
        }
        val text = SosMessage.compose(
            text = config.message,
            latitude = location?.latitude,
            longitude = location?.longitude,
            // in the app's language: an emergency sms in a language the sender does not
            // speak would be the worst place for that mistake.
            fallback = texts.getString(R.string.sos_message_default),
            ageNote = note,
        )
        return text to (location != null)
    }

    fun send(context: Context, config: SosConfig): SosResult {
        if (config.numbers.isEmpty()) return SosResult(0, 0, false, SosFailure.NO_NUMBERS)
        if (!hasPermission(context, Manifest.permission.SEND_SMS)) {
            return SosResult(0, config.numbers.size, false, SosFailure.NO_PERMISSION)
        }

        val (text, location) = compose(context, config)

        val sms = smsManager(context)
        var sent = 0
        var failed = 0
        config.numbers.forEach { number ->
            val ok = runCatching {
                val parts = sms.divideMessage(text)
                if (parts.size > 1) {
                    sms.sendMultipartTextMessage(number, null, parts, null, null)
                } else {
                    sms.sendTextMessage(number, null, text, null, null)
                }
            }.isSuccess
            if (ok) sent++ else failed++
        }
        return SosResult(sent, failed, location)
    }

    // on the jelly 2 (android 11) exactly this branch runs; no replacement exists there.
    @Suppress("DEPRECATION")
    private fun smsManager(context: Context): SmsManager =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }

    private fun lastKnownLocation(context: Context): Location? {
        if (!hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) &&
            !hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            return null
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        return runCatching {
            manager.getProviders(true)
                .mapNotNull { manager.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
        }.getOrNull()
    }

    private fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
