package dev.kicker.hometiles.phone

import dev.kicker.hometiles.data.CallerPhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * how large the caller's photo may get (`PLAN.md` 4.6).
 *
 * the point is the upper bound. on the screen where a fault means somebody cannot answer a
 * call, a photo must never push the answer button out - not even at the step "as large as it
 * fits".
 */
class CallerPhotoSizeTest {

    /** the jelly 2's usable height (PLAN.md 3.2). */
    private val jelly = 581f

    /** it rings: answer and reject. */
    private val ringing = 2

    /** the call runs: hang up, mute, speaker, hold, keypad. */
    private val inCall = 5

    @Test
    fun `without a photo no room is taken`() {
        assertEquals(0f, CallerPhotoSize.heightDp(CallerPhoto.OFF, jelly, ringing), 0.01f)
    }

    @Test
    fun `the steps get larger`() {
        val small = CallerPhotoSize.heightDp(CallerPhoto.SMALL, jelly, ringing)
        val half = CallerPhotoSize.heightDp(CallerPhoto.HALF, jelly, ringing)
        val full = CallerPhotoSize.heightDp(CallerPhoto.FULL, jelly, ringing)
        assertTrue("small < half", small < half)
        assertTrue("half <= full", half <= full)
    }

    @Test
    fun `even the largest step leaves room for the name and the buttons`() {
        CallerPhoto.entries.forEach { step ->
            val height = CallerPhotoSize.heightDp(step, jelly, ringing)
            assertTrue(
                "$step leaves only ${jelly - height} dp",
                jelly - height >= CallerPhotoSize.reservedDp(ringing),
            )
        }
    }

    @Test
    fun `on a very short screen the photo goes entirely`() {
        // less room than the rest needs: then rather no photo than no button.
        assertEquals(0f, CallerPhotoSize.heightDp(CallerPhoto.FULL, 200f, ringing), 0.01f)
    }

    @Test
    fun `the height never goes negative`() {
        CallerPhoto.entries.forEach { step ->
            assertTrue(CallerPhotoSize.heightDp(step, 50f, ringing) >= 0f)
        }
    }

    @Test
    fun `small really stays small on the jelly 2`() {
        // barely a fifth - enough to recognise a face without crowding out the name.
        assertEquals(104.6f, CallerPhotoSize.heightDp(CallerPhoto.SMALL, jelly, ringing), 1f)
    }

    // --- photo, initials or nothing ---

    /**
     * with "half the display" only two of five buttons stood in the picture after answering.
     * the room for the photo has to depend on the number of buttons, not on a fixed number.
     */
    @Test
    fun `during the call all five buttons stay in the picture`() {
        CallerPhoto.entries.forEach { step ->
            val height = CallerPhotoSize.heightDp(step, jelly, inCall)
            val left = jelly - height
            assertTrue(
                "$step leaves only $left dp for five buttons",
                left >= CallerPhotoSize.reservedDp(inCall) ||
                    // or the photo goes entirely - then everything is free anyway.
                    height == 0f,
            )
        }
    }

    @Test
    fun `more buttons leave the photo less room`() {
        val atTwo = CallerPhotoSize.heightDp(CallerPhoto.FULL, 900f, ringing)
        val atFive = CallerPhotoSize.heightDp(CallerPhoto.FULL, 900f, inCall)
        assertTrue("$atFive < $atTwo", atFive < atTwo)
    }

    @Test
    fun `on three inches the photo yields entirely during a call`() {
        // 581 dp minus the header, five buttons and the gap against the ear: no strip is left
        // that could show a face.
        assertEquals(0f, CallerPhotoSize.heightDp(CallerPhoto.FULL, jelly, inCall), 0.01f)
    }

    @Test
    fun `the gap against the ear sits in the reservation`() {
        val without = CallerPhotoSize.HEADER_DP + inCall * CallerPhotoSize.ROW_DP +
            (inCall + 1) * CallerPhotoSize.ROW_GAP_DP
        assertEquals(
            CallerPhotoSize.EAR_GAP_DP,
            CallerPhotoSize.reservedDp(inCall) - without,
            0.01f,
        )
    }

    @Test
    fun `with a photo the photo stands there`() {
        assertEquals(
            CallerPhotoSize.Image.PHOTO,
            CallerPhotoSize.imageFor(200f, "content://foto/1", "Anna Bauer"),
        )
    }

    /** found on the emulator: half the area reserved, no photo, and it all stayed black. */
    @Test
    fun `without a photo but with a name, the initials`() {
        assertEquals(CallerPhotoSize.Image.INITIALS, CallerPhotoSize.imageFor(200f, null, "Anna Bauer"))
    }

    /** out of "+43" no character could be made that means anything. */
    @Test
    fun `an unknown number gets nothing`() {
        assertEquals(CallerPhotoSize.Image.NONE, CallerPhotoSize.imageFor(200f, null, null))
        assertEquals(CallerPhotoSize.Image.NONE, CallerPhotoSize.imageFor(200f, null, "  "))
    }

    @Test
    fun `without a reserved height nothing stands there`() {
        assertEquals(CallerPhotoSize.Image.NONE, CallerPhotoSize.imageFor(0f, "content://foto/1", "Anna Bauer"))
    }

    @Test
    fun `the notice line for a second call takes room from the photo`() {
        val without = CallerPhotoSize.heightDp(CallerPhoto.HALF, jelly, ringing, notice = false)
        val with = CallerPhotoSize.heightDp(CallerPhoto.HALF, jelly, ringing, notice = true)
        assertTrue("$with < $without", with < without)
        assertEquals(
            CallerPhotoSize.NOTICE_DP,
            CallerPhotoSize.reservedDp(ringing, notice = true) -
                CallerPhotoSize.reservedDp(ringing),
            0.01f,
        )
    }
}
