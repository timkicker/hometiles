package dev.kicker.hometiles.safety

enum class StartMode { NORMAL, SAFE }

/**
 * remembers whether the last start got through.
 *
 * a crash in a side screen once cost hometiles the launcher role: android clears preferred
 * activities after repeated crashes, and without a second phone or adb one is then left with
 * no home screen.
 *
 * two consecutive starts that never reached drawing switch to a stripped-down mode which
 * shows only what one needs to get back, and loads no config, because the config might be
 * the problem. **two** on purpose: a single crash can be a slip, and landing in safe mode
 * after every slip destroys trust in the app.
 */
object CrashGuard {

    const val THRESHOLD = 2

    fun modeFor(consecutiveFailedStarts: Int): StartMode =
        if (consecutiveFailedStarts >= THRESHOLD) StartMode.SAFE else StartMode.NORMAL

    /** counted up at start; it only goes down once something was really drawn. */
    fun onStart(previous: Int): Int = (previous + 1).coerceAtMost(THRESHOLD * 5)

    fun onRendered(): Int = 0
}
