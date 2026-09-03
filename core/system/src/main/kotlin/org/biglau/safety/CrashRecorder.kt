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

    /** Der Fangnetz-Handler, den wir gesetzt haben - fuer [armed]. */
    private var ours: Thread.UncaughtExceptionHandler? = null

    /** Haengt sich in den Standard-Handler ein, ohne ihn zu ersetzen. */
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
     * Haengt unser Netz noch?
     *
     * Am 03.09.2026 stand in der Diagnose „letzter Absturz: keiner", obwohl es um 02:24
     * einen gegeben hatte - und `files/last-crash.txt` fehlte. Der Handler wird seit dem
     * 31.08. als Erstes in `BigLauApp` gesetzt; er haette also schreiben muessen.
     *
     * Wer spaeter `setDefaultUncaughtExceptionHandler` ruft und den vorherigen **nicht**
     * weiterreicht, haengt unser Netz lautlos aus. Das ist von aussen nicht zu sehen -
     * ausser man fragt nach. Genau dafuer ist das hier: eine Zeile in der Diagnose, die
     * „ja" oder „nein" sagt, statt einer Vermutung.
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
