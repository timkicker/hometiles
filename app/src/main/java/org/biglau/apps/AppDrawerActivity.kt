package org.biglau.apps

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.ui.bigSp
import org.biglau.ui.BigLauActivity
import org.biglau.R
import org.biglau.data.ConfigStore
import org.biglau.security.Pin
import org.biglau.ui.PinGate
import org.biglau.settings.SettingsActivity
import org.biglau.ui.BigHeading
import androidx.compose.material.icons.filled.Apps
import org.biglau.ui.BigRow
import org.biglau.ui.BigSearchField
import org.biglau.ui.Notice
import org.biglau.ui.ScrollButtons
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

/**
 * Die vollstaendige App-Liste. Grosse Zeilen, Suche oben, zuletzt Benutztes davor.
 * Langdruck blendet eine App aus - der Weg zurueck fuehrt ueber die Einstellungen.
 */
class AppDrawerActivity : BigLauActivity() {

    companion object {
        /** Mit der Liste der zuletzt benutzten Apps oeffnen. */
        const val EXTRA_RECENT = "recentOnly"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)
        val repository = AppRepository.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            var all by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
            var query by rememberSaveable { mutableStateOf("") }
            // Von der Kachel "Zuletzt benutzt" aus: erst nur die letzten, aber jederzeit
            // umschaltbar - eine Liste ohne Weg zur vollstaendigen waere eine Sackgasse.
            var recentOnly by rememberSaveable {
                mutableStateOf(intent?.getBooleanExtra(EXTRA_RECENT, false) == true)
            }
            // Die App-Liste ist der Weg zu jeder App, die auf keiner Kachel liegt. Wer
            // eine PIN setzt und diesen Schutz einschaltet, will genau diesen Weg zu.
            var locked by remember {
                mutableStateOf(
                    Pin.protects(config.security.pin, config.security.pinProtectsAppList),
                )
            }

            LaunchedEffect(Unit) { all = repository.loadApps() }

            val hidden = config.apps.hidden
            val shown = remember(all, hidden, query) { AppDrawer.search(all, hidden, query) }
            val recents = remember(all, hidden, config.apps.recent, query) {
                if (query.isEmpty()) {
                    AppDrawer.recents(all, config.apps.recent, hidden, config.apps.recentCount)
                } else {
                    emptyList()
                }
            }

            fun launch(app: LaunchableApp) {
                store.update {
                    it.copy(
                        apps = it.apps.copy(recent = AppDrawer.remember(it.apps.recent, AppDrawer.keyOf(app))),
                    )
                }
                if (!repository.launch(app.packageName, app.activityName)) {
                    // Zwischen dem Aufbau der Liste und dem Tippen kann die App verschwinden.
                    Notice.show(this@AppDrawerActivity, R.string.app_gone)
                }
            }

