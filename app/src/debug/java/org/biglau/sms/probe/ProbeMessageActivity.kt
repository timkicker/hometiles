package org.biglau.sms.probe

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import org.biglau.sms.SmsDeliverReceiver
import org.biglau.sms.WapPushDeliverReceiver

/**
 * lays a message before BigLau that never came through a radio net.
 *
 *     adb shell am start -n org.biglau.debug/org.biglau.sms.probe.ProbeMessageActivity
 *     adb shell am start -n ... -e text "Hello" -e was raeumen
 *
 * the receiver is called by hand and not by broadcast because
 * `android.provider.Telephony.SMS_DELIVER` is a protected broadcast - only the system may
 * send it, neither `adb` nor this app. calling `onReceive` takes the same way from the first
 * step in BigLau on: same class, same intent, same reading through
 * `Telephony.Sms.Intents.getMessagesFromIntent`.
 *
 * what this does **not** check: that android hands the broadcast to BigLau at all. only a
 * real message shows that.
 *
 * the adb extras (`was`, `raeumen`) are values on the device and stay german, PLAN.md 11.4.
 */
class ProbeMessageActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.getStringExtra("was") == "raeumen") {
            clear()
            finish()
            return
        }

        // the mms notice. the receiver does not read the picture message at all - it only
        // says that one is there and sends on to the phone's own messaging app. so an empty
        // intent is enough here.
        if (intent?.getStringExtra("was") == "mms") {
            Log.i("BigLau", "probe: mms notice")
            WapPushDeliverReceiver().onReceive(this, Intent())
            finish()
            return
        }

        val text = intent?.getStringExtra("text") ?: "probe message without radio"
        val pdu = ProbePdu.build(ProbeMessage.SENDER, text)
        val into = Intent(Telephony.Sms.Intents.SMS_DELIVER_ACTION).apply {
            putExtra("pdus", arrayOf<Any>(pdu))
            putExtra("format", "3gpp")
        }
        Log.i("BigLau", "probe message: ${pdu.size} bytes from ${ProbeMessage.SENDER}")
        SmsDeliverReceiver().onReceive(this, into)
        finish()
    }

    /** clears the probes away again - same thought as with the probe call. */
    private fun clear() {
        val removed = runCatching {
            contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms.ADDRESS} LIKE ?",
                arrayOf("%" + ProbeMessage.SENDER.takeLast(9)),
            )
        }.getOrDefault(-1)
        Log.i("BigLau", "probe messages cleared: $removed")
    }
}

/** who sends the probe message. */
object ProbeMessage {

    /**
     * a different number than the probe call, so that the two probes do not merge into one
     * conversation on screen. same reserved range.
     */
    const val SENDER = "+447700900124"
}
