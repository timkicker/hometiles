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
import androidx.compose.material.icons.filled.Sms
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
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
import org.biglau.data.PhoneConfig
import org.biglau.data.Behaviour
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
import org.biglau.toggles.SosSettings
import org.biglau.actions.SosMessage
import org.biglau.data.ClockDisplay
import org.biglau.data.FontChoice
import org.biglau.data.ContactsConfig
import androidx.compose.material.icons.filled.Person
import org.biglau.phone.CallBlocking
import org.biglau.phone.DialerRole
import org.biglau.sms.MessagesSettingsList
import org.biglau.sms.SmsRepository
import org.biglau.phone.CallDirection
import org.biglau.phone.callDirectionLabel
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
            // Fuer Arbeit, die nicht in den Hauptthread gehoert - siehe HauptfadenTest.
            val fadenBereich = rememberCoroutineScope()
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
            // Die Probe des Notruf-Alarms steht seit dem 03.09.2026 in `SosSettings`
            // selbst, samt ihrem `onDispose`. Das ist strenger, nicht lockerer: die Seite
            // liegt **innerhalb** dieser Komposition, ihr Aufraeumen kommt also immer zuerst -
            // und zusaetzlich schon dann, wenn man nur die Unterseite verlaesst.
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
                // Nicht im Hauptthread: das Ziel kann eine Cloud-App sein, und dann geht
                // das Schreiben ins Netz. Siehe HauptfadenTest.
                fadenBereich.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            contentResolver.openOutputStream(uri)?.use { stream ->
                                stream.write(ConfigTransfer.export(store.current).toByteArray())
                            } != null
                        }.getOrDefault(false)
                    }
                    Notice.show(
                        this@SettingsActivity,
                        if (ok) R.string.transfer_exported else R.string.transfer_failed,
                    )
                }
            }

            val importFile = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                // Weiter an `ImportActivity` statt hier einzulesen.
                //
                // Bis zum 04.09.2026 tat diese Stelle es selbst - und dabei weniger: sie
                // ersetzte die ganze Einrichtung, sobald eine Datei gewaehlt war, ohne zu
                // zeigen, was darin steht. `ImportActivity`, die den Weg von aussen
                // bedient (Datei antippen), zeigt genau das und fragt dann. Zwei Wege in
                // dieselbe Sache, zwei verschiedene Antworten auf die Frage, ob gefragt
                // wird - und der haeufigere Weg war der unvorsichtigere.
                //
                // Nebenbei fiel noch etwas weg: hier hiess eine Datei, die sich gar nicht
                // **oeffnen** liess, "Das ist keine BigLau-Sicherung". Drueben stehen
                // dafuer zwei verschiedene Saetze.
                startActivity(
                    Intent(this@SettingsActivity, ImportActivity::class.java)
                        .setData(uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                )
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
                            istStartbildschirm = remember(fortsetzungen.intValue) {
                                Diagnostics.isDefaultHome(this@SettingsActivity)
                            },
                            istTelefonApp = remember(fortsetzungen.intValue) {
                                DialerRole.held(this@SettingsActivity)
                            },
                            istNachrichtenApp = remember(fortsetzungen.intValue) {
                                SmsRepository.get(this@SettingsActivity).isDefaultSmsApp()
                            },
                            onHomeApp = {
                                val absicht = Intents.homeRoleIntent(this@SettingsActivity)
                                if (absicht != null) {
                                    askDialerRole.launch(absicht)
                                } else {
                                    Intents.chooseHomeApp(this@SettingsActivity)
                                }
                            },
                            onSmsApp = {
                                // Wie beim Telefon: haelt BigLau die Rolle schon, fuehrt der
                                // Rollendialog nirgendwohin.
                                val absicht = if (
                                    SmsRepository.get(this@SettingsActivity).isDefaultSmsApp()
                                ) {
                                    null
                                } else {
                                    Intents.smsRoleIntent(this@SettingsActivity)
                                }
                                if (absicht != null) {
                                    askDialerRole.launch(absicht)
                                } else {
                                    Intents.chooseSmsApp(this@SettingsActivity)
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
                            onAddJumpTile = { ziel ->
                                store.update { current ->
                                    ScreenEdits.withJumpTile(current, ziel.id) ?: current
                                }
                            },
                            jumpTilePossible = { ziel ->
                                ScreenEdits.withJumpTile(config, ziel.id) != null
                            },
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

                        // Wie bei den Nachrichten und beim Notruf: die Seite bekommt ihren
                        // Teil und gibt ihn geaendert zurueck. Vorher standen hier 123 Zeilen
                        // mit siebzehn Rueckrufen, die alle dasselbe taten. Hier bleibt nur,
                        // was eine Activity braucht - sich neu aufbauen und sich drehen.
                        Page.APPEARANCE -> AppearanceList(
                            appearance = config.appearance,
                            onChange = { neu -> store.update { it.copy(appearance = neu) } },
                            onLanguageChanged = { recreate() },
                            onOrientationChanged = { requestedOrientation = Orientation.requested(it) },
                        )


                        Page.BEHAVIOUR -> BehaviourList(
                            blinkOn = config.behaviour.blinkOnNotification,
                            // Beim Wiederkommen neu nachsehen - siehe `fortsetzungen`.
                            accessGranted = remember(fortsetzungen.intValue) {
                                NotificationRepository.isEnabled(this@SettingsActivity)
                            },
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

                        // Diese Seite stellt das **Verhalten** ein, also bekommt sie
                        // `behaviour` und gibt es geaendert zurueck. Vorher: 94 Zeilen mit
                        // acht Rueckrufen, jeder eine Kopie einer Kopie.
                        Page.ACCESSIBILITY -> AccessibilityList(
                            behaviour = config.behaviour,
                            onChange = { neu -> store.update { it.copy(behaviour = neu) } },
                        )


                        Page.SOS -> SosSettings(
                            config = config.sos,
                            onChange = { neu -> store.update { it.copy(sos = neu) } },
                            onNeedLocation = {
                                askLocation.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            },
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

                        // Die Seite bekommt ihren Teil der Einstellungen und gibt ihn
                        // geaendert zurueck. Vorher standen hier 56 Zeilen mit fuenfzehn
                        // Rueckrufen, die alle dasselbe taten - und der Wecker fuer die
                        // Erinnerung stand mitten drin, obwohl er zu den Nachrichten gehoert.
                        Page.MESSAGES -> MessagesSettingsList(
                            sms = config.sms,
                            onChange = { neu -> store.update { it.copy(sms = neu) } },
                            istStandardApp = remember(fortsetzungen.intValue) {
                                SmsRepository.get(this@SettingsActivity).isDefaultSmsApp()
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

                        // Fünfte Seite nach demselben Muster. Der Wechsel der
                        // Standard-Telefon-App bleibt hier: dafür braucht es eine Activity,
                        // die auf die Antwort des Systems wartet.
                        Page.CALL_TYPES -> CallTypesList(
                            phone = config.phone,
                            onChange = { neu -> store.update { it.copy(phone = neu) } },
                            hatTelefonRolle = remember(fortsetzungen.intValue) {
                                DialerRole.held(this@SettingsActivity)
                            },
                            onDialerApp = {
                                val absicht = Intents.dialerRoleIntent(this@SettingsActivity)
                                if (absicht != null) {
                                    askDialerRole.launch(absicht)
                                } else {
                                    Intents.chooseDialerApp(this@SettingsActivity)
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
    onSmsApp: () -> Unit,
    istStartbildschirm: Boolean,
    istTelefonApp: Boolean,
    istNachrichtenApp: Boolean,
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
        // Die dritte Rolle stand nirgends. Startbildschirm und Telefon liessen sich hier
        // sehen und aendern, die Nachrichten-Rolle nur im Nachrichten-Bildschirm - und dort
        // nur, solange BigLau sie **nicht** hatte. Wer sie hatte, erfuhr es nirgends und
        // kam von hier aus nicht mehr davon los. Drei Rollen, ein Ort.
        item {
            BigRow(
                label = stringResource(
                    if (istNachrichtenApp) R.string.is_sms else R.string.set_as_sms,
                ),
                secondary = stringResource(
                    if (istNachrichtenApp) R.string.role_change_hint else R.string.set_as_sms_hint,
                ),
                // Nicht dasselbe Symbol wie die Nachrichten-Zeile weiter unten: zwei
                // gleiche Symbole in einer Liste sind zwei Zeilen, die man verwechselt.
                // SlopRulesTest hat es gemeldet.
                icon = Icons.Filled.Sms,
                surface = if (istNachrichtenApp) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onSmsApp,
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
    /** Legt eine Sprungkachel auf den Startbildschirm; null heisst: dort ist kein Platz. */
    onAddJumpTile: (Screen) -> Unit,
    jumpTilePossible: (Screen) -> Boolean,
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
                    color = palette.dangerText,
                    fontSize = bigSp(15f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            // Der Weg dorthin statt der Wegbeschreibung - dieselbe Regel wie beim Notruf
            // ohne Kontakte und bei der Anrufliste. Der Satz darueber sagte bis zum
            // 03.09.2026 nur, was zu tun waere.
            items(unreachable, key = { "sprung-${it.id}" }) { schirm ->
                if (jumpTilePossible(schirm)) {
                    BigRow(
                        label = stringResource(R.string.screens_add_jump, schirm.name),
                        icon = Icons.Filled.Add,
                        surface = palette.surfaceAccent,
                        onClick = { onAddJumpTile(schirm) },
                    )
                } else {
                    Text(
                        text = stringResource(R.string.screens_add_jump_full),
                        color = palette.onBackground,
                        fontSize = bigSp(15f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
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
                    color = palette.dangerText,
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
 * Die Namen der Hintergrundfarben, in der Reihenfolge von `ScreenBackground.choicesFor`.
 *
 * Gesprochen, nicht geschrieben: die Zeile zeigt die Farbe in voller Breite, und das ist
 * fuer das Auge die bessere Auskunft. Wer sie nicht sieht, hoerte bis zum 04.09.2026
 * fuenfmal denselben Satz.
 */
internal val HINTERGRUND_NAMEN = listOf(
    R.string.screen_background_blue,
    R.string.screen_background_violet,
    R.string.screen_background_green,
    R.string.screen_background_red,
    R.string.screen_background_ochre,
)

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
                selected = gewaehlt,
                surface = BigSurface(palette.background, palette.onBackground),
                borderColor = if (gewaehlt) palette.accent else null,
                onClick = { onBackground(Background.Theme) },
            )
        }
        // Jede Zeile in ihrer eigenen Farbe. Bei einer Farbe ist der Name nutzlos - man
        // will sie sehen, und zwar in der Groesse, in der sie spaeter dasteht.
        //
        // **Zu sehen**, und genau da endet das Argument. Am 04.09.2026 am Emulator im
        // Knotenabzug nachgesehen: fuenf Zeilen, fuenfmal "Diese Farbe", kein Wort dazu.
        // Wer die Farbe nicht sieht, hat fuenf gleiche Angebote vor sich. Die Zeile bleibt
        // also, wie sie ist - gesprochen wird der Name der Farbe. Genau dafuer gibt es
        // `labelSpeech`.
        itemsIndexed(hintergrundfarben) { platz, farbe ->
            val gewaehlt = (screen.background as? Background.Solid)?.argb == farbe
            BigRow(
                label = stringResource(R.string.screen_background_colour),
                labelSpeech = stringResource(HINTERGRUND_NAMEN[platz % HINTERGRUND_NAMEN.size]),
                icon = if (gewaehlt) Icons.Filled.Check else null,
                selected = gewaehlt,
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
    Language.FRENCH -> R.string.language_french
    Language.SPANISH -> R.string.language_spanish
    Language.ITALIAN -> R.string.language_italian
}

@Composable
private fun AppearanceList(
    appearance: Appearance,
    onChange: (Appearance) -> Unit,
    /** Nach einem Sprachwechsel baut die Activity sich neu auf. */
    onLanguageChanged: () -> Unit,
    /** Eine Drehung setzt die Activity sofort um. */
    onOrientationChanged: (ScreenOrientation) -> Unit,
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
                icon = if (entry == appearance.language) Icons.Filled.Check else null,
                selected = entry == appearance.language,
                onClick = {
                        if (entry != appearance.language) {
                            onChange(appearance.copy(language = entry))
                            // Sofort neu aufbauen. Wer hier „Deutsch" antippt und nichts
                            // geschieht, tippt noch einmal und noch einmal - und das ist die
                            // Seite, auf der man gerade nicht lesen kann, was los ist.
                            onLanguageChanged()
                        }
                    },
            )
        }
        // Jede Zeile ist in ihrem eigenen Thema gemalt. Dreimal dasselbe Paletten-Symbol
        // sagte nichts; die Farben selbst sagen alles. Die Auswahl traegt deshalb ein
        // Haekchen statt einer Akzentflaeche - die Flaeche gehoert hier dem Thema.
        items(ThemeName.entries.toList()) { entry ->
            val own = paletteFor(entry, isSystemInDarkTheme())
            val chosen = entry == appearance.theme
            BigRow(
                label = stringResource(themeLabel(entry)),
                icon = if (chosen) Icons.Filled.Check else null,
                selected = chosen,
                surface = BigSurface(own.emptyTile, own.onBackground),
                borderColor = if (chosen) palette.accent else null,
                onClick = { onChange(appearance.copy(theme = entry)) },
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
                icon = if (entry == appearance.font) Icons.Filled.Check else null,
                selected = entry == appearance.font,
                fontFamily = familyFor(entry),
                onClick = { onChange(appearance.copy(font = entry)) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_text_size)) }
        // Jede Zeile in ihrer eigenen Groesse: man sieht, was man waehlt, statt es zu lesen.
        items(listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)) { scale ->
            CompositionLocalProvider(LocalTextScale provides scale) {
                BigRow(
                    label = "${(scale * 100).toInt()} %",
                    icon = if (scale == appearance.textScale) Icons.Filled.Check else null,
                    selected = scale == appearance.textScale,
                    onClick = { onChange(appearance.copy(textScale = scale)) },
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
                    icon = if (scale == appearance.labelScale) Icons.Filled.Check else null,
                    selected = scale == appearance.labelScale,
                    onClick = { onChange(appearance.copy(labelScale = scale)) },
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
                        imageVector = if (percent == appearance.iconPercent) {
                            Icons.Filled.Check
                        } else {
                            Icons.Filled.Apps
                        },
                        contentDescription = null,
                        tint = palette.onBackground,
                        modifier = Modifier.size((16 + percent / 2).dp),
                    )
                },
                selected = percent == appearance.iconPercent,
                onClick = { onChange(appearance.copy(iconPercent = percent)) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_label_position)) }
        items(LabelPosition.entries.toList()) { position ->
            BigRow(
                label = stringResource(labelPositionLabel(position)),
                selected = position == appearance.labelPosition,
                onClick = { onChange(appearance.copy(labelPosition = position)) },
            )
        }
        // PLAN.md 3.2: "auf 3 Zoll ist ein abgeschnittenes Wort schlimmer als gar keins."
        // Nur anbieten, wo die Beschriftung ueberhaupt steht - bei "ohne Beschriftung"
        // waere es ein Schalter ohne Wirkung.
        if (appearance.labelPosition != LabelPosition.HIDDEN) {
            item {
                BigRow(
                    label = stringResource(
                        if (appearance.hideCutLabels) R.string.appearance_hide_cut_on else R.string.appearance_hide_cut_off,
                    ),
                    secondary = stringResource(R.string.appearance_hide_cut_hint),
                    checked = appearance.hideCutLabels,
                    onClick = { onChange(appearance.copy(hideCutLabels = !appearance.hideCutLabels)) },
                )
            }
        }
        item {
            BigRow(
                label = stringResource(
                    if (appearance.fullScreen) R.string.appearance_fullscreen_on else R.string.appearance_fullscreen_off,
                ),
                secondary = stringResource(R.string.appearance_fullscreen_hint),
                checked = appearance.fullScreen,
                onClick = { onChange(appearance.copy(fullScreen = !appearance.fullScreen)) },
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
                icon = if (entry == appearance.icons) Icons.Filled.Check else null,
                selected = entry == appearance.icons,
                onClick = { onChange(appearance.withIcons(entry)) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_header)) }
        item {
            BigRow(
                label = stringResource(if (appearance.showHeader) R.string.header_on else R.string.header_off),
                secondary = stringResource(R.string.header_explainer),
                icon = Icons.Filled.Schedule,
                surface = if (appearance.showHeader) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onChange(appearance.copy(showHeader = !appearance.showHeader)) },
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
                    color = palette.dangerText,
                    fontSize = bigSp(15f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
        item { BigHeading(stringResource(R.string.appearance_gutter)) }
        items(GridLooks.GUTTERS) { wert ->
            BigRow(
                label = "$wert dp",
                icon = if (wert == appearance.gutterDp) Icons.Filled.Check else null,
                selected = wert == appearance.gutterDp,
                onClick = { onChange(appearance.copy(gutterDp = GridLooks.gutter(wert))) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_border)) }
        items(GridLooks.BORDERS) { wert ->
            BigRow(
                label = "$wert %",
                icon = if (wert == appearance.safeBorderPercent) Icons.Filled.Check else null,
                selected = wert == appearance.safeBorderPercent,
                onClick = { onChange(appearance.copy(safeBorderPercent = GridLooks.border(wert))) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_corner)) }
        // Jede Zeile in ihrer eigenen Rundung: die Zahl sagt nichts, die Ecke alles.
        items(GridLooks.RADII) { wert ->
            BigRow(
                label = "$wert dp",
                icon = if (wert == appearance.cornerRadiusDp) Icons.Filled.Check else null,
                selected = wert == appearance.cornerRadiusDp,
                cornerRadius = wert.dp,
                onClick = { onChange(appearance.copy(cornerRadiusDp = GridLooks.radius(wert))) },
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
                icon = if (entry == appearance.orientation) Icons.Filled.Check else null,
                selected = entry == appearance.orientation,
                onClick = {
                        onChange(appearance.copy(orientation = entry))
                        // Sofort umsetzen: eine Drehung, die erst beim naechsten Start
                        // kaeme, sieht aus wie ein Schalter, der klemmt.
                        onOrientationChanged(entry)
                    },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_clock_size)) }
        items(ClockFormat.SCALES) { wert ->
            BigRow(
                label = "${(wert * 100).toInt()} %",
                icon = if (wert == appearance.clockScale) Icons.Filled.Check else null,
                selected = wert == appearance.clockScale,
                onClick = { onChange(appearance.copy(clockScale = ClockFormat.scale(wert))) },
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
                icon = if (entry == appearance.clock) Icons.Filled.Check else null,
                selected = entry == appearance.clock,
                onClick = { onChange(appearance.withClock(entry)) },
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
    behaviour: Behaviour,
    onChange: (Behaviour) -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_accessibility)) }
        item {
            BigRow(
                label = stringResource(
                    if (behaviour.accessibility.speakOnLongPress) R.string.a11y_speak_on else R.string.a11y_speak_off,
                ),
                secondary = stringResource(R.string.a11y_speak_hint),
                checked = behaviour.accessibility.speakOnLongPress,
                onClick = {
                    val a = behaviour.accessibility
                    onChange(behaviour.copy(accessibility = a.copy(speakOnLongPress = !a.speakOnLongPress)))
                },
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (behaviour.accessibility.popupOnLongPress) R.string.a11y_popup_on else R.string.a11y_popup_off,
                ),
                secondary = stringResource(R.string.a11y_popup_hint),
                checked = behaviour.accessibility.popupOnLongPress,
                onClick = {
                    val a = behaviour.accessibility
                    onChange(behaviour.copy(accessibility = a.copy(popupOnLongPress = !a.popupOnLongPress)))
                },
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (behaviour.accessibility.scrollButtons) R.string.a11y_scroll_on else R.string.a11y_scroll_off,
                ),
                secondary = stringResource(R.string.a11y_scroll_hint),
                checked = behaviour.accessibility.scrollButtons,
                onClick = {
                    val a = behaviour.accessibility
                    onChange(behaviour.copy(accessibility = a.copy(scrollButtons = !a.scrollButtons)))
                },
            )
        }
        item {
            BigRow(
                label = stringResource(
                    when (behaviour.haptics) {
                        HapticStrength.OFF -> R.string.haptics_off
                        HapticStrength.LIGHT -> R.string.haptics_light
                        HapticStrength.STRONG -> R.string.haptics_strong
                    }
                ),
                secondary = stringResource(R.string.haptics_hint),
                surface = if (behaviour.haptics != HapticStrength.OFF) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onChange(behaviour.withHaptics(Haptics.next(behaviour.haptics))) },
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (behaviour.confirmMessages) {
                        R.string.confirm_messages_on
                    } else {
                        R.string.confirm_messages_off
                    }
                ),
                secondary = stringResource(R.string.confirm_messages_hint),
                surface = if (behaviour.confirmMessages) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onChange(behaviour.copy(confirmMessages = !behaviour.confirmMessages)) },
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (behaviour.homeKeyReturnsToStart) R.string.home_key_on else R.string.home_key_off,
                ),
                secondary = stringResource(R.string.home_key_hint),
                surface = if (behaviour.homeKeyReturnsToStart) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onChange(behaviour.copy(homeKeyReturnsToStart = !behaviour.homeKeyReturnsToStart)) },
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (behaviour.pressMode == PressMode.LONG) R.string.press_long else R.string.press_short,
                ),
                secondary = stringResource(R.string.press_hint),
                surface = if (behaviour.pressMode == PressMode.LONG) palette.surfaceAccent else palette.surfaceDefault,
                onClick = {
                    onChange(
                        behaviour.copy(
                            pressMode = if (behaviour.pressMode == PressMode.SHORT) {
                                PressMode.LONG
                            } else {
                                PressMode.SHORT
                            },
                        ),
                    )
                },
            )
        }
        item {
            BigRow(
                label = stringResource(if (behaviour.swipeBetweenScreens) R.string.swipe_on else R.string.swipe_off),
                secondary = stringResource(R.string.swipe_hint),
                checked = behaviour.swipeBetweenScreens,
                onClick = { onChange(behaviour.copy(swipeBetweenScreens = !behaviour.swipeBetweenScreens)) },
            )
        }
        if (LongPress.needsEditModeEntry(behaviour.accessibility, behaviour.pressMode)) {
            item {
                Text(
                    text = stringResource(R.string.a11y_editor_moved),
                    color = palette.dangerText,
                    fontSize = bigSp(15f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
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
                secondary = stringResource(R.string.transfer_import_hint),
                icon = Icons.Filled.FolderOpen,
                // Kein zweiter Tipp mehr davor. Er sagte "Noch einmal tippen, dann ist
                // alles ersetzt" - und das stimmte nicht: der zweite Tipp oeffnete den
                // Dateidialog. Seit die Datei drueben in `ImportActivity` erst gezeigt und
                // dann gefragt wird, steht die Rueckfrage dort, wo etwas zu sehen ist,
                // und nicht davor, wo sie nur schreckt. Zwei Tipps sind ausserdem genau
                // das, was eine zittrige Hand von selbst macht.
                onClick = onImport,
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
                selected = anzahl == recentCount,
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
                    checked = protectsEditor,
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
// `BigLauActivity` statt `ComponentActivity`: die Diagnose liest Zustaende, die das System
// vergibt, und braucht dafuer `fortsetzungen` - siehe dort. Eine Diagnoseseite, die veraltete
// Werte zeigt, ist schlimmer als keine.
private fun DiagnosticsList(activity: BigLauActivity) {
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
            // `defaultDisplay` ist seit Android 11 abgelöst - dies ist der Zweig für
            // alles davor, der Ersatz steht im if darüber.
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
    // `fortsetzungen` als zweiter Schluessel: die Diagnose liest Rollen und Berechtigungen,
    // die das System vergibt. Wer sie erteilt und zurueckkommt, bekam sonst die alten Werte -
    // auf ausgerechnet der Seite, die man aufschlaegt, um nachzusehen, was stimmt.
    val lines = remember(nutzbar, activity.fortsetzungen.intValue) {
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
            color = palette.dangerText,
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
        selected = current,
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
                    checked = erlaubt,
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
                surface = if (contacts.searchNumbers) palette.surfaceAccent else palette.surfaceDefault,
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
                surface = if (contacts.favouritesFirst) palette.surfaceAccent else palette.surfaceDefault,
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
    phone: PhoneConfig,
    onChange: (PhoneConfig) -> Unit,
    /** Die Standard-Telefon-App zu wechseln geht nur über eine Activity. */
    onDialerApp: () -> Unit,
    /**
     * Haelt BigLau die Telefon-Rolle?
     *
     * Kommt von aussen, weil das **System** sie vergibt: wer sie erteilt und zurueckkommt,
     * soll nicht denselben Hinweis noch einmal lesen. Siehe `BigLauActivity.fortsetzungen`.
     */
    hatTelefonRolle: Boolean,
) {
    val palette = LocalBigPalette.current
    var gesperrtText by remember(phone.blockedNumbers) { mutableStateOf(CallBlocking.format(phone.blockedNumbers)) }
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
                selected = art == phone.callGrouping,
                onClick = { onChange(phone.copy(callGrouping = art)) },
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
            val sichtbar = art.name !in phone.hiddenCallTypes
            BigRow(
                label = stringResource(callDirectionLabel(art)),
                icon = if (sichtbar) Icons.Filled.Check else null,
                checked = sichtbar,
                onClick = {
                    val jetzt = phone.hiddenCallTypes
                    onChange(
                        phone.copy(
                            hiddenCallTypes = if (art.name in jetzt) {
                                jetzt - art.name
                            } else {
                                jetzt + art.name
                            },
                        ),
                    )
                },
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
                // DialerRole. Am 03.09.2026 hielt die Rolle ein anderes Programm; seit dem
                // 04.09.2026 haelt BigLau sie, und damit gilt der erste Satz.
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
                    color = palette.dangerText,
                    fontSize = bigSp(15f),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        item {
            BigRow(
                label = stringResource(R.string.blocked_numbers_save),
                surface = palette.surfaceAccent,
                onClick = { onChange(phone.copy(blockedNumbers = CallBlocking.parse(gesperrtText))) },
            )
        }
        // PLAN.md 4.6: Standard-Audioausgabe und Lautsprecher bei abgehenden Anrufen.
        item { BigHeading(stringResource(R.string.call_audio)) }
        items(AudioRoute.entries.toList()) { weg ->
            BigRow(
                label = stringResource(audioLabel(weg)),
                selected = weg == phone.audioRoute,
                onClick = { onChange(phone.copy(audioRoute = weg)) },
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (phone.speakerOnOutgoing) R.string.call_speaker_out_on else R.string.call_speaker_out_off,
                ),
                secondary = stringResource(R.string.call_speaker_out_hint),
                surface = if (phone.speakerOnOutgoing) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onChange(phone.copy(speakerOnOutgoing = !phone.speakerOnOutgoing)) },
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
                selected = groesse == phone.callerPhoto,
                onClick = { onChange(phone.copy(callerPhoto = groesse)) },
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



