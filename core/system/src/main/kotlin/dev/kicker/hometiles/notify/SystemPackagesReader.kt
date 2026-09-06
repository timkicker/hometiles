package dev.kicker.hometiles.notify

import android.content.Context
import android.os.Build
import android.provider.Telephony
import android.telecom.TelecomManager

/** null means none set or not readable, and then the matching tile simply does not blink. */
data class SystemPackages(
    val sms: String? = null,
    val dialer: String? = null,
)

object SystemPackagesReader {

    fun read(context: Context): SystemPackages = SystemPackages(
        sms = runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull(),
        // no version check: the telecom manager is there from android 6 on, minSdk is 26.
        dialer = runCatching {
            context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
        }.getOrNull(),
    )
}
