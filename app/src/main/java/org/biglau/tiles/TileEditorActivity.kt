package org.biglau.tiles

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Folder
import org.biglau.ui.bigSp
import org.biglau.ui.theme.LocalCornerRadius
import org.biglau.ui.theme.LocalIconVisibility
import org.biglau.ui.BigLauActivity
import org.biglau.notify.TileNotifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.R
import org.biglau.core.ui.R as UiR
import org.biglau.actions.Intents
import org.biglau.apps.AppRepository
import org.biglau.search.TextSearch
import org.biglau.shortcuts.ShortcutAnswer
import org.biglau.shortcuts.ShortcutRepository
import org.biglau.shortcuts.ShortcutRow
import org.biglau.shortcuts.Shortcuts
import org.biglau.ui.Notice
import org.biglau.widgets.WidgetFit
import org.biglau.widgets.WidgetHostController
import org.biglau.widgets.WidgetProviderRow
import org.biglau.apps.LaunchableApp
import org.biglau.data.Builtin
import org.biglau.data.IconVisibility
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.contacts.ContactRepository
import org.biglau.contacts.PhoneContact
import org.biglau.data.ConfigStore
import org.biglau.data.LauncherConfig
import org.biglau.data.Cell
import org.biglau.data.ContactMode
import org.biglau.data.Screen
import androidx.compose.ui.platform.LocalConfiguration
import org.biglau.phone.PhoneNumbers
import org.biglau.ui.IconCatalogue
import org.biglau.ui.BigHeading
import org.biglau.ui.gridMetrics
import org.biglau.ui.BigSearchField
import org.biglau.ui.ContactAvatar
import org.biglau.security.Pin
import org.biglau.ui.PinGate
import org.biglau.ui.BigRow
import org.biglau.web.LinkTarget
import org.biglau.ui.icon
import org.biglau.ui.labelRes
import org.biglau.ui.theme.BigLauTheme
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import org.biglau.ui.theme.FreeTileColor
import org.biglau.ui.theme.toArgbLong
import org.biglau.ui.theme.LocalBigPalette

private enum class Mode { MENU, MOVE, EDIT_LINK, PICK_LONG_PRESS, PICK_BUILTIN, PICK_APP, PICK_CONTACT, PICK_NUMBER, PICK_MODE, EDIT_LABEL, PICK_COLOR, PICK_HUE, PICK_ICON, RESIZE, PICK_SCREEN, EDIT_NUMBER, PICK_SHORTCUT_APP, PICK_SHORTCUT, PICK_WIDGET }

/**
 * assigns a single tile. writes straight into the ConfigStore; the home screen watches the
 * same flow and redraws as soon as something happens here.
 */
class TileEditorActivity : BigLauActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val screenId = intent.getStringExtra(EXTRA_SCREEN) ?: return finish()
        val startX = intent.getIntExtra(EXTRA_X, -1)
        val startY = intent.getIntExtra(EXTRA_Y, -1)
        if (startX < 0 || startY < 0) return finish()

        val store = ConfigStore.get(this)
        val apps = AppRepository.get(this)
        val contacts = ContactRepository.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            val screen = config.screenById(screenId) ?: return@setContent finish()
            // shrinking up or left moves the cell's corner, so the anchor has to move with
            // it or the editor suddenly points at an empty slot.
            var x by rememberSaveable { mutableStateOf(startX) }
            var y by rememberSaveable { mutableStateOf(startY) }
            val cell = screen.cellAt(x, y)
            // the real cell size of this screen; hard-wired default grid values were wrong
            // for every other grid.
            val configuration = LocalConfiguration.current
            val metrics = remember(configuration, screen.cols, screen.rows, config.appearance) {
                gridMetrics(
                    availableWidth = configuration.screenWidthDp.toFloat(),
                    availableHeight = configuration.screenHeightDp.toFloat(),
                    cols = screen.cols,
                    rows = screen.rows,
                    gutter = config.appearance.gutterDp.toFloat(),
                    borderPercent = config.appearance.safeBorderPercent,
                )
            }
            val button = cell?.button ?: Button()
            var mode by remember { mutableStateOf(Mode.MENU) }
            // where the next choice goes. `PLAN.md` 4.3 promises that *every* action may
            // sit on the long press too; two separate lists covered only apps and functions.
            // the same lists for both ways instead of eight more modes.
            var aufLangdruck by remember { mutableStateOf(false) }
            var chosenContact by remember { mutableStateOf<PhoneContact?>(null) }
            var chosenNumber by remember { mutableStateOf<String?>(null) }
            var shortcutApp by remember { mutableStateOf<LaunchableApp?>(null) }
            var clearing by remember { mutableStateOf<ButtonAction.Folder?>(null) }
            // a pin before the editor when one is set: a long press happens faster than
            // one thinks.
            var locked by remember {
                mutableStateOf(
                    Pin.protectsEditor(config.security.pin, config.security.pinProtectsEditor),
                )
            }
            val widgets = remember { WidgetHostController.get(this@TileEditorActivity) }
            /**
             * give back a widget id that was lying here before reassigning, or the host keeps
             * it forever: a leak nobody can see.
             */
            fun releaseWidgetIfAny(current: Button) {
                (current.action as? ButtonAction.Widget)?.let {
                    WidgetHostController.get(this@TileEditorActivity).release(it.widgetId)
                }
            }

            // reassigning a tile that carries a folder left the folder and its contents
            // behind, with no way to it: it is not in the screen list, and it could not be
            // deleted either. emptying the tile had long asked; every other way had not.
            var replacingFolder by remember { mutableStateOf<Pair<ButtonAction.Folder, Button>?>(null) }

            fun writeNow(next: Button) {
                if (next.action !is ButtonAction.Widget) releaseWidgetIfAny(button)
                // the folder is created here, not before: creating it ahead of the question
                // left an unopenable empty folder behind whenever the answer was keep.
                // creating now hangs on the same condition as writing.
                val neuerOrdner = next.action as? ButtonAction.Folder
                if (neuerOrdner != null && store.current.screens.none { it.id == neuerOrdner.screenId }) {
                    store.update {
                        ScreenEdits.add(
                            it,
                            FolderEdits.newFolder(
                                neuerOrdner.screenId,
                                getString(R.string.folder_default_name),
                                screen,
                            ),
                        )
                    }
                }
                store.setButton(screenId, x, y, next)
            }

            fun write(next: Button) {
                val ordner = button.action as? ButtonAction.Folder
                if (ordner != null && next.action != ordner) {
                    replacingFolder = ordner to next
                } else {
                    writeNow(next)
                }
            }

            /** puts the chosen action on the short or the long press, by the way one came. */
            fun belege(action: ButtonAction) {
                if (aufLangdruck) {
                    write(TileEdits.withLongPress(button, action))
                } else {
                    write(TileEdits.withAction(button, action))
                }
                mode = Mode.MENU
            }

            // back in the menu the short press holds again, also after the back key, or the
            // next choice would silently land on the long press.
            LaunchedEffect(mode) { if (mode == Mode.MENU) aufLangdruck = false }

            var pendingWidget by remember { mutableStateOf<Pair<Int, WidgetProviderRow>?>(null) }

            fun finishWidget(id: Int, provider: WidgetProviderRow) {
                val next = TileEdits.withAction(
                    button,
                    ButtonAction.Widget(provider.component, id, provider.label),
                )
                write(next)

                // size the cell right away: an oversized widget is squeezed and looks
                // broken, and one would have to work out that the tile needs enlarging.
                val (needX, needY) = WidgetFit.requirement(
                    provider,
                    metrics.cellWidth,
                    metrics.cellHeight,
                    config.appearance.gutterDp.toFloat(),
                )
                val live = store.current.screenById(screenId)?.cellAt(x, y)
                if (live != null && (live.w < needX || live.h < needY)) {
                    var grew = false
                    store.updateScreen(screenId) { board ->
                        val target = board.cellAt(x, y) ?: return@updateScreen board
                        val bigger = CellLayout.growTo(board, target, needX, needY)
                        grew = bigger != null
                        bigger ?: board
                    }
                    if (!grew) {
                        Notice.show(
                            this@TileEditorActivity,
                            getString(R.string.widget_no_room, needX, needY),
                        )
                    }
                }

                pendingWidget = null
                mode = Mode.MENU
            }

            val configureWidget = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                val pending = pendingWidget
                if (pending == null) return@rememberLauncherForActivityResult
                if (result.resultCode == RESULT_OK) {
                    finishWidget(pending.first, pending.second)
                } else {
                    // cancelled: give the id back, or it stays taken.
                    widgets.release(pending.first)
                    pendingWidget = null
                    mode = Mode.MENU
                }
            }

            val bindWidget = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                val pending = pendingWidget
                if (pending == null) return@rememberLauncherForActivityResult
                if (result.resultCode == RESULT_OK) {
                    if (pending.second.needsConfiguration) {
                        widgets.startConfiguration(
                            this@TileEditorActivity,
                            pending.first,
                            WidgetHostController.REQUEST_CONFIGURE,
                        )
                    } else {
                        finishWidget(pending.first, pending.second)
                    }
                } else {
                    widgets.release(pending.first)
                    pendingWidget = null
                    mode = Mode.MENU
                }
            }

            fun chooseWidget(provider: WidgetProviderRow) {
                val id = widgets.allocateId()
                pendingWidget = id to provider
                if (widgets.bindDirectly(id, provider.component)) {
                    if (provider.needsConfiguration) {
                        val started = widgets.startConfiguration(
                            this@TileEditorActivity,
                            id,
                            WidgetHostController.REQUEST_CONFIGURE,
                        )
                        if (!started) finishWidget(id, provider)
                    } else {
                        finishWidget(id, provider)
                    }
                } else {
                    val intent = widgets.bindRequestIntent(id, provider.component)
                    if (intent != null) {
                        bindWidget.launch(intent)
                    } else {
                        widgets.release(id)
                        pendingWidget = null
                    }
                }
            }
            // `resumes` as the key: on a permanently refused permission this screen sends
            // people into the app settings, and nothing comes back from there.
