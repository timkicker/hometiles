package org.biglau.ui

import android.text.format.DateFormat
import java.util.Locale

/**
 * Aus den Bestandteilen eines Datums das Muster der jeweiligen Sprache machen.
 *
 * `getBestDateTimePattern` weiss, dass „2. September" auf Deutsch mit Punkt und auf Englisch
 * ohne geschrieben wird und dass der Monat dort vor den Tag gehoert. Vorher stand in der App
 * ein fest geschriebenes deutsches Muster; am englisch eingestellten Jelly 2 stand deshalb
 * „Wednesday, 2. September" auf der Uhr-Kachel.
 */
fun bestDatePattern(skeleton: String?, locale: Locale): String? =
    skeleton?.let { DateFormat.getBestDateTimePattern(locale, it) }
