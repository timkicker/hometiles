package dev.kicker.hometiles.phone

import android.content.Context
import android.telephony.PhoneNumberFormattingTextWatcher
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.text.SpannableStringBuilder
import android.text.Spanned
import java.util.Locale

/**
 * the system half of [PhoneNumbers], the only place that touches android for it.
 *
 * [PhoneNumbers] only computes and therefore lives in `core:model`, where the compiler
 * enforces "no android". two facts cannot be fetched there: how a number is written (android
 * carries the dialling-code tables) and the sim's country. [install] hooks both up at start.
 *
 * without that call hometiles writes numbers in plain blocks of three. that is no crash but a
 * quiet regression, so `StartupTasksTest` checks the call is made.
 */
object SystemNumbers {

    fun install(context: Context) {
        PhoneNumbers.systemFormat = { number, country ->
            runCatching { PhoneNumberUtils.formatNumber(number, country) }.getOrNull()
        }
        // the public way to the system's as-you-type formatter is a text watcher, so it gets
        // a text to watch. appending the whole number lets it format it as typed.
        PhoneNumbers.systemFormatTyping = { number, country ->
            runCatching {
                val text = SpannableStringBuilder()
                text.setSpan(
                    PhoneNumberFormattingTextWatcher(country ?: Locale.getDefault().country),
                    0,
                    0,
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE,
                )
                text.append(number)
                text.toString()
            }.getOrNull()
        }
        // needs no permission: the sim's country code is freely readable. upper case, since
        // the dialling-code tables only know "US": with "us" a national number found no
        // spelling at all (emulator, sim country us, 25.10.2026, issue #2).
        PhoneNumbers.region = runCatching {
            context.getSystemService(TelephonyManager::class.java)
                ?.simCountryIso
                ?.takeIf { it.isNotBlank() }
                ?.uppercase(Locale.ROOT)
        }.getOrNull()
    }
}
