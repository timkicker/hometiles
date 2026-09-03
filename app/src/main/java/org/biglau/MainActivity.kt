package org.biglau

import android.app.role.RoleManager
import android.content.ComponentName
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Close
import org.biglau.apps.AppLock
import org.biglau.security.Pin
import org.biglau.sms.SmsRepository
import org.biglau.ui.bigSp
import org.biglau.ui.PinGate
import org.biglau.ui.SystemBarsEffect
import java.util.Locale
import org.biglau.ui.AppLocale
import org.biglau.ui.BigLauActivity
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
import org.biglau.data.PressMode
import org.biglau.data.Cell
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import org.biglau.data.ConfigStore
import org.biglau.data.LauncherConfig
import org.biglau.safety.CrashGuard
import org.biglau.settings.Reset
import org.biglau.safety.CrashRecorder
import org.biglau.safety.EmergencyScreen
import org.biglau.safety.StartMode
import org.biglau.contacts.ContactsActivity
import org.biglau.data.ContactMode
import org.biglau.info.BatteryRepository
import org.biglau.info.SignalRepository
import org.biglau.notify.NotificationRepository
import org.biglau.phone.CallLogRepository
import org.biglau.phone.DialerActivity
import org.biglau.toggles.SosActivity
import org.biglau.ui.Notice
import org.biglau.wizard.WizardActivity
import org.biglau.wizard.WizardSteps
import org.biglau.toggles.ToggleActions
import org.biglau.toggles.ToggleKind
import org.biglau.notify.SystemPackagesReader
import org.biglau.settings.SettingsActivity
import org.biglau.sms.SmsActivity
import org.biglau.widgets.WidgetHostController
import org.biglau.shortcuts.ShortcutRepository
import org.biglau.tiles.ScreenOrder
import org.biglau.tiles.SwipeGesture
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import org.biglau.web.LinkTarget
import org.biglau.tiles.TileEditorActivity
import org.biglau.tiles.TileLabel
import org.biglau.a11y.LongPress
import org.biglau.a11y.LongPressAction
import org.biglau.a11y.Speaker
import org.biglau.ui.HomeHeader
import org.biglau.ui.labelRes
import org.biglau.ui.HomeScreenView
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

class MainActivity : BigLauActivity() {

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

    /** Steht die Erklaerung zur Leseberechtigung gerade offen? */
    private val phoneStateAsked = mutableStateOf(false)

    /** Holt die Leseerlaubnis fuer die Empfangskachel - mehr nicht. */
    private val askPhoneState =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** Der Kontakt, bei dem gerade "anrufen oder schreiben?" offen steht. */
    private val contactChoice = mutableStateOf<ButtonAction.Contact?>(null)

    /** Der gerade geoeffnete Ordner, oder `null`. */
    private val openFolder = mutableStateOf<String?>(null)

    /**
     * Die gesperrte App **und die Kachel, von der sie kam**.
     *
     * Die Kachel muss mit: ist die App nach dem Entsperren verschwunden, sagt die Meldung
     * „Kachel neu belegen" - und dann soll der Editor auch aufgehen, genau wie beim Tipp
     * ohne Sperre. Bis zum 3.9.2026 stand hier nur die App, und der Rat blieb an dieser
     * einen Stelle im Raum stehen.
     *
     * Seit dem 03.09.2026 steht hier die Aktion und nicht mehr die App: eine
     * **Verknuepfung** startete bis dahin ganz ohne Frage, und damit war die Sperre zu
     * umgehen, indem man die App als Verknuepfung auf eine Kachel legte.
     */
    private data class GesperrterTipp(val action: ButtonAction, val x: Int, val y: Int)

    /**
     * Eine App, die auf die PIN wartet. PLAN.md 4.5 - siehe [org.biglau.apps.AppLock].
     * Auf der Activity und nicht in der Komposition, damit die Frage einen Wechsel in eine
     * andere App und zurueck ueberlebt.
     */
    private val lockedApp = mutableStateOf<GesperrterTipp?>(null)

