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
import org.biglau.ui.absorbTouches
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import org.biglau.ui.PermissionState
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import org.biglau.web.LinkTarget
import org.biglau.tiles.TileEditorActivity
import org.biglau.tiles.TileLabel
import org.biglau.a11y.TileMenu
import org.biglau.a11y.LongPress
import org.biglau.a11y.MenuItem
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
     * the launcher runs as singleTask, so starting it from the settings lands in onNewIntent
     * and not in onCreate. the wish has to live here and not in the intent, or the settings
     * would never reach edit mode.
     */
    private val editModeRequest = mutableStateOf(false)

    /**
     * which screen is showing; `null` means the home screen as the config has it.
     *
     * here and not as a `rememberSaveable`: the activity runs as singleTask and is not
     * rebuilt, so a remembered initial value stayed put when another home screen was chosen.
     * and the home gesture should lead out of a side screen.
     */
    private val currentScreen = mutableStateOf<String?>(null)

    /** is the explainer for the read permission open right now? */
    private val phoneStateAsked = mutableStateOf(false)

    /**
     * has the signal permission been refused once, and does android still ask?
     *
     * after the *second* refusal it does not: the call returns at once with nothing to see,
     * the screen simply closed, and the tile kept saying tap to allow. endlessly.
     */
    private val phoneStateDeniedOnce = mutableStateOf(false)
    private val phoneStateCanAskAgain = mutableStateOf(true)

    /** fetches the read permission for the signal tile, nothing more. */
    private val askPhoneState =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                phoneStateDeniedOnce.value = true
                phoneStateCanAskAgain.value =
                    shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE)
            }
        }

    /** the contact whose call-or-write question is open. */
    private val contactChoice = mutableStateOf<ButtonAction.Contact?>(null)

    /** the folder currently open, or `null`. */
    private val openFolder = mutableStateOf<String?>(null)

    /** the enlarged tile label, or `null`. on the activity so [closeOverlays] can clear it. */
    private val popupLabelState = mutableStateOf<String?>(null)

    /** the menu-key list: screen and slot of the tile it is open for. see [closeOverlays]. */
    private val tileMenu = mutableStateOf<Triple<String, Int, Int>?>(null)

    /**
     * the locked app *and the tile it came from*.
     *
     * the tile has to come along: if the app is gone after unlocking, the notice says to
     * reassign the tile, and then the editor should open too.
     *
     * the *action* and not the app, because a shortcut used to start without any question,
     * so the lock could be walked around by putting the app on a tile as a shortcut.
     */
    private data class LockedTap(
        val action: ButtonAction,
        val screenId: String,
        val x: Int,
        val y: Int,
    )

    /**
     * an app waiting for the pin. `PLAN.md` 4.5, see [org.biglau.apps.AppLock]. on the
     * activity so the question survives a trip into another app and back.
     */
    private val lockedApp = mutableStateOf<LockedTap?>(null)

    /**
     * counts every return to this screen. whatever is answered outside the app, such as the
     * default launcher question, has to be read again afterwards.
     */
    private val resumeTick = mutableStateOf(0)

    override fun onDestroy() {
        // speech holds a connection to the system service; without this it would outlive
        // the activity.
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
            // tapping home means the home screen, not whatever happens to lie over it.
            closeOverlays()
            // the screen change is a setting: it sat in the model and was read nowhere, and
            // a switch that does nothing is worse than one that does not exist.
            if (ConfigStore.get(this).current.behaviour.homeKeyReturnsToStart) {
                currentScreen.value = null
            }
        }
    }


    /**
     * clears everything lying over the home screen.
     *
     * there are five overlays: the open folder, the enlarged label, the call-or-write
     * question, the pin lock and the signal permission explainer. home used to clear *only*
     * the folder, and a question that survives the home key is a bolt.
     *
     * right for the pin lock too: it falls away without the app starting, so walking around
     * the lock this way only reaches the home screen.
     */
    private fun closeOverlays() {
        openFolder.value = null
        popupLabelState.value = null
        tileMenu.value = null
        contactChoice.value = null
        lockedApp.value = null
        phoneStateAsked.value = false
    }

    private val widgetHost by lazy { WidgetHostController.get(this) }

    override fun onStart() {
        super.onStart()
        // without listening no widget updates; without stopping they keep running.
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
                        // free the widget ids first, then throw them away, as the reset in
                        // the settings does. without it the host keeps them forever and the
                        // provider app keeps a widget alive that nobody sees.
                        val store = ConfigStore.get(this)
                        val host = WidgetHostController.get(this)
                        Reset.widgetIds(store.current).forEach { host.release(it) }
                        store.update { Reset.fresh() }
                        crashes.clearCrash()
                        recreate()
                    },
                )
            }
            // safe mode counts as a successful start too, or there is no way out.
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
            // rebuild after the home question: android answers whether we hold the role
            // from a cache within the running process.
            val homeRoleAsk = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { recreate() }
            val context = LocalContext.current
            val screenId = currentScreen.value ?: config.homeScreenId
            var editMode by editModeRequest
            var popupLabel by popupLabelState
            val screen = config.screenById(screenId) ?: config.homeScreen
            val counts by NotificationRepository.counts.collectAsStateWithLifecycle()
            // unseen missed calls, counted again on every return: having just read the list
            // should not leave the number on the tile.
            var missed by remember { mutableStateOf(0) }
            // unread messages from the provider; null means may not read, and then the
            // notices stay the answer.
            var unread by remember { mutableStateOf<Int?>(null) }
            LaunchedEffect(resumeTick.value, counts) {
                missed = CallLogRepository.get(context).newMissedCount(config.phone.lastSeenMissedAt)
                val sms = SmsRepository.get(context)
                unread = if (sms.hasReadPermission()) sms.unreadCount() else null
            }
            // read again on every return: the default app can have changed meanwhile.
            val systemPackages = remember(counts) { SystemPackagesReader.read(context) }
            val battery by remember { BatteryRepository.readings(context) }
                .collectAsStateWithLifecycle(initialValue = null)
            // read only: the flow listens to telephony and registers nothing.
            val signal by remember { SignalRepository.readings(context) }
                .collectAsStateWithLifecycle(initialValue = null)

            // a launcher must not treat back like an ordinary app: on the home screen it
            // does nothing, on a side screen it leads home, or a screen without a home tile
            // would be a dead end.
            //
            // always enabled rather than overriding onBackPressed, which would swallow the
            // event before this handler sees it.
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

            // we got this far: the start counts as successful.
            LaunchedEffect(Unit) { crashes.noteRendered() }

            // the long press and the menu-key list carry out the same actions and only
            // decide differently which: the long press through LongPress.decide, the list
            // through TileMenu, which shows all three. so the doing stands here once.
            val perform: (org.biglau.data.Screen, Int, Int, List<LongPressAction>) -> Unit =
                { shown, x, y, actions ->
                    val cell = shown.cellAt(x, y)
                    actions.forEach { action ->
                        when (action) {
                            // the long press starts the tile, for hands that would
                            // otherwise trigger something by brushing.
                            LongPressAction.ACTIVATE -> cell?.let { hit ->
                                activate(hit, shown.id, apps) { target ->
                                    currentScreen.value = target
                                }
                            }
                            // the second assignment: the same doing as a short press, with
                            // the other action.
                            LongPressAction.SECOND_ACTION -> cell?.button?.longPress?.let { second ->
                                activate(
                                    cell.copy(button = cell.button.copy(action = second)),
                                    shown.id,
                                    apps,
                                ) { target -> currentScreen.value = target }
                            }
                            LongPressAction.EDIT -> context.startActivity(
                                TileEditorActivity.intent(context, shown.id, x, y),
                            )
                            LongPressAction.SPEAK -> Speaker.say(
                                context,
                                labelAt(config, shown.id, x, y, apps),
                                // the language comes from here; the speaker should not have
                                // to look for it. see Speaker.
                                AppLocale.localeFor(config.appearance.language)
                                    ?: Locale.getDefault(),
                            )
                            LongPressAction.POPUP -> popupLabel =
                                labelAt(config, shown.id, x, y, apps)
                            LongPressAction.NOTHING -> Unit
                        }
                    }
                }

            // written once, used twice: for the screen and for the folder over it. a second
            // copied call is where the two would drift apart on the next change.
            val showTile: @Composable (
                org.biglau.data.Screen, Modifier, Boolean, FocusRequester?, FocusRequester?,
            ) -> Unit =
                { shown, gestalt, onTop, strip, back ->
                    HomeScreenView(
                        screen = shown,
                        active = onTop,
                        below = strip,
                        gridAnchor = back,
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
                        missedCalls = if (config.behaviour.blinkOnNotification) missed else 0,
                        unreadMessages =
                            if (config.behaviour.blinkOnNotification) unread else null,
                        systemPackages = systemPackages,
                        battery = battery,
                        signal = signal,
                        // in edit mode a short tap already opens the editor: a long press
                        // must not be a precondition, or whoever cannot manage one could
                        // never change their tiles.
                        onActivate = { cell ->
                            when {
                                editMode -> context.startActivity(
                                    TileEditorActivity.intent(context, shown.id, cell.x, cell.y),
                                )
                                // having chosen the long press means wanting nothing from a
                                // short one, or the setting would have no effect.
                                config.behaviour.pressMode == PressMode.LONG -> Unit
                                else -> activate(cell, shown.id, apps) { currentScreen.value = it }
                            }
                        },
                        editMode = editMode,
                        onEdit = { x, y ->
                            perform(
                                shown, x, y,
                                LongPress.decide(
                                    config.behaviour.accessibility,
                                    editMode,
                                    config.behaviour.pressMode,
                                    hasSecondAction = shown.cellAt(x, y)?.button?.longPress != null,
                                ),
                            )
                        },
                        // `PLAN.md` 10.3.4: the menu key opens the list instead of doing
                        // something at once. the long press can do only one of three, and the
                        // settings decide which; by key the other two would be out of reach.
                        onMenu = { x, y -> tileMenu.value = Triple(shown.id, x, y) },
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
                val folder = openFolder.value?.let { id ->
                    config.screens.firstOrNull { it.id == id && it.isFolder }
                }
                // what is covered does not exist for the screen reader either.
                //
                // the overlays lie as siblings over the home screen, and the tiles beneath
                // stayed in the accessibility tree: with a folder open, six app tiles were
                // still in the node dump. someone listening would walk through tiles they
                // cannot see and open an app that is not there.
                val label = popupLabel
                val pending = lockedApp.value
                val asking = contactChoice.value
                // the list of overlays, in one place. it answers two questions at once:
                // what the screen reader may no longer see, and who gets the keys. those
                // turned out to be the same list.
                val covered = folder != null ||
                    label != null ||
                    phoneStateAsked.value ||
                    pending != null ||
                    tileMenu.value != null ||
                    asking != null
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(LocalBigPalette.current.background)
                        .safeDrawingPadding()
                        .then(if (covered) Modifier.clearAndSetSemantics {} else Modifier),
                ) {
                    // ask again on every return: tapping the banner picks BigLau in the
                    // system dialog and comes back, and a banner still standing there reads
                    // as a failure.
                    if (!remember(resumeTick.value) { isDefaultHome() }) {
                        HomeRolePrompt {
                            val intentToHome = Intents.homeRoleIntent(this@MainActivity)
                            if (intentToHome != null) {
                                homeRoleAsk.launch(intentToHome)
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
                    // swiping is a setting and off by default (`PLAN.md` 3.2). the edge
                    // strips stay with the back gesture.
                    val density = LocalDensity.current
                    val swipe = if (!config.behaviour.swipeBetweenScreens) {
                        Modifier
                    } else {
                        Modifier.pointerInput(screenId, config.screens.size) {
                            var startX = 0f
                            var travelled = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { point ->
                                    startX = with(density) { point.x.toDp().value }
                                    travelled = 0f
                                },
                                onDragEnd = {
                                    val width = with(density) { size.width.toDp().value }
                                    when (SwipeGesture.decide(startX, travelled, width)) {
                                        SwipeGesture.Direction.NEXT ->
                                            ScreenOrder.next(config, screenId)?.let { currentScreen.value = it }
                                        SwipeGesture.Direction.PREVIOUS ->
                                            ScreenOrder.previous(config, screenId)?.let { currentScreen.value = it }
                                        SwipeGesture.Direction.NONE -> Unit
                                    }
                                },
                            ) { _, amount ->
                                travelled += with(density) { amount.toDp().value }
                            }
                        }
                    }
                    // the home screen gets the keys only while nothing lies over it, the
                    // same list as for the screen reader. no strip under the home screen:
                    // there the bottom really is the end.
                    showTile(
                        screen, Modifier.fillMaxSize().then(swipe), !covered, null, null,
                    )
                }

                if (label != null) {
                    LabelPopup(label) { popupLabel = null }
                }

                if (folder != null) {
                    FolderOverlay(
                        name = folder.name,
                        onClose = { openFolder.value = null },
                        // otherwise the strip lies in the covered column below: from an
                        // open folder into the settings, tap change tiles, back into the
                        // folder, and nothing said the next tap opens the editor. it did,
                        // and the way out sits on that same hidden strip.
                        banner = if (editMode) {
                            { EditModeBanner { editMode = false } }
                        } else {
                            null
                        },
                    ) { strip, back ->
                        showTile(folder, Modifier.fillMaxSize(), true, strip, back)
                    }
                }

                tileMenu.value?.let { (screenId, x, y) ->
                    val menuScreen = config.screens.firstOrNull { it.id == screenId }
                    val cell = menuScreen?.cellAt(x, y)
                    if (menuScreen == null) {
                        tileMenu.value = null
                    } else {
                        FolderOverlay(
                            name = labelAt(config, screenId, x, y, apps),
                            onClose = { tileMenu.value = null },
                            closeLabel = R.string.dialog_close,
                        ) { strip, back ->
                            // `PLAN.md` 10.3.5: while the list is open the focus lies in
                            // it. without this the d-pad walked it into the home screen
                            // underneath, invisible below the list.
                            val menuItems = TileMenu.items(
                                hasSecondAction = cell?.button?.longPress != null,
                            )
                            val anchors = remember(menuItems) { menuItems.map { FocusRequester() } }
                            var at by remember(screenId, x, y) { mutableStateOf(0) }
                            LaunchedEffect(screenId, x, y) {
                                runCatching { anchors.first().requestFocus() }
                            }
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    // every direction key is consumed, even when nothing
                                    // moves: otherwise compose picks a target itself, and the
                                    // next one lies under the list.
                                    .onPreviewKeyEvent { event ->
                                        if (event.type != KeyEventType.KeyDown) {
                                            false
                                        } else {
                                            when (event.key) {
                                                Key.DirectionDown -> {
                                                    if (at < menuItems.lastIndex) {
                                                        at += 1
                                                        anchors[at].requestFocus()
                                                    } else {
                                                        // under the last entry sits the
                                                        // strip that closes the list.
                                                        runCatching { strip.requestFocus() }
                                                    }
                                                    true
                                                }
                                                Key.DirectionUp -> {
                                                    if (at > 0) {
                                                        at -= 1
                                                        anchors[at].requestFocus()
                                                    }
                                                    true
                                                }
                                                Key.DirectionLeft, Key.DirectionRight -> true
                                                else -> false
                                            }
                                        }
                                    },
                            ) {
                                for ((index, menuItem) in menuItems.withIndex()) {
                                    BigRow(
                                        label = stringResource(
                                            when (menuItem) {
                                                MenuItem.SECOND_ACTION -> R.string.key_menu_second
                                                MenuItem.EDIT -> R.string.editor_title
                                                MenuItem.SPEAK -> R.string.a11y_speak_off
                                                MenuItem.SHOW_LARGE -> R.string.a11y_popup_off
                                            },
                                        ),
                                        modifier = Modifier
                                            .focusRequester(anchors[index])
                                            .then(
                                                if (at == index) {
                                                    Modifier.focusRequester(back)
                                                } else {
                                                    Modifier
                                                },
                                            )
                                            .onFocusChanged { if (it.isFocused) at = index },
                                        onClick = {
                                            tileMenu.value = null
                                            perform(
                                                menuScreen, x, y,
                                                listOf(
                                                    when (menuItem) {
                                                        MenuItem.SECOND_ACTION ->
                                                            LongPressAction.SECOND_ACTION
                                                        MenuItem.EDIT -> LongPressAction.EDIT
                                                        MenuItem.SPEAK -> LongPressAction.SPEAK
                                                        MenuItem.SHOW_LARGE -> LongPressAction.POPUP
                                                    },
                                                ),
                                            )
                                        },
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }

                if (phoneStateAsked.value) {
                    SignalPermissionExplainer(
                        blocked = PermissionState.blocked(
                            phoneStateDeniedOnce.value,
                            phoneStateCanAskAgain.value,
                        ),
                        onAsk = {
                            phoneStateAsked.value = false
                            askPhoneState.launch(Manifest.permission.READ_PHONE_STATE)
                        },
                        onSettings = {
                            phoneStateAsked.value = false
                            Intents.appSettings(this@MainActivity)
                        },
                        onDismiss = { phoneStateAsked.value = false },
                    )
                }

                if (pending != null) {
                    PinGate(
                        title = stringResource(R.string.applock_locked),
                        explainer = stringResource(R.string.applock_locked_hint),
                        wrongText = stringResource(R.string.security_wrong_pin),
                        confirmLabel = stringResource(R.string.editor_done),
                        onCheck = { entered -> Pin.verify(entered, config.security.pin) },
                        onAccept = {
                            lockedApp.value = null
                            startAction(pending.action, pending.screenId, pending.x, pending.y, apps)
                        },
                        acceptOnComplete = true,
                    )
                    // back closes the question instead of falling out of the home screen.
                    BackHandler { lockedApp.value = null }
                    return@BigLauTheme
                }

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

    /** what stands on the tile: the same source for speech, popup and display. */
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

    /** which screen is showing, for next and previous. */
    private fun currentScreenId(): String =
        currentScreen.value ?: ConfigStore.get(this).current.homeScreenId

    /**
     * does this action need the pin?
     *
     * an app and a shortcut ask the same lock, since a shortcut leads into the same app.
     * only the key differs in precision: the app names its activity, the shortcut has none.
     */
    private fun isLocked(action: ButtonAction): Boolean {
        val (paket, schluessel) = when (action) {
            is ButtonAction.App -> action.packageName to "${action.packageName}/${action.activityName}"
            is ButtonAction.Shortcut -> action.packageName to action.packageName
            else -> return false
        }
        return AppLock.needsPin(ConfigStore.get(this).current, schluessel, paket)
    }

    /**
     * starts an app or a shortcut, and says so when there is nothing left.
     *
     * the notice says to reassign the tile, so the editor stands right behind it: the way
     * instead of directions to it. without the notice one taps a tile that simply does
     * nothing and thinks the phone is broken.
     *
     * [screenId] must be the *shown* screen, not [currentScreenId]: a folder lies over the
     * home screen without changing it, so a dead tile at (1,2) inside a folder opened the
     * editor on (1,2) of the home screen, which is the tile that opens the folder. anyone
     * following the invitation overwrote their folder instead of the broken tile.
     */
    private fun startAction(
        action: ButtonAction,
        screenId: String,
        x: Int,
        y: Int,
        apps: AppRepository,
    ) {
        val worked = when (action) {
            is ButtonAction.App -> apps.launch(action.packageName, action.activityName)
            is ButtonAction.Shortcut ->
                ShortcutRepository.get(this).launch(action.packageName, action.shortcutId)
            else -> true
        }
        if (worked) return
        Notice.show(
            this,
            if (action is ButtonAction.Shortcut) R.string.shortcut_gone else R.string.app_gone,
        )
        startActivity(TileEditorActivity.intent(this, screenId, x, y))
    }

    private fun activate(
        cell: Cell,
        screenId: String,
        apps: AppRepository,
        goToScreen: (String) -> Unit,
    ) {
        // every screen change closes the folder first, or the screen changes *behind* the
        // overlay and nothing is seen: a go-to-screen tile inside a folder left the folder
        // open and looked inert. holds for home, next and previous too.
        val switchScreen: (String) -> Unit = { target ->
            openFolder.value = null
            goToScreen(target)
        }
        when (val action = cell.button.action) {
            is ButtonAction.App, is ButtonAction.Shortcut ->
                if (isLocked(action)) {
                    lockedApp.value = LockedTap(action, screenId, cell.x, cell.y)
                } else {
                    startAction(action, screenId, cell.x, cell.y, apps)
                }

            is ButtonAction.Contact -> when (action.mode) {
                ContactMode.CALL -> Intents.call(this, action.number)
                ContactMode.SMS -> Intents.sms(this, action.number)
                // not ACTION_DIAL: that passed the question to the system, which asks
                // something else entirely, namely which app takes over dialling.
                ContactMode.ASK -> contactChoice.value = action
            }

            is ButtonAction.GoToScreen -> switchScreen(action.screenId)
            // a folder does not change the screen, it lies over it, so a state of its own.
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
                // fetch the read permission only: the tile then shows the signal, it dials
                // nothing and registers nowhere.
                //
                // explain first, then ask: android files this read permission under making
                // and managing calls, which sounds like something else entirely.
                Builtin.SIGNAL -> if (!SignalRepository.hasPermission(this)) {
                    phoneStateAsked.value = true
                }
                Builtin.FLASHLIGHT -> ToggleActions.run(this, ToggleKind.FLASHLIGHT)
                Builtin.WIFI -> ToggleActions.run(this, ToggleKind.WIFI)
                Builtin.BLUETOOTH -> ToggleActions.run(this, ToggleKind.BLUETOOTH)
                Builtin.AIRPLANE -> ToggleActions.run(this, ToggleKind.AIRPLANE)
                Builtin.RINGER -> ToggleActions.run(this, ToggleKind.RINGER)
                Builtin.SOS -> startActivity(Intent(this, SosActivity::class.java))
                Builtin.HOME_SCREEN -> switchScreen(ConfigStore.get(this).current.homeScreenId)
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
                // no else: a new entry must force a decision. next and previous screen sat
                // silently in the else branch and reported coming soon.
                Builtin.NEXT_SCREEN -> ScreenOrder.next(ConfigStore.get(this).current, currentScreenId())
                    ?.let { switchScreen(it) }
                Builtin.PREV_SCREEN -> ScreenOrder.previous(ConfigStore.get(this).current, currentScreenId())
                    ?.let { switchScreen(it) }
            }

            // a widget serves itself; tapping the cell does nothing here.
            is ButtonAction.Widget -> Unit

            ButtonAction.None -> Unit
        }
    }

    /**
     * a launcher is only useful once it gets the home key.
     *
     * `PackageManager.resolveActivity` is no good for this: without a preference set, it
     * returns the calling app itself on the jelly 2 while the home key lands elsewhere. the
     * role query is reliable; below it the preferred-activity comparison stays.
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

/** a visible sign that editing is on, or the taps are a surprise. */
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

/** the label, large over the whole screen; a tap closes it again. */
@Composable
private fun LabelPopup(label: String, onDismiss: () -> Unit) {
    val palette = LocalBigPalette.current
    // this surface has exactly one action: go away. so *every* key closes it, and the hint
    // says so. `tools/unerreichbar.py` reported one clickable area and none reached; back
    // did close it, but nothing said so, and the text offered a tap. on the very screen made
    // for someone who cannot read the label otherwise.
    val anchors = remember { FocusRequester() }
    LaunchedEffect(label) { runCatching { anchors.requestFocus() } }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .focusRequester(anchors)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    onDismiss()
                    true
                } else {
                    false
                }
            }
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
                text = stringResource(R.string.popup_close_any_key),
                color = palette.onBackground.copy(alpha = 0.75f),
                fontSize = org.biglau.ui.dpSp(16f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * call or write? the question a contact tile asks in the ask-every-time mode.
 *
 * two whole rows and not a system dialog: that one asks something else (which app does it
 * at all), its buttons are small, and it looks different on every phone. a tap beside it
 * closes, so the question is not a bolt.
 */
@Composable
private fun ContactChoice(
    name: String,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalBigPalette.current
    // two rows, and neither was reachable by key: three clickable areas, *none* reached.
    // the same reason as the folder and the enlarged label - a surface that comes on top
    // later does not take the focus by itself, and without focus no key handler runs.
    //
    // unlike the enlarged label, *not* every key closes here: this is a question with two
    // answers, and reading it by key means being able to choose between them.
    val anchors = remember { List(2) { FocusRequester() } }
    var at by remember(name) { mutableStateOf(0) }
    LaunchedEffect(name) { runCatching { anchors.first().requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when (event.key) {
                        Key.DirectionDown -> {
                            if (at < anchors.lastIndex) {
                                at += 1
                                anchors[at].requestFocus()
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (at > 0) {
                                at -= 1
                                anchors[at].requestFocus()
                            }
                            true
                        }
                        Key.DirectionLeft, Key.DirectionRight -> true
                        // the way out, and it has to stand here.
                        //
                        // the home screen's `BackHandler` clears this question, and that was
                        // the only way out by key. but once something in here has the focus,
                        // back no longer arrives there: without the focus request it closed,
                        // with it the question stayed. a `BackHandler` of its own did not
                        // help either, the key being consumed in the focus tree.
                        //
                        // that is the other side of making an overlay usable at all: whoever
                        // takes the focus takes over the exit.
                        Key.Back -> {
                            onDismiss()
                            true
                        }
                        else -> false
                    }
                }
            }
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
                modifier = Modifier
                    .focusRequester(anchors[0])
                    .onFocusChanged { if (it.isFocused) at = 0 },
                onClick = onCall,
            )
            BigRow(
                label = stringResource(R.string.contacts_action_sms),
                icon = Icons.AutoMirrored.Filled.Message,
                modifier = Modifier
                    .focusRequester(anchors[1])
                    .onFocusChanged { if (it.isFocused) at = 1 },
                onClick = onSms,
            )
            Text(
                text = stringResource(R.string.tap_to_close),
                color = palette.onBackground.copy(alpha = 0.75f),
                fontSize = org.biglau.ui.dpSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

/**
 * an open folder: the name above, its tile grid below, and a strip at the bottom to close.
 *
 * opaque and filling the screen rather than a floating window: on three inches a window with
 * a margin is either tiny or has no margin, and then it is no window. this way the tiles
 * inside get exactly the same area, and the same target, as on the home screen.
 */
@Composable
private fun FolderOverlay(
    name: String,
    onClose: () -> Unit,
    banner: (@Composable () -> Unit)? = null,
    /**
     * what stands on the strip below. close folder by default, since that is what the frame
     * is built for; the menu-key list uses the same frame and is no folder.
     */
    closeLabel: Int = R.string.folder_close,
    content: @Composable (FocusRequester, FocusRequester) -> Unit,
) {
    val palette = LocalBigPalette.current
    // the two ways between the content and the strip: the content sends the focus down to
    // here, the strip sends it back up.
    val belowAnchor = remember { FocusRequester() }
    val backAnchor = remember { FocusRequester() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .absorbTouches()
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
        banner?.invoke()
        Box(Modifier.weight(1f)) { content(belowAnchor, backAnchor) }
        // the back gesture closes it too. the strip is for everyone who does not use it,
        // and it says what it does instead of showing a cross.
        BigRow(
            label = stringResource(closeLabel),
            icon = Icons.Filled.Close,
            modifier = Modifier
                .focusRequester(belowAnchor)
                // the strip consumes the direction keys like the grid above it, for the
                // same reason: one press to the right and the focus was *gone*, since compose
                // then searches and finds nothing, the row spanning the full width. the same
                // fault the whole folder had, one row smaller.
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        false
                    } else {
                        when (event.key) {
                            Key.DirectionUp -> {
                                runCatching { backAnchor.requestFocus() }
                                true
                            }
                            Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight -> true
                            else -> false
                        }
                    }
                },
            onClick = onClose,
        )
    }
}

/**
 * explains the read permission before android asks for it.
 *
 * android files `READ_PHONE_STATE` under making and managing calls, the heading of a whole
 * group, which sounds like far more than is needed here: BigLau wants the number of bars
 * and nothing else. seeing the system dialog unwarned, one rightly refuses.
 */
@Composable
private fun SignalPermissionExplainer(
    blocked: Boolean,
    onAsk: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalBigPalette.current
    // two clickable rows, *none* reached: neither answer could be selected by key, a
    // question without an answer.
    //
    // `focusGroup` alone was not enough - the focus sat on the first row and did not move,
    // because the search for the next target ran under the overlay. so named anchors and an
    // arithmetic of its own, as in the tile list and the contact choice. `Key.Back` stands
    // here because whoever takes the focus takes over the exit.
    val anchors = remember { List(2) { FocusRequester() } }
    var at by remember(blocked) { mutableStateOf(0) }
    LaunchedEffect(blocked) { runCatching { anchors.first().requestFocus() } }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .absorbTouches()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when (event.key) {
                        Key.Back -> {
                            onDismiss()
                            true
                        }
                        Key.DirectionDown -> {
                            if (at < anchors.lastIndex) {
                                at += 1
                                runCatching { anchors[at].requestFocus() }
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (at > 0) {
                                at -= 1
                                runCatching { anchors[at].requestFocus() }
                            }
                            true
                        }
                        Key.DirectionLeft, Key.DirectionRight -> true
                        else -> false
                    }
                }
            }
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
            // the stronger sentence promises something about *this* device, so it is
            // checked: holding the phone role means holding CALL_PHONE, and the promise
            // would be false.
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
        // once android stops asking, the button leads into the system settings, the only
        // place left to change the decision. `PermissionGate` has had the same grip all
        // along; this screen was the one that did not use it.
        if (blocked) {
            Text(
                text = stringResource(org.biglau.core.ui.R.string.permission_blocked),
                color = palette.dangerText,
                fontSize = org.biglau.ui.dpSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            BigRow(
                label = stringResource(org.biglau.core.ui.R.string.permission_open_settings),
                surface = palette.surfaceAccent,
                modifier = Modifier
                    .focusRequester(anchors[0])
                    .onFocusChanged { if (it.isFocused) at = 0 },
                onClick = onSettings,
            )
        } else {
            BigRow(
                label = stringResource(R.string.signal_permission_ask),
                surface = palette.surfaceAccent,
                modifier = Modifier
                    .focusRequester(anchors[0])
                    .onFocusChanged { if (it.isFocused) at = 0 },
                onClick = onAsk,
            )
        }
        BigRow(
            label = stringResource(R.string.signal_permission_no),
            modifier = Modifier
                .focusRequester(anchors[1])
                .onFocusChanged { if (it.isFocused) at = 1 },
            onClick = onDismiss,
        )
    }
}
