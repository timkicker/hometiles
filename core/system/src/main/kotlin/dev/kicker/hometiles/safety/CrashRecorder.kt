package dev.kicker.hometiles.safety

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * counts failed starts and keeps the last crash.
 *
 * SharedPreferences and not the config file: a broken config must still leave this counter
 * readable, or there is no way out of safe mode.
 */
class CrashRecorder(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("safety", Context.MODE_PRIVATE)
    private val traceFile = File(appContext.filesDir, "last-crash.txt")

    var failedStarts: Int
        get() = prefs.getInt(KEY_FAILED, 0)
        private set(value) = prefs.edit().putInt(KEY_FAILED, value).commit().let { }

    fun noteStart(): Int {
        val next = CrashGuard.onStart(failedStarts)
        failedStarts = next
        return next
    }

    fun noteRendered() {
        if (failedStarts != 0) failedStarts = CrashGuard.onRendered()
    }

    /** the handler we installed, for [armed]. */
    private var ours: Thread.UncaughtExceptionHandler? = null

    /** chains into the default handler instead of replacing it. */
    fun installHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val handler = Thread.UncaughtExceptionHandler { thread, error ->
            runCatching { write(error) }
            previous?.uncaughtException(thread, error)
        }
        ours = handler
        Thread.setDefaultUncaughtExceptionHandler(handler)
    }

    /**
     * is our net still hanging?
     *
     * anyone calling `setDefaultUncaughtExceptionHandler` later without chaining the
     * previous one unhooks it silently, and that is invisible unless asked.
     */
    fun armed(): Boolean = ours != null && Thread.getDefaultUncaughtExceptionHandler() === ours

    private fun write(error: Throwable) {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        traceFile.writeText("$stamp\n\n$trace")
    }

    fun lastCrash(): String? = runCatching {
        if (traceFile.exists()) traceFile.readText().take(4000) else null
    }.getOrNull()

    fun clearCrash() {
        runCatching { traceFile.delete() }
        failedStarts = 0
    }

    companion object {
        private const val KEY_FAILED = "failedStarts"

        @Volatile
        private var instance: CrashRecorder? = null

        fun get(context: Context): CrashRecorder =
            instance ?: synchronized(this) {
                instance ?: CrashRecorder(context).also { instance = it }
            }
    }
}
