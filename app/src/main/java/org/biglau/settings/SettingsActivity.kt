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
    private val pageRequest = mutableStateOf<Page?>(null)

    private fun readTarget(intent: Intent?): Page? =
        SettingsDeepLink.target(intent?.getStringExtra(EXTRA_PAGE))

    /**
     * android does not change `intent` by itself when the request arrives while the settings
     * are already open, and the earlier page would stay.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readTarget(intent)?.let { pageRequest.value = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pageRequest.value = readTarget(intent)
        val store = ConfigStore.get(this)

        setContent {
            // for work that does not belong on the main thread. see MainThreadTest.
            val scope = rememberCoroutineScope()
            val config by store.config.collectAsStateWithLifecycle()
            val locked = Pin.usable(config.security.pin)
            // rememberSaveable so a language change does not throw one back to the top: it
            // rebuilds the activity, and whoever just picked a language wants to see the tick
            // move.
            //
            // a subpage can be addressed from outside, but the pin comes first: a target in
            // the intent must not open a lock.
            //
            // read from [pageRequest] and not from `intent` directly: the activity is often
            // already in the stack, and then `startActivity` only brings it forward without
            // changing `intent`.
            val target = pageRequest.value
            var page by rememberSaveable { mutableStateOf(SettingsDeepLink.start(locked, target)) }
            // trying out the sos alarm lives in `SosSettings` itself, with its own
            // `onDispose`. that is stricter, not looser: the page lies *inside* this
            // composition, so its clean-up always runs first.
            LaunchedEffect(target) {
                SettingsDeepLink.jump(page, target)?.let { page = it }
            }
            var renaming by remember { mutableStateOf<Screen?>(null) }
            var switching by remember { mutableStateOf<Screen?>(null) }

            // duplicating says afterwards what it did and what it left out: a silent jump
            // to an almost identical screen leaves one guessing whether it worked.
            fun duplicate(screen: Screen) {
                val copyName = getString(R.string.screen_copy_name, screen.name)
                when (val outcome = ScreenCopy.duplicate(store.current, screen.id, copyName)) {
                    is ScreenCopy.Result.Done -> {
                        store.update { outcome.config }
                        val skipped = outcome.skippedWidgets + outcome.skippedFolders
                        Notice.show(
                            this@SettingsActivity,
                            if (skipped == 0) {
                                resources.getQuantityString(
                                    R.plurals.screen_copy_done,
                                    outcome.copied,
                                    copyName,
                                    outcome.copied,
                                )
                            } else {
                                getString(
                                    R.plurals.screen_copy_done_partial,
                                    outcome.copied,
                                    copyName,
                                    outcome.copied,
                                    skipped,
                                )
                            },
                        )
                    }

                    ScreenCopy.Result.NoRoomForJumpTile ->
                        Notice.show(this@SettingsActivity, R.string.screen_copy_no_room)

                    ScreenCopy.Result.NoSuchScreen -> Unit
                }
            }

            // the sos promises with location and checked the right for it, which nobody ever
            // asked for: the message went out without coordinates silently, and that would
            // have been noticed in exactly the case the sos exists for.
            //
            // asked here while setting up, not in an emergency, where a system dialog before
            // sending would cost the second that is missing.
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
            // a role dialog needs a caller, so through a launcher and not `startActivity`,
            // or it aborts before it is seen.
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
                // not on the main thread: the target can be a cloud app, and then the write
                // goes over the network.
                scope.launch {
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
                // handed to `ImportActivity` instead of read here.
                //
                // this place used to do it itself and did less: it replaced the whole setup as
                // soon as a file was picked, without showing what was in it. two ways into the
                // same thing with two different answers to whether one is asked, and the more
                // common way was the less careful one.
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
                            // the lock carries only the way out, not the reasoning: the long
                            // text did not fit three lines and was cut exactly where the way
                            // out stood.
                            explainer = stringResource(R.string.security_forgot),
                            wrongText = stringResource(R.string.security_wrong_pin),
                            confirmLabel = stringResource(R.string.editor_done),
                            onCheck = { entered -> Pin.verify(entered, config.security.pin) },
                            onAccept = { page = target ?: Page.MAIN },
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
                                // deliberately without resetting wizardDone: cancelling the
                                // wizard would otherwise put one back in it on every start.
                                startActivity(Intent(this@SettingsActivity, WizardActivity::class.java))
                            },
                            onDiagnostics = { page = Page.DIAGNOSTICS },
                            onReset = { page = Page.RESET },
                            onContacts = { page = Page.CONTACTS },
                            onCallTypes = { page = Page.CALL_TYPES },
                            onMessages = { page = Page.MESSAGES },
                            isHomeScreen = remember(resumes.intValue) {
                                Diagnostics.isDefaultHome(this@SettingsActivity)
                            },
                            isDialerApp = remember(resumes.intValue) {
                                DialerRole.held(this@SettingsActivity)
                            },
                            isSmsApp = remember(resumes.intValue) {
                                SmsRepository.get(this@SettingsActivity).isDefaultSmsApp()
                            },
                            onHomeApp = {
                                val roleIntent = Intents.homeRoleIntent(this@SettingsActivity)
                                if (roleIntent != null) {
                                    askDialerRole.launch(roleIntent)
                                } else {
                                    Intents.chooseHomeApp(this@SettingsActivity)
                                }
                            },
                            onSmsApp = {
                                // as with the phone: holding the role already, the role
                                // dialog leads nowhere.
                                val roleIntent = if (
                                    SmsRepository.get(this@SettingsActivity).isDefaultSmsApp()
                                ) {
                                    null
                                } else {
                                    Intents.smsRoleIntent(this@SettingsActivity)
                                }
                                if (roleIntent != null) {
                                    askDialerRole.launch(roleIntent)
                                } else {
                                    Intents.chooseSmsApp(this@SettingsActivity)
                                }
                            },
                            onDialerApp = {
                                // holding the role already, the dialog closes at once. then
                                // into the system settings, where it can be given back.
                                val roleIntent = if (DialerRole.held(this@SettingsActivity)) {
                                    null
                                } else {
                                    Intents.dialerRoleIntent(this@SettingsActivity)
                                }
                                if (roleIntent != null) {
                                    askDialerRole.launch(roleIntent)
                                } else {
                                    Intents.chooseDialerApp(this@SettingsActivity)
                                }
                            },
                            onDone = { finish() },
                        )

                        Page.SCREENS -> if (switching != null) {
                            val target = switching!!
                            NoSettingsWarning(
                                name = target.name,
                                canAddTile = ScreenEdits.withSettingsTile(config, target.id) != null,
                                onAddAndSwitch = {
                                    store.update { current ->
                                        val withTile = ScreenEdits.withSettingsTile(current, target.id)
                                            ?: return@update current
                                        withTile.copy(homeScreenId = target.id)
                                    }
                                    switching = null
                                },
                                onCancel = { switching = null },
                            )
                        } else ScreenList(
                            // folders belong to their tile, not to the screen list.
                            screens = FolderEdits.plainScreens(config),
                            homeId = config.homeScreenId,
                            unreachable = ScreenEdits.unreachable(config),
                            onAddJumpTile = { target ->
                                store.update { current ->
                                    ScreenEdits.withJumpTile(current, target.id) ?: current
                                }
                            },
                            jumpTilePossible = { target ->
                                ScreenEdits.withJumpTile(config, target.id) != null
                            },
                            // folders are otherwise not in this list, belonging to their
                            // tile. one without a tile belongs to nobody, and then this is the
                            // only place it can still appear.
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
                                // without a way into the settings the change could not be
                                // undone: one would never get back here.
                                if (ScreenEdits.settingsReachable(config, target.id)) {
                                    store.update { current -> current.copy(homeScreenId = target.id) }
                                } else {
                                    switching = target
                                }
                            },
                        )

                        Page.RENAME -> ScreenPanel(
                            // always the fresh version from the config: a remembered one
                            // kept showing the old grid after a grid change.
                            screen = renaming?.id?.let { id -> config.screens.firstOrNull { it.id == id } },
                            gutterDp = config.appearance.gutterDp,
                            borderPercent = config.appearance.safeBorderPercent,
                            theme = config.appearance.theme,
                            onBackground = { pick ->
                                val target = renaming
                                if (target != null) {
                                    store.update { current ->
                                        current.copy(
                                            screens = current.screens.map {
                                                if (it.id == target.id) {
                                                    it.copy(background = pick)
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

                        // as with the messages and the sos: the page gets its part and hands
                        // it back changed. what stays here is what an activity needs, to
                        // rebuild itself and to turn.
                        Page.APPEARANCE -> AppearanceList(
                            appearance = config.appearance,
                            onChange = { updated -> store.update { it.copy(appearance = updated) } },
                            onLanguageChanged = { recreate() },
                            onOrientationChanged = { requestedOrientation = Orientation.requested(it) },
                        )


                        Page.BEHAVIOUR -> BehaviourList(
                            blinkOn = config.behaviour.blinkOnNotification,
                            // look again on coming back - see `resumes`.
                            accessGranted = remember(resumes.intValue) {
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
                            onRecentCount = { count ->
                                store.update { it.copy(apps = it.apps.copy(recentCount = count)) }
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
                                    // do not switch on when nothing would be allowed: that
                                    // is not a secured phone but a locked one. show the list
                                    // instead.
                                    AppLock.Step.CHOOSE_FIRST -> {
                                        Notice.show(
                                            this@SettingsActivity,
                                            R.string.security_applock_choose_first,
                                        )
                                        page = Page.ALLOWED_APPS
                                    }

                                    AppLock.Step.TURN_ON -> store.update { current ->
                                        // allow the tile apps by themselves: whoever set
                                        // this up just put them within reach on purpose.
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
                                // the guards go with the pin, all of them, including the app
                                // lock in `apps`. without a pin they appear nowhere, so they
                                // could not be reached, and a pin set later would lock doors
                                // that had been open.
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

                        // this page sets the *behaviour*, so it gets `behaviour` and hands
                        // it back changed.
                        Page.ACCESSIBILITY -> AccessibilityList(
                            behaviour = config.behaviour,
                            onChange = { updated -> store.update { it.copy(behaviour = updated) } },
                        )


                        Page.SOS -> SosSettings(
                            config = config.sos,
                            onChange = { updated -> store.update { it.copy(sos = updated) } },
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
                            // no type filter: a backup from another phone can arrive with any
                            // mime type, and filtered entries are visible but not tappable in
                            // the system dialog, which looks like a broken app.
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

                        // the page gets its part of the settings and hands it back changed.
                        // the reminder alarm sat in the middle of this, although it belongs
                        // to the messages.
                        Page.MESSAGES -> MessagesSettingsList(
                            sms = config.sms,
                            onChange = { updated -> store.update { it.copy(sms = updated) } },
                            holdsSmsRole = remember(resumes.intValue) {
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

                        // fifth page in the same pattern. changing the default phone app
                        // stays here, since that needs an activity awaiting the system's
                        // answer.
                        Page.CALL_TYPES -> CallTypesList(
                            phone = config.phone,
                            onChange = { updated -> store.update { it.copy(phone = updated) } },
                            hasPhoneRole = remember(resumes.intValue) {
                                DialerRole.held(this@SettingsActivity)
                            },
                            onDialerApp = {
                                val roleIntent = Intents.dialerRoleIntent(this@SettingsActivity)
                                if (roleIntent != null) {
                                    askDialerRole.launch(roleIntent)
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
                                // free the widget ids first, then throw the config away: the
                                // other way round they could not be found again, and the host
                                // would keep them forever.
                                val host = WidgetHostController.get(this@SettingsActivity)
                                Reset.widgetIds(config).forEach { host.release(it) }
                                store.update { Reset.fresh() }
                                // and then really from the beginning. the promise is like on
                                // the first day, and the first day starts with the wizard;
                                // showing it only at the next cold start would not keep it.
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
         * the ids live in [org.biglau.ui.SettingsLink], where the intent other screens jump
         * with lives too. kept here as a reference so the page and the way to it stay together.
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
    isHomeScreen: Boolean,
    isDialerApp: Boolean,
    isSmsApp: Boolean,
    onDone: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings)) }
        // not the same house as the set-as-home row below: two rows with the same icon
        // carry no knowledge, they cause confusion.
        item { BigRow(stringResource(R.string.settings_screens), icon = Icons.Filled.ViewCarousel, onClick = onScreens) }
        item { BigRow(stringResource(R.string.settings_appearance), icon = Icons.Filled.Palette, onClick = onAppearance) }
        item { BigRow(stringResource(R.string.settings_behaviour), icon = Icons.Filled.NotificationsActive, onClick = onBehaviour) }
        item { BigRow(stringResource(R.string.settings_app_list), icon = Icons.Filled.Apps, onClick = onHiddenApps) }
        item { BigRow(stringResource(R.string.settings_security), icon = Icons.Filled.Lock, onClick = onSecurity) }
        // both rows say the state instead of repeating a request already fulfilled: the
        // invitation used to stand there even when BigLau was already the app, and a tap did
        // visibly nothing, the role dialog closing at once.
        item {
            BigRow(
                label = stringResource(
                    if (isHomeScreen) R.string.is_home else R.string.set_as_home,
                ),
                secondary = if (isHomeScreen) stringResource(R.string.role_change_hint) else null,
                icon = Icons.Filled.Home,
                surface = if (isHomeScreen) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onHomeApp,
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (isDialerApp) R.string.is_dialer else R.string.set_as_dialer,
                ),
                secondary = stringResource(
                    if (isDialerApp) R.string.role_change_hint else R.string.set_as_dialer_hint,
                ),
                icon = Icons.Filled.Call,
                surface = if (isDialerApp) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onDialerApp,
            )
        }
        // the third role stood nowhere: home and phone could be seen and changed here, the
        // sms role only on the messages screen, and there only while BigLau did *not* hold
        // it. three roles, one place.
        item {
            BigRow(
                label = stringResource(
                    if (isSmsApp) R.string.is_sms else R.string.set_as_sms,
                ),
                secondary = stringResource(
                    if (isSmsApp) R.string.role_change_hint else R.string.set_as_sms_hint,
                ),
                // not the same icon as the messages row below: two identical icons in a
                // list are two rows that get confused.
                icon = Icons.Filled.Sms,
                surface = if (isSmsApp) palette.surfaceAccent else palette.surfaceDefault,
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
    /** puts a jump tile on the home screen; null means there is no room. */
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
    // a screen with all its tiles was gone in one tap, with no question and no way back.
    // two steps, as when shrinking the grid: the first says what it costs.
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_screens)) }
        // a screen no tile leads to is set up and unreachable: without this hint one looks
        // for it and never finds it.
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
            // the way there instead of directions to it, as with the sos without contacts
            // and the call log.
            items(unreachable, key = { "sprung-${it.id}" }) { screen ->
                if (jumpTilePossible(screen)) {
                    BigRow(
                        label = stringResource(R.string.screens_add_jump, screen.name),
                        icon = Icons.Filled.Add,
                        surface = palette.surfaceAccent,
                        onClick = { onAddJumpTile(screen) },
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
                // the name belongs in the button: with three screens there are three pairs
                // below one another, and this one would have to be guessed from the order.
                BigRow(
                    label = stringResource(R.string.screen_make_home, screen.name),
                    icon = Icons.Filled.Home,
                    onClick = { onMakeHome(screen) },
                )
                val (tiles, folders) = lossesFor(screen)
                val isPending = pendingDelete == screen.id
                BigRow(
                    label = if (isPending) {
                        stringResource(R.string.screen_delete_now, screen.name)
                    } else {
                        stringResource(R.string.screen_delete, screen.name)
                    },
                    secondary = if (!isPending) {
                        null
                    } else {
                        buildString {
                            append(pluralStringResource(R.plurals.screen_delete_tiles, tiles, tiles))
                            if (folders > 0) {
                                append(' ')
                                append(pluralStringResource(R.plurals.screen_delete_folders, folders, folders))
                            }
                        }
                    },
                    icon = Icons.Filled.Delete,
                    surface = palette.surfaceDanger,
                    onClick = { if (isPending) onDelete(screen) else pendingDelete = screen.id },
                )
            }
        }
        // a folder without a tile can be neither seen nor opened. new ones no longer arise,
        // but older ones exist and would otherwise lie in the config and in every backup
        // forever.
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
                    // tiles, not cells: an empty slot in a folder is no tile, and a wrong
                    // count in exactly the row that invites deleting is the worst place.
                    secondary = pluralStringResource(
                        R.plurals.folder_delete_body,
                        folder.tileCount,
                        folder.tileCount,
                    ),
                    // not the same bin as the screen above: this is not ordinary deleting
                    // but tidying away something that cannot be opened any more.
                    icon = Icons.Filled.FolderOff,
                    surface = palette.surfaceDanger,
                    onClick = { onDeleteFolder(folder) },
                )
            }
        }
        // screens are created in the editor, not here, so every new screen has a tile
        // leading to it from the start.
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
 * the names of the background colours, in the order of `ScreenBackground.choicesFor`.
 *
 * spoken, not written: the row shows the colour full width, which is the better answer for
 * the eye. anyone not seeing it heard the same sentence five times.
 */
