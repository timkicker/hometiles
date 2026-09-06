package dev.kicker.hometiles.settings

/** the settings sub-pages. lives here, not in the activity, so [SettingsDeepLink] stays free of android. */
enum class Page {
    GATE, MAIN, MESSAGES, SCREENS, APPEARANCE, BEHAVIOUR, SECURITY, SET_PIN, DIAGNOSTICS,
    RENAME, HIDDEN_APPS, TRANSFER, SOS, ACCESSIBILITY, RESET, SWIPE_ORDER, ALLOWED_APPS,
    CONTACTS, CALL_TYPES,
}
