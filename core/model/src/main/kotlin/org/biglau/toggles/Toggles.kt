package org.biglau.toggles

enum class ToggleKind {
    FLASHLIGHT, RINGER, WIFI, BLUETOOTH, AIRPLANE,

    /**
     * an ordinary app may flip none of these itself: mobile data never, location never,
     * brightness only with `WRITE_SETTINGS`, which is a special permission of its own. they
     * open the responsible system page, and the label says so.
     */
    MOBILE_DATA, LOCATION, BRIGHTNESS,
}

enum class ToggleAction {
    /** we flip it ourselves. */
    SWITCH,

    /** android will not let us; we open the system panel. */
    PANEL,

    /** not even a panel: the settings page. */
    SETTINGS,
}

/**
 * what a toggle tile can actually do.
 *
 * the original calls these "switches", and that has only been half true for years: airplane
 * mode has been off limits since android 4.2, wifi since 10, bluetooth since 13. a tile
 * labelled "turn on wifi" promises something that does not happen.
 */
object Toggles {

    fun actionFor(kind: ToggleKind, sdkInt: Int): ToggleAction = when (kind) {
        ToggleKind.FLASHLIGHT -> ToggleAction.SWITCH
        ToggleKind.RINGER -> ToggleAction.SWITCH
        ToggleKind.WIFI -> if (sdkInt >= 29) ToggleAction.PANEL else ToggleAction.SWITCH
        ToggleKind.BLUETOOTH -> if (sdkInt >= 33) ToggleAction.SETTINGS else ToggleAction.SWITCH
        ToggleKind.AIRPLANE -> ToggleAction.SETTINGS
        ToggleKind.MOBILE_DATA -> if (sdkInt >= 29) ToggleAction.PANEL else ToggleAction.SETTINGS
        ToggleKind.LOCATION -> ToggleAction.SETTINGS
        ToggleKind.BRIGHTNESS -> ToggleAction.SETTINGS
    }
}
