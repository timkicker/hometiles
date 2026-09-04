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
 * Belegt eine einzelne Kachel. Schreibt direkt in den ConfigStore - der Homescreen
 * beobachtet denselben Fluss und zeichnet sich neu, sobald hier etwas passiert.
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
            // Wohin die naechste Wahl geht. PLAN.md 4.3 sagt zu, dass *jede* Aktion auch
            // auf Langdruck liegen darf; vorher gab es dafuer zwei eigene Auswahllisten
            // (App und Funktion), und Kontakte, Verknuepfungen, Screens und Webseiten
            // fehlten. Dieselben Listen fuer beide Wege statt acht weiterer Betriebsarten.
            var aufLangdruck by remember { mutableStateOf(false) }
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

            // Eine Kachel neu zu belegen, auf der ein Ordner liegt, liess den Ordner samt
            // Inhalt zurueck: kein Weg fuehrte mehr hin, in der Screen-Liste steht er nicht
            // ("Ordner gehoeren ihrer Kachel"), und geloescht werden konnte er auch nicht
            // mehr. "Kachel leeren" fragte laengst nach - jeder andere Weg auf dieselbe
            // Kachel nicht.
            var replacingFolder by remember { mutableStateOf<Pair<ButtonAction.Folder, Button>?>(null) }

            fun writeNow(next: Button) {
                if (next.action !is ButtonAction.Widget) releaseWidgetIfAny(button)
                // **Der Ordner entsteht hier, nicht vorher.**
                //
                // Bis zum 04.09.2026 legte `onNewFolder` den Ordner-Screen an, *bevor* die
                // Rueckfrage kam. Wer auf einer Ordnerkachel "Ordner anlegen" waehlte und
                // die Frage ("Mehr loeschen?") mit "Behalten" beantwortete, liess einen
                // leeren Ordner zurueck, den niemand mehr oeffnen kann. Am Geraet erzeugt
                // und in `config.json` gesehen: `folder4`, null Kacheln, kein Weg hin.
                //
                // Jetzt haengt das Anlegen an derselben Bedingung wie das Schreiben: wird
                // nicht geschrieben, entsteht auch nichts.
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

            /** Legt die gewaehlte Aktion auf den Kurz- oder den Langdruck - je nachdem, woher der Weg kam. */
            fun belege(action: ButtonAction) {
                if (aufLangdruck) {
                    write(TileEdits.withLongPress(button, action))
                } else {
                    write(TileEdits.withAction(button, action))
                }
                mode = Mode.MENU
            }

            // Zurueck im Menue gilt wieder der Kurzdruck - auch nach der Zurueck-Taste,
            // sonst legte die naechste Wahl stillschweigend wieder auf den Langdruck.
            LaunchedEffect(mode) { if (mode == Mode.MENU) aufLangdruck = false }

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
                        // `fortsetzungen` als Schluessel: dieser Bildschirm schickt den Nutzer bei
            // dauerhaft verweigerter Berechtigung in die **App-Einstellungen**, und von dort
            // kommt kein Ergebnis zurueck. Ohne das Neulesen beim Wiederkommen stuende hier
            // weiter „keine Berechtigung" - auf einem Bildschirm, der einen selbst dorthin
            // geschickt hat. Siehe `BigLauActivity.fortsetzungen`.
var contactsGranted by remember(fortsetzungen.intValue) { mutableStateOf(contacts.hasPermission()) }
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
                // Die Zurueck-Taste tut, was der Knopf daneben tut.
                //
                // Die beiden Loeschfragen sind Vollbild-Tafeln: sie biegen mit `return@Box`
                // ab, bevor der Modus ueberhaupt drankommt, und sie werden aus dem Menue
                // heraus gesetzt. `mode != Mode.MENU` war dort also false, und ein Druck auf
                // Zurueck beendete den ganzen Editor statt die Frage. Am 04.09.2026 am
                // Emulator nachgemessen: aus der Frage nach dem Ordner heraus stand man
                // wieder auf dem Startbildschirm. Zerstoert wurde nichts - aber wer mit
                // Tasten arbeitet, verliert damit seinen Platz, ohne dass etwas es ansagt.
                //
                // Die Reihenfolge ist die, in der die Tafeln uebereinanderliegen: von oben
                // nach unten wieder weg. Die Sperre bleibt aussen vor; dort fuehrt die Taste
                // hinaus und darf nicht hineinfuehren.
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
                                // Erst den Ordner weg, dann die neue Belegung schreiben.
                                // FolderEdits.delete raeumt auch diese Kachel ab; setButton
                                // legt sie danach neu an.
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

                    // Ein Band statt sechs zusaetzlicher Ueberschriften: dieselben Listen
                    // belegen jetzt beide Druckarten. Ohne den Hinweis waere "App waehlen"
                    // auf dem Langdruckweg nicht von der Hauptbelegung zu unterscheiden -
                    // und wer sich vertut, ueberschreibt, was die Kachel bisher tat.
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
                                // Nur die Kennung wird hier gewaehlt; den Ordner legt
                                // `writeNow` an, wenn die Kachel wirklich geschrieben wird.
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
                                    // Erst fragen. Die Kachel zu leeren wuerde den Ordner
                                    // zurueckliegen lassen, ohne dass noch ein Weg hinfuehrt -
                                    // samt allem, was darin liegt.
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
                                // Ohne Nummer waere die Kachel eine, die nie etwas tut.
                                // Lieber gar nicht erst anlegen als still nichts belegen.
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

                        // Erst die Art, dann die Sache. Eine Liste, die Apps und Funktionen
                        // vermischt, waere auf diesem Schirm zu lang zum Durchsehen.
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
                                    // Der Anker wandert mit, sonst bearbeitete der Editor
                                    // danach den leeren Platz, von dem die Kachel kam.
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
        // **Welche** Kachel, nicht nur "eine Kachel".
        //
        // Bis zum 04.09.2026 stand hier nur die Ueberschrift. Wer eine von zwei leeren
        // Kacheln antippte, sah nirgends, welche er erwischt hat - und der Fehler vom
        // Vormittag (der Editor ging auf dem Startbildschirm statt im Ordner auf) waere
        // sofort dagestanden, wenn der Name des Screens hier gestanden haette.
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
        // In einem Ordner nicht: zwei Ebenen zerstoeren den Ueberblick, den grosse Kacheln
        // herstellen sollen. Siehe PLAN.md 4.9.
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
        // Nur wo Blinken ueberhaupt etwas bedeutet: eine Uhr und eine leere Kachel haben
        // keine Benachrichtigungen, und ein Schalter dafuer waere eine Zusage ohne Deckung.
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
        // Zweitbelegung: PLAN.md 4.3 sagt sie zu. Nur wo die Kachel ueberhaupt etwas tut -
        // eine leere Kachel mit Zweitbelegung waere ein Raetsel.
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
                // Eine Zweitbelegung geht dem Editor vor (siehe LongPress.decide) - fuer
                // **diese** Kachel fuehrt der Langdruck also nicht mehr hierher. Dasselbe
                // sagt `a11y_editor_moved`, wenn eine Einstellung den Langdruck nimmt; hier
                // nimmt ihn die Kachel selbst, und bis zum 04.09.2026 sagte es niemand.
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
                // Sind die Symbole global aus, waere die Wahl sonst eine Einstellung ohne
                // sichtbare Wirkung - der Nutzer waehlt und nichts passiert. Die Zeile
                // bleibt trotzdem: die Wahl gilt, sobald die Symbole wieder an sind.
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
        // Nur, wenn es etwas zu leeren gibt - siehe TileEdits.clearable. Bei einer frischen
        // Kachel stand hier ein roter Knopf ohne Wirkung.
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
 * Dieselbe Ableitung wie auf dem Startbildschirm - siehe [TileLabel]. Der Editor hatte
 * frueher seine eigene und nannte einen Ordner nur "Ordner", wo der Startbildschirm den
 * Namen zeigte.
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
 * Was das Halten tun soll.
 *
 * `PLAN.md` 4.3 sagt zu: "Jede Aktion zusaetzlich auf Langdruck belegbar, unabhaengig vom
 * Kurzdruck." Zur Wahl standen aber nur Apps und eingebaute Funktionen - Kontakte,
 * Verknuepfungen, Screens und Webseiten fehlten, obwohl das Modell sie laengst tragen kann.
 * Kein Widget und kein Ordner: beide sind kein Griff, sondern der Inhalt einer Zelle.
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
            // Der automatische Name steht blass im leeren Feld. Sonst sieht man nur einen
            // leeren Kasten und weiss nicht, was man da eigentlich ersetzt.
            placeholder = {
                Text(automatic, fontSize = bigSp(22f), fontWeight = FontWeight.Bold)
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
            selected = selected < 0 && hue == null,
            onClick = { onPick(null) },
        )

        // PLAN.md 4.2 nennt drei Arten: Auto, Palette, frei. Die freie steht hier, und sie
        // fragt nur nach dem Farbton - die Helligkeit dazu rechnet FreeTileColor so aus,
        // dass beide Schwellen aus 3.3 halten. Ein Farbwaehler, der zu blasse Farben
        // annimmt, waere schlimmer als keiner.
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
 * Die Symbolauswahl. `PLAN.md` 2.2 und 3.4.
 *
 * Vier nebeneinander, in Gruppen mit Ueberschrift - dieselbe Aufteilung wie bei den Farben.
 * Ganz oben "Automatisch", denn das ist der Zustand, in dem jede Kachel anfaengt, und der
 * Weg zurueck muss so gross sein wie der Weg hin.
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
            // Drei nebeneinander, nicht vier: bei vier bricht das Wort mitten im Wort
            // ("Nachrich/t"), und ein zerbrochenes Wort ist schlechter als eine Zeile mehr.
            // Am Bildschirm nachgesehen.
            items(gruppe.names.chunked(3)) { reihe ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    reihe.forEach { name ->
                        val bild = IconCatalogue.vectorFor(name)
                        val gewaehlt = name == selected
                        val flaeche =
                            if (gewaehlt) palette.surfaceAccent else palette.surfaceDefault
                        val wort = IconCatalogue.labelFor(name)?.let { stringResource(it) } ?: name
                        // Der Zustand gehoert in den Namen - siehe die Messung im
                        // Farbwaehler daneben.
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
                                    // Das Wort steht darunter; eine zweite Ansage waere
                                    // dieselbe Auskunft zweimal.
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
                    // Die letzte Reihe fuellt sich auf, sonst waeren ihre Felder breiter
                    // als die darueber.
                    repeat(3 - reihe.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * Die freie Farbwahl: vierundzwanzig Farbtöne, jeder in der Fassung, die in diesem Thema
 * lesbar ist.
 *
 * Gewählt wird der Ton, nicht die Helligkeit — die hängt am Thema und an zwei Schwellen,
 * und das ist nichts, wonach man jemanden mit einem Schieberegler fragt.
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
                        // Der Zustand gehoert in den Namen - dieselbe Ueberlegung wie bei
                        // den sechs Palettenfeldern darueber.
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

/**
 * Ein Name für den Farbton, damit die Felder für einen Screenreader nicht
 * vierundzwanzig namenlose Schaltflächen sind.
 */
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
    // **Ordner sind keine Sprungziele.** Ein Ordner gehoert seiner Kachel und legt sich als
    // Ueberlagerung darueber; als Sprungziel wuerde er zum gewoehnlichen Screen - ohne die
    // Zeile "Ordner schliessen", ohne Eintrag in der Screen-Liste, und daneben stuende
    // weiter die Kachel, die ihn als Ueberlagerung oeffnet. Zwei Wege zu derselben Sache,
    // die verschieden aussehen.
    //
    // Am 04.09.2026 am Emulator erzeugt: eine Sprungkachel auf einen Ordner, angetippt, und
    // der Ordner stand als Screen da. `SwipeChain` filtert Ordner seit jeher heraus; diese
    // Liste war die einzige, die es nicht tat.
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

/** Verknuepfungen gibt Android nur an den Standard-Launcher heraus - das muss dastehen. */
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
            // Zwei verschiedene Saetze fuer zwei verschiedene Zustaende: die App hat keine,
            // oder wir konnten nicht nachsehen. Der Ausweg darunter ist derselbe.
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
        // Der Hinweis steht **vor** der Wahl, nicht danach. Am 04.09.2026 am Emulator
        // nachgestellt: Widget auf eine Kachel gelegt, langer Druck darauf - und die
        // Weckerapp ging auf. Das Widget bekommt die Beruehrung zuerst, und damit ist der
        // uebliche Weg zum Editor fuer diese eine Kachel zu. Es gibt einen anderen
        // (Einstellungen, "Kacheln aendern"), aber wer ihn nicht kennt, haelt die Kachel
        // fuer festgewachsen.
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
    /** Wird die Kachel neu belegt statt geleert? Dann heisst der Knopf anders. */
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
 * Wohin die Kachel soll.
 *
 * Angeboten wird nur, wo tatsächlich Platz ist - ein Ziel, das dann ablehnt, wäre ein
 * Knopf, der nichts tut. Neben dem Namen steht, ob es ein Ordner ist und wie viel dort
 * noch frei ist.
 */
@Composable
private fun MoveTargetList(
    /** Welche Kachel hier verschoben wird, und wo sie gerade liegt. */
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
        // Welche Kachel eigentlich? Jedes Ziel unten heisst "Zeile x, Spalte y" - ohne
        // diese Zeile ist der ganze Bildschirm eine Liste abstrakter Plaetze, und wer
        // zwischendurch weggeschaut hat, weiss nicht mehr, was er da bewegt.
        item {
            Text(
                text = welche,
                color = palette.onBackground,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        // Der eigene Bildschirm zuerst: wer eine Kachel verschiebt, ordnet meistens den
        // Bildschirm um, auf dem er gerade steht. Zeile und Spalte werden ab 1 gezaehlt -
        // "Zeile 0" liest sich wie ein Fehler.
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
            // Eine grosse Kachel kommt woanders einfeldrig an ([TileMove.move]) - sonst
            // ragte sie ueber den Rand. Das steht hier, bevor es passiert; hinterher
            // sieht es aus, als haette das Verschieben die Kachel kaputtgemacht.
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
 * Die Nummer eintippen, an die diese Kachel eine Nachricht beginnt.
 *
 * Die Zifferntastatur statt der vollen: der Empfänger ist eine Nummer, kein Name. Die
 * Haken-Taste übernimmt, denn mit offener Tastatur ist „Fertig" verdeckt - derselbe Fall
 * wie beim Webseiten-Feld darunter. Was hier steht, wird nicht verschickt; die Kachel
 * öffnet später den Schreiben-Bildschirm, siehe [MessageTile].
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
