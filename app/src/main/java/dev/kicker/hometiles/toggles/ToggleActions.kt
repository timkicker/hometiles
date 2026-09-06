package dev.kicker.hometiles.toggles

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import dev.kicker.hometiles.R
import dev.kicker.hometiles.actions.Flashlight
import dev.kicker.hometiles.ui.Notice

/**
 * carries out what [Toggles] thinks possible.
 *
 * where android does not let us switch, the system panel opens, and we say so: opening the
 * settings silently breaks the promise a tile called wifi makes.
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
                // ring and vibrate only: silent needs do-not-disturb access, which we do
                // not ask for over a switch.
                val next = if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                    AudioManager.RINGER_MODE_VIBRATE
                } else {
                    AudioManager.RINGER_MODE_NORMAL
                }
                // with do-not-disturb on the system throws here: the ringer mode is no
                // longer ours, and without a word the switch would simply do nothing.
                runCatching { audio.ringerMode = next }
                    .onFailure { Notice.show(context, R.string.toggle_ringer_blocked) }
            }

            ToggleKind.BLUETOOTH -> {
                // `BluetoothManager` is older but hands out no adapter before android 12
                // without a permission.
                @Suppress("DEPRECATION")
                val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
                runCatching {
                    // `enable`/`disable` still switch on android 11 and are inert from 13
                    // on; the fallback to the settings sits right below.
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

    /**
     * the last stop, with no further fallback: not every device has every settings page, and
     * a silent failure means a tile that does nothing at all.
     */
    private fun start(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Notice.show(context, R.string.toggle_no_settings) }
    }
}
