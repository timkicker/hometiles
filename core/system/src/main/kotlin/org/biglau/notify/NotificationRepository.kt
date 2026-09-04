package org.biglau.notify

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wie viele Meldungen je App offen sind - damit Kacheln blinken koennen.
 *
 * Stand bis zum 03.09.2026 mitten in `BigNotificationListener.kt`, und war deshalb fuer
 * `AblagenTest` unsichtbar: die Regel sah nur Dateinamen. Jetzt hat sie eine eigene Datei
 * und ist auffindbar.
 *
 * Sie bleibt in `:app` und nicht in `core:system` wie die anderen sieben Ablagen, weil
 * `isEnabled` unseren **eigenen** Dienst beim Namen nennen muss: das System fuehrt in
 * `enabled_notification_listeners` genau diese Klasse, keine Oberklasse tut es. Das steht
 * so auch in der Ausnahmeliste von `AblagenTest`.
 */
object NotificationRepository {

    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    fun publish(value: Map<String, Int>) {
        _counts.value = value
    }

    fun isEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val us = ComponentName(context, BigNotificationListener::class.java)
        return ListenerList.contains(flat, us.packageName, us.className)
    }

    /**
     * Die Liste der erlaubten Dienste, wie sie in `enabled_notification_listeners` steht.
     *
     * Eigene Zerlegung, weil das System zwei Schreibweisen zulaesst: `paket/vollstaendige
     * .Klasse` und die Kurzform `paket/.Klasse`. Bisher wurde nur das Paket verglichen -
     * das ging gut, solange BigLau genau einen solchen Dienst hat, haette aber beim
     * zweiten stillschweigend "ja" gesagt, obwohl der falsche erlaubt ist.
     */
    object ListenerList {

        fun contains(flat: String, packageName: String, className: String): Boolean =
            flat.split(':').map { it.trim() }.any { eintrag ->
                eintrag == "$packageName/$className" ||
                    eintrag == "$packageName/" + className.removePrefix(packageName)
            }
    }
}
