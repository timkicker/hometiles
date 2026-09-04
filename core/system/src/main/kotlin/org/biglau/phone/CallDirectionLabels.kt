package org.biglau.phone

import androidx.annotation.StringRes
import org.biglau.core.system.R

/**
 * Das Wort zur Richtung eines Anrufs.
 *
 * Stand als private Funktion im Einstellungsbaum, wo man die Arten ein- und ausschaltet.
 * Die Anrufliste selbst brauchte dieselben Woerter: dort zeigt ein Pfeil die Richtung, und
 * ein Pfeil hat keinen Namen zum Vorlesen. Zweimal dieselbe Liste zu fuehren waere die
 * sichere Art, sie auseinanderlaufen zu lassen.
 *
 * Warum hier und nicht in `:app`: von `settings` nach `phone` waere eine fuenfte Kante
 * zwischen den kuenftigen Modulen geworden - fuer eine Wortliste, also fuer nichts. Sie
 * liegt bei [CallDirection], zu der sie gehoert, und `:app` bekommt keine neue Kante.
 */
@StringRes
fun callDirectionLabel(direction: CallDirection): Int = when (direction) {
    CallDirection.INCOMING -> R.string.call_type_incoming
    CallDirection.OUTGOING -> R.string.call_type_outgoing
    CallDirection.MISSED -> R.string.call_type_missed
    CallDirection.REJECTED -> R.string.call_type_rejected
    CallDirection.BLOCKED -> R.string.call_type_blocked
    CallDirection.OTHER -> R.string.call_type_other
}

/**
 * Dasselbe fuer eine einzelne Zeile - "verpasst" statt "Verpasste Anrufe".
 *
 * Die Ueberschriften der Einstellungen sind Kategorienamen im Plural; an eine Zeile gehaengt
 * lasen sie sich falsch ("Mark Helsi, angenommene Anrufe"), und "Alles andere" sagte an
 * einer einzelnen Zeile gar nichts. Zwei Saetze fuer zwei Zwecke, aber in einer Datei und
 * an derselben Aufzaehlung - auseinanderlaufen koennen sie so nicht.
 */
@StringRes
fun callDirectionSpeech(direction: CallDirection): Int = when (direction) {
    CallDirection.INCOMING -> R.string.call_dir_incoming
    CallDirection.OUTGOING -> R.string.call_dir_outgoing
    CallDirection.MISSED -> R.string.call_dir_missed
    CallDirection.REJECTED -> R.string.call_dir_rejected
    CallDirection.BLOCKED -> R.string.call_dir_blocked
    CallDirection.OTHER -> R.string.call_dir_other
}
