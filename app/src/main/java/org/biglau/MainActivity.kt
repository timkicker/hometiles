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
import androidx.compose.foundation.layout.Box
import org.biglau.ui.BigRow
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import org.biglau.actions.Intents
import org.biglau.apps.AppDrawerActivity
import org.biglau.apps.AppRepository
import org.biglau.data.Builtin
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import androidx.compose.runtime.LaunchedEffect
import org.biglau.data.ConfigStore
import org.biglau.data.LauncherConfig
import org.biglau.safety.CrashGuard
import org.biglau.safety.CrashRecorder
import org.biglau.safety.EmergencyScreen
import org.biglau.safety.StartMode
import org.biglau.contacts.ContactsActivity
import org.biglau.data.ContactMode
import org.biglau.info.BatteryRepository
import org.biglau.notify.NotificationRepository
import org.biglau.phone.DialerActivity
import org.biglau.toggles.SosActivity
import org.biglau.wizard.WizardActivity
import org.biglau.wizard.WizardSteps
import org.biglau.toggles.ToggleActions
import org.biglau.toggles.ToggleKind
import org.biglau.notify.SystemPackagesReader
import org.biglau.settings.SettingsActivity
import org.biglau.sms.SmsActivity
import org.biglau.widgets.WidgetHostController
import org.biglau.shortcuts.ShortcutRepository
import org.biglau.tiles.TileEditorActivity
import org.biglau.a11y.LongPress
import org.biglau.a11y.LongPressAction
import org.biglau.a11y.Speaker
import org.biglau.ui.HomeHeader
import org.biglau.ui.labelRes
import org.biglau.ui.HomeScreenView
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_EDIT_MODE = "editMode"
    }

    /**
     * Der Launcher laeuft als singleTask - wer ihn aus den Einstellungen heraus startet, landet
     * in onNewIntent statt in onCreate. Der Wunsch muss deshalb hier liegen und nicht im Intent,
     * sonst kaeme man ueber die Einstellungen nie in den Bearbeitungsmodus.
     */
    private val editModeRequest = mutableStateOf(false)

    /**
     * Welcher Screen gerade zu sehen ist - `null` heisst "der Startbildschirm, wie er in der
     * Konfiguration steht".
     *
     * Bewusst hier und nicht als `rememberSaveable` in der Komposition: die Activity laeuft
     * als singleTask und wird nicht neu gebaut. Ein gemerkter Anfangswert blieb deshalb
     * stehen, wenn der Nutzer in den Einstellungen einen anderen Startbildschirm waehlte -
     * er tippte "Zum Startbildschirm machen", ging heim und sah den alten. Und die
     * Heim-Geste soll aus einem Nebenscreen herausfuehren, nicht nur die App wiederholen.
     */
    private val currentScreen = mutableStateOf<String?>(null)

    /** Der Kontakt, bei dem gerade "anrufen oder schreiben?" offen steht. */
    private val contactChoice = mutableStateOf<ButtonAction.Contact?>(null)

    override fun onDestroy() {
        // Die Sprachausgabe haelt eine Verbindung zum System-Dienst; ohne dieses Aufraeumen
        // bliebe sie ueber die Lebenszeit der Activity hinaus offen.
        Speaker.shutdown()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_EDIT_MODE, false)) {
            editModeRequest.value = true
        } else if (Intent.ACTION_MAIN == intent.action) {
            // Die Heim-Geste auf einem Nebenscreen fuehrt heim.
            currentScreen.value = null
        }
    }


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
        if (intent?.getBooleanExtra(EXTRA_EDIT_MODE, false) == true) editModeRequest.value = true
        enableEdgeToEdge()

        val crashes = CrashRecorder.get(this)
        val startMode = CrashGuard.modeFor(crashes.noteStart())

        if (startMode == StartMode.SAFE) {
            setContent {
                EmergencyScreen(
                    lastCrash = crashes.lastCrash(),
                    onRetry = {
                        crashes.clearCrash()
                        recreate()
                    },
                    onSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    onChooseOtherLauncher = { Intents.chooseHomeApp(this) },
                    onResetConfig = {
                        ConfigStore.get(this).update { LauncherConfig() }
                        crashes.clearCrash()
                        recreate()
                    },
                )
            }
            // Auch der Notmodus zaehlt als erfolgreicher Start - sonst kaeme man nie heraus.
            crashes.noteRendered()
            return
        }

        val store = ConfigStore.get(this)
        val apps = AppRepository.get(this)

        if (WizardSteps.showOnLaunch(store.current.wizardDone)) {
            startActivity(Intent(this, WizardActivity::class.java))
        }

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val screenId = currentScreen.value ?: config.homeScreenId
            var editMode by editModeRequest
            var popupLabel by remember { mutableStateOf<String?>(null) }
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
                when {
                    contactChoice.value != null -> contactChoice.value = null
                    popupLabel != null -> popupLabel = null
                    editMode -> editMode = false
                    screenId != config.homeScreenId -> currentScreen.value = null
                }
            }

            // Wir sind bis hierher gekommen: der Start gilt als geglueckt.
            LaunchedEffect(Unit) { crashes.noteRendered() }

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
                    if (editMode) {
                        EditModeBanner { editMode = false }
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
                        // Im Bearbeitungsmodus oeffnet schon der kurze Tipp den Editor. Ein
                        // langer Druck bleibt dann keine Voraussetzung - wer ihn nicht schafft,
                        // koennte seine Kacheln sonst nie aendern.
                        onActivate = { cell ->
                            if (editMode) {
                                context.startActivity(
                                    TileEditorActivity.intent(context, screen.id, cell.x, cell.y),
                                )
                            } else {
                                activate(cell, apps) { currentScreen.value = it }
                            }
                        },
                        onEdit = { x, y ->
                            LongPress.decide(config.behaviour.accessibility, editMode).forEach { action ->
                                when (action) {
                                    LongPressAction.EDIT -> context.startActivity(
                                        TileEditorActivity.intent(context, screen.id, x, y),
                                    )
                                    LongPressAction.SPEAK -> Speaker.say(
                                        context,
                                        labelAt(config, screen.id, x, y, apps),
                                    )
                                    LongPressAction.POPUP -> popupLabel =
                                        labelAt(config, screen.id, x, y, apps)
                                    LongPressAction.NOTHING -> Unit
                                }
                            }
                        },
                    )
                }

                val label = popupLabel
                if (label != null) {
                    LabelPopup(label) { popupLabel = null }
                }

                val asking = contactChoice.value
                if (asking != null) {
                    ContactChoice(
                        name = asking.name,
                        onCall = {
                            contactChoice.value = null
                            Intents.call(this@MainActivity, asking.number)
                        },
                        onSms = {
                            contactChoice.value = null
                            Intents.sms(this@MainActivity, asking.number)
                        },
                        onDismiss = { contactChoice.value = null },
                    )
                }
            }
        }
    }

    /** Was auf der Kachel steht - fuer Vorlesen und Popup dieselbe Quelle wie fuer die Anzeige. */
    private fun labelAt(
        config: org.biglau.data.LauncherConfig,
        screenId: String,
        x: Int,
        y: Int,
        apps: AppRepository,
    ): String {
        val button = config.screenById(screenId)?.cellAt(x, y)?.button ?: return getString(R.string.empty_tile)
        button.label?.let { return it }
        return when (val action = button.action) {
            is ButtonAction.App -> apps.labelFor(action.packageName, action.activityName) ?: action.packageName
            is ButtonAction.Contact -> action.name
            is ButtonAction.Shortcut -> action.label
            is ButtonAction.Widget -> action.label
            is ButtonAction.GoToScreen -> config.screenById(action.screenId)?.name ?: getString(R.string.next_screen)
            is ButtonAction.Action -> getString(action.builtin.labelRes())
            ButtonAction.None -> getString(R.string.empty_tile)
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
                // Nicht ACTION_DIAL: das reichte die Frage nur ans System weiter, und das
                // fragte etwas ganz anderes - naemlich welche App den Wahlvorgang uebernimmt.
                // "Jedes Mal fragen" verspricht, dass BigLau fragt: anrufen oder schreiben.
                ContactMode.ASK -> contactChoice.value = action
            }

            is ButtonAction.GoToScreen -> goToScreen(action.screenId)

            is ButtonAction.Action -> when (action.builtin) {
                Builtin.DIALER -> startActivity(Intent(this, DialerActivity::class.java))
                Builtin.MISSED_CALLS -> startActivity(
                    Intent(this, DialerActivity::class.java)
                        .putExtra(DialerActivity.EXTRA_MISSED, true),
                )
                Builtin.MESSAGES -> startActivity(Intent(this, SmsActivity::class.java))
                Builtin.CONTACTS -> startActivity(Intent(this, ContactsActivity::class.java))
                Builtin.CAMERA -> Intents.openCamera(this)
                Builtin.CLOCK -> Intents.openClock(this)
                Builtin.BATTERY -> Unit
                Builtin.FLASHLIGHT -> ToggleActions.run(this, ToggleKind.FLASHLIGHT)
                Builtin.WIFI -> ToggleActions.run(this, ToggleKind.WIFI)
                Builtin.BLUETOOTH -> ToggleActions.run(this, ToggleKind.BLUETOOTH)
                Builtin.AIRPLANE -> ToggleActions.run(this, ToggleKind.AIRPLANE)
                Builtin.RINGER -> ToggleActions.run(this, ToggleKind.RINGER)
                Builtin.SOS -> startActivity(Intent(this, SosActivity::class.java))
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

/** Sichtbarer Hinweis, dass gerade bearbeitet wird - sonst wundert man sich ueber die Tipps. */
@Composable
private fun EditModeBanner(onLeave: () -> Unit) {
    val palette = LocalBigPalette.current
    Text(
        text = stringResource(R.string.edit_mode_banner),
        color = palette.surfaceAccent.ink,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surfaceAccent.fill)
            .clickable { onLeave() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/** Die Beschriftung gross ueber dem ganzen Bildschirm - ein Tipp schliesst sie wieder. */
@Composable
private fun LabelPopup(label: String, onDismiss: () -> Unit) {
    val palette = LocalBigPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = label,
                color = palette.onBackground,
                fontSize = org.biglau.ui.dpSp(44f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.tap_to_close),
                color = palette.onBackground.copy(alpha = 0.7f),
                fontSize = org.biglau.ui.dpSp(16f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * "Anrufen oder schreiben?" - die Frage, die eine Kontaktkachel im Modus "Jedes Mal fragen"
 * stellt.
 *
 * Bewusst zwei ganze Zeilen und kein System-Dialog: der Systemdialog fragt etwas anderes
 * (welche App das ueberhaupt macht), seine Knoepfe sind klein, und er sieht auf jedem
 * Telefon anders aus. Ein Tipp daneben schliesst, damit die Frage kein Riegel ist.
 */
@Composable
private fun ContactChoice(
    name: String,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalBigPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = name,
                color = palette.onBackground,
                fontSize = org.biglau.ui.dpSp(30f),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            BigRow(
                label = stringResource(R.string.dialer_call),
                icon = Icons.Filled.Call,
                surface = palette.surfaceAccent,
                onClick = onCall,
            )
            BigRow(
                label = stringResource(R.string.contacts_action_sms),
                icon = Icons.AutoMirrored.Filled.Message,
                onClick = onSms,
            )
            Text(
                text = stringResource(R.string.tap_to_close),
                color = palette.onBackground.copy(alpha = 0.7f),
                fontSize = org.biglau.ui.dpSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
