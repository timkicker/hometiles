package org.biglau.sms

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the message notice's channels clear themselves away.
 *
 * android does not let a notification channel's vibration be changed after it is created.
 * BigLau therefore creates a channel per vibration length (`sms-500`) and deletes the others
 * next time. that hangs on the identifier and the clearing using the **same prefix**.
 *
 * if the two drifted apart, silent channels nobody ever uses again would pile up in the
 * user's system settings. that would be visible only there - not in the app, in no test, in
 * no notice.
 */
class SmsChannelTest {

    private val source = Quelltext.withoutComments("org/biglau/sms/SmsNotifications.kt")

    @Test
    fun `identifier and clearing use the same prefix`() {
        assertTrue(
            "channelId does not build the identifier from CHANNEL_PREFIX",
            "\"\$CHANNEL_PREFIX\$vibrationMs\"" in source,
        )
        assertTrue(
            "the clearing does not look for CHANNEL_PREFIX - then old channels stay behind",
            "startsWith(CHANNEL_PREFIX)" in source,
        )
        // the definition itself is the one allowed place - it **is** the prefix. the first
        // version of this check counted it and fell over its own subject.
        val hardcoded = source.lines()
            .filterNot { "const val CHANNEL_PREFIX" in it }
            .count { "\"sms-\"" in it }
        assertEquals(
            "a hardcoded \"sms-\" beside the definition - then the places drift apart one day",
            0,
            hardcoded,
        )
    }

    @Test
    fun `every offered vibration length gives its own identifier`() {
        val lengths = SmsNotifications.VIBRATION_CHOICES
        assertEquals(
            "two lengths with the same identifier - then one would keep the other's vibration",
            lengths.size,
            lengths.map { SmsNotifications.channelId(it) }.toSet().size,
        )
    }
}
