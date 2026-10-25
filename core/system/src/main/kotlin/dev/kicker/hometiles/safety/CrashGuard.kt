package dev.kicker.hometiles.safety

enum class StartMode { NORMAL, SAFE }

/**
 * remembers whether the last start got through.
 *
 * a crash in a side screen once cost hometiles the launcher role: android clears preferred
 * activities after repeated crashes, and without a second phone or adb one is then left with
 * no home screen.
 *
 * two crashed starts in a row, before the one now running, switch to a stripped-down mode
 * which shows only what one needs to get back, and loads no config, because the config might
 * be the problem. **two** on purpose: a single crash can be a slip, and landing in safe mode
 * after every slip destroys trust in the app. the count includes the running start, hence
 * a threshold of three.
 *
 * a start that never drew counts only if it ended in a crash. an update, the low memory
 * killer or switching off end a start just as well, and a single one of those put the next
 * start into safe mode.
 */
object CrashGuard {

    const val THRESHOLD = 3

    fun modeFor(consecutiveFailedStarts: Int): StartMode =
        if (consecutiveFailedStarts >= THRESHOLD) StartMode.SAFE else StartMode.NORMAL

    /**
     * counted up at start; it only goes down once something was really drawn. an earlier
     * start that did not draw and did not crash is forgiven, so only this start counts.
     */
    fun onStart(previous: Int, previousCrashed: Boolean): Int =
        if (previous > 0 && !previousCrashed) 1 else (previous + 1).coerceAtMost(THRESHOLD * 5)

    fun onRendered(): Int = 0
}