internal val BACKGROUND_NAMES = listOf(
    R.string.screen_background_blue,
    R.string.screen_background_violet,
    R.string.screen_background_green,
    R.string.screen_background_red,
    R.string.screen_background_ochre,
)

/**
 * a screen's name and grid.
 *
 * the grid stands beside the name and not in a corner of its own: both belong to the same
 * screen. shrinking costs tiles, so how many stands in red beforehand, and only the second
 * tap carries it out.
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
    // what the grid really keeps: screen minus the outer margin, a percentage of the width
    // holding on all four sides.
    val window = LocalConfiguration.current
    val margin = window.screenWidthDp * borderPercent / 100f
    val usableWidthDp = window.screenWidthDp - 2 * margin
    val usableHeightDp = window.screenHeightDp - 2 * margin
    // asked outside the list: no composable is allowed inside an `items` call.
    val backgroundColours = ScreenBackground.choicesFor(theme, isSystemInDarkTheme())
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { BigHeading(stringResource(R.string.screen_edit)) }
        item {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = bigSp(26f), fontWeight = FontWeight.Bold),
                // the same trap as with the tile name: the keyboard covers done entirely,
                // so its own tick key takes over.
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
                        // tapping another grid disarms the warning again, or a red row would
                        // stay armed somewhere and cost tiles on the next tap.
                        loses == 0 -> { onGrid(cols, rows); confirming = null }
                        armed -> { onGrid(cols, rows); confirming = null }
                        else -> confirming = cols to rows
                    }
                },
            )
        }
        // freely choosable as far as this screen carries it (see GridLimits): the bound is
        // not a number in the source but what is still readable as a tile.
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
            val isSelected = screen.background == Background.Theme
            BigRow(
                label = stringResource(R.string.screen_background_theme),
                icon = if (isSelected) Icons.Filled.Check else null,
                selected = isSelected,
                surface = BigSurface(palette.background, palette.onBackground),
                borderColor = if (isSelected) palette.accent else null,
                onClick = { onBackground(Background.Theme) },
            )
        }
        // each row in its own colour: a name is useless for a colour, one wants to see it
        // in the size it will have.
        //
        // to *see*, and that is where the argument ends: the node dump showed five rows all
        // saying this colour. the row stays as it is and the colour's name is spoken, which
        // is what `labelSpeech` is for.
        itemsIndexed(backgroundColours) { index, colour ->
            val isSelected = (screen.background as? Background.Solid)?.argb == colour
            BigRow(
                label = stringResource(R.string.screen_background_colour),
                labelSpeech = stringResource(BACKGROUND_NAMES[index % BACKGROUND_NAMES.size]),
                icon = if (isSelected) Icons.Filled.Check else null,
                selected = isSelected,
                surface = BigSurface(
                    Color(colour.toInt()),
                    Color(ScreenBackground.inkFor(colour).toInt()),
                ),
                borderColor = if (isSelected) palette.accent else null,
                onClick = { onBackground(Background.Solid(colour)) },
            )
        }
        item { BigHeading(stringResource(R.string.screen_columns)) }
        items(GridLimits.columns(usableWidthDp, gutterDp)) { colCount ->
            GridChoiceRow(
                label = pluralStringResource(R.plurals.screen_columns_n, colCount, colCount),
                cols = colCount,
                rows = screen.rows,
                screen = screen,
                confirming = confirming,
                onArm = { confirming = it },
                onGrid = onGrid,
            )
        }
        item { BigHeading(stringResource(R.string.screen_rows)) }
        items(GridLimits.rows(usableHeightDp, gutterDp)) { rowCount ->
            GridChoiceRow(
                label = pluralStringResource(R.plurals.screen_rows_n, rowCount, rowCount),
                cols = screen.cols,
                rows = rowCount,
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
    /** the activity rebuilds itself after a language change. */
    onLanguageChanged: () -> Unit,
    /** a turn takes effect on the activity at once. */
    onOrientationChanged: (ScreenOrientation) -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_appearance)) }
        item { BigHeading(stringResource(R.string.appearance_language)) }
        // the language names stay in their own language: whoever cannot read the current one
        // looks for the word they know, and a translated name is unreadable to exactly the
        // person who needs the row.
        items(Language.entries.toList()) { entry ->
            BigRow(
                label = stringResource(languageLabel(entry)),
                icon = if (entry == appearance.language) Icons.Filled.Check else null,
                selected = entry == appearance.language,
                onClick = {
                        if (entry != appearance.language) {
                            onChange(appearance.copy(language = entry))
                            // rebuild at once: tapping a language and seeing nothing happen
                            // leads to tapping again, on the one page where one cannot read
                            // what is going on.
                            onLanguageChanged()
                        }
                    },
            )
        }
        // each row is painted in its own theme: the same palette icon three times said
        // nothing. the choice carries a tick instead of an accent surface, the surface
        // belonging to the theme here.
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
        // each row in its own face: one sees the difference instead of reading about it,
        // which for a font meant to help poor sight is the only preview worth anything.
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
        // each row in its own size: one sees what one chooses.
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
        // each row in its own size, as with the text size above: three lists of the same
        // percentages look alike once the heading has scrolled away.
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
        // and here the icon itself, in the size in question.
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
        // `PLAN.md` 3.2: on three inches a cut-off word is worse than none. offered only
        // where a label stands at all, or it would be a switch without effect.
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
        // full screen removes the system bar and the header carries the rest; both off means
        // no time and no battery. allowed, but said, or the phone gets the blame.
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
        items(GridLooks.GUTTERS) { value ->
            BigRow(
                label = "$value dp",
                icon = if (value == appearance.gutterDp) Icons.Filled.Check else null,
                selected = value == appearance.gutterDp,
                onClick = { onChange(appearance.copy(gutterDp = GridLooks.gutter(value))) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_border)) }
        items(GridLooks.BORDERS) { value ->
            BigRow(
                label = "$value %",
                icon = if (value == appearance.safeBorderPercent) Icons.Filled.Check else null,
                selected = value == appearance.safeBorderPercent,
                onClick = { onChange(appearance.copy(safeBorderPercent = GridLooks.border(value))) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_corner)) }
        // each row in its own rounding: the number says nothing, the corner everything.
        items(GridLooks.RADII) { value ->
            BigRow(
                label = "$value dp",
                icon = if (value == appearance.cornerRadiusDp) Icons.Filled.Check else null,
                selected = value == appearance.cornerRadiusDp,
                cornerRadius = value.dp,
                onClick = { onChange(appearance.copy(cornerRadiusDp = GridLooks.radius(value))) },
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
                        // applied at once: a rotation arriving only at the next start looks
                        // like a jammed switch.
                        onOrientationChanged(entry)
                    },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_clock_size)) }
        items(ClockFormat.SCALES) { value ->
            BigRow(
                label = "${(value * 100).toInt()} %",
                icon = if (value == appearance.clockScale) Icons.Filled.Check else null,
                selected = value == appearance.clockScale,
                onClick = { onChange(appearance.copy(clockScale = ClockFormat.scale(value))) },
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
 * accessibility. both switches take the editor away from the long press, so below them
 * stands where to find it instead: a setting that closes a way must name the new one.
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
 * backup and restore, meant for moving to another phone, hence through the system file
 * dialog: the file should lie where it can be found again, not in an app directory that
 * disappears on uninstall.
 */
@Composable
private fun TransferList(onExport: () -> Unit, onImport: () -> Unit) {
    val palette = LocalBigPalette.current
    // importing replaces the whole arrangement, irreversibly. the same two steps as
    // shrinking the grid: the first tap warns, the second does it.

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
                // no second tap in front any more: it promised that everything would be
                // replaced, which was untrue, since the second tap opened the file dialog.
                // the question now stands in `ImportActivity`, where there is something to
                // see. two taps are also what an unsteady hand does by itself.
                onClick = onImport,
            )
        }
    }
}

