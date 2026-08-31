package org.biglau.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.R
import org.biglau.actions.Intents
import org.biglau.apps.AppRepository
import org.biglau.a11y.LongPress
import org.biglau.data.Accessibility
import org.biglau.data.ConfigStore
import org.biglau.wizard.WizardActivity
import org.biglau.data.ConfigTransfer
import org.biglau.data.LabelPosition
import org.biglau.data.Screen
import org.biglau.data.SosConfig
import org.biglau.toggles.SosCountdown
import org.biglau.toggles.SosMessage
import org.biglau.toggles.SosNumbers
import org.biglau.data.ThemeName
import org.biglau.notify.NotificationRepository
import org.biglau.security.Pin
import org.biglau.tiles.ScreenEdits
import org.biglau.ui.BigHeading
import org.biglau.ui.BigRow
import org.biglau.ui.PinGate
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette
import org.biglau.ui.theme.paletteFor
import org.biglau.ui.theme.LocalTextScale
import org.biglau.ui.theme.BigSurface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.icons.filled.Check

private enum class Page { GATE, MAIN, SCREENS, APPEARANCE, BEHAVIOUR, SECURITY, SET_PIN, DIAGNOSTICS, RENAME, HIDDEN_APPS, TRANSFER, SOS, ACCESSIBILITY }

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val bypassPin = intent.getBooleanExtra(EXTRA_BYPASS_PIN, false)
        val store = ConfigStore.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            val locked = config.security.pin != null && !bypassPin
            var page by remember { mutableStateOf(if (locked) Page.GATE else Page.MAIN) }
            var renaming by remember { mutableStateOf<Screen?>(null) }

            val exportFile = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json"),
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                val ok = runCatching {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(ConfigTransfer.export(store.current).toByteArray())
                    } != null
                }.getOrDefault(false)
                Toast.makeText(
                    this@SettingsActivity,
                    if (ok) R.string.transfer_exported else R.string.transfer_failed,
                    Toast.LENGTH_LONG,
                ).show()
            }

            val importFile = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                val text = runCatching {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                val loaded = text?.let { ConfigTransfer.import(it) }
                if (loaded == null) {
                    // Bewusst nichts anfassen: eine unbrauchbare Datei darf die bestehende
                    // Belegung nicht ersetzen.
                    Toast.makeText(this@SettingsActivity, R.string.transfer_bad_file, Toast.LENGTH_LONG).show()
                } else {
                    store.update { loaded }
                    Toast.makeText(this@SettingsActivity, R.string.transfer_imported, Toast.LENGTH_LONG).show()
                    page = Page.MAIN
                }
            }

            BigLauTheme(config.appearance.theme, config.appearance.textScale) {
                BackHandler(enabled = page != Page.MAIN && page != Page.GATE) { page = Page.MAIN }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(LocalBigPalette.current.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    when (page) {
                        Page.GATE -> PinGate(
                            title = stringResource(R.string.settings_locked),
                            explainer = stringResource(R.string.security_explainer),
                            wrongText = stringResource(R.string.security_wrong_pin),
                            confirmLabel = stringResource(R.string.editor_done),
                            onCheck = { entered -> Pin.verify(entered, config.security.pin) },
                            onAccept = { page = Page.MAIN },
                            acceptOnComplete = true,
                            onEmergencyExit = { page = Page.MAIN },
                        )

                        Page.MAIN -> MainList(
                            onScreens = { page = Page.SCREENS },
                            onAppearance = { page = Page.APPEARANCE },
                            onBehaviour = { page = Page.BEHAVIOUR },
                            onHiddenApps = { page = Page.HIDDEN_APPS },
                            onSecurity = { page = Page.SECURITY },
                            onAccessibility = { page = Page.ACCESSIBILITY },
                            onEditTiles = {
                                startActivity(
                                    Intent(this@SettingsActivity, org.biglau.MainActivity::class.java)
                                        .putExtra(org.biglau.MainActivity.EXTRA_EDIT_MODE, true)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                                finish()
                            },
                            onSos = { page = Page.SOS },
                            onTransfer = { page = Page.TRANSFER },
                            onWizard = {
                                // Bewusst ohne wizardDone zurueckzusetzen: wer den Assistenten
                                // abbricht, stuende sonst bei jedem Start wieder darin - der
                                // eigene Startbildschirm waere hinter einer Frage verschwunden,
                                // die er gar nicht beantworten wollte.
                                startActivity(Intent(this@SettingsActivity, WizardActivity::class.java))
                            },
                            onDiagnostics = { page = Page.DIAGNOSTICS },
                            onHomeApp = { Intents.chooseHomeApp(this@SettingsActivity) },
                            onDialerApp = { Intents.chooseDialerApp(this@SettingsActivity) },
                            onDone = { finish() },
                        )

                        Page.SCREENS -> ScreenList(
                            screens = config.screens,
                            homeId = config.homeScreenId,
                            unreachable = ScreenEdits.unreachable(config),
                            onRename = { renaming = it; page = Page.RENAME },
                            onDelete = { store.update { current -> ScreenEdits.delete(current, it.id) } },
                            onMakeHome = { target ->
                                store.update { current -> current.copy(homeScreenId = target.id) }
                            },
                        )

                        Page.RENAME -> ScreenPanel(
                            // Immer die frische Fassung aus der Konfiguration: nach einem
                            // Rasterwechsel zeigte die gemerkte sonst weiter das alte Raster.
                            screen = renaming?.id?.let { id -> config.screens.firstOrNull { it.id == id } },
                            onDone = { name ->
                                val target = renaming
                                if (target != null) {
                                    store.update { ScreenEdits.rename(it, target.id, name) }
                                }
                                page = Page.SCREENS
                            },
                            onGrid = { cols, rows ->
                                val target = renaming
                                if (target != null) {
                                    store.update { ScreenEdits.setGrid(it, target.id, cols, rows) }
                                }
                            },
                        )

                        Page.APPEARANCE -> AppearanceList(
                            themeName = config.appearance.theme,
                            textScale = config.appearance.textScale,
                            labelPosition = config.appearance.labelPosition,
                            showIcons = config.appearance.showIcons,
                            onTheme = { next ->
                                store.update { it.copy(appearance = it.appearance.copy(theme = next)) }
                            },
                            onTextScale = { next ->
                                store.update { it.copy(appearance = it.appearance.copy(textScale = next)) }
                            },
                            onLabelPosition = { next ->
                                store.update { it.copy(appearance = it.appearance.copy(labelPosition = next)) }
                            },
                            showHeader = config.appearance.showHeader,
                            onToggleHeader = {
                                store.update {
                                    it.copy(appearance = it.appearance.copy(showHeader = !it.appearance.showHeader))
                                }
                            },
                            clockShowsDate = config.appearance.clockShowsDate,
                            onToggleClockDate = {
                                store.update {
                                    it.copy(appearance = it.appearance.copy(clockShowsDate = !it.appearance.clockShowsDate))
                                }
                            },
                            onToggleIcons = {
                                store.update {
                                    it.copy(appearance = it.appearance.copy(showIcons = !it.appearance.showIcons))
                                }
                            },
                        )

                        Page.BEHAVIOUR -> BehaviourList(
                            blinkOn = config.behaviour.blinkOnNotification,
                            accessGranted = NotificationRepository.isEnabled(this@SettingsActivity),
                            onToggleBlink = {
                                store.update {
                                    it.copy(
                                        behaviour = it.behaviour.copy(
                                            blinkOnNotification = !it.behaviour.blinkOnNotification,
                                        ),
                                    )
                                }
                            },
                            onGrantAccess = { Intents.notificationListenerSettings(this@SettingsActivity) },
                        )

                        Page.HIDDEN_APPS -> HiddenAppsList(
                            hidden = config.apps.hidden,
                            labelFor = { key ->
                                val parts = key.split("/")
                                if (parts.size == 2) {
                                    AppRepository.get(this@SettingsActivity).labelFor(parts[0], parts[1]) ?: parts[0]
                                } else {
                                    key
                                }
                            },
                            onShowAgain = { key ->
                                store.update { it.copy(apps = it.apps.copy(hidden = it.apps.hidden - key)) }
                            },
                        )

                        Page.SECURITY -> SecurityList(
                            hasPin = config.security.pin != null,
                            onSetPin = { page = Page.SET_PIN },
                            onRemovePin = {
                                store.update { it.copy(security = it.security.copy(pin = null)) }
                            },
                        )

                        Page.SET_PIN -> PinGate(
                            title = stringResource(R.string.security_new_pin),
                            explainer = null,
                            wrongText = stringResource(R.string.security_pin_rules),
                            confirmLabel = stringResource(R.string.editor_done),
                            onCheck = { entered -> Pin.isValid(entered) },
                            onAccept = { entered ->
                                Pin.hash(entered)?.let { hashed ->
                                    store.update { it.copy(security = it.security.copy(pin = hashed)) }
                                }
                                page = Page.SECURITY
                            },
                            acceptOnComplete = false,
                        )

                        Page.ACCESSIBILITY -> AccessibilityList(
                            config = config.behaviour.accessibility,
                            onToggleSpeak = {
                                store.update {
                                    val a = it.behaviour.accessibility
                                    it.copy(
                                        behaviour = it.behaviour.copy(
                                            accessibility = a.copy(speakOnLongPress = !a.speakOnLongPress),
                                        ),
                                    )
                                }
                            },
                            onToggleScroll = {
                                store.update {
                                    val a = it.behaviour.accessibility
                                    it.copy(
                                        behaviour = it.behaviour.copy(
                                            accessibility = a.copy(scrollButtons = !a.scrollButtons),
                                        ),
                                    )
                                }
                            },
                            onTogglePopup = {
                                store.update {
                                    val a = it.behaviour.accessibility
                                    it.copy(
                                        behaviour = it.behaviour.copy(
                                            accessibility = a.copy(popupOnLongPress = !a.popupOnLongPress),
                                        ),
                                    )
                                }
                            },
                        )

                        Page.SOS -> SosSettings(
                            config = config.sos,
                            onNumbers = { text ->
                                store.update { it.copy(sos = it.sos.copy(numbers = SosNumbers.parse(text))) }
                            },
                            onMessage = { text ->
                                store.update { it.copy(sos = it.sos.copy(message = text.trim())) }
                            },
                            onCountdown = { seconds ->
                                store.update {
                                    it.copy(sos = it.sos.copy(countdownSeconds = SosCountdown.clamp(seconds)))
                                }
                            },
                            onToggleLocation = {
                                store.update {
                                    it.copy(sos = it.sos.copy(sendLocation = !it.sos.sendLocation))
                                }
                            },
                        )

                        Page.TRANSFER -> TransferList(
                            onExport = { exportFile.launch(ConfigTransfer.suggestedFileName(System.currentTimeMillis())) },
                            // Bewusst ohne Typfilter: eine Sicherung vom anderen Telefon kann mit
                            // beliebigem MIME-Typ ankommen, und gefilterte Eintraege sind im
                            // Systemdialog zwar sichtbar, aber nicht antippbar - was aussieht,
                            // als waere die App kaputt.
                            onImport = { importFile.launch(arrayOf("*/*")) },
                        )

                        Page.DIAGNOSTICS -> DiagnosticsList(this@SettingsActivity)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_BYPASS_PIN = "bypassPin"
    }
}

@Composable
private fun MainList(
    onScreens: () -> Unit,
    onAppearance: () -> Unit,
    onBehaviour: () -> Unit,
    onHiddenApps: () -> Unit,
    onSecurity: () -> Unit,
    onAccessibility: () -> Unit,
    onEditTiles: () -> Unit,
    onSos: () -> Unit,
    onTransfer: () -> Unit,
    onWizard: () -> Unit,
    onDiagnostics: () -> Unit,
    onHomeApp: () -> Unit,
    onDialerApp: () -> Unit,
    onDone: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings)) }
        item { BigRow(stringResource(R.string.settings_screens), icon = Icons.Filled.Home, onClick = onScreens) }
        item { BigRow(stringResource(R.string.settings_appearance), icon = Icons.Filled.Palette, onClick = onAppearance) }
        item { BigRow(stringResource(R.string.settings_behaviour), icon = Icons.Filled.NotificationsActive, onClick = onBehaviour) }
        item { BigRow(stringResource(R.string.settings_hidden_apps), icon = Icons.Filled.VisibilityOff, onClick = onHiddenApps) }
        item { BigRow(stringResource(R.string.settings_security), icon = Icons.Filled.Lock, onClick = onSecurity) }
        item { BigRow(stringResource(R.string.set_as_home), icon = Icons.Filled.Home, onClick = onHomeApp) }
        item {
            BigRow(
                label = stringResource(R.string.set_as_dialer),
                secondary = stringResource(R.string.set_as_dialer_hint),
                icon = Icons.Filled.Call,
                onClick = onDialerApp,
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.settings_edit_tiles),
                secondary = stringResource(R.string.settings_edit_tiles_hint),
                icon = Icons.Filled.Edit,
                onClick = onEditTiles,
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.settings_accessibility),
                icon = Icons.Filled.Accessibility,
                onClick = onAccessibility,
            )
        }
        item { BigRow(stringResource(R.string.sos), icon = Icons.Filled.Warning, onClick = onSos) }
        item { BigRow(stringResource(R.string.settings_transfer), icon = Icons.Filled.Save, onClick = onTransfer) }
        item {
            BigRow(
                label = stringResource(R.string.wizard_again),
                icon = Icons.Filled.Replay,
                onClick = onWizard,
            )
        }
        item { BigRow(stringResource(R.string.settings_diagnostics), icon = Icons.Filled.Info, onClick = onDiagnostics) }
        item {
            BigRow(
                label = stringResource(R.string.editor_done),
                surface = palette.surfaceAccent,
                onClick = onDone,
            )
        }
    }
}

