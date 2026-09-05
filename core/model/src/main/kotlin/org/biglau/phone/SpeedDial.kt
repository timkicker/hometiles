package org.biglau.phone

import org.biglau.data.PhoneConfig
import org.biglau.data.SpeedDialTarget

/**
 * speed dial on the number keys.
 *
 * 2 to 9 only: on many networks 1 is the mailbox and 0 the international prefix, and those
 * habits are older than this phone.
 */
object SpeedDial {

    val ASSIGNABLE: List<Char> = ('2'..'9').toList()

    fun isAssignable(key: Char): Boolean = key in ASSIGNABLE

    fun targetFor(config: PhoneConfig, key: Char): SpeedDialTarget? =
        if (!isAssignable(key)) null else config.speedDial[key.toString()]

    /** an unassignable key leaves the config untouched. */
    fun assign(config: PhoneConfig, key: Char, target: SpeedDialTarget): PhoneConfig {
        if (!isAssignable(key)) return config
        if (target.number.isBlank() || !PhoneNumbers.isDialable(target.number)) return config
        return config.copy(speedDial = config.speedDial + (key.toString() to target))
    }

    fun clear(config: PhoneConfig, key: Char): PhoneConfig =
        config.copy(speedDial = config.speedDial - key.toString())

    /** decides what a long press *does*: assign on an empty key, dial straight away on a full one. */
    fun anyAssigned(config: PhoneConfig): Boolean = ASSIGNABLE.any { targetFor(config, it) != null }
}
