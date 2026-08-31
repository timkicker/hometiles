package org.biglau.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import org.biglau.R
import org.biglau.data.Builtin

fun Builtin.icon(): ImageVector = when (this) {
    Builtin.DIALER -> Icons.Filled.Call
    Builtin.MESSAGES -> Icons.AutoMirrored.Filled.Message
    Builtin.CONTACTS -> Icons.Filled.Person
    Builtin.CAMERA -> Icons.Filled.PhotoCamera
    Builtin.CLOCK -> Icons.Filled.Schedule
    Builtin.APP_LIST -> Icons.Filled.Apps
    Builtin.SETTINGS -> Icons.Filled.Settings
    Builtin.FLASHLIGHT -> Icons.Filled.FlashlightOn
    Builtin.SOS -> Icons.Filled.Warning
    Builtin.NEXT_SCREEN -> Icons.AutoMirrored.Filled.ArrowForward
    Builtin.PREV_SCREEN -> Icons.AutoMirrored.Filled.ArrowBack
    Builtin.HOME_SCREEN -> Icons.Filled.Home
    Builtin.BATTERY -> Icons.Filled.BatteryFull
}

fun Builtin.labelRes(): Int = when (this) {
    Builtin.DIALER -> R.string.phone
    Builtin.MESSAGES -> R.string.messages
    Builtin.CONTACTS -> R.string.contacts
    Builtin.CAMERA -> R.string.camera
    Builtin.CLOCK -> R.string.clock
    Builtin.APP_LIST -> R.string.apps
    Builtin.SETTINGS -> R.string.settings
    Builtin.FLASHLIGHT -> R.string.flashlight
    Builtin.SOS -> R.string.sos
    Builtin.NEXT_SCREEN -> R.string.next_screen
    Builtin.PREV_SCREEN -> R.string.prev_screen
    Builtin.HOME_SCREEN -> R.string.home_screen
    Builtin.BATTERY -> R.string.battery
}
