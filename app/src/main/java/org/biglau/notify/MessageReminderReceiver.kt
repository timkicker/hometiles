package org.biglau.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.biglau.contacts.ContactRepository
import org.biglau.data.ConfigStore
import org.biglau.sms.SmsRepository

/**
 * Der Wecker für die wiederholte Erinnerung. `PLAN.md` 4.7.
 *
 * Die Kette hängt nicht an einem gemerkten Zustand, sondern fragt jedes Mal die Datenbank:
 * gibt es noch ungelesene Nachrichten? Nur dann meldet sie sich wieder und stellt den
 * nächsten Wecker. Damit hört sie von selbst auf, sobald jemand liest - ein gemerkter
 * Zustand wäre nach einem Neustart oder einem Absturz falsch, und eine Erinnerung, die zu
 * viel weiss, erinnert an Nachrichten, die es nicht mehr gibt.
 *
 * Ungenau geweckt (`setAndAllowWhileIdle`): auf die Minute genau zu wecken verlangt ab
 * Android 12 eine eigene Berechtigung, und eine Erinnerung braucht keine Sekunden.
 */
class MessageReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val fertig = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                erinnern(context.applicationContext)
            } finally {
                fertig.finish()
            }
        }
    }

    private suspend fun erinnern(context: Context) {
        val config = ConfigStore.get(context).current.sms
        if (!SmsReminder.active(config)) return
        // Nur als Standard-App. Sonst meldet die richtige Standard-App dieselbe Nachricht
        // und BigLau legte seine Erinnerung daneben - zweimal dasselbe, aus zwei Apps.
        if (!SmsRepository.get(context).isDefaultSmsApp()) return
        val offen = SmsReminder.due(SmsRepository.get(context).load(), config)
        if (offen.isEmpty()) return
        val kontakte = ContactRepository.get(context)
        offen.forEach { nachricht ->
            SmsNotifications.show(context, nachricht, kontakte.nameFor(nachricht.address), config)
        }
        schedule(context, config.repeatMinutes)
    }

    companion object {

        /** Stellt den nächsten Wecker. Ein zweiter Aufruf ersetzt den ersten, statt zu stapeln. */
        fun schedule(context: Context, minutes: Int) {
            val manager = context.getSystemService(AlarmManager::class.java) ?: return
            val absicht = pendingIntent(context)
            if (minutes <= 0) {
                manager.cancel(absicht)
                return
            }
            manager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + SmsReminder.delayMs(minutes),
                absicht,
            )
        }

        private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST,
            Intent(context, MessageReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private const val REQUEST = 4711
    }
}
