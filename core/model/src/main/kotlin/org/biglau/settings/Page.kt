package org.biglau.settings

/**
 * Die Unterseiten der Einstellungen.
 *
 * Ein reiner Aufzaehlungstyp - er haelt [SettingsDeepLink] an `:app` fest, solange er in
 * der Activity steht. Deshalb liegt er hier, im Modul ohne Android. `internal` ging dabei
 * verloren: quer ueber Modulgrenzen gibt es das nicht.
 */
enum class Page { GATE, MAIN, MESSAGES, SCREENS, APPEARANCE, BEHAVIOUR, SECURITY, SET_PIN, DIAGNOSTICS, RENAME, HIDDEN_APPS, TRANSFER, SOS, ACCESSIBILITY, RESET , SWIPE_ORDER, ALLOWED_APPS, CONTACTS, CALL_TYPES}
