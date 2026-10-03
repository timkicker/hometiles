package dev.kicker.hometiles.phone

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import dev.kicker.hometiles.data.ConfigStore

/**
 * rejects blocked numbers *before* the phone rings.
 *
 * rejecting in [BigInCallService.onCallAdded] happens after android has taken the call,
 * started the ringtone and bound the call screen, and the log then showed it as rejected,
 * as if the user had pushed it away.
 *
 * android asks this service before anything happens. only the default phone app is asked,
 * so the rejection in the service stays for the case where HomeTiles has no role.
 */
class CallScreening : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        val isBlocked = CallBlocking.blocksIncoming(
            number = number,
            incoming = isIncoming(callDetails),
            blocked = ConfigStore.get(this).current.phone.blockedNumbers,
        )
        respondToCall(callDetails, response(isBlocked))
    }

    /**
     * [Call.Details.getCallDirection] arrived with android 10, and so did screening of
     * outgoing calls. before that this service was only ever asked about a call coming in,
     * so there is nothing to read: the answer is always the same one.
     */
    private fun isIncoming(details: Call.Details): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            details.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            true
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
