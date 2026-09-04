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
 * Notruf einrichten. Die Nummern stehen in einer Zeile, weil das auf drei Zoll schneller
 * geht als eine Liste mit Plus-Knopf - und weil [SosNumbers] beim Einlesen streng aussortiert,
 * kostet die Bequemlichkeit nichts.
 */
@Composable
internal fun SosSettings(
    config: SosConfig,
    onChange: (SosConfig) -> Unit,
    /** Wird gerufen, wenn der Standort **eingeschaltet** wird und die Erlaubnis fehlt. */
    onNeedLocation: () -> Unit,
    locationGranted: Boolean,
    locationBlocked: Boolean,
    onAskLocation: () -> Unit,
    onLocationSettings: () -> Unit,
) {
    val palette = LocalBigPalette.current
    val context = LocalContext.current
    // Die Probe des Notruf-Alarms gehoert hierher, nicht in den Einstellungsbaum. Sie laeuft
    // nur, solange **diese Seite** offen ist - vorher hoerte sie erst auf, wenn man die
    // Einstellungen ganz verliess. Ein Alarm, der weiterlaeuft, waere schlimmer als keiner.
    var probe by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { SosAlarm.stop(context) } }
    val defaultMessage = stringResource(R.string.sos_message_default)
    var numbersText by remember(config.numbers) { mutableStateOf(SosNumbers.format(config.numbers)) }
    // Beim ersten Oeffnen steht der Vorgabetext schon im Feld. So sieht der Nutzer, was
    // verschickt wuerde, statt vor einem leeren Kasten zu raten.
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
                // Der Hinweis gehoert ans Feld, nicht an den Knopf: am Knopf stand er in
                // einer Zeile, die abgeschnitten wurde, und ein leerer Kasten sagt nichts.
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
            // Zwei Dinge, die am 04.09.2026 am Emulator gefehlt haben.
            //
            // Erstens: der Knopf sah immer gleich aus. Wer nichts geaendert hat, konnte ihn
            // druecken, und es geschah nichts - derselbe Fall wie beim Senden ohne Text und
            // beim Anrufen ohne Nummer. Ohne Aenderung ist er kein Knopf.
            //
            // Zweitens: nach dem Speichern sah der Bildschirm genauso aus wie davor. Bei
            // Nummern, die man einmal eintraegt und hoffentlich nie braucht, ist "hat es
            // geklappt?" die einzige Frage, die zaehlt.
            val geaendert = numbersText != SosNumbers.format(config.numbers)
            val zusammenhang = LocalContext.current
            BigRow(
                label = stringResource(R.string.sos_numbers_save),
                surface = if (geaendert) palette.surfaceAccent else palette.surfaceDefault,
                onClick = if (geaendert) {
                    {
                        // Sagen, was wirklich passiert ist. "Nummern gespeichert" nach einer
                        // Eingabe, von der nichts brauchbar war, waere ein wahrer Satz an der
                        // falschen Stelle - die rote Zeile darueber sagt ja schon, dass die
                        // Eingabe nicht taugt. Am 04.09.2026 am Emulator mit einem "x"
                        // ausprobiert und genau so gesehen.
                        val genommen = SosNumbers.parse(numbersText)
                        onChange(config.copy(numbers = genommen))
                        // Ueber AppLocale, nicht ueber den rohen Context: BigLau hat eine
                        // eigene Sprache, und die Meldung soll in ihr stehen.
                        val texte = AppLocale.forApp(zusammenhang)
                        Notice.show(
                            zusammenhang,
                            if (genommen.isEmpty()) {
                                texte.getString(R.string.sos_numbers_cleared)
                            } else {
                                texte.resources.getQuantityString(
                                    R.plurals.sos_numbers_saved_n,
                                    genommen.size,
                                    genommen.size,
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
            // Mit der laengsten Zeile gerechnet, die dazukommen kann: seit die Nachricht
            // das Alter eines alten Standorts nennt, waere die Zahl sonst im schlechten
            // Fall um eine SMS zu niedrig - und zu niedrig ist bei Kosten die falsche
            // Richtung.
            val laengstesAlter = pluralStringResource(R.plurals.sos_location_age_hours, 24, 24)
            val preview = SosMessage.compose(
                text = messageText,
                latitude = 48.20849,
                longitude = 16.37208,
                fallback = defaultMessage,
                ageNote = laengstesAlter,
            )
            // Dieselbe Frage wie bei den Nummern - und dieselbe Antwort. Verglichen wird
            // gegen das, was gespeichert **wuerde**: steht im Feld der Vorgabetext und in
            // der Einrichtung nichts, ist das kein Unterschied.
            val nachrichtGeaendert = messageText.trim() != config.message.ifBlank { defaultMessage }
            val zusammenhang = LocalContext.current
            BigRow(
                label = stringResource(R.string.sos_message_save),
                // Vorschau mit Beispielkoordinaten: der Nutzer soll sehen, was ankommt,
                // und wie viele SMS es kostet.
                secondary = pluralStringResource(
                    R.plurals.sos_message_parts,
                    SosMessage.partsNeeded(preview),
                    SosMessage.partsNeeded(preview),
                ),
                surface = if (nachrichtGeaendert) palette.surfaceAccent else palette.surfaceDefault,
                onClick = if (nachrichtGeaendert) {
                    {
                        onChange(config.copy(message = messageText.trim()))
                        Notice.show(zusammenhang, R.string.sos_message_saved)
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
                    val an = !config.sendLocation
                    onChange(config.copy(sendLocation = an))
                    if (an && !locationGranted) onNeedLocation()
                },
            )
        }

        // PLAN.md 4.8: lauter Alarmton und blinkendes Licht. Sie wirken ohne Netz und
        // erreichen den, der zwei Raeume weiter steht - die Nachricht erreicht den nicht.
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
        // Ausprobieren, bevor es zaehlt: wer den Alarm im Notfall zum ersten Mal hoert,
        // erschrickt und drueckt ihn weg. Der Knopf loest **keinen** Notruf aus, es geht
        // dabei keine Nachricht hinaus.
        if (config.alarmSound || config.alarmFlash) {
            item {
                BigRow(
                    label = stringResource(
                        if (probe) R.string.sos_alarm_stop else R.string.sos_alarm_try,
                    ),
                    secondary = stringResource(R.string.sos_alarm_try_hint),
                    icon = if (probe) Icons.Filled.StopCircle else Icons.Filled.PlayCircle,
                    surface = if (probe) palette.surfaceAccent else palette.surfaceDefault,
                    onClick = {
                        if (probe) SosAlarm.stop(context) else SosAlarm.start(context, config)
                        probe = !probe
                    },
                )
            }
        }

        // Den Ablauf einmal ansehen, ohne dass etwas hinausgeht. Wer den Notruf einrichtet,
        // will ihn dem Menschen erklaeren koennen, der ihn spaeter im Ernst drueckt - und
        // eine Erklaerung, die man zeigen kann, ist besser als eine, die man liest.
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

        // Der Schalter steht auf "mit Standort", das Recht fehlt: dann geht die Nachricht
        // ohne Koordinaten hinaus. Das gehoert hier hingeschrieben, nicht erst im Notfall
        // gemerkt.
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
                            // Die beiden Texte gehören zum Zugriffs-Baustein und liegen
                            // deshalb im Design-System; `nonTransitiveRClass` heisst,
                            // dass man sie dort auch ansprechen muss.
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
