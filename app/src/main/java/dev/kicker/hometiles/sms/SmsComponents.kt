package dev.kicker.hometiles.sms

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Telephony
import dev.kicker.hometiles.contacts.ContactRepository
import dev.kicker.hometiles.data.ConfigStore

/**
 * the four components a default sms app must have.
 *
 * android demands all four or the app does not appear in the picker at all, whether or not
 * it would work. they sit in one file so that deleting one shows the other three beside it.
 *
 * 1. [SmsDeliverReceiver] takes incoming sms
 * 2. [WapPushDeliverReceiver] takes mms notifications
 * 3. [RespondViaMessageService] rejects a call with a message
 * 4. the SENDTO activity, see the manifest at SmsActivity
 */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // holding the role means storing it ourselves: SMS_DELIVER goes only to the
        // default app, and unwritten the message is nobody's.
        if (SmsDelivery.mayWrite(Telephony.Sms.getDefaultSmsPackage(context), context.packageName)) {
            store(context, intent)
        }
        SmsRepository.notifyChanged()
    }

    private fun store(context: Context, intent: Intent) {
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent).orEmpty().mapNotNull { message ->
            val sender = message?.displayOriginatingAddress ?: return@mapNotNull null
            SmsDelivery.Part(
                address = sender,
                body = message.displayMessageBody.orEmpty(),
                timestamp = message.timestampMillis,
            )
        }
        // a failed write loses the message; a crash in the receiver takes the app with it,
        // and does so again on every further sms.
        runCatching {
            SmsDelivery.merge(parts).forEach { whole ->
                announce(context, whole)
                context.contentResolver.insert(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    ContentValues().apply {
                        put(Telephony.Sms.ADDRESS, whole.address)
                        put(Telephony.Sms.BODY, whole.body)
                        // DATE is the arrival here, DATE_SENT the sending network's stamp,
                        // which need not match this device's clock: a message that had just
                        // arrived stood at 14:46 while it was 13:47. the list sorts by
                        // arrival.
                        put(Telephony.Sms.DATE, System.currentTimeMillis())
                        put(Telephony.Sms.DATE_SENT, whole.timestamp)
                        put(Telephony.Sms.READ, 0)
                        put(Telephony.Sms.SEEN, 0)
                    },
                )
            }
        }
    }
}

/**
 * says so. without it the message lies in the database and nobody knows until they open the
 * list themselves, which for a phone in a pocket is the same as lost.
 */
private fun announce(context: Context, whole: SmsDelivery.Incoming) {
    val config = ConfigStore.get(context).current.sms
    SmsNotifications.show(
        context,
        SmsMessage(
            id = 0,
            threadId = 0,
            address = whole.address,
            body = whole.body,
            timestamp = whole.timestamp,
            incoming = true,
            read = false,
        ),
        name = ContactRepository.get(context).nameFor(whole.address),
        config = config,
    )
}

class WapPushDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SmsRepository.notifyChanged()
        // `PLAN.md` 6, mms line: report a failure visibly instead of swallowing it. with
        // only the line above, a picture message arrived and HomeTiles said nothing.
        SmsNotifications.showMmsHint(context, ConfigStore.get(context).current.sms)
    }
}

/**
 * reject a call with a message: the path on which another app asks us to send one.
 *
 * this used to be a silent no-op that took the request, did nothing and considered itself
 * done, which is the worst kind of fault here: one that looks like success.
 *
 * HomeTiles does not send by itself. a message triggered by a foreign app that nobody gets to
 * see would be the opposite of what this app promises, so a notice leads into the
 * conversation with the text in the field and the owner sends it.
 */
class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val number = intent?.data?.schemeSpecificPart.orEmpty()
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        RespondNotice.show(this, number, text)
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
