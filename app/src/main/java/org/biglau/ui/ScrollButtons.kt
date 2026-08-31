package org.biglau.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.biglau.R
import org.biglau.a11y.Paging
import org.biglau.ui.theme.LocalBigPalette

/**
 * Zwei große Knöpfe zum Blättern, unter der Liste.
 *
 * Sie sitzen unten und über die volle Breite, weil dort der Daumen ohnehin liegt. Am
 * Listenende wird der jeweilige Knopf sichtbar blass — ein Knopf, der aussieht wie immer
 * und nichts tut, lässt einen an der eigenen Bedienung zweifeln.
 */
@Composable
fun ScrollButtons(
    state: LazyListState,
    modifier: Modifier = Modifier,
) = ScrollButtonPair(state, modifier) { up, down ->
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        up(Modifier.weight(1f))
        down(Modifier.weight(1f))
    }
}

/**
 * Dieselben zwei Knoepfe, aber der Aufrufer bestimmt, wo sie liegen. Der Assistent setzt
 * sie neben "Weiter" statt in eine eigene Zeile - dort waere jede Zeile eine zu viel.
 */
@Composable
fun ScrollButtonPair(
    state: LazyListState,
    modifier: Modifier = Modifier,
    layout: @Composable (
        up: @Composable (Modifier) -> Unit,
        down: @Composable (Modifier) -> Unit,
    ) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val layout = state.layoutInfo
    val rows = layout.visibleItemsInfo.map { Paging.Row(it.index, it.offset, it.size) }
    val whole = Paging.fullyVisible(rows, layout.viewportStartOffset, layout.viewportEndOffset)
    val visible = whole.size
    val total = layout.totalItemsCount
    val first = Paging.firstFullyVisibleIndex(
        rows,
        layout.viewportStartOffset,
        layout.viewportEndOffset,
        state.firstVisibleItemIndex,
    )
    val up = Paging.canGoUp(first)
    val down = Paging.canGoDown(first, visible, total)

    layout(
        { m ->
            PageButton(
                icon = Icons.Filled.KeyboardArrowUp,
                description = stringResource(R.string.scroll_up),
                enabled = up,
                modifier = m,
            ) {
                scope.launch { state.animateScrollToItem(Paging.up(first, visible)) }
            }
        },
        { m ->
            PageButton(
                icon = Icons.Filled.KeyboardArrowDown,
                description = stringResource(R.string.scroll_down),
                enabled = down,
                modifier = m,
            ) {
                scope.launch { state.animateScrollToItem(Paging.down(first, visible, total)) }
            }
        },
    )
}

@Composable
private fun PageButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalBigPalette.current
    val surface = palette.surfaceDefault
    Box(
        // Hoehe zuerst, damit ein Aufrufer sie ueberschreiben kann: neben einer 72-dp-Zeile
        // saehe ein 56-dp-Knopf nach einem Versehen aus.
        modifier = Modifier
            .height(56.dp)
            .then(modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) surface.fill else surface.fill.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) surface.ink else surface.ink.copy(alpha = 0.4f),
            modifier = Modifier.padding(4.dp).height(36.dp),
        )
    }
}
