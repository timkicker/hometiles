package org.biglau.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.biglau.ui.theme.LocalCornerRadius
import org.biglau.core.ui.R
import org.biglau.ui.theme.LocalBigPalette
import org.biglau.ui.theme.tileBorder

/**
 * digit keypad in the tiles' shape language. the keys fill the available area instead of
 * having a fixed size: on three inches every unused area is a missed target.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BigKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    onLongDigit: ((Char) -> Unit)? = null,
    /** short hint under the digit, such as the speed-dial name. */
    hintFor: (Char) -> String? = { null },
    /** extra key bottom left: the dialer puts the plus there, the pin entry leaves it empty. */
    extraKey: Char? = null,
    /**
     * only where the keypad is the screen's main thing, the pin entry: the dialer has a
     * number field above it and must not take the focus from it.
     */
    takesFocus: Boolean = false,
    /**
     * where the focus goes when it runs out at the bottom. without this anchor that row is
     * unreachable by key: a named anchor, never a blind search.
     */
    below: FocusRequester? = null,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(extraKey?.toString() ?: "", "0", "⌫"),
    )

    // the movement is led, not searched.
    //
    // over the home screen the keypad did take focus on the one, but every direction key
    // ran into nothing: compose searches for the next target and finds it under the
    // overlay, on tiles nobody sees. after ten ups the focus was gone entirely, and with it
    // any way into the locked app.
    //
    // a `moveFocus` in the key's direction left the keypad at the edge just the same, so
    // each key has an anchor and its own arithmetic. empty slots are skipped; at an edge
    // nothing happens, except at the bottom, where [below] stands.
    val anchors = remember(rows.size) { rows.map { row -> row.map { FocusRequester() } } }
    var at by remember { mutableStateOf(0 to 0) }

    fun filled(row: Int, column: Int): Boolean =
        rows.getOrNull(row)?.getOrNull(column)?.isNotEmpty() == true

    /** the next filled key in this direction, or null at the edge. */
    fun neighbour(row: Int, column: Int, dr: Int, dc: Int): Pair<Int, Int>? {
        var r = row + dr
        var c = column + dc
        while (r in rows.indices && c in 0..2) {
            if (filled(r, c)) return r to c
            r += dr
            c += dc
        }
        return null
    }

    fun goTo(target: Pair<Int, Int>?): Boolean {
        if (target == null) return false
        at = target
        return runCatching { anchors[target.first][target.second].requestFocus() }.isSuccess
    }

    LaunchedEffect(takesFocus) { if (takesFocus) goTo(0 to 0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    val (r, c) = at
                    when (event.key) {
                        Key.DirectionUp -> {
                            goTo(neighbour(r, c, -1, 0))
                            true
                        }
                        Key.DirectionDown -> {
                            val target = neighbour(r, c, 1, 0)
                            if (target != null) {
                                goTo(target)
                            } else {
                                below?.let { runCatching { it.requestFocus() } }
                            }
                            true
                        }
                        Key.DirectionLeft -> {
                            goTo(neighbour(r, c, 0, -1))
                            true
                        }
                        Key.DirectionRight -> {
                            goTo(neighbour(r, c, 0, 1))
                            true
                        }
                        else -> false
                    }
                }
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEachIndexed { columnIndex, key ->
                    val slot = Modifier
                        .focusRequester(anchors[rowIndex][columnIndex])
                        .onFocusChanged { if (it.isFocused) at = rowIndex to columnIndex }
                    when (key) {
                        "" -> Box(Modifier.weight(1f))
                        "⌫" -> KeypadKey(
                            Modifier.weight(1f).then(slot),
                            onClick = onBackspace,
                            // without a description the key is silent to TalkBack and not
                            // findable in the tree at all.
                            description = stringResource(R.string.keypad_backspace),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = null,
                                tint = LocalBigPalette.current.onBackground,
                            )
                        }
                        else -> KeypadKey(
                            Modifier.weight(1f).then(slot),
                            onClick = { onDigit(key[0]) },
                            onLongClick = onLongDigit?.let { handler -> { handler(key[0]) } },
                            description = key,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = key,
                                    color = LocalBigPalette.current.onBackground,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                hintFor(key[0])?.let { hint ->
                                    Text(
                                        text = hint,
                                        color = LocalBigPalette.current.accent,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadKey(
    modifier: Modifier,
    onClick: () -> Unit,
    description: String,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val palette = LocalBigPalette.current
    val border = palette.tileBorder()
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(LocalCornerRadius.current))
            .background(palette.emptyTile)
            .then(if (border != null) Modifier.border(3.dp, border, RoundedCornerShape(LocalCornerRadius.current)) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/** how visible the border of an empty dot is; the reason sits at [PinDots]. */
internal const val DOT_BORDER = 0.55f

/**
 * dot row for a hidden entry.
 *
 * how many digits stand there was said by the filled dots alone, so anyone who cannot see
 * them got no answer at all: BigLau makes no sound on purpose. the count does not give the
 * pin away, and withholding it helps nobody.
 *
 * `liveRegion`, so it is said while typing and not only when touched.
 */
@Composable
fun PinDots(length: Int, total: Int = 8, modifier: Modifier = Modifier) {
    val palette = LocalBigPalette.current
    val announcement = if (length == 0) {
        stringResource(R.string.a11y_pin_empty)
    } else {
        pluralStringResource(R.plurals.a11y_pin_digits, length, length)
    }
    Row(
        modifier = modifier.semantics {
            contentDescription = announcement
            liveRegion = LiveRegionMode.Polite
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (index < length) palette.accent else palette.emptyTile)
                    // the border carries the empty dot alone: its fill is the quiet tile
                    // colour and sits at 1.09:1 to 1.16:1 against the background. at 0.35
                    // the border reached 2.23:1 in the light theme, under the area
                    // threshold of 3.0; at 0.55 it is 6.12 / 3.94 / 5.28.
                    .border(2.dp, palette.onBackground.copy(alpha = DOT_BORDER), RoundedCornerShape(50))
                    .size(18.dp)
            )
        }
    }
}
