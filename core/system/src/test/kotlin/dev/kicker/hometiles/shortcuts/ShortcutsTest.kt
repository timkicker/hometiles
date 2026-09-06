package dev.kicker.hometiles.shortcuts

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
    fun `the long label wins because it says more`() {
        assertEquals("Message to Anna", Shortcuts.labelOf(row("a", short = "Anna", long = "Message to Anna")))
    }

    @Test
    fun `without a long label the short one stays`() {
        assertEquals("Anna", Shortcuts.labelOf(row("a", short = "Anna", long = null)))
        assertEquals("Anna", Shortcuts.labelOf(row("a", short = "Anna", long = "   ")))
    }

    @Test
    fun `disabled shortcuts are not offered`() {
        val rows = listOf(row("a"), row("b", enabled = false))
        assertEquals(listOf("a"), Shortcuts.usable(rows).map { it.id })
    }

    @Test
    fun `shortcuts without a name fly out`() {
        // a nameless tile could not be placed by anyone looking at it.
        val rows = listOf(row("a"), row("b", short = "  "))
        assertEquals(listOf("a"), Shortcuts.usable(rows).map { it.id })
    }

    @Test
    fun `pinned ones stand in front because they were chosen by hand`() {
        val rows = listOf(
            row("static", rank = 0),
            row("pinned", rank = 9, kind = ShortcutKind.PINNED),
        )
        assertEquals(listOf("pinned", "static"), Shortcuts.usable(rows).map { it.id })
    }

    @Test
    fun `the app's rank is respected`() {
        // ignoring the rank shows the fourth option first.
        val rows = listOf(row("c", rank = 2), row("a", rank = 0), row("b", rank = 1))
        assertEquals(listOf("a", "b", "c"), Shortcuts.usable(rows).map { it.id })
    }

    @Test
    fun `at equal rank the name decides`() {
        val rows = listOf(row("z", short = "Zebra"), row("a", short = "Anna"))
        assertEquals(listOf("Anna", "Zebra"), Shortcuts.usable(rows).map { Shortcuts.labelOf(it) })
    }

    @Test
    fun `the same shortcut appears only once`() {
        // a shortcut can be reported as dynamic and pinned at the same time.
        val rows = listOf(
            row("a", kind = ShortcutKind.DYNAMIC),
            row("a", kind = ShortcutKind.PINNED),
        )
        assertEquals(1, Shortcuts.usable(rows).size)
    }

    @Test
    fun `the same id in different apps stays apart`() {
        val rows = listOf(row("new", pkg = "com.chat"), row("new", pkg = "com.mail"))
        assertEquals(2, Shortcuts.usable(rows).size)
    }

    @Test
    fun `an app without a usable shortcut offers nothing`() {
        assertTrue(Shortcuts.usable(emptyList()).isEmpty())
        assertTrue(Shortcuts.usable(listOf(row("a", enabled = false))).isEmpty())
        assertTrue(Shortcuts.usable(listOf(row("a"))).isNotEmpty())
    }
}
