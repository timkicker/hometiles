package org.biglau.ui

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
import org.biglau.widgets.WidgetHostController

/**
 * Ein Widget in einer Zelle.
 *
 * Das Widget bekommt die ganze Flaeche und alle Beruehrungen - sonst waere es nicht bedienbar.
 * Zum Bearbeiten gibt es deshalb einen schmalen Griff am unteren Rand: sichtbar, aber klein
 * genug, dass er dem Widget nichts wegnimmt. Ohne ihn waere eine Widget-Kachel die einzige,
 * die man nicht mehr aendern kann.
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

    // Die Platte bleibt, das Widget sitzt darauf. Ohne sie schwebt es im Schwarz und
    // bricht den Rasterrhythmus - die Zelle sieht dann halb leer aus.
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(org.biglau.ui.theme.LocalBigPalette.current.emptyTile),
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

        // Im Bearbeitungsmodus gehoert die ganze Kachel der App. Der Balken oben sagt
        // "tippe eine Kachel an, um sie zu aendern" - fuer eine Widget-Kachel stimmte das
        // nicht: das Widget nahm die Beruehrung und oeffnete seine eigene App. Eine
        // Anleitung, die fuer eine Kachel nicht gilt, ist schlimmer als keine.
        if (editMode) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) { detectTapGestures(onTap = { onEdit() }) },
            )
        }

        // Der Griff: nur Langdruck, damit gewoehnliche Beruehrungen beim Widget bleiben.
        //
        // Er bringt seine eigene dunkle Flaeche mit. Die erste Fassung war ein heller Balken
        // mit halber Deckkraft - auf einem Widget mit weisser Karte war er schlicht
        // unsichtbar, und damit war die Kachel praktisch nicht mehr zu bearbeiten.
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
                    .background(org.biglau.ui.theme.LocalBigPalette.current.background.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 34.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(org.biglau.ui.theme.LocalBigPalette.current.onBackground),
                )
            }
        }
    }
}
