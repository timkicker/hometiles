package org.biglau.toggles

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * Fragt während des Countdowns nach einer frischen Position.
 *
 * **Der Grund:** die Notruf-SMS nahm die *zuletzt bekannte* Position — und die ist auf einem
 * Telefon, das in der Tasche liegt, oft Stunden alt oder gar nicht vorhanden. Am Emulator ist
 * sie es immer (`last location=null`), und auf einem echten Gerät hängt sie davon ab, ob
 * zufällig kurz vorher eine andere App nach dem Standort gefragt hat. Ein Notruf, der den
 * Standort von gestern mitschickt, führt die Hilfe an den falschen Ort.
 *
 * Der Countdown ist genau das Fenster, das dafür da ist: er dauert ohnehin einige Sekunden,
 * und in dieser Zeit kann das Telefon suchen. Gesucht wird nur so lange, wie der Bildschirm
 * offen ist — danach wird die Anfrage wieder abgemeldet, sonst liefe der Empfänger weiter und
 * verbrauchte Strom.
 *
 * Verlangt wird nichts: kommt in der Zeit keine Position, bleibt es bei der zuletzt bekannten.
 */
class SosLocation(private val context: Context) {

    private val listener = LocationListener { /* Es genuegt, dass das System sucht. */ }
    private var läuft = false

    private fun darfOrten(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun start() {
        if (läuft || !darfOrten()) return
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        runCatching {
            manager.getProviders(true).forEach { provider ->
                manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            }
            läuft = true
        }
    }

    fun stop() {
        if (!läuft) return
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        runCatching { manager.removeUpdates(listener) }
        läuft = false
    }
}
