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
import androidx.compose.ui.Modifier
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
import org.biglau.data.Cell
import org.biglau.data.LabelPosition
import android.text.format.DateFormat
import androidx.compose.ui.platform.LocalContext
import org.biglau.data.Builtin
import org.biglau.data.Screen
import org.biglau.info.BatteryReading
import org.biglau.notify.SystemPackages
import org.biglau.notify.TileNotifications
import org.biglau.tiles.TileEdits
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
    notificationCounts: Map<String, Int> = emptyMap(),
    battery: BatteryReading? = null,
    systemPackages: SystemPackages = SystemPackages(),
    onActivate: (Cell) -> Unit = {},
    onEdit: (x: Int, y: Int) -> Unit = { _, _ -> },
) {
    val palette = LocalBigPalette.current
    val gutter = appearance.gutterDp.dp
    val radius = appearance.cornerRadiusDp.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                when (val bg = screen.background) {
                    is Background.Solid -> Color(bg.argb.toInt())
                    else -> palette.background
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

        Box(Modifier.fillMaxSize().padding(border)) {
            screen.freeSlots().forEach { (x, y) ->
                EmptyTile(
                    appearance = appearance,
                    cellWidth = cellW,
                    cellHeight = cellH,
                    modifier = Modifier
                        .offset(
                            x = metrics.offsetX(x, gutter.value).dp,
                            y = metrics.offsetY(y, gutter.value).dp,
                        )
                        .size(cellW, cellH),
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
                    systemPackages = systemPackages,
                    battery = battery,
                    appIcon = appIcon,
                    appLabel = appLabel,
                    shortcutIcon = shortcutIcon,
                    modifier = Modifier
                        .offset(
                            x = metrics.offsetX(cell.x, gutter.value).dp,
                            y = metrics.offsetY(cell.y, gutter.value).dp,
                        )
                        .size(w, h),
                    onClick = { onActivate(cell) },
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
    systemPackages: SystemPackages,
    battery: BatteryReading?,
    appIcon: (String, String) -> ImageBitmap?,
    appLabel: (String, String) -> String?,
    shortcutIcon: (String, String) -> ImageBitmap?,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val palette = LocalBigPalette.current
    val button = cell.button
    val color = tileColor(button, cell.x, cell.y, cols)
    val badge = TileNotifications.badgeFor(button, notificationCounts, systemPackages)

    when (val action = button.action) {
        is ButtonAction.Action -> BigTile(
            label = button.label ?: stringResource(action.builtin.labelRes()),
            background = color,
            cellHeight = cellHeight,
            cellWidth = cellWidth,
            badgeCount = badge,
            content = when (action.builtin) {
                Builtin.CLOCK -> {
                    {
                        ClockContent(
                            cellWidth = cellWidth,
                            cellHeight = cellHeight,
                            showDate = appearance.clockShowsDate,
                            twentyFourHour = DateFormat.is24HourFormat(LocalContext.current),
                        )
                    }
                }
                Builtin.BATTERY -> {
                    { BatteryContent(battery, cellWidth, cellHeight) }
                }
                else -> null
            },
            icon = if (appearance.showIcons) action.builtin.icon() else null,
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
            iconBitmap = if (appearance.showIcons) appIcon(action.packageName, action.activityName) else null,
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
            icon = if (appearance.showIcons && action.photoUri == null) {
                org.biglau.data.Builtin.CONTACTS.icon()
            } else {
                null
            },
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
            onEdit = onLongClick,
        )

        is ButtonAction.Shortcut -> BigTile(
            label = button.label ?: action.label,
            background = color,
            cellHeight = cellHeight,
            cellWidth = cellWidth,
            badgeCount = badge,
            iconBitmap = if (appearance.showIcons) shortcutIcon(action.packageName, action.shortcutId) else null,
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
            icon = if (appearance.showIcons) org.biglau.data.Builtin.NEXT_SCREEN.icon() else null,
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
            label = button.label,
            modifier = modifier,
            onEdit = onLongClick,
        )
    }
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
    label: String? = null,
    onEdit: () -> Unit,
) {
    val palette = LocalBigPalette.current
    BigTile(
        label = label ?: stringResource(R.string.empty_tile),
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
    button.customColor?.let { return Color(it.toInt()) }
    val index = if (button.colorIndex >= 0) {
        button.colorIndex.mod(palette.tiles.size)
    } else {
        TileEdits.autoColorIndex(x, y, cols, palette.tiles.size)
    }
    return palette.tiles[index]
}
