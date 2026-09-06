package dev.kicker.hometiles.phone

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Message
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
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import dev.kicker.hometiles.ui.cappedTextScale
import dev.kicker.hometiles.ui.theme.LocalCornerRadius
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import dev.kicker.hometiles.ui.HomeTilesActivity
import dev.kicker.hometiles.ui.Notice
import dev.kicker.hometiles.R
import dev.kicker.hometiles.data.AudioRoute
import dev.kicker.hometiles.data.ConfigStore
import dev.kicker.hometiles.ui.BigKeypad
import dev.kicker.hometiles.ui.BigHeading
import dev.kicker.hometiles.ui.BigRow
import dev.kicker.hometiles.ui.ContactAvatar
import dev.kicker.hometiles.ui.tabularFigures
import dev.kicker.hometiles.ui.dpSp
import dev.kicker.hometiles.ui.theme.HomeTilesTheme
import dev.kicker.hometiles.ui.theme.LocalBigPalette

/**
 * the call screen.
 *
 * full screen, a name as large as possible, and buttons taking a whole row each. which
 * buttons appear is decided by [CallActions]; not a single condition for it stands in this
 * file, so it stays testable.
 *
 * the back gesture does nothing here: swiping away by accident during a call and then not
 * finding hang up would be the worst way to lose this app.
 */
