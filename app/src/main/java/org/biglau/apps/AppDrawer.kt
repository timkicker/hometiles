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
    /**
     * Wie viele Einträge überhaupt gespeichert werden.
     *
     * Muss mindestens so groß sein wie die größte anzeigbare Zahl, sonst wäre eine
     * Einstellung wählbar, die nie erreicht wird. Am 01.09.2026 stand die Liste nach einem
     * Tag Gebrauch genau auf diesem Wert - zwölf verschiedene Apps an einem Tag.
     */
    const val STORAGE_CAP = 12

    /** Was die Einstellung zur Auswahl stellt. 0 heißt: gar keine Vorschläge. */
    val RECENT_CHOICES = listOf(0, 4, 6, 8, 12)

    fun remember(recentKeys: List<String>, key: String, cap: Int = STORAGE_CAP): List<String> =
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
