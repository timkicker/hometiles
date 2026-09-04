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
 * Warum nichts hinausging.
 *
 * Der Bildschirm sagte in jedem Fall nur „Es konnte nichts gesendet werden." - und das ist
 * auf dem Bildschirm, der im Notfall der letzte ist, zu wenig. Ob die Erlaubnis fehlt, ob
 * niemand eingetragen ist oder ob das Netz nicht mitspielte, sind drei verschiedene Dinge,
 * und nur beim ersten kann der Mensch davor etwas tun.
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
 * Schickt eine Notruf-SMS mit Standort an die hinterlegten Nummern.
 * Bewusst ohne Google Play Services: LocationManager reicht fuer die letzte bekannte Position.
 */
object Sos {

    /**
     * Welcher Satz zu welchem Ausgang gehoert.
     *
     * Als reine Zuordnung, damit sie geprueft werden kann - der Bildschirm selbst laesst
     * sich im Notfall schlecht ausprobieren.
     */
    fun failureText(failure: SosFailure): Int = when (failure) {
        SosFailure.NO_PERMISSION -> R.string.sos_failed_permission
        SosFailure.NO_NUMBERS -> R.string.sos_not_configured
        else -> R.string.sos_failed
    }


    /**
     * Der Text, der hinausginge - und ob ein Standort dabei ist.
     *
     * Steht fuer sich, weil die Probe ihn **zeigt**, ohne zu senden: wer den Notruf fuer
     * jemanden einrichtet, soll sehen koennen, was ankommt. Vorher liess sich das nur
     * herausfinden, indem man es abschickte.
     */
    fun compose(context: Context, config: SosConfig): Pair<String, Boolean> {
        val location = if (config.sendLocation) lastKnownLocation(context) else null
        // Bewusst ueber SosMessage und nicht hier zusammengebaut: die Koordinaten muessen
        // einen Punkt als Trennzeichen haben. Mit der Systemsprache formatiert stuende auf
        // einem deutschen Telefon "47,26543" im Link - und der Empfaenger koennte ihn nicht
        // oeffnen. Ausgerechnet in der Notruf-SMS.
        val texte = AppLocale.forApp(context)
        // Wie alt die Position ist, entscheidet, ob es dabeisteht. Siehe SosMessage.ageNote.
        val alter = location?.let { (System.currentTimeMillis() - it.time) / 60_000L }
        val hinweis = SosMessage.ageNote(alter)?.let { (einheit, wert) ->
            texte.resources.getQuantityString(
                when (einheit) {
                    SosMessage.AgeUnit.MINUTES -> R.plurals.sos_location_age_minutes
                    SosMessage.AgeUnit.HOURS -> R.plurals.sos_location_age_hours
                },
                wert,
                wert,
            )
        }
        val text = SosMessage.compose(
            text = config.message,
            latitude = location?.latitude,
            longitude = location?.longitude,
            // In der Sprache der App: eine Notruf-SMS in einer Sprache, die der
            // Absender nicht spricht, waere der schlechteste Ort fuer diesen Fehler.
            fallback = texte.getString(R.string.sos_message_default),
            ageNote = hinweis,
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

    // `SmsManager.getDefault()` ist seit Android 12 abgelöst; auf dem Jelly 2
    // (Android 11) läuft genau dieser Zweig, ein Ersatz existiert dort nicht.
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
