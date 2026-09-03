package org.biglau.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import coil.compose.AsyncImage
import org.biglau.ui.cappedTextScale
import org.biglau.ui.theme.LocalCornerRadius
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.biglau.ui.BigLauActivity
import org.biglau.ui.Notice
import org.biglau.R
import org.biglau.data.AudioRoute
import org.biglau.data.ConfigStore
import org.biglau.ui.BigKeypad
import org.biglau.ui.BigHeading
import org.biglau.ui.BigRow
import org.biglau.ui.ContactAvatar
import org.biglau.ui.tabellenZiffern
import org.biglau.ui.dpSp
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

/**
 * Die Gespraechsansicht.
 *
 * Vollbild, ein Name so gross wie moeglich, und darunter Knoepfe, die eine ganze Zeile
 * einnehmen. Welche Knoepfe erscheinen, entscheidet [CallActions] - in dieser Datei steht
 * dazu keine einzige Bedingung, damit es geprueft bleibt.
 *
 * Die Zurueck-Geste tut hier nichts: waehrend eines Anrufs versehentlich wegzuwischen und
 * dann das Auflegen nicht mehr zu finden, waere die schlimmste Art, diese App zu verlieren.
 */
class InCallActivity : BigLauActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            val view by InCallRepository.call.collectAsStateWithLifecycle()
            var showKeypad by remember { mutableStateOf(false) }
            // Nur mit Bluetooth erreichbar: sonst schaltet die Zeile direkt um.
            var audioChoice by remember { mutableStateOf(false) }
            var now by remember { mutableStateOf(System.currentTimeMillis()) }

            LaunchedEffect(Unit) {
                while (true) {
                    now = System.currentTimeMillis()
                    delay(500)
                }
            }

            LaunchedEffect(view?.status) {
                if (view == null || view?.status == CallStatus.DISCONNECTED) {
                    delay(600)
                    finish()
                }
            }

            BackHandler(enabled = true) { /* absichtlich nichts */ }

            BigLauTheme(
                config.appearance.theme,
                // Gedeckelt: dieser Bildschirm scrollt nicht, und die Knopfhoehe steht
                // fest. Bei 200 % stand auf dem Knopf "Lautsprec…" - am Emulator gesehen.
                // Die Ziffern der Tastatur und der Name des Anrufers sind ohnehin aus der
                // Flaeche gerechnet und bleiben so gross, wie der Platz es zulaesst.
                cappedTextScale(config.appearance.textScale, INCALL_MAX_TEXT_SCALE),
                haptics = config.behaviour.haptics,
                font = config.appearance.font,
                labelScale = config.appearance.labelScale,
                iconPercent = config.appearance.iconPercent,
                icons = config.appearance.icons,
                hideCutLabels = config.appearance.hideCutLabels,
                cornerRadiusDp = config.appearance.cornerRadiusDp,
            ) {
                val palette = LocalBigPalette.current
                val current = view
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(palette.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    if (current == null) {
                        Text(
                            text = stringResource(R.string.incall_ended),
                            color = palette.onBackground,
                            fontSize = dpSp(22f),
                            modifier = Modifier.align(Alignment.Center),
                        )
                        return@Box
                    }

                    if (audioChoice) {
                        AudioChoice(
                            current = current.audioRoute,
                            onPick = {
                                if (InCallRepository.setRoute(it).isFailure) {
                                    Notice.show(this@InCallActivity, R.string.call_action_failed)
                                }
                                audioChoice = false
                            },
                            onClose = { audioChoice = false },
                        )
                        return@BigLauTheme
                    }

                    // Bei geoeffneter Tastatur bleiben nur zwei Zeilen stehen, sonst
                    // bleibt fuer die Ziffern nichts uebrig. Siehe CallActions.whileKeypad.
                    val zeilen =
                        if (showKeypad) CallActions.whileKeypad(current)
                        else CallActions.availableFor(current)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Das Foto steht ueber dem Namen und nimmt nur, was uebrig
                            // bleibt: CallerPhotoSize haelt fest, was Name, Zustand und
                            // Knoepfe brauchen. Ein Foto, das den Annehmen-Knopf aus dem
                            // Bild schiebt, waere auf genau diesem Bildschirm der
                            // schlimmste Fehler.
                            // Bei geoeffneter Tastatur gar kein Foto: den Platz braucht
                            // die Tastatur, und ein Bild daneben hilft niemandem beim Tippen.
                            val fotoHoehe =
                                if (showKeypad) 0f
                                else CallerPhotoSize.heightDp(
                                    size = config.phone.callerPhoto,
                                    availableDp = LocalConfiguration.current.screenHeightDp
                                        .toFloat(),
                                    buttons = zeilen.size,
                                    notice = current.otherName != null &&
                                        current.status == CallStatus.RINGING,
                                )
                            val foto = current.photoUri
                            val name = current.name?.takeIf { it.isNotBlank() }
                            when (CallerPhotoSize.imageFor(fotoHoehe, foto, name)) {
                                CallerPhotoSize.Image.NONE -> Unit
                                CallerPhotoSize.Image.PHOTO -> AsyncImage(
                                    model = foto,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .height(fotoHoehe.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(LocalCornerRadius.current)),
                                )
                                // Kein Foto, aber ein Name: die Initialen, wie ueberall sonst
                                // in der App. Begruendet in CallerPhotoSize.imageFor.
                                CallerPhotoSize.Image.INITIALS -> ContactAvatar(
                                    name = name.orEmpty(),
                                    photoUri = null,
                                    size = fotoHoehe.dp,
                                )
                            }
                            Text(
                                text = CallActions.headline(current, stringResource(R.string.call_unknown)),
                                color = palette.onBackground,
                                fontSize = dpSp(34f),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                            Text(
                                text = CallActions.durationSeconds(current, now)
                                    ?.let(CallActions::formatDuration)
                                    ?: stringResource(statusLabel(current.status)),
                                color = palette.onBackground,
                                fontSize = dpSp(20f),
                                // Die Dauer laeuft im Sekundentakt - ohne Tabellenziffern
                                // wackelt sie bei jeder Sekunde. PLAN.md 3.7.
                                style = tabellenZiffern(),
                            )
                            // Waehrend eines Gespraechs klingelt es: ohne diese Zeile
                            // stuende hier nur der neue Anrufer, und dass nebenan noch
                            // jemand in der Leitung ist, waere nirgends zu sehen.
                            val anderer = current.otherName
                            if (anderer != null && current.status == CallStatus.RINGING) {
                                Text(
                                    text = stringResource(R.string.incall_other_active, anderer),
                                    color = palette.onBackground,
                                    fontSize = dpSp(18f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                )
                            }
                        }

                        if (showKeypad) {
                            Box(Modifier.weight(1f)) {
                                BigKeypad(
                                    onDigit = { InCallRepository.playDigit(it) },
                                    onBackspace = { showKeypad = false },
                                )
                            }
                        } else {
                            // Leerraum ueber den Knoepfen: er verhindert, dass das Ohr
                            // waehrend des Gespraechs etwas ausloest.
                            Box(Modifier.weight(1f))
                        }

                        zeilen.forEach { action ->
                            ActionRow(
                                action = action,
                                keypadOpen = showKeypad,
                                route = current.audioRoute,
                            ) {
                                perform(
                                    action = action,
                                    toggleKeypad = { showKeypad = !showKeypad },
                                    openAudio = { audioChoice = true },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun perform(
        action: CallAction,
        toggleKeypad: () -> Unit,
        openAudio: () -> Unit = {},
    ) {
        // Jede dieser Anweisungen kann fehlschlagen - der Anruf ist inzwischen weg, das
        // Telefonsystem hat ihn uns entzogen. `InCallRepository` faengt das ab und gibt ein
        // `Result` zurueck; wer es wegwirft, laesst einen toten Knopf stehen. Und zwar den
        // Knopf „Annehmen", waehrend es klingelt.
        val ausgang: Result<*>? = when (action) {
            CallAction.ANSWER -> InCallRepository.answer()
            CallAction.REJECT -> InCallRepository.reject()
            CallAction.HANG_UP -> InCallRepository.hangUp()
            CallAction.HOLD -> InCallRepository.hold()
            CallAction.UNHOLD -> InCallRepository.unhold()
            CallAction.MUTE -> InCallRepository.setMuted(true)
            CallAction.UNMUTE -> InCallRepository.setMuted(false)
            CallAction.SPEAKER -> InCallRepository.setSpeaker(true)
            CallAction.SPEAKER_OFF -> InCallRepository.setSpeaker(false)
            CallAction.SWITCH -> InCallRepository.switchCall()
            CallAction.AUDIO -> { openAudio(); null }
            CallAction.KEYPAD -> { toggleKeypad(); null }
        }
        if (ausgang?.isFailure == true) Notice.show(this, R.string.call_action_failed)
    }
}

@Composable
private fun ActionRow(
    action: CallAction,
    keypadOpen: Boolean = false,
    route: AudioRoute = AudioRoute.EARPIECE,
    onClick: () -> Unit,
) {
    val palette = LocalBigPalette.current
    val surface = when (action) {
        CallAction.ANSWER -> palette.surfaceTile(1)
        CallAction.REJECT, CallAction.HANG_UP -> palette.surfaceDanger
        else -> palette.surfaceDefault
    }
    BigRow(
        label = stringResource(
            when {
                action == CallAction.KEYPAD && keypadOpen -> R.string.incall_keypad_hide
                // Die Tonzeile sagt, wohin der Ton gerade geht - sonst muesste man
                // sie antippen, um es zu erfahren.
                action == CallAction.AUDIO -> audioLabel(route)
                else -> actionLabel(action)
            },
        ),
        icon = when (action) {
            CallAction.ANSWER -> Icons.Filled.Call
            CallAction.REJECT, CallAction.HANG_UP -> Icons.Filled.CallEnd
            CallAction.MUTE -> Icons.Filled.MicOff
            CallAction.UNMUTE -> Icons.Filled.Mic
            CallAction.SPEAKER -> Icons.Filled.VolumeUp
            CallAction.SPEAKER_OFF -> Icons.Filled.VolumeOff
            CallAction.HOLD -> Icons.Filled.Pause
            CallAction.UNHOLD -> Icons.Filled.PlayArrow
            CallAction.SWITCH -> Icons.Filled.SwapCalls
            CallAction.AUDIO -> when (route) {
                AudioRoute.SPEAKER -> Icons.Filled.VolumeUp
                AudioRoute.BLUETOOTH -> Icons.Filled.Bluetooth
                AudioRoute.EARPIECE -> Icons.Filled.PhoneInTalk
            }
            CallAction.KEYPAD -> Icons.Filled.Dialpad
        },
        surface = surface,
        modifier = Modifier.height(72.dp),
        onClick = onClick,
    )
}

/** Bis hierher wirkt die eingestellte Textgroesse im Gespraech. Siehe cappedTextScale. */
const val INCALL_MAX_TEXT_SCALE = 1.25f

private fun actionLabel(action: CallAction) = when (action) {
    CallAction.ANSWER -> R.string.incall_answer
    CallAction.REJECT -> R.string.incall_reject
    CallAction.HANG_UP -> R.string.incall_hangup
    CallAction.MUTE -> R.string.incall_mute
    CallAction.UNMUTE -> R.string.incall_unmute
    CallAction.SPEAKER -> R.string.incall_speaker
    CallAction.SPEAKER_OFF -> R.string.incall_speaker_off
    CallAction.HOLD -> R.string.incall_hold
    CallAction.UNHOLD -> R.string.incall_unhold
    CallAction.SWITCH -> R.string.incall_switch
    // Die Beschriftung haengt am Weg, nicht an der Aktion - siehe audioLabel.
    CallAction.AUDIO -> R.string.call_audio
    CallAction.KEYPAD -> R.string.incall_keypad
}

private fun statusLabel(status: CallStatus) = when (status) {
    CallStatus.RINGING -> R.string.incall_ringing
    CallStatus.DIALING -> R.string.incall_dialing
    CallStatus.CONNECTING -> R.string.incall_connecting
    CallStatus.HOLDING -> R.string.incall_holding
    else -> R.string.incall_ended
}

/**
 * Wohin der Ton geht, als Beschriftung der Knopfzeile im Gespraech.
 *
 * Mit "Ton: " davor, weil die Zeile zwischen "Stumm" und "Halten" steht und sonst nicht zu
 * erkennen waere, dass sie eine Auskunft ist und keine Handlung. In der Auswahl selbst
 * faellt das Praefix weg - dort steht die Frage schon in der Ueberschrift.
 */
private fun audioLabel(route: AudioRoute): Int = when (route) {
    AudioRoute.EARPIECE -> R.string.incall_audio_earpiece
    AudioRoute.SPEAKER -> R.string.incall_audio_speaker
    AudioRoute.BLUETOOTH -> R.string.incall_audio_bluetooth
}

/**
 * Die Auswahl, wohin der Ton geht.
 *
 * Erscheint nur, wenn ein Bluetooth-Geraet verbunden ist: mit drei Moeglichkeiten reicht
 * ein Umschalter nicht, und eine vierte Knopfzeile passt auf drei Zoll nicht mehr dazu.
 * Der derzeitige Weg ist farbig hervorgehoben, damit die Auswahl auch eine Auskunft ist.
 */
@Composable
private fun AudioChoice(
    current: AudioRoute,
    onPick: (AudioRoute) -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalBigPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .safeDrawingPadding()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BigHeading(stringResource(R.string.call_audio))
        AudioRoute.entries.forEach { weg ->
            BigRow(
                label = stringResource(
                    when (weg) {
                        AudioRoute.EARPIECE -> R.string.call_audio_earpiece
                        AudioRoute.SPEAKER -> R.string.call_audio_speaker
                        AudioRoute.BLUETOOTH -> R.string.call_audio_bluetooth
                    },
                ),
                icon = when (weg) {
                    AudioRoute.SPEAKER -> Icons.Filled.VolumeUp
                    AudioRoute.BLUETOOTH -> Icons.Filled.Bluetooth
                    AudioRoute.EARPIECE -> Icons.Filled.PhoneInTalk
                },
                surface = if (weg == current) palette.surfaceAccent else palette.surfaceDefault,
                modifier = Modifier.height(72.dp),
                onClick = { onPick(weg) },
            )
        }
        BigRow(
            label = stringResource(R.string.prev_screen),
            icon = Icons.Filled.ArrowBack,
            modifier = Modifier.height(72.dp),
            onClick = onClose,
        )
    }
}