/**
 * unhide hidden apps. without this page hiding would be a one-way street, and an action
 * without a way back is a fault even when it does what it should.
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
        // the number was fixed at four while the app stores twelve starts, so anyone using
        // many different apps never saw the fifth again.
        item { BigHeading(stringResource(R.string.apps_recent_count)) }
        items(AppDrawer.RECENT_CHOICES) { count ->
            BigRow(
                label = if (count == 0) {
                    stringResource(R.string.apps_recent_none_choice)
                } else {
                    pluralStringResource(R.plurals.apps_recent_count_value, count, count)
                },
                selected = count == recentCount,
                onClick = { onRecentCount(count) },
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
                    // the row below claimed that apps on the tiles are allowed from the
                    // start, even when not a single app lies on one and the switch would lock
                    // the phone.
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
            // reachable with the lock off too, or the list could only be prepared once one
            // had already locked oneself out.
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
// `BigLauActivity` and not `ComponentActivity`: the diagnostics read states the system
// grants and need `resumes` for that. a diagnostics page showing stale values is worse than
// none.
private fun DiagnosticsList(activity: BigLauActivity) {
    // what is left after the system bars, exactly the area a tile gets: the window alone
    // said 605 dp of height, while 581 are usable.
    val screenDensity = LocalDensity.current
    val insets = WindowInsets.safeDrawing
    val usable = run {
        // the *full* screen size and not `displayMetrics`, which already excludes the
        // gesture bar; it would come off twice. see `Diagnostics.usableDp`.
        val whole = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.windowManager.currentWindowMetrics.bounds
                .let { it.width() to it.height() }
        } else {
            val metrics = android.util.DisplayMetrics()
            // this is the branch for everything before android 11; the replacement is in
            // the if above.
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
        Diagnostics.usableDp(
            fullWidthPx = whole.first,
            fullHeightPx = whole.second,
            left = insets.getLeft(screenDensity, LayoutDirection.Ltr),
            top = insets.getTop(screenDensity),
            right = insets.getRight(screenDensity, LayoutDirection.Ltr),
            bottom = insets.getBottom(screenDensity),
            density = screenDensity.density,
        )
    }
    val context = LocalContext.current
    // `resumes` as a second key: the diagnostics read roles and permissions the system
    // grants, and granting one and coming back gave the old values on exactly the page one
    // opens to check what is true.
    val lines = remember(usable, activity.resumes.intValue) {
        Diagnostics.collect(activity, usable) { id -> context.getString(id) }
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
 * a warning before locking oneself out.
 *
 * the chosen screen carries no settings tile, and none is reachable from it through jumps
 * or folders either. after the change one would never get back here; only another launcher
 * or a computer with adb would help.
 *
 * so the way out is offered instead of a ban: create a settings tile, then change. only
 * with no room for one does it really not work.
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
 * the question before the one step that cannot be undone.
 *
 * it counts up what disappears instead of asking whether one is sure: a number makes a
 * warning true, a phrase gets tapped away unread. and the first button is not the deletion
 * but the backup, after which it is no longer a loss but a fresh start.
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
            // words that agree with the number: a mismatch makes a warning look sloppy, and
            // a sloppy warning is not taken seriously. with no folders the half sentence
            // falls away entirely rather than saying zero.
            val screensText = pluralStringResource(
                R.plurals.reset_screens, losses.screens, losses.screens,
            )
            val tilesText = pluralStringResource(
                R.plurals.reset_tiles, losses.tiles, losses.tiles,
            )
            Text(
                text = if (losses.folders == 0) {
                    stringResource(R.string.reset_losses_plain, screensText, tilesText)
                } else {
                    stringResource(
                        R.string.reset_losses,
                        screensText,
                        tilesText,
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
 * the order when swiping and for the next and previous tiles.
 *
 * a screen moves one place up or down, it is not dragged: dragging needs a steady hand,
 * which is not to be assumed here. tapping twice moves the same screen two places.
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
                val mayExclude = mayLeave(screen.id)
                BigRow(
                    label = screen.name,
                    // why it does not work, instead of a button that does nothing: without a
                    // jump tile the screen would be unfindable after being taken out.
                    secondary = if (mayExclude) {
                        stringResource(R.string.swipe_order_position, index + 1, screens.size)
                    } else {
                        stringResource(R.string.swipe_order_needed)
                    },
                    secondaryMaxLines = 2,
                    modifier = Modifier.weight(1f),
                    onClick = { if (mayExclude) onExclude(screen.id) },
                )
                // no button at the edge rather than one that does nothing: an arrow that
                // sometimes works and sometimes does not makes one doubt oneself.
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
 * one row of the grid picker: preset, column or row, always the same warning.
 *
 * changing a grid can cost tiles, and how many stands in the row; the first tap does
 * nothing yet. tapping another row disarms the warning again.
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
 * which apps start without the pin. `PLAN.md` 4.5.
 *
 * the whole list with ticks on the allowed ones, not only the allowed ones: allowing an app
 * means finding it first, and a list of what is already allowed is useless for that.
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
                val key = AppDrawer.keyOf(app)
                val isAllowed = key in allowed || app.packageName in allowed
                BigRow(
                    label = app.label,
                    icon = if (isAllowed) Icons.Filled.Check else Icons.Filled.Lock,
                    checked = isAllowed,
                    onClick = { onToggle(key) },
                )
            }
        }
    }
}

/**
 * the contact list's three switches. they sat in the model and were read, and two of them
 * could be changed nowhere.
 *
 * the sorting also sits as an icon in the contact list itself, where it is needed. it is
 * here because this is where one looks after not recognising the icon.
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
 * which call kinds appear in the list. `PLAN.md` 4.6.
 *
 * the logic for it stood in the source with its tests and was never called: a filter with no
 * switch. the quick missed-only toggle lives in the list itself; here stands what appears
 * at all.
 */
