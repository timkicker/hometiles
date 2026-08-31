package org.biglau.apps

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.R
import org.biglau.data.ConfigStore
import org.biglau.ui.BigHeading
import org.biglau.ui.BigRow
import org.biglau.ui.BigSearchField
import org.biglau.ui.ScrollButtons
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

/**
 * Die vollstaendige App-Liste. Grosse Zeilen, Suche oben, zuletzt Benutztes davor.
 * Langdruck blendet eine App aus - der Weg zurueck fuehrt ueber die Einstellungen.
 */
class AppDrawerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)
        val repository = AppRepository.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            var all by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
            var query by rememberSaveable { mutableStateOf("") }
            val palette = LocalBigPalette.current

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
                repository.launch(app.packageName, app.activityName)
            }

            BigLauTheme(config.appearance.theme, config.appearance.textScale) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(palette.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (query.isEmpty()) {
                            BigHeading(stringResource(R.string.apps))
                        }
                        BigSearchField(
                            value = query,
                            onValueChange = { query = it },
                            hint = stringResource(R.string.search_apps),
                        )
                        if (all.isNotEmpty() && shown.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_no_match),
                                color = palette.onBackground,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                            )
                        }
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            if (recents.isNotEmpty()) {
                                item { BigHeading(stringResource(R.string.apps_recent)) }
                                items(recents, key = { "recent-" + AppDrawer.keyOf(it) }) { app ->
                                    AppRow(app, repository, onClick = { launch(app) }, onHide = null)
                                }
                                item { BigHeading(stringResource(R.string.apps_all)) }
                            }
                            items(shown, key = { AppDrawer.keyOf(it) }) { app ->
                                AppRow(
                                    app = app,
                                    repository = repository,
                                    onClick = { launch(app) },
                                    onHide = {
                                        store.update {
                                            it.copy(apps = it.apps.copy(hidden = AppDrawer.toggleHidden(it.apps.hidden, app)))
                                        }
                                        // Ohne diesen Hinweis waere die App einfach verschwunden.
                                        Toast.makeText(
                                            this@AppDrawerActivity,
                                            getString(R.string.apps_hidden_hint, app.label),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    },
                                )
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
