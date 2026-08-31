package org.biglau.notify

import android.content.Context
import android.os.Build
import android.provider.Telephony
import android.telecom.TelecomManager

/** Liest die aktuell eingestellten Standard-Apps. Alles gekapselt, damit die Zuordnung testbar bleibt. */
object SystemPackagesReader {

    fun read(context: Context): SystemPackages = SystemPackages(
        sms = runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull(),
        dialer = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
            } else {
                null
            }
        }.getOrNull(),
    )
}
