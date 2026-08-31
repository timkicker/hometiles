package org.biglau.phone

import org.biglau.data.PhoneConfig
import org.biglau.data.SpeedDialTarget

/**
 * Kurzwahl auf den Zifferntasten.
 *
 * Belegbar sind nur 2 bis 9. Die 1 ist auf vielen Netzen die Mailbox und die 0 die
 * Auslandsvorwahl - beide zu belegen wuerde Gewohnheiten brechen, die aelter sind als
 * dieses Telefon.
 */
object SpeedDial {

    val ASSIGNABLE: List<Char> = ('2'..'9').toList()

    fun isAssignable(key: Char): Boolean = key in ASSIGNABLE

    fun targetFor(config: PhoneConfig, key: Char): SpeedDialTarget? =
        if (!isAssignable(key)) null else config.speedDial[key.toString()]

    /** Belegt eine Taste. Eine unbelegbare Taste laesst die Konfiguration unveraendert. */
    fun assign(config: PhoneConfig, key: Char, target: SpeedDialTarget): PhoneConfig {
        if (!isAssignable(key)) return config
        if (target.number.isBlank() || !PhoneNumbers.isDialable(target.number)) return config
        return config.copy(speedDial = config.speedDial + (key.toString() to target))
    }

    fun clear(config: PhoneConfig, key: Char): PhoneConfig =
        config.copy(speedDial = config.speedDial - key.toString())

    /** Belegte Tasten in aufsteigender Reihenfolge - so steht es auch in den Einstellungen. */
    fun assigned(config: PhoneConfig): List<Pair<Char, SpeedDialTarget>> = ASSIGNABLE
        .mapNotNull { key -> targetFor(config, key)?.let { key to it } }
}
