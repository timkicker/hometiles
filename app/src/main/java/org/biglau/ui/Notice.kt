package org.biglau.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.biglau.data.ConfigStore

/** Wie eine Meldung erscheint. */
enum class NoticeStyle { TOAST, DIALOG }

/**
 * Die eine Stelle, an der die App etwas mitteilt.
 *
 * Eine kurze Einblendung ist nach zwei Sekunden weg. Wer langsam liest, wer erst die
 * Brille sucht, wer das Telefon gerade zum Ohr genommen hat, liest sie nie - und weiss
 * dann nur, dass etwas aufgeblitzt ist. Deshalb kann man in den Einstellungen sagen:
 * Meldungen bleiben stehen, bis ich sie wegtippe. PLAN.md 4.4.
 *
 * Vorgabe bleibt die Einblendung. Eine Meldung, die jedes Mal einen Knopf verlangt, ist
 * fuer die meisten eine Zumutung; die Wahl gehoert dem, der sie braucht.
 */
object Notice {

    fun styleFor(confirm: Boolean): NoticeStyle =
        if (confirm) NoticeStyle.DIALOG else NoticeStyle.TOAST

    fun show(context: Context, textRes: Int) = show(context, context.getString(textRes))

    fun show(context: Context, text: String) {
        val confirm = ConfigStore.get(context).current.behaviour.confirmMessages
        when (styleFor(confirm)) {
            NoticeStyle.TOAST -> Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            NoticeStyle.DIALOG -> context.startActivity(
                Intent(context, NoticeActivity::class.java)
                    .putExtra(NoticeActivity.EXTRA_TEXT, text)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
