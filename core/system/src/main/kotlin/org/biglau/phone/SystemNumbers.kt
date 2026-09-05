package org.biglau.phone

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager

/**
 * the system half of [PhoneNumbers], the only place that touches android for it.
 *
 * [PhoneNumbers] only computes and therefore lives in `core:model`, where the compiler
 * enforces "no android". two facts cannot be fetched there: how a number is written (android
 * carries the dialling-code tables) and the sim's country. [install] hooks both up at start.
 *
 * without that call biglau writes numbers in plain blocks of three. that is no crash but a
 * quiet regression, so `StartAufgabenTest` checks the call is made.
 */
object SystemNumbers {

    fun install(context: Context) {
        PhoneNumbers.systemFormat = { number, country ->
            runCatching { PhoneNumberUtils.formatNumber(number, country) }.getOrNull()
        }
        // needs no permission: the sim's country code is freely readable.
        PhoneNumbers.region = runCatching {
            context.getSystemService(TelephonyManager::class.java)
                ?.simCountryIso
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
