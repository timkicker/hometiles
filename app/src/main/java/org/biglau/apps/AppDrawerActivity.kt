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
 * the full app list: large rows, search on top, recently used in front. a long press hides
 * an app; the way back leads through the settings.
 */
class AppDrawerActivity : BigLauActivity() {

    companion object {
        /** open showing the recently used apps. */
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
            // the settings row's name, fetched here because the list itself cannot, and
            // because it is *searched* there too.
            val einstellungen = stringResource(R.string.apps_open_settings)
            // from the recently used tile: the recent ones first, switchable at any time.
            // a list without a way to the full one would be a dead end.
            var recentOnly by rememberSaveable {
                mutableStateOf(intent?.getBooleanExtra(EXTRA_RECENT, false) == true)
            }
            // the app list is the way to every app that sits on no tile. setting a pin and
            // turning this on means wanting exactly that way closed.
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
                    // an app can vanish between building the list and the tap.
                    //
                    // a text of its own and not `app_gone`, which advises reassigning a tile
                    // where there is none. the list reloads instead, so the dead entry goes
                    // rather than doing nothing again on the next tap.
                    Notice.show(this@AppDrawerActivity, R.string.app_gone_list)
                    all = repository.loadApps()
                }
            }

            // the lock holds here too, not only on the tiles, or one tap through the list
            // would walk around it.
            var lockedApp by remember { mutableStateOf<LaunchableApp?>(null) }
            fun open(app: LaunchableApp) {
                if (AppLock.needsPin(config, AppDrawer.keyOf(app), app.packageName)) {
                    lockedApp = app
                } else {
                    launch(app)
                }
            }

            // the settings row is a match like any other and stands in the list, so it
            // counts: with only `shown` counted, two rows stood under a count of one, and
            // the search key opened one of them without showing which.
            val einstellungTrifft = TextSearch.rank(einstellungen, query.trim()) != null
            // what the search may not show because it is hidden, but must still name. see
            // AppDrawer.hiddenMatches.
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
                            // exactly one match: the search key starts it. on three inches
                            // that is often the whole way, and the list is never seen.
                            //
                            // it goes through `open` and not past it: a `launch` here let one
                            // key press walk around the app lock.
                            onSearch = { einzigerTreffer?.invoke() },
                        )
                        // not when a hidden app matches: the row below then says something
                        // more precise, and no app matches beside it would contradict it.
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
                                // in the recently used view the heading would repeat the
                                // page title, which says nothing the second time.
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
                                        // without this the app would simply have vanished.
                                        Notice.show(
                                            this@AppDrawerActivity,
                                            getString(R.string.apps_hidden_hint, app.label),
                                        )
                                    },
                                )
                            }
                            // the way into the settings, and always.
                            //
                            // measured on the user's device: eight assigned tiles, none of
                            // them the settings, swiping off, no free slot to long press. no
                            // way there was left except giving up an app by reassigning its
                            // tile, and one has to think of that first.
                            //
                            // and it stands there while someone *searches* too: the row used
                            // to vanish at the first letter, though searching is exactly what
                            // the searcher does.
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
                            // one row, two cases: without a search it counts all hidden
                            // apps, with one those that match. hanging on `query.isEmpty()`
                            // it was gone exactly when needed.
                            //
                            // and *one* row, not two: two rows with the same icon in a list
                            // are two that get confused.
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
