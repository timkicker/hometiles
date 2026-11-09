package dev.kicker.hometiles.a11y

/**
 * back to the start when the screen goes off, the way closing a flip phone ends what was
 * open. issue #2: no app gets the lid switch itself, but closing the lid turns the screen off.
 *
 * never during a call: the proximity sensor turns the screen off at the ear, and the call
 * screen has to be there when it comes back on. the audio mode says so for every kind of
 * call, the phone's own and the ones of a messenger, without a permission.
 */
object ScreenOffHome {

    // AudioManager.MODE_NORMAL. ringing, a call, a voip call and call screening all differ.
    private const val MODE_NORMAL = 0

    fun goHome(enabled: Boolean, audioMode: Int): Boolean = enabled && audioMode == MODE_NORMAL
}
