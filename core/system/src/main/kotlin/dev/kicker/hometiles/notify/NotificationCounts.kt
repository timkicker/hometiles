package dev.kicker.hometiles.notify

/** one notification as far as it concerns us, without android types so it can be tested. */
data class NotificationRow(
    val packageName: String,
    val clearable: Boolean,
    val ongoing: Boolean,
    val groupSummary: Boolean,
    val number: Int = 0,
    /** what the app itself says the notification is for. most say nothing. */
    val category: String? = null,
    /** the media template means a playback, not a message. */
    val mediaStyle: Boolean = false,
    /** the channel the app posted it on; the only mark a voicemail notice carries. */
    val channel: String? = null,
)

/**
 * counts notifications per package.
 *
 * miscounting here makes a tile blink for no reason, and a tile that always blinks is worse
 * than no hint at all, because one learns to ignore it - and then misses the one that counts.
 */
object NotificationCounts {

    fun summarise(rows: List<NotificationRow>): Map<String, Int> = rows
        .filter { counts(it) }
        .groupingBy { it.packageName }
        .eachCount()

    /**
     * notifications that are not a message to the user.
     *
     * android lets the app say what its notification is for. these are notices **about
     * something** running or holding, not something one would answer: playback, a background
     * service, progress, navigation, the running call, the alarm, a stopwatch, a status line.
     */
    private val NOT_MEANT = setOf(
        "transport", "service", "progress", "navigation", "call", "alarm", "stopwatch",
        "sys", "status",
    )

    /**
     * `ongoing` alone is not enough: a media notification is only `ongoing` while playing, so
     * paused it becomes swipeable and counted as a message, although it is the same notice
     * and reports nothing new.
     */
    fun counts(row: NotificationRow): Boolean = when {
        row.ongoing -> false        // playback, navigation, file transfer
        row.mediaStyle -> false     // media template: a playback, paused or not
        row.category in NOT_MEANT -> false
        !row.clearable -> false     // cannot be swiped away, so not a message to the user
        row.groupSummary -> false   // only reports what the single entries already report
        else -> true
    }

    /**
     * waiting voicemail. the phone service says so in one notice on its channel `voiceMail`
     * (emulator, voicemail indicator from the network, 25.10.2026); it has no category and its
     * package posts other things too, so the channel is what tells it apart. issue #2.
     */
    fun voicemail(rows: List<NotificationRow>): Int =
        rows.count { it.channel?.contains("voicemail", ignoreCase = true) == true }

    /** past nine it stacks instead of counting. */
    fun badgeText(count: Int): String? = when {
        count <= 0 -> null
        count > 9 -> "9+"
        else -> count.toString()
    }
}