    /**
     * Zaehlt jede Rueckkehr auf diesen Bildschirm. Womit etwas ausserhalb der App
     * beantwortet wird - die Standard-Launcher-Frage etwa -, muss danach neu gelesen
     * werden; sonst zeigt der Startbildschirm einen Zustand, den es nicht mehr gibt.
     */
    private val resumeTick = mutableStateOf(0)

    override fun onDestroy() {
        // Die Sprachausgabe haelt eine Verbindung zum System-Dienst; ohne dieses Aufraeumen
        // bliebe sie ueber die Lebenszeit der Activity hinaus offen.
        Speaker.shutdown()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        resumeTick.value++
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_EDIT_MODE, false)) {
            editModeRequest.value = true
        } else if (Intent.ACTION_MAIN == intent.action) {
            // Ein offener Ordner schliesst immer: er ist eine Ueberlagerung, und wer heim
            // tippt, will nicht weiter darin stehen.
            openFolder.value = null
            // Der Screenwechsel dagegen ist eine Einstellung. Sie stand bisher im Modell
            // und wurde nirgends gelesen - ein Schalter, der nichts tut, ist schlimmer als
            // einer, den es nicht gibt.
            if (ConfigStore.get(this).current.behaviour.homeKeyReturnsToStart) {
                currentScreen.value = null
            }
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
                        // Erst die Widget-Kennungen freigeben, dann wegwerfen - genau wie
                        // beim Zuruecksetzen in den Einstellungen. Ohne diesen Schritt
                        // hielte der Widget-Host sie fuer immer, und die Anbieter-App
                        // haelt ein Widget am Leben, das niemand mehr sieht. Dass es hier
                        // fehlte, faellt nicht auf: man sieht ja gerade gar nichts.
                        val store = ConfigStore.get(this)
                        val host = WidgetHostController.get(this)
                        Reset.widgetIds(store.current).forEach { host.release(it) }
                        store.update { Reset.fresh() }
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
            SystemBarsEffect(config.appearance.fullScreen)
            // Nach der Startbildschirm-Frage neu aufbauen: ob wir die Rolle halten,
            // beantwortet Android im laufenden Prozess aus dem Zwischenspeicher, und der
            // Balken staende sonst weiter da, obwohl es geklappt hat.
            val homeRoleAsk = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { recreate() }
            val context = LocalContext.current
            val screenId = currentScreen.value ?: config.homeScreenId
            var editMode by editModeRequest
            var popupLabel by remember { mutableStateOf<String?>(null) }
            val screen = config.screenById(screenId) ?: config.homeScreen
            val counts by NotificationRepository.counts.collectAsStateWithLifecycle()
            // Ungesehene verpasste Anrufe, bei jeder Rueckkehr neu gezaehlt: wer die Liste
            // gerade gelesen hat, soll die Zahl nicht weiter auf der Kachel stehen sehen.
            var verpasst by remember { mutableStateOf(0) }
            // Ungelesene Nachrichten aus dem Anbieter; null heisst "darf nicht lesen",
            // dann bleiben die Meldungen die Auskunft.
            var ungelesen by remember { mutableStateOf<Int?>(null) }
            LaunchedEffect(resumeTick.value, counts) {
                verpasst = CallLogRepository.get(context).newMissedCount(config.phone.lastSeenMissedAt)
                val sms = SmsRepository.get(context)
                ungelesen = if (sms.hasReadPermission()) sms.unreadCount() else null
            }
            // Bei jeder Rueckkehr neu lesen: der Nutzer kann die Standard-App
            // zwischendurch in den Systemeinstellungen gewechselt haben.
            val systemPackages = remember(counts) { SystemPackagesReader.read(context) }
            val battery by remember { BatteryRepository.readings(context) }
                .collectAsStateWithLifecycle(initialValue = null)
            // Rein lesend: der Fluss hoert dem Telefoniedienst zu und meldet nichts an.
            val signal by remember { SignalRepository.readings(context) }
                .collectAsStateWithLifecycle(initialValue = null)

            // Ein Launcher darf die Zurueck-Geste nicht wie eine gewoehnliche App behandeln:
            // auf dem Startscreen tut sie nichts. Auf einem Nebenscreen fuehrt sie heim,
            // sonst waere ein Screen ohne Heim-Kachel eine Sackgasse.
            //
            // Bewusst immer aktiv statt onBackPressed zu ueberschreiben - ein solcher
            // Override schluckt das Ereignis, bevor dieser Handler es ueberhaupt sieht.
            BackHandler(enabled = true) {
                when {
                    phoneStateAsked.value -> phoneStateAsked.value = false
                    contactChoice.value != null -> contactChoice.value = null
                    openFolder.value != null -> openFolder.value = null
                    popupLabel != null -> popupLabel = null
                    editMode -> editMode = false
                    screenId != config.homeScreenId -> currentScreen.value = null
                }
            }

            // Wir sind bis hierher gekommen: der Start gilt als geglueckt.
            LaunchedEffect(Unit) { crashes.noteRendered() }

            // Einmal beschrieben, zweimal benutzt: fuer den Screen und fuer den Ordner
            // darueber. Ein zweiter, abgeschriebener Aufruf waere die Stelle, an der die
            // beiden nach der naechsten Aenderung auseinanderlaufen.
            val zeigeKachel: @Composable (org.biglau.data.Screen, Modifier) -> Unit =
                { gezeigt, gestalt ->
                    HomeScreenView(
                        screen = gezeigt,
                        appearance = config.appearance,
                        modifier = gestalt,
                        appIcon = { pkg, act ->
                            apps.iconFor(pkg, act)?.toBitmap(96, 96)?.asImageBitmap()
                        },
                        appLabel = { pkg, act -> apps.labelFor(pkg, act) },
                        shortcutIcon = { pkg, id ->
                            ShortcutRepository.get(context)
                                .iconFor(pkg, id, resources.displayMetrics.densityDpi)
                                ?.toBitmap(96, 96)?.asImageBitmap()
                        },
                        folderOf = { id -> config.screens.firstOrNull { it.id == id && it.isFolder } },
                        notificationCounts =
                            if (config.behaviour.blinkOnNotification) counts else emptyMap(),
                        missedCalls = if (config.behaviour.blinkOnNotification) verpasst else 0,
                        unreadMessages =
                            if (config.behaviour.blinkOnNotification) ungelesen else null,
                        systemPackages = systemPackages,
                        battery = battery,
                        signal = signal,
                        // Im Bearbeitungsmodus oeffnet schon der kurze Tipp den Editor. Ein
                        // langer Druck bleibt dann keine Voraussetzung - wer ihn nicht schafft,
                        // koennte seine Kacheln sonst nie aendern.
                        onActivate = { cell ->
                            when {
                                editMode -> context.startActivity(
                                    TileEditorActivity.intent(context, gezeigt.id, cell.x, cell.y),
                                )
                                // Wer den langen Druck gewaehlt hat, will vom kurzen nichts
                                // ausgeloest bekommen - sonst waere die Einstellung wirkungslos.
                                config.behaviour.pressMode == PressMode.LONG -> Unit
                                else -> activate(cell, apps) { currentScreen.value = it }
                            }
                        },
                        editMode = editMode,
                        onEdit = { x, y ->
                            val zelle = gezeigt.cellAt(x, y)
                            LongPress.decide(
                                config.behaviour.accessibility,
                                editMode,
                                config.behaviour.pressMode,
                                hasSecondAction = zelle?.button?.longPress != null,
                            ).forEach { action ->
                                when (action) {
                                    // Der lange Druck startet die Kachel - fuer Haende, die
                                    // beim Streifen sonst etwas ausloesen wuerden.
                                    LongPressAction.ACTIVATE -> zelle?.let { treffer ->
                                        activate(treffer, apps) { ziel -> currentScreen.value = ziel }
                                    }
                                    // Die Zweitbelegung: dieselbe Ausfuehrung wie beim
                                    // Kurzdruck, nur mit der anderen Aktion.
                                    LongPressAction.SECOND_ACTION -> zelle?.button?.longPress?.let { zweite ->
                                        activate(
                                            zelle.copy(button = zelle.button.copy(action = zweite)),
                                            apps,
                                        ) { ziel -> currentScreen.value = ziel }
                                    }
                                    LongPressAction.EDIT -> context.startActivity(
                                        TileEditorActivity.intent(context, gezeigt.id, x, y),
                                    )
                                    LongPressAction.SPEAK -> Speaker.say(
                                        context,
                                        labelAt(config, gezeigt.id, x, y, apps),
                                        // Die Sprache kommt von hier: der Speaker soll sie
                                        // nicht selbst suchen muessen. Siehe Speaker.
                                        AppLocale.localeFor(config.appearance.language)
                                            ?: Locale.getDefault(),
                                    )
                                    LongPressAction.POPUP -> popupLabel =
                                        labelAt(config, gezeigt.id, x, y, apps)
                                    LongPressAction.NOTHING -> Unit
                                }
                            }
                        },
                    )
                }

            BigLauTheme(
                theme = config.appearance.theme,
                textScale = config.appearance.textScale,
                haptics = config.behaviour.haptics,
                font = config.appearance.font,
                labelScale = config.appearance.labelScale,
                iconPercent = config.appearance.iconPercent,
                icons = config.appearance.icons,
                hideCutLabels = config.appearance.hideCutLabels,
                cornerRadiusDp = config.appearance.cornerRadiusDp,
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(LocalBigPalette.current.background)
                        .safeDrawingPadding(),
                ) {
                    // Bei jeder Rueckkehr neu fragen. Wer den Balken antippt, waehlt
                    // BigLau im Systemdialog und kommt zurueck - stand der Balken dann
                    // immer noch da, haelt er es fuer gescheitert und tippt wieder.
                    if (!remember(resumeTick.value) { isDefaultHome() }) {
                        HomeRolePrompt {
                            val absicht = Intents.homeRoleIntent(this@MainActivity)
                            if (absicht != null) {
                                homeRoleAsk.launch(absicht)
                            } else {
                                Intents.chooseHomeApp(this@MainActivity)
                            }
                        }
                    }
                    if (editMode) {
                        EditModeBanner { editMode = false }
                    }
                    if (config.appearance.showHeader) {
                        HomeHeader(
                            battery = battery,
                            clock = config.appearance.clock,
                            clockScale = config.appearance.clockScale,
                            modifier = Modifier.padding(
                                start = 8.dp,
                                end = 8.dp,
                                top = 4.dp,
                                bottom = 4.dp,
                            ),
                        )
                    }
                    // Wischen ist eine Einstellung und standardmaessig aus - siehe
                    // PLAN.md 3.2. Die Kantenstreifen bleiben der Zurueck-Geste.
                    val dichte = LocalDensity.current
                    val wischen = if (!config.behaviour.swipeBetweenScreens) {
                        Modifier
                    } else {
                        Modifier.pointerInput(screenId, config.screens.size) {
                            var startX = 0f
                            var strecke = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { punkt ->
                                    startX = with(dichte) { punkt.x.toDp().value }
                                    strecke = 0f
                                },
                                onDragEnd = {
                                    val breite = with(dichte) { size.width.toDp().value }
                                    when (SwipeGesture.decide(startX, strecke, breite)) {
                                        SwipeGesture.Direction.NEXT ->
                                            ScreenOrder.next(config, screenId)?.let { currentScreen.value = it }
                                        SwipeGesture.Direction.PREVIOUS ->
                                            ScreenOrder.previous(config, screenId)?.let { currentScreen.value = it }
                                        SwipeGesture.Direction.NONE -> Unit
                                    }
                                },
                            ) { _, betrag ->
                                strecke += with(dichte) { betrag.toDp().value }
                            }
                        }
                    }
                    zeigeKachel(screen, Modifier.fillMaxSize().then(wischen))
                }

                val label = popupLabel
                if (label != null) {
                    LabelPopup(label) { popupLabel = null }
                }

                val ordner = openFolder.value?.let { id ->
                    config.screens.firstOrNull { it.id == id && it.isFolder }
                }
                if (ordner != null) {
                    FolderOverlay(
                        name = ordner.name,
                        onClose = { openFolder.value = null },
                    ) {
                        zeigeKachel(ordner, Modifier.fillMaxSize())
                    }
                }

                if (phoneStateAsked.value) {
                    SignalPermissionExplainer(
                        onAsk = {
                            phoneStateAsked.value = false
                            askPhoneState.launch(Manifest.permission.READ_PHONE_STATE)
                        },
                        onDismiss = { phoneStateAsked.value = false },
                    )
                }

                val wartend = lockedApp.value
                if (wartend != null) {
                    PinGate(
                        title = stringResource(R.string.applock_locked),
                        explainer = stringResource(R.string.applock_locked_hint),
                        wrongText = stringResource(R.string.security_wrong_pin),
                        confirmLabel = stringResource(R.string.editor_done),
                        onCheck = { eingabe -> Pin.verify(eingabe, config.security.pin) },
                        onAccept = {
                            lockedApp.value = null
                            starten(wartend.action, wartend.x, wartend.y, apps)
                        },
                        acceptOnComplete = true,
                    )
                    // Zurueck schliesst die Frage, statt aus dem Startbildschirm zu fallen.
                    BackHandler { lockedApp.value = null }
                    return@BigLauTheme
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
        return TileLabel.of(
            button,
            words(),
            screenName = { id -> config.screenById(id)?.name },
            appLabel = { a -> apps.labelFor(a.packageName, a.activityName) },
            builtinLabel = { builtin -> getString(builtin.labelRes()) },
        )
    }

    private fun words() = TileLabel.Words(
        emptyTile = getString(R.string.empty_tile),
        folder = getString(R.string.folder),
        nextScreen = getString(R.string.next_screen),
        widget = getString(R.string.editor_pick_widget),
    )

    /** Welcher Screen gerade zu sehen ist - fuer "naechster" und "voriger". */
    private fun currentScreenId(): String =
        currentScreen.value ?: ConfigStore.get(this).current.homeScreenId

    /**
     * Braucht diese Aktion die PIN?
     *
     * App und Verknuepfung fragen dieselbe Sperre - eine Verknuepfung fuehrt in dieselbe
     * App. Der Schluessel unterscheidet sich nur darin, wie genau er zeigt: die App nennt
     * ihre Activity mit, die Verknuepfung hat keine.
     */
    private fun gesperrt(action: ButtonAction): Boolean {
        val (paket, schluessel) = when (action) {
            is ButtonAction.App -> action.packageName to "${action.packageName}/${action.activityName}"
            is ButtonAction.Shortcut -> action.packageName to action.packageName
            else -> return false
        }
        return AppLock.needsPin(ConfigStore.get(this).current, schluessel, paket)
    }

    /**
     * Startet App oder Verknuepfung - und sagt es, wenn nichts mehr da ist.
     *
     * Die Meldung sagt „Kachel neu belegen", also steht der Editor gleich dahinter: der
     * Weg statt der Wegbeschreibung. Wer das nicht will, kommt mit der Zurueck-Geste
     * heraus. Ohne die Meldung tippt man auf eine Kachel, die einfach nichts tut - und
     * haelt das Telefon fuer kaputt.
     */
    private fun starten(action: ButtonAction, x: Int, y: Int, apps: AppRepository) {
        val geklappt = when (action) {
            is ButtonAction.App -> apps.launch(action.packageName, action.activityName)
            is ButtonAction.Shortcut ->
                ShortcutRepository.get(this).launch(action.packageName, action.shortcutId)
            else -> true
        }
        if (geklappt) return
        Notice.show(
            this,
            if (action is ButtonAction.Shortcut) R.string.shortcut_gone else R.string.app_gone,
        )
        startActivity(TileEditorActivity.intent(this, currentScreenId(), x, y))
    }

    private fun activate(cell: Cell, apps: AppRepository, goToScreen: (String) -> Unit) {
        when (val action = cell.button.action) {
            is ButtonAction.App, is ButtonAction.Shortcut ->
                if (gesperrt(action)) {
                    lockedApp.value = GesperrterTipp(action, cell.x, cell.y)
                } else {
                    starten(action, cell.x, cell.y, apps)
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
            // Ein Ordner wechselt den Screen nicht, er legt sich darueber - deshalb ein
            // eigener Zustand und nicht currentScreen. Zurueck schliesst ihn wieder.
            is ButtonAction.Folder -> openFolder.value = action.screenId
            is ButtonAction.Link -> Intents.openLink(this, action.url)

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
                Builtin.CALCULATOR -> Intents.openCalculator(this)
                Builtin.BATTERY -> Unit
                // Nur die Leseerlaubnis holen. Die Kachel zeigt danach den Empfang; sie
                // waehlt nichts und meldet sich nirgends an.
                // Erst erklaeren, dann fragen. Android stellt diese Leseberechtigung unter
                // der Ueberschrift "Anrufe taetigen und verwalten" - das klingt nach etwas
                // ganz anderem, als es ist, und wer das liest, lehnt zu Recht erst einmal ab.
                Builtin.SIGNAL -> if (!SignalRepository.hasPermission(this)) {
                    phoneStateAsked.value = true
                }
                Builtin.FLASHLIGHT -> ToggleActions.run(this, ToggleKind.FLASHLIGHT)
                Builtin.WIFI -> ToggleActions.run(this, ToggleKind.WIFI)
                Builtin.BLUETOOTH -> ToggleActions.run(this, ToggleKind.BLUETOOTH)
                Builtin.AIRPLANE -> ToggleActions.run(this, ToggleKind.AIRPLANE)
                Builtin.RINGER -> ToggleActions.run(this, ToggleKind.RINGER)
                Builtin.SOS -> startActivity(Intent(this, SosActivity::class.java))
                Builtin.HOME_SCREEN -> goToScreen(ConfigStore.get(this).current.homeScreenId)
                Builtin.SETTINGS -> startActivity(Intent(this, SettingsActivity::class.java))
                Builtin.APP_LIST -> startActivity(Intent(this, AppDrawerActivity::class.java))
                Builtin.MOBILE_DATA -> ToggleActions.run(this, ToggleKind.MOBILE_DATA)
                Builtin.LOCATION -> ToggleActions.run(this, ToggleKind.LOCATION)
                Builtin.BRIGHTNESS -> ToggleActions.run(this, ToggleKind.BRIGHTNESS)
                Builtin.ANDROID_SETTINGS -> Intents.androidSettings(this)
                Builtin.FAVOURITES -> startActivity(
                    Intent(this, ContactsActivity::class.java)
                        .putExtra(ContactsActivity.EXTRA_FAVOURITES, true),
                )
                Builtin.RECENT_APPS -> startActivity(
                    Intent(this, AppDrawerActivity::class.java)
                        .putExtra(AppDrawerActivity.EXTRA_RECENT, true),
                )
                Builtin.CALL_LOG -> startActivity(
                    Intent(this, DialerActivity::class.java)
                        .putExtra(DialerActivity.EXTRA_LOG, true),
                )
                // Kein else: ein neuer Eintrag soll den Übersetzer zwingen, sich zu
                // entscheiden. Im else standen bisher stillschweigend "nächster Screen"
                // und "voriger Screen" und meldeten "demnächst".
                Builtin.NEXT_SCREEN -> ScreenOrder.next(ConfigStore.get(this).current, currentScreenId())
                    ?.let { goToScreen(it) }
                Builtin.PREV_SCREEN -> ScreenOrder.previous(ConfigStore.get(this).current, currentScreenId())
                    ?.let { goToScreen(it) }
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
private fun HomeRolePrompt(onClick: () -> Unit) {
    val palette = LocalBigPalette.current
    Text(
        text = stringResource(R.string.set_as_home),
        color = palette.surfaceAccent.ink,
        fontSize = bigSp(18f),
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surfaceAccent.fill)
            .clickable(onClick = onClick)
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
        fontSize = bigSp(16f),
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surfaceAccent.fill)
            .clickable { onLeave() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
            Spacer(Modifier.height(24.dp))
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

/**
 * Ein geöffneter Ordner: der Name oben, darunter sein Kachelraster, und ein Streifen unten
 * zum Schließen.
 *
 * Bewusst deckend und bildschirmfüllend statt als schwebendes Fenster: auf drei Zoll wäre ein
 * Fenster mit Rand entweder winzig oder ohne Rand, und dann ist es kein Fenster mehr. So
 * bekommen die Kacheln darin genau dieselbe Fläche wie auf dem Startbildschirm - und dieselbe
 * Trefferfläche.
 */
@Composable
private fun FolderOverlay(
    name: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val palette = LocalBigPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .safeDrawingPadding()
            .padding(horizontal = 8.dp),
    ) {
        Text(
            text = name,
            color = palette.onBackground,
            fontSize = org.biglau.ui.dpSp(26f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
        Box(Modifier.weight(1f)) { content() }
        // Die Zurueck-Geste schliesst ihn auch. Der Streifen ist fuer alle da, die sie nicht
        // benutzen - und er sagt, was er tut, statt nur ein Kreuz zu zeigen.
        BigRow(
            label = stringResource(R.string.folder_close),
            icon = Icons.Filled.Close,
            onClick = onClose,
        )
    }
}

/**
 * Erklärt die Leseberechtigung, bevor Android sie erfragt.
 *
 * Android führt `READ_PHONE_STATE` unter „Anrufe tätigen und verwalten". Das ist die
 * Überschrift einer ganzen Gruppe und klingt nach weit mehr, als hier gebraucht wird:
 * BigLau will die Anzahl der Balken wissen und sonst nichts. Wer den Systemdialog ohne
 * Vorwarnung sieht, lehnt zu Recht ab - und hat dann eine Kachel, die nie etwas anzeigt.
 */
@Composable
private fun SignalPermissionExplainer(onAsk: () -> Unit, onDismiss: () -> Unit) {
    val palette = LocalBigPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .safeDrawingPadding()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.signal_permission_title),
            color = palette.onBackground,
            fontSize = org.biglau.ui.dpSp(26f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
        Text(
            // Der Satz "kann es auch nicht: die Berechtigung dafuer hat es nicht" ist eine
            // Zusage ueber dieses Geraet - also wird nachgesehen. Haelt die App die
            // Telefon-Rolle, hat sie CALL_PHONE, und die starke Fassung waere falsch. Am
            // Emulator aufgefallen, wo genau das der Fall ist.
            text = if (
                ContextCompat.checkSelfPermission(LocalContext.current, Manifest.permission.CALL_PHONE) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                stringResource(R.string.signal_permission_body_may_call)
            } else {
                stringResource(R.string.signal_permission_body)
            },
            color = palette.onBackground,
            fontSize = org.biglau.ui.dpSp(16f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
        BigRow(
            label = stringResource(R.string.signal_permission_ask),
            surface = palette.surfaceAccent,
            onClick = onAsk,
        )
        BigRow(
            label = stringResource(R.string.signal_permission_no),
            onClick = onDismiss,
        )
    }
}