@Composable
private fun ScreenList(
    screens: List<Screen>,
    homeId: String,
    unreachable: List<Screen>,
    onRename: (Screen) -> Unit,
    onDelete: (Screen) -> Unit,
    onMakeHome: (Screen) -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_screens)) }
        // Ein Screen, zu dem keine Kachel fuehrt, ist eingerichtet und unerreichbar. Ohne
        // diesen Hinweis merkt man das nie - man sucht ihn und findet ihn nicht.
        if (unreachable.isNotEmpty()) {
            item {
                Text(
                    text = pluralStringResource(
                        R.plurals.screens_unreachable,
                        unreachable.size,
                        unreachable.joinToString(", ") { it.name },
                    ),
                    color = palette.danger,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
        items(screens, key = { it.id }) { screen ->
            val isHome = screen.id == homeId
            BigRow(
                label = screen.name,
                secondary = stringResource(
                    if (isHome) R.string.screen_is_home else R.string.screen_grid,
                    screen.cols,
                    screen.rows,
                ),
                secondaryMaxLines = 1,
                icon = Icons.Filled.Edit,
                surface = if (isHome) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onRename(screen) },
            )
            if (!isHome) {
                // Der Name gehoert in den Knopf. Bei drei Screens stehen hier drei Paare
                // untereinander, und "diesen" waere dann nur noch aus der Reihenfolge zu
                // erraten - beim Loeschen kann man sich das nicht leisten.
                BigRow(
                    label = stringResource(R.string.screen_make_home, screen.name),
                    icon = Icons.Filled.Home,
                    onClick = { onMakeHome(screen) },
                )
                BigRow(
                    label = stringResource(R.string.screen_delete, screen.name),
                    icon = Icons.Filled.Delete,
                    surface = palette.surfaceDanger,
                    onClick = { onDelete(screen) },
                )
            }
        }
        // Angelegt werden Screens im Editor, nicht hier - so haengt an jedem neuen Screen
        // von Anfang an eine Kachel, die hinfuehrt. Wer hier danach sucht, soll das lesen.
        item {
            Text(
                text = stringResource(R.string.screens_where_new),
                color = palette.onBackground,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
            )
        }
    }
}

