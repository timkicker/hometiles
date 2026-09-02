package org.biglau.safety

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Zaehlt Fehlstarts und haelt den letzten Absturz fest.
 *
 * Bewusst SharedPreferences und nicht die Konfigurationsdatei: wenn die kaputt ist, muss
 * dieser Zaehler trotzdem lesbar bleiben - sonst kaeme man aus dem Notmodus nie heraus.
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

    /** Haengt sich in den Standard-Handler ein, ohne ihn zu ersetzen. */
    fun installHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(error) }
            previous?.uncaughtException(thread, error)
        }
    }

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
