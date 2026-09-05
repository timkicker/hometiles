package org.biglau.sms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.biglau.R
import org.biglau.data.SmsConfig
import org.biglau.phone.CallBlocking
import org.biglau.ui.BigHeading
import org.biglau.ui.BigRow
import org.biglau.ui.theme.LocalBigPalette
import org.biglau.ui.bigSp

/** the vibration lengths in words. see [SmsNotifications.VIBRATION_CHOICES]. */
private fun vibrationLabel(millis: Int): Int = when (millis) {
    0 -> R.string.sms_vibration_off
    200 -> R.string.sms_vibration_short
    500 -> R.string.sms_vibration_medium
    else -> R.string.sms_vibration_long
}

/**
 * messages (`PLAN.md` 4.7).
 *
 * the filter hides, it does not block: a filtered message arrives and lies in the system
 * database, it simply is not in this list. the texts here say so, because a block one
 * believes tighter than it is, is more dangerous than one whose limit is known.
 *
 * not even the default app can refuse an sms; the network has long delivered it. that is
 * why `sms_filter_hint` and `sms_filter_hint_default` say the same with the right reason.
 */
@Composable
internal fun MessagesSettingsList(
    sms: SmsConfig,
    onChange: (SmsConfig) -> Unit,
    /**
     * comes from outside because the *system* grants the role: the activity knows via
     * `resumes` that it is back in front, a page on its own does not.
     */
    holdsSmsRole: Boolean,
) {
    val palette = LocalBigPalette.current
    val context = LocalContext.current
    var numbersText by remember(sms.hiddenNumbers) { mutableStateOf(CallBlocking.format(sms.hiddenNumbers)) }
    var wordsText by remember(sms.hiddenWords) { mutableStateOf(SmsFilter.formatWords(sms.hiddenWords)) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_messages)) }
        // what BigLau cannot do: an mms is not in the notice, it is fetched from the mmsc
        // over the data connection, and BigLau has no INTERNET permission. the no network
        // access line in the README is what carries the price here.
        //
        // two versions, because only the default app fetches an mms: once that is BigLau, a
        // consolation pointing at the phone's messaging app becomes false.
        item {
            Text(
                text = stringResource(
                    if (holdsSmsRole) R.string.sms_no_mms_default else R.string.sms_no_mms,
                ),
                color = palette.onBackground,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        // `PLAN.md` 4.7: the conversation font size is deliberately apart from the global
        // one. a message is read in one go and out of the hand.
        item { BigHeading(stringResource(R.string.sms_scale)) }
        items(ConversationText.CHOICES) { wert ->
            BigRow(
                label = "${(wert * 100).toInt()} %",
                selected = wert == sms.conversationScale,
                onClick = { onChange(sms.copy(conversationScale = wert)) },
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (sms.fullScreenAlert) R.string.sms_fullscreen_on else R.string.sms_fullscreen_off,
                ),
                secondary = stringResource(R.string.sms_fullscreen_hint),
                checked = sms.fullScreenAlert,
                onClick = { onChange(sms.copy(fullScreenAlert = !sms.fullScreenAlert)) },
            )
        }
        // `PLAN.md` 4.7: the repeated reminder. a notice that comes once is missed by
        // anyone with the phone in a pocket.
        item { BigHeading(stringResource(R.string.sms_repeat)) }
        items(SmsReminder.CHOICES) { minuten ->
            BigRow(
                label = if (minuten == 0) {
                    stringResource(R.string.sms_repeat_off)
                } else {
                    pluralStringResource(R.plurals.sms_repeat_minutes, minuten, minuten)
                },
                selected = minuten == sms.repeatMinutes,
                onClick = {
                    onChange(sms.copy(repeatMinutes = minuten))
                    // off means off now, not at the next message: an old alarm would keep
                    // reminding.
                    MessageReminderReceiver.schedule(context, minuten)
                },
            )
        }
        // `PLAN.md` 4.7: vibration length. it sits in the notification channel so the
        // notice keeps honouring do-not-disturb. see SmsNotifications.
        item { BigHeading(stringResource(R.string.sms_vibration)) }
        items(SmsNotifications.VIBRATION_CHOICES) { dauer ->
            BigRow(
                // short, medium, long instead of milliseconds: a number in ms tells nobody
                // how it feels.
                label = stringResource(vibrationLabel(dauer)),
                selected = dauer == sms.vibrationMs,
                onClick = { onChange(sms.copy(vibrationMs = dauer)) },
            )
        }
        // `PLAN.md` 4.7: send button, its place, size and confirmation.
        item { BigHeading(stringResource(R.string.sms_send_heading)) }
        item {
            BigRow(
                label = stringResource(
                    if (sms.confirmBeforeSending) R.string.sms_confirm_on else R.string.sms_confirm_off,
                ),
                secondary = stringResource(R.string.sms_confirm_hint),
                surface = if (sms.confirmBeforeSending) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onChange(sms.copy(confirmBeforeSending = !sms.confirmBeforeSending)) },
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (sms.sendButtonAbove) R.string.sms_send_above else R.string.sms_send_below,
                ),
                surface = if (sms.sendButtonAbove) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onChange(sms.copy(sendButtonAbove = !sms.sendButtonAbove)) },
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (sms.sendButtonLarge) R.string.sms_send_large else R.string.sms_send_normal,
                ),
                surface = if (sms.sendButtonLarge) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onChange(sms.copy(sendButtonLarge = !sms.sendButtonLarge)) },
            )
        }
        item { BigHeading(stringResource(R.string.sms_filter_numbers_heading)) }
        item {
            Text(
                // the sentence claims something about this phone and has to look: with the
                // sms role held, BigLau is not the sms app is plainly wrong.
                text = if (holdsSmsRole) {
                    stringResource(R.string.sms_filter_hint_default)
                } else {
                    stringResource(R.string.sms_filter_hint)
                },
                color = palette.onBackground,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        item { BigHeading(stringResource(R.string.sms_filter_numbers)) }
        item {
            OutlinedTextField(
                value = numbersText,
                onValueChange = { numbersText = it },
                placeholder = { Text(stringResource(R.string.blocked_numbers_placeholder), fontSize = bigSp(15f)) },
                textStyle = LocalTextStyle.current.copy(fontSize = bigSp(17f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.blocked_numbers_save),
                surface = palette.surfaceAccent,
                onClick = { onChange(sms.copy(hiddenNumbers = CallBlocking.parse(numbersText))) },
            )
        }
        item { BigHeading(stringResource(R.string.sms_filter_words)) }
        item {
            OutlinedTextField(
                value = wordsText,
                onValueChange = { wordsText = it },
                placeholder = { Text(stringResource(R.string.sms_filter_words_placeholder), fontSize = bigSp(15f)) },
                textStyle = LocalTextStyle.current.copy(fontSize = bigSp(17f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.blocked_numbers_save),
                surface = palette.surfaceAccent,
                onClick = { onChange(sms.copy(hiddenWords = SmsFilter.parseWords(wordsText))) },
            )
        }
    }
}