var contactsGranted by remember(resumes.intValue) { mutableStateOf(contacts.hasPermission()) }
            val askForContacts = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                contactsGranted = granted
                if (!granted) mode = Mode.MENU
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
                // the back key does what the button beside it does.
                //
                // both delete questions are full-screen panels that branch out with
                // `return@Box` before the mode is reached, and they are set from the menu, so
                // back ended the whole editor instead of the question. nothing was destroyed,
                // but anyone working by key lost their place unannounced.
                //
                // the order is the order the panels lie in. the lock stays out of it: there
                // the key leads out and must not lead in.
                BackHandler(
                    enabled = mode != Mode.MENU || replacingFolder != null || clearing != null,
                ) {
                    when {
                        replacingFolder != null -> replacingFolder = null
                        clearing != null -> clearing = null
                        else -> mode = Mode.MENU
                    }
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(LocalBigPalette.current.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    if (locked) {
                        PinGate(
                            title = stringResource(R.string.editor_locked),
                            explainer = stringResource(R.string.editor_locked_hint),
                            wrongText = stringResource(R.string.security_wrong_pin),
                            confirmLabel = stringResource(R.string.editor_done),
                            onCheck = { eingabe -> Pin.verify(eingabe, config.security.pin) },
                            onAccept = { locked = false },
                            acceptOnComplete = true,
                        )
                        return@Box
                    }

                    val zuErsetzen = replacingFolder
                    if (zuErsetzen != null) {
                        val (ordner, neu) = zuErsetzen
                        FolderDeletePanel(
                            name = FolderEdits.folderFor(config, ordner)?.name.orEmpty(),
                            count = FolderEdits.contentCount(config, ordner.screenId),
                            replacing = true,
                            onKeep = { replacingFolder = null },
                            onDelete = {
                                // folder first, then the new assignment: FolderEdits.delete
                                // clears this tile too, and setButton lays it out again.
                                store.update { FolderEdits.delete(it, ordner.screenId) }
                                writeNow(neu)
                                replacingFolder = null
                                finish()
                            },
                        )
                        return@Box
                    }

                    val zuLoeschen = clearing
                    if (zuLoeschen != null) {
                        FolderDeletePanel(
                            name = FolderEdits.folderFor(config, zuLoeschen)?.name.orEmpty(),
                            count = FolderEdits.contentCount(config, zuLoeschen.screenId),
                            onKeep = { clearing = null },
                            onDelete = {
                                store.update { FolderEdits.delete(it, zuLoeschen.screenId) }
                                finish()
                            },
                        )
                        return@Box
                    }

                    // one banner instead of six more headings: the same lists now serve both
                    // press kinds, and without it the long-press way would look like the main
                    // assignment. getting that wrong overwrites what the tile did.
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (aufLangdruck && mode != Mode.MENU) {
                        Text(
                            text = stringResource(R.string.editor_long_press_banner),
                            color = LocalBigPalette.current.onBackground,
                            fontSize = bigSp(15f),
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                    when (mode) {
                        Mode.MENU -> MenuList(
                            button = button,
                            platz = stringResource(R.string.editor_where, screen.name, y + 1, x + 1),
                            screenName = { id -> config.screenById(id)?.name },
                            appLabel = { a -> apps.labelFor(a.packageName, a.activityName) },
                            onPickBuiltin = { mode = Mode.PICK_BUILTIN },
                            onPickApp = { mode = Mode.PICK_APP },
                            onPickContact = {
                                mode = Mode.PICK_CONTACT
                                if (!contactsGranted) {
                                    askForContacts.launch(Manifest.permission.READ_CONTACTS)
                                }
                            },
                            onEditLabel = { mode = Mode.EDIT_LABEL },
                            moveTargets = cell?.let {
                                TileMove.targetsFor(config, screenId, it).size +
                                    TileMove.spotsFor(screen, it).size
                            } ?: 0,
                            onMove = { mode = Mode.MOVE },
                            onPickShortcut = { mode = Mode.PICK_SHORTCUT_APP },
                            onPickWidget = { mode = Mode.PICK_WIDGET },
                            onPickScreen = { mode = Mode.PICK_SCREEN },
                            onPickLink = { mode = Mode.EDIT_LINK },
                            onPickMessage = { mode = Mode.EDIT_NUMBER },
                            onToggleBlink = { write(button.copy(blink = !button.blink)) },
                            onPickLongPress = { mode = Mode.PICK_LONG_PRESS },
                            onClearLongPress = { write(TileEdits.withLongPress(button, null)) },
                            mayAddFolder = FolderEdits.mayContainFolder(screen),
                            onNewFolder = {
                                // only the id is chosen here; `writeNow` creates the folder
                                // when the tile is really written.
                                val id = ScreenEdits.freeId(store.current, base = "folder")
                                write(TileEdits.withAction(button, ButtonAction.Folder(id)))
                                mode = Mode.MENU
                            },
                            onPickColor = { mode = Mode.PICK_COLOR },
                            onPickIcon = { mode = Mode.PICK_ICON },
                            onResize = if (cell != null) ({ mode = Mode.RESIZE }) else null,
                            onClear = {
                                val ordner = button.action as? ButtonAction.Folder
                                if (ordner == null) {
                                    releaseWidgetIfAny(button)
                                    store.clearButton(screenId, x, y)
                                    finish()
                                } else {
                                    // ask first: emptying the tile would leave the folder
                                    // behind with no way to it, contents and all.
                                    clearing = ordner
                                }
                            },
                            onDone = { finish() },
                        )

                        Mode.PICK_BUILTIN -> BuiltinList { builtin ->
                            belege(ButtonAction.Action(builtin))
                        }

                        Mode.PICK_APP -> AppList(apps) { app ->
                            belege(ButtonAction.App(app.packageName, app.activityName))
                        }

                        Mode.PICK_CONTACT -> ContactList(
                            repository = contacts,
                            granted = contactsGranted,
                            onGrant = { askForContacts.launch(Manifest.permission.READ_CONTACTS) },
                        ) { contact ->
                            chosenContact = contact
                            when {
                                contact.hasChoice -> mode = Mode.PICK_NUMBER
                                // without a number the tile would never do anything: better
                                // not created than silently assigned to nothing.
                                !contact.isCallable -> {
                                    Notice.show(
                                        this@TileEditorActivity,
                                        getString(R.string.contact_without_number, contact.name),
                                    )
                                }
                                else -> {
                                    chosenNumber = contact.primaryNumber
                                    mode = Mode.PICK_MODE
                                }
                            }
                        }

                        Mode.PICK_NUMBER -> NumberList(chosenContact) { number ->
                            chosenNumber = number
                            mode = Mode.PICK_MODE
                        }

                        Mode.PICK_MODE -> ContactModeList { contactMode ->
                            val contact = chosenContact
                            val number = chosenNumber
                            if (contact != null && number != null) {
                                belege(
                                    ButtonAction.Contact(
                                        name = contact.name,
                                        number = number,
                                        photoUri = contact.photoUri,
                                        mode = contactMode,
                                    ),
                                )
                            } else {
                                mode = Mode.MENU
                            }
                        }

                        Mode.EDIT_LABEL -> {
                            // a folder has exactly one name, on the tile and as the heading
                            // inside: a second name for the tile alone would call the same
                            // thing differently depending on where one stands.
                            val ordner = button.action as? ButtonAction.Folder
                            LabelEditor(
                                initial = if (ordner != null) {
                                    FolderEdits.folderFor(config, ordner)?.name.orEmpty()
                                } else {
                                    button.label.orEmpty()
                                },
                                automatic = describe(
                                    button.copy(label = null),
                                    screenName = { id -> config.screenById(id)?.name },
                                    appLabel = { a -> apps.labelFor(a.packageName, a.activityName) },
                                ),
                                titleRes = if (ordner != null) R.string.folder_rename else R.string.editor_label,
                                hintRes = if (ordner != null) {
                                    R.string.folder_rename_hint
                                } else {
                                    R.string.editor_label_hint
                                },
                            ) { text ->
                                if (ordner != null) {
                                    store.update { ScreenEdits.rename(it, ordner.screenId, text) }
                                } else {
                                    write(TileEdits.withLabel(button, text))
                                }
                                mode = Mode.MENU
                            }
                        }

                        Mode.PICK_SHORTCUT_APP -> {
                            val shortcuts = ShortcutRepository.get(this@TileEditorActivity)
                            if (!shortcuts.available()) {
                                NeedsHomeRole { Intents.chooseHomeApp(this@TileEditorActivity) }
                            } else {
                                AppList(apps, headingRes = R.string.editor_pick_shortcut) { app ->
                                    shortcutApp = app
                                    mode = Mode.PICK_SHORTCUT
                                }
                            }
                        }

                        Mode.PICK_SHORTCUT -> ShortcutList(
                            app = shortcutApp,
                            answer = remember(shortcutApp) {
                                shortcutApp?.let {
                                    ShortcutRepository.get(this@TileEditorActivity).forPackage(it.packageName)
                                } ?: ShortcutAnswer.Failed
                            },
                            onBack = { mode = Mode.PICK_SHORTCUT_APP },
                        ) { row ->
                            belege(ButtonAction.Shortcut(row.packageName, row.id, Shortcuts.labelOf(row)))
                        }

                        Mode.PICK_WIDGET -> WidgetPicker(
                            rows = remember { widgets.providers() },
                            cellWidthDp = metrics.cellWidth,
                            cellHeightDp = metrics.cellHeight,
                            gutterDp = config.appearance.gutterDp.toFloat(),
                            onPick = ::chooseWidget,
                        )

                        Mode.PICK_SCREEN -> ScreenPicker(
                            config = config,
                            currentScreenId = screenId,
                            onPick = { targetId -> belege(ButtonAction.GoToScreen(targetId)) },
                            onCreate = {
                                val id = ScreenEdits.freeId(store.current)
                                val name = getString(R.string.screen_default_name, store.current.screens.size + 1)
                                store.update { ScreenEdits.add(it, ScreenEdits.newScreen(id, name, screen)) }
                                belege(ButtonAction.GoToScreen(id))
                            },
                        )

                        Mode.EDIT_NUMBER -> NumberEditor(
                            initial = (button.action as? ButtonAction.Contact)
                                ?.takeIf { it.mode == ContactMode.SMS }?.number.orEmpty(),
                        ) { eingabe ->
                            val action = MessageTile.actionFor(eingabe)
                            if (action == null) {
                                Notice.show(this@TileEditorActivity, R.string.message_number_invalid)
                            } else {
                                belege(action)
                            }
                        }

                        Mode.EDIT_LINK -> LinkEditor(
                            initial = (button.action as? ButtonAction.Link)?.url.orEmpty(),
                        ) { eingabe ->
                            val adresse = LinkTarget.normalise(eingabe)
                            if (adresse == null) {
                                Notice.show(this@TileEditorActivity, R.string.link_invalid)
                            } else {
                                belege(ButtonAction.Link(adresse))
                            }
                        }

                        // the kind first, then the thing: a list mixing apps and functions
                        // would be too long to scan on this screen.
                        Mode.PICK_LONG_PRESS -> LongPressKindList(
                            onPick = { gewaehlt ->
                                aufLangdruck = true
                                mode = gewaehlt
                                if (gewaehlt == Mode.PICK_CONTACT && !contactsGranted) {
                                    askForContacts.launch(Manifest.permission.READ_CONTACTS)
                                }
                            },
                        )

                        Mode.MOVE -> MoveTargetList(
                            welche = stringResource(
                                R.string.move_which,
                                describe(button, { id -> config.screenById(id)?.name }, { a ->
                                    apps.labelFor(a.packageName, a.activityName)
                                }),
                                y + 1,
                                x + 1,
                            ),
                            spots = cell?.let { TileMove.spotsFor(screen, it) }.orEmpty(),
                            targets = cell?.let { TileMove.targetsFor(config, screenId, it) }.orEmpty(),
                            shrinks = cell?.let { it.w > 1 || it.h > 1 } ?: false,
                            screenName = { id -> config.screenById(id)?.name },
                            appLabel = { a -> apps.labelFor(a.packageName, a.activityName) },
                            onSpot = { platz ->
                                val gerueckt =
                                    TileMove.moveWithin(store.current, screenId, x, y, platz.x, platz.y)
                                if (gerueckt != null) {
                                    store.update { gerueckt }
                                    // the anchor moves along, or the editor would then edit
                                    // the empty slot the tile came from.
                                    x = platz.x
                                    y = platz.y
                                }
                                mode = Mode.MENU
                            },
                            onPick = { ziel ->
                                val verschoben = TileMove.move(store.current, screenId, x, y, ziel.id)
                                if (verschoben != null) {
                                    store.update { verschoben }
                                    finish()
                                } else {
                                    mode = Mode.MENU
                                }
                            },
                        )

                        Mode.RESIZE -> ResizePanel(
                            screen = screen,
                            cell = cell,
                            onApply = { operation ->
                                store.updateScreen(screenId) { current ->
                                    val live = current.cellAt(x, y) ?: return@updateScreen current
                                    operation(current, live)
                                }
                                // Anker auf die neue Ecke setzen
                                store.current.screenById(screenId)?.let { updated ->
                                    val moved = updated.cells.firstOrNull { it.covers(x, y) }
                                        ?: updated.cells.firstOrNull { it.button == button }
                                    if (moved != null) {
                                        x = moved.x
                                        y = moved.y
                                    }
                                }
                            },
                            onDone = { mode = Mode.MENU },
                        )

                        Mode.PICK_COLOR -> ColorPicker(
                            selected = button.colorIndex,
                            hue = button.colorHue,
                            onPick = { index ->
                                write(TileEdits.withColorIndex(button, index))
                                mode = Mode.MENU
                            },
                            onFree = { mode = Mode.PICK_HUE },
                        )

                        Mode.PICK_ICON -> IconPicker(
                            selected = button.iconName,
                            onPick = { name ->
                                write(TileEdits.withIcon(button, name))
                                mode = Mode.MENU
                            },
                        )

                        Mode.PICK_HUE -> HuePicker(
                            selected = button.colorHue,
                            onPick = { ton ->
                                write(TileEdits.withColorHue(button, ton))
                                mode = Mode.MENU
                            },
                        )
                    }
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_SCREEN = "screen"
        private const val EXTRA_X = "x"
        private const val EXTRA_Y = "y"

        fun intent(context: Context, screenId: String, x: Int, y: Int): Intent =
            Intent(context, TileEditorActivity::class.java)
                .putExtra(EXTRA_SCREEN, screenId)
                .putExtra(EXTRA_X, x)
                .putExtra(EXTRA_Y, y)
    }
}

@Composable
private fun MenuList(
    button: Button,
    /** Wo diese Kachel liegt - Screen und Platz, fertig zusammengesetzt. */
    platz: String,
    screenName: (String) -> String?,
    appLabel: (ButtonAction.App) -> String?,
    onPickBuiltin: () -> Unit,
    onPickApp: () -> Unit,
    onPickContact: () -> Unit,
    onPickShortcut: () -> Unit,
    onPickWidget: () -> Unit,
    onPickScreen: () -> Unit,
    onPickLink: () -> Unit,
    onPickMessage: () -> Unit,
    onToggleBlink: () -> Unit,
    onPickLongPress: () -> Unit,
    onClearLongPress: () -> Unit,
    mayAddFolder: Boolean,
    onNewFolder: () -> Unit,
    onEditLabel: () -> Unit,
    moveTargets: Int,
    onMove: () -> Unit,
    onPickColor: () -> Unit,
    onPickIcon: () -> Unit,
    onResize: (() -> Unit)?,
    onClear: () -> Unit,
    onDone: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.editor_title)) }
        // *which* tile, not just a tile: with only the heading, tapping one of two empty
        // tiles showed nowhere which one was caught, and the editor opening on the wrong
        // screen would have been visible at once had the screen's name stood here.
        item {
            Text(
                text = platz,
                color = palette.onBackground,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.editor_current),
                secondary = describe(button, screenName, appLabel),
                surface = palette.surfaceDefault,
            )
        }
        item { BigRow(stringResource(R.string.editor_pick_app), icon = Icons.Filled.Apps, onClick = onPickApp) }
        item { BigRow(stringResource(R.string.editor_pick_contact), icon = Icons.Filled.Person, onClick = onPickContact) }
        item { BigRow(stringResource(R.string.editor_pick_builtin), icon = Icons.Filled.Tune, onClick = onPickBuiltin) }
        item { BigRow(stringResource(R.string.editor_pick_shortcut), icon = Icons.Filled.Bolt, onClick = onPickShortcut) }
        item { BigRow(stringResource(R.string.editor_pick_widget), icon = Icons.Filled.Widgets, onClick = onPickWidget) }
        item { BigRow(stringResource(R.string.editor_pick_screen), icon = Icons.AutoMirrored.Filled.ArrowForward, onClick = onPickScreen) }
        // not inside a folder: two levels destroy the overview large tiles are meant to
        // create. see `PLAN.md` 4.9.
        item {
            BigRow(
                stringResource(R.string.editor_pick_message),
                icon = Icons.AutoMirrored.Filled.Message,
                onClick = onPickMessage,
            )
        }
        item {
            BigRow(
                stringResource(R.string.editor_pick_link),
                icon = Icons.Filled.Public,
                onClick = onPickLink,
            )
        }
        if (mayAddFolder) {
            item {
                BigRow(
                    stringResource(R.string.editor_new_folder),
                    icon = Icons.Filled.Folder,
                    onClick = onNewFolder,
                )
            }
        }
        item {
            BigRow(
                stringResource(
                    if (button.action is ButtonAction.Folder) R.string.folder_rename else R.string.editor_label,
                ),
                icon = Icons.Filled.Edit,
                onClick = onEditLabel,
            )
        }
        // only when there is a target at all: offering a row that then says it cannot is
        // worse than leaving it out.
        if (button.action != ButtonAction.None && moveTargets > 0) {
            item {
                BigRow(
                    stringResource(R.string.editor_move),
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    onClick = onMove,
                )
            }
        }
        // only where blinking means anything: a clock and an empty tile have no
        // notifications, and a switch for them would be a promise without cover.
        if (TileNotifications.canBlink(button.action)) {
            item {
                BigRow(
                    label = stringResource(
                        if (button.blink) R.string.editor_blink_on else R.string.editor_blink_off,
                    ),
                    secondary = stringResource(R.string.editor_blink_hint),
                    icon = Icons.Filled.NotificationsActive,
                    surface = if (button.blink) palette.surfaceAccent else palette.surfaceDefault,
                    onClick = onToggleBlink,
                )
            }
        }
        // the second assignment (`PLAN.md` 4.3), only where the tile does anything at all:
        // an empty tile with one would be a riddle.
        if (button.action != ButtonAction.None) {
            item {
                BigRow(
                    label = if (button.longPress == null) {
                        stringResource(R.string.editor_long_press_set)
                    } else {
                        stringResource(
                            R.string.editor_long_press_is,
                            describe(
                                button.copy(action = button.longPress!!, label = null),
                                screenName,
                                appLabel,
                            ),
                        )
                    },
                    secondary = stringResource(R.string.editor_long_press_hint),
                    icon = Icons.Filled.TouchApp,
                    onClick = onPickLongPress,
                )
            }
            if (button.longPress != null) {
                item {
                    BigRow(
                        label = stringResource(R.string.editor_long_press_clear),
                        onClick = onClearLongPress,
                    )
                }
                // a second assignment comes before the editor (see LongPress.decide), so
                // for *this* tile the long press no longer leads here. `a11y_editor_moved`
                // says the same when a setting takes the long press; here the tile takes it.
                item {
                    Text(
                        text = stringResource(R.string.editor_long_press_takes_editor),
                        color = palette.dangerText,
                        fontSize = bigSp(15f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }
        }
        item {
            BigRow(
                label = stringResource(R.string.editor_pick_icon),
                // with icons globally off the choice would have no visible effect. the row
                // stays anyway: it holds as soon as the icons are back on.
                secondary = if (LocalIconVisibility.current == IconVisibility.NEVER) {
                    stringResource(R.string.editor_pick_icon_off)
                } else {
                    null
                },
                icon = Icons.Filled.Category,
                onClick = onPickIcon,
            )
        }
        item { BigRow(stringResource(R.string.editor_color), icon = Icons.Filled.Palette, onClick = onPickColor) }
        if (onResize != null) {
            item { BigRow(stringResource(R.string.editor_resize), icon = Icons.Filled.OpenInFull, onClick = onResize) }
        }
        // only when there is something to empty (see TileEdits.clearable): a fresh tile had
        // a red button here that did nothing.
        if (TileEdits.clearable(button)) {
            item {
                BigRow(
                    label = stringResource(R.string.editor_clear),
                    icon = Icons.Filled.Delete,
                    surface = palette.surfaceDanger,
                    onClick = onClear,
                )
            }
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

/**
 * the same derivation as on the home screen, see [TileLabel]. the editor once had its own
 * and called a folder just folder where the home screen showed the name.
 */
@Composable
private fun describe(
    button: Button,
    screenName: (String) -> String?,
    appLabel: (ButtonAction.App) -> String?,
): String {
    val context = LocalContext.current
    return TileLabel.of(
        button,
        TileLabel.Words(
            emptyTile = stringResource(R.string.empty_tile),
            folder = stringResource(R.string.folder),
            nextScreen = stringResource(R.string.next_screen),
            widget = stringResource(R.string.editor_pick_widget),
        ),
        screenName = screenName,
        appLabel = appLabel,
        builtinLabel = { builtin -> context.getString(builtin.labelRes()) },
    )
}

/**
 * what holding should do.
 *
 * `PLAN.md` 4.3 promises every action on the long press, independent of the short one, but
 * only apps and built-in functions were offered. no widget and no folder: neither is a
 * handle, both are the content of a cell.
 */
@Composable
private fun LongPressKindList(onPick: (Mode) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.editor_long_press_kind)) }
        item {
            BigRow(
                label = stringResource(R.string.editor_long_press_kind_app),
                icon = Icons.Filled.Apps,
                onClick = { onPick(Mode.PICK_APP) },
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.editor_long_press_kind_contact),
                icon = Icons.Filled.Person,
                onClick = { onPick(Mode.PICK_CONTACT) },
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.editor_long_press_kind_builtin),
                icon = Icons.Filled.TouchApp,
                onClick = { onPick(Mode.PICK_BUILTIN) },
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.editor_long_press_kind_shortcut),
                icon = Icons.Filled.Bolt,
                onClick = { onPick(Mode.PICK_SHORTCUT_APP) },
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.editor_long_press_kind_screen),
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = { onPick(Mode.PICK_SCREEN) },
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.editor_long_press_kind_message),
                icon = Icons.AutoMirrored.Filled.Message,
                onClick = { onPick(Mode.EDIT_NUMBER) },
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.editor_long_press_kind_link),
                icon = Icons.Filled.Public,
                onClick = { onPick(Mode.EDIT_LINK) },
            )
        }
    }
}

