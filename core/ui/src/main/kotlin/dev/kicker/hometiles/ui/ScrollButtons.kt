package dev.kicker.hometiles.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
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
import dev.kicker.hometiles.ui.theme.LocalCornerRadius
import dev.kicker.hometiles.core.ui.R
import dev.kicker.hometiles.a11y.Paging
import dev.kicker.hometiles.ui.theme.LocalBigPalette

/**
 * two big paging buttons under the list, full width where the thumb already rests.
 *
 * at the end of the list the button goes visibly pale: one that looks as always and does
 * nothing makes people doubt their own handling.
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

/** the same two buttons, placed by the caller; the wizard puts them beside Next. */
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

/**
 * how pale a button goes at the end of the list.
 *
 * pale is not invisible: the button must stay findable. an icon is an area, not text, so
 * `Tokens.MIN_TILE_ON_BACKGROUND` (3.0) applies. at 0.4 the light theme reached 2.59, under
 * the threshold; 0.5 gives 5.37 / 4.53 / 3.44 against 18.10 / 17.20 / 15.66 when awake.
 */
internal const val PALE = 0.5f

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
        // caller first, then the floors. a fixed size *before* the caller's modifier
        // bounds it: the wizard asked for 72 dp and got 56. `heightIn`/`widthIn` are
        // floors, and 48 dp is the minimum for a fingertip.
        modifier = Modifier
            .then(modifier)
            .heightIn(min = 56.dp)
            .widthIn(min = 48.dp)
            .clip(RoundedCornerShape(LocalCornerRadius.current))
            .background(if (enabled) surface.fill else surface.fill.copy(alpha = PALE))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) surface.ink else surface.ink.copy(alpha = PALE),
            modifier = Modifier.padding(4.dp).height(36.dp),
        )
    }
}
