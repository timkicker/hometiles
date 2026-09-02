package org.biglau.toggles

enum class ToggleKind {
    FLASHLIGHT, RINGER, WIFI, BLUETOOTH, AIRPLANE,

    /**
     * Mobile Daten, Standort und Helligkeit. Alle drei darf eine gewöhnliche App nicht
     * selbst umlegen - mobile Daten nie, den Standort nie, die Helligkeit nur mit
     * `WRITE_SETTINGS`, was eine eigene Sondererlaubnis ist. Sie öffnen deshalb die
     * zuständige Systemseite, und die Beschriftung sagt das auch.
     */
    MOBILE_DATA, LOCATION, BRIGHTNESS,
}

/** Was beim Antippen tatsaechlich passieren kann. */
enum class ToggleAction {
    /** Wir legen den Schalter selbst um. */
    SWITCH,

    /** Android laesst uns nicht - wir oeffnen die Systemblende. */
    PANEL,

    /** Nicht einmal eine Blende: nur die Systemeinstellungen. */
    SETTINGS,
}

/**
 * Was eine Schalter-Kachel wirklich tun kann.
 *
 * Das Original nennt diese Kacheln "Schalter", aber das stimmt seit Jahren nur noch teilweise:
 * Flugmodus darf keine App mehr umlegen (seit Android 4.2), WLAN nicht mehr seit Android 10,
 * Bluetooth nicht mehr seit Android 13. Wer die Kachel trotzdem "WLAN einschalten" nennt,
 * verspricht etwas, das nicht eintritt.
 *
 * Deshalb entscheidet diese Tabelle, und die Beschriftung richtet sich danach.
 */
object Toggles {

    fun actionFor(kind: ToggleKind, sdkInt: Int): ToggleAction = when (kind) {
        ToggleKind.FLASHLIGHT -> ToggleAction.SWITCH
        ToggleKind.RINGER -> ToggleAction.SWITCH
        ToggleKind.WIFI -> if (sdkInt >= 29) ToggleAction.PANEL else ToggleAction.SWITCH
        ToggleKind.BLUETOOTH -> if (sdkInt >= 33) ToggleAction.SETTINGS else ToggleAction.SWITCH
        ToggleKind.AIRPLANE -> ToggleAction.SETTINGS
        // Mobile Daten haben ab Android 10 eine eigene Systemblende; darunter bleibt nur
        // die Einstellungsseite.
        ToggleKind.MOBILE_DATA -> if (sdkInt >= 29) ToggleAction.PANEL else ToggleAction.SETTINGS
        ToggleKind.LOCATION -> ToggleAction.SETTINGS
        ToggleKind.BRIGHTNESS -> ToggleAction.SETTINGS
    }
}