@Composable
private fun BuiltinList(onPick: (Builtin) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.editor_pick_builtin)) }
        items(Builtin.entries.toList()) { builtin ->
            BigRow(
                label = stringResource(builtin.labelRes()),
                icon = builtin.icon(),
                onClick = { onPick(builtin) },
            )
        }
    }
}

@Composable
private fun AppList(
    repository: AppRepository,
    headingRes: Int = R.string.editor_pick_app,
    onPick: (LaunchableApp) -> Unit,
) {
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = repository.loadApps() }

    val shown = remember(apps, query) { TextSearch.filter(apps, query) { it.label } }
    val palette = LocalBigPalette.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // the heading gives way once searching starts: with the keyboard open only one or
        // two result rows are left of 581 dp, and the heading says nothing the field does not.
        if (query.isEmpty()) {
            BigHeading(stringResource(headingRes))
        }
        BigSearchField(
            value = query,
            onValueChange = { query = it },
            hint = stringResource(R.string.search_apps),
        )
        if (shown.isEmpty() && apps.isNotEmpty()) {
            Text(
                text = stringResource(R.string.search_no_match),
                color = palette.onBackground,
                fontSize = bigSp(18f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(shown, key = { it.packageName + it.activityName }) { app ->
                BigRow(
                    label = app.label,
                    iconBitmap = repository.iconFor(app.packageName, app.activityName)
                        ?.toBitmap(72, 72)?.asImageBitmap(),
                    onClick = { onPick(app) },
                )
            }
        }
    }
}