/**
 * Name und Raster eines Screens.
 *
 * Das Raster steht bewusst neben dem Namen und nicht in einer eigenen Ecke: beides gehoert
 * demselben Screen, und wer hier ist, will ihn einrichten. Verkleinern kostet Kacheln, also
 * steht vorher rot daneben, wie viele - und erst der zweite Tipp fuehrt es aus.
 */
@Composable
private fun ScreenPanel(
    screen: Screen?,
    onDone: (String) -> Unit,
    onGrid: (Int, Int) -> Unit,
) {
    if (screen == null) return
    var text by remember(screen.id) { mutableStateOf(screen.name) }
    var confirming by remember(screen.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { BigHeading(stringResource(R.string.screen_edit)) }
        item {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                // Dieselbe Falle wie beim Kachelnamen: die Tastatur verdeckt "Fertig"
                // vollstaendig, also uebernimmt ihre eigene Haken-Taste.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone(text) }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { BigHeading(stringResource(R.string.screen_grid_heading)) }
        items(ScreenEdits.GRID_PRESETS) { (cols, rows) ->
            val current = cols == screen.cols && rows == screen.rows
            val loses = ScreenEdits.dropped(screen, cols, rows).size
            val armed = confirming == (cols to rows)
            BigRow(
                label = stringResource(R.string.screen_grid, cols, rows),
                secondary = when {
                    current -> stringResource(R.string.screen_grid_current)
                    armed -> stringResource(R.string.screen_grid_confirm)
                    loses > 0 -> pluralStringResource(R.plurals.screen_grid_loses, loses, loses)
                    else -> null
                },
                surface = when {
                    current -> palette.surfaceAccent
                    armed -> palette.surfaceDanger
                    else -> palette.surfaceDefault
                },
                onClick = {
                    when {
                        current -> Unit
                        // Ein anderes Raster anzutippen nimmt die scharfe Warnung wieder
                        // zurueck - sonst bliebe irgendwo eine rote Zeile scharf stehen,
                        // die beim naechsten Tipp ungefragt Kacheln kostet.
                        loses == 0 -> { onGrid(cols, rows); confirming = null }
                        armed -> { onGrid(cols, rows); confirming = null }
                        else -> confirming = cols to rows
                    }
                },
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.editor_done),
                surface = palette.surfaceAccent,
                onClick = { onDone(text) },
            )
        }
    }
}

