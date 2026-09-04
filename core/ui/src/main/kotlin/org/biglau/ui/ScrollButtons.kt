package org.biglau.ui

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
import org.biglau.ui.theme.LocalCornerRadius
import org.biglau.core.ui.R
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

/**
 * Wie blass ein Knopf am Listenende wird.
 *
 * Blass heisst **nicht** unsichtbar: wer nicht weiterblaettern kann, soll sehen, dass der
 * Knopf noch da ist - sonst sucht er ihn. Ein Symbol ist eine Flaeche, keine Schrift, also
 * gilt `Tokens.MIN_TILE_ON_BACKGROUND` (3,0).
 *
 * Bis zum 04.09.2026 stand hier 0,4. Nachgerechnet und am Emulator im Bildpunkt bestaetigt:
 * dunkel 3,81, Kontrast 3,18 - und **hell 2,59**. Im hellen Thema war das blasse Symbol
 * `#999999` auf `#F3F4F4` und damit unter der Schwelle. Mit 0,5 sind es 5,37 / 4,53 / 3,44,
 * und der Abstand zum wachen Knopf (18,10 / 17,20 / 15,66) bleibt gross genug, dass man den
 * Unterschied sieht.
 */
internal const val BLASS = 0.5f

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
        // Der Aufrufer zuerst, dann die Untergrenzen.
        //
        // Bis zum 04.09.2026 stand hier `.height(56.dp).then(modifier)` mit dem Kommentar
        // "Hoehe zuerst, damit ein Aufrufer sie ueberschreiben kann". Das Gegenteil war der
        // Fall: eine feste Groesse **vor** dem Aufrufer-Modifier begrenzt ihn. Der Assistent
        // bat um 72 dp und bekam 56 - am Emulator nachgemessen, 77 statt 99 Bildpunkten.
        //
        // `heightIn`/`widthIn` sind Untergrenzen und tun genau, was der alte Kommentar
        // versprach: wer nichts sagt, bekommt 56 dp hoch; wer etwas sagt, bekommt es. Die
        // 48 dp Breite sind das Mindestmass fuer einen Fingertipp - ohne sie war der Knopf
        // im Assistenten 40,7 dp breit, weil er sich seine Breite vom Symbol holte.
        modifier = Modifier
            .then(modifier)
            .heightIn(min = 56.dp)
            .widthIn(min = 48.dp)
            .clip(RoundedCornerShape(LocalCornerRadius.current))
            .background(if (enabled) surface.fill else surface.fill.copy(alpha = BLASS))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) surface.ink else surface.ink.copy(alpha = BLASS),
            modifier = Modifier.padding(4.dp).height(36.dp),
        )
    }
}
