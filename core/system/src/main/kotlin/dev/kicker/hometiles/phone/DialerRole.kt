package dev.kicker.hometiles.phone

import android.content.Context
import android.telecom.TelecomManager

/**
 * does hometiles hold the dialler role?
 *
 * more hangs off this than it looks: **only the default phone app sees incoming calls** and
 * can turn them away. without the role the number block works outwards only - a blocked
 * number cannot be dialled from hometiles, but whoever calls still rings through the system's
 * phone app.
 *
 * the settings promised exactly that ("calls from these numbers are turned away without
 * ringing") without anyone checking.
 */
object DialerRole {

    fun held(context: Context): Boolean =
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage == context.packageName
}
