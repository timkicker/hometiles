package org.biglau.tiles

import android.Manifest
import android.content.Context
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.R
import org.biglau.actions.Intents
import org.biglau.apps.AppRepository
import org.biglau.search.TextSearch
import org.biglau.shortcuts.ShortcutRepository
import org.biglau.shortcuts.ShortcutRow
import org.biglau.shortcuts.Shortcuts
import org.biglau.widgets.WidgetFit
import org.biglau.widgets.WidgetHostController
import org.biglau.widgets.WidgetProviderRow
import org.biglau.apps.LaunchableApp
import org.biglau.data.Builtin
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
import org.biglau.ui.theme.LocalBigPalette

private enum class Mode { MENU, MOVE, EDIT_LINK, PICK_BUILTIN, PICK_APP, PICK_CONTACT, PICK_NUMBER, PICK_MODE, EDIT_LABEL, PICK_COLOR, RESIZE, PICK_SCREEN, PICK_SHORTCUT_APP, PICK_SHORTCUT, PICK_WIDGET }

/**
 * Belegt eine einzelne Kachel. Schreibt direkt in den ConfigStore - der Homescreen
 * beobachtet denselben Fluss und zeichnet sich neu, sobald hier etwas passiert.
 */
class TileEditorActivity : ComponentActivity() {

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
            // Beim Verkleinern nach oben oder links wandert die Ecke der Zelle - der Anker
            // muss mitwandern, sonst zeigt der Editor plaetzlich auf einen leeren Platz.
            var x by rememberSaveable { mutableStateOf(startX) }
            var y by rememberSaveable { mutableStateOf(startY) }
            val cell = screen.cellAt(x, y)
            // Die echten Zellmasse dieses Screens - vorher standen hier die Werte des
            // Standardrasters fest verdrahtet, was bei jedem anderen Raster falsch war.
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
            var chosenContact by remember { mutableStateOf<PhoneContact?>(null) }
            var chosenNumber by remember { mutableStateOf<String?>(null) }
            var shortcutApp by remember { mutableStateOf<LaunchableApp?>(null) }
            var clearing by remember { mutableStateOf<ButtonAction.Folder?>(null) }
            // Vor dem Editor eine PIN, wenn eine gesetzt und der Schutz eingeschaltet ist.
            // Ein langer Druck passiert schneller, als man denkt.
            var locked by remember {
                mutableStateOf(
                    Pin.protectsEditor(config.security.pin, config.security.pinProtectsEditor),
                )
            }
            val widgets = remember { WidgetHostController.get(this@TileEditorActivity) }
            /**
             * Vor jeder Neubelegung: eine bisher hier liegende Widget-Kennung zurueckgeben.
             * Sonst bleibt sie beim AppWidgetHost fuer immer belegt, obwohl niemand sie
             * mehr benutzt - ein Leck, das man nirgends sieht.
             */
            fun releaseWidgetIfAny(current: Button) {
                (current.action as? ButtonAction.Widget)?.let {
                    WidgetHostController.get(this@TileEditorActivity).release(it.widgetId)
                }
            }

            fun write(next: Button) {
                if (next.action !is ButtonAction.Widget) releaseWidgetIfAny(button)
                store.setButton(screenId, x, y, next)
            }

            var pendingWidget by remember { mutableStateOf<Pair<Int, WidgetProviderRow>?>(null) }

            fun finishWidget(id: Int, provider: WidgetProviderRow) {
                val next = TileEdits.withAction(
                    button,
                    ButtonAction.Widget(provider.component, id, provider.label),
                )
                write(next)

                // Die Zelle gleich auf die noetige Groesse bringen. Ein zu grosses Widget
                // wird sonst gestaucht und sieht kaputt aus - und der Nutzer muesste selbst
                // darauf kommen, dass er die Kachel vergroessern muss.
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
                        Toast.makeText(
                            this@TileEditorActivity,
                            getString(R.string.widget_no_room, needX, needY),
                            Toast.LENGTH_LONG,
                        ).show()
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
                    // Abgebrochen: die vergebene Kennung wieder freigeben, sonst bleibt sie belegt.
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
            var contactsGranted by remember { mutableStateOf(contacts.hasPermission()) }
            val askForContacts = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                contactsGranted = granted
                if (!granted) mode = Mode.MENU
            }


