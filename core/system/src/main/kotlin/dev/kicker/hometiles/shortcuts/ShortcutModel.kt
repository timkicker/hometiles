package dev.kicker.hometiles.shortcuts

/** a shortcut as far as it concerns us, free of android types so it can be tested. */
data class ShortcutRow(
    val packageName: String,
    val id: String,
    val shortLabel: String,
    val longLabel: String? = null,
    val enabled: Boolean = true,
    val rank: Int = 0,
    val kind: ShortcutKind = ShortcutKind.STATIC,
)

enum class ShortcutKind { STATIC, DYNAMIC, PINNED }

/**
 * what the query returned.
 *
 * an empty list and a failure look alike but say different things; merged into
 * `emptyList()` the screen claims the app offers no shortcuts although nobody asked it.
 */
sealed interface ShortcutAnswer {

    /** android answered. the list may be empty, then the app really has none. */
    data class Rows(val rows: List<ShortcutRow>) : ShortcutAnswer

    /** the query failed. whether there are shortcuts is unknown. */
    data object Failed : ShortcutAnswer
}

object Shortcuts {

    /** the long label says more. */
    fun labelOf(row: ShortcutRow): String =
        row.longLabel?.takeIf { it.isNotBlank() } ?: row.shortLabel

    /** pinned first, those the user picked; then the app's rank, then the name. */
    fun usable(rows: List<ShortcutRow>): List<ShortcutRow> = rows
        .filter { it.enabled && labelOf(it).isNotBlank() }
        .distinctBy { it.packageName to it.id }
        .sortedWith(
            compareBy(
                { it.kind != ShortcutKind.PINNED },
                { it.rank },
                { labelOf(it).lowercase() },
            ),
        )
}
