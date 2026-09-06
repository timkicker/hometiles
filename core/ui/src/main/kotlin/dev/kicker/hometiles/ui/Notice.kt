package dev.kicker.hometiles.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import dev.kicker.hometiles.data.ConfigStore

/** how a notice appears. */
enum class NoticeStyle { TOAST, DIALOG }

/**
 * the one place where the app says something.
 *
 * a toast is gone after two seconds, which is never for someone still looking for their
 * glasses; a setting keeps notices standing until tapped away. `PLAN.md` 4.4.
 */
object Notice {

    /**
     * word for word in the manifest of `NoticeActivity`.
     *
     * an intent and not the class: naming it would pull half the app into the design system.
     */
    const val ACTION = "dev.kicker.hometiles.action.NOTICE"

    /** the text that should stand. */
    const val EXTRA_TEXT = "text"

    fun styleFor(confirm: Boolean): NoticeStyle =
        if (confirm) NoticeStyle.DIALOG else NoticeStyle.TOAST

    fun show(context: Context, textRes: Int) = show(context, context.getString(textRes))

    fun show(context: Context, text: String) {
        val confirm = ConfigStore.get(context).current.behaviour.confirmMessages
        when (styleFor(confirm)) {
            NoticeStyle.TOAST -> Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            NoticeStyle.DIALOG -> context.startActivity(
                Intent(ACTION)
                    .setPackage(context.packageName)
                    .putExtra(EXTRA_TEXT, text)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
