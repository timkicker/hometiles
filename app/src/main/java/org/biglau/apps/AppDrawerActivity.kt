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
import androidx.compose.material.icons.filled.Settings
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
import org.biglau.ui.BigHeading
import androidx.compose.material.icons.filled.Apps
import org.biglau.ui.BigRow
import org.biglau.search.TextSearch
import org.biglau.ui.SettingsLink
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
            // Der Name der Einstellungszeile - hier geholt, weil er in der Liste selbst
            // nicht mehr zu holen ist, und weil er dort auch **gesucht** wird.
            val einstellungen = stringResource(R.string.apps_open_settings)
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
                    //
                    // **Eigener Text und nicht `app_gone`:** der sagt „Kachel neu belegen",
                    // und hier gibt es keine Kachel - der Rat waere ins Leere gesprochen.
                    // Stattdessen wird die Liste neu geladen, damit der tote Eintrag
                    // verschwindet, statt beim naechsten Tipp wieder nichts zu tun.
                    Notice.show(this@AppDrawerActivity, R.string.app_gone_list)
                    all = repository.loadApps()
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

            // Die Einstellungszeile ist ein Treffer wie jeder andere - sie steht mit in der
            // Liste, also zaehlt sie mit. Bis zum 03.09.2026 zaehlte nur `shown`: bei „big"
            // standen zwei Zeilen da und darueber „1 Treffer", und die Lupentaste oeffnete
            // die eine, ohne dass zu sehen war, welche.
            val einstellungTrifft = TextSearch.rank(einstellungen, query.trim()) != null
            // Was die Suche nicht zeigen darf, weil es ausgeblendet ist - aber sehr wohl
            // nennen muss. Siehe AppDrawer.hiddenMatches.
            val versteckteTreffer = remember(all, hidden, query) {
                AppDrawer.hiddenMatches(all, hidden, query)
            }
            val treffer = shown.size + if (einstellungTrifft) 1 else 0
            val einzigerTreffer: (() -> Unit)? = when {
                treffer != 1 -> null
                shown.size == 1 -> ({ open(shown.first()) })
                else -> ({ startActivity(SettingsLink.toRoot(this@AppDrawerActivity)) })
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
                                pluralStringResource(R.plurals.search_matches, treffer, treffer)
                            },
                            // Genau ein Treffer: die Lupentaste startet ihn direkt. Bei drei
                            // Zoll ist das oft der ganze Weg - man sieht die Liste nie.
                            //
                            // Sie geht durch `open`, nicht an ihm vorbei: bis zum 03.09.2026
                            // stand hier `launch`, und damit war die App-Sperre ueber das
                            // Suchfeld in einem Tastendruck zu umgehen.
                            onSearch = { einzigerTreffer?.invoke() },
                        )
                        // Nicht, wenn eine ausgeblendete App passt: dann steht die Zeile
                        // darunter und sagt etwas Genaueres. „Keine App passt dazu" waere
                        // daneben ein Widerspruch.
                        if (all.isNotEmpty() && shown.isEmpty() && versteckteTreffer.isEmpty()) {
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
                            // Der Weg in die Einstellungen, und zwar immer.
                            //
                            // Am 03.09.2026 am Geraet des Nutzers nachgesehen: acht belegte
                            // Kacheln, keine davon die Einstellungen, Wischen zwischen den
                            // Screens aus, kein freies Feld zum Langdruecken. Damit fuehrte
                            // kein Weg mehr dorthin ausser: eine vorhandene Kachel lange
                            // druecken und umbelegen - also eine App aufgeben, und man muss
                            // erst darauf kommen. Das Original hat die Einstellungen in der
                            // App-Liste; hier fehlten sie.
                            //
                            // Und sie steht auch da, wenn jemand **sucht**. Bis zum
                            // 03.09.2026 verschwand die Zeile, sobald ein Buchstabe im
                            // Suchfeld stand - dabei ist der Suchende genau der, der etwas
                            // sucht. Wer „einstell" tippt, findet jetzt die Einstellungen
                            // von BigLau und nicht nur die von Android.
                            if (einstellungTrifft) {
                                item {
                                    BigRow(
                                        label = einstellungen,
                                        icon = Icons.Filled.Settings,
                                        onClick = {
                                            startActivity(SettingsLink.toRoot(this@AppDrawerActivity))
                                        },
                                    )
                                }
                            }
                            // Eine Zeile, zwei Faelle: ohne Suche zaehlt sie alle
                            // ausgeblendeten Apps, mit Suche die, die dazu passen. Sie hing
                            // vorher an `query.isEmpty()` - also war sie genau dann weg,
                            // wenn sie gebraucht wird: wer eine ausgeblendete App sucht, las
                            // "Keine App passt dazu" und hatte keinen Anhaltspunkt mehr.
                            // Dieselbe Sache wie bei der Einstellungszeile darueber.
                            //
                            // Und **eine** Zeile, nicht zwei: zwei Zeilen mit demselben
                            // Symbol in einer Liste sind zwei, die man verwechselt -
                            // SlopRulesTest hat genau das gemeldet.
                            val versteckteZeile = when {
                                query.isNotEmpty() -> versteckteTreffer.size.takeIf { it > 0 }
                                else -> config.apps.hidden.size.takeIf { it > 0 }
                            }
                            if (versteckteZeile != null) {
                                item {
                                    BigRow(
                                        label = pluralStringResource(
                                            if (query.isEmpty()) {
                                                R.plurals.apps_hidden_count
                                            } else {
                                                R.plurals.apps_hidden_match
                                            },
                                            versteckteZeile,
                                            versteckteZeile,
                                        ),
                                        secondary = versteckteTreffer
                                            .takeIf { it.isNotEmpty() }
                                            ?.joinToString(", ") { app -> app.label },
                                        secondaryMaxLines = 1,
                                        icon = Icons.Filled.VisibilityOff,
                                        onClick = {
                                            startActivity(
                                                SettingsLink.toPage(
                                                    this@AppDrawerActivity,
                                                    SettingsLink.PAGE_HIDDEN_APPS,
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
