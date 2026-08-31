package org.biglau.actions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import org.biglau.R
import org.biglau.data.SosConfig
import org.biglau.toggles.SosMessage

data class SosResult(val sent: Int, val failed: Int, val hadLocation: Boolean) {
    val ok: Boolean get() = sent > 0
}

/**
 * Schickt eine Notruf-SMS mit Standort an die hinterlegten Nummern.
 * Bewusst ohne Google Play Services: LocationManager reicht fuer die letzte bekannte Position.
 */
object Sos {

    fun send(context: Context, config: SosConfig): SosResult {
        if (config.numbers.isEmpty()) return SosResult(0, 0, false)
        if (!hasPermission(context, Manifest.permission.SEND_SMS)) return SosResult(0, config.numbers.size, false)

        val location = if (config.sendLocation) lastKnownLocation(context) else null
        // Bewusst ueber SosMessage und nicht hier zusammengebaut: die Koordinaten muessen
        // einen Punkt als Trennzeichen haben. Mit der Systemsprache formatiert stuende auf
        // einem deutschen Telefon "47,26543" im Link - und der Empfaenger koennte ihn nicht
        // oeffnen. Ausgerechnet in der Notruf-SMS.
        val text = SosMessage.compose(
            text = config.message,
            latitude = location?.latitude,
            longitude = location?.longitude,
            fallback = context.getString(R.string.sos_message_default),
        )

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
        return SosResult(sent, failed, location != null)
    }

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
