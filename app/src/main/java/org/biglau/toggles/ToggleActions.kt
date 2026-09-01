package org.biglau.toggles

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import org.biglau.R
import org.biglau.actions.Flashlight
import org.biglau.ui.Notice

/**
 * Fuehrt aus, was [Toggles] fuer moeglich haelt.
 *
 * Wo Android uns nicht schalten laesst, oeffnen wir die Systemblende - und sagen es auch.
 * Stillschweigend die Einstellungen aufzumachen, waere fuer jemanden, der eine Kachel
 * "WLAN" drueckt, ein Bruch des Versprechens.
 */
object ToggleActions {

    fun run(context: Context, kind: ToggleKind) {
        when (Toggles.actionFor(kind, Build.VERSION.SDK_INT)) {
            ToggleAction.SWITCH -> switch(context, kind)
            ToggleAction.PANEL -> openPanel(context, kind)
            ToggleAction.SETTINGS -> openSettings(context, kind)
        }
    }

    private fun switch(context: Context, kind: ToggleKind) {
        when (kind) {
            ToggleKind.FLASHLIGHT -> if (!Flashlight.toggle(context)) {
                Notice.show(context, R.string.toggle_no_flashlight)
            }

            ToggleKind.RINGER -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
                // Nur zwischen Klingeln und Vibrieren: Lautlos verlangt Zugriff auf
                // "Nicht stoeren", und den erfragen wir nicht fuer einen Schalter.
                val next = if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                    AudioManager.RINGER_MODE_VIBRATE
                } else {
                    AudioManager.RINGER_MODE_NORMAL
                }
                runCatching { audio.ringerMode = next }
            }

            ToggleKind.BLUETOOTH -> {
                @Suppress("DEPRECATION")
                val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
                runCatching {
                    @Suppress("DEPRECATION")
                    if (adapter.isEnabled) adapter.disable() else adapter.enable()
                }.onFailure { openSettings(context, kind) }
            }

            else -> openSettings(context, kind)
        }
    }

    private fun openPanel(context: Context, kind: ToggleKind) {
        val action = when (kind) {
            ToggleKind.WIFI -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Settings.Panel.ACTION_WIFI
            } else {
                Settings.ACTION_WIFI_SETTINGS
            }
            ToggleKind.MOBILE_DATA -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Settings.Panel.ACTION_INTERNET_CONNECTIVITY
            } else {
                Settings.ACTION_DATA_ROAMING_SETTINGS
            }
            else -> Settings.ACTION_SETTINGS
        }
        start(context, Intent(action))
    }

    private fun openSettings(context: Context, kind: ToggleKind) {
        val action = when (kind) {
            ToggleKind.AIRPLANE -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
            ToggleKind.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
            ToggleKind.WIFI -> Settings.ACTION_WIFI_SETTINGS
            ToggleKind.MOBILE_DATA -> Settings.ACTION_DATA_ROAMING_SETTINGS
            ToggleKind.LOCATION -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            ToggleKind.BRIGHTNESS -> Settings.ACTION_DISPLAY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        start(context, Intent(action))
    }

    private fun start(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