@Composable
private fun AppearanceList(
    themeName: ThemeName,
    textScale: Float,
    labelPosition: LabelPosition,
    showIcons: Boolean,
    showHeader: Boolean,
    clockShowsDate: Boolean,
    onToggleHeader: () -> Unit,
    onToggleClockDate: () -> Unit,
    onTheme: (ThemeName) -> Unit,
    onTextScale: (Float) -> Unit,
    onLabelPosition: (LabelPosition) -> Unit,
    onToggleIcons: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_appearance)) }
        // Jede Zeile ist in ihrem eigenen Thema gemalt. Dreimal dasselbe Paletten-Symbol
        // sagte nichts; die Farben selbst sagen alles. Die Auswahl traegt deshalb ein
        // Haekchen statt einer Akzentflaeche - die Flaeche gehoert hier dem Thema.
        items(ThemeName.entries.toList()) { entry ->
            val own = paletteFor(entry)
            val chosen = entry == themeName
            BigRow(
                label = stringResource(themeLabel(entry)),
                icon = if (chosen) Icons.Filled.Check else null,
                surface = BigSurface(own.emptyTile, own.onBackground),
                borderColor = if (chosen) palette.accent else null,
                onClick = { onTheme(entry) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_text_size)) }
        // Jede Zeile in ihrer eigenen Groesse: man sieht, was man waehlt, statt es zu lesen.
        items(listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)) { scale ->
            CompositionLocalProvider(LocalTextScale provides scale) {
                BigRow(
                    label = "${(scale * 100).toInt()} %",
                    icon = if (scale == textScale) Icons.Filled.Check else null,
                    surface = if (scale == textScale) palette.surfaceAccent else palette.surfaceDefault,
                    onClick = { onTextScale(scale) },
                )
            }
        }
        item { BigHeading(stringResource(R.string.appearance_label_position)) }
        items(LabelPosition.entries.toList()) { position ->
            BigRow(
                label = stringResource(labelPositionLabel(position)),
                surface = if (position == labelPosition) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onLabelPosition(position) },
            )
        }
        item {
            BigRow(
                label = stringResource(if (showIcons) R.string.appearance_icons_on else R.string.appearance_icons_off),
                onClick = onToggleIcons,
            )
        }
        item { BigHeading(stringResource(R.string.appearance_header)) }
        item {
            BigRow(
                label = stringResource(if (showHeader) R.string.header_on else R.string.header_off),
                secondary = stringResource(R.string.header_explainer),
                icon = Icons.Filled.Schedule,
                surface = if (showHeader) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleHeader,
            )
        }
        item {
            BigRow(
                label = stringResource(if (clockShowsDate) R.string.header_date_on else R.string.header_date_off),
                onClick = onToggleClockDate,
            )
        }
    }
}

