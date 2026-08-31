package org.biglau.info

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Der Ladestand als Fluss. Registriert wird erst, wenn jemand zuhoert, und wieder
 * abgemeldet, sobald niemand mehr hinsieht.
 */
object BatteryRepository {

    fun readings(context: Context): Flow<BatteryReading> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ignored: Context?, intent: Intent?) {
                intent?.toReading()?.let { trySend(it) }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        // Der Sticky-Broadcast liefert den aktuellen Stand sofort mit.
        val current = context.registerReceiver(receiver, filter)
        current?.toReading()?.let { trySend(it) }
        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }

    private fun Intent.toReading() = BatteryReading(
        level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
        scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1),
        status = getIntExtra(BatteryManager.EXTRA_STATUS, -1),
        plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
    )
}
