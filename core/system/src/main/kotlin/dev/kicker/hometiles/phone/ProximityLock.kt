package dev.kicker.hometiles.phone

import android.content.Context
import android.os.PowerManager

/**
 * the screen goes off while something is close to the sensor, for as long as the lock is held;
 * the decision when sits in [CallProximity].
 *
 * a phone without a proximity sensor does not support the level, and then this does nothing:
 * a screen that went off with no sensor to bring it back would end the call blind.
 */
class ProximityLock(context: Context) {

    private val lock: PowerManager.WakeLock? = runCatching {
        val power = context.getSystemService(PowerManager::class.java)
        if (power?.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) == true) {
            power.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "hometiles:call")
                .apply { setReferenceCounted(false) }
        } else {
            null
        }
    }.getOrNull()

    fun hold(wanted: Boolean) {
        val lock = lock ?: return
        runCatching {
            when {
                wanted && !lock.isHeld -> lock.acquire()
                // still at the ear when the call ends: the screen comes back once the phone
                // is taken away, not into the cheek.
                !wanted && lock.isHeld -> lock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
            }
        }
    }
}
