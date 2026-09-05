package org.biglau.toggles

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.biglau.phone.PhoneNumbers
import org.biglau.actions.Intents
import org.biglau.ui.BigLauActivity
import org.biglau.R
import org.biglau.data.ConfigStore
import org.biglau.ui.BigHeading
import org.biglau.ui.BigRow
import org.biglau.ui.SettingsLink
import org.biglau.ui.dpSp
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

/**
 * the emergency sequence.
 *
 * the countdown is not ornament: an sos tile gets hit by accident, and an sms to three
 * people cannot be recalled. cancel is therefore the largest area on the screen.
 */
class SosActivity : BigLauActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)

        // preview: the same sequence, but nothing goes out at the end. for showing it to
        // the person who will one day press it for real.
        val preview = intent?.getBooleanExtra(EXTRA_PREVIEW, false) == true

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            val sos = config.sos
            var startedAt by remember { mutableStateOf(System.currentTimeMillis()) }
            var remaining by remember { mutableStateOf(SosCountdown.clamp(sos.countdownSeconds)) }
            var result by remember { mutableStateOf<String?>(null) }
            // preview only: the text that would go out, and whether a location is in it.
            var previewText by remember { mutableStateOf<String?>(null) }
            var previewLocation by remember { mutableStateOf(false) }

            val askSms = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { }

            val configured = SosCountdown.isConfigured(sos.numbers)

            // the alarm stops as soon as this screen closes: a sound one could only lose by
            // restarting turns the emergency call into a nuisance. while the countdown runs
            // the phone looks for a fresh position; the last known one is often hours old.
            val locator = remember { SosLocation(this@SosActivity) }
            DisposableEffect(Unit) {
                if (sos.sendLocation) locator.start()
                onDispose {
                    SosAlarm.stop(this@SosActivity)
                    locator.stop()
                }
            }

            LaunchedEffect(configured, preview) {
                // in the preview the countdown runs without contacts too: it is there to
                // show the sequence. the end still says that nothing would go out.
                if (!configured && !preview) return@LaunchedEffect
                startedAt = System.currentTimeMillis()
                while (true) {
                    remaining = SosCountdown.remaining(startedAt, System.currentTimeMillis(), sos.countdownSeconds)
                    if (remaining == 0) break
                    delay(200)
                }
                if (preview) {
                    // no Sos.send: a preview that sends is none. what *would* go out is
                    // shown, or the text could only be learnt by sending it.
                    val (text, withLocation) = Sos.compose(this@SosActivity, sos)
                    result = listOfNotNull(
                        getString(R.string.sos_preview_done),
                        getString(R.string.sos_not_configured).takeIf { !configured },
                    ).joinToString(" ")
                    previewText = text
                    previewLocation = withLocation
                    return@LaunchedEffect
                }
                // only now, not during the countdown: a cancelled false alarm stays silent.
                //
                // and *after* the preview returns, not before: the preview promises nothing
                // goes out and was starting the siren, the loudest thing in this app, past
                // do-not-disturb. the settings have a try-it-now button with a stop.
                SosAlarm.start(this@SosActivity, sos)
                val outcome = Sos.send(this@SosActivity, sos)
                result = when {
                    outcome.ok && outcome.hadLocation ->
                        resources.getQuantityString(R.plurals.sos_sent_location_plural, outcome.sent, outcome.sent)
                    outcome.ok ->
                        resources.getQuantityString(R.plurals.sos_sent_plain_plural, outcome.sent, outcome.sent)
                    else -> getString(Sos.failureText(outcome.failure))
                }
                // ask only when the permission is really missing: asking after a network
                // failure blames something that was not absent.
                if (outcome.failure == SosFailure.NO_PERMISSION) {
                    askSms.launch(Manifest.permission.SEND_SMS)
                }
            }

            BigLauTheme(
                config.appearance.theme,
                config.appearance.textScale,
                haptics = config.behaviour.haptics,
                font = config.appearance.font,
                labelScale = config.appearance.labelScale,
                iconPercent = config.appearance.iconPercent,
                icons = config.appearance.icons,
                hideCutLabels = config.appearance.hideCutLabels,
                cornerRadiusDp = config.appearance.cornerRadiusDp,
            ) {
                val palette = LocalBigPalette.current
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(palette.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // the preview carries another title, or nobody seeing the screen by
                        // chance knows whether this is real.
                        BigHeading(
                            stringResource(if (preview) R.string.sos_preview_title else R.string.sos),
                        )
                        when {
                            !configured && !preview -> {
                                Text(
                                    text = stringResource(R.string.sos_not_configured),
                                    color = palette.onBackground,
                                    fontSize = dpSp(17f),
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                                SosSetupRow()
                                BigRow(stringResource(R.string.dialog_close), onClick = { finish() })
                            }

                            result != null -> {
                                Text(
                                    text = result.orEmpty(),
                                    color = palette.onBackground,
                                    fontSize = dpSp(20f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                                // the text that would go out, word for word, with the map
                                // link if there is a location.
                                previewText?.let { text ->
                                    Text(
                                        text = stringResource(
                                            if (previewLocation) R.string.sos_preview_text_location
                                            else R.string.sos_preview_text,
                                        ),
                                        color = palette.onBackground,
                                        fontSize = dpSp(15f),
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                    )
                                    Text(
                                        text = text,
                                        color = palette.onBackground,
                                        fontSize = dpSp(17f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f, fill = false)
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 4.dp),
                                    )
                                }
                                // the preview without contacts reads the same sentence, at
                                // exactly the moment someone is setting the sos up.
                                if (!configured) SosSetupRow()
                                // `PLAN.md` 4.8: a call button afterwards, never an
                                // automatic call. nothing could be sent used to be the end of
                                // the screen. `Intents.dial` opens the keypad with the number
                                // and does not dial: one tap away, never by itself. no button
                                // in the preview, where nothing happened.
                                if (!preview && configured) {
                                    sos.numbers.firstOrNull()?.let { number ->
                                        BigRow(
                                            label = stringResource(R.string.sos_call_now, PhoneNumbers.forDisplay(number)),
                                            secondary = stringResource(R.string.sos_call_hint),
                                            icon = Icons.Filled.Call,
                                            surface = palette.surfaceAccent,
                                            onClick = { Intents.dial(this@SosActivity, number) },
                                        )
                                    }
                                }
                                BigRow(stringResource(R.string.dialog_close), onClick = { finish() })
                            }

                            else -> {
                                Text(
                                    // the preview says nothing goes out: sending to 0
                                    // contacts is neither true nor understandable.
                                    text = if (preview) {
                                        stringResource(R.string.sos_preview_hint)
                                    } else {
                                        pluralStringResource(
                                            R.plurals.sos_counting_plural,
                                            sos.numbers.size,
                                            sos.numbers.size,
                                        )
                                    },
                                    color = palette.onBackground,
                                    fontSize = dpSp(17f),
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize().weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = remaining.toString(),
                                        color = palette.dangerText,
                                        fontSize = dpSp(120f),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                BigRow(
                                    label = stringResource(R.string.sos_cancel),
                                    surface = palette.surfaceAccent,
                                    onClick = { finish() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        /** preview: the same sequence, but nothing goes out. */
        const val EXTRA_PREVIEW = "biglau.sos.preview"
    }

    /**
     * the way there instead of directions to it: whoever pressed the sos button does not
     * want to read where something would have to be entered.
     *
     * a function of its own because both branches need it, the real case without contacts
     * and the preview without contacts.
     */
    @Composable
    private fun SosSetupRow() {
        BigRow(
            label = stringResource(R.string.sos_open_settings),
            icon = Icons.Filled.Settings,
            surface = LocalBigPalette.current.surfaceAccent,
            onClick = {
                startActivity(SettingsLink.toPage(this@SosActivity, SettingsLink.PAGE_SOS))
                finish()
            },
        )
    }

}
