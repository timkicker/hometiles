package dev.kicker.hometiles.toggles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SosNumbersTest {

    @Test
    fun `a line with commas is split`() {
        assertEquals(
            listOf("+436601234567", "+4351299988"),
            SosNumbers.parse("+43 660 123 45 67, +43 512 999 88"),
        )
    }

    @Test
    fun `semicolon and line break separate too`() {
        // nobody should have to guess which character is meant.
        assertEquals(listOf("111", "222"), SosNumbers.parse("111; 222"))
        assertEquals(listOf("111", "222"), SosNumbers.parse("111\n222"))
    }

    @Test
    fun `unusable entries fly out`() {
        // what slips through here is, in an emergency, a message that never arrives.
        assertEquals(listOf("111"), SosNumbers.parse("111, Alex, ---"))
    }

    @Test
    fun `the sorted-out entries can be named`() {
        assertEquals(listOf("Alex", "---"), SosNumbers.rejected("111, Alex, ---"))
        assertTrue(SosNumbers.rejected("111, 222").isEmpty())
    }

    @Test
    fun `the same number stands in it only once`() {
        assertEquals(listOf("+43660"), SosNumbers.parse("+43 660, +43660"))
    }

    @Test
    fun `the number of recipients is bounded`() {
        val many = (1..20).joinToString(",") { "+4366012345$it" }
        assertEquals(SosNumbers.MAX, SosNumbers.parse(many).size)
    }

    @Test
    fun `empty input gives no recipients`() {
        assertTrue(SosNumbers.parse("").isEmpty())
        assertTrue(SosNumbers.parse("  ,  ; ").isEmpty())
    }

    @Test
    fun `reading in and writing out match`() {
        val numbers = SosNumbers.parse("+43660, +43512")
        assertEquals(numbers, SosNumbers.parse(SosNumbers.format(numbers)))
    }

    @Test
    fun `dialling characters are kept`() {
        assertEquals(listOf("*100#"), SosNumbers.parse("*100#"))
    }
}
