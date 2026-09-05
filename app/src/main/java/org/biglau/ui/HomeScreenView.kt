package org.biglau.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import org.biglau.tiles.FocusOrder
import org.biglau.tiles.PadDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.biglau.R
import org.biglau.data.Appearance
import org.biglau.data.Background
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import androidx.compose.material.icons.Icons
import org.biglau.web.LinkTarget
import androidx.compose.material.icons.filled.Public
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Folder
import org.biglau.data.Cell
import org.biglau.tiles.FolderEdits
import org.biglau.data.IconVisibility
import org.biglau.data.LabelPosition
import android.text.format.DateFormat
import androidx.compose.ui.platform.LocalContext
import org.biglau.data.Builtin
import org.biglau.data.Screen
import org.biglau.a11y.TileSpeech
import org.biglau.info.BatteryInfo
import org.biglau.info.BatteryReading
import org.biglau.info.SignalInfo
import org.biglau.info.SignalReading
import org.biglau.notify.SystemPackages
import org.biglau.notify.TileNotifications
import org.biglau.tiles.TileEdits
import org.biglau.ui.theme.LocalTextScale
import org.biglau.ui.theme.LocalLabelScale
import org.biglau.ui.theme.FreeTileColor
import org.biglau.ui.theme.toArgbLong
import org.biglau.ui.theme.LocalBigPalette

/**
 * Zeichnet einen Screen als Raster. Zellen koennen mehrere Rasterplaetze ueberspannen;
 * freie Plaetze bekommen eine gedaempfte Platzhalterkachel, die den Editor oeffnet.
 */
