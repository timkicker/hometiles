package org.biglau.toggles

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.media.Ringtone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import org.biglau.actions.Flashlight
import org.biglau.data.SosConfig

/**
 * Lauter Alarmton und blinkendes Licht während des Notrufs. `PLAN.md` 4.8.
 *
 * Wofür: die Nachricht geht an Menschen, die weit weg sind. Wer gestürzt ist, braucht aber
 * zuerst den, der zwei Räume weiter steht - und der hört und sieht ein Telefon, das lärmt
 * und blinkt. Beides ist der einzige Teil des Notrufs, der ohne Netz wirkt.
 *
 * **Erst nach dem Countdown**, nicht währenddessen: ein abgebrochener Fehlalarm soll still
 * bleiben. Wer im Supermarkt versehentlich auf den Knopf kommt und ihn wegdrückt, hat sonst
 * schon eine Sirene ausgelöst - und schaltet den Notruf danach ganz ab.
 *
 * Der Ton läuft auf dem **Wecker-Kanal**: der ist auch dann laut, wenn das Telefon auf
 * lautlos steht. Genau darum geht es.
 */
object SosAlarm {

    /** Wie schnell das Licht blinkt. Langsam genug, dass es als Blinken zu erkennen ist. */
    const val BLINK_MS = 500L

    private var ringtone: Ringtone? = null
    private var blinken: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Läuft überhaupt etwas? Beides aus heisst: gar nicht erst anfangen. */
    fun active(config: SosConfig): Boolean = config.alarmSound || config.alarmFlash

    fun start(context: Context, config: SosConfig) {
        if (config.alarmSound) startSound(context)
        if (config.alarmFlash) startFlash(context)
    }

    fun stop(context: Context) {
        runCatching { ringtone?.stop() }
        ringtone = null
        blinken?.cancel()
        blinken = null
        // Das Licht bleibt sonst an, und der Nutzer sucht den Schalter dafür.
        if (Flashlight.on.value) Flashlight.toggle(context)
    }

    private fun startSound(context: Context) {
        if (ringtone?.isPlaying == true) return
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        runCatching {
            RingtoneManager.getRingtone(context, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                isLooping = true
                play()
                ringtone = this
            }
        }
    }

    private fun startFlash(context: Context) {
        if (blinken != null) return
        blinken = scope.launch {
            while (isActive) {
                Flashlight.toggle(context)
                delay(BLINK_MS)
            }
        }
    }
}