@Composable
private fun CallTypesList(
    phone: PhoneConfig,
    onChange: (PhoneConfig) -> Unit,
    /** changing the default phone app works only through an activity. */
    onDialerApp: () -> Unit,
    /**
     * does BigLau hold the phone role? comes from outside because the *system* grants it.
     * see `BigLauActivity.resumes`.
     */
    hasPhoneRole: Boolean,
) {
    val palette = LocalBigPalette.current
    var blockedText by remember(phone.blockedNumbers) { mutableStateOf(CallBlocking.format(phone.blockedNumbers)) }
    val rejected = remember(blockedText) { CallBlocking.rejected(blockedText) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // the call log stands at the top because the way here comes from it, and seeing a
        // field for blocked numbers first reads as the wrong page. within it, grouping before
        // the kinds: grouping concerns the whole list, hiding kinds only its content.
        item { BigHeading(stringResource(R.string.call_grouping)) }
        items(CallGrouping.entries.toList()) { kind ->
            BigRow(
                label = stringResource(groupingLabel(kind)),
                secondary = stringResource(groupingHint(kind)),
                selected = kind == phone.callGrouping,
                onClick = { onChange(phone.copy(callGrouping = kind)) },
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
        items(CallDirection.entries.toList()) { kind ->
            val visible = kind.name !in phone.hiddenCallTypes
            BigRow(
                label = stringResource(callDirectionLabel(kind)),
                icon = if (visible) Icons.Filled.Check else null,
                checked = visible,
                onClick = {
                    val hidden = phone.hiddenCallTypes
                    onChange(
                        phone.copy(
                            hiddenCallTypes = if (kind.name in hidden) {
                                hidden - kind.name
                            } else {
                                hidden + kind.name
                            },
                        ),
                    )
                },
            )
        }
        // `PLAN.md` 4.6: blocked numbers, in one field as with the sos numbers. faster on
        // three inches than a list with a plus button, and CallBlocking sorts strictly.
        item { BigHeading(stringResource(R.string.blocked_numbers)) }
        item {
            Text(
                // rejected without ringing holds only while BigLau has the phone role: only
                // the default phone app sees incoming calls. without it the block works
                // outwards alone. see DialerRole.
                text = if (hasPhoneRole) {
                    stringResource(R.string.blocked_numbers_hint)
                } else {
                    stringResource(R.string.blocked_numbers_hint_outgoing)
                },
                color = palette.onBackground,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        // offer the way, not just the reason: without the phone role the block works half,
        // and the role is two screens away. the row stands only while it is missing.
        if (!hasPhoneRole) {
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
                value = blockedText,
                onValueChange = { blockedText = it },
                placeholder = { Text(stringResource(R.string.blocked_numbers_placeholder), fontSize = bigSp(15f)) },
                textStyle = LocalTextStyle.current.copy(fontSize = bigSp(17f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        }
        if (rejected.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.blocked_numbers_rejected, rejected.joinToString(", ")),
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
                onClick = { onChange(phone.copy(blockedNumbers = CallBlocking.parse(blockedText))) },
            )
        }
        // PLAN.md 4.6: default audio route and speaker on outgoing calls.
        item { BigHeading(stringResource(R.string.call_audio)) }
        items(AudioRoute.entries.toList()) { route ->
            BigRow(
                label = stringResource(audioLabel(route)),
                selected = route == phone.audioRoute,
                onClick = { onChange(phone.copy(audioRoute = route)) },
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
        items(CallerPhoto.entries.toList()) { size ->
            BigRow(
                label = stringResource(photoLabel(size)),
                selected = size == phone.callerPhoto,
                onClick = { onChange(phone.copy(callerPhoto = size)) },
            )
        }
    }
}

private fun audioLabel(route: AudioRoute): Int = when (route) {
    AudioRoute.EARPIECE -> R.string.call_audio_earpiece
    AudioRoute.SPEAKER -> R.string.call_audio_speaker
    AudioRoute.BLUETOOTH -> R.string.call_audio_bluetooth
}

private fun photoLabel(size: CallerPhoto): Int = when (size) {
    CallerPhoto.OFF -> R.string.caller_photo_off
    CallerPhoto.SMALL -> R.string.caller_photo_small
    CallerPhoto.HALF -> R.string.caller_photo_half
    CallerPhoto.FULL -> R.string.caller_photo_full
}

private fun groupingLabel(kind: CallGrouping): Int = when (kind) {
    CallGrouping.NONE -> R.string.call_grouping_none
    CallGrouping.NUMBER -> R.string.call_grouping_number
    CallGrouping.DIRECTION -> R.string.call_grouping_direction
}

private fun groupingHint(kind: CallGrouping): Int = when (kind) {
    CallGrouping.NONE -> R.string.call_grouping_none_hint
    CallGrouping.NUMBER -> R.string.call_grouping_number_hint
    CallGrouping.DIRECTION -> R.string.call_grouping_direction_hint
}



