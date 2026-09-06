package dev.kicker.hometiles.security

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "done" without a single digit is no button.
 *
 * on 04.09.2026: the app lock stood there, no dot filled, a tap on done - and the answer was
 * that the pin was wrong. it was not: nothing had been entered. whoever takes the notice
 * seriously looks for the fault in themselves and never tries their correct pin.
 *
 * the same handle as sending without text (`SmsActivity`) and calling without a number
 * (`DialerActivity`): no colour, no `onClick`.
 *
 * with a **too short** entry it stays a button: three digits are an entry, and "wrong" is
 * then true. the boundary lies at zero, not at [dev.kicker.hometiles.security.Pin.MIN_LENGTH] -
 * otherwise the button would betray how long a pin has to be to anyone holding the phone.
 */
class EmptyPinButtonTest {

    private val button = Quelltext.cut(
        Quelltext.withoutComments("dev/kicker/hometiles/ui/PinGate.kt"),
        from = "BigRow(\n            label = confirmLabel,",
        to = "\n        )",
    )

    @Test
    fun `without a digit no colour`() {
        assertTrue(
            "the done button always carries the accent colour: $button\n" +
                "without an entry it can do nothing; then it must not look as if it could.",
            "surface = if (entered.isEmpty())" in button,
        )
    }

    @Test
    fun `without a digit no onClick`() {
        assertTrue(
            "the done button can be pressed without an entry: $button\n" +
                "it then answers that the pin is wrong to a pin nobody entered.",
            "onClick = if (entered.isEmpty())" in button && "null" in button,
        )
    }
}
