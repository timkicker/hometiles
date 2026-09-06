package dev.kicker.hometiles.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.ui.graphics.vector.ImageVector
import dev.kicker.hometiles.R
import dev.kicker.hometiles.data.Builtin

fun Builtin.icon(): ImageVector = when (this) {
    Builtin.DIALER -> Icons.Filled.Call
    Builtin.MESSAGES -> Icons.AutoMirrored.Filled.Message
    Builtin.CONTACTS -> Icons.Filled.Person
    Builtin.CAMERA -> Icons.Filled.PhotoCamera
    Builtin.CLOCK -> Icons.Filled.Schedule
    Builtin.CALCULATOR -> Icons.Filled.Calculate
    Builtin.APP_LIST -> Icons.Filled.Apps
    Builtin.SETTINGS -> Icons.Filled.Settings
    Builtin.FLASHLIGHT -> Icons.Filled.FlashlightOn
    Builtin.SOS -> Icons.Filled.Warning
    Builtin.NEXT_SCREEN -> Icons.AutoMirrored.Filled.ArrowForward
    Builtin.PREV_SCREEN -> Icons.AutoMirrored.Filled.ArrowBack
    Builtin.HOME_SCREEN -> Icons.Filled.Home
    Builtin.BATTERY -> Icons.Filled.BatteryFull
    Builtin.MISSED_CALLS -> Icons.Filled.CallMissed
    Builtin.WIFI -> Icons.Filled.Wifi
    Builtin.BLUETOOTH -> Icons.Filled.Bluetooth
    Builtin.AIRPLANE -> Icons.Filled.AirplanemodeActive
    Builtin.RINGER -> Icons.AutoMirrored.Filled.VolumeUp
    Builtin.SIGNAL -> Icons.Filled.SignalCellularAlt
    Builtin.MOBILE_DATA -> Icons.Filled.NetworkCell
    Builtin.LOCATION -> Icons.Filled.LocationOn
    Builtin.BRIGHTNESS -> Icons.Filled.BrightnessMedium
    Builtin.ANDROID_SETTINGS -> Icons.Filled.Tune
    Builtin.CALL_LOG -> Icons.Filled.History
    Builtin.FAVOURITES -> Icons.Filled.Star
    Builtin.RECENT_APPS -> Icons.Filled.Restore
}

fun Builtin.labelRes(): Int = when (this) {
    Builtin.DIALER -> R.string.phone
    Builtin.MESSAGES -> R.string.messages
    Builtin.CONTACTS -> R.string.contacts
    Builtin.CAMERA -> R.string.camera
    Builtin.CLOCK -> R.string.clock
    Builtin.CALCULATOR -> R.string.calculator
    Builtin.APP_LIST -> R.string.apps
    Builtin.SETTINGS -> R.string.settings
    Builtin.FLASHLIGHT -> R.string.flashlight
    Builtin.SOS -> R.string.sos
    Builtin.NEXT_SCREEN -> R.string.next_screen
    Builtin.PREV_SCREEN -> R.string.prev_screen
    Builtin.HOME_SCREEN -> R.string.home_screen
    Builtin.BATTERY -> R.string.battery
    Builtin.MISSED_CALLS -> R.string.missed_calls
    Builtin.WIFI -> R.string.toggle_wifi
    Builtin.BLUETOOTH -> R.string.toggle_bluetooth
    Builtin.AIRPLANE -> R.string.toggle_airplane
    Builtin.RINGER -> R.string.toggle_ringer
    Builtin.SIGNAL -> R.string.signal
    Builtin.MOBILE_DATA -> R.string.toggle_mobile_data
    Builtin.LOCATION -> R.string.toggle_location
    Builtin.BRIGHTNESS -> R.string.toggle_brightness
    Builtin.ANDROID_SETTINGS -> R.string.android_settings
    Builtin.CALL_LOG -> R.string.calllog
    Builtin.FAVOURITES -> R.string.favourites
    Builtin.RECENT_APPS -> R.string.apps_recent
}