/**
 * Ausgeblendete Apps wieder einblenden. Ohne diese Seite waere das Ausblenden eine
 * Einbahnstrasse - eine Aktion ohne Rueckweg ist ein Fehler, auch wenn sie tut, was sie soll.
 */
/**
 * Sicherung und Wiederherstellung. Gedacht fuer den Wechsel auf ein anderes Telefon,
 * deshalb ueber den System-Dateidialog: die Datei soll dort liegen, wo der Nutzer sie
 * auch wiederfindet, nicht in einem App-Verzeichnis, das beim Deinstallieren verschwindet.
 */
/**
 * Notruf einrichten. Die Nummern stehen in einer Zeile, weil das auf drei Zoll schneller
 * geht als eine Liste mit Plus-Knopf - und weil [SosNumbers] beim Einlesen streng aussortiert,
 * kostet die Bequemlichkeit nichts.
 */
/**
 * Barrierefreiheit. Beide Schalter nehmen dem Langdruck den Editor weg - deshalb steht
 * darunter, wo man ihn dann findet. Eine Einstellung, die einen Weg schliesst, muss den
 * neuen Weg nennen.
 */
@Composable
private fun AccessibilityList(
    config: Accessibility,
    onToggleSpeak: () -> Unit,
    onTogglePopup: () -> Unit,
    onToggleScroll: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_accessibility)) }
        item {
            BigRow(
                label = stringResource(
                    if (config.speakOnLongPress) R.string.a11y_speak_on else R.string.a11y_speak_off,
                ),
                secondary = stringResource(R.string.a11y_speak_hint),
                surface = if (config.speakOnLongPress) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleSpeak,
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (config.popupOnLongPress) R.string.a11y_popup_on else R.string.a11y_popup_off,
                ),
                secondary = stringResource(R.string.a11y_popup_hint),
                surface = if (config.popupOnLongPress) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onTogglePopup,
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (config.scrollButtons) R.string.a11y_scroll_on else R.string.a11y_scroll_off,
                ),
                secondary = stringResource(R.string.a11y_scroll_hint),
                surface = if (config.scrollButtons) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleScroll,
            )
        }
        if (LongPress.needsEditModeEntry(config)) {
            item {
                Text(
                    text = stringResource(R.string.a11y_editor_moved),
                    color = palette.danger,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SosSettings(
    config: SosConfig,
    onNumbers: (String) -> Unit,
    onMessage: (String) -> Unit,
    onCountdown: (Int) -> Unit,
    onToggleLocation: () -> Unit,
) {
    val palette = LocalBigPalette.current
    val defaultMessage = stringResource(R.string.sos_message_default)
    var numbersText by remember(config.numbers) { mutableStateOf(SosNumbers.format(config.numbers)) }
    // Beim ersten Oeffnen steht der Vorgabetext schon im Feld. So sieht der Nutzer, was
    // verschickt wuerde, statt vor einem leeren Kasten zu raten.
    var messageText by remember(config.message) {
        mutableStateOf(config.message.ifBlank { defaultMessage })
    }
    val rejected = remember(numbersText) { SosNumbers.rejected(numbersText) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { BigHeading(stringResource(R.string.sos)) }
        item {
            Text(
                text = stringResource(R.string.sos_explainer),
                color = palette.onBackground,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }

        item { BigHeading(stringResource(R.string.sos_numbers)) }
        item {
            OutlinedTextField(
                value = numbersText,
                onValueChange = { numbersText = it },
                singleLine = false,
                textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                // Der Hinweis gehoert ans Feld, nicht an den Knopf: am Knopf stand er in
                // einer Zeile, die abgeschnitten wurde, und ein leerer Kasten sagt nichts.
                placeholder = { Text(stringResource(R.string.sos_numbers_placeholder), fontSize = 17.sp) },
                supportingText = {
                    Text(stringResource(R.string.sos_numbers_hint, SosNumbers.MAX), fontSize = 15.sp)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (rejected.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.sos_numbers_rejected, rejected.joinToString(", ")),
                    color = palette.danger,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        item {
            BigRow(
                label = stringResource(R.string.sos_numbers_save),
                surface = palette.surfaceAccent,
                onClick = { onNumbers(numbersText) },
            )
        }

        item { BigHeading(stringResource(R.string.sos_message)) }
        item {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                textStyle = TextStyle(fontSize = 18.sp),
                placeholder = { Text(defaultMessage, fontSize = 17.sp) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            val preview = SosMessage.compose(messageText, 48.20849, 16.37208, defaultMessage)
            BigRow(
                label = stringResource(R.string.sos_message_save),
                // Vorschau mit Beispielkoordinaten: der Nutzer soll sehen, was ankommt,
                // und wie viele SMS es kostet.
                secondary = stringResource(R.string.sos_message_parts, SosMessage.partsNeeded(preview)),
                surface = palette.surfaceAccent,
                onClick = { onMessage(messageText) },
            )
        }

        item { BigHeading(stringResource(R.string.sos_countdown)) }
        items(listOf(0, 3, 5, 8, 10)) { seconds ->
            BigRow(
                label = if (seconds == 0) {
                    stringResource(R.string.sos_countdown_none)
                } else {
                    stringResource(R.string.sos_countdown_seconds, seconds)
                },
                surface = if (seconds == config.countdownSeconds) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onCountdown(seconds) },
            )
        }

        item {
            BigRow(
                label = stringResource(
                    if (config.sendLocation) R.string.sos_location_on else R.string.sos_location_off,
                ),
                surface = if (config.sendLocation) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleLocation,
            )
        }
    }
}

@Composable
private fun TransferList(onExport: () -> Unit, onImport: () -> Unit) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_transfer)) }
        item {
            Text(
                text = stringResource(R.string.transfer_explainer),
                color = palette.onBackground,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.transfer_export),
                secondary = stringResource(R.string.transfer_export_hint),
                icon = Icons.Filled.Save,
                onClick = onExport,
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.transfer_import),
                secondary = stringResource(R.string.transfer_import_hint),
                icon = Icons.Filled.FolderOpen,
                onClick = onImport,
            )
        }
    }
}

