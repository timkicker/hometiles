package org.biglau.toggles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Intent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import org.biglau.R
import org.biglau.core.ui.R as UiR
import org.biglau.actions.SosMessage
import org.biglau.data.SosConfig
import androidx.compose.foundation.lazy.items
import org.biglau.ui.BigHeading
import org.biglau.ui.BigRow
import org.biglau.ui.AppLocale
import org.biglau.ui.Notice
import org.biglau.ui.bigSp
import org.biglau.ui.theme.LocalBigPalette

/**
 * setting up the emergency call. the numbers stand in one field because that is faster on
 * three inches than a list with a plus button, and [SosNumbers] sorts strictly on reading.
 */
@Composable
internal fun SosSettings(
    config: SosConfig,
    onChange: (SosConfig) -> Unit,
    /** called when the location is switched *on* and the permission is missing. */
    onNeedLocation: () -> Unit,
    locationGranted: Boolean,
    locationBlocked: Boolean,
    onAskLocation: () -> Unit,
    onLocationSettings: () -> Unit,
) {
    val palette = LocalBigPalette.current
    val context = LocalContext.current
    // trying out the alarm belongs here and not in the settings tree: it runs only while
    // *this page* is open. an alarm that keeps running would be worse than none.
    var trying by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { SosAlarm.stop(context) } }
    val defaultMessage = stringResource(R.string.sos_message_default)
    var numbersText by remember(config.numbers) { mutableStateOf(SosNumbers.format(config.numbers)) }
    // the default text stands in the field from the start: one sees what would be sent
    // instead of guessing in front of an empty box.
    var messageText by remember(config.message) {
        mutableStateOf(config.message.ifBlank { defaultMessage })
    }
    val rejected = remember(numbersText) { SosNumbers.rejected(numbersText) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { BigHeading(stringResource(R.string.sos)) }
        item {
            Text(
                text = stringResource(R.string.sos_explainer),
                color = palette.onBackground,
                fontSize = bigSp(16f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }

        item { BigHeading(stringResource(R.string.sos_numbers)) }
        item {
            OutlinedTextField(
                value = numbersText,
                onValueChange = { numbersText = it },
                singleLine = false,
                textStyle = TextStyle(fontSize = bigSp(20f), fontWeight = FontWeight.Bold),
                // the hint belongs on the field, not the button, where it sat in a line
                // that was cut off.
                placeholder = { Text(stringResource(R.string.sos_numbers_placeholder), fontSize = bigSp(17f)) },
                supportingText = {
                    Text(stringResource(R.string.sos_numbers_hint, SosNumbers.MAX), fontSize = bigSp(15f))
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (rejected.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.sos_numbers_rejected, rejected.joinToString(", ")),
                    color = palette.dangerText,
                    fontSize = bigSp(15f),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        item {
            // without a change this is not a button, the same as sending without text and
            // calling without a number. and after saving the screen has to look different:
            // for numbers one enters once and hopefully never needs, did it work is the only
            // question that counts.
            val changed = numbersText != SosNumbers.format(config.numbers)
            val here = LocalContext.current
            BigRow(
                label = stringResource(R.string.sos_numbers_save),
                surface = if (changed) palette.surfaceAccent else palette.surfaceDefault,
                onClick = if (changed) {
                    {
                        // say what really happened: numbers saved after an entry with nothing
                        // usable in it would be a true sentence in the wrong place.
                        val taken = SosNumbers.parse(numbersText)
                        onChange(config.copy(numbers = taken))
                        // through AppLocale, not the raw context: the notice belongs in the
                        // app's language.
                        val texts = AppLocale.forApp(here)
                        Notice.show(
                            here,
                            if (taken.isEmpty()) {
                                texts.getString(R.string.sos_numbers_cleared)
                            } else {
                                texts.resources.getQuantityString(
                                    R.plurals.sos_numbers_saved_n,
                                    taken.size,
                                    taken.size,
                                )
                            },
                        )
                    }
                } else {
                    null
                },
            )
        }

        item { BigHeading(stringResource(R.string.sos_message)) }
        item {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                textStyle = TextStyle(fontSize = bigSp(18f)),
                placeholder = { Text(defaultMessage, fontSize = bigSp(17f)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            // computed with the longest line that can be added: too low is the wrong
            // direction when it costs money.
            val longestAge = pluralStringResource(R.plurals.sos_location_age_hours, 24, 24)
            val preview = SosMessage.compose(
                text = messageText,
                latitude = 48.20849,
                longitude = 16.37208,
                fallback = defaultMessage,
                ageNote = longestAge,
            )
            // the same question as for the numbers, compared against what *would* be saved:
            // the default text in the field against nothing stored is no difference.
            val messageChanged = messageText.trim() != config.message.ifBlank { defaultMessage }
            val here = LocalContext.current
            BigRow(
                label = stringResource(R.string.sos_message_save),
                // preview with sample coordinates: one should see what arrives and how many
                // messages it costs.
                secondary = pluralStringResource(
                    R.plurals.sos_message_parts,
                    SosMessage.partsNeeded(preview),
                    SosMessage.partsNeeded(preview),
                ),
                surface = if (messageChanged) palette.surfaceAccent else palette.surfaceDefault,
                onClick = if (messageChanged) {
                    {
                        onChange(config.copy(message = messageText.trim()))
                        Notice.show(here, R.string.sos_message_saved)
                    }
                } else {
                    null
                },
            )
        }

        item { BigHeading(stringResource(R.string.sos_countdown)) }
        items(listOf(0, 3, 5, 8, 10)) { seconds ->
            BigRow(
                label = if (seconds == 0) {
                    stringResource(R.string.sos_countdown_none)
                } else {
                    pluralStringResource(R.plurals.sos_countdown_seconds, seconds, seconds)
                },
                selected = seconds == config.countdownSeconds,
                onClick = { onChange(config.copy(countdownSeconds = SosCountdown.clamp(seconds))) },
            )
        }

        item {
            BigRow(
                label = stringResource(
                    if (config.sendLocation) R.string.sos_location_on else R.string.sos_location_off,
                ),
                surface = if (config.sendLocation) palette.surfaceAccent else palette.surfaceDefault,
                onClick = {
                    val on = !config.sendLocation
                    onChange(config.copy(sendLocation = on))
                    if (on && !locationGranted) onNeedLocation()
                },
            )
        }

        // `PLAN.md` 4.8: loud alarm and blinking light. they work without a network and
        // reach the person two rooms away, whom the message does not.
        item { BigHeading(stringResource(R.string.sos_alarm_heading)) }
        item {
            BigRow(
                label = stringResource(
                    if (config.alarmSound) R.string.sos_alarm_sound_on else R.string.sos_alarm_sound_off,
                ),
                secondary = stringResource(R.string.sos_alarm_sound_hint),
                checked = config.alarmSound,
                onClick = { onChange(config.copy(alarmSound = !config.alarmSound)) },
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (config.alarmFlash) R.string.sos_alarm_flash_on else R.string.sos_alarm_flash_off,
                ),
                checked = config.alarmFlash,
                onClick = { onChange(config.copy(alarmFlash = !config.alarmFlash)) },
            )
        }
        // try it before it counts: hearing the alarm for the first time in an emergency
        // startles and gets it pushed away. this button raises no emergency call.
        if (config.alarmSound || config.alarmFlash) {
            item {
                BigRow(
                    label = stringResource(
                        if (trying) R.string.sos_alarm_stop else R.string.sos_alarm_try,
                    ),
                    secondary = stringResource(R.string.sos_alarm_try_hint),
                    icon = if (trying) Icons.Filled.StopCircle else Icons.Filled.PlayCircle,
                    surface = if (trying) palette.surfaceAccent else palette.surfaceDefault,
                    onClick = {
                        if (trying) SosAlarm.stop(context) else SosAlarm.start(context, config)
                        trying = !trying
                    },
                )
            }
        }

        // see the sequence once without anything going out: an explanation one can show
        // beats one that has to be read.
        item {
            BigRow(
                label = stringResource(R.string.sos_preview),
                secondary = stringResource(R.string.sos_preview_hint),
                icon = Icons.Filled.PlayCircle,
                onClick = {
                    context.startActivity(
                        Intent(context, SosActivity::class.java)
                            .putExtra(SosActivity.EXTRA_PREVIEW, true),
                    )
                },
            )
        }

        // the switch says with location and the right is missing: then the message goes
        // out without coordinates, and that belongs here, not noticed in an emergency.
        if (config.sendLocation && !locationGranted) {
            item {
                Text(
                    text = stringResource(R.string.sos_location_missing),
                    color = palette.dangerText,
                    fontSize = bigSp(15f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            item {
                BigRow(
                    label = stringResource(
                        if (locationBlocked) {
                            // both texts belong to the permission block and live in the
                            // design system; `nonTransitiveRClass` means addressing them there.
                            UiR.string.permission_open_settings
                        } else {
                            UiR.string.permission_allow
                        },
                    ),
                    surface = palette.surfaceAccent,
                    onClick = if (locationBlocked) onLocationSettings else onAskLocation,
                )
            }
        }
    }
}
