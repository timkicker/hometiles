package dev.kicker.hometiles.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.kicker.hometiles.widgets.WidgetHostController

/**
 * a widget in a cell.
 *
 * the widget gets the whole area and every touch, or it could not be used, so editing hangs
 * on a narrow handle at the bottom edge. without it a widget tile would be the one tile
 * that can never be changed again.
 */
@Composable
fun WidgetTile(
    widgetId: Int,
    cellWidth: Dp,
    cellHeight: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    editMode: Boolean = false,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember { WidgetHostController.get(context) }

    // the plate stays and the widget sits on it: without it the widget floats in the black
    // and breaks the grid rhythm.
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(dev.kicker.hometiles.ui.theme.LocalBigPalette.current.emptyTile),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                controller.createView(viewContext, widgetId)
                    ?: android.widget.FrameLayout(viewContext)
            },
            update = { view ->
                (view as? android.appwidget.AppWidgetHostView)?.let {
                    controller.resize(it, cellWidth.value.toInt(), cellHeight.value.toInt())
                }
            },
        )

        // in edit mode the whole tile belongs to the app: the banner says tap a tile to
        // change it, and the widget was taking the touch and opening its own app instead.
        if (editMode) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) { detectTapGestures(onTap = { onEdit() }) },
            )
        }

        // the handle: long press only, so ordinary touches stay with the widget. it brings
        // its own dark surface, since a half-transparent light bar was invisible on a widget
        // with a white card.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(30.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onEdit() })
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(dev.kicker.hometiles.ui.theme.LocalBigPalette.current.background.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 34.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(dev.kicker.hometiles.ui.theme.LocalBigPalette.current.onBackground),
                )
            }
        }
    }
}
