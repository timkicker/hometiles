package dev.kicker.hometiles.phone

/**
 * whether the proximity sensor may switch the screen off (issue #4).
 *
 * at the ear the cheek presses the big buttons: hang up, mute, speaker. the system dialer
 * switches the screen off there, HomeTiles did not. only for a call that runs on the earpiece:
 * a ringing one has to show who calls, and on the speaker or a headset the phone sits in the
 * hand or on the table, where a hand over the sensor must not take the buttons away.
 */
object CallProximity {

    private val atTheEar = setOf(
        CallStatus.DIALING,
        CallStatus.CONNECTING,
        CallStatus.ACTIVE,
        CallStatus.HOLDING,
    )

    fun screenOff(status: CallStatus?, earpiece: Boolean): Boolean = earpiece && status in atTheEar
}
