package org.biglau.settings

/**
 * the jump into a settings sub-page: from the call log to the call types, from the contacts
 * to their sort order. without it, "this can be changed" would be a promise with no way
 * there.
 */
// `internal` was lost when this moved to core:model; across module borders there is no such
// thing. see `PLAN.md` 2.1.
object SettingsDeepLink {

    /** an unknown name gives null rather than throwing. */
    fun target(name: String?): Page? = name?.let { wanted ->
        Page.entries.firstOrNull { it.name == wanted }
    }

    /** the lock comes before any target. */
    fun start(locked: Boolean, target: Page?): Page = when {
        locked -> Page.GATE
        target != null -> target
        else -> Page.MAIN
    }

    /**
     * where a later request leads, or null to stay put.
     *
     * it stays put at the lock: otherwise one call from outside would walk past the pin.
     */
    fun jump(current: Page, target: Page?): Page? = when {
        target == null || current == Page.GATE || current == target -> null
        else -> target
    }
}
