package dev.kicker.hometiles.phone

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/** DialerBack decides, but only if the dialler asks it. see DialerBackTest. */
class DialerBackWiringTest {

    private val text = Quelltext.file("dev/kicker/hometiles/phone/DialerActivity.kt").readText()

    @Test
    fun `the dialler asks DialerBack and knows where it was opened`() {
        assertTrue("DialerActivity does not ask DialerBack", "DialerBack.step(" in text)
        assertTrue("the back handler ignores the answer", "backStep != DialerBackStep.LEAVE" in text)
        assertTrue("the dialler forgets it was opened on the log", "openedOnLog = openedOnLog" in text)
    }
}
