package dev.kicker.hometiles.ui

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
import dev.kicker.hometiles.tiles.FocusOrder
import dev.kicker.hometiles.tiles.PadDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.kicker.hometiles.R
import dev.kicker.hometiles.data.Appearance
import dev.kicker.hometiles.data.Background
import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import androidx.compose.material.icons.Icons
import dev.kicker.hometiles.web.LinkTarget
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
import dev.kicker.hometiles.data.Cell
import dev.kicker.hometiles.tiles.FolderEdits
import dev.kicker.hometiles.data.IconVisibility
import dev.kicker.hometiles.data.LabelPosition
import android.text.format.DateFormat
import androidx.compose.ui.platform.LocalContext
import dev.kicker.hometiles.data.Builtin
import dev.kicker.hometiles.data.Screen
import dev.kicker.hometiles.a11y.TileSpeech
import dev.kicker.hometiles.info.BatteryInfo
import dev.kicker.hometiles.info.BatteryReading
import dev.kicker.hometiles.info.SignalInfo
import dev.kicker.hometiles.info.SignalReading
import dev.kicker.hometiles.notify.SystemPackages
import dev.kicker.hometiles.notify.TileNotifications
import dev.kicker.hometiles.tiles.TileEdits
import dev.kicker.hometiles.ui.theme.LocalTextScale
import dev.kicker.hometiles.ui.theme.LocalLabelScale
import dev.kicker.hometiles.ui.theme.FreeTileColor
import dev.kicker.hometiles.ui.theme.toArgbLong
import dev.kicker.hometiles.ui.theme.LocalBigPalette

/**
 * draws a screen as a grid. cells may span several slots; free slots get a muted
 * placeholder tile that opens the editor.
 */