@Composable
fun HomeScreenView(
    screen: Screen,
    appearance: Appearance,
    modifier: Modifier = Modifier,
    appIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    appLabel: (String, String) -> String? = { _, _ -> null },
    shortcutIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    /** Der Ordner zu einer Kennung - fuer die Vorschau auf der Ordnerkachel. */
    folderOf: (String) -> Screen? = { null },
    notificationCounts: Map<String, Int> = emptyMap(),
    /** Ungesehene verpasste Anrufe - die Kachel dafuer zaehlt die Anrufliste, nicht Meldungen. */
    missedCalls: Int = 0,
    /** Ungelesene Nachrichten, oder null, wenn BigLau sie nicht lesen darf. */
    unreadMessages: Int? = null,
    battery: BatteryReading? = null,
    signal: SignalReading? = null,
    systemPackages: SystemPackages = SystemPackages(),
    onActivate: (Cell) -> Unit = {},
    /** Im Bearbeitungsmodus gehoert jede Beruehrung der App, auch auf einem Widget. */
    editMode: Boolean = false,
    onEdit: (x: Int, y: Int) -> Unit = { _, _ -> },
    /**
     * Die linke Softkey-Taste. PLAN.md 10.3.3.
     *
     * Sie traegt, was am langen Druck haengt. Eigener Weg und nicht [onEdit], weil die
     * Einstellung "erst bei langem Druck ausloesen" fuer eine Taste nicht gilt: sie schuetzt
     * vor dem Streifen mit dem Finger, und eine Taste wird nicht gestreift.
     */
    onMenu: (x: Int, y: Int) -> Unit = { _, _ -> },
    /**
     * Liegt dieser Rahmen gerade obenauf?
     *
     * Der Startbildschirm und ein offener Ordner benutzen denselben Rahmen und sind
     * gleichzeitig komponiert; der Ordner liegt im selben Fenster darueber. Wer from beiden
     * die Tasten bekommen soll, kann der Rahmen nicht selbst wissen.
     */
    active: Boolean = true,
    /**
     * Der Streifen unter dem Raster, falls es einen gibt.
     *
     * In einem Ordner und in der Liste der Menuetaste steht unter dem Raster eine Zeile, die
     * es schliesst. Sie ist ein Geschwister des Rahmens, nicht sein Kind, und der Rahmen
     * verbraucht jede Richtungstaste - also kam der Fokus nie zu ihr. Am 04.09.2026 from
     * `tools/unerreichbar.py` gemeldet: neun anklickbare Flaechen im Ordner, acht erreicht,
     * und die neunte war der Streifen.
     *
     * Ein blindes `moveFocus(Down)` waere hier falsch. Es nimmt den naechsten fokussierbaren
     * Knoten, und der kann eine Kachel des Startbildschirms unter der Ueberlagerung sein.
     * Deshalb ein benannter Anker: der Rahmen weiss, wohin, oder er tut nichts.
     */
    below: FocusRequester? = null,
    /**
     * Der Weg zurueck ins Raster, fuer den Streifen darunter.
     *
     * Er haengt immer an der Zelle, auf der der Fokus zuletzt sass, damit man dort
     * herauskommt, wo man hineingegangen ist. Ein blindes `moveFocus(Up)` waere auch hier
     * falsch: unter der Ueberlagerung liegen die Kacheln des Startbildschirms an denselben
     * Stellen.
     */
    gridAnchor: FocusRequester? = null,
) {
    val palette = LocalBigPalette.current
    val gutter = appearance.gutterDp.dp
    val radius = appearance.cornerRadiusDp.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                // Erschoepfend: ein else-Zweig hatte hier jahrelang den Bild-Fall
                // verschluckt, und niemand sah, dass er nie gemalt wurde.
                when (val bg = screen.background) {
                    is Background.Solid -> Color(bg.argb.toInt())
                    Background.Theme -> palette.background
                }
            ),
    ) {
        val metrics = gridMetrics(
            availableWidth = maxWidth.value,
            availableHeight = maxHeight.value,
            cols = screen.cols,
            rows = screen.rows,
            gutter = gutter.value,
            borderPercent = appearance.safeBorderPercent,
        )
        val border = metrics.border.dp
        val cellW = metrics.cellWidth.dp
        val cellH = metrics.cellHeight.dp

        // PLAN.md 10.3.2: die Reihenfolge folgt dem Raster, nicht der Liste in der Datei.
        // Die Tasten faengt der Rahmen ab und nicht die einzelne Kachel: nur hier ist
        // bekannt, welche Zelle den Fokus hat und aus welcher Spalte er kam. Und nur wer
        // die Taste verbraucht, kann am Rand nichts passieren lassen; sonst sucht Compose
        // sich selbst ein Ziel, zur Not in der Kopfzeile.
        // Die leeren Plaetze gehoeren dazu: sie sind anklickbar, also muessen sie
        // erreichbar sein.
        val targets = remember(screen) { FocusOrder.targets(screen) }
        val anchors = remember(targets) { targets.associateWith { FocusRequester() } }
        var focusedCell by remember(screen.id) { mutableStateOf(FocusOrder.first(targets)) }
        var rememberedColumn by remember(screen.id) { mutableStateOf<Int?>(null) }

        // PLAN.md 10.3.1: **beim Start** ist der Fokus from selbst da, aber nicht, wenn
        // dieser Rahmen spaeter obenauf kommt.
        //
        // Beim Start stimmt es ohne Zutun: das Fenster bekommt den Fokus, Compose sucht sich
        // das erste Ziel. Ein Versuch, das selbst zu setzen, hat lange nichts getan, und am
        // 04.09.2026 kam heraus, warum: Android hat einen Beruehrungsmodus. Solange die
        // letzte Eingabe ein Tipp war, nimmt kein Element den Fokus, und `requestFocus` wird
        // still ignoriert. Mit `dumpsys window` nachgestellt: nach einem Tipp
        // `mInTouchMode=true` und kein Fokus, nach einer Taste `mInTouchMode=false` und der
        // Fokus sitzt sofort auf der Kachel oben links.
        //
        // Der Ordner ist der andere Fall, und er war kaputt. Am 04.09.2026 gefunden, vom
        // ersten Lauf from `tools/unerreichbar.py` ueber einen echten Bildschirm: neun
        // anklickbare Flaechen, **null** davon je fokussiert. Von Hand bestaetigt - Kachel
        // mit der Auswahltaste geoeffnet, also `mInTouchMode=false`, davor sass der Fokus
        // auf der Ordnerkachel, danach auf **gar nichts**, und vier Tastendruecke aenderten
        // daran nichts.
        //
        // Und sie konnten es nicht: `onPreviewKeyEvent` laeuft nur den Weg vom fokussierten
        // Knoten zur Wurzel. Ohne Fokus laeuft der Rahmen gar nicht erst an, also bewegt
        // sich nichts, also kommt der Fokus nie zurueck. Der Ordner war mit Tasten
        // ueberhaupt nicht zu bedienen, und ein Bildschirm ohne Fokus ist an einem
        // Tastentelefon dasselbe wie ein eingefrorener.
        //
        // Deshalb holt der Rahmen ihn sich, sobald er obenauf kommt. Im Beruehrungsmodus
        // wird das weiter still verworfen, und das ist richtig: wer tippt, will keinen
        // Rahmen um eine Kachel.
        LaunchedEffect(active, targets) {
            if (active) {
                val target = focusedCell ?: FocusOrder.first(targets)
                // `requestFocus` wirft, solange der Knoten noch nicht haengt. Das ist kein
                // Fehler, sondern eine Reihenfolge: dann sitzt der Fokus ohnehin schon da,
                // wo Compose ihn beim Aufbau hingelegt hat.
                runCatching { target?.let { anchors[it]?.requestFocus() } }
            }
        }

        // PLAN.md 10.3.3: eine Ziffer waehlt den Platz mit dieser Nummer und loest ihn aus.
        //
        // Ausloesen und nicht nur hinspringen: wer die Vier drueckt, will die Apotheke, nicht
        // den Fokus auf der Apotheke. Ein zweiter Druck waere ein Umweg, den man sich merken
        // muesste.
        //
        // Ziffern duerfen das nur, wo sie sonst nichts bedeuten. Hier ist das from selbst so:
        // auf dem Startbildschirm und in einem Ordner gibt es kein Eingabefeld. Die
        // Waehltastatur und der Nachrichtentext liegen in eigenen Bildschirmen, die diesen
        // Rahmen nicht benutzen.
        fun select(digit: Int): Boolean {
            val target = FocusOrder.numbered(targets, digit) ?: return true
            if (target in screen.cells) {
                onActivate(target)
            } else {
                // Ein leerer Platz hat nichts zu starten; er fuehrt dorthin, wo man ihn
                // fuellt, genau wie ein Tipp darauf.
                onEdit(target.x, target.y)
            }
            return true
        }

        fun move(direction: PadDirection): Boolean {
            val from = focusedCell ?: FocusOrder.first(targets) ?: return true
            val target = FocusOrder.neighbour(targets, from, direction, rememberedColumn)
            if (target == null && direction == PadDirection.DOWN && below != null) {
                // Unter der letzten Zeile steht der Streifen, der die Ueberlagerung
                // schliesst. Nur nach below, und nur wenn es ihn gibt.
                runCatching { below.requestFocus() }
                return true
            }
            if (target != null) {
                // Waagerecht setzt die gemerkte Spalte neu, senkrecht laesst sie stehen.
                if (direction == PadDirection.LEFT || direction == PadDirection.RIGHT) {
                    rememberedColumn = target.x
                }
                anchors[target]?.requestFocus()
            }
            return true
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(border)
                .onPreviewKeyEvent { key ->
                    if (key.type != KeyEventType.KeyDown) {
                        false
                    } else {
                        when (key.key) {
                            Key.DirectionLeft -> move(PadDirection.LEFT)
                            Key.DirectionRight -> move(PadDirection.RIGHT)
                            Key.DirectionUp -> move(PadDirection.UP)
                            Key.DirectionDown -> move(PadDirection.DOWN)
                            Key.One -> select(1)
                            Key.Two -> select(2)
                            Key.Three -> select(3)
                            Key.Four -> select(4)
                            Key.Five -> select(5)
                            Key.Six -> select(6)
                            Key.Seven -> select(7)
                            Key.Eight -> select(8)
                            Key.Nine -> select(9)
                            // Die linke Softkey-Taste. Die rechte ist die Zurueck-Taste, die
                            // Android schon selbst an die Activity gibt.
                            Key.Menu -> {
                                focusedCell?.let { onMenu(it.x, it.y) }
                                true
                            }
                            else -> false
                        }
                    }
                },
        ) {
            screen.freeSlots().forEach { (x, y) ->
                EmptyTile(
                    appearance = appearance,
                    cellWidth = cellW,
                    cellHeight = cellH,
                    column = x,
                    row = y,
                    modifier = Modifier
                        .offset(
                            x = metrics.offsetX(x, gutter.value).dp,
                            y = metrics.offsetY(y, gutter.value).dp,
                        )
                        .size(cellW, cellH)
                        .then(
                            anchors[Cell(x = x, y = y)]?.let { Modifier.focusRequester(it) }
                            ?.then(
                                if (gridAnchor != null && focusedCell == Cell(x = x, y = y)) {
                                    Modifier.focusRequester(gridAnchor)
                                } else {
                                    Modifier
                                },
                            )
                                ?: Modifier,
                        )
                        .onFocusChanged {
                            if (it.isFocused) focusedCell = Cell(x = x, y = y)
                        },
                    onEdit = { onEdit(x, y) },
                )
            }

            screen.cells.forEach { cell ->
                val w = metrics.spanWidth(cell.w, gutter.value).dp
                val h = metrics.spanHeight(cell.h, gutter.value).dp
                TileFor(
                    cell = cell,
                    cols = screen.cols,
                    appearance = appearance,
                    cellHeight = h,
                    cellWidth = w,
                    notificationCounts = notificationCounts,
                    missedCalls = missedCalls,
                    unreadMessages = unreadMessages,
                    systemPackages = systemPackages,
                    battery = battery,
                    signal = signal,
                    appIcon = appIcon,
                    appLabel = appLabel,
                    shortcutIcon = shortcutIcon,
                    folderOf = folderOf,
                    modifier = Modifier
                        .offset(
                            x = metrics.offsetX(cell.x, gutter.value).dp,
                            y = metrics.offsetY(cell.y, gutter.value).dp,
                        )
                        .size(w, h)
                        .then(anchors[cell]?.let { Modifier.focusRequester(it) } ?: Modifier)
                        .then(
                            if (gridAnchor != null && focusedCell == cell) {
                                Modifier.focusRequester(gridAnchor)
                            } else {
                                Modifier
                            },
                        )
                        .onFocusChanged {
                            if (it.isFocused) focusedCell = cell
                        },
                    onClick = { onActivate(cell) },
                    editMode = editMode,
                    onLongClick = { onEdit(cell.x, cell.y) },
                )
            }
        }
    }
}