            // Die Sperre gilt auch hier, nicht nur auf den Kacheln - sonst waere sie ueber
            // die Liste in einem Tipp zu umgehen.
            var lockedApp by remember { mutableStateOf<LaunchableApp?>(null) }
            fun open(app: LaunchableApp) {
                if (AppLock.needsPin(config, AppDrawer.keyOf(app), app.packageName)) {
                    lockedApp = app
                } else {
                    launch(app)
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
                    val gesperrt = lockedApp
                    if (gesperrt != null) {
                        PinGate(
                            title = stringResource(R.string.applock_locked),
                            explainer = stringResource(R.string.applock_locked_hint),
                            wrongText = stringResource(R.string.security_wrong_pin),
                            confirmLabel = stringResource(R.string.editor_done),
                            onCheck = { eingabe -> Pin.verify(eingabe, config.security.pin) },
                            onAccept = {
                                lockedApp = null
                                launch(gesperrt)
                            },
                            acceptOnComplete = true,
                        )
                        return@Box
                    }
                    if (locked) {
                        PinGate(
                            title = stringResource(R.string.apps_locked),
                            explainer = stringResource(R.string.apps_locked_hint),
                            wrongText = stringResource(R.string.security_wrong_pin),
                            confirmLabel = stringResource(R.string.editor_done),
                            onCheck = { eingabe -> Pin.verify(eingabe, config.security.pin) },
                            onAccept = { locked = false },
                            acceptOnComplete = true,
                        )
                        return@Box
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (query.isEmpty()) {
                            BigHeading(
                                stringResource(if (recentOnly) R.string.apps_recent else R.string.apps),
                            )
                        }
                        BigSearchField(
                            value = query,
                            onValueChange = { query = it },
                            hint = stringResource(R.string.search_apps),
                            secondary = if (query.isEmpty()) {
                                null
                            } else {
                                pluralStringResource(R.plurals.search_matches, shown.size, shown.size)
                            },
                            // Genau ein Treffer: die Lupentaste startet ihn direkt. Bei drei
                            // Zoll ist das oft der ganze Weg - man sieht die Liste nie.
                            onSearch = { shown.singleOrNull()?.let { launch(it) } },
                        )
                        if (all.isNotEmpty() && shown.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_no_match),
                                color = palette.onBackground,
                                fontSize = bigSp(18f),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                            )
                        }
                        val listState = rememberLazyListState()
                        if (recentOnly && query.isEmpty()) {
                            if (recents.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.apps_recent_none),
                                    color = palette.onBackground,
                                    fontSize = bigSp(17f),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                )
                            }
                            BigRow(
                                label = stringResource(R.string.apps_show_all),
                                icon = Icons.Filled.Apps,
                                surface = palette.surfaceAccent,
                                onClick = { recentOnly = false },
                            )
                        }
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            if (recents.isNotEmpty()) {
                                // In der Ansicht "Zuletzt benutzt" waere die Ueberschrift
                                // dieselbe wie der Seitentitel - zweimal dasselbe Wort
                                // untereinander sagt beim zweiten Mal nichts mehr.
                                if (!recentOnly || query.isNotEmpty()) {
                                    item { BigHeading(stringResource(R.string.apps_recent)) }
                                }
                                items(recents, key = { "recent-" + AppDrawer.keyOf(it) }) { app ->
                                    AppRow(app, repository, onClick = { open(app) }, onHide = null)
                                }
                                if (!recentOnly || query.isNotEmpty()) {
                                    item { BigHeading(stringResource(R.string.apps_all)) }
                                }
                            }
                            items(
                                if (recentOnly && query.isEmpty()) emptyList() else shown,
                                key = { AppDrawer.keyOf(it) },
                            ) { app ->
                                AppRow(
                                    app = app,
                                    repository = repository,
                                    onClick = { open(app) },
                                    onHide = {
                                        store.update {
                                            it.copy(apps = it.apps.copy(hidden = AppDrawer.toggleHidden(it.apps.hidden, app)))
                                        }
                                        // Ohne diesen Hinweis waere die App einfach verschwunden.
                                        Notice.show(
                                            this@AppDrawerActivity,
                                            getString(R.string.apps_hidden_hint, app.label),
                                        )
                                    },
                                )
                            }
                            // Der Weg zurueck zu den ausgeblendeten Apps, und zwar dort, wo
                            // man sie sucht. Vorher nannte eine Einblendung nur den Pfad
                            // ("Einstellungen -> Ausgeblendete Apps"), und die war nach zwei
                            // Sekunden weg. Die Zeile steht nur da, wenn wirklich etwas
                            // ausgeblendet ist - sonst waere sie eine Zeile ueber nichts.
                            if (config.apps.hidden.isNotEmpty() && query.isEmpty()) {
                                item {
                                    BigRow(
                                        label = pluralStringResource(
                                            R.plurals.apps_hidden_count,
                                            config.apps.hidden.size,
                                            config.apps.hidden.size,
                                        ),
                                        icon = Icons.Filled.VisibilityOff,
                                        onClick = {
                                            startActivity(
                                                Intent(this@AppDrawerActivity, SettingsActivity::class.java)
                                                    .putExtra(
                                                        SettingsActivity.EXTRA_PAGE,
                                                        SettingsActivity.PAGE_HIDDEN_APPS,
                                                    ),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        if (config.behaviour.accessibility.scrollButtons) {
                            ScrollButtons(listState)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: LaunchableApp,
    repository: AppRepository,
    onClick: () -> Unit,
    onHide: (() -> Unit)?,
) {
    val icon = remember(app) {
        repository.iconFor(app.packageName, app.activityName)?.toBitmap(72, 72)?.asImageBitmap()
    }
    BigRow(
        label = app.label,
        iconBitmap = icon,
        icon = if (icon == null) Icons.Filled.VisibilityOff else null,
        onClick = onClick,
        onLongClick = onHide,
    )
}