@Composable
private fun HiddenAppsList(
    hidden: Set<String>,
    labelFor: (String) -> String,
    onShowAgain: (String) -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_hidden_apps)) }
        if (hidden.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.hidden_apps_none),
                    color = palette.onBackground,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
        items(hidden.toList().sortedBy { labelFor(it).lowercase() }) { key ->
            BigRow(
                label = labelFor(key),
                secondary = stringResource(R.string.hidden_apps_show_again),
                icon = Icons.Filled.Visibility,
                onClick = { onShowAgain(key) },
            )
        }
    }
}

@Composable
private fun BehaviourList(
    blinkOn: Boolean,
    accessGranted: Boolean,
    onToggleBlink: () -> Unit,
    onGrantAccess: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_behaviour)) }
        item {
            Text(
                text = stringResource(R.string.blink_explainer),
                color = palette.onBackground,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
        item {
            BigRow(
                label = stringResource(if (blinkOn) R.string.blink_on else R.string.blink_off),
                icon = Icons.Filled.NotificationsActive,
                surface = if (blinkOn) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleBlink,
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (accessGranted) R.string.blink_access_granted else R.string.blink_access_missing,
                ),
                icon = Icons.Filled.Lock,
                surface = if (accessGranted) palette.surfaceDefault else palette.surfaceDanger,
                onClick = onGrantAccess,
            )
        }
    }
}