@Composable
private fun TileFor(
    cell: Cell,
    cols: Int,
    appearance: Appearance,
    cellHeight: androidx.compose.ui.unit.Dp,
    cellWidth: androidx.compose.ui.unit.Dp,
    notificationCounts: Map<String, Int>,
    missedCalls: Int,
    unreadMessages: Int?,
    systemPackages: SystemPackages,
    battery: BatteryReading?,
    signal: SignalReading?,
    appIcon: (String, String) -> ImageBitmap?,
    appLabel: (String, String) -> String?,
    shortcutIcon: (String, String) -> ImageBitmap?,
    folderOf: (String) -> Screen?,
    modifier: Modifier,
    editMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val palette = LocalBigPalette.current
    val button = cell.button
    val color = tileColor(button, cell.x, cell.y, cols)
    val badge = TileNotifications.badgeFor(
        button,
        notificationCounts,
        systemPackages,
        missedCalls,
        unreadMessages,
    )

    when (val action = button.action) {
        is ButtonAction.Action -> BigTile(
            label = button.label ?: stringResource(action.builtin.labelRes()),
            // Balken und Ladebalken sind gezeichnet; ohne diesen Zusatz hoerte ein
            // Screenreader nur "Empfang" und nicht, wie gut er ist. Siehe TileSpeech.
            contentDescription = TileSpeech.describe(
                label = button.label ?: stringResource(action.builtin.labelRes()),
                state = when (action.builtin) {
                    Builtin.SIGNAL -> signalSpeech(signal)
                    Builtin.BATTERY -> batterySpeech(battery)
                    else -> null
                },
            ),
            background = color,
            cellHeight = cellHeight,
            cellWidth = cellWidth,
            badgeCount = badge,
            badgeSpeech = when (action.builtin) {
                Builtin.MISSED_CALLS -> R.plurals.a11y_new_calls
                Builtin.MESSAGES -> R.plurals.a11y_unread
                else -> R.plurals.a11y_badge
            },
            content = when (action.builtin) {
                Builtin.CLOCK -> {
                    {
                        ClockContent(
                            cellWidth = cellWidth,
                            cellHeight = cellHeight,
                            clock = appearance.clock,
                            twentyFourHour = DateFormat.is24HourFormat(LocalContext.current),
                        )
                    }
                }
                Builtin.BATTERY -> {
                    { BatteryContent(battery, cellWidth, cellHeight) }
                }
                Builtin.SIGNAL -> {
                    { SignalContent(signal, cellWidth, cellHeight) }
                }
                else -> null
            },
            // Hier faellt nur die harte Entscheidung "gar keine Symbole". Ob eines auf
            // diese eine Kachel passt, weiss erst BigTile - dort stehen die Zellmasse.
            // Selbst gewaehltes Symbol schlaegt das abgeleitete. PLAN.md 2.2.
            icon = if (appearance.icons != IconVisibility.NEVER) {
                IconCatalogue.vectorFor(button.iconName) ?: action.builtin.icon()
            } else {
                null
            },
            labelPosition = appearance.labelPosition,
            cornerRadius = appearance.cornerRadiusDp.dp,
            modifier = modifier,
            onClick = onClick,
            onLongClick = onLongClick,
        )

        is ButtonAction.App -> BigTile(
            label = button.label ?: appLabel(action.packageName, action.activityName) ?: action.packageName,
            background = color,
            cellHeight = cellHeight,
            cellWidth = cellWidth,
            // Ein selbst gewaehltes Symbol schlaegt auch das App-Bild - wer eines waehlt,
            // hat sich das ueberlegt.
            icon = if (appearance.icons != IconVisibility.NEVER) {
                IconCatalogue.vectorFor(button.iconName)
            } else {
                null
            },
            iconBitmap = if (appearance.icons != IconVisibility.NEVER && button.iconName == null) {
                appIcon(action.packageName, action.activityName)
            } else {
                null
            },
            badgeCount = badge,
            labelPosition = appearance.labelPosition,
            cornerRadius = appearance.cornerRadiusDp.dp,
            modifier = modifier,
            onClick = onClick,
            onLongClick = onLongClick,
        )

        is ButtonAction.Contact -> BigTile(
            label = button.label ?: action.name,
            background = color,
            cellHeight = cellHeight,
            cellWidth = cellWidth,
            photoUri = action.photoUri,
            // PLAN.md 3.4: ohne Foto die Initialen. Vorher stand auf jeder Kontaktkachel
            // dasselbe Personensymbol - drei Kontakte nebeneinander sahen gleich aus, und
            // das Symbol sagte nichts, was die Beschriftung nicht schon sagte.
            initials = if (action.photoUri == null && button.iconName == null) {
                tileInitials(button.label ?: action.name)
            } else {
                null
            },
            icon = if (appearance.icons != IconVisibility.NEVER) IconCatalogue.vectorFor(button.iconName) else null,
            labelPosition = appearance.labelPosition,
            cornerRadius = appearance.cornerRadiusDp.dp,
            modifier = modifier,
            onClick = onClick,
            onLongClick = onLongClick,
        )

        is ButtonAction.Widget -> WidgetTile(
            widgetId = action.widgetId,
            cellWidth = cellWidth,
            cellHeight = cellHeight,
            cornerRadius = appearance.cornerRadiusDp.dp,
            modifier = modifier,
            editMode = editMode,
            onEdit = onLongClick,
        )

        is ButtonAction.Shortcut -> BigTile(
            label = button.label ?: action.label,
            background = color,
            cellHeight = cellHeight,
            cellWidth = cellWidth,
            badgeCount = badge,
            iconBitmap = if (appearance.icons != IconVisibility.NEVER) shortcutIcon(action.packageName, action.shortcutId) else null,
            labelPosition = appearance.labelPosition,
            cornerRadius = appearance.cornerRadiusDp.dp,
            modifier = modifier,
            onClick = onClick,
            onLongClick = onLongClick,
        )

        is ButtonAction.GoToScreen -> BigTile(
            label = button.label ?: stringResource(R.string.next_screen),
            background = color,
            cellHeight = cellHeight,
            cellWidth = cellWidth,
            icon = if (appearance.icons != IconVisibility.NEVER) {
                IconCatalogue.vectorFor(button.iconName) ?: org.biglau.data.Builtin.NEXT_SCREEN.icon()
            } else {
                null
            },
            labelPosition = appearance.labelPosition,
            cornerRadius = appearance.cornerRadiusDp.dp,
            modifier = modifier,
            onClick = onClick,
            onLongClick = onLongClick,
        )

        // Der Ordner zeigt, was drin ist: bis zu vier verkleinerte Symbole seines Inhalts.
        // Ein Pfeil wie beim Screenwechsel waere hier gelogen - man wechselt nicht, man
        // schaut hinein.
        is ButtonAction.Folder -> FolderTile(
            // Bewusst nicht button.label: der Ordner hat genau einen Namen, und der steht
            // am Ordner selbst - sonst hiesse dasselbe Ding auf der Kachel anders als darin.
            label = folderOf(action.screenId)?.name ?: stringResource(R.string.folder),
            iconName = button.iconName,
            preview = folderOf(action.screenId)?.let { FolderEdits.preview(it) }.orEmpty(),
            appIcon = appIcon,
            background = color,
            cellWidth = cellWidth,
            cellHeight = cellHeight,
            appearance = appearance,
            modifier = modifier,
            onClick = onClick,
            onLongClick = onLongClick,
        )

        is ButtonAction.Link -> BigTile(
            label = button.label ?: LinkTarget.labelFor(action.url),
            background = color,
            cellHeight = cellHeight,
            cellWidth = cellWidth,
            icon = if (appearance.icons != IconVisibility.NEVER) {
                IconCatalogue.vectorFor(button.iconName) ?: Icons.Filled.Public
            } else {
                null
            },
            labelPosition = appearance.labelPosition,
            cornerRadius = appearance.cornerRadiusDp.dp,
            modifier = modifier,
            onClick = onClick,
            onLongClick = onLongClick,
        )

        ButtonAction.None -> EmptyTile(
            appearance = appearance,
            cellWidth = cellWidth,
            cellHeight = cellHeight,
            column = cell.x,
            row = cell.y,
            label = button.label,
            modifier = modifier,
            onEdit = onLongClick,
        )
    }
}

