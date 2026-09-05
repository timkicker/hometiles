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
 * signal and network type as a flow, read only.
 *
 * without `READ_PHONE_STATE` it reports a state without a card instead of throwing.
 */
object SignalRepository {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    // `PhoneStateListener` and not `TelephonyCallback`; the reason is at the listener below.
    @Suppress("DEPRECATION")
    fun readings(context: Context): Flow<SignalReading> = callbackFlow {
        val telephony = context.getSystemService(TelephonyManager::class.java)
        if (telephony == null || !hasPermission(context)) {
            trySend(unknown(mayRead = telephony != null && hasPermission(context)))
            awaitClose { }
            return@callbackFlow
        }

        var signalLevel = -1
        var hasService = telephony.simState == TelephonyManager.SIM_STATE_READY
        var roaming = telephony.isNetworkRoaming

        fun report() {
            trySend(
                SignalReading(
                    level = signalLevel,
                    mayRead = true,
                    hasSim = telephony.simState == TelephonyManager.SIM_STATE_READY,
                    inService = hasService,
                    roaming = roaming,
                    networkType = netTypeOf(telephony),
                ),
            )
        }

        // the device runs android 11; the replacement exists only from 12 on.
        val listener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(strength: SignalStrength?) {
                signalLevel = strength?.level ?: -1
                report()
            }

            override fun onServiceStateChanged(state: ServiceState?) {
                hasService = state?.state == ServiceState.STATE_IN_SERVICE
                roaming = state?.roaming ?: roaming
                report()
            }
        }

        runCatching {
            telephony.listen(
                listener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or PhoneStateListener.LISTEN_SERVICE_STATE,
            )
        }
        report()
        awaitClose {
            runCatching { telephony.listen(listener, PhoneStateListener.LISTEN_NONE) }
        }
    }

    private fun unknown(mayRead: Boolean) = SignalReading(
        level = -1,
        mayRead = mayRead,
        hasSim = false,
        inService = false,
        roaming = false,
        networkType = "",
    )

    // `networkType` is the branch for anything before android 7; `dataNetworkType` is above.
    @Suppress("DEPRECATION")
    private fun netTypeOf(telephony: TelephonyManager): String = runCatching {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            telephony.dataNetworkType
        } else {
            telephony.networkType
        }
        when (type) {
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
