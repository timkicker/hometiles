package org.biglau.toggles

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * asks for a fresh position during the countdown.
 *
 * the emergency message took the *last known* position, which on a phone in a pocket is
 * often hours old or absent, and yesterday's location sends help to the wrong place.
 *
 * the countdown is the window for it. nothing is demanded: with no position in that time it
 * stays with the last known one.
 */
class SosLocation(private val context: Context) {

    private val listener = LocationListener { /* it is enough that the system searches. */ }
    private var running = false

    private fun mayLocate(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun start() {
        if (running || !mayLocate()) return
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        runCatching {
            manager.getProviders(true).forEach { provider ->
                manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            }
            running = true
        }
    }

    fun stop() {
        if (!running) return
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        runCatching { manager.removeUpdates(listener) }
        running = false
    }
}