/**
 * Wie gut der Empfang ist, in Worten.
 *
 * Die Balken tragen keinen Text - fuer einen Screenreader waere die Kachel sonst so
 * aussagekraeftig wie eine leere Flaeche mit dem Wort "Empfang" darauf.
 */
@Composable
private fun signalSpeech(reading: SignalReading?): String? {
    val zustand = reading?.let { SignalInfo.stateOf(it) } ?: return null
    return when (zustand) {
        SignalInfo.State.NO_PERMISSION -> stringResource(R.string.signal_no_permission)
        SignalInfo.State.NO_SIM -> stringResource(R.string.signal_no_sim)
        SignalInfo.State.NO_SERVICE -> stringResource(R.string.signal_no_service)
        // Schwach steht als Wort da, nicht als Farbe: die Warnfarbe kommt auf jedem
        // Kachelton auf unter 2 zu 1 und ist damit ausgerechnet in der Lage unlesbar,
        // in der sie etwas sagen soll. Siehe TileDangerTest.
        else -> TileSpeech.describe(
            label = if (zustand == SignalInfo.State.WEAK) stringResource(R.string.signal_weak) else "",
            state = stringResource(R.string.a11y_signal_bars, SignalInfo.bars(reading), SignalInfo.MAX_LEVEL),
            badge = SignalInfo.caption(reading),
        )
    }
}