@Composable
fun HomeScreenView(
    screen: Screen,
    appearance: Appearance,
    modifier: Modifier = Modifier,
    appIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    appLabel: (String, String) -> String? = { _, _ -> null },
    shortcutIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    /** the folder behind an id, for the preview on the folder tile. */
    folderOf: (String) -> Screen? = { null },
    notificationCounts: Map<String, Int> = emptyMap(),
    /** unseen missed calls; that tile counts the call log, not notices. */
    missedCalls: Int = 0,
    /** unread messages, or null when HomeTiles may not read them. */
    unreadMessages: Int? = null,
    battery: BatteryReading? = null,
    signal: SignalReading? = null,
    systemPackages: SystemPackages = SystemPackages(),
    onActivate: (Cell) -> Unit = {},
    /** in edit mode every touch belongs to the app, even on a widget. */
    editMode: Boolean = false,
    onEdit: (x: Int, y: Int) -> Unit = { _, _ -> },
    /**
     * the left softkey. `PLAN.md` 10.3.3.
     *
     * it carries what hangs on the long press. a way of its own and not [onEdit], because
     * the trigger-on-long-press setting guards against brushing with a finger, and a key
     * is not brushed.
     */
    onMenu: (x: Int, y: Int) -> Unit = { _, _ -> },
    /**
     * is this frame on top right now?
     *
     * the home screen and an open folder use the same frame and are composed at the same
     * time, the folder lying over it in the same window. which of the two should get the
     * keys is not something the frame can know by itself.
     */
    active: Boolean = true,
    /**
     * the strip under the grid, if there is one.
     *
     * a folder and the menu-key list carry a closing row under the grid. it is a sibling of
     * the frame, not its child, and the frame consumes every direction key, so the focus
     * never reached it: `tools/unerreichbar.py` reported nine clickable areas in a folder
     * and eight reached.
     *
     * a blind `moveFocus(Down)` would be wrong here, taking the next focusable node, which
     * can be a home screen tile under the overlay. a named anchor instead: the frame knows
     * where, or it does nothing.
     */
    below: FocusRequester? = null,
    /**
     * the way back into the grid, for the strip below.
     *
     * it always hangs on the cell the focus last sat on, so one comes out where one went
     * in. a blind `moveFocus(Up)` would be wrong here too.
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
                // exhaustive: an else branch swallowed the image case, and nobody saw
                // that it was never drawn.
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

        // `PLAN.md` 10.3.2: the order follows the grid, not the list in the file.
        //
        // the frame catches the keys, not the single tile: only here is it known which cell
        // has the focus and which column it came from. and only whoever consumes the key can
        // let nothing happen at an edge; otherwise compose picks a target itself.
        //
        // the empty slots belong to it: they are clickable, so they must be reachable.
        val targets = remember(screen) { FocusOrder.targets(screen) }
        val anchors = remember(targets) { targets.associateWith { FocusRequester() } }
        var focusedCell by remember(screen.id) { mutableStateOf(FocusOrder.first(targets)) }
        var rememberedColumn by remember(screen.id) { mutableStateOf<Int?>(null) }

        // `PLAN.md` 10.3.1: at *start* the focus is there by itself, but not when this
        // frame comes on top later.
        //
        // android has a touch mode: while the last input was a tap, no element takes the
        // focus and `requestFocus` is silently ignored. measured with `dumpsys window` -
        // after a tap `mInTouchMode=true` and no focus, after a key press
        // `mInTouchMode=false` and the focus sits on the top left tile at once.
        //
        // the folder is the other case, and it was broken: nine clickable areas, *none* ever
        // focused. `onPreviewKeyEvent` only walks from the focused node to the root, so
        // without a focus the frame never runs, nothing moves, and the focus never comes
        // back. a screen without focus is a frozen one on a key phone.
        //
        // so the frame fetches it as soon as it is on top. in touch mode that stays silently
        // discarded, which is right: whoever taps wants no frame around a tile.
        LaunchedEffect(active, targets) {
            if (active) {
                val target = focusedCell ?: FocusOrder.first(targets)
                // `requestFocus` throws while the node is not attached yet. that is an
                // order, not a fault: the focus then sits where compose put it.
                runCatching { target?.let { anchors[it]?.requestFocus() } }
            }
        }

        // `PLAN.md` 10.3.3: a digit picks the slot with that number and triggers it.
        //
        // triggers and not merely jumps: pressing four means the pharmacy, not the focus on
        // the pharmacy. a second press would be a detour one has to remember.
        //
        // digits may do this only where they mean nothing else, which holds here: neither
        // the home screen nor a folder has an input field.
        fun select(digit: Int): Boolean {
            val target = FocusOrder.numbered(targets, digit) ?: return true
            if (target in screen.cells) {
                onActivate(target)
            } else {
                // an empty slot has nothing to start; it leads where one fills it.
                onEdit(target.x, target.y)
            }
            return true
        }

        fun move(direction: PadDirection): Boolean {
            val from = focusedCell ?: FocusOrder.first(targets) ?: return true
            val target = FocusOrder.neighbour(targets, from, direction, rememberedColumn)
            if (target == null && direction == PadDirection.DOWN && below != null) {
                // under the last row sits the strip that closes the overlay: downwards
                // only, and only when there is one.
                runCatching { below.requestFocus() }
                return true
            }
            if (target != null) {
                // horizontal sets the remembered column, vertical leaves it.
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
                            // the left softkey; the right one is back, which android
                            // already hands to the activity.
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
            // the bars are drawn: without this a screen reader hears only signal and not
            // how good it is. see TileSpeech.
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
            // only the hard decision no icons at all falls here; whether one fits this tile
            // is known in BigTile, where the cell size is. a chosen icon beats a derived
            // one. `PLAN.md` 2.2.
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
            // a chosen icon beats the app's own image too: choosing one was deliberate.
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
            // `PLAN.md` 3.4: initials without a photo. the same person icon on every
            // contact tile made three contacts side by side look alike.
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
                IconCatalogue.vectorFor(button.iconName) ?: dev.kicker.hometiles.data.Builtin.NEXT_SCREEN.icon()
            } else {
                null
            },
            labelPosition = appearance.labelPosition,
            cornerRadius = appearance.cornerRadiusDp.dp,
            modifier = modifier,
            onClick = onClick,
            onLongClick = onLongClick,
        )

        // the folder shows what is in it: an arrow like the screen change would lie, since
        // one does not change, one looks inside.
        is ButtonAction.Folder -> FolderTile(
            // not button.label: the folder has exactly one name, and it lives on the folder
            // itself, or the same thing would be called differently outside and inside.
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
 * how good the signal is, in words: the bars carry no text, so to a screen reader the tile
 * would otherwise say as much as an empty area labelled signal.
 */
@Composable
private fun signalSpeech(reading: SignalReading?): String? {
    val state = reading?.let { SignalInfo.stateOf(it) } ?: return null
    return when (state) {
        SignalInfo.State.NO_PERMISSION -> stringResource(R.string.signal_no_permission)
        SignalInfo.State.NO_SIM -> stringResource(R.string.signal_no_sim)
        SignalInfo.State.NO_SERVICE -> stringResource(R.string.signal_no_service)
        // weak stands as a word, not a colour: the danger colour reaches under 2 to one on
        // every tile hue and is unreadable in exactly the situation it should speak in.
        else -> TileSpeech.describe(
            label = if (state == SignalInfo.State.WEAK) stringResource(R.string.signal_weak) else "",
            state = stringResource(R.string.a11y_signal_bars, SignalInfo.bars(reading), SignalInfo.MAX_LEVEL),
            badge = SignalInfo.caption(reading),
        )
    }
}

/**
 * the battery in words. the bolt beside the number is an icon without text, and on the
 * cable or not is exactly the question one looks at the tile for.
 */
@Composable
private fun batterySpeech(reading: BatteryReading?): String? {
    val percent = reading?.let { BatteryInfo.percent(it) } ?: return null
    return TileSpeech.describe(
        label = "$percent %",
        state = if (BatteryInfo.isCharging(reading)) stringResource(R.string.battery_charging) else null,
    )
}

/**
 * an empty slot. two ways lead here, a grid slot without a cell and a cell without an
 * action, and both must look alike or the home screen looks broken. the fill stays quiet,
 * the border makes the slot findable.
 */
@Composable
private fun EmptyTile(
    appearance: Appearance,
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier,
    /** slot in the grid, zero based, for the announcement. */
    column: Int,
    row: Int,
    label: String? = null,
    onEdit: () -> Unit,
) {
    val palette = LocalBigPalette.current
    val invite = label ?: stringResource(R.string.empty_tile_invite)
    BigTile(
        // an empty state is an invitation, not a loss: the tile says what it offers rather
        // than what it lacks. in the editor it stays empty, a statement of state.
        label = invite,
        // where the slot is stands only in the picture: two empty tiles were both called
        // tap, so anyone not seeing them had the same offer twice. found with
        // `tools/gleiche-namen.py`. the same words as in the move view.
        contentDescription = TileSpeech.describe(
            label = invite,
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

/** automatic colour from the position, so a fresh screen looks ordered at once. */
@Composable
private fun tileColor(button: Button, x: Int, y: Int, cols: Int): Color {
    val palette = LocalBigPalette.current
    // the free hue comes after the palette and before the automatic one, and only where the
    // theme knows tile colours at all: in the contrast theme every palette slot is the
    // background colour (`PLAN.md` 3.3).
    val themeHasColours = FreeTileColor.themeUsesTileColours(palette.tiles.map { it.toArgbLong() })
    if (themeHasColours) {
        button.colorHue?.let { hue ->
            return Color(
                FreeTileColor.forHue(
                    hue,
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
 * a folder's tile: up to four shrunken icons of its contents, the name below in the same
 * zone as on any other tile.
 *
 * no shape of its own, no stack, no circle: the tile has to fall in line or the baseline of
 * the grid row breaks. that it is a folder is said by the four small icons, which is
 * information, not decoration.
 */
@Composable
private fun FolderTile(
    label: String,
    /** a hand-picked icon, see [dev.kicker.hometiles.ui.IconCatalogue]. */
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
        // empty: the folder icon. filled: the contents, which is the answer one needs
        // before opening it.
        icon = when {
            appearance.icons == IconVisibility.NEVER -> null
            // a chosen icon beats the preview: giving the banking tile a card means wanting
            // to see the card, not four tiny app icons.
            iconName != null -> IconCatalogue.vectorFor(iconName)
            preview.isEmpty() -> Icons.Filled.Folder
            else -> null
        },
        // no icons holds here too: the preview is made of icons, and leaving them while
        // there are none anywhere else looks like a fault.
        iconContent = if (preview.isEmpty() || appearance.icons == IconVisibility.NEVER) {
            null
        } else {
            {
                FolderPreview(
                    cells = preview,
                    cellWidth = cellWidth,
                    // what is left after the label, the same arithmetic as in BigTile.
                    availableHeight = (cellHeight.value - previewZone(cellWidth, cellHeight)).dp,
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
 * up to four shrunken icons of the folder's contents, in two rows.
 *
 * icons and not miniature tiles: a tile in small is a grey smudge, an icon stays
 * recognisable. and top left, where every other tile carries its icon.
 */
@Composable
private fun FolderPreview(
    cells: List<Cell>,
    cellWidth: androidx.compose.ui.unit.Dp,
    availableHeight: androidx.compose.ui.unit.Dp,
    appIcon: (String, String) -> ImageBitmap?,
) {
    val palette = LocalBigPalette.current
    val edgeDp = FolderPreviewLayout.edgeDp(cellWidth.value, availableHeight.value)
    val edge = edgeDp.dp
    val rowCount = FolderPreviewLayout.rows(availableHeight.value, edgeDp)
    Column(verticalArrangement = Arrangement.spacedBy(FolderPreviewLayout.GAP_DP.dp)) {
        cells.chunked(2).take(rowCount).forEach { chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                chunk.forEach { cell ->
                    Box(Modifier.size(edge), contentAlignment = Alignment.Center) {
                        when (val action = cell.button.action) {
                            is ButtonAction.App -> {
                                val image = appIcon(action.packageName, action.activityName)
                                if (image != null) {
                                    Image(bitmap = image, contentDescription = null, modifier = Modifier.size(edge))
                                } else {
                                    Icon(Icons.Filled.Apps, null, tint = palette.onTile, modifier = Modifier.size(edge))
                                }
                            }

                            is ButtonAction.Action -> Icon(
                                action.builtin.icon(),
                                contentDescription = null,
                                tint = palette.onTile,
                                modifier = Modifier.size(edge),
                            )

                            is ButtonAction.Contact -> Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = palette.onTile,
                                modifier = Modifier.size(edge),
                            )

                            is ButtonAction.Shortcut -> Icon(
                                Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = palette.onTile,
                                modifier = Modifier.size(edge),
                            )

                            else -> Icon(
                                Icons.Filled.Widgets,
                                contentDescription = null,
                                tint = palette.onTile,
                                modifier = Modifier.size(edge),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** the height the folder tile's label claims. */
@Composable
private fun previewZone(
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
