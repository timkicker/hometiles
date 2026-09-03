package org.biglau.settings

import android.content.Intent
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.ui.bigSp
import org.biglau.ui.ICON_PERCENTS
import org.biglau.ui.LABEL_SCALES
import org.biglau.ui.BigLauActivity
import org.biglau.R
import org.biglau.core.ui.R as UiR
import org.biglau.actions.Intents
import org.biglau.ui.PermissionState
import org.biglau.apps.AppDrawer
import org.biglau.apps.AppRepository
import org.biglau.a11y.LongPress
import org.biglau.data.Accessibility
import org.biglau.data.ConfigStore
import org.biglau.ui.GridLooks
import org.biglau.data.Appearance
import org.biglau.ui.StatusVisibility
import org.biglau.ui.ClockFormat
import org.biglau.ui.Notice
import org.biglau.ui.Orientation
import org.biglau.wizard.WizardActivity
import org.biglau.data.ConfigTransfer
import org.biglau.data.LabelPosition
import org.biglau.data.Screen
import org.biglau.data.SosConfig
import org.biglau.toggles.SosActivity
import org.biglau.toggles.SosAlarm
import org.biglau.toggles.SosCountdown
import org.biglau.actions.SosMessage
import org.biglau.toggles.SosNumbers
import org.biglau.data.ClockDisplay
import org.biglau.data.FontChoice
import org.biglau.data.ContactsConfig
import androidx.compose.material.icons.filled.Person
import org.biglau.phone.CallBlocking
import org.biglau.phone.DialerRole
import org.biglau.sms.ConversationText
import org.biglau.sms.MessageReminderReceiver
import org.biglau.sms.SmsNotifications
import org.biglau.sms.SmsReminder
import org.biglau.sms.SmsFilter
import org.biglau.sms.SmsRepository
import org.biglau.phone.CallDirection
import org.biglau.data.ScreenOrientation
import org.biglau.data.Security
import org.biglau.data.HapticStrength
import org.biglau.data.IconVisibility
import org.biglau.data.Language
import org.biglau.ui.Haptics
import org.biglau.data.PressMode
import org.biglau.data.AudioRoute
import org.biglau.data.CallGrouping
import org.biglau.data.CallerPhoto
import org.biglau.data.ThemeName
import org.biglau.notify.NotificationRepository
import org.biglau.security.Pin
import org.biglau.tiles.FolderEdits
import org.biglau.tiles.ScreenEdits
import org.biglau.apps.AppLock
import org.biglau.apps.LaunchableApp
import org.biglau.ui.BigSearchField
import org.biglau.search.TextSearch
import org.biglau.ui.BigHeading
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.ContentCopy
import org.biglau.tiles.GridLimits
import androidx.compose.ui.platform.LocalConfiguration
import org.biglau.tiles.ScreenCopy
import org.biglau.tiles.ScreenOrder
import org.biglau.tiles.SwipeChain
import org.biglau.ui.BigIconButton
import org.biglau.ui.BigRow
import org.biglau.ui.SettingsLink
import org.biglau.ui.PinGate
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.familyFor
import org.biglau.data.Background
import org.biglau.ui.theme.ScreenBackground
import androidx.compose.ui.graphics.Color
import org.biglau.ui.theme.LocalBigPalette
import org.biglau.widgets.WidgetHostController
import org.biglau.ui.theme.paletteFor
import org.biglau.ui.theme.LocalTextScale
import org.biglau.ui.theme.BigSurface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.icons.filled.Check

internal enum class Page { GATE, MAIN, MESSAGES, SCREENS, APPEARANCE, BEHAVIOUR, SECURITY, SET_PIN, DIAGNOSTICS, RENAME, HIDDEN_APPS, TRANSFER, SOS, ACCESSIBILITY, RESET , SWIPE_ORDER, ALLOWED_APPS, CONTACTS, CALL_TYPES}

class SettingsActivity : BigLauActivity() {

    /** Die zuletzt angeforderte Unterseite. Siehe [onNewIntent]. */
    private val zielAnfrage = mutableStateOf<Page?>(null)

    private fun leseZiel(intent: Intent?): Page? =
        SettingsDeepLink.ziel(intent?.getStringExtra(EXTRA_PAGE))

