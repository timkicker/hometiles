package org.biglau.apps

import org.biglau.search.TextSearch

/**
 * Was die App-Liste zeigt und in welcher Reihenfolge.
 *
 * Getrennt von der Oberflaeche, weil hier die Entscheidungen liegen: was ausgeblendet ist,
 * was als "zuletzt benutzt" oben steht, und was passiert, wenn eine App deinstalliert wurde,
 * die noch in der Liste der zuletzt benutzten steht.
 */
object AppDrawer {

    /** Eindeutiger Schluessel einer startbaren App. */
    fun keyOf(app: LaunchableApp): String = "${app.packageName}/${app.activityName}"

    /** Alles, was nicht ausgeblendet ist. */
    fun visible(apps: List<LaunchableApp>, hidden: Set<String>): List<LaunchableApp> =
        apps.filterNot { keyOf(it) in hidden || it.packageName in hidden }

    /**
     * Die zuletzt benutzten, in der gespeicherten Reihenfolge. Schluessel, zu denen es keine
     * App mehr gibt, fallen still weg - eine deinstallierte App darf keine Luecke hinterlassen.
     */
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

    /**
     * Merkt sich einen Start. Zuletzt benutzt heisst ganz vorn, jeder Schluessel nur einmal,
     * und die Liste waechst nicht unbegrenzt.
     */
    fun remember(recentKeys: List<String>, key: String, cap: Int = 12): List<String> =
        (listOf(key) + recentKeys.filterNot { it == key }).take(cap.coerceAtLeast(1))

    /** Die vollstaendige, durchsuchte Liste. Leere Anfrage heisst alphabetisch. */
    fun search(
        apps: List<LaunchableApp>,
        hidden: Set<String>,
        query: String,
    ): List<LaunchableApp> = TextSearch.filter(visible(apps, hidden), query) { it.label }

    /** Ausblenden umschalten. */
    fun toggleHidden(hidden: Set<String>, app: LaunchableApp): Set<String> {
        val key = keyOf(app)
        return if (key in hidden) hidden - key else hidden + key
    }
}
