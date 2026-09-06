package dev.kicker.hometiles.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.kicker.hometiles.ui.theme.LocalCornerRadius
import dev.kicker.hometiles.core.ui.R
import dev.kicker.hometiles.ui.theme.LocalBigPalette
import dev.kicker.hometiles.ui.theme.LocalTextScale
import dev.kicker.hometiles.ui.theme.tileBorder

/**
 * search field in the tiles' shape language: flat, no shadow, large type.
 *
 * takes no focus on opening: on three inches the keyboard would eat half the list before
 * anyone has looked at it.
 */
@Composable
fun BigSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    /** second line in the field, such as the number of matches. */
    secondary: String? = null,
    /** the keyboard's search key. */
    onSearch: (() -> Unit)? = null,
) {
    val palette = LocalBigPalette.current
    val scale = LocalTextScale.current
    val border = palette.tileBorder()

    // a tap anywhere in the row places the caret. the drawn row is 64 dp high, the input
    // inside it only 48, and 38 with the match count below: a tap on the top fourteen
    // pixels did nothing at all although a field is drawn there.
    //
    // `pointerInput` and not `clickable`: a clickable row would be a button to the screen
    // reader (see BigRow), and this is an input field.
    val caret = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LocalCornerRadius.current))
            .background(palette.emptyTile)
            .pointerInput(Unit) {
                detectTapGestures { caret.requestFocus() }
            }
            .then(if (border != null) Modifier.border(3.dp, border, RoundedCornerShape(LocalCornerRadius.current)) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = palette.onBackground,
            modifier = Modifier.size(28.dp),
        )
        Column(Modifier.weight(1f)) {
            Box(contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = hint,
                    color = palette.onBackground,
                    fontSize = (20f * scale).sp,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = palette.onBackground,
                    fontSize = (20f * scale).sp,
                    fontWeight = FontWeight.Bold,
                ),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                // without this name a screen reader says only "input field": the painted
                // placeholder is not a label to it.
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(caret)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) {
                            false
                        } else {
                            when (event.key) {
                                // the search key `search_matches` names. `ImeAction.Search`
                                // covers the enter key only, and a phone with a keyboard
                                // really has the magnifier.
                                Key.Search -> {
                                    onSearch?.invoke()
                                    true
                                }
                                // down belongs to the list. an `EditText` takes the focus
                                // and does not pass it on: measured four presses without
                                // movement, which left the whole list unreachable by key.
                                Key.DirectionDown -> {
                                    focus.moveFocus(FocusDirection.Down)
                                    true
                                }
                                else -> false
                            }
                        }
                    }
                    .semantics { contentDescription = hint },
            )
            }
            // on three inches the keyboard covers the result list entirely, so the count
            // stands in the field itself, the one line that stays visible while typing.
            if (secondary != null) {
                Text(
                    text = secondary,
                    color = palette.onBackground,
                    fontSize = (14f * scale).sp,
                    // two lines: at 1.35 system scale the search-key hint does not fit one.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value.isNotEmpty()) {
            // a surface of its own: 48 dp is the minimum for a fingertip, and an unnamed
            // button stays silent to TalkBack.
            val clear = stringResource(R.string.search_clear)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(LocalCornerRadius.current))
                    .clickable { onValueChange("") }
                    .semantics { contentDescription = clear },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    tint = palette.onBackground,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
