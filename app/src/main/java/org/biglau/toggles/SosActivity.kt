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
import org.biglau.actions.Sos
import org.biglau.actions.SosLocation
import org.biglau.actions.SosFailure
import org.biglau.data.ConfigStore
import org.biglau.ui.BigHeading
import org.biglau.ui.BigRow
import org.biglau.ui.SettingsLink
import org.biglau.ui.dpSp
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

/**
 * Der Notruf-Ablauf.
 *
 * Der Countdown ist kein Schmuck: eine Notruf-Kachel wird auch versehentlich getroffen, und
 * eine SMS an drei Menschen laesst sich nicht zurueckholen. Der Abbruch ist deshalb die
 * groesste Flaeche auf dem Bildschirm - im Ernstfall drueckt man nicht daneben, und im
 * Versehensfall trifft man ihn sofort.
 */
class SosActivity : BigLauActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)

        // Probe: derselbe Ablauf, aber am Ende geht nichts hinaus. Gedacht zum Zeigen -
        // wer den Notruf einrichtet, will ihn dem Menschen erklaeren koennen, der ihn
        // spaeter im Ernst drueckt. Und geprueft werden kann der Bildschirm damit auch.
        val probe = intent?.getBooleanExtra(EXTRA_PREVIEW, false) == true

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            val sos = config.sos
            var startedAt by remember { mutableStateOf(System.currentTimeMillis()) }
            var remaining by remember { mutableStateOf(SosCountdown.clamp(sos.countdownSeconds)) }
            var result by remember { mutableStateOf<String?>(null) }
            // Nur in der Probe: der Text, der hinausginge, und ob ein Standort drin steht.
            var previewText by remember { mutableStateOf<String?>(null) }
            var previewLocation by remember { mutableStateOf(false) }

            val askSms = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { }

            val configured = SosCountdown.isConfigured(sos.numbers)

            // Der Alarm hoert auf, sobald dieser Bildschirm zu ist. Ein Ton, den man nur
            // durch Neustart losgeworden waere, macht aus dem Notruf ein Aergernis.
            // Waehrend der Countdown laeuft, sucht das Telefon nach einer frischen
            // Position - die zuletzt bekannte ist oft Stunden alt. Siehe SosLocation.
            val ortung = remember { SosLocation(this@SosActivity) }
            DisposableEffect(Unit) {
                if (sos.sendLocation) ortung.start()
                onDispose {
                    SosAlarm.stop(this@SosActivity)
                    ortung.stop()
                }
            }

            LaunchedEffect(configured, probe) {
                // In der Probe laeuft der Countdown auch ohne eingetragene Kontakte: sie
                // soll den Ablauf zeigen, und wer sie startet, hat den Notruf gerade erst
                // vor sich. Am Ende steht dann trotzdem, dass ohne Kontakte auch im
                // Ernstfall nichts hinausginge.
                if (!configured && !probe) return@LaunchedEffect
                startedAt = System.currentTimeMillis()
                while (true) {
                    remaining = SosCountdown.remaining(startedAt, System.currentTimeMillis(), sos.countdownSeconds)
                    if (remaining == 0) break
                    delay(200)
                }
                if (probe) {
                    // Kein Sos.send: eine Probe, die sendet, ist keine. Gezeigt wird aber,
                    // **was** hinausginge - sonst liesse sich der Text nur herausfinden,
                    // indem man ihn abschickt.
                    val (text, mitStandort) = Sos.compose(this@SosActivity, sos)
                    result = listOfNotNull(
                        getString(R.string.sos_preview_done),
                        getString(R.string.sos_not_configured).takeIf { !configured },
                    ).joinToString(" ")
                    previewText = text
                    previewLocation = mitStandort
                    return@LaunchedEffect
                }
                // Erst jetzt, nicht schon waehrend des Countdowns: ein abgebrochener
                // Fehlalarm bleibt still. Siehe SosAlarm.
                //
                // Und **nach** der Probe, nicht davor: die Probe sagt von sich „derselbe
                // Ablauf wie im Ernstfall, es geht nichts hinaus" - und startete dabei die
                // Sirene, die lauteste Sache dieser App, an Bitte-nicht-stoeren vorbei. Wer
                // den Alarm hoeren will, hat dafuer in den Einstellungen „Jetzt ausprobieren"
                // samt Stopp-Knopf; eine Probe, die das Haus weckt, ist keine.
                SosAlarm.start(this@SosActivity, sos)
                val outcome = Sos.send(this@SosActivity, sos)
                result = when {
                    outcome.ok && outcome.hadLocation ->
                        resources.getQuantityString(R.plurals.sos_sent_location_plural, outcome.sent, outcome.sent)
                    outcome.ok ->
                        resources.getQuantityString(R.plurals.sos_sent_plain_plural, outcome.sent, outcome.sent)
                    else -> getString(Sos.failureText(outcome.failure))
                }
                // Nur fragen, wenn die Erlaubnis wirklich fehlt. Nach einem Netzfehler
                // danach zu fragen, schiebt die Schuld auf etwas, das gar nicht fehlte.
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
                        // In der Probe heisst der Bildschirm anders - sonst weiss niemand,
                        // der ihn zufaellig sieht, ob es gerade ernst ist.
                        BigHeading(
                            stringResource(if (probe) R.string.sos_preview_title else R.string.sos),
                        )
                        when {
                            !configured && !probe -> {
                                Text(
                                    text = stringResource(R.string.sos_not_configured),
                                    color = palette.onBackground,
                                    fontSize = dpSp(17f),
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                                NotrufEinrichtenZeile()
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
                                // Der Text, der hinausginge - Wort fuer Wort, mit dem
                                // Kartenlink, wenn ein Standort da ist. Wer den Notruf fuer
                                // jemanden einrichtet, soll ihn lesen koennen, bevor er im
                                // Ernstfall bei jemand anderem ankommt.
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
                                // Wer die Probe macht und noch keine Kontakte hat, liest
                                // hier denselben Satz - und stand bis zum 3.9.2026 ohne Weg
                                // dorthin da, ausgerechnet an der Stelle, an der jemand den
                                // Notruf gerade einrichtet.
                                if (!configured) NotrufEinrichtenZeile()
                                // PLAN.md 4.8: "Danach ein Anruf-Knopf, kein automatischer
                                // Anruf." Der Satz "Es konnte nichts gesendet werden" war
                                // bis zum 03.09.2026 das Ende des Bildschirms - im
                                // schlimmsten Fall der ganzen App. `Intents.dial` oeffnet
                                // die Waehltastatur mit der Nummer und waehlt **nicht**:
                                // ein Tipp entfernt, und nie von selbst. In der Probe steht
                                // der Knopf nicht, dort ist nichts passiert.
                                if (!probe && configured) {
                                    sos.numbers.firstOrNull()?.let { nummer ->
                                        BigRow(
                                            label = stringResource(R.string.sos_call_now, PhoneNumbers.forDisplay(nummer)),
                                            secondary = stringResource(R.string.sos_call_hint),
                                            icon = Icons.Filled.Call,
                                            surface = palette.surfaceAccent,
                                            onClick = { Intents.dial(this@SosActivity, nummer) },
                                        )
                                    }
                                }
                                BigRow(stringResource(R.string.dialog_close), onClick = { finish() })
                            }

                            else -> {
                                Text(
                                    // In der Probe steht hier, dass nichts hinausgeht -
                                    // "Wird an 0 Kontakte gesendet" waere sonst der Satz,
                                    // und der ist weder wahr noch verstaendlich.
                                    text = if (probe) {
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
                                        color = palette.danger,
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
        /** Probe: derselbe Ablauf, aber es geht nichts hinaus. */
        const val EXTRA_PREVIEW = "biglau.sos.preview"
    }

    /**
     * Der Weg dorthin statt der Wegbeschreibung: wer den SOS-Knopf drueckt, will nicht
     * lesen, wo etwas einzutragen waere. Dieselbe Regel wie bei der Anrufliste, die zu den
     * Anrufarten fuehrt.
     *
     * Steht als eigene Funktion da, weil beide Zweige sie brauchen - der Ernstfall ohne
     * Kontakte und die Probe ohne Kontakte. Zweimal hingeschrieben waere es zweimal zu
     * pflegen, und `SlopRulesTest` haette es ohnehin gemeldet: zwei gleiche Symbole in
     * einer Funktion.
     */
    @Composable
    private fun NotrufEinrichtenZeile() {
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
