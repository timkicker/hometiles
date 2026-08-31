package org.biglau

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import org.biglau.actions.Flashlight
import org.biglau.actions.Intents
import org.biglau.apps.AppDrawerActivity
import org.biglau.apps.AppRepository
import org.biglau.data.Builtin
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.ConfigStore
import org.biglau.contacts.ContactsActivity
import org.biglau.data.ContactMode
import org.biglau.info.BatteryRepository
import org.biglau.notify.NotificationRepository
import org.biglau.notify.SystemPackagesReader
import org.biglau.settings.SettingsActivity
import org.biglau.widgets.WidgetHostController
import org.biglau.shortcuts.ShortcutRepository
import org.biglau.tiles.TileEditorActivity
import org.biglau.ui.HomeHeader
import org.biglau.ui.HomeScreenView
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

class MainActivity : ComponentActivity() {

    private val widgetHost by lazy { WidgetHostController.get(this) }

    override fun onStart() {
        super.onStart()
        // Ohne Zuhoeren aktualisiert sich kein Widget; ohne Aufhoeren laufen sie im
        // Hintergrund weiter und kosten Akku.
        widgetHost.startListening()
    }

    override fun onStop() {
        widgetHost.stopListening()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val store = ConfigStore.get(this)
        val apps = AppRepository.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            val context = LocalContext.current
            var screenId by rememberSaveable { mutableStateOf(config.homeScreenId) }
            val screen = config.screenById(screenId) ?: config.homeScreen
            val counts by NotificationRepository.counts.collectAsStateWithLifecycle()
            // Bei jeder Rueckkehr neu lesen: der Nutzer kann die Standard-App
            // zwischendurch in den Systemeinstellungen gewechselt haben.
            val systemPackages = remember(counts) { SystemPackagesReader.read(context) }
            val battery by remember { BatteryRepository.readings(context) }
                .collectAsStateWithLifecycle(initialValue = null)

            // Ein Launcher darf die Zurueck-Geste nicht wie eine gewoehnliche App behandeln:
            // auf dem Startscreen tut sie nichts. Auf einem Nebenscreen fuehrt sie heim,
            // sonst waere ein Screen ohne Heim-Kachel eine Sackgasse.
            //
            // Bewusst immer aktiv statt onBackPressed zu ueberschreiben - ein solcher
            // Override schluckt das Ereignis, bevor dieser Handler es ueberhaupt sieht.
            BackHandler(enabled = true) {
                if (screenId != config.homeScreenId) {
                    screenId = config.homeScreenId
                }
            }

            BigLauTheme(
                theme = config.appearance.theme,
                textScale = config.appearance.textScale,
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(LocalBigPalette.current.background)
                        .safeDrawingPadding(),
                ) {
                    if (!isDefaultHome()) {
                        HomeRolePrompt()
                    }
                    if (config.appearance.showHeader) {
                        HomeHeader(
                            battery = battery,
                            showDate = config.appearance.clockShowsDate,
                            modifier = Modifier.padding(
                                start = 8.dp,
                                end = 8.dp,
                                top = 4.dp,
                                bottom = 4.dp,
                            ),
                        )
                    }
                    HomeScreenView(
                        screen = screen,
                        appearance = config.appearance,
                        modifier = Modifier.fillMaxSize(),
                        appIcon = { pkg, act ->
                            apps.iconFor(pkg, act)?.toBitmap(96, 96)?.asImageBitmap()
                        },
                        appLabel = { pkg, act -> apps.labelFor(pkg, act) },
                        shortcutIcon = { pkg, id ->
                            ShortcutRepository.get(context)
                                .iconFor(pkg, id, resources.displayMetrics.densityDpi)
                                ?.toBitmap(96, 96)?.asImageBitmap()
                        },
                        notificationCounts = if (config.behaviour.blinkOnNotification) counts else emptyMap(),
                        systemPackages = systemPackages,
                        battery = battery,
                        onActivate = { cell -> activate(cell, apps) { screenId = it } },
                        onEdit = { x, y ->
                            context.startActivity(
                                TileEditorActivity.intent(context, screen.id, x, y)
                            )
                        },
                    )
                }
            }
        }
    }

    private fun activate(cell: Cell, apps: AppRepository, goToScreen: (String) -> Unit) {
        when (val action = cell.button.action) {
            is ButtonAction.App -> apps.launch(action.packageName, action.activityName)

            is ButtonAction.Shortcut ->
                if (!ShortcutRepository.get(this).launch(action.packageName, action.shortcutId)) {
                    Toast.makeText(this, R.string.shortcut_gone, Toast.LENGTH_SHORT).show()
                }

            is ButtonAction.Contact -> when (action.mode) {
                ContactMode.CALL -> Intents.call(this, action.number)
                ContactMode.SMS -> Intents.sms(this, action.number)
                ContactMode.ASK -> Intents.dial(this, action.number)
            }

            is ButtonAction.GoToScreen -> goToScreen(action.screenId)

            is ButtonAction.Action -> when (action.builtin) {
                Builtin.DIALER -> Intents.openDialer(this)
                Builtin.MESSAGES -> Intents.openMessages(this)
                Builtin.CONTACTS -> startActivity(Intent(this, ContactsActivity::class.java))
                Builtin.CAMERA -> Intents.openCamera(this)
                Builtin.CLOCK -> Intents.openClock(this)
                Builtin.BATTERY -> Unit
                Builtin.FLASHLIGHT -> Flashlight.toggle(this)
                Builtin.HOME_SCREEN -> goToScreen(ConfigStore.get(this).current.homeScreenId)
                Builtin.SETTINGS -> startActivity(Intent(this, SettingsActivity::class.java))
                Builtin.APP_LIST -> startActivity(Intent(this, AppDrawerActivity::class.java))
                else -> Toast.makeText(this, R.string.editor_soon, Toast.LENGTH_SHORT).show()
            }

            // Ein Widget bedient sich selbst - ein Antippen der Zelle tut hier nichts.
            is ButtonAction.Widget -> Unit

            ButtonAction.None -> Unit
        }
    }

    /**
     * Ein Launcher ist erst nuetzlich, wenn er die Home-Taste bekommt.
     *
     * PackageManager.resolveActivity taugt dafuer nicht: ohne gesetzte Praeferenz liefert es
     * auf dem Jelly 2 die aufrufende App selbst zurueck, obwohl die Home-Taste woanders landet.
     * Verlaesslich ist die Rollenabfrage; darunter bleibt der Abgleich der bevorzugten Aktivitaeten.
     */
    private fun isDefaultHome(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roles.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val filters = mutableListOf<IntentFilter>()
        val activities = mutableListOf<ComponentName>()
        packageManager.getPreferredActivities(filters, activities, null)
        return filters.indices.any { i ->
            filters[i].hasCategory(Intent.CATEGORY_HOME) &&
                activities.getOrNull(i)?.packageName == packageName
        }
    }
}

@Composable
private fun HomeRolePrompt() {
    val context = LocalContext.current
    val palette = LocalBigPalette.current
    Text(
        text = stringResource(R.string.set_as_home),
        color = palette.surfaceAccent.ink,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surfaceAccent.fill)
            .clickable { Intents.chooseHomeApp(context) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
