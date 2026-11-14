package dev.kicker.hometiles.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import dev.kicker.hometiles.R
import dev.kicker.hometiles.ui.AppLocale
import dev.kicker.hometiles.ui.Notice

/**
 * the network's receipt for a sent message.
 *
 * `sendTextMessage` returns at once; whether the network took the message is settled
 * seconds later and arrives here as a broadcast. passing `null` meant sent was claimed
 * because the call had not thrown, and a refused message looked like every other.
 */
class SmsSentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // the app's language, not the phone's: a receiver gets the bare context.
        val texts = AppLocale.forApp(context)
        if (resultCode == Activity.RESULT_OK) {
            if (intent.getBooleanExtra(EXTRA_LAST, false)) {
                Notice.show(context, texts.getString(R.string.sms_sent))
            }
            return
        }

        // the row in question rides in the intent's data, as it came out of the write.
        intent.data?.let { row ->
            runCatching {
                context.contentResolver.update(
                    row,
                    ContentValues().apply {
                        put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_FAILED)
                    },
                    null,
                    null,
                )
            }
        }
        SmsRepository.notifyChanged()
        SmsNotifications.showSendFailed(context, texts.getString(reasonText(resultCode)))
    }

    companion object {

        /** set on the receipt of the last part, the one that may say sent. */
        const val EXTRA_LAST = "hometiles.sms.last_part"

        /** a general error is no answer; no service and flight mode are ones you can act on. */
        private fun reasonText(code: Int): Int = when (code) {
            SmsManager.RESULT_ERROR_NO_SERVICE -> R.string.sms_send_no_service
            SmsManager.RESULT_ERROR_RADIO_OFF -> R.string.sms_send_radio_off
            else -> R.string.sms_send_generic
        }
    }
}