class InCallActivity : HomeTilesActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            val view by InCallRepository.call.collectAsStateWithLifecycle()
            var showKeypad by remember { mutableStateOf(false) }
            // reachable only with bluetooth; otherwise the row toggles directly.
            var audioChoice by remember { mutableStateOf(false) }
            var replyChoice by remember { mutableStateOf(false) }
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

            HomeTilesTheme(
                config.appearance.theme,
                // capped: this screen does not scroll and the button height is fixed. at
                // 200 % a button read "Lautsprec...". the keypad digits and the caller's
                // name come from the area anyway.
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

                    if (replyChoice) {
                        ReplyChoice(
                            onPick = { text ->
                                if (InCallRepository.rejectWith(text).isFailure) {
                                    Notice.show(this@InCallActivity, R.string.call_action_failed)
                                }
                                replyChoice = false
                            },
                            onClose = { replyChoice = false },
                        )
                        return@HomeTilesTheme
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
                        return@HomeTilesTheme
                    }

                    // with the keypad open only two rows stay, or nothing is left for the
                    // digits. see CallActions.whileKeypad.
                    val rows =
                        if (showKeypad) CallActions.whileKeypad(current)
                        else CallActions.availableFor(current)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // the photo stands over the name and takes only what is left:
                            // CallerPhotoSize holds what name, state and buttons need. a
                            // photo that pushes the answer button off screen would be the
                            // worst fault on exactly this screen. no photo with the keypad
                            // open, where the room is needed for the digits.
                            // the *usable* height, not the window's: `screenHeightDp` counts
                            // the status and gesture bars in. with two rows the slack hid it,
                            // with three the last row ran under the gesture bar. seen at the
                            // emulator on 05.09.2026. same reckoning as in SystemBarsTest.
                            val screenDensity = LocalDensity.current
                            val insets = WindowInsets.safeDrawing
                            val usableHeightDp = LocalConfiguration.current.screenHeightDp -
                                with(screenDensity) {
                                    (insets.getTop(this) + insets.getBottom(this)).toDp().value
                                }
                            val photoHeight =
                                if (showKeypad) 0f
                                else CallerPhotoSize.heightDp(
                                    size = config.phone.callerPhoto,
                                    availableDp = usableHeightDp,
                                    buttons = rows.size,
                                    notice = current.otherName != null &&
                                        current.status == CallStatus.RINGING,
                                )
                            val photo = current.photoUri
                            val name = current.name?.takeIf { it.isNotBlank() }
                            when (CallerPhotoSize.imageFor(photoHeight, photo, name)) {
                                CallerPhotoSize.Image.NONE -> Unit
                                CallerPhotoSize.Image.PHOTO -> AsyncImage(
                                    model = photo,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .height(photoHeight.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(LocalCornerRadius.current)),
                                )
                                // no photo but a name: the initials, as everywhere else.
                                // reasoned in CallerPhotoSize.imageFor.
                                CallerPhotoSize.Image.INITIALS -> ContactAvatar(
                                    name = name.orEmpty(),
                                    photoUri = null,
                                    size = photoHeight.dp,
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
                                // the duration ticks every second and would wobble without
                                // tabular figures. `PLAN.md` 3.7.
                                style = tabularFigures(),
                            )
                            // a second call while one is running: without this row only the
                            // new caller would stand here and the one still on the line would
                            // be nowhere to be seen.
                            val other = current.otherName
                            if (other != null && current.status == CallStatus.RINGING) {
                                Text(
                                    text = stringResource(R.string.incall_other_active, other),
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
                            // empty space over the buttons keeps the ear from triggering
                            // something during the call.
                            Box(Modifier.weight(1f))
                        }

                        rows.forEach { action ->
                            ActionRow(
                                action = action,
                                keypadOpen = showKeypad,
                                route = current.audioRoute,
                            ) {
                                perform(
                                    action = action,
                                    toggleKeypad = { showKeypad = !showKeypad },
                                    openAudio = { audioChoice = true },
                                    openReplies = { replyChoice = true },
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
        openReplies: () -> Unit = {},
    ) {
        // any of these can fail: the call is gone by now, telecom took it from us.
        // `InCallRepository` catches that and returns a `Result`; throwing it away leaves a
        // dead button standing, and that button is answer, while it rings.
        val outcome: Result<*>? = when (action) {
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
            CallAction.REJECT_WITH_TEXT -> { openReplies(); null }
        }
        if (outcome?.isFailure == true) Notice.show(this, R.string.call_action_failed)
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
        // turning down with a word is not the same danger as turning down flat: it stays a
        // plain row, so the eye does not read two red buttons under the answer one.
        CallAction.REJECT_WITH_TEXT -> palette.surfaceDefault
        else -> palette.surfaceDefault
    }
    BigRow(
        label = stringResource(
            when {
                action == CallAction.KEYPAD && keypadOpen -> R.string.incall_keypad_hide
                // the audio row says where the sound goes; otherwise one would have to tap
                // it to find out.
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
            CallAction.REJECT_WITH_TEXT -> Icons.Filled.Message
        },
        surface = surface,
        modifier = Modifier.height(72.dp),
        onClick = onClick,
    )
}

/**
 * the short answers offered instead of a plain refusal.
 *
 * tapping one **sends it**: telecom takes the text and puts it on the wire itself. the row
 * therefore carries the whole sentence and nothing else, so that what leaves is what was
 * read. see OutgoingTest, which counts this place among those that may send.
 */
@Composable
private fun ReplyChoice(
    onPick: (String) -> Unit,
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
        BigHeading(stringResource(R.string.incall_reject_with_text))
        listOf(
            R.string.incall_reply_busy,
            R.string.incall_reply_later,
            R.string.incall_reply_who,
        ).forEach { text ->
            val body = stringResource(text)
            BigRow(
                label = body,
                icon = Icons.Filled.Message,
                // room to grow, not the fixed 72 dp of a button row: these labels are whole
                // sentences, and at this text size two lines do not fit in 72 dp. seen at
                // the emulator on 05.09.2026, where all three read "I cannot talk rig...".
                // what one is about to send has to be readable before it goes.
                modifier = Modifier.heightIn(min = 72.dp),
                onClick = { onPick(body) },
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

/** the text size setting acts on the call screen only up to here. see cappedTextScale. */
const val INCALL_MAX_TEXT_SCALE = 1.25f

private fun actionLabel(action: CallAction) = when (action) {
    CallAction.ANSWER -> R.string.incall_answer
    CallAction.REJECT_WITH_TEXT -> R.string.incall_reject_with_text
    CallAction.REJECT -> R.string.incall_reject
    CallAction.HANG_UP -> R.string.incall_hangup
    CallAction.MUTE -> R.string.incall_mute
    CallAction.UNMUTE -> R.string.incall_unmute
    CallAction.SPEAKER -> R.string.incall_speaker
    CallAction.SPEAKER_OFF -> R.string.incall_speaker_off
    CallAction.HOLD -> R.string.incall_hold
    CallAction.UNHOLD -> R.string.incall_unhold
    CallAction.SWITCH -> R.string.incall_switch
    // the label hangs on the route, not on the action. see audioLabel.
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
 * where the sound goes, as the label of the button row.
 *
 * prefixed, because the row sits between mute and hold and would otherwise not read as an
 * answer rather than an action. inside the picker the prefix falls away.
 */
private fun audioLabel(route: AudioRoute): Int = when (route) {
    AudioRoute.EARPIECE -> R.string.incall_audio_earpiece
    AudioRoute.SPEAKER -> R.string.incall_audio_speaker
    AudioRoute.BLUETOOTH -> R.string.incall_audio_bluetooth
}

/**
 * the picker for where the sound goes.
 *
 * only with a bluetooth device connected: a toggle does not cover three choices, and a
 * fourth button row does not fit three inches. the current route is highlighted, so the
 * picker is also an answer.
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
        AudioRoute.entries.forEach { route ->
            BigRow(
                label = stringResource(
                    when (route) {
                        AudioRoute.EARPIECE -> R.string.call_audio_earpiece
                        AudioRoute.SPEAKER -> R.string.call_audio_speaker
                        AudioRoute.BLUETOOTH -> R.string.call_audio_bluetooth
                    },
                ),
                icon = when (route) {
                    AudioRoute.SPEAKER -> Icons.Filled.VolumeUp
                    AudioRoute.BLUETOOTH -> Icons.Filled.Bluetooth
                    AudioRoute.EARPIECE -> Icons.Filled.PhoneInTalk
                },
                selected = route == current,
                modifier = Modifier.height(72.dp),
                onClick = { onPick(route) },
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
