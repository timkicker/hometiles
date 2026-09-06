package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a setting holds for all messages, not for some.
 *
 * the sms channel honoured the vibration setting; the mms hint channel did not, and stood
 * beside it noting that it "hangs on no vibration setting", as if that were a property rather
 * than a fault. gaps like that are hard to notice because they only show in the rarer case.
 */
class OneSettingTest {

    private val source = Quelltext.file("dev/kicker/hometiles/sms/SmsNotifications.kt").readText()

    @Test
    fun `every message channel goes by the vibration setting`() {
        // only the constructor at the start of a line: `createNotificationChannel(` and
        // `deleteNotificationChannel(` carry the same name and were counted three times over.
        val channels = Regex("""(?m)^\s*NotificationChannel\(""")
            .findAll(source)
            .map { it.range.last }
            .toList()
        assertTrue("there are no channels left - does the rule still read what it means?", channels.size >= 2)

        val without = channels.filterNot { from ->
            // the send-failure channel is not an incoming message but an answer to one's own
            // action, so it may behave differently. its name stands **behind** the bracket.
            //
            // 900 characters, because the comment above the line belongs to the block: at 500
            // the reasoning for the fix pushed the fix itself out of the window and the rule
            // reported exactly what it had just been given.
            val block = source.substring(from, minOf(source.length, from + 900))
            "ERROR_CHANNEL" in block.take(80) || "enableVibration" in block
        }
        assertEquals(
            "a message channel stands here that does not know the vibration setting. a " +
                "setting holding for part of the messages is none.",
            emptyList<Int>(),
            without,
        )
    }

    /**
     * and the setting has to stand in the channel's **id**.
     *
     * a channel is immutable once created: `createNotificationChannel` on an existing one
     * changes name and description and nothing else, so sound and vibration stay as they were
     * the very first time. seen on the device *after* the fix was already in: the setting said
     * 500 ms and the channel reported `mVibrationEnabled=false`, left over from an earlier
     * try. a setting that only works after a reinstall is none.
     */
    @Test
    fun `a channel's id carries the setting`() {
        listOf("fun channelId(", "fun mmsChannelId(").forEach { name ->
            val from = source.indexOf(name)
            assertTrue("$name no longer exists", from > 0)
            assertTrue(
                "$name does not put the vibration length into the id - then a channel once " +
                    "created keeps its old setting forever.",
                "vibrationMs" in source.substring(from, from + 120),
            )
        }
        listOf("CHANNEL_PREFIX", "MMS_CHANNEL_PREFIX").forEach { prefix ->
            assertTrue(
                "nothing is cleared up for $prefix - then several channels with the same " +
                    "name pile up in the system settings.",
                Regex("""startsWith\($prefix\)""").containsMatchIn(source),
            )
        }
    }

    @Test
    fun `the mms hint is handed the setting at all`() {
        assertTrue(
            "showMmsHint does not know the settings - then it cannot go by them.",
            "fun showMmsHint(context: Context, config: SmsConfig)" in source,
        )
        val receiver = Quelltext.file("dev/kicker/hometiles/sms/SmsComponents.kt").readText()
        assertTrue(
            "the wap push receiver does not pass the settings on.",
            "showMmsHint(context, ConfigStore" in receiver,
        )
    }
}
