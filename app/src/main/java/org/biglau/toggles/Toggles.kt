package org.biglau.toggles

enum class ToggleKind { FLASHLIGHT, RINGER, WIFI, BLUETOOTH, AIRPLANE }

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
    }

    /** Kann der Nutzer erwarten, dass sich der Zustand sofort aendert? */
    fun switchesImmediately(kind: ToggleKind, sdkInt: Int): Boolean =
        actionFor(kind, sdkInt) == ToggleAction.SWITCH
}
