package dev.kicker.hometiles.ui

import android.text.format.DateFormat
import java.util.Locale

/**
 * turns date parts into the pattern of the given language.
 *
 * `getBestDateTimePattern` knows where the month goes and whether the day carries a dot; a
 * hard-written german pattern put "Wednesday, 2. September" on an english phone.
 */
fun bestDatePattern(skeleton: String?, locale: Locale): String? =
    skeleton?.let { DateFormat.getBestDateTimePattern(locale, it) }
