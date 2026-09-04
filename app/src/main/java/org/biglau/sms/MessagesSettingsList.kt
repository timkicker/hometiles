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

/** Die Vibrationsdauern in Worten. Siehe [SmsNotifications.VIBRATION_CHOICES]. */
private fun vibrationLabel(dauer: Int): Int = when (dauer) {
    0 -> R.string.sms_vibration_off
    200 -> R.string.sms_vibration_short
    500 -> R.string.sms_vibration_medium
    else -> R.string.sms_vibration_long
}

/**
 * Nachrichten (`PLAN.md` 4.7).
 *
 * Der Filter **verbirgt**, er sperrt nicht. Eine gefilterte Nachricht kommt an und liegt in
 * der Datenbank des Systems — sie steht nur nicht in dieser Liste. Genau das sagen die Texte
 * hier auch; eine Sperre, die man für dichter hält, als sie ist, ist gefährlicher als eine,
 * deren Grenze man kennt.
 *
 * Bis zum 04.09.2026 stand hier als Begründung „BigLau hält die SMS-Rolle nicht". Seit
 * BigLau sie hält, stimmt das nicht mehr — an der Sache ändert es nichts: abweisen kann
 * auch die Standard-App eine SMS nicht, das Netz hat sie längst zugestellt. Die beiden
 * Texte `sms_filter_hint` und `sms_filter_hint_default` sagen deshalb je nach Rolle
 * dasselbe mit dem richtigen Grund.
 */
@Composable
internal fun MessagesSettingsList(
    sms: SmsConfig,
    onChange: (SmsConfig) -> Unit,
    /**
     * Haelt BigLau die SMS-Rolle?
     *
     * Kommt von aussen, weil die Rolle das **System** vergibt: wer sie erteilt und
     * zurueckkommt, soll nicht denselben Satz noch einmal lesen. Die Activity weiss ueber
     * `fortsetzungen`, dass sie wieder vorn ist - eine Seite fuer sich weiss das nicht.
     */
    istStandardApp: Boolean,
) {
    val palette = LocalBigPalette.current
    val context = LocalContext.current
    var nummernText by remember(sms.hiddenNumbers) { mutableStateOf(CallBlocking.format(sms.hiddenNumbers)) }
    var woerterText by remember(sms.hiddenWords) { mutableStateOf(SmsFilter.formatWords(sms.hiddenWords)) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_messages)) }
        // Was BigLau **nicht** kann: eine MMS steht nicht in der Meldung, sie wird ueber
        // die Datenverbindung vom MMSC geholt. BigLau hat keine INTERNET-Berechtigung -
        // der Satz im README ("ohne Netzwerkzugriff") ist genau das, was hier den Preis
        // hat. Bis zum 03.09.2026 stand das nirgends; wer die Rolle vergibt, haette es
        // erst gemerkt, wenn ein Bild nicht ankommt.
        //
        // **Zwei Fassungen seit dem 04.09.2026.** Der erste Satz war als Warnung *vor* der
        // Rollenvergabe geschrieben und endete mit "die Nachrichten-App des Telefons kann
        // sie weiterhin oeffnen". Danach stimmt das nicht mehr: eine MMS holt nur die
        // Standard-App, und das ist dann BigLau. Ein Trost, der nach der Entscheidung
        // falsch wird, ist schlimmer als keiner.
        item {
            Text(
                text = stringResource(
                    if (istStandardApp) R.string.sms_no_mms_default else R.string.sms_no_mms,
                ),
                color = palette.onBackground,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        // PLAN.md 4.7: die Schriftgroesse im Gespraech ist ausdruecklich getrennt von der
        // globalen. Eine Nachricht liest man am Stueck und aus der Hand.
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
        // PLAN.md 4.7: wiederholte Erinnerung. Eine Meldung, die einmal kommt, verpasst
        // man - wer das Telefon in der Tasche hat, sieht sie sonst erst am Abend.
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
                    // Aus heisst sofort aus, nicht erst bei der naechsten Nachricht: sonst
                    // erinnert ein alter Wecker weiter. Der Wecker gehoert zu dieser Seite,
                    // nicht zum Einstellungsbaum - deshalb steht er jetzt hier.
                    MessageReminderReceiver.schedule(context, minuten)
                },
            )
        }
        // PLAN.md 4.7: Vibrationsdauer. Sie steht im Benachrichtigungskanal, damit die
        // Meldung sich weiter an "Bitte nicht stoeren" haelt - siehe SmsNotifications.
        item { BigHeading(stringResource(R.string.sms_vibration)) }
        items(SmsNotifications.VIBRATION_CHOICES) { dauer ->
            BigRow(
                // Kurz, mittel, lang statt Millisekunden: eine Zahl in ms sagt niemandem,
                // wie sich das anfuehlt, und diese App richtet sich nicht an Techniker.
                label = stringResource(vibrationLabel(dauer)),
                selected = dauer == sms.vibrationMs,
                onClick = { onChange(sms.copy(vibrationMs = dauer)) },
            )
        }
        // PLAN.md 4.7: Sendeknopf - Position, Groesse, Bestaetigung vor dem Senden.
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
                // Der Satz behauptet etwas ueber dieses Telefon und muss deshalb nachsehen:
                // haelt BigLau die SMS-Rolle, ist "BigLau ist nicht die SMS-App dieses
                // Telefons" schlicht falsch. Am Emulator aufgefallen, wo es die Rolle hat.
                text = if (istStandardApp) {
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
                value = nummernText,
                onValueChange = { nummernText = it },
                placeholder = { Text(stringResource(R.string.blocked_numbers_placeholder), fontSize = bigSp(15f)) },
                textStyle = LocalTextStyle.current.copy(fontSize = bigSp(17f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.blocked_numbers_save),
                surface = palette.surfaceAccent,
                onClick = { onChange(sms.copy(hiddenNumbers = CallBlocking.parse(nummernText))) },
            )
        }
        item { BigHeading(stringResource(R.string.sms_filter_words)) }
        item {
            OutlinedTextField(
                value = woerterText,
                onValueChange = { woerterText = it },
                placeholder = { Text(stringResource(R.string.sms_filter_words_placeholder), fontSize = bigSp(15f)) },
                textStyle = LocalTextStyle.current.copy(fontSize = bigSp(17f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.blocked_numbers_save),
                surface = palette.surfaceAccent,
                onClick = { onChange(sms.copy(hiddenWords = SmsFilter.parseWords(woerterText))) },
            )
        }
    }
}
