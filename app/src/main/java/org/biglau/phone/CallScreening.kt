package org.biglau.phone

import android.telecom.Call
import android.telecom.CallScreeningService
import org.biglau.data.ConfigStore

/**
 * rejects blocked numbers *before* the phone rings.
 *
 * rejecting in [BigInCallService.onCallAdded] happens after android has taken the call,
 * started the ringtone and bound the call screen, and the log then showed it as rejected,
 * as if the user had pushed it away.
 *
 * android asks this service before anything happens. only the default phone app is asked,
 * so the rejection in the service stays for the case where BigLau has no role.
 */
class CallScreening : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        val isBlocked = CallBlocking.blocksIncoming(
            number = number,
            incoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING,
            blocked = ConfigStore.get(this).current.phone.blockedNumbers,
        )
        respondToCall(callDetails, response(isBlocked))
    }

    /**
     * `skipCallLog = false`: the call belongs in the list. a block that makes calls vanish
     * without trace cannot be checked, and an accidental block would never be noticed.
     */
    private fun response(blocked: Boolean): CallResponse = CallResponse.Builder()
        .setDisallowCall(blocked)
        .setRejectCall(blocked)
        .setSkipCallLog(false)
        .setSkipNotification(blocked)
        .build()
}