/**
 * Ladestand in Worten. Der Blitz neben der Zahl ist ein Symbol ohne Text; am Kabel oder
 * nicht ist aber genau die Frage, wegen der man auf die Kachel sieht.
 */
@Composable
private fun batterySpeech(reading: BatteryReading?): String? {
    val prozent = reading?.let { BatteryInfo.percent(it) } ?: return null
    return TileSpeech.describe(
        label = "$prozent %",
        state = if (BatteryInfo.isCharging(reading)) stringResource(R.string.battery_charging) else null,
    )
}

/**
 * Ein leerer Platz. Es gibt zwei Wege hierher - ein Rasterplatz ohne Zelle und eine Zelle
 * ohne Aktion - und beide muessen gleich aussehen, sonst wirkt der Homescreen kaputt.
 * Die Fuellung bleibt still, der Rahmen macht den Platz auffindbar.
 */
@Composable
private fun EmptyTile(
    appearance: Appearance,
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier,
    /** Platz im Raster, nullbasiert - fuer die Ansage. */
    column: Int,
    row: Int,
    label: String? = null,
    onEdit: () -> Unit,
) {
    val palette = LocalBigPalette.current
    val einladung = label ?: stringResource(R.string.empty_tile_invite)
    BigTile(
        // Ein leerer Zustand ist eine Aufforderung, kein Trauerfall: die Kachel sagt, was
        // sie anbietet, statt was ihr fehlt. In der Beschreibung im Editor bleibt es
        // "Leer" - dort ist es eine Zustandsangabe und keine Einladung.
        label = einladung,
        // Wo der Platz ist, steht nur im Bild. Zwei leere Kacheln heissen beide
        // "Antippen", und wer sie nicht sieht, hat zweimal dasselbe Angebot vor sich -
        // am 04.09.2026 mit `tools/gleiche-namen.py` auf dem Startbildschirm gefunden.
        // Danach sagt auch der Editor nicht, welche Zelle er bearbeitet; die Kette
        // schweigt also durchgehend. Dieselben Worte wie in der Verschieben-Ansicht.
        contentDescription = TileSpeech.describe(
            label = einladung,
            state = stringResource(R.string.move_spot, row + 1, column + 1),
        ),
        background = palette.emptyTile,
        cellHeight = cellHeight,
        cellWidth = cellWidth,
        labelPosition = appearance.labelPosition,
        cornerRadius = appearance.cornerRadiusDp.dp,
        borderOverride = palette.emptyTileBorder,
        modifier = modifier,
        onClick = onEdit,
        onLongClick = onEdit,
    )
}

