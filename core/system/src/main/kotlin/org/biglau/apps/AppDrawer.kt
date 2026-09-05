package org.biglau.apps

import org.biglau.search.TextSearch

/** what the app list shows and in which order. */
object AppDrawer {

    fun keyOf(app: LaunchableApp): String = "${app.packageName}/${app.activityName}"

    fun visible(apps: List<LaunchableApp>, hidden: Set<String>): List<LaunchableApp> =
        apps.filterNot { keyOf(it) in hidden || it.packageName in hidden }

    /** keys without an app fall away: an uninstalled app must leave no gap. */
    fun recents(
        apps: List<LaunchableApp>,
        recentKeys: List<String>,
        hidden: Set<String>,
        limit: Int,
    ): List<LaunchableApp> {
        if (limit <= 0) return emptyList()
        val byKey = visible(apps, hidden).associateBy(::keyOf)
        return recentKeys.mapNotNull { byKey[it] }.take(limit)
    }

    /** at least the largest displayable count, else a setting would never be reached. */
    const val STORAGE_CAP = 12

    /** 0 means no suggestions at all. */
    val RECENT_CHOICES = listOf(0, 4, 6, 8, 12)

    fun remember(recentKeys: List<String>, key: String, cap: Int = STORAGE_CAP): List<String> =
        (listOf(key) + recentKeys.filterNot { it == key }).take(cap.coerceAtLeast(1))

    /** an empty query means alphabetical. */
    fun search(
        apps: List<LaunchableApp>,
        hidden: Set<String>,
        query: String,
    ): List<LaunchableApp> = TextSearch.filter(visible(apps, hidden), query) { it.label }

    /**
     * hidden apps matching the query.
     *
     * without it, searching for an app you hid months ago answers no app matches, which is
     * true of the visible list and false of the phone.
     */
    fun hiddenMatches(
        apps: List<LaunchableApp>,
        hidden: Set<String>,
        query: String,
    ): List<LaunchableApp> =
        if (query.isBlank()) {
            emptyList()
        } else {
            TextSearch.filter(apps.filter { keyOf(it) in hidden }, query) { it.label }
        }

    fun toggleHidden(hidden: Set<String>, app: LaunchableApp): Set<String> {
        val key = keyOf(app)
        return if (key in hidden) hidden - key else hidden + key
    }
}
