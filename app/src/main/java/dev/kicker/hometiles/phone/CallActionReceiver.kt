package dev.kicker.hometiles.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.kicker.hometiles.R
import dev.kicker.hometiles.ui.Notice

/**
 * answer and reject from the ringing notice.
 *
 * with the screen on android shows a full screen intent only as a small notice, and that one
 * had no buttons: on a flip phone in use a call could only be taken by finding the notice in
 * the shade (issue #2, emulator 25.10.2026). not exported: only our own notice sends here.
 */
class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val outcome = when (intent.action) {
            ANSWER -> InCallRepository.answer()
            REJECT -> InCallRepository.reject()
            else -> return
        }
        // a button that does nothing while the phone rings is the worst kind. see SilentFailureTest.
        if (outcome.isFailure) Notice.show(context, R.string.call_action_failed)
    }

    companion object {
        const val ANSWER = "dev.kicker.hometiles.phone.ANSWER"
        const val REJECT = "dev.kicker.hometiles.phone.REJECT"
    }
}
