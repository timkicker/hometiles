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
 * loud alarm and blinking light during the emergency call. `PLAN.md` 4.8.
 *
 * the message goes to people far away; someone who has fallen first needs the person two
 * rooms away, and this is the only part of the emergency call that works without a network.
 *
 * only *after* the countdown: a cancelled false alarm must stay silent, or someone who hits
 * the button in a supermarket has already set off a siren and turns the whole thing off.
 *
 * the sound runs on the alarm channel, which is loud even on silent. that is the point.
 */
object SosAlarm {

    /** slow enough to read as blinking. */
    const val BLINK_MS = 500L

    private var ringtone: Ringtone? = null
    private var blinking: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** both off means do not start at all. */
    fun active(config: SosConfig): Boolean = config.alarmSound || config.alarmFlash

    fun start(context: Context, config: SosConfig) {
        if (config.alarmSound) startSound(context)
        if (config.alarmFlash) startFlash(context)
    }

    fun stop(context: Context) {
        runCatching { ringtone?.stop() }
        ringtone = null
        blinking?.cancel()
        blinking = null
        // the light would stay on and the user would go looking for its switch.
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
        if (blinking != null) return
        blinking = scope.launch {
            while (isActive) {
                Flashlight.toggle(context)
                delay(BLINK_MS)
            }
        }
    }
}