@Composable
private fun SecurityList(hasPin: Boolean, onSetPin: () -> Unit, onRemovePin: () -> Unit) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_security)) }
        item {
            Text(
                text = stringResource(R.string.security_explainer),
                color = palette.onBackground,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
        item {
            BigRow(
                label = stringResource(if (hasPin) R.string.security_change_pin else R.string.security_set_pin),
                icon = Icons.Filled.Lock,
                onClick = onSetPin,
            )
        }
        if (hasPin) {
            item {
                BigRow(
                    label = stringResource(R.string.security_remove_pin),
                    icon = Icons.Filled.Delete,
                    surface = palette.surfaceDanger,
                    onClick = onRemovePin,
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsList(activity: ComponentActivity) {
    val lines = remember { Diagnostics.collect(activity) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_diagnostics)) }
        items(lines) { line ->
            BigRow(label = line.first, secondary = line.second, onClick = {})
        }
    }
}

private fun themeLabel(theme: ThemeName) = when (theme) {
    ThemeName.DARK -> R.string.theme_dark
    ThemeName.HIGH_CONTRAST -> R.string.theme_contrast
    ThemeName.LIGHT -> R.string.theme_light
}

private fun labelPositionLabel(position: LabelPosition) = when (position) {
    LabelPosition.BOTTOM_LEFT -> R.string.label_bottom_left
    LabelPosition.BOTTOM_CENTER -> R.string.label_bottom_center
    LabelPosition.TOP_LEFT -> R.string.label_top_left
    LabelPosition.HIDDEN -> R.string.label_hidden
}
