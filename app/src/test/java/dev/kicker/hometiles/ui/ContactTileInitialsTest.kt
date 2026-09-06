package dev.kicker.hometiles.ui

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a contact tile without a photo carries the initials.
 *
 * PLAN.md 3.4 promises exactly that. drawn instead was the same person icon on **every**
 * contact tile - three contacts side by side looked alike, and the icon said nothing the
 * label did not say already.
 */
class ContactTileInitialsTest {

    private val branch = Quelltext.file("dev/kicker/hometiles/ui/HomeScreenView.kt")
        .readText()
        .let { Quelltext.cut(it, "is ButtonAction.Contact -> BigTile(", "is ButtonAction.Shortcut") }

    @Test
    fun `the contact tile passes initials on`() {
        assertTrue("initials is missing on the contact tile", "initials =" in branch)
        assertTrue("tileInitials is missing", "tileInitials(" in branch)
    }

    @Test
    fun `no general person icon any more`() {
        assertTrue(
            "a person icon on every contact tile carries nothing: $branch",
            "Builtin.CONTACTS.icon()" !in branch,
        )
    }

    /** two letters, and one from a single-part name. see [initialsOf]. */
    @Test
    fun `the initials come from the name`() {
        assertEquals("AB", tileInitials("Anna Bauer"))
        assertEquals("FM", tileInitials("Franz Moser"))
        assertEquals("A", tileInitials("Alex"))
    }

    /**
     * a tile named after a number carried the initials "00". two zeros say nothing and look
     * like a fault; on the tile empty is better than wrong.
     */
    @Test
    fun `a number gets no initials`() {
        assertNull(tileInitials("081 234 56"))
        assertNull(tileInitials("+43 664 000 111"))
        assertNull(tileInitials(""))
    }
}
