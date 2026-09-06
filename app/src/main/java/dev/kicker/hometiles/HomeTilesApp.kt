package dev.kicker.hometiles

import android.app.Application
import dev.kicker.hometiles.sms.SmsReminder
import dev.kicker.hometiles.sms.MessageReminderReceiver
import dev.kicker.hometiles.apps.AppRepository
import dev.kicker.hometiles.data.ConfigStore
import dev.kicker.hometiles.phone.SystemNumbers
import dev.kicker.hometiles.safety.CrashRecorder

class HomeTilesApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // first and on this thread: catching crashes gives safe mode something to show.
        // costs two milliseconds.
        CrashRecorder.get(this).installHandler()

        // reading the config costs 186 ms on the jelly 2, not for the twelve kilobytes but
        // for the first use of the serialiser. safe beside the activity start, because
        // `ConfigStore.get` is guarded against two callers anyway.
        Thread {
            ConfigStore.get(this)
            AppRepository.get(this)
            // spelling and country for numbers without an area code; without it numbers
            // come out in bare groups of three. see PhoneNumbers.forDisplay.
            SystemNumbers.install(this)
            // set the reminder alarm again: it hangs on `ELAPSED_REALTIME_WAKEUP` and is
            // gone after a restart. no `BOOT_COMPLETED` receiver for it, since HomeTiles is
            // the home screen and runs after every restart anyway.
            //
            // harmless when nothing is due: the receiver checks by itself.
            val sms = ConfigStore.get(this).current.sms
            if (SmsReminder.active(sms)) {
                MessageReminderReceiver.schedule(this, sms.repeatMinutes)
            }
        }.start()
    }
}
