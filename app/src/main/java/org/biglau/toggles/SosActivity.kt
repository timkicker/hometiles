package org.biglau.toggles

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
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
import org.biglau.ui.BigLauActivity
import org.biglau.R
import org.biglau.actions.Sos
import org.biglau.data.ConfigStore
import org.biglau.settings.SettingsActivity
import org.biglau.ui.BigHeading
import org.biglau.ui.BigRow
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

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            val sos = config.sos
            var startedAt by remember { mutableStateOf(System.currentTimeMillis()) }
            var remaining by remember { mutableStateOf(SosCountdown.clamp(sos.countdownSeconds)) }
            var result by remember { mutableStateOf<String?>(null) }

            val askSms = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { }

            val configured = SosCountdown.isConfigured(sos.numbers)

            // Der Alarm hoert auf, sobald dieser Bildschirm zu ist. Ein Ton, den man nur
            // durch Neustart losgeworden waere, macht aus dem Notruf ein Aergernis.
            DisposableEffect(Unit) {
                onDispose { SosAlarm.stop(this@SosActivity) }
            }

            LaunchedEffect(configured) {
                if (!configured) return@LaunchedEffect
                startedAt = System.currentTimeMillis()
                while (true) {
                    remaining = SosCountdown.remaining(startedAt, System.currentTimeMillis(), sos.countdownSeconds)
                    if (remaining == 0) break
                    delay(200)
                }
                // Erst jetzt, nicht schon waehrend des Countdowns: ein abgebrochener
                // Fehlalarm bleibt still. Siehe SosAlarm.
                SosAlarm.start(this@SosActivity, sos)
                val outcome = Sos.send(this@SosActivity, sos)
                result = when {
                    outcome.ok && outcome.hadLocation ->
                        resources.getQuantityString(R.plurals.sos_sent_location_plural, outcome.sent, outcome.sent)
                    outcome.ok ->
                        resources.getQuantityString(R.plurals.sos_sent_plain_plural, outcome.sent, outcome.sent)
                    else -> getString(R.string.sos_failed)
                }
                if (!outcome.ok) askSms.launch(Manifest.permission.SEND_SMS)
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
                        BigHeading(stringResource(R.string.sos))
                        when {
                            !configured -> {
                                Text(
                                    text = stringResource(R.string.sos_not_configured),
                                    color = palette.onBackground,
                                    fontSize = dpSp(17f),
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                                // Der Weg dorthin statt der Wegbeschreibung: wer den
                                // SOS-Knopf drueckt, will nicht lesen, wo etwas einzutragen
                                // waere. Dieselbe Regel wie bei der Anrufliste, die zu den
                                // Anrufarten fuehrt.
                                BigRow(
                                    label = stringResource(R.string.sos_open_settings),
                                    icon = Icons.Filled.Settings,
                                    surface = palette.surfaceAccent,
                                    onClick = {
                                        startActivity(
                                            Intent(this@SosActivity, SettingsActivity::class.java)
                                                .putExtra(
                                                    SettingsActivity.EXTRA_PAGE,
                                                    SettingsActivity.PAGE_SOS,
                                                ),
                                        )
                                        finish()
                                    },
                                )
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
                                BigRow(stringResource(R.string.dialog_close), onClick = { finish() })
                            }

                            else -> {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.sos_counting_plural,
                                        sos.numbers.size,
                                        sos.numbers.size,
                                    ),
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
}
