package org.biglau.info

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Empfang und Netzart als Fluss - **rein lesend**.
 *
 * Es wird nichts gewählt, nichts gesendet und nichts angemeldet; die Klasse hört dem
 * Telefoniedienst zu und schweigt selbst. Ohne `READ_PHONE_STATE` meldet sie einen
 * Zustand ohne Karte, statt zu werfen.
 */
object SignalRepository {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    // `PhoneStateListener` statt `TelephonyCallback` - der Grund steht bei der
    // Anmeldung des Zuhörers weiter unten.
    @Suppress("DEPRECATION")
    fun readings(context: Context): Flow<SignalReading> = callbackFlow {
        val telefonie = context.getSystemService(TelephonyManager::class.java)
        if (telefonie == null || !hasPermission(context)) {
            trySend(unbekannt(darfLesen = telefonie != null && hasPermission(context)))
            awaitClose { }
            return@callbackFlow
        }

        var pegel = -1
        var imNetz = telefonie.simState == TelephonyManager.SIM_STATE_READY
        var roaming = telefonie.isNetworkRoaming

        fun melden() {
            trySend(
                SignalReading(
                    level = pegel,
                    mayRead = true,
                    hasSim = telefonie.simState == TelephonyManager.SIM_STATE_READY,
                    inService = imNetz,
                    roaming = roaming,
                    networkType = netzart(telefonie),
                ),
            )
        }

        // PhoneStateListener und nicht TelephonyCallback: das Geraet laeuft auf Android 11,
        // die Ablösung gibt es erst ab 12. Der Ersatz kommt, wenn minSdk dort ankommt.
        val zuhoerer = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(strength: SignalStrength?) {
                pegel = strength?.level ?: -1
                melden()
            }

            override fun onServiceStateChanged(state: ServiceState?) {
                imNetz = state?.state == ServiceState.STATE_IN_SERVICE
                roaming = state?.roaming ?: roaming
                melden()
            }
        }

        runCatching {
            telefonie.listen(
                zuhoerer,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or PhoneStateListener.LISTEN_SERVICE_STATE,
            )
        }
        melden()
        awaitClose {
            runCatching { telefonie.listen(zuhoerer, PhoneStateListener.LISTEN_NONE) }
        }
    }

    private fun unbekannt(darfLesen: Boolean) = SignalReading(
        level = -1,
        mayRead = darfLesen,
        hasSim = false,
        inService = false,
        roaming = false,
        networkType = "",
    )

    // `networkType` ist seit Android 11 abgelöst; das ist der Zweig für alles vor
    // Android 7, `dataNetworkType` steht im if darüber.
    @Suppress("DEPRECATION")
    private fun netzart(telefonie: TelephonyManager): String = runCatching {
        val typ = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            telefonie.dataNetworkType
        } else {
            telefonie.networkType
        }
        when (typ) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            -> "3G"
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_GPRS,
            -> "2G"
            else -> ""
        }
    }.getOrDefault("")
}