/** Auto-Farbe aus der Position, damit ein frisch angelegter Screen sofort sortiert wirkt. */
@Composable
private fun tileColor(button: Button, x: Int, y: Int, cols: Int): Color {
    val palette = LocalBigPalette.current
    // Der freie Ton kommt nach der Palette und vor der Automatik - und nur, wenn das Thema
    // ueberhaupt Kachelfarben kennt. Im Kontrast-Thema sind alle Palettenplaetze die
    // Hintergrundfarbe (PLAN.md 3.3: dort zaehlt nur Schwarz/Gelb); eine freie Farbe
    // dorthin durchzureichen waere genau der Fehler, den der Vorlaeufer gemacht haette.
    val themaKenntFarben = FreeTileColor.themeUsesTileColours(palette.tiles.map { it.toArgbLong() })
    if (themaKenntFarben) {
        button.colorHue?.let { ton ->
            return Color(
                FreeTileColor.forHue(
                    ton,
                    palette.background.toArgbLong(),
                    palette.onTile.toArgbLong(),
                    FreeTileColor.targetLuminance(palette.tiles.map { it.toArgbLong() }),
                ).toInt(),
            )
        }
    }
    val index = if (button.colorIndex >= 0) {
        button.colorIndex.mod(palette.tiles.size)
    } else {
        TileEdits.autoColorIndex(x, y, cols, palette.tiles.size)
    }
    return palette.tiles[index]
}

