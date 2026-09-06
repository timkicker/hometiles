package dev.kicker.hometiles.ui

import android.content.Context
import android.content.Intent

/**
 * the way into the settings, by intent and not by class.
 *
 * four screens jump to a settings page; naming `SettingsActivity` would tie every future
 * `feature:*` to the settings tree, which `PLAN.md` 2.1 forbids. `SettingsLinkTest` holds
 * both sides together: without the filter the jump would land nowhere, silently.
 */
object SettingsLink {

    /** word for word in the manifest of `SettingsActivity`. */
    const val ACTION = "dev.kicker.hometiles.action.SETTINGS"

    /** the page, as the name of a `Page` value. */
    const val EXTRA_PAGE = "hometiles.settings.page"

    const val PAGE_CALL_TYPES = "CALL_TYPES"

    const val PAGE_CONTACTS = "CONTACTS"

    const val PAGE_SOS = "SOS"

    const val PAGE_HIDDEN_APPS = "HIDDEN_APPS"

    /** the settings from the front; the only way back for someone who gave up their tile. */
    fun toRoot(context: Context): Intent =
        Intent(ACTION).setPackage(context.packageName)

    /** `setPackage` keeps it in our own app, where another could claim the same name. */
    fun toPage(context: Context, page: String): Intent =
        Intent(ACTION).setPackage(context.packageName).putExtra(EXTRA_PAGE, page)
}
