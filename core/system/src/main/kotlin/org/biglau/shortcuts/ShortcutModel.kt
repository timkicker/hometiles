package org.biglau.shortcuts

/** Eine Verknuepfung, so weit sie uns interessiert - ohne Android-Typen, damit pruefbar. */
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
 * Was bei der Abfrage herauskam.
 *
 * Eine leere Liste und ein Fehlschlag sehen im Ergebnis gleich aus, sagen aber
 * Verschiedenes: das eine ist eine Antwort, das andere keine. Wer beides zu `emptyList()`
 * verschmilzt, laesst die Oberflaeche behaupten, die App biete keine Verknuepfungen an -
 * obwohl niemand sie gefragt hat.
 */
sealed interface ShortcutAnswer {

    /** Android hat geantwortet. Die Liste darf leer sein, dann hat die App wirklich keine. */
    data class Rows(val rows: List<ShortcutRow>) : ShortcutAnswer

    /** Die Abfrage ist fehlgeschlagen. Ob es Verknuepfungen gibt, wissen wir nicht. */
    data object Failed : ShortcutAnswer
}

/**
 * Welche Verknuepfungen angeboten werden und in welcher Reihenfolge.
 *
 * Die Reihenfolge ist nicht beliebig: Apps vergeben einen Rang, und wer ihn ignoriert,
 * zeigt dem Nutzer die vierte Option zuerst.
 */
object Shortcuts {

    /** Anzeigename: das lange Label, wenn es eines gibt - es sagt mehr. */
    fun labelOf(row: ShortcutRow): String =
        row.longLabel?.takeIf { it.isNotBlank() } ?: row.shortLabel

    /**
     * Was tatsaechlich angeboten wird: nur aktive Verknuepfungen, angepinnte zuerst
     * (die hat der Nutzer selbst gewaehlt), dann nach dem Rang der App, dann nach Namen.
     */
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