    /**
     * Kommt die Anfrage, waehrend die Einstellungen schon offen sind, aendert Android
     * `intent` nicht von selbst. Ohne das hier bliebe die Seite von vorhin stehen.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        leseZiel(intent)?.let { zielAnfrage.value = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        zielAnfrage.value = leseZiel(intent)
        val store = ConfigStore.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            val locked = Pin.usable(config.security.pin)
            // rememberSaveable, damit der Sprachwechsel nicht an den Anfang zurueckwirft:
            // er baut die Activity neu auf, und wer gerade eine Sprache gewaehlt hat, will
            // sehen, dass das Haekchen umgesprungen ist - nicht die oberste Seite.
            // Eine Unterseite laesst sich von aussen ansteuern - die Anrufliste schickt
            // hierher, wenn jede Anrufart ausgeblendet ist. Die PIN kommt trotzdem zuerst;
            // ein Ziel im Intent darf kein Schloss aufmachen.
            //
            // Gelesen wird aus [zielAnfrage] und nicht direkt aus `intent`: die Activity
            // steht oft schon im Stapel, und dann bringt `startActivity` sie nur nach vorn,
            // ohne dass sich `intent` aendert. Wer die Einstellungen vorher offen hatte,
            // landete auf der Seite von damals - am Emulator nachgestellt: erst Diagnose
            // aufgerufen, dann die Anrufarten angefordert, und es blieb die Diagnose.
            val ziel = zielAnfrage.value
            var page by rememberSaveable { mutableStateOf(SettingsDeepLink.start(locked, ziel)) }
            // Die Probe des Notruf-Alarms. Sie laeuft nur, solange die Einstellungen offen
            // sind - ein Alarm, der weiterlaeuft, waere schlimmer als keiner.
            var alarmProbe by remember { mutableStateOf(false) }
            DisposableEffect(Unit) {
                onDispose { SosAlarm.stop(this@SettingsActivity) }
            }
            LaunchedEffect(ziel) {
                SettingsDeepLink.sprung(page, ziel)?.let { page = it }
            }
            var renaming by remember { mutableStateOf<Screen?>(null) }
            var switching by remember { mutableStateOf<Screen?>(null) }

            // Verdoppeln sagt hinterher, was es getan und was es ausgelassen hat. Ein
            // stiller Sprung auf einen fast gleichen Screen laesst einen raten, ob es
            // geklappt hat - und ob die Widgets nun weg sind oder nur unsichtbar.
            fun duplicate(screen: Screen) {
                val kopieName = getString(R.string.screen_copy_name, screen.name)
                when (val ergebnis = ScreenCopy.duplicate(store.current, screen.id, kopieName)) {
                    is ScreenCopy.Result.Done -> {
                        store.update { ergebnis.config }
                        val ausgelassen = ergebnis.skippedWidgets + ergebnis.skippedFolders
                        Notice.show(
                            this@SettingsActivity,
                            if (ausgelassen == 0) {
                                resources.getQuantityString(
                                    R.plurals.screen_copy_done,
                                    ergebnis.copied,
                                    kopieName,
                                    ergebnis.copied,
                                )
                            } else {
                                getString(
                                    R.plurals.screen_copy_done_partial,
                                    ergebnis.copied,
                                    kopieName,
                                    ergebnis.copied,
                                    ausgelassen,
                                )
                            },
                        )
                    }

                    ScreenCopy.Result.NoRoomForJumpTile ->
                        Notice.show(this@SettingsActivity, R.string.screen_copy_no_room)

                    ScreenCopy.Result.NoSuchScreen -> Unit
                }
            }

            // Der Notruf verspricht "mit Standort", pruefte das Recht dafuer - und niemand
            // hat es je erfragt. Die Nachricht ging still ohne Koordinaten hinaus, und
            // gemerkt haette man es erst in dem Fall, fuer den der Notruf da ist.
            // Gefragt wird hier beim Einrichten, nicht im Notfall: ein Systemdialog vor
            // dem Absenden waere genau die Sekunde, die dann fehlt.
            var locationGranted by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        this@SettingsActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            this@SettingsActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED,
                )
            }
            var locationDeniedOnce by remember { mutableStateOf(false) }
            var locationCanAskAgain by remember { mutableStateOf(true) }
            // Ein Rollendialog braucht einen Aufrufer - also ueber einen Launcher und nicht
            // ueber startActivity. Ohne das bricht er ab, bevor er zu sehen ist.
            val askDialerRole = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { }

            val askLocation = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { result ->
                locationGranted = result.values.any { it }
                if (!locationGranted) {
                    locationDeniedOnce = true
                    locationCanAskAgain =
                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            val exportFile = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json"),
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                val ok = runCatching {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(ConfigTransfer.export(store.current).toByteArray())
                    } != null
                }.getOrDefault(false)
                Notice.show(
                    this@SettingsActivity,
                    if (ok) R.string.transfer_exported else R.string.transfer_failed,
                )
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
                    Notice.show(this@SettingsActivity, R.string.transfer_bad_file)
                } else {
                    store.update { loaded }
                    // Eine Sicherung aus einer neueren Fassung enthaelt Felder, die diese
                    // hier nicht kennt; sie fallen beim Einlesen weg. Lieber gesagt als
                    // still verloren.
                    Notice.show(
                        this@SettingsActivity,
                        if (ConfigTransfer.isFromNewerVersion(text)) {
                            R.string.transfer_imported_older
                        } else {
                            R.string.transfer_imported
                        },
                    )
                    page = Page.MAIN
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
                            // Auf dem Schloss steht nur der Ausweg, nicht die Begruendung:
                            // der lange Text passte dort nicht in die drei Zeilen und wurde
                            // ausgerechnet an der Stelle abgeschnitten, an der der Ausweg
                            // stand ("Wenn Sie sie ..."). Am Bildschirm gesehen.
                            explainer = stringResource(R.string.security_forgot),
                            wrongText = stringResource(R.string.security_wrong_pin),
                            confirmLabel = stringResource(R.string.editor_done),
                            onCheck = { entered -> Pin.verify(entered, config.security.pin) },
                            onAccept = { page = ziel ?: Page.MAIN },
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
                            onReset = { page = Page.RESET },
                            onContacts = { page = Page.CONTACTS },
                            onCallTypes = { page = Page.CALL_TYPES },
                            onMessages = { page = Page.MESSAGES },
                            istStartbildschirm = Diagnostics.isDefaultHome(this@SettingsActivity),
                            istTelefonApp = DialerRole.held(this@SettingsActivity),
                            onHomeApp = {
                                val absicht = Intents.homeRoleIntent(this@SettingsActivity)
                                if (absicht != null) {
                                    askDialerRole.launch(absicht)
                                } else {
                                    Intents.chooseHomeApp(this@SettingsActivity)
                                }
                            },
                            onDialerApp = {
                                // Haelt BigLau die Rolle schon, fuehrt der Rollendialog
                                // nirgendwohin - er schliesst sich sofort wieder. Dann in
                                // die Systemeinstellungen, wo sie sich zurueckgeben laesst.
                                val absicht = if (DialerRole.held(this@SettingsActivity)) {
                                    null
                                } else {
                                    Intents.dialerRoleIntent(this@SettingsActivity)
                                }
                                if (absicht != null) {
                                    askDialerRole.launch(absicht)
                                } else {
                                    Intents.chooseDialerApp(this@SettingsActivity)
                                }
                            },
                            onDone = { finish() },
                        )

                        Page.SCREENS -> if (switching != null) {
                            val ziel = switching!!
                            NoSettingsWarning(
                                name = ziel.name,
                                canAddTile = ScreenEdits.withSettingsTile(config, ziel.id) != null,
                                onAddAndSwitch = {
                                    store.update { current ->
                                        val mitKachel = ScreenEdits.withSettingsTile(current, ziel.id)
                                            ?: return@update current
                                        mitKachel.copy(homeScreenId = ziel.id)
                                    }
                                    switching = null
                                },
                                onCancel = { switching = null },
                            )
                        } else ScreenList(
                            // Ordner gehoeren ihrer Kachel, nicht der Screen-Liste.
                            screens = FolderEdits.plainScreens(config),
                            homeId = config.homeScreenId,
                            unreachable = ScreenEdits.unreachable(config),
                            // Ordner stehen sonst nicht in dieser Liste ("Ordner gehoeren
                            // ihrer Kachel"). Einer ohne Kachel gehoert niemandem mehr -
                            // dann ist das hier die einzige Stelle, an der er noch
                            // auftauchen kann.
                            orphanedFolders = FolderEdits.orphaned(config),
                            lossesFor = { screen -> ScreenEdits.deletionLosses(config, screen.id) },
                            onDeleteFolder = { folder ->
                                store.update { FolderEdits.delete(it, folder.id) }
                            },
                            onRename = { renaming = it; page = Page.RENAME },
                            onDelete = { store.update { current -> ScreenEdits.delete(current, it.id) } },
                            onSwipeOrder = { page = Page.SWIPE_ORDER },
                            onDuplicate = { screen -> duplicate(screen) },
                            onMakeHome = { target ->
                                // Ohne Weg in die Einstellungen waere der Wechsel nicht
                                // rueckgaengig zu machen - man kaeme nie wieder hierher.
                                if (ScreenEdits.settingsReachable(config, target.id)) {
                                    store.update { current -> current.copy(homeScreenId = target.id) }
                                } else {
                                    switching = target
                                }
                            },
                        )

                        Page.RENAME -> ScreenPanel(
                            // Immer die frische Fassung aus der Konfiguration: nach einem
                            // Rasterwechsel zeigte die gemerkte sonst weiter das alte Raster.
                            screen = renaming?.id?.let { id -> config.screens.firstOrNull { it.id == id } },
                            gutterDp = config.appearance.gutterDp,
                            borderPercent = config.appearance.safeBorderPercent,
                            theme = config.appearance.theme,
                            onBackground = { hintergrund ->
                                val ziel = renaming
                                if (ziel != null) {
                                    store.update { current ->
                                        current.copy(
                                            screens = current.screens.map {
                                                if (it.id == ziel.id) {
                                                    it.copy(background = hintergrund)
                                                } else {
                                                    it
                                                }
                                            },
                                        )
                                    }
                                }
                            },
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
                            language = config.appearance.language,
                            onLanguage = { next ->
                                // Sofort neu aufbauen. Wer hier "Deutsch" antippt und
                                // nichts geschieht, tippt noch einmal und noch einmal -
                                // und das ist die Seite, auf der man gerade nicht lesen
                                // kann, was los ist.
                                val anders = next != config.appearance.language
                                store.update {
                                    it.copy(appearance = it.appearance.copy(language = next))
                                }
                                if (anders) recreate()
                            },
                            themeName = config.appearance.theme,
                            textScale = config.appearance.textScale,
                            labelPosition = config.appearance.labelPosition,
                            hideCutLabels = config.appearance.hideCutLabels,
                            onToggleHideCutLabels = {
                                store.update {
                                    it.copy(
                                        appearance = it.appearance.copy(
                                            hideCutLabels = !it.appearance.hideCutLabels,
                                        ),
                                    )
                                }
                            },
                            icons = config.appearance.icons,
                            onTheme = { next ->
                                store.update { it.copy(appearance = it.appearance.copy(theme = next)) }
                            },
                            onTextScale = { next ->
                                store.update { it.copy(appearance = it.appearance.copy(textScale = next)) }
                            },
                            fullScreen = config.appearance.fullScreen,
                            font = config.appearance.font,
                            labelScale = config.appearance.labelScale,
                            onLabelScale = { next ->
                                store.update {
                                    it.copy(appearance = it.appearance.copy(labelScale = next))
                                }
                            },
                            iconPercent = config.appearance.iconPercent,
                            onIconPercent = { next ->
                                store.update {
                                    it.copy(appearance = it.appearance.copy(iconPercent = next))
                                }
                            },
                            onFont = { next ->
                                store.update { it.copy(appearance = it.appearance.copy(font = next)) }
                            },
                            onToggleFullScreen = {
                                store.update {
                                    it.copy(
                                        appearance = it.appearance.copy(
                                            fullScreen = !it.appearance.fullScreen,
                                        ),
                                    )
                                }
                            },
                            onLabelPosition = { next ->
                                store.update { it.copy(appearance = it.appearance.copy(labelPosition = next)) }
                            },
                            showHeader = config.appearance.showHeader,
                            appearance = config.appearance,
                            onToggleHeader = {
                                store.update {
                                    it.copy(appearance = it.appearance.copy(showHeader = !it.appearance.showHeader))
                                }
                            },
                            clock = config.appearance.clock,
                            gutterDp = config.appearance.gutterDp,
                            onGutter = { next ->
                                store.update {
                                    it.copy(appearance = it.appearance.copy(gutterDp = GridLooks.gutter(next)))
                                }
                            },
                            borderPercent = config.appearance.safeBorderPercent,
                            onBorder = { next ->
                                store.update {
                                    it.copy(
                                        appearance = it.appearance.copy(
                                            safeBorderPercent = GridLooks.border(next),
                                        ),
                                    )
                                }
                            },
                            cornerRadiusDp = config.appearance.cornerRadiusDp,
                            onCornerRadius = { next ->
                                store.update {
                                    it.copy(
                                        appearance = it.appearance.copy(
                                            cornerRadiusDp = GridLooks.radius(next),
                                        ),
                                    )
                                }
                            },
                            orientation = config.appearance.orientation,
                            onOrientation = { next ->
                                store.update {
                                    it.copy(appearance = it.appearance.copy(orientation = next))
                                }
                                // Sofort umsetzen: eine Drehung, die erst beim naechsten
                                // Start kaeme, sieht aus wie ein Schalter, der klemmt.
                                requestedOrientation = Orientation.requested(next)
                            },
                            onClock = { next ->
                                store.update { it.copy(appearance = it.appearance.withClock(next)) }
                            },
                            onIcons = { next ->
                                store.update { it.copy(appearance = it.appearance.withIcons(next)) }
                            },
                            clockScale = config.appearance.clockScale,
                            onClockScale = { next ->
                                store.update {
                                    it.copy(
                                        appearance = it.appearance.copy(
                                            clockScale = ClockFormat.scale(next),
                                        ),
                                    )
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
                            recentCount = config.apps.recentCount,
                            onRecentCount = { anzahl ->
                                store.update { it.copy(apps = it.apps.copy(recentCount = anzahl)) }
                            },
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
                            hasPin = Pin.usable(config.security.pin),
                            protectsEditor = config.security.pinProtectsEditor,
                            lockOthers = config.apps.lockOthers,
                            wouldAllow = AppLock.wouldAllow(config),
                            allowedApps = config.apps.allowed.size,
                            onToggleAppLock = {
                                when (AppLock.toggle(config)) {
                                    // Nicht einschalten, wenn nichts erlaubt waere: das
                                    // ist kein gesichertes Telefon, sondern ein
                                    // verschlossenes. Statt dessen die Liste zeigen.
                                    AppLock.Step.CHOOSE_FIRST -> {
                                        Notice.show(
                                            this@SettingsActivity,
                                            R.string.security_applock_choose_first,
                                        )
                                        page = Page.ALLOWED_APPS
                                    }

                                    AppLock.Step.TURN_ON -> store.update { current ->
                                        // Die Kachel-Apps von selbst erlauben: die hat der
                                        // Einrichtende gerade bewusst in Reichweite gelegt.
                                        current.copy(
                                            apps = current.apps.copy(
                                                lockOthers = true,
                                                allowed = current.apps.allowed.ifEmpty {
                                                    AppLock.initialAllowance(current)
                                                },
                                            ),
                                        )
                                    }

                                    AppLock.Step.TURN_OFF -> store.update {
                                        it.copy(apps = it.apps.copy(lockOthers = false))
                                    }
                                }
                            },
                            onAllowedApps = { page = Page.ALLOWED_APPS },
                            protectsAppList = config.security.pinProtectsAppList,
                            onToggleAppListProtection = {
                                store.update {
                                    it.copy(
                                        security = it.security.copy(
                                            pinProtectsAppList = !it.security.pinProtectsAppList,
                                        ),
                                    )
                                }
                            },
                            protectsCallLog = config.security.pinProtectsCallLogDelete,
                            onToggleCallLogProtection = {
                                store.update {
                                    it.copy(
                                        security = it.security.copy(
                                            pinProtectsCallLogDelete =
                                                !it.security.pinProtectsCallLogDelete,
                                        ),
                                    )
                                }
                            },
                            onToggleEditorProtection = {
                                store.update {
                                    it.copy(
                                        security = it.security.copy(
                                            pinProtectsEditor = !it.security.pinProtectsEditor,
                                        ),
                                    )
                                }
                            },
                            onSetPin = { page = Page.SET_PIN },
                            onRemovePin = {
                                // Mit der PIN gehen auch die Schutzschalter - alle, auch
                                // die App-Sperre, die in `apps` liegt und beim ersten Mal
                                // deshalb stehen blieb. Sie stehen ohne PIN nirgends mehr,
                                // man kaeme also nicht mehr an sie heran, und eine spaeter
                                // gesetzte PIN wuerde ungefragt Tueren zusperren, die
                                // vorher offen waren.
                                store.update {
                                    it.copy(
                                        security = Security(),
                                        apps = it.apps.copy(
                                            lockOthers = false,
                                            allowed = emptySet(),
                                        ),
                                    )
                                }
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
                            haptics = config.behaviour.haptics,
                            confirmMessages = config.behaviour.confirmMessages,
                            homeKeyReturns = config.behaviour.homeKeyReturnsToStart,
                            swipeScreens = config.behaviour.swipeBetweenScreens,
                            pressMode = config.behaviour.pressMode,
                            onTogglePressMode = {
                                store.update {
                                    val jetzt = it.behaviour.pressMode
                                    it.copy(
                                        behaviour = it.behaviour.copy(
                                            pressMode = if (jetzt == PressMode.SHORT) {
                                                PressMode.LONG
                                            } else {
                                                PressMode.SHORT
                                            },
                                        ),
                                    )
                                }
                            },
                            onToggleSwipe = {
                                store.update {
                                    it.copy(
                                        behaviour = it.behaviour.copy(
                                            swipeBetweenScreens = !it.behaviour.swipeBetweenScreens,
                                        ),
                                    )
                                }
                            },
                            onToggleHaptics = {
                                store.update {
                                    it.copy(
                                        behaviour = it.behaviour.withHaptics(
                                            Haptics.next(it.behaviour.haptics),
                                        ),
                                    )
                                }
                            },
                            onToggleConfirmMessages = {
                                store.update {
                                    it.copy(
                                        behaviour = it.behaviour.copy(
                                            confirmMessages = !it.behaviour.confirmMessages,
                                        ),
                                    )
                                }
                            },
                            onToggleHomeKey = {
                                store.update {
                                    it.copy(
                                        behaviour = it.behaviour.copy(
                                            homeKeyReturnsToStart = !it.behaviour.homeKeyReturnsToStart,
                                        ),
                                    )
                                }
                            },
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
                                val an = !config.sos.sendLocation
                                store.update { it.copy(sos = it.sos.copy(sendLocation = an)) }
                                if (an && !locationGranted) {
                                    askLocation.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                        ),
                                    )
                                }
                            },
                            onToggleAlarmSound = {
                                store.update { it.copy(sos = it.sos.copy(alarmSound = !it.sos.alarmSound)) }
                            },
                            onToggleAlarmFlash = {
                                store.update { it.copy(sos = it.sos.copy(alarmFlash = !it.sos.alarmFlash)) }
                            },
                            onPreview = {
                                startActivity(
                                    Intent(this@SettingsActivity, SosActivity::class.java)
                                        .putExtra(SosActivity.EXTRA_PREVIEW, true),
                                )
                            },
                            onTryAlarm = {
                                if (alarmProbe) {
                                    SosAlarm.stop(this@SettingsActivity)
                                    alarmProbe = false
                                } else {
                                    SosAlarm.start(this@SettingsActivity, config.sos)
                                    alarmProbe = true
                                }
                            },
                            alarmRunning = alarmProbe,
                            locationGranted = locationGranted,
                            locationBlocked = PermissionState.blocked(
                                locationDeniedOnce,
                                locationCanAskAgain,
                            ),
                            onAskLocation = {
                                askLocation.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            },
                            onLocationSettings = { Intents.appSettings(this@SettingsActivity) },
                        )

                        Page.TRANSFER -> TransferList(
                            onExport = { exportFile.launch(ConfigTransfer.suggestedFileName(System.currentTimeMillis())) },
                            // Bewusst ohne Typfilter: eine Sicherung vom anderen Telefon kann mit
                            // beliebigem MIME-Typ ankommen, und gefilterte Eintraege sind im
                            // Systemdialog zwar sichtbar, aber nicht antippbar - was aussieht,
                            // als waere die App kaputt.
                            onImport = { importFile.launch(arrayOf("*/*")) },
                        )

                        Page.SWIPE_ORDER -> SwipeOrderList(
                            screens = ScreenOrder.ordered(config),
                            excluded = SwipeChain.excluded(config),
                            mayLeave = { id -> SwipeChain.mayLeave(config, id) },
                            onUp = { id ->
                                store.update { SwipeChain.moveUp(it, id) }
                            },
                            onDown = { id ->
                                store.update { SwipeChain.moveDown(it, id) }
                            },
                            onExclude = { id -> store.update { SwipeChain.exclude(it, id) } },
                            onInclude = { id -> store.update { SwipeChain.include(it, id) } },
                        )

                        Page.ALLOWED_APPS -> AllowedAppsList(
                            repository = AppRepository.get(this@SettingsActivity),
                            allowed = config.apps.allowed,
                            onToggle = { key ->
                                store.update { it.copy(apps = AppLock.toggleAllowed(it.apps, key)) }
                            },
                        )

                        Page.MESSAGES -> MessagesSettingsList(
                            confirmSend = config.sms.confirmBeforeSending,
                            onToggleConfirmSend = {
                                store.update {
                                    it.copy(
                                        sms = it.sms.copy(
                                            confirmBeforeSending = !it.sms.confirmBeforeSending,
                                        ),
                                    )
                                }
                            },
                            sendAbove = config.sms.sendButtonAbove,
                            onToggleSendAbove = {
                                store.update {
                                    it.copy(sms = it.sms.copy(sendButtonAbove = !it.sms.sendButtonAbove))
                                }
                            },
                            sendLarge = config.sms.sendButtonLarge,
                            onToggleSendLarge = {
                                store.update {
                                    it.copy(sms = it.sms.copy(sendButtonLarge = !it.sms.sendButtonLarge))
                                }
                            },
                            scale = config.sms.conversationScale,
                            onScale = { wert ->
                                store.update { it.copy(sms = it.sms.copy(conversationScale = wert)) }
                            },
                            numbers = config.sms.hiddenNumbers,
                            words = config.sms.hiddenWords,
                            onNumbers = { text ->
                                store.update {
                                    it.copy(sms = it.sms.copy(hiddenNumbers = CallBlocking.parse(text)))
                                }
                            },
                            onWords = { text ->
                                store.update {
                                    it.copy(sms = it.sms.copy(hiddenWords = SmsFilter.parseWords(text)))
                                }
                            },
                            vibrationMs = config.sms.vibrationMs,
                            onVibration = { dauer ->
                                store.update { it.copy(sms = it.sms.copy(vibrationMs = dauer)) }
                            },
                            fullScreen = config.sms.fullScreenAlert,
                            onToggleFullScreen = {
                                store.update {
                                    it.copy(sms = it.sms.copy(fullScreenAlert = !it.sms.fullScreenAlert))
                                }
                            },
                            repeatMinutes = config.sms.repeatMinutes,
                            onRepeat = { minuten ->
                                store.update { it.copy(sms = it.sms.copy(repeatMinutes = minuten)) }
                                // Aus heisst sofort aus, nicht erst bei der naechsten
                                // Nachricht: sonst erinnert ein alter Wecker weiter.
                                MessageReminderReceiver.schedule(this@SettingsActivity, minuten)
                            },
                        )

                        Page.CONTACTS -> ContactsSettingsList(
                            contacts = config.contacts,
                            onToggleSort = {
                                store.update {
                                    it.copy(
                                        contacts = it.contacts.copy(
                                            sortBySurname = !it.contacts.sortBySurname,
                                        ),
                                    )
                                }
                            },
                            onToggleSearchNumbers = {
                                store.update {
                                    it.copy(
                                        contacts = it.contacts.copy(
                                            searchNumbers = !it.contacts.searchNumbers,
                                        ),
                                    )
                                }
                            },
                            onToggleFavouritesFirst = {
                                store.update {
                                    it.copy(
                                        contacts = it.contacts.copy(
                                            favouritesFirst = !it.contacts.favouritesFirst,
                                        ),
                                    )
                                }
                            },
                        )

                        Page.CALL_TYPES -> CallTypesList(
                            blocked = config.phone.blockedNumbers,
                            onDialerApp = {
                                val absicht = Intents.dialerRoleIntent(this@SettingsActivity)
                                if (absicht != null) {
                                    askDialerRole.launch(absicht)
                                } else {
                                    Intents.chooseDialerApp(this@SettingsActivity)
                                }
                            },
                            onBlocked = { text ->
                                store.update {
                                    it.copy(
                                        phone = it.phone.copy(
                                            blockedNumbers = CallBlocking.parse(text),
                                        ),
                                    )
                                }
                            },
                            audioRoute = config.phone.audioRoute,
                            onAudioRoute = { weg ->
                                store.update { it.copy(phone = it.phone.copy(audioRoute = weg)) }
                            },
                            speakerOnOutgoing = config.phone.speakerOnOutgoing,
                            onToggleSpeakerOnOutgoing = {
                                store.update {
                                    it.copy(
                                        phone = it.phone.copy(
                                            speakerOnOutgoing = !it.phone.speakerOnOutgoing,
                                        ),
                                    )
                                }
                            },
                            photo = config.phone.callerPhoto,
                            onPhoto = { groesse ->
                                store.update { it.copy(phone = it.phone.copy(callerPhoto = groesse)) }
                            },
                            grouping = config.phone.callGrouping,
                            onGrouping = { art ->
                                store.update { it.copy(phone = it.phone.copy(callGrouping = art)) }
                            },
                            hidden = config.phone.hiddenCallTypes,
                            onToggle = { name ->
                                store.update {
                                    val jetzt = it.phone.hiddenCallTypes
                                    it.copy(
                                        phone = it.phone.copy(
                                            hiddenCallTypes = if (name in jetzt) {
                                                jetzt - name
                                            } else {
                                                jetzt + name
                                            },
                                        ),
                                    )
                                }
                            },
                        )

                        Page.DIAGNOSTICS -> DiagnosticsList(this@SettingsActivity)

                        Page.RESET -> ResetPanel(
                            losses = Reset.losses(config),
                            onBackup = { page = Page.TRANSFER },
                            onCancel = { page = Page.MAIN },
                            onReset = {
                                // Erst die Widget-Kennungen freigeben, dann die
                                // Konfiguration wegwerfen. Andersherum waeren sie nicht
                                // mehr aufzufinden, und der Widget-Host hielte sie fuer
                                // immer.
                                val host = WidgetHostController.get(this@SettingsActivity)
                                Reset.widgetIds(config).forEach { host.release(it) }
                                store.update { Reset.fresh() }
                                // Und dann wirklich von vorn. Die Zusage lautet "wie am
                                // ersten Tag", und der erste Tag faengt mit dem
                                // Assistenten an. Ihn nur beim naechsten Kaltstart zu
                                // zeigen hiesse, den Satz nicht einzuloesen: der
                                // Startbildschirm laeuft laengst und prueft nicht noch
                                // einmal nach.
                                startActivity(
                                    Intent(this@SettingsActivity, WizardActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                                finish()
                            },
                        )
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Die Kennungen stehen in [org.biglau.ui.SettingsLink] - dort, wo auch die
         * Absicht steht, mit der andere Bildschirme hierher springen. Hier bleiben sie
         * als Verweis, damit die Seite und der Weg zu ihr nicht auseinanderlaufen.
         */
        const val EXTRA_PAGE = SettingsLink.EXTRA_PAGE
        const val PAGE_CALL_TYPES = SettingsLink.PAGE_CALL_TYPES
        const val PAGE_CONTACTS = SettingsLink.PAGE_CONTACTS
        const val PAGE_SOS = SettingsLink.PAGE_SOS
        const val PAGE_HIDDEN_APPS = SettingsLink.PAGE_HIDDEN_APPS
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
    onReset: () -> Unit,
    onContacts: () -> Unit,
    onCallTypes: () -> Unit,
    onMessages: () -> Unit,
    onHomeApp: () -> Unit,
    onDialerApp: () -> Unit,
    istStartbildschirm: Boolean,
    istTelefonApp: Boolean,
    onDone: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings)) }
        // Nicht dasselbe Haus wie "Als Startbildschirm verwenden" weiter unten: zwei Zeilen
        // mit demselben Symbol tragen kein Wissen, sie stiften Verwechslung.
        item { BigRow(stringResource(R.string.settings_screens), icon = Icons.Filled.ViewCarousel, onClick = onScreens) }
        item { BigRow(stringResource(R.string.settings_appearance), icon = Icons.Filled.Palette, onClick = onAppearance) }
        item { BigRow(stringResource(R.string.settings_behaviour), icon = Icons.Filled.NotificationsActive, onClick = onBehaviour) }
        item { BigRow(stringResource(R.string.settings_app_list), icon = Icons.Filled.Apps, onClick = onHiddenApps) }
        item { BigRow(stringResource(R.string.settings_security), icon = Icons.Filled.Lock, onClick = onSecurity) }
        // Beide Zeilen sagen den Zustand, statt eine Aufforderung zu wiederholen, die schon
        // erfuellt ist. Vorher stand "Als Telefon-App verwenden" auch dann da, wenn BigLau
        // es laengst war - und ein Tipp darauf tat sichtbar nichts: der Rollendialog schloss
        // sich sofort wieder ("Application is already a role holder", im Protokoll gesehen).
        item {
            BigRow(
                label = stringResource(
                    if (istStartbildschirm) R.string.is_home else R.string.set_as_home,
                ),
                secondary = if (istStartbildschirm) stringResource(R.string.role_change_hint) else null,
                icon = Icons.Filled.Home,
                surface = if (istStartbildschirm) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onHomeApp,
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (istTelefonApp) R.string.is_dialer else R.string.set_as_dialer,
                ),
                secondary = stringResource(
                    if (istTelefonApp) R.string.role_change_hint else R.string.set_as_dialer_hint,
                ),
                icon = Icons.Filled.Call,
                surface = if (istTelefonApp) palette.surfaceAccent else palette.surfaceDefault,
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
        item {
            BigRow(
                stringResource(R.string.settings_call_types),
                icon = Icons.Filled.History,
                onClick = onCallTypes,
            )
        }
        item {
            BigRow(
                stringResource(R.string.settings_messages),
                icon = Icons.AutoMirrored.Filled.Message,
                onClick = onMessages,
            )
        }
        item {
            BigRow(
                stringResource(R.string.settings_contacts),
                icon = Icons.Filled.Person,
                onClick = onContacts,
            )
        }
        item { BigRow(stringResource(R.string.settings_diagnostics), icon = Icons.Filled.Info, onClick = onDiagnostics) }
        item {
            BigRow(
                label = stringResource(R.string.settings_reset),
                icon = Icons.Filled.DeleteForever,
                onClick = onReset,
            )
        }
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
    orphanedFolders: List<Screen>,
    onDeleteFolder: (Screen) -> Unit,
    lossesFor: (Screen) -> Pair<Int, Int>,
    onRename: (Screen) -> Unit,
    onDelete: (Screen) -> Unit,
    onMakeHome: (Screen) -> Unit,
    onSwipeOrder: () -> Unit,
    onDuplicate: (Screen) -> Unit,
) {
    val palette = LocalBigPalette.current
    // Ein Screen mit allen Kacheln war mit einem einzigen Tipp weg - ohne Rueckfrage, ohne
    // Weg zurueck. Dieselbe Zweistufigkeit wie beim Verkleinern des Rasters: der erste Tipp
    // sagt, was es kostet, erst der zweite tut es.
    var scharf by remember { mutableStateOf<String?>(null) }
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
                    fontSize = bigSp(15f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
        if (screens.size > 1) {
            item {
                BigRow(
                    label = stringResource(R.string.swipe_order),
                    secondary = stringResource(R.string.swipe_order_hint),
                    icon = Icons.Filled.SwapVert,
                    onClick = onSwipeOrder,
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
            BigRow(
                label = stringResource(R.string.screen_duplicate, screen.name),
                secondary = stringResource(R.string.screen_duplicate_hint),
                icon = Icons.Filled.ContentCopy,
                onClick = { onDuplicate(screen) },
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
                val (kacheln, ordner) = lossesFor(screen)
                val gespannt = scharf == screen.id
                BigRow(
                    label = if (gespannt) {
                        stringResource(R.string.screen_delete_now, screen.name)
                    } else {
                        stringResource(R.string.screen_delete, screen.name)
                    },
                    secondary = if (!gespannt) {
                        null
                    } else {
                        buildString {
                            append(pluralStringResource(R.plurals.screen_delete_tiles, kacheln, kacheln))
                            if (ordner > 0) {
                                append(' ')
                                append(pluralStringResource(R.plurals.screen_delete_folders, ordner, ordner))
                            }
                        }
                    },
                    icon = Icons.Filled.Delete,
                    surface = palette.surfaceDanger,
                    onClick = { if (gespannt) onDelete(screen) else scharf = screen.id },
                )
            }
        }
        // Ein Ordner ohne Kachel ist nirgends zu sehen und nirgends zu oeffnen. Neue
        // entstehen nicht mehr - das Neubelegen einer Ordnerkachel fragt jetzt nach, und
        // das Loeschen eines Screens raeumt seine Ordner mit ab. Aeltere gibt es aber, und
        // sie liegen sonst fuer immer in der Konfiguration und in jeder Sicherung.
        if (orphanedFolders.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.folders_orphaned),
                    color = palette.danger,
                    fontSize = bigSp(15f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
            items(orphanedFolders, key = { "orphan-${it.id}" }) { folder ->
                BigRow(
                    label = stringResource(R.string.folder_delete_title, folder.name),
                    // Kacheln, nicht Zellen: ein leerer Platz im Ordner ist keine Kachel,
                    // und "mit 6 Kacheln" ueber einem Ordner mit fuenf waere eine falsche
                    // Zahl in genau der Zeile, die zum Loeschen auffordert.
                    secondary = pluralStringResource(
                        R.plurals.folder_delete_body,
                        folder.tileCount,
                        folder.tileCount,
                    ),
                    // Nicht derselbe Papierkorb wie beim Screen darueber: das hier ist
                    // kein gewoehnliches Loeschen, sondern das Aufraeumen von etwas, das
                    // sich ohnehin nicht mehr oeffnen laesst.
                    icon = Icons.Filled.FolderOff,
                    surface = palette.surfaceDanger,
                    onClick = { onDeleteFolder(folder) },
                )
            }
        }
        // Angelegt werden Screens im Editor, nicht hier - so haengt an jedem neuen Screen
        // von Anfang an eine Kachel, die hinfuehrt. Wer hier danach sucht, soll das lesen.
        item {
            Text(
                text = stringResource(R.string.screens_where_new),
                color = palette.onBackground,
                fontSize = bigSp(15f),
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
    gutterDp: Int,
    borderPercent: Int,
    onDone: (String) -> Unit,
    onGrid: (Int, Int) -> Unit,
    onBackground: (Background) -> Unit,
    theme: ThemeName,
) {
    if (screen == null) return
    var text by remember(screen.id) { mutableStateOf(screen.name) }
    var confirming by remember(screen.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    val palette = LocalBigPalette.current
    // Was dem Raster wirklich bleibt: Bildschirm minus Aussenrand. Der Rand ist ein
    // Prozentsatz der Breite und gilt auf allen vier Seiten.
    val fenster = LocalConfiguration.current
    val rand = fenster.screenWidthDp * borderPercent / 100f
    val usableWidthDp = fenster.screenWidthDp - 2 * rand
    val usableHeightDp = fenster.screenHeightDp - 2 * rand
    // Ausserhalb der Liste erfragt: in einem items-Aufruf ist kein Composable erlaubt.
    val hintergrundfarben = ScreenBackground.choicesFor(theme, isSystemInDarkTheme())
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { BigHeading(stringResource(R.string.screen_edit)) }
        item {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = bigSp(26f), fontWeight = FontWeight.Bold),
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
        // Und frei waehlbar, so weit dieser Bildschirm es traegt - siehe GridLimits. Die
        // Grenze ist keine Zahl im Quelltext, sondern was hier noch als Kachel lesbar ist.
        item { BigHeading(stringResource(R.string.screen_background)) }
        item {
            Text(
                text = if (ScreenBackground.offersChoices(theme)) {
                    stringResource(R.string.screen_background_hint)
                } else {
                    stringResource(R.string.screen_background_contrast)
                },
                color = palette.onBackground,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        item {
            val gewaehlt = screen.background == Background.Theme
            BigRow(
                label = stringResource(R.string.screen_background_theme),
                icon = if (gewaehlt) Icons.Filled.Check else null,
                surface = BigSurface(palette.background, palette.onBackground),
                borderColor = if (gewaehlt) palette.accent else null,
                onClick = { onBackground(Background.Theme) },
            )
        }
        // Jede Zeile in ihrer eigenen Farbe. Bei einer Farbe ist der Name nutzlos - man
        // will sie sehen, und zwar in der Groesse, in der sie spaeter dasteht.
        items(hintergrundfarben) { farbe ->
            val gewaehlt = (screen.background as? Background.Solid)?.argb == farbe
            BigRow(
                label = stringResource(R.string.screen_background_colour),
                icon = if (gewaehlt) Icons.Filled.Check else null,
                surface = BigSurface(
                    Color(farbe.toInt()),
                    Color(ScreenBackground.inkFor(farbe).toInt()),
                ),
                borderColor = if (gewaehlt) palette.accent else null,
                onClick = { onBackground(Background.Solid(farbe)) },
            )
        }
        item { BigHeading(stringResource(R.string.screen_columns)) }
        items(GridLimits.columns(usableWidthDp, gutterDp)) { spalten ->
            GridChoiceRow(
                label = pluralStringResource(R.plurals.screen_columns_n, spalten, spalten),
                cols = spalten,
                rows = screen.rows,
                screen = screen,
                confirming = confirming,
                onArm = { confirming = it },
                onGrid = onGrid,
            )
        }
        item { BigHeading(stringResource(R.string.screen_rows)) }
        items(GridLimits.rows(usableHeightDp, gutterDp)) { zeilen ->
            GridChoiceRow(
                label = pluralStringResource(R.plurals.screen_rows_n, zeilen, zeilen),
                cols = screen.cols,
                rows = zeilen,
                screen = screen,
                confirming = confirming,
                onArm = { confirming = it },
                onGrid = onGrid,
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

private fun iconVisibilityLabel(icons: IconVisibility): Int = when (icons) {
    IconVisibility.ALWAYS -> R.string.appearance_icons_always
    IconVisibility.IF_ROOM -> R.string.appearance_icons_if_room
    IconVisibility.NEVER -> R.string.appearance_icons_never
}

private fun fontLabel(font: FontChoice): Int = when (font) {
    FontChoice.HYPERLEGIBLE -> R.string.font_hyperlegible
    FontChoice.SYSTEM -> R.string.font_system
}

private fun languageLabel(language: Language): Int = when (language) {
    Language.SYSTEM -> R.string.language_system
    Language.GERMAN -> R.string.language_german
    Language.ENGLISH -> R.string.language_english
}

@Composable
private fun AppearanceList(
    language: Language,
    onLanguage: (Language) -> Unit,
    themeName: ThemeName,
    textScale: Float,
    labelPosition: LabelPosition,
    hideCutLabels: Boolean,
    onToggleHideCutLabels: () -> Unit,
    icons: IconVisibility,
    showHeader: Boolean,
    appearance: Appearance,
    clock: ClockDisplay,
    clockScale: Float,
    onClockScale: (Float) -> Unit,
    orientation: ScreenOrientation,
    onOrientation: (ScreenOrientation) -> Unit,
    gutterDp: Int,
    onGutter: (Int) -> Unit,
    borderPercent: Int,
    onBorder: (Int) -> Unit,
    cornerRadiusDp: Int,
    onCornerRadius: (Int) -> Unit,
    fullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    font: FontChoice,
    onFont: (FontChoice) -> Unit,
    labelScale: Float,
    onLabelScale: (Float) -> Unit,
    iconPercent: Int,
    onIconPercent: (Int) -> Unit,
    onToggleHeader: () -> Unit,
    onClock: (ClockDisplay) -> Unit,
    onTheme: (ThemeName) -> Unit,
    onTextScale: (Float) -> Unit,
    onLabelPosition: (LabelPosition) -> Unit,
    onIcons: (IconVisibility) -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_appearance)) }
        item { BigHeading(stringResource(R.string.appearance_language)) }
        // Die Sprachnamen bleiben in ihrer eigenen Sprache. Wer die eingestellte Sprache
        // nicht liest, sucht hier nach dem Wort, das er kennt - "Deutsch" auf Englisch
        // uebersetzt zu "German" waere genau fuer den unlesbar, der die Zeile braucht.
        items(Language.entries.toList()) { entry ->
            BigRow(
                label = stringResource(languageLabel(entry)),
                icon = if (entry == language) Icons.Filled.Check else null,
                surface = if (entry == language) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onLanguage(entry) },
            )
        }
        // Jede Zeile ist in ihrem eigenen Thema gemalt. Dreimal dasselbe Paletten-Symbol
        // sagte nichts; die Farben selbst sagen alles. Die Auswahl traegt deshalb ein
        // Haekchen statt einer Akzentflaeche - die Flaeche gehoert hier dem Thema.
        items(ThemeName.entries.toList()) { entry ->
            val own = paletteFor(entry, isSystemInDarkTheme())
            val chosen = entry == themeName
            BigRow(
                label = stringResource(themeLabel(entry)),
                icon = if (chosen) Icons.Filled.Check else null,
                surface = BigSurface(own.emptyTile, own.onBackground),
                borderColor = if (chosen) palette.accent else null,
                onClick = { onTheme(entry) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_font)) }
        // Jede Zeile in ihrer eigenen Schrift: man sieht den Unterschied, statt ihn zu
        // lesen. Bei einer Schrift, die es fuer schlechte Augen leichter machen soll, ist
        // das die einzige Vorschau, die etwas taugt.
        items(FontChoice.entries.toList()) { entry ->
            BigRow(
                label = stringResource(fontLabel(entry)),
                secondary = if (entry == FontChoice.HYPERLEGIBLE) {
                    stringResource(R.string.font_hyperlegible_hint)
                } else {
                    null
                },
                icon = if (entry == font) Icons.Filled.Check else null,
                surface = if (entry == font) palette.surfaceAccent else palette.surfaceDefault,
                fontFamily = familyFor(entry),
                onClick = { onFont(entry) },
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
        item { BigHeading(stringResource(R.string.appearance_label_size)) }
        // Jede Zeile in ihrer eigenen Groesse - wie bei der Textgroesse darueber. Drei
        // Listen aus denselben Prozentzahlen sehen sonst gleich aus, sobald die
        // Ueberschrift weggescrollt ist, und man stellt die falsche.
        items(LABEL_SCALES) { scale ->
            CompositionLocalProvider(LocalTextScale provides scale) {
                BigRow(
                    label = "${(scale * 100).toInt()} %",
                    icon = if (scale == labelScale) Icons.Filled.Check else null,
                    surface = if (scale == labelScale) {
                        palette.surfaceAccent
                    } else {
                        palette.surfaceDefault
                    },
                    onClick = { onLabelScale(scale) },
                )
            }
        }
        item { BigHeading(stringResource(R.string.appearance_icon_size)) }
        // Und hier das Symbol selbst in der Groesse, um die es geht.
        items(ICON_PERCENTS) { percent ->
            BigRow(
                label = "$percent %",
                leading = {
                    Icon(
                        imageVector = if (percent == iconPercent) {
                            Icons.Filled.Check
                        } else {
                            Icons.Filled.Apps
                        },
                        contentDescription = null,
                        tint = palette.onBackground,
                        modifier = Modifier.size((16 + percent / 2).dp),
                    )
                },
                surface = if (percent == iconPercent) {
                    palette.surfaceAccent
                } else {
                    palette.surfaceDefault
                },
                onClick = { onIconPercent(percent) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_label_position)) }
        items(LabelPosition.entries.toList()) { position ->
            BigRow(
                label = stringResource(labelPositionLabel(position)),
                surface = if (position == labelPosition) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onLabelPosition(position) },
            )
        }
        // PLAN.md 3.2: "auf 3 Zoll ist ein abgeschnittenes Wort schlimmer als gar keins."
        // Nur anbieten, wo die Beschriftung ueberhaupt steht - bei "ohne Beschriftung"
        // waere es ein Schalter ohne Wirkung.
        if (labelPosition != LabelPosition.HIDDEN) {
            item {
                BigRow(
                    label = stringResource(
                        if (hideCutLabels) R.string.appearance_hide_cut_on else R.string.appearance_hide_cut_off,
                    ),
                    secondary = stringResource(R.string.appearance_hide_cut_hint),
                    surface = if (hideCutLabels) palette.surfaceAccent else palette.surfaceDefault,
                    onClick = onToggleHideCutLabels,
                )
            }
        }
        item {
            BigRow(
                label = stringResource(
                    if (fullScreen) R.string.appearance_fullscreen_on else R.string.appearance_fullscreen_off,
                ),
                secondary = stringResource(R.string.appearance_fullscreen_hint),
                surface = if (fullScreen) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleFullScreen,
            )
        }
        item { BigHeading(stringResource(R.string.appearance_icons)) }
        items(IconVisibility.entries.toList()) { entry ->
            BigRow(
                label = stringResource(iconVisibilityLabel(entry)),
                secondary = if (entry == IconVisibility.IF_ROOM) {
                    stringResource(R.string.appearance_icons_if_room_hint)
                } else {
                    null
                },
                icon = if (entry == icons) Icons.Filled.Check else null,
                surface = if (entry == icons) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onIcons(entry) },
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
        // Vollbild nimmt die Systemleiste weg, die Kopfzeile traegt den Rest. Beides aus
        // heisst: keine Uhrzeit, kein Ladestand. Erlaubt, aber gesagt - sonst sucht man
        // den Fehler beim Telefon.
        if (StatusVisibility.warns(appearance)) {
            item {
                Text(
                    text = stringResource(
                        if (!StatusVisibility.showsTime(appearance)) {
                            R.string.status_hidden_all
                        } else {
                            R.string.status_hidden_battery
                        },
                    ),
                    color = palette.danger,
                    fontSize = bigSp(15f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
        item { BigHeading(stringResource(R.string.appearance_gutter)) }
        items(GridLooks.GUTTERS) { wert ->
            BigRow(
                label = "$wert dp",
                icon = if (wert == gutterDp) Icons.Filled.Check else null,
                surface = if (wert == gutterDp) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onGutter(wert) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_border)) }
        items(GridLooks.BORDERS) { wert ->
            BigRow(
                label = "$wert %",
                icon = if (wert == borderPercent) Icons.Filled.Check else null,
                surface = if (wert == borderPercent) {
                    palette.surfaceAccent
                } else {
                    palette.surfaceDefault
                },
                onClick = { onBorder(wert) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_corner)) }
        // Jede Zeile in ihrer eigenen Rundung: die Zahl sagt nichts, die Ecke alles.
        items(GridLooks.RADII) { wert ->
            BigRow(
                label = "$wert dp",
                icon = if (wert == cornerRadiusDp) Icons.Filled.Check else null,
                surface = if (wert == cornerRadiusDp) {
                    palette.surfaceAccent
                } else {
                    palette.surfaceDefault
                },
                cornerRadius = wert.dp,
                onClick = { onCornerRadius(wert) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_orientation)) }
        items(ScreenOrientation.entries.toList()) { entry ->
            BigRow(
                label = stringResource(orientationLabel(entry)),
                secondary = if (entry == ScreenOrientation.LANDSCAPE) {
                    stringResource(R.string.orientation_landscape_hint)
                } else {
                    null
                },
                icon = if (entry == orientation) Icons.Filled.Check else null,
                surface = if (entry == orientation) {
                    palette.surfaceAccent
                } else {
                    palette.surfaceDefault
                },
                onClick = { onOrientation(entry) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_clock_size)) }
        items(ClockFormat.SCALES) { wert ->
            BigRow(
                label = "${(wert * 100).toInt()} %",
                icon = if (wert == clockScale) Icons.Filled.Check else null,
                surface = if (wert == clockScale) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onClockScale(wert) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_clock)) }
        items(ClockDisplay.entries.toList()) { entry ->
            BigRow(
                label = stringResource(clockLabel(entry)),
                secondary = if (entry == ClockDisplay.OFF) {
                    stringResource(R.string.clock_off_hint)
                } else {
                    null
                },
                icon = if (entry == clock) Icons.Filled.Check else null,
                surface = if (entry == clock) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onClock(entry) },
            )
        }
    }
}

private fun orientationLabel(orientation: ScreenOrientation): Int = when (orientation) {
    ScreenOrientation.PORTRAIT -> R.string.orientation_portrait
    ScreenOrientation.LANDSCAPE -> R.string.orientation_landscape
    ScreenOrientation.AUTO -> R.string.orientation_auto
}

private fun clockLabel(display: ClockDisplay): Int = when (display) {
    ClockDisplay.OFF -> R.string.clock_off
    ClockDisplay.TIME -> R.string.clock_time
    ClockDisplay.TIME_DATE -> R.string.clock_time_date
    ClockDisplay.TIME_DATE_WEEKDAY -> R.string.clock_time_date_weekday
}

/**
 * Barrierefreiheit. Beide Schalter nehmen dem Langdruck den Editor weg - deshalb steht
 * darunter, wo man ihn dann findet. Eine Einstellung, die einen Weg schliesst, muss den
 * neuen Weg nennen.
 */
@Composable
private fun AccessibilityList(
    config: Accessibility,
    haptics: HapticStrength,
    confirmMessages: Boolean,
    homeKeyReturns: Boolean,
    swipeScreens: Boolean,
    pressMode: PressMode,
    onTogglePressMode: () -> Unit,
    onToggleSwipe: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleConfirmMessages: () -> Unit,
    onToggleHomeKey: () -> Unit,
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
        item {
            BigRow(
                label = stringResource(
                    when (haptics) {
                        HapticStrength.OFF -> R.string.haptics_off
                        HapticStrength.LIGHT -> R.string.haptics_light
                        HapticStrength.STRONG -> R.string.haptics_strong
                    }
                ),
                secondary = stringResource(R.string.haptics_hint),
                surface = if (haptics != HapticStrength.OFF) {
                    palette.surfaceAccent
                } else {
                    palette.surfaceDefault
                },
                onClick = onToggleHaptics,
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (confirmMessages) {
                        R.string.confirm_messages_on
                    } else {
                        R.string.confirm_messages_off
                    }
                ),
                secondary = stringResource(R.string.confirm_messages_hint),
                surface = if (confirmMessages) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleConfirmMessages,
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (homeKeyReturns) R.string.home_key_on else R.string.home_key_off,
                ),
                secondary = stringResource(R.string.home_key_hint),
                surface = if (homeKeyReturns) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleHomeKey,
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (pressMode == PressMode.LONG) R.string.press_long else R.string.press_short,
                ),
                secondary = stringResource(R.string.press_hint),
                surface = if (pressMode == PressMode.LONG) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onTogglePressMode,
            )
        }
        item {
            BigRow(
                label = stringResource(if (swipeScreens) R.string.swipe_on else R.string.swipe_off),
                secondary = stringResource(R.string.swipe_hint),
                surface = if (swipeScreens) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleSwipe,
            )
        }
        if (LongPress.needsEditModeEntry(config, pressMode)) {
            item {
                Text(
                    text = stringResource(R.string.a11y_editor_moved),
                    color = palette.danger,
                    fontSize = bigSp(15f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * Notruf einrichten. Die Nummern stehen in einer Zeile, weil das auf drei Zoll schneller
 * geht als eine Liste mit Plus-Knopf - und weil [SosNumbers] beim Einlesen streng aussortiert,
 * kostet die Bequemlichkeit nichts.
 */
@Composable
private fun SosSettings(
    config: SosConfig,
    onNumbers: (String) -> Unit,
    onMessage: (String) -> Unit,
    onCountdown: (Int) -> Unit,
    onToggleLocation: () -> Unit,
    locationGranted: Boolean,
    locationBlocked: Boolean,
    onAskLocation: () -> Unit,
    onLocationSettings: () -> Unit,
    onToggleAlarmSound: () -> Unit,
    onToggleAlarmFlash: () -> Unit,
    onTryAlarm: () -> Unit,
    /** Zeigt den Notrufbildschirm als Probe - siehe SosActivity.EXTRA_PREVIEW. */
    onPreview: () -> Unit,
    alarmRunning: Boolean,
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

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { BigHeading(stringResource(R.string.sos)) }
        item {
            Text(
                text = stringResource(R.string.sos_explainer),
                color = palette.onBackground,
                fontSize = bigSp(16f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }

        item { BigHeading(stringResource(R.string.sos_numbers)) }
        item {
            OutlinedTextField(
                value = numbersText,
                onValueChange = { numbersText = it },
                singleLine = false,
                textStyle = TextStyle(fontSize = bigSp(20f), fontWeight = FontWeight.Bold),
                // Der Hinweis gehoert ans Feld, nicht an den Knopf: am Knopf stand er in
                // einer Zeile, die abgeschnitten wurde, und ein leerer Kasten sagt nichts.
                placeholder = { Text(stringResource(R.string.sos_numbers_placeholder), fontSize = bigSp(17f)) },
                supportingText = {
                    Text(stringResource(R.string.sos_numbers_hint, SosNumbers.MAX), fontSize = bigSp(15f))
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (rejected.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.sos_numbers_rejected, rejected.joinToString(", ")),
                    color = palette.danger,
                    fontSize = bigSp(15f),
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
                textStyle = TextStyle(fontSize = bigSp(18f)),
                placeholder = { Text(defaultMessage, fontSize = bigSp(17f)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            // Mit der laengsten Zeile gerechnet, die dazukommen kann: seit die Nachricht
            // das Alter eines alten Standorts nennt, waere die Zahl sonst im schlechten
            // Fall um eine SMS zu niedrig - und zu niedrig ist bei Kosten die falsche
            // Richtung.
            val laengstesAlter = pluralStringResource(R.plurals.sos_location_age_hours, 24, 24)
            val preview = SosMessage.compose(
                text = messageText,
                latitude = 48.20849,
                longitude = 16.37208,
                fallback = defaultMessage,
                ageNote = laengstesAlter,
            )
            BigRow(
                label = stringResource(R.string.sos_message_save),
                // Vorschau mit Beispielkoordinaten: der Nutzer soll sehen, was ankommt,
                // und wie viele SMS es kostet.
                secondary = pluralStringResource(
                    R.plurals.sos_message_parts,
                    SosMessage.partsNeeded(preview),
                    SosMessage.partsNeeded(preview),
                ),
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
                    pluralStringResource(R.plurals.sos_countdown_seconds, seconds, seconds)
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

        // PLAN.md 4.8: lauter Alarmton und blinkendes Licht. Sie wirken ohne Netz und
        // erreichen den, der zwei Raeume weiter steht - die Nachricht erreicht den nicht.
        item { BigHeading(stringResource(R.string.sos_alarm_heading)) }
        item {
            BigRow(
                label = stringResource(
                    if (config.alarmSound) R.string.sos_alarm_sound_on else R.string.sos_alarm_sound_off,
                ),
                secondary = stringResource(R.string.sos_alarm_sound_hint),
                surface = if (config.alarmSound) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleAlarmSound,
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (config.alarmFlash) R.string.sos_alarm_flash_on else R.string.sos_alarm_flash_off,
                ),
                surface = if (config.alarmFlash) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleAlarmFlash,
            )
        }
        // Ausprobieren, bevor es zaehlt: wer den Alarm im Notfall zum ersten Mal hoert,
        // erschrickt und drueckt ihn weg. Der Knopf loest **keinen** Notruf aus, es geht
        // dabei keine Nachricht hinaus.
        if (config.alarmSound || config.alarmFlash) {
            item {
                BigRow(
                    label = stringResource(
                        if (alarmRunning) R.string.sos_alarm_stop else R.string.sos_alarm_try,
                    ),
                    secondary = stringResource(R.string.sos_alarm_try_hint),
                    icon = if (alarmRunning) Icons.Filled.StopCircle else Icons.Filled.PlayCircle,
                    surface = if (alarmRunning) palette.surfaceAccent else palette.surfaceDefault,
                    onClick = onTryAlarm,
                )
            }
        }

        // Den Ablauf einmal ansehen, ohne dass etwas hinausgeht. Wer den Notruf einrichtet,
        // will ihn dem Menschen erklaeren koennen, der ihn spaeter im Ernst drueckt - und
        // eine Erklaerung, die man zeigen kann, ist besser als eine, die man liest.
        item {
            BigRow(
                label = stringResource(R.string.sos_preview),
                secondary = stringResource(R.string.sos_preview_hint),
                icon = Icons.Filled.PlayCircle,
                onClick = onPreview,
            )
        }

        // Der Schalter steht auf "mit Standort", das Recht fehlt: dann geht die Nachricht
        // ohne Koordinaten hinaus. Das gehoert hier hingeschrieben, nicht erst im Notfall
        // gemerkt.
        if (config.sendLocation && !locationGranted) {
            item {
                Text(
                    text = stringResource(R.string.sos_location_missing),
                    color = palette.danger,
                    fontSize = bigSp(15f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            item {
                BigRow(
                    label = stringResource(
                        if (locationBlocked) {
                            // Die beiden Texte gehören zum Zugriffs-Baustein und liegen
                            // deshalb im Design-System; `nonTransitiveRClass` heisst,
                            // dass man sie dort auch ansprechen muss.
                            UiR.string.permission_open_settings
                        } else {
                            UiR.string.permission_allow
                        },
                    ),
                    surface = palette.surfaceAccent,
                    onClick = if (locationBlocked) onLocationSettings else onAskLocation,
                )
            }
        }
    }
}

/**
 * Sicherung und Wiederherstellung. Gedacht fuer den Wechsel auf ein anderes Telefon,
 * deshalb ueber den System-Dateidialog: die Datei soll dort liegen, wo der Nutzer sie
 * auch wiederfindet, nicht in einem App-Verzeichnis, das beim Deinstallieren verschwindet.
 */
@Composable
private fun TransferList(onExport: () -> Unit, onImport: () -> Unit) {
    val palette = LocalBigPalette.current
    // Einlesen ersetzt die ganze Belegung, und zwar unwiderruflich. Dieselbe zweistufige
    // Rueckfrage wie beim Verkleinern des Rasters: der erste Tipp warnt, der zweite tut es.
    var armed by remember { mutableStateOf(false) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_transfer)) }
        item {
            Text(
                text = stringResource(R.string.transfer_explainer),
                color = palette.onBackground,
                fontSize = bigSp(16f),
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
                secondary = stringResource(
                    if (armed) R.string.transfer_import_confirm else R.string.transfer_import_hint,
                ),
                icon = Icons.Filled.FolderOpen,
                surface = if (armed) palette.surfaceDanger else palette.surfaceDefault,
                onClick = {
                    if (armed) {
                        armed = false
                        onImport()
                    } else {
                        armed = true
                    }
                },
            )
        }
    }
}

/**
 * Ausgeblendete Apps wieder einblenden. Ohne diese Seite waere das Ausblenden eine
 * Einbahnstrasse - eine Aktion ohne Rueckweg ist ein Fehler, auch wenn sie tut, was sie soll.
 */
@Composable
private fun HiddenAppsList(
    hidden: Set<String>,
    recentCount: Int,
    onRecentCount: (Int) -> Unit,
    labelFor: (String) -> String,
    onShowAgain: (String) -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_app_list)) }
        // Die Zahl stand fest auf vier, waehrend die App zwoelf Starts speichert. Wer
        // viele verschiedene Apps benutzt, sieht den fuenften nie wieder.
        item { BigHeading(stringResource(R.string.apps_recent_count)) }
        items(AppDrawer.RECENT_CHOICES) { anzahl ->
            BigRow(
                label = if (anzahl == 0) {
                    stringResource(R.string.apps_recent_none_choice)
                } else {
                    pluralStringResource(R.plurals.apps_recent_count_value, anzahl, anzahl)
                },
                surface = if (anzahl == recentCount) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onRecentCount(anzahl) },
            )
        }
        item { BigHeading(stringResource(R.string.settings_hidden_apps)) }
        if (hidden.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.hidden_apps_none),
                    color = palette.onBackground,
                    fontSize = bigSp(16f),
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
                fontSize = bigSp(16f),
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
private fun SecurityList(
    hasPin: Boolean,
    protectsEditor: Boolean,
    onToggleEditorProtection: () -> Unit,
    protectsAppList: Boolean,
    onToggleAppListProtection: () -> Unit,
    protectsCallLog: Boolean,
    onToggleCallLogProtection: () -> Unit,
    lockOthers: Boolean,
    wouldAllow: Int,
    allowedApps: Int,
    onToggleAppLock: () -> Unit,
    onAllowedApps: () -> Unit,
    onSetPin: () -> Unit,
    onRemovePin: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_security)) }
        item {
            Text(
                text = stringResource(R.string.security_explainer),
                color = palette.onBackground,
                fontSize = bigSp(16f),
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
                    label = stringResource(
                        if (protectsEditor) R.string.security_editor_on else R.string.security_editor_off,
                    ),
                    secondary = stringResource(R.string.security_editor_hint),
                    surface = if (protectsEditor) palette.surfaceAccent else palette.surfaceDefault,
                    onClick = onToggleEditorProtection,
                )
            }
            item {
                BigRow(
                    label = stringResource(
                        if (protectsAppList) R.string.security_apps_on else R.string.security_apps_off,
                    ),
                    secondary = stringResource(R.string.security_apps_hint),
                    surface = if (protectsAppList) palette.surfaceAccent else palette.surfaceDefault,
                    onClick = onToggleAppListProtection,
                )
            }
            item {
                BigRow(
                    label = stringResource(
                        if (protectsCallLog) {
                            R.string.security_calllog_on
                        } else {
                            R.string.security_calllog_off
                        },
                    ),
                    secondary = stringResource(R.string.security_calllog_hint),
                    surface = if (protectsCallLog) palette.surfaceAccent else palette.surfaceDefault,
                    onClick = onToggleCallLogProtection,
                )
            }
            item {
                BigRow(
                    label = stringResource(
                        if (lockOthers) R.string.security_applock_on else R.string.security_applock_off,
                    ),
                    // Die Zeile darunter sagte fest "Apps auf Deinen Kacheln sind von
                    // Anfang an erlaubt" - auch dann, wenn keine einzige App auf einer
                    // Kachel liegt und der Schalter das Telefon zusperren wuerde.
                    secondary = when {
                        lockOthers -> pluralStringResource(
                            R.plurals.security_applock_allowed,
                            allowedApps,
                            allowedApps,
                        )

                        wouldAllow == 0 -> stringResource(R.string.security_applock_no_tiles)

                        else -> pluralStringResource(
                            R.plurals.security_applock_would_allow,
                            wouldAllow,
                            wouldAllow,
                        )
                    },
                    surface = if (lockOthers) palette.surfaceAccent else palette.surfaceDefault,
                    onClick = onToggleAppLock,
                )
            }
            // Auch bei ausgeschalteter Sperre erreichbar: sonst laesst sich die Liste erst
            // vorbereiten, wenn man sich schon ausgesperrt hat.
            item {
                BigRow(
                    label = stringResource(R.string.security_allowed_apps),
                    icon = Icons.Filled.Apps,
                    onClick = onAllowedApps,
                )
            }
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
    // Was nach den Systemleisten uebrig bleibt - genau die Flaeche, die eine Kachel
    // bekommt. Das Fenster allein sagte 605 dp Hoehe, tatsaechlich nutzbar sind 581.
    val dichte = LocalDensity.current
    val einblendungen = WindowInsets.safeDrawing
    val nutzbar = run {
        // Die **ganzen** Bildschirmmasse, nicht `displayMetrics`: das liefert das Fenster
        // schon ohne die Gestenleiste, und die ginge dann zweimal ab. Siehe
        // `Diagnostics.usableDp`.
        val ganz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.windowManager.currentWindowMetrics.bounds
                .let { it.width() to it.height() }
        } else {
            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
        Diagnostics.usableDp(
            fullWidthPx = ganz.first,
            fullHeightPx = ganz.second,
            left = einblendungen.getLeft(dichte, LayoutDirection.Ltr),
            top = einblendungen.getTop(dichte),
            right = einblendungen.getRight(dichte, LayoutDirection.Ltr),
            bottom = einblendungen.getBottom(dichte),
            density = dichte.density,
        )
    }
    val zusammenhang = LocalContext.current
    val lines = remember(nutzbar) {
        Diagnostics.collect(activity, nutzbar) { id -> zusammenhang.getString(id) }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_diagnostics)) }
        items(lines) { line ->
            BigRow(label = line.first, secondary = line.second)
        }
    }
}

private fun themeLabel(theme: ThemeName) = when (theme) {
    ThemeName.SYSTEM -> R.string.theme_system
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

/**
 * Warnung, bevor jemand sich selbst aussperrt.
 *
 * Auf dem gewählten Screen liegt keine Einstellungen-Kachel, und von dort führt auch über
 * Sprünge und Ordner keine hin. Nach dem Wechsel käme man nie wieder hierher - es hülfe nur
 * noch ein anderer Launcher oder ein Rechner mit adb.
 *
 * Angeboten wird deshalb der Ausweg statt eines Verbots: eine Einstellungen-Kachel anlegen
 * und dann wechseln. Nur wenn dafür kein Platz ist, geht es wirklich nicht.
 */
@Composable
private fun NoSettingsWarning(
    name: String,
    canAddTile: Boolean,
    onAddAndSwitch: () -> Unit,
    onCancel: () -> Unit,
) {
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(stringResource(R.string.home_switch_warning_title))
        Text(
            text = stringResource(
                if (canAddTile) R.string.home_switch_warning else R.string.home_switch_blocked,
                name,
            ),
            color = palette.danger,
            fontSize = bigSp(16f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (canAddTile) {
            BigRow(
                label = stringResource(R.string.home_switch_add_tile),
                icon = Icons.Filled.Settings,
                surface = palette.surfaceAccent,
                onClick = onAddAndSwitch,
            )
        }
        BigRow(
            label = stringResource(R.string.home_switch_cancel),
            onClick = onCancel,
        )
    }
}

/**
 * Die Rückfrage vor dem einzigen Schritt, der nicht rückgängig zu machen ist.
 *
 * Sie zählt auf, was verschwindet, statt „bist du sicher" zu fragen. Eine Zahl macht eine
 * Warnung wahr; eine Floskel tippt man weg, ohne sie zu lesen. Und der erste Knopf ist
 * nicht das Löschen, sondern die Sicherung - danach ist es kein Verlust mehr, sondern ein
 * Neuanfang.
 */
@Composable
private fun ResetPanel(
    losses: Reset.Losses,
    onBackup: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_reset)) }
        item {
            // Zahlen in Worten, die zur Zahl passen: "1 folders" laesst eine Warnung
            // schlampig aussehen, und wer sie schlampig findet, nimmt sie nicht ernst.
            // Ohne Ordner faellt der halbe Satz ganz weg statt "und 0 Ordner" zu sagen.
            val bildschirme = pluralStringResource(
                R.plurals.reset_screens, losses.screens, losses.screens,
            )
            val kacheln = pluralStringResource(
                R.plurals.reset_tiles, losses.tiles, losses.tiles,
            )
            Text(
                text = if (losses.folders == 0) {
                    stringResource(R.string.reset_losses_plain, bildschirme, kacheln)
                } else {
                    stringResource(
                        R.string.reset_losses,
                        bildschirme,
                        kacheln,
                        pluralStringResource(
                            R.plurals.reset_folders, losses.folders, losses.folders,
                        ),
                    )
                },
                color = palette.onBackground,
                fontSize = bigSp(17f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
        if (losses.hasPin) {
            item {
                Text(
                    text = stringResource(R.string.reset_pin_too),
                    color = palette.onBackground,
                    fontSize = bigSp(17f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
        item {
            BigRow(
                label = stringResource(R.string.reset_backup_first),
                surface = palette.surfaceAccent,
                onClick = onBackup,
            )
        }
        item { BigRow(label = stringResource(R.string.dialog_cancel), onClick = onCancel) }
        item {
            BigRow(
                label = stringResource(R.string.reset_do),
                surface = palette.surfaceDanger,
                onClick = onReset,
            )
        }
    }
}

/**
 * Die Reihenfolge beim Wischen und für die Kacheln „nächster" und „voriger".
 *
 * Bewegt wird eine Stelle nach oben oder unten, nicht gezogen. Ziehen setzt eine ruhige
 * Hand voraus, und die ist bei den Leuten, für die diese App gebaut ist, nicht
 * vorauszusetzen. Zweimal tippen bringt denselben Screen zwei Stellen weiter.
 */
@Composable
private fun SwipeOrderList(
    screens: List<Screen>,
    excluded: List<Screen>,
    mayLeave: (String) -> Boolean,
    onUp: (String) -> Unit,
    onDown: (String) -> Unit,
    onExclude: (String) -> Unit,
    onInclude: (String) -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.swipe_order)) }
        item {
            Text(
                text = stringResource(R.string.swipe_order_explainer),
                color = palette.onBackground,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
        itemsIndexed(screens, key = { _, screen -> screen.id }) { index, screen ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val darfRaus = mayLeave(screen.id)
                BigRow(
                    label = screen.name,
                    // Warum es nicht geht, statt eines Knopfes, der nichts tut: ohne
                    // Sprungkachel waere der Screen nach dem Herausnehmen unauffindbar.
                    secondary = if (darfRaus) {
                        stringResource(R.string.swipe_order_position, index + 1, screens.size)
                    } else {
                        stringResource(R.string.swipe_order_needed)
                    },
                    secondaryMaxLines = 2,
                    modifier = Modifier.weight(1f),
                    onClick = { if (darfRaus) onExclude(screen.id) },
                )
                // Am Rand kein Knopf statt eines Knopfes, der nichts tut: ein Pfeil, der
                // manchmal wirkt und manchmal nicht, laesst einen an sich selbst zweifeln.
                if (index > 0) {
                    BigIconButton(
                        icon = Icons.Filled.KeyboardArrowUp,
                        description = stringResource(R.string.swipe_order_up, screen.name),
                        onClick = { onUp(screen.id) },
                    )
                }
                if (index < screens.lastIndex) {
                    BigIconButton(
                        icon = Icons.Filled.KeyboardArrowDown,
                        description = stringResource(R.string.swipe_order_down, screen.name),
                        onClick = { onDown(screen.id) },
                    )
                }
            }
        }
        if (excluded.isNotEmpty()) {
            item { BigHeading(stringResource(R.string.swipe_order_outside)) }
            items(excluded, key = { it.id }) { screen ->
                BigRow(
                    label = screen.name,
                    secondary = stringResource(R.string.swipe_order_add_back),
                    icon = Icons.Filled.Add,
                    onClick = { onInclude(screen.id) },
                )
            }
        }
    }
}

/**
 * Eine Zeile der Rasterauswahl - Vorlage, Spalte oder Zeile, immer dieselbe Warnung.
 *
 * Ein Raster zu wechseln kann Kacheln kosten; wie viele, steht in der Zeile, und beim
 * ersten Tipp passiert noch nichts. Wer eine andere Zeile antippt, nimmt die scharfe
 * Warnung wieder zurueck - sonst bliebe irgendwo eine rote Zeile stehen, die beim
 * naechsten Tipp ungefragt Kacheln kostet.
 */
@Composable
private fun GridChoiceRow(
    label: String,
    cols: Int,
    rows: Int,
    screen: Screen,
    confirming: Pair<Int, Int>?,
    onArm: (Pair<Int, Int>?) -> Unit,
    onGrid: (Int, Int) -> Unit,
) {
    val palette = LocalBigPalette.current
    val current = cols == screen.cols && rows == screen.rows
    val loses = ScreenEdits.dropped(screen, cols, rows).size
    val armed = confirming == (cols to rows)
    BigRow(
        label = label,
        secondary = when {
            current -> stringResource(R.string.screen_grid_current)
            armed -> stringResource(R.string.screen_grid_confirm)
            loses > 0 -> pluralStringResource(R.plurals.screen_grid_loses, loses, loses)
            else -> null
        },
        icon = if (current) Icons.Filled.Check else null,
        surface = when {
            current -> palette.surfaceAccent
            armed -> palette.surfaceDanger
            else -> palette.surfaceDefault
        },
        onClick = {
            when {
                current -> Unit
                loses == 0 -> { onGrid(cols, rows); onArm(null) }
                armed -> { onGrid(cols, rows); onArm(null) }
                else -> onArm(cols to rows)
            }
        },
    )
}

/**
 * Welche Apps ohne PIN starten. PLAN.md 4.5.
 *
 * Die ganze Liste, mit Haken an den erlaubten - und nicht nur die erlaubten. Wer eine App
 * freigeben will, muss sie finden koennen; eine Liste, die nur zeigt, was schon erlaubt
 * ist, waere fuer genau diesen Schritt nutzlos.
 */
@Composable
private fun AllowedAppsList(
    repository: AppRepository,
    allowed: Set<String>,
    onToggle: (String) -> Unit,
) {
    val palette = LocalBigPalette.current
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = repository.loadApps() }
    val shown = remember(apps, query) { TextSearch.filter(apps, query) { it.label } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(stringResource(R.string.security_allowed_apps))
        Text(
            text = stringResource(R.string.security_allowed_hint),
            color = palette.onBackground,
            fontSize = bigSp(15f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        BigSearchField(
            value = query,
            onValueChange = { query = it },
            hint = stringResource(R.string.search_apps),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(shown, key = { AppDrawer.keyOf(it) }) { app ->
                val schluessel = AppDrawer.keyOf(app)
                val erlaubt = schluessel in allowed || app.packageName in allowed
                BigRow(
                    label = app.label,
                    icon = if (erlaubt) Icons.Filled.Check else Icons.Filled.Lock,
                    surface = if (erlaubt) palette.surfaceAccent else palette.surfaceDefault,
                    onClick = { onToggle(schluessel) },
                )
            }
        }
    }
}

/**
 * Die drei Schalter der Kontaktliste. PLAN.md 4.1 nennt sie nicht eigens, aber sie standen
 * im Modell und wurden gelesen - zwei davon waren nirgends zu aendern.
 *
 * Die Sortierung steht zusaetzlich als Symbol in der Kontaktliste selbst, wo man sie
 * braucht. Hier steht sie, weil man sie hier sucht, wenn man das Symbol nicht erkannt hat.
 */
@Composable
private fun ContactsSettingsList(
    contacts: ContactsConfig,
    onToggleSort: () -> Unit,
    onToggleSearchNumbers: () -> Unit,
    onToggleFavouritesFirst: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_contacts)) }
        item {
            BigRow(
                label = stringResource(
                    if (contacts.sortBySurname) {
                        R.string.contacts_sort_surname
                    } else {
                        R.string.contacts_sort_first
                    },
                ),
                secondary = stringResource(R.string.contacts_sort_hint),
                onClick = onToggleSort,
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (contacts.searchNumbers) {
                        R.string.contacts_search_numbers_on
                    } else {
                        R.string.contacts_search_numbers_off
                    },
                ),
                secondary = stringResource(R.string.contacts_search_numbers_hint),
                surface = if (contacts.searchNumbers) {
                    palette.surfaceAccent
                } else {
                    palette.surfaceDefault
                },
                onClick = onToggleSearchNumbers,
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (contacts.favouritesFirst) {
                        R.string.contacts_favourites_first_on
                    } else {
                        R.string.contacts_favourites_first_off
                    },
                ),
                secondary = stringResource(R.string.contacts_favourites_first_hint),
                surface = if (contacts.favouritesFirst) {
                    palette.surfaceAccent
                } else {
                    palette.surfaceDefault
                },
                onClick = onToggleFavouritesFirst,
            )
        }
    }
}

/**
 * Welche Anrufarten in der Liste erscheinen. PLAN.md 4.6.
 *
 * Die Logik dafuer stand samt Tests im Quelltext und wurde von der App nie aufgerufen -
 * ein Filter ohne Schalter. Wer nur die verpassten sehen will, hat dafuer die schnelle
 * Umschaltung in der Liste selbst; hier steht, was ueberhaupt auftaucht.
 */
@Composable
private fun CallTypesList(
    blocked: List<String>,
    onBlocked: (String) -> Unit,
    onDialerApp: () -> Unit,
    audioRoute: AudioRoute,
    onAudioRoute: (AudioRoute) -> Unit,
    speakerOnOutgoing: Boolean,
    onToggleSpeakerOnOutgoing: () -> Unit,
    photo: CallerPhoto,
    onPhoto: (CallerPhoto) -> Unit,
    grouping: CallGrouping,
    onGrouping: (CallGrouping) -> Unit,
    hidden: Set<String>,
    onToggle: (String) -> Unit,
) {
    val palette = LocalBigPalette.current
    // Einmal gelesen, zweimal gebraucht: fuer den Hinweis und fuer die Zeile darunter.
    val hatTelefonRolle = DialerRole.held(LocalContext.current)
    var gesperrtText by remember(blocked) { mutableStateOf(CallBlocking.format(blocked)) }
    val abgewiesen = remember(gesperrtText) { CallBlocking.rejected(gesperrtText) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Die Anrufliste steht oben, weil der Weg hierher von ihr kommt: aus der Liste
        // fuehrt "welche Arten erscheinen" hierher, und wer dann als Erstes ein Feld fuer
        // gesperrte Nummern sieht, glaubt, auf der falschen Seite gelandet zu sein.
        // Innerhalb davon die Gruppierung vor den Arten: sie betrifft die ganze Liste, das
        // Aus- und Einblenden einzelner Arten nur ihren Inhalt.
        item { BigHeading(stringResource(R.string.call_grouping)) }
        items(CallGrouping.entries.toList()) { art ->
            BigRow(
                label = stringResource(groupingLabel(art)),
                secondary = stringResource(groupingHint(art)),
                surface = if (art == grouping) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onGrouping(art) },
            )
        }
        item { BigHeading(stringResource(R.string.settings_call_types)) }
        item {
            Text(
                text = stringResource(R.string.call_types_hint),
                color = palette.onBackground,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        items(CallDirection.entries.toList()) { art ->
            val sichtbar = art.name !in hidden
            BigRow(
                label = stringResource(callDirectionLabel(art)),
                icon = if (sichtbar) Icons.Filled.Check else null,
                surface = if (sichtbar) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onToggle(art.name) },
            )
        }
        // PLAN.md 4.6: Nummernsperre. Die Liste steht in einer Zeile wie bei den
        // Notrufnummern - auf drei Zoll geht das schneller als eine Liste mit Plus-Knopf,
        // und CallBlocking sortiert beim Einlesen streng aus.
        item { BigHeading(stringResource(R.string.blocked_numbers)) }
        item {
            Text(
                // "werden abgewiesen, ohne zu klingeln" gilt nur, wenn BigLau die
                // Telefon-Rolle haelt - nur die Standard-Telefon-App sieht eingehende
                // Anrufe. Ohne die Rolle wirkt die Sperre allein nach aussen. Siehe
                // DialerRole; am Telefon des Nutzers haelt die Rolle ein anderes Programm.
                text = if (hatTelefonRolle) {
                    stringResource(R.string.blocked_numbers_hint)
                } else {
                    stringResource(R.string.blocked_numbers_hint_outgoing)
                },
                color = palette.onBackground,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        // Den Weg anbieten, nicht nur den Grund nennen: ohne die Telefon-Rolle wirkt die
        // Sperre halb, und die Rolle ist zwei Bildschirme weit weg. Die Zeile steht nur da,
        // solange sie fehlt.
        if (!hatTelefonRolle) {
            item {
                BigRow(
                    label = stringResource(R.string.blocked_numbers_take_role),
                    icon = Icons.Filled.Call,
                    onClick = onDialerApp,
                )
            }
        }
        item {
            OutlinedTextField(
                value = gesperrtText,
                onValueChange = { gesperrtText = it },
                placeholder = { Text(stringResource(R.string.blocked_numbers_placeholder), fontSize = bigSp(15f)) },
                textStyle = LocalTextStyle.current.copy(fontSize = bigSp(17f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        }
        if (abgewiesen.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.blocked_numbers_rejected, abgewiesen.joinToString(", ")),
                    color = palette.danger,
                    fontSize = bigSp(15f),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        item {
            BigRow(
                label = stringResource(R.string.blocked_numbers_save),
                surface = palette.surfaceAccent,
                onClick = { onBlocked(gesperrtText) },
            )
        }
        // PLAN.md 4.6: Standard-Audioausgabe und Lautsprecher bei abgehenden Anrufen.
        item { BigHeading(stringResource(R.string.call_audio)) }
        items(AudioRoute.entries.toList()) { weg ->
            BigRow(
                label = stringResource(audioLabel(weg)),
                surface = if (weg == audioRoute) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onAudioRoute(weg) },
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (speakerOnOutgoing) R.string.call_speaker_out_on else R.string.call_speaker_out_off,
                ),
                secondary = stringResource(R.string.call_speaker_out_hint),
                surface = if (speakerOnOutgoing) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleSpeakerOnOutgoing,
            )
        }
        // PLAN.md 4.6: Kontaktfoto beim Anruf.
        item { BigHeading(stringResource(R.string.caller_photo)) }
        item {
            Text(
                text = stringResource(R.string.caller_photo_hint),
                color = palette.onBackground,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        items(CallerPhoto.entries.toList()) { groesse ->
            BigRow(
                label = stringResource(photoLabel(groesse)),
                surface = if (groesse == photo) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onPhoto(groesse) },
            )
        }
    }
}

private fun audioLabel(weg: AudioRoute): Int = when (weg) {
    AudioRoute.EARPIECE -> R.string.call_audio_earpiece
    AudioRoute.SPEAKER -> R.string.call_audio_speaker
    AudioRoute.BLUETOOTH -> R.string.call_audio_bluetooth
}

private fun photoLabel(groesse: CallerPhoto): Int = when (groesse) {
    CallerPhoto.OFF -> R.string.caller_photo_off
    CallerPhoto.SMALL -> R.string.caller_photo_small
    CallerPhoto.HALF -> R.string.caller_photo_half
    CallerPhoto.FULL -> R.string.caller_photo_full
}

private fun groupingLabel(art: CallGrouping): Int = when (art) {
    CallGrouping.NONE -> R.string.call_grouping_none
    CallGrouping.NUMBER -> R.string.call_grouping_number
    CallGrouping.DIRECTION -> R.string.call_grouping_direction
}

private fun groupingHint(art: CallGrouping): Int = when (art) {
    CallGrouping.NONE -> R.string.call_grouping_none_hint
    CallGrouping.NUMBER -> R.string.call_grouping_number_hint
    CallGrouping.DIRECTION -> R.string.call_grouping_direction_hint
}

private fun callDirectionLabel(direction: CallDirection): Int = when (direction) {
    CallDirection.INCOMING -> R.string.call_type_incoming
    CallDirection.OUTGOING -> R.string.call_type_outgoing
    CallDirection.MISSED -> R.string.call_type_missed
    CallDirection.REJECTED -> R.string.call_type_rejected
    CallDirection.BLOCKED -> R.string.call_type_blocked
    CallDirection.OTHER -> R.string.call_type_other
}

/** Die Vibrationsdauern in Worten. Siehe [SmsNotifications.VIBRATION_CHOICES]. */
private fun vibrationLabel(dauer: Int): Int = when (dauer) {
    0 -> R.string.sms_vibration_off
    200 -> R.string.sms_vibration_short
    500 -> R.string.sms_vibration_medium
    else -> R.string.sms_vibration_long
}

/**
 * Nachrichten (`PLAN.md` 4.7).
 *
 * Der Filter **verbirgt**, er sperrt nicht: BigLau hält die SMS-Rolle nicht und kann eine
 * Nachricht weder abweisen noch am Speichern hindern. Sie kommt an und liegt in der
 * Datenbank des Systems — sie steht nur nicht in dieser Liste. Genau das sagen die Texte
 * hier auch; eine Sperre, die man für dichter hält, als sie ist, ist gefährlicher als eine,
 * deren Grenze man kennt.
 */
@Composable
private fun MessagesSettingsList(
    confirmSend: Boolean,
    onToggleConfirmSend: () -> Unit,
    sendAbove: Boolean,
    onToggleSendAbove: () -> Unit,
    sendLarge: Boolean,
    onToggleSendLarge: () -> Unit,
    scale: Float,
    onScale: (Float) -> Unit,
    numbers: List<String>,
    words: List<String>,
    onNumbers: (String) -> Unit,
    onWords: (String) -> Unit,
    vibrationMs: Int,
    onVibration: (Int) -> Unit,
    fullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    repeatMinutes: Int,
    onRepeat: (Int) -> Unit,
) {
    val palette = LocalBigPalette.current
    var nummernText by remember(numbers) { mutableStateOf(CallBlocking.format(numbers)) }
    var woerterText by remember(words) { mutableStateOf(SmsFilter.formatWords(words)) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_messages)) }
        // PLAN.md 4.7: die Schriftgroesse im Gespraech ist ausdruecklich getrennt von der
        // globalen. Eine Nachricht liest man am Stueck und aus der Hand.
        item { BigHeading(stringResource(R.string.sms_scale)) }
        items(ConversationText.CHOICES) { wert ->
            BigRow(
                label = "${(wert * 100).toInt()} %",
                surface = if (wert == scale) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onScale(wert) },
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (fullScreen) R.string.sms_fullscreen_on else R.string.sms_fullscreen_off,
                ),
                secondary = stringResource(R.string.sms_fullscreen_hint),
                surface = if (fullScreen) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleFullScreen,
            )
        }
        // PLAN.md 4.7: wiederholte Erinnerung. Eine Meldung, die einmal kommt, verpasst
        // man - wer das Telefon in der Tasche hat, sieht sie sonst erst am Abend.
        item { BigHeading(stringResource(R.string.sms_repeat)) }
        items(SmsReminder.CHOICES) { minuten ->
            BigRow(
                label = if (minuten == 0) {
                    stringResource(R.string.sms_repeat_off)
                } else {
                    pluralStringResource(R.plurals.sms_repeat_minutes, minuten, minuten)
                },
                surface = if (minuten == repeatMinutes) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onRepeat(minuten) },
            )
        }
        // PLAN.md 4.7: Vibrationsdauer. Sie steht im Benachrichtigungskanal, damit die
        // Meldung sich weiter an "Bitte nicht stoeren" haelt - siehe SmsNotifications.
        item { BigHeading(stringResource(R.string.sms_vibration)) }
        items(SmsNotifications.VIBRATION_CHOICES) { dauer ->
            BigRow(
                // Kurz, mittel, lang statt Millisekunden: eine Zahl in ms sagt niemandem,
                // wie sich das anfuehlt, und diese App richtet sich nicht an Techniker.
                label = stringResource(vibrationLabel(dauer)),
                surface = if (dauer == vibrationMs) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onVibration(dauer) },
            )
        }
        // PLAN.md 4.7: Sendeknopf - Position, Groesse, Bestaetigung vor dem Senden.
        item { BigHeading(stringResource(R.string.sms_send_heading)) }
        item {
            BigRow(
                label = stringResource(
                    if (confirmSend) R.string.sms_confirm_on else R.string.sms_confirm_off,
                ),
                secondary = stringResource(R.string.sms_confirm_hint),
                surface = if (confirmSend) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleConfirmSend,
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (sendAbove) R.string.sms_send_above else R.string.sms_send_below,
                ),
                surface = if (sendAbove) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleSendAbove,
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (sendLarge) R.string.sms_send_large else R.string.sms_send_normal,
                ),
                surface = if (sendLarge) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleSendLarge,
            )
        }
        item { BigHeading(stringResource(R.string.sms_filter_numbers_heading)) }
        item {
            Text(
                // Der Satz behauptet etwas ueber dieses Telefon und muss deshalb nachsehen:
                // haelt BigLau die SMS-Rolle, ist "BigLau ist nicht die SMS-App dieses
                // Telefons" schlicht falsch. Am Emulator aufgefallen, wo es die Rolle hat.
                text = if (SmsRepository.get(LocalContext.current).isDefaultSmsApp()) {
                    stringResource(R.string.sms_filter_hint_default)
                } else {
                    stringResource(R.string.sms_filter_hint)
                },
                color = palette.onBackground,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        item { BigHeading(stringResource(R.string.sms_filter_numbers)) }
        item {
            OutlinedTextField(
                value = nummernText,
                onValueChange = { nummernText = it },
                placeholder = { Text(stringResource(R.string.blocked_numbers_placeholder), fontSize = bigSp(15f)) },
                textStyle = LocalTextStyle.current.copy(fontSize = bigSp(17f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.blocked_numbers_save),
                surface = palette.surfaceAccent,
                onClick = { onNumbers(nummernText) },
            )
        }
        item { BigHeading(stringResource(R.string.sms_filter_words)) }
        item {
            OutlinedTextField(
                value = woerterText,
                onValueChange = { woerterText = it },
                placeholder = { Text(stringResource(R.string.sms_filter_words_placeholder), fontSize = bigSp(15f)) },
                textStyle = LocalTextStyle.current.copy(fontSize = bigSp(17f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.blocked_numbers_save),
                surface = palette.surfaceAccent,
                onClick = { onWords(woerterText) },
            )
        }
    }
}