@Composable
private fun LabelEditor(
    initial: String,
    automatic: String,
    titleRes: Int,
    hintRes: Int,
    onDone: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(stringResource(titleRes))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(fontSize = bigSp(26f), fontWeight = FontWeight.Bold),
            // the automatic name stands pale in the empty field, or one sees an empty box
            // and does not know what is being replaced.
            placeholder = {
                Text(automatic, fontSize = bigSp(22f), fontWeight = FontWeight.Bold)
            },
            // the keyboard's tick key takes over: on three inches the keyboard covers the
            // done button entirely.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone(text) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(hintRes),
            color = palette.onBackground,
            fontSize = bigSp(15f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        BigRow(
            label = stringResource(R.string.editor_done),
            surface = palette.surfaceAccent,
            onClick = { onDone(text) },
        )
    }
}

@Composable
private fun ColorPicker(selected: Int, hue: Float?, onPick: (Int?) -> Unit, onFree: () -> Unit) {
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(stringResource(R.string.editor_color))
        // the colour names run in the same order as the colours: without them the swatches
        // are six nameless buttons to a screen reader.
        val names = stringArrayResource(R.array.tile_colors)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            palette.tiles.forEachIndexed { index, color ->
                val chosen = index == selected
                val plain = names.getOrElse(index) { "" }
                // the state belongs in the name: the bare selected property does not reach
                // the accessibility interface, checked in the device's node dump.
                val name = if (chosen) stringResource(UiR.string.a11y_chosen, plain) else plain
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(LocalCornerRadius.current))
                        .background(color)
                        .then(
                            if (chosen) {
                                Modifier.border(4.dp, palette.onBackground, RoundedCornerShape(LocalCornerRadius.current))
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onPick(index) }
                        .semantics {
                            contentDescription = name
                            if (chosen) this.selected = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // a border alone does not carry the choice: on a coloured swatch it
                    // reads as decoration. the tick is unambiguous.
                    if (chosen) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = palette.onTile,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
        BigRow(
            label = stringResource(R.string.editor_color_auto),
            selected = selected < 0 && hue == null,
            onClick = { onPick(null) },
        )

        // `PLAN.md` 4.2 names three kinds: automatic, palette, free. the free one asks only
        // for the hue; FreeTileColor computes the lightness so both thresholds from 3.3
        // hold. a picker that accepts unreadable colours is worse than none.
        BigRow(
            label = stringResource(R.string.editor_color_free),
            secondary = stringResource(R.string.editor_color_free_hint),
            icon = Icons.Filled.Palette,
            selected = hue != null,
            onClick = onFree,
        )
    }
}

/**
 * the icon picker. `PLAN.md` 2.2 and 3.4.
 *
 * four side by side, in headed groups, the same layout as the colours. automatic at the very
 * top, because that is where every tile starts, and the way back must be as large as the
 * way there.
 */
@Composable
private fun IconPicker(selected: String?, onPick: (String?) -> Unit) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.editor_pick_icon)) }
        item {
            BigRow(
                label = stringResource(R.string.icon_automatic),
                secondary = stringResource(R.string.icon_automatic_hint),
                selected = selected == null,
                onClick = { onPick(null) },
            )
        }
        IconCatalogue.GROUPS.forEach { gruppe ->
            item { BigHeading(stringResource(gruppe.titleRes)) }
            // three side by side, not four: at four the word breaks mid-word, and a broken
            // word is worse than one more row.
            items(gruppe.names.chunked(3)) { reihe ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    reihe.forEach { name ->
                        val bild = IconCatalogue.vectorFor(name)
                        val gewaehlt = name == selected
                        val flaeche =
                            if (gewaehlt) palette.surfaceAccent else palette.surfaceDefault
                        val wort = IconCatalogue.labelFor(name)?.let { stringResource(it) } ?: name
                        // the state belongs in the name, see the colour picker beside it.
                        val ansage =
                            if (gewaehlt) stringResource(UiR.string.a11y_chosen, wort) else wort
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(LocalCornerRadius.current))
                                .background(flaeche.fill)
                                .then(
                                    if (gewaehlt) {
                                        Modifier.border(
                                            4.dp,
                                            palette.onBackground,
                                            RoundedCornerShape(LocalCornerRadius.current),
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { onPick(name) }
                                .semantics {
                                    if (gewaehlt) {
                                        this.selected = true
                                        contentDescription = ansage
                                    }
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (bild != null) {
                                Icon(
                                    imageVector = bild,
                                    // the word stands below; a second announcement would be
                                    // the same answer twice.
                                    contentDescription = null,
                                    tint = flaeche.ink,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                            Text(
                                text = wort,
                                color = flaeche.ink,
                                fontSize = bigSp(13f),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    // the last row fills up, or its swatches would be wider than the rest.
                    repeat(3 - reihe.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * the free colour choice: twenty-four hues, each in the shade readable in this theme.
 *
 * the hue is chosen, not the lightness, which hangs on the theme and on two thresholds and
 * is nothing to ask anyone with a slider.
 */
@Composable
private fun HuePicker(selected: Float?, onPick: (Float) -> Unit) {
    val palette = LocalBigPalette.current
    val grund = palette.background.toArgbLong()
    val schrift = palette.onTile.toArgbLong()
    val gewicht = FreeTileColor.targetLuminance(palette.tiles.map { it.toArgbLong() })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(stringResource(R.string.editor_color_free))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FreeTileColor.hues.chunked(4)) { reihe ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    reihe.forEach { ton ->
                        val farbe = Color(FreeTileColor.forHue(ton, grund, schrift, gewicht).toInt())
                        val chosen = selected != null && abs(selected - ton) < 0.5f
                        val schlicht = hueName(ton)
                        // the state belongs in the name, as with the six palette swatches.
                        val name = if (chosen) stringResource(UiR.string.a11y_chosen, schlicht) else schlicht
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(LocalCornerRadius.current))
                                .background(farbe)
                                .then(
                                    if (chosen) {
                                        Modifier.border(
                                            4.dp,
                                            palette.onBackground,
                                            RoundedCornerShape(LocalCornerRadius.current),
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { onPick(ton) }
                                .semantics {
                                    contentDescription = name
                                    if (chosen) this.selected = true
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (chosen) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = palette.onTile,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }
                    // Eine angefangene Reihe darf die Felder nicht breiter machen.
                    repeat(4 - reihe.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** a name for the hue, so the swatches are not twenty-four nameless buttons. */
private fun hueName(hue: Float): String = "${hue.toInt()}°"

@Composable
private fun ContactList(
    repository: ContactRepository,
    granted: Boolean,
    onGrant: () -> Unit,
    onPick: (PhoneContact) -> Unit,
) {
    val palette = LocalBigPalette.current
    var all by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(granted) {
        if (granted) {
            loading = true
            all = repository.load()
            loading = false
        }
    }

    if (!granted) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BigHeading(stringResource(R.string.editor_pick_contact))
            Text(
                text = stringResource(R.string.contacts_permission),
                color = palette.onBackground,
                fontSize = bigSp(18f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
            BigRow(
                label = stringResource(R.string.contacts_grant),
                surface = palette.surfaceAccent,
                onClick = onGrant,
            )
        }
        return
    }

    val shown = remember(all, query) { TextSearch.filter(all, query) { it.name } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (query.isEmpty()) {
            BigHeading(stringResource(R.string.editor_pick_contact))
        }
        BigSearchField(
            value = query,
            onValueChange = { query = it },
            hint = stringResource(R.string.search_contacts),
        )
        if (loading) {
            Text(
                text = stringResource(R.string.contacts_loading),
                color = palette.onBackground,
                fontSize = bigSp(18f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
        } else if (shown.isEmpty()) {
            Text(
                text = stringResource(R.string.contacts_no_match),
                color = palette.onBackground,
                fontSize = bigSp(18f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(shown, key = { it.id }) { contact ->
                BigRow(
                    label = contact.name,
                    secondary = contact.numbers.firstOrNull()?.number
                        ?.let(PhoneNumbers::forDisplay),
                    leading = { ContactAvatar(contact.name, contact.photoUri) },
                    onClick = { onPick(contact) },
                )
            }
        }
    }
}

@Composable
private fun NumberList(contact: PhoneContact?, onPick: (String) -> Unit) {
    if (contact == null) return
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.contacts_pick_number)) }
        items(contact.numbers) { number ->
            BigRow(
                label = PhoneNumbers.forDisplay(number.number),
                secondary = number.label,
                onClick = { onPick(number.number) },
            )
        }
    }
}

@Composable
private fun ContactModeList(onPick: (ContactMode) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.contacts_action)) }
        item {
            BigRow(
                stringResource(R.string.contacts_action_call),
                icon = Icons.Filled.Call,
                onClick = { onPick(ContactMode.CALL) },
            )
        }
        item {
            BigRow(
                stringResource(R.string.contacts_action_sms),
                icon = Icons.AutoMirrored.Filled.Message,
                onClick = { onPick(ContactMode.SMS) },
            )
        }
        item {
            BigRow(
                stringResource(R.string.contacts_action_ask),
                icon = Icons.Filled.QuestionMark,
                onClick = { onPick(ContactMode.ASK) },
            )
        }
    }
}

/**
 * shows only the steps that are possible right now: greyed-out buttons would have to be
 * tapped to learn they do nothing, and on three inches that is wasted room.
 */
@Composable
private fun ResizePanel(
    screen: Screen,
    cell: Cell?,
    onApply: ((Screen, Cell) -> Screen) -> Unit,
    onDone: () -> Unit,
) {
    if (cell == null) {
        onDone()
        return
    }
    val palette = LocalBigPalette.current
    val grow = CellLayout.stretchable(screen, cell)
    val shrink = CellLayout.shrinkable(cell)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.editor_resize)) }
        item {
            BigRow(
                label = stringResource(R.string.resize_current, cell.w, cell.h),
                surface = palette.surfaceDefault,
            )
        }
        items(grow) { direction ->
            BigRow(
                label = stringResource(growLabel(direction)),
                icon = arrowFor(direction),
                onClick = { onApply { board, live -> CellLayout.stretch(board, live, direction) } },
            )
        }
        items(shrink) { direction ->
            BigRow(
                label = stringResource(shrinkLabel(direction)),
                icon = arrowFor(direction),
                onClick = { onApply { board, live -> CellLayout.shrink(board, live, direction) } },
            )
        }
        // without this sentence only the current size and done would stand here, which
        // looks broken when in fact there is simply no room around it.
        if (grow.isEmpty() && shrink.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.resize_no_room),
                    color = palette.onBackground,
                    fontSize = bigSp(16f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
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

private fun growLabel(direction: Direction) = when (direction) {
    Direction.UP -> R.string.resize_grow_up
    Direction.DOWN -> R.string.resize_grow_down
    Direction.LEFT -> R.string.resize_grow_left
    Direction.RIGHT -> R.string.resize_grow_right
}

private fun shrinkLabel(direction: Direction) = when (direction) {
    Direction.UP -> R.string.resize_shrink_up
    Direction.DOWN -> R.string.resize_shrink_down
    Direction.LEFT -> R.string.resize_shrink_left
    Direction.RIGHT -> R.string.resize_shrink_right
}

private fun arrowFor(direction: Direction) = when (direction) {
    Direction.UP -> Icons.Filled.KeyboardArrowUp
    Direction.DOWN -> Icons.Filled.KeyboardArrowDown
    Direction.LEFT -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
    Direction.RIGHT -> Icons.AutoMirrored.Filled.KeyboardArrowRight
}

@Composable
private fun ScreenPicker(
    config: LauncherConfig,
    currentScreenId: String,
    onPick: (String) -> Unit,
    onCreate: () -> Unit,
) {
    val palette = LocalBigPalette.current
    // folders are no jump targets: a folder belongs to its tile and lies over it as an
    // overlay. as a target it becomes an ordinary screen, without the closing row and
    // without an entry in the screen list, while the tile that opens it as an overlay still
    // stands beside it - two ways to the same thing that look different.
    //
    // `SwipeChain` has always filtered folders out; this list was the only one that did not.
    val others = config.screens.filter { it.id != currentScreenId && !it.isFolder }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.editor_pick_screen)) }
        items(others, key = { it.id }) { target ->
            BigRow(
                label = target.name,
                secondary = stringResource(R.string.screen_grid, target.cols, target.rows),
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = { onPick(target.id) },
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.screen_new),
                icon = Icons.Filled.Add,
                surface = palette.surfaceAccent,
                onClick = onCreate,
            )
        }
    }
}

/** android hands shortcuts only to the default launcher, and that has to be said. */
@Composable
private fun NeedsHomeRole(onChoose: () -> Unit) {
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(stringResource(R.string.editor_pick_shortcut))
        Text(
            text = stringResource(R.string.shortcut_needs_home_role),
            color = palette.onBackground,
            fontSize = bigSp(16f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
        BigRow(
            label = stringResource(R.string.set_as_home),
            surface = palette.surfaceAccent,
            onClick = onChoose,
        )
    }
}

@Composable
private fun ShortcutList(
    app: LaunchableApp?,
    answer: ShortcutAnswer,
    onBack: () -> Unit,
    onPick: (ShortcutRow) -> Unit,
) {
    val palette = LocalBigPalette.current
    val rows = (answer as? ShortcutAnswer.Rows)?.rows.orEmpty()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(app?.label ?: stringResource(R.string.editor_pick_shortcut)) }
        if (rows.isEmpty()) {
            // two sentences for two states: the app has none, or we could not look. the way
            // out below is the same.
            item {
                Text(
                    text = stringResource(
                        if (answer is ShortcutAnswer.Failed) {
                            R.string.shortcut_unreadable
                        } else {
                            R.string.shortcut_none
                        },
                    ),
                    color = palette.onBackground,
                    fontSize = bigSp(16f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
            // most apps offer no shortcuts, and the list does not say which. without this
            // button one stands before a sentence and needs the back gesture to try another.
            item {
                BigRow(
                    label = stringResource(R.string.shortcut_other_app),
                    surface = palette.surfaceAccent,
                    onClick = onBack,
                )
            }
        }
        items(rows, key = { it.packageName + "/" + it.id }) { row ->
            BigRow(
                label = Shortcuts.labelOf(row),
                icon = Icons.Filled.Bolt,
                onClick = { onPick(row) },
            )
        }
    }
}

/**
 * the widget picker. beside the name stands how many slots the widget needs, or an oversized
 * one lands on a small tile and comes out squeezed.
 */
@Composable
private fun WidgetPicker(
    rows: List<WidgetProviderRow>,
    cellWidthDp: Float,
    cellHeightDp: Float,
    gutterDp: Float,
    onPick: (WidgetProviderRow) -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.editor_pick_widget)) }
        // the hint stands *before* the choice: a widget takes the touch first, so the usual
        // way to the editor is closed for that tile. there is another one through the
        // settings, but without knowing it the tile seems grown fast.
        if (rows.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.widget_long_press_hint),
                    color = palette.onBackground,
                    fontSize = bigSp(15f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
        if (rows.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.widget_none),
                    color = palette.onBackground,
                    fontSize = bigSp(16f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
        items(rows, key = { it.component }) { row ->
            val (needX, needY) = WidgetFit.requirement(row, cellWidthDp, cellHeightDp, gutterDp)
            BigRow(
                label = row.label,
                secondary = stringResource(
                    if (WidgetFit.fixedSize(row)) R.string.widget_fixed
                    else R.string.widget_needs,
                    row.appLabel,
                    needX,
                    needY,
                ),
                icon = Icons.Filled.Widgets,
                onClick = { onPick(row) },
            )
        }
    }
}

/**
 * the question before deleting a folder.
 *
 * throwing a folder away throws away everything in it, and unlike a single tile that is not
 * visible, the contents being folded up. hence the count here, and hence buttons named after
 * their action rather than yes and no.
 */
@Composable
private fun FolderDeletePanel(
    name: String,
    count: Int,
    onKeep: () -> Unit,
    onDelete: () -> Unit,
    /** is the tile being reassigned rather than emptied? then the button reads differently. */
    replacing: Boolean = false,
) {
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(stringResource(R.string.folder_delete_title, name))
        Text(
            text = pluralStringResource(R.plurals.folder_delete_body, count, count),
            color = palette.onBackground,
            fontSize = bigSp(17f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        BigRow(
            label = stringResource(
                if (replacing) R.string.folder_replace_confirm else R.string.folder_delete_confirm,
            ),
            icon = Icons.Filled.Delete,
            surface = palette.surfaceDanger,
            onClick = onDelete,
        )
        BigRow(
            label = stringResource(R.string.folder_delete_keep),
            surface = palette.surfaceAccent,
            onClick = onKeep,
        )
    }
}

/**
 * where the tile should go.
 *
 * only places with actual room are offered: a target that then refuses would be a button
 * that does nothing. beside the name stands whether it is a folder and how much is free.
 */
@Composable
private fun MoveTargetList(
    /** which tile is being moved, and where it lies now. */
    welche: String,
    spots: List<TileMove.Spot>,
    targets: List<Screen>,
    shrinks: Boolean,
    screenName: (String) -> String?,
    appLabel: (ButtonAction.App) -> String?,
    onSpot: (TileMove.Spot) -> Unit,
    onPick: (Screen) -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.editor_move)) }
        // which tile, though? every target below is a row and column, so without this line
        // the whole screen is a list of abstract slots.
        item {
            Text(
                text = welche,
                color = palette.onBackground,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        // the own screen first: moving a tile usually means rearranging the screen one is
        // standing on. rows and columns count from 1, since row 0 reads like a fault.
        if (spots.isNotEmpty()) {
            item { BigHeading(stringResource(R.string.move_on_this_screen)) }
            items(spots, key = { "platz-${it.x}-${it.y}" }) { platz ->
                BigRow(
                    label = stringResource(R.string.move_spot, platz.y + 1, platz.x + 1),
                    secondary = platz.occupant?.let { belegt ->
                        stringResource(
                            R.string.move_spot_swap,
                            describe(belegt.button, screenName, appLabel),
                        )
                    } ?: stringResource(R.string.move_spot_free),
                    icon = if (platz.occupant == null) Icons.Filled.CropFree else Icons.Filled.SwapHoriz,
                    onClick = { onSpot(platz) },
                )
            }
        }
        if (targets.isNotEmpty()) {
            item { BigHeading(stringResource(R.string.move_other_screens)) }
            // a large tile arrives elsewhere as a single slot ([TileMove.move]), or it
            // would hang over the edge. said before it happens; afterwards it looks as if
            // moving had broken the tile.
            if (shrinks) {
                item {
                    Text(
                        text = stringResource(R.string.move_shrinks),
                        color = palette.onBackground,
                        fontSize = bigSp(15f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }
            items(targets, key = { it.id }) { ziel ->
                BigRow(
                    label = ziel.name,
                    secondary = pluralStringResource(
                        if (ziel.isFolder) R.plurals.move_target_folder else R.plurals.move_target_screen,
                        ziel.freeSlots().size,
                        ziel.freeSlots().size,
                    ),
                    icon = if (ziel.isFolder) Icons.Filled.Folder else Icons.Filled.ViewCarousel,
                    onClick = { onPick(ziel) },
                )
            }
        }
    }
}

/**
 * type the number this tile starts a message to.
 *
 * the digit keypad and not the full one: the recipient is a number, not a name. the tick key
 * takes over, since done is covered with the keyboard open. nothing here is sent; the tile
 * opens the writing screen later, see [MessageTile].
 */
@Composable
private fun NumberEditor(initial: String, onDone: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(stringResource(R.string.editor_pick_message))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(fontSize = bigSp(22f), fontWeight = FontWeight.Bold),
            placeholder = { Text(stringResource(R.string.message_number_example), fontSize = bigSp(18f)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone(text) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.message_number_hint),
            color = palette.onBackground,
            fontSize = bigSp(15f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        BigRow(
            label = stringResource(R.string.editor_done),
            surface = palette.surfaceAccent,
            onClick = { onDone(text) },
        )
    }
}

/**
 * Die Adresse einer Webseiten-Kachel eingeben.
 *
 * Ohne „https://" davor - das ergaenzt [LinkTarget], weil es auf drei Zoll niemand tippt.
 * Die Haken-Taste der Tastatur uebernimmt, denn mit offener Tastatur ist "Fertig" verdeckt.
 */
@Composable
private fun LinkEditor(initial: String, onDone: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(stringResource(R.string.editor_pick_link))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(fontSize = bigSp(22f), fontWeight = FontWeight.Bold),
            placeholder = { Text(stringResource(R.string.link_example), fontSize = bigSp(18f)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone(text) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.link_hint),
            color = palette.onBackground,
            fontSize = bigSp(15f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        BigRow(
            label = stringResource(R.string.editor_done),
            surface = palette.surfaceAccent,
            onClick = { onDone(text) },
        )
    }
}
