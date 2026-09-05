package org.biglau.notify

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** how many notices each app has open, so tiles can blink. */
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
     * the allowed services as they stand in `enabled_notification_listeners`.
     *
     * parsed here because the system allows two spellings: `package/full.Class` and the
     * short `package/.Class`. comparing the package alone would say yes for a second
     * service of ours that is not the enabled one.
     */
    object ListenerList {

        fun contains(flat: String, packageName: String, className: String): Boolean =
            flat.split(':').map { it.trim() }.any { entry ->
                entry == "$packageName/$className" ||
                    entry == "$packageName/" + className.removePrefix(packageName)
            }
    }
}