            BigLauTheme(
                config.appearance.theme,
                config.appearance.textScale,
                haptics = config.behaviour.hapticFeedback,
            ) {
                BackHandler(enabled = mode != Mode.MENU) { mode = Mode.MENU }

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

                    when (mode) {
                        Mode.MENU -> MenuList(
                            button = button,
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
                            moveTargets = cell?.let { TileMove.targetsFor(config, screenId, it).size } ?: 0,
                            onMove = { mode = Mode.MOVE },
                            onPickShortcut = { mode = Mode.PICK_SHORTCUT_APP },
                            onPickWidget = { mode = Mode.PICK_WIDGET },
                            onPickScreen = { mode = Mode.PICK_SCREEN },
                            onPickLink = { mode = Mode.EDIT_LINK },
                            mayAddFolder = FolderEdits.mayContainFolder(screen),
                            onNewFolder = {
                                val id = ScreenEdits.freeId(store.current, base = "folder")
                                val name = getString(R.string.folder_default_name)
                                store.update {
                                    ScreenEdits.add(it, FolderEdits.newFolder(id, name, screen))
                                }
                                write(TileEdits.withAction(button, ButtonAction.Folder(id)))
                                mode = Mode.MENU
                            },
                            onPickColor = { mode = Mode.PICK_COLOR },
                            onResize = if (cell != null) ({ mode = Mode.RESIZE }) else null,
                            onClear = {
                                val ordner = button.action as? ButtonAction.Folder
                                if (ordner == null) {
                                    releaseWidgetIfAny(button)
                                    store.clearButton(screenId, x, y)
                                    finish()
                                } else {
                                    // Erst fragen. Die Kachel zu leeren wuerde den Ordner
                                    // zurueckliegen lassen, ohne dass noch ein Weg hinfuehrt -
                                    // samt allem, was darin liegt.
                                    clearing = ordner
                                }
                            },
                            onDone = { finish() },
                        )

                        Mode.PICK_BUILTIN -> BuiltinList { builtin ->
                            write(TileEdits.withAction(button, ButtonAction.Action(builtin)))
                            mode = Mode.MENU
                        }

                        Mode.PICK_APP -> AppList(apps) { app ->
                            write(
                                TileEdits.withAction(
                                    button,
                                    ButtonAction.App(app.packageName, app.activityName),
                                )
                            )
                            mode = Mode.MENU
                        }

                        Mode.PICK_CONTACT -> ContactList(
                            repository = contacts,
                            granted = contactsGranted,
                            onGrant = { askForContacts.launch(Manifest.permission.READ_CONTACTS) },
                        ) { contact ->
                            chosenContact = contact
                            when {
                                contact.hasChoice -> mode = Mode.PICK_NUMBER
                                // Ohne Nummer waere die Kachel eine, die nie etwas tut.
                                // Lieber gar nicht erst anlegen als still nichts belegen.
                                !contact.isCallable -> {
                                    Toast.makeText(
                                        this@TileEditorActivity,
                                        getString(R.string.contact_without_number, contact.name),
                                        Toast.LENGTH_LONG,
                                    ).show()
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
                                write(
                                    TileEdits.withAction(
                                        button,
                                        ButtonAction.Contact(
                                            name = contact.name,
                                            number = number,
                                            photoUri = contact.photoUri,
                                            mode = contactMode,
                                        ),
                                    )
                                )
                            }
                            mode = Mode.MENU
                        }

                        Mode.EDIT_LABEL -> {
                            // Ein Ordner hat genau einen Namen. Er steht auf der Kachel und
                            // als Ueberschrift im geoeffneten Ordner; ein zweiter Name nur
                            // fuer die Kachel hiesse, dass dasselbe Ding zweimal anders
                            // heisst - je nachdem, ob man davor steht oder darin.
                            val ordner = button.action as? ButtonAction.Folder
                            LabelEditor(
                                initial = if (ordner != null) {
                                    FolderEdits.folderFor(config, ordner)?.name.orEmpty()
                                } else {
                                    button.label.orEmpty()
                                },
                                automatic = describe(button.copy(label = null)) { a ->
                                    apps.labelFor(a.packageName, a.activityName)
                                },
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
                            rows = remember(shortcutApp) {
                                shortcutApp?.let {
                                    ShortcutRepository.get(this@TileEditorActivity).forPackage(it.packageName)
                                }.orEmpty()
                            },
                            onBack = { mode = Mode.PICK_SHORTCUT_APP },
                        ) { row ->
                            write(
                                TileEdits.withAction(
                                    button,
                                    ButtonAction.Shortcut(row.packageName, row.id, Shortcuts.labelOf(row)),
                                )
                            )
                            mode = Mode.MENU
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
                            onPick = { targetId ->
                                write(TileEdits.withAction(button, ButtonAction.GoToScreen(targetId)))
                                mode = Mode.MENU
                            },
                            onCreate = {
                                val id = ScreenEdits.freeId(store.current)
                                val name = getString(R.string.screen_default_name, store.current.screens.size + 1)
                                store.update { ScreenEdits.add(it, ScreenEdits.newScreen(id, name, screen)) }
                                write(TileEdits.withAction(button, ButtonAction.GoToScreen(id)))
                                mode = Mode.MENU
                            },
                        )

                        Mode.EDIT_LINK -> LinkEditor(
                            initial = (button.action as? ButtonAction.Link)?.url.orEmpty(),
                        ) { eingabe ->
                            val adresse = LinkTarget.normalise(eingabe)
                            if (adresse == null) {
                                Toast.makeText(
                                    this@TileEditorActivity,
                                    R.string.link_invalid,
                                    Toast.LENGTH_LONG,
                                ).show()
                            } else {
                                write(TileEdits.withAction(button, ButtonAction.Link(adresse)))
                                mode = Mode.MENU
                            }
                        }

                        Mode.MOVE -> MoveTargetList(
                            targets = cell?.let { TileMove.targetsFor(config, screenId, it) }.orEmpty(),
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
                            onPick = { index ->
                                write(TileEdits.withColorIndex(button, index))
                                mode = Mode.MENU
                            },
                        )
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
    appLabel: (ButtonAction.App) -> String?,
    onPickBuiltin: () -> Unit,
    onPickApp: () -> Unit,
    onPickContact: () -> Unit,
    onPickShortcut: () -> Unit,
    onPickWidget: () -> Unit,
    onPickScreen: () -> Unit,
    onPickLink: () -> Unit,
    mayAddFolder: Boolean,
    onNewFolder: () -> Unit,
    onEditLabel: () -> Unit,
    moveTargets: Int,
    onMove: () -> Unit,
    onPickColor: () -> Unit,
    onResize: (() -> Unit)?,
    onClear: () -> Unit,
    onDone: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.editor_title)) }
        item {
            BigRow(
                label = stringResource(R.string.editor_current),
                secondary = describe(button, appLabel),
                surface = palette.surfaceDefault,
                onClick = {},
            )
        }
        item { BigRow(stringResource(R.string.editor_pick_app), icon = Icons.Filled.Apps, onClick = onPickApp) }
        item { BigRow(stringResource(R.string.editor_pick_contact), icon = Icons.Filled.Person, onClick = onPickContact) }
        item { BigRow(stringResource(R.string.editor_pick_builtin), icon = Icons.Filled.Tune, onClick = onPickBuiltin) }
        item { BigRow(stringResource(R.string.editor_pick_shortcut), icon = Icons.Filled.Bolt, onClick = onPickShortcut) }
        item { BigRow(stringResource(R.string.editor_pick_widget), icon = Icons.Filled.Widgets, onClick = onPickWidget) }
        item { BigRow(stringResource(R.string.editor_pick_screen), icon = Icons.AutoMirrored.Filled.ArrowForward, onClick = onPickScreen) }
        // In einem Ordner nicht: zwei Ebenen zerstoeren den Ueberblick, den grosse Kacheln
        // herstellen sollen. Siehe PLAN.md 4.9.
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
        // Nur wenn es ueberhaupt ein Ziel gibt. Eine Zeile anzubieten, die dann sagt "geht
        // nicht", ist schlechter als sie wegzulassen.
        if (button.action != ButtonAction.None && moveTargets > 0) {
            item {
                BigRow(
                    stringResource(R.string.editor_move),
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    onClick = onMove,
                )
            }
        }
        item { BigRow(stringResource(R.string.editor_color), icon = Icons.Filled.Palette, onClick = onPickColor) }
        if (onResize != null) {
            item { BigRow(stringResource(R.string.editor_resize), icon = Icons.Filled.OpenInFull, onClick = onResize) }
        }
        item {
            BigRow(
                label = stringResource(R.string.editor_clear),
                icon = Icons.Filled.Delete,
                surface = palette.surfaceDanger,
                onClick = onClear,
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
private fun describe(button: Button, appLabel: (ButtonAction.App) -> String?): String =
    button.label ?: when (val a = button.action) {
        is ButtonAction.Action -> stringResource(a.builtin.labelRes())
        is ButtonAction.App -> appLabel(a) ?: a.packageName
        is ButtonAction.Contact -> a.name
        is ButtonAction.Shortcut -> a.label
        is ButtonAction.Widget -> a.label.ifBlank { stringResource(R.string.editor_pick_widget) }
        is ButtonAction.GoToScreen -> stringResource(R.string.next_screen)
        is ButtonAction.Folder -> stringResource(R.string.folder)
        is ButtonAction.Link -> LinkTarget.labelFor(a.url)
        ButtonAction.None -> stringResource(R.string.empty_tile)
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
        // Sobald gesucht wird, weicht die Ueberschrift: mit offener Tastatur bleiben auf
        // 581 dp sonst nur ein bis zwei Trefferzeilen uebrig, und die Ueberschrift sagt
        // an dieser Stelle nichts mehr, was das Suchfeld nicht schon zeigt.
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
                fontSize = 18.sp,
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
            textStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
            // Der automatische Name steht blass im leeren Feld. Sonst sieht man nur einen
            // leeren Kasten und weiss nicht, was man da eigentlich ersetzt.
            placeholder = {
                Text(automatic, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            },
            // Die Haken-Taste der Tastatur uebernimmt. Auf drei Zoll verdeckt die Tastatur
            // den "Fertig"-Knopf vollstaendig - wer tippt, kommt sonst nicht an ihn heran.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone(text) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(hintRes),
            color = palette.onBackground,
            fontSize = 15.sp,
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
private fun ColorPicker(selected: Int, onPick: (Int?) -> Unit) {
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(stringResource(R.string.editor_color))
        // Die Farbnamen stehen in derselben Reihenfolge wie die Farben selbst. Ohne sie
        // waeren die Felder fuer einen Screenreader sechs namenlose Schaltflaechen.
        val names = stringArrayResource(R.array.tile_colors)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            palette.tiles.forEachIndexed { index, color ->
                val chosen = index == selected
                val plain = names.getOrElse(index) { "" }
                // Der Zustand gehoert in den Namen. Die reine selected-Eigenschaft kommt in
                // der Bedienungshilfen-Schnittstelle nicht an - geprueft im Knotenabzug des
                // Geraets -, und eine Auswahl, die nur zu sehen ist, hilft beim Vorlesen nicht.
                val name = if (chosen) stringResource(R.string.a11y_chosen, plain) else plain
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color)
                        .then(
                            if (chosen) {
                                Modifier.border(4.dp, palette.onBackground, RoundedCornerShape(12.dp))
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
                    // Der Rahmen allein traegt die Auswahl nicht: auf einem farbigen Feld
                    // sieht ein Rahmen schnell nach Zierrat aus. Das Haekchen ist eindeutig.
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
            surface = if (selected < 0) palette.surfaceAccent else palette.surfaceDefault,
            onClick = { onPick(null) },
        )
    }
}

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
                fontSize = 18.sp,
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
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
        } else if (shown.isEmpty()) {
            Text(
                text = stringResource(R.string.contacts_no_match),
                color = palette.onBackground,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(shown, key = { it.id }) { contact ->
                BigRow(
                    label = contact.name,
                    secondary = contact.numbers.firstOrNull()?.number,
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
                label = number.number,
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
 * Zeigt ausschliesslich die Schritte, die gerade moeglich sind. Ausgegraute Knoepfe
 * muesste der Nutzer erst antippen, um zu erfahren, dass sie nichts tun - auf drei
 * Zoll ist das verschwendeter Platz.
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
                onClick = {},
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
        // Ohne diesen Satz stuenden hier nur "Aktuell 1 x 1" und "Fertig" - das sieht aus,
        // als waere die Seite kaputt, dabei ist rundherum schlicht kein Platz frei.
        if (grow.isEmpty() && shrink.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.resize_no_room),
                    color = palette.onBackground,
                    fontSize = 16.sp,
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
    val others = config.screens.filter { it.id != currentScreenId }

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

/** Verknuepfungen gibt Android nur an den Standard-Launcher heraus - das muss dastehen. */
@Composable
private fun NeedsHomeRole(onChoose: () -> Unit) {
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(stringResource(R.string.editor_pick_shortcut))
        Text(
            text = stringResource(R.string.shortcut_needs_home_role),
            color = palette.onBackground,
            fontSize = 16.sp,
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
    rows: List<ShortcutRow>,
    onBack: () -> Unit,
    onPick: (ShortcutRow) -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(app?.label ?: stringResource(R.string.editor_pick_shortcut)) }
        if (rows.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.shortcut_none),
                    color = palette.onBackground,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
            // Die meisten Apps bieten keine Verknuepfungen an, und welche das sind, sieht man
            // der Liste nicht an. Ohne diesen Knopf steht man vor einem Satz und muesste die
            // Zurueck-Geste kennen, um eine andere App zu probieren.
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
 * Widget-Auswahl. Neben dem Namen steht, wie viele Felder das Widget braucht - sonst legt
 * der Nutzer ein zu grosses auf eine kleine Kachel und bekommt ein gestauchtes Ergebnis.
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
        if (rows.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.widget_none),
                    color = palette.onBackground,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
        items(rows, key = { it.component }) { row ->
            val (needX, needY) = WidgetFit.requirement(row, cellWidthDp, cellHeightDp, gutterDp)
            BigRow(
                label = row.label,
                secondary = stringResource(R.string.widget_needs, row.appLabel, needX, needY),
                icon = Icons.Filled.Widgets,
                onClick = { onPick(row) },
            )
        }
    }
}

/**
 * Rückfrage vor dem Löschen eines Ordners.
 *
 * Ein Ordner wegzuwerfen wirft alles mit weg, was darin liegt - und anders als bei einer
 * einzelnen Kachel sieht man das nicht, weil der Inhalt zugeklappt ist. Deshalb steht die
 * Zahl hier, und deshalb heißen die Knöpfe nach ihrer Handlung und nicht „Ja"/„Nein".
 */
@Composable
private fun FolderDeletePanel(
    name: String,
    count: Int,
    onKeep: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(stringResource(R.string.folder_delete_title, name))
        Text(
            text = pluralStringResource(R.plurals.folder_delete_body, count, count),
            color = palette.onBackground,
            fontSize = 17.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        BigRow(
            label = stringResource(R.string.folder_delete_confirm),
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
 * Wohin die Kachel soll.
 *
 * Angeboten wird nur, wo tatsächlich Platz ist - ein Ziel, das dann ablehnt, wäre ein
 * Knopf, der nichts tut. Neben dem Namen steht, ob es ein Ordner ist und wie viel dort
 * noch frei ist.
 */
@Composable
private fun MoveTargetList(targets: List<Screen>, onPick: (Screen) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.editor_move)) }
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

/**
 * Die Adresse einer Webseiten-Kachel eingeben.
 *
 * Ohne „https://" davor - das ergänzt [LinkTarget], weil es auf drei Zoll niemand tippt.
 * Die Haken-Taste der Tastatur übernimmt, denn mit offener Tastatur ist „Fertig" verdeckt.
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
            textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
            placeholder = { Text(stringResource(R.string.link_example), fontSize = 18.sp) },
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
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        BigRow(
            label = stringResource(R.string.editor_done),
            surface = palette.surfaceAccent,
            onClick = { onDone(text) },
        )
    }
}