/**
 * Die Kachel eines Ordners: bis zu vier verkleinerte Symbole seines Inhalts, darunter der
 * Name in derselben Zone wie bei jeder anderen Kachel.
 *
 * Warum keine eigene Form, kein Stapel, kein Kreis: die Kachel muss sich in die Reihe fügen,
 * sonst bricht die Grundlinie über die Rasterzeile. Dass es ein Ordner ist, sagen die vier
 * kleinen Symbole - das ist Information, keine Verzierung.
 */
@Composable
private fun FolderTile(
    label: String,
    /** Selbst gewaehltes Symbol, siehe [org.biglau.ui.IconCatalogue]. */
    iconName: String?,
    preview: List<Cell>,
    appIcon: (String, String) -> ImageBitmap?,
    background: Color,
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
    appearance: Appearance,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    BigTile(
        label = label,
        background = background,
        cellWidth = cellWidth,
        cellHeight = cellHeight,
        labelPosition = appearance.labelPosition,
        cornerRadius = appearance.cornerRadiusDp.dp,
        // Leer: das Ordnersymbol. Gefuellt: der Inhalt selbst - das ist die Auskunft, die
        // man vor dem Oeffnen braucht.
        icon = when {
            appearance.icons == IconVisibility.NEVER -> null
            // Ein gewaehltes Symbol geht der Vorschau vor: wer der Bank-Kachel eine Karte
            // gibt, will die Karte sehen und nicht vier winzige App-Symbole.
            iconName != null -> IconCatalogue.vectorFor(iconName)
            preview.isEmpty() -> Icons.Filled.Folder
            else -> null
        },
        // "Keine Symbole" gilt auch hier. Die Vorschau besteht aus Symbolen; sie stehen zu
        // lassen, waehrend ueberall sonst keine mehr sind, sieht nach einem Fehler aus.
        // Der Ordnername allein sagt dann, was drin ist.
        iconContent = if (preview.isEmpty() || appearance.icons == IconVisibility.NEVER) {
            null
        } else {
            {
                FolderPreview(
                    cells = preview,
                    cellWidth = cellWidth,
                    // Was nach der Beschriftung uebrig bleibt - dieselbe Rechnung wie in
                    // BigTile, damit die Vorschau nicht mehr beansprucht, als da ist.
                    availableHeight = (cellHeight.value - vorschauZone(cellWidth, cellHeight)).dp,
                    appIcon = appIcon,
                )
            }
        },
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

/**
 * Bis zu vier verkleinerte Symbole des Ordnerinhalts, in zwei Reihen.
 *
 * Bewusst Symbole und keine Miniaturkacheln: eine Kachel im Kleinen ist ein grauer Fleck,
 * ein Symbol bleibt erkennbar. Und bewusst oben links, wo bei jeder anderen Kachel auch das
 * Symbol sitzt - die Ordnerkachel soll sich in die Reihe fuegen, nicht auffallen.
 */
@Composable
private fun FolderPreview(
    cells: List<Cell>,
    cellWidth: androidx.compose.ui.unit.Dp,
    availableHeight: androidx.compose.ui.unit.Dp,
    appIcon: (String, String) -> ImageBitmap?,
) {
    val palette = LocalBigPalette.current
    val kanteDp = FolderPreviewLayout.edgeDp(cellWidth.value, availableHeight.value)
    val kante = kanteDp.dp
    val reihen = FolderPreviewLayout.rows(availableHeight.value, kanteDp)
    Column(verticalArrangement = Arrangement.spacedBy(FolderPreviewLayout.GAP_DP.dp)) {
        cells.chunked(2).take(reihen).forEach { reihe ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                reihe.forEach { cell ->
                    Box(Modifier.size(kante), contentAlignment = Alignment.Center) {
                        when (val action = cell.button.action) {
                            is ButtonAction.App -> {
                                val bild = appIcon(action.packageName, action.activityName)
                                if (bild != null) {
                                    Image(bitmap = bild, contentDescription = null, modifier = Modifier.size(kante))
                                } else {
                                    Icon(Icons.Filled.Apps, null, tint = palette.onTile, modifier = Modifier.size(kante))
                                }
                            }

                            is ButtonAction.Action -> Icon(
                                action.builtin.icon(),
                                contentDescription = null,
                                tint = palette.onTile,
                                modifier = Modifier.size(kante),
                            )

                            is ButtonAction.Contact -> Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = palette.onTile,
                                modifier = Modifier.size(kante),
                            )

                            is ButtonAction.Shortcut -> Icon(
                                Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = palette.onTile,
                                modifier = Modifier.size(kante),
                            )

                            else -> Icon(
                                Icons.Filled.Widgets,
                                contentDescription = null,
                                tint = palette.onTile,
                                modifier = Modifier.size(kante),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Die Hoehe, die die Beschriftung der Ordnerkachel beansprucht. */
@Composable
private fun vorschauZone(
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
): Float {
    val labelSp = labelSizeSp(
        cellWidth.value,
        cellHeight.value,
        LocalTextScale.current,
        LocalLabelScale.current,
    )
    return labelZoneDp(cellHeight.value, labelSp)
}
