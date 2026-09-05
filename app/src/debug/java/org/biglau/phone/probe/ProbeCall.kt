package org.biglau.phone.probe

import org.biglau.phone.PhoneNumbers

/**
 * a call nobody makes.
 *
 * the call screen is the one part of BigLau one cannot try out without ringing somebody - and
 * the one where a fault means one cannot pick up. so BigLau places the call itself: an own
 * `ConnectionService` reports a call to telecom, telecom binds the `InCallService`, and from
 * there everything runs as in earnest. only the radio is never involved.
 *
 * debug build only: this file lies in `src/debug`, because a probe inside the delivered
 * program is a way to fake a call.
 */
object ProbeCall {

    /** the id of the account telecom runs the probe under. it lies on the device, so it stays. */
    const val ACCOUNT = "biglau-probe"

    /**
     * the number the probe call shows.
     *
     * from the range ofcom reserved for film and television (07700 900000 to 900999). those
     * numbers are assigned to nobody and cannot become assigned. the probe reaches no network
     * anyway - but an invented number that one day does exist would be a needless bet.
     */
    const val NUMBER = "+447700900123"

    const val NAME = "Probe call"

    /**
     * should the probe ring?
     *
     * without this field it starts as a **running** call: the call screen stands there at
     * once, with audio route, mute, hold and keypad - and without a sound. that is the state
     * in which almost everything can be checked. the ringing is the other half and needs a
     * person who wants to hear it.
     *
     * the value is the extra name on the device and stays as it is, PLAN.md 11.4.
     */
    const val EXTRA_RINGING = "org.biglau.probe.KLINGELN"

    /**
     * asks telecom not to write this call into the call log.
     *
     * the constant exists in `TelecomManager` only as a hidden field, so the name stands here
     * as a string - and so it is checked at the device whether it works instead of assuming.
     */
    const val EXTRA_DO_NOT_LOG = "android.telecom.extra.DO_NOT_LOG_CALL"

    /**
     * may this number be used for a probe?
     *
     * the probe never reaches the network, and still this check stands here: it costs nothing
     * and rules out the one way a probe could become an emergency call. see
     * `NoEmergencyCallTest`.
     */
    fun allowed(number: String): Boolean =
        number.isNotBlank() && !PhoneNumbers.looksLikeEmergency(number)
}
