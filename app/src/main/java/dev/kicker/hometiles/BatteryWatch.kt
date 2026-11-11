package dev.kicker.hometiles

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import dev.kicker.hometiles.data.ConfigStore
import dev.kicker.hometiles.info.BatteryInfo
import dev.kicker.hometiles.info.BatteryStep
import dev.kicker.hometiles.info.BatteryWarning
import dev.kicker.hometiles.toggles.Sos
import dev.kicker.hometiles.ui.AppLocale
import dev.kicker.hometiles.ui.NoticeActivity

/**
 * the battery warning of issue #8: a notice on the screen, a text to the sos numbers, or both.
 *
 * registered at run time like [HomeOnScreenOff]: ACTION_BATTERY_CHANGED reaches no receiver
 * from the manifest, and HomeTiles is the home screen, so its process runs anyway.
 *
 * whether this discharge was warned about already is kept on disk. after a restart of the
 * process below the mark the relatives would otherwise get the same text twice.
 */
object BatteryWatch {

    private const val PREFS = "battery-warning"
    private const val WARNED = "warned"
    const val CHANNEL = "battery-low"
    private const val NOTIFICATION_ID = 424250
    private const val TAG = "BatteryWatch"

    fun register(context: Context) {
        val app = context.applicationContext
        app.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) = onReading(app, intent)
            },
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
    }

    private fun onReading(app: Context, intent: Intent) {
        val sos = ConfigStore.get(app).current.sos
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val warned = prefs.getBoolean(WARNED, false)
        if (!sos.batteryNotice && !sos.batteryText && !warned) return
        val percent = BatteryInfo.percent(
            intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
            intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
        )
        val charging = BatteryInfo.isCharging(
            intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1),
            intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
        )
        when (BatteryWarning.step(percent, charging, BatteryWarning.clamp(sos.batteryLevel), warned)) {
            BatteryStep.NOTHING -> Unit
            BatteryStep.RESET -> {
                prefs.edit().putBoolean(WARNED, false).apply()
                app.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
            }
            BatteryStep.WARN -> {
                prefs.edit().putBoolean(WARNED, true).apply()
                val texts = AppLocale.forApp(app)
                if (sos.batteryNotice) {
                    notice(app, texts.getString(R.string.battery_notice_text, percent))
                }
                if (sos.batteryText && sos.numbers.isNotEmpty()) {
                    // sent from here, on the main thread: a handful of numbers, and the
                    // sms manager only queues them.
                    val (sent, failed) = Sos.sendText(
                        app,
                        sos.numbers,
                        texts.getString(R.string.battery_sms_text, percent),
                    )
                    Log.i(TAG, "low battery text at $percent %: sent=$sent failed=$failed")
                }
            }
        }
    }

    /**
     * a full-screen notice, the way an incoming call comes: android lets no app start a
     * screen from the background, but it lets a notice open one when the phone lies locked.
     * with the screen in use it stays a banner, and a tap opens the same notice.
     */
    private fun notice(app: Context, text: String) {
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        val texts = AppLocale.forApp(app)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                texts.getString(R.string.battery_warning),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                // the screen is the message. a tone at night would wake the house for a
                // battery that can wait until the morning.
                setSound(null, null)
            },
        )
        val open = Intent(app, NoticeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(NoticeActivity.EXTRA_TEXT, text)
            .putExtra(NoticeActivity.EXTRA_UNTIL_CHARGING, true)
        val screen = PendingIntent.getActivity(
            app,
            NOTIFICATION_ID,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_battery)
            .setContentTitle(texts.getString(R.string.battery_warning))
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(screen)
            .setFullScreenIntent(screen, true)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}
