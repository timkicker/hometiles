package dev.kicker.hometiles.phone

import dev.kicker.hometiles.data.AudioRoute
import dev.kicker.hometiles.data.PhoneConfig

/**
 * where the sound goes when a call begins.
 *
 * set once on connect, never on later state changes: following those would switch the
 * speaker back on that the user just turned off by hand.
 */
object CallAudio {

    /** `null` means leave it to the system. */
    fun routeOnConnect(config: PhoneConfig, outgoing: Boolean): AudioRoute? = when {
        outgoing && config.speakerOnOutgoing -> AudioRoute.SPEAKER
        config.audioRoute != AudioRoute.EARPIECE -> config.audioRoute
        else -> null
    }
}
