package dev.kicker.hometiles.sms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dev.kicker.hometiles.contacts.ContactRepository
import dev.kicker.hometiles.data.ConfigStore
import dev.kicker.hometiles.ui.AppLocale

/**
 * the alarm for the repeated reminder. `PLAN.md` 4.7.
 *
 * the chain asks the database every time instead of keeping state: only unread messages
 * make it report again and set the next alarm, so it stops by itself once someone reads.
 * kept state would be wrong after a restart and would remind about messages that are gone.
 *
 * woken inexactly: to the minute needs its own permission from android 12 on.
 */
class MessageReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val done = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                remind(context.applicationContext)
            } finally {
                done.finish()
            }
        }
    }

    private suspend fun remind(context: Context) {
        val config = ConfigStore.get(context).current.sms
        if (!SmsReminder.active(config)) return
        // only as the default app, or the real one reports the same message beside ours.
        if (!SmsRepository.get(context).isDefaultSmsApp()) return
        val open = SmsReminder.due(SmsRepository.get(context).load(
            AppLocale.forApp(context).getString(dev.kicker.hometiles.core.system.R.string.mms_picture),
        ), config)
        if (open.isEmpty()) return
        val contacts = ContactRepository.get(context)
        open.forEach { message ->
            SmsNotifications.show(context, message, contacts.nameFor(message.address), config)
        }
        schedule(context, config.repeatMinutes)
    }

    companion object {

        /** a second call replaces the first instead of stacking. */
        fun schedule(context: Context, minutes: Int) {
            val manager = context.getSystemService(AlarmManager::class.java) ?: return
            val intent = pendingIntent(context)
            if (minutes <= 0) {
                manager.cancel(intent)
                return
            }
            manager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + SmsReminder.delayMs(minutes),
                intent,
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
