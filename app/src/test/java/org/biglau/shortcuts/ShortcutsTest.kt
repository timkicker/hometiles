package org.biglau.shortcuts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutsTest {

    private fun row(
        id: String,
        short: String = id,
        long: String? = null,
        enabled: Boolean = true,
        rank: Int = 0,
        kind: ShortcutKind = ShortcutKind.STATIC,
        pkg: String = "com.chat",
    ) = ShortcutRow(pkg, id, short, long, enabled, rank, kind)

    @Test
    fun `das lange Label gewinnt weil es mehr sagt`() {
        assertEquals("Nachricht an Anna", Shortcuts.labelOf(row("a", short = "Anna", long = "Nachricht an Anna")))
    }

    @Test
    fun `ohne langes Label bleibt das kurze`() {
        assertEquals("Anna", Shortcuts.labelOf(row("a", short = "Anna", long = null)))
        assertEquals("Anna", Shortcuts.labelOf(row("a", short = "Anna", long = "   ")))
    }

    @Test
    fun `abgeschaltete Verknuepfungen werden nicht angeboten`() {
        val rows = listOf(row("a"), row("b", enabled = false))
        assertEquals(listOf("a"), Shortcuts.usable(rows).map { it.id })
    }

    @Test
    fun `Verknuepfungen ohne Namen fliegen raus`() {
        // Eine namenlose Kachel waere fuer den Nutzer nicht zuzuordnen.
        val rows = listOf(row("a"), row("b", short = "  "))
        assertEquals(listOf("a"), Shortcuts.usable(rows).map { it.id })
    }

    @Test
    fun `angepinnte stehen vorn weil der Nutzer sie selbst gewaehlt hat`() {
        val rows = listOf(
            row("statisch", rank = 0),
            row("angepinnt", rank = 9, kind = ShortcutKind.PINNED),
        )
        assertEquals(listOf("angepinnt", "statisch"), Shortcuts.usable(rows).map { it.id })
    }

    @Test
    fun `der Rang der App wird beachtet`() {
        // Wer den Rang ignoriert, zeigt dem Nutzer die vierte Option zuerst.
        val rows = listOf(row("c", rank = 2), row("a", rank = 0), row("b", rank = 1))
        assertEquals(listOf("a", "b", "c"), Shortcuts.usable(rows).map { it.id })
    }

    @Test
    fun `bei gleichem Rang entscheidet der Name`() {
        val rows = listOf(row("z", short = "Zebra"), row("a", short = "Anna"))
        assertEquals(listOf("Anna", "Zebra"), Shortcuts.usable(rows).map { Shortcuts.labelOf(it) })
    }

    @Test
    fun `dieselbe Verknuepfung erscheint nur einmal`() {
        // Eine Verknuepfung kann gleichzeitig dynamisch und angepinnt gemeldet werden.
        val rows = listOf(
            row("a", kind = ShortcutKind.DYNAMIC),
            row("a", kind = ShortcutKind.PINNED),
        )
        assertEquals(1, Shortcuts.usable(rows).size)
    }

    @Test
    fun `gleiche Kennung in verschiedenen Apps bleibt getrennt`() {
        val rows = listOf(row("neu", pkg = "com.chat"), row("neu", pkg = "com.mail"))
        assertEquals(2, Shortcuts.usable(rows).size)
    }

    @Test
    fun `eine App ohne brauchbare Verknuepfung bietet nichts an`() {
        assertTrue(!Shortcuts.hasAny(emptyList()))
        assertTrue(!Shortcuts.hasAny(listOf(row("a", enabled = false))))
        assertTrue(Shortcuts.hasAny(listOf(row("a"))))
    }
}
