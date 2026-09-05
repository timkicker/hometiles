package org.biglau.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import org.biglau.ui.theme.LocalTextScale
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.biglau.security.Pin
import org.biglau.ui.theme.LocalBigPalette

/** the text size setting acts on the pin entry only up to here. */
const val PIN_MAX_TEXT_SCALE = 1.25f

/** capped because the keys matter more than the words here; never smaller than chosen. */
fun pinTextScale(current: Float): Float = cappedTextScale(current, PIN_MAX_TEXT_SCALE)

/** how long the emergency exit must be held. */
const val EMERGENCY_HOLD_MILLIS = 30_000L

/**
 * pin entry: check against a stored value, or take a new pin ([onAccept] set).
 *
 * the emergency exit sits here and not on the first tile, which answers after half a second
 * with the editor, so a thirty-second press would never arrive there.
 */
@Composable
fun PinGate(
    title: String,
    explainer: String?,
    wrongText: String,
    confirmLabel: String,
    onCheck: (String) -> Boolean,
    onAccept: (String) -> Unit,
    acceptOnComplete: Boolean,
    onEmergencyExit: (() -> Unit)? = null,
) {
    val palette = LocalBigPalette.current
    var entered by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    var heldSeconds by remember { mutableStateOf(0) }
    // the keypad sends the focus here when it runs out below the last row; without the
    // anchor this row is unreachable by key.
    val doneAnchor = remember { FocusRequester() }

    if (onEmergencyExit != null) {
        LaunchedEffect(holding) {
            heldSeconds = 0
            if (!holding) return@LaunchedEffect
            while (heldSeconds < EMERGENCY_HOLD_MILLIS / 1000) {
                delay(1000)
                heldSeconds++
            }
            onEmergencyExit()
        }
    }

    // the keys matter more than the words here: at 200 % the heading and the confirm row
    // grew until only a strip was left for the keypad, whose digit keys measured 21 dp on a
    // screen one has to hit exactly. the digits themselves come from the area anyway.
    val capped = pinTextScale(LocalTextScale.current)
    CompositionLocalProvider(LocalTextScale provides capped) {
    Column(
        // opaque: over the home screen the tiles shone through between the keys, and a
        // lock one can see through does not look like a lock. safeDrawingPadding belongs
        // here too, or the confirm row hides behind the navigation bar; where a caller has
        // set the insets already nothing is added, compose consumes them.
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            // the lock lies *over* the tiles instead of replacing them: without this a tap
            // into its margin started the tile underneath, and a lock one taps past is none.
            .absorbTouches()
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BigHeading(title)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onEmergencyExit == null) {
                        Modifier
                    } else {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    holding = true
                                    tryAwaitRelease()
                                    holding = false
                                },
                            )
                        }
                    }
                )
                .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            PinDots(entered.length)
        }

        // explanation and error share a slot of fixed height: five lines against one made
        // the keypad jump by centimetres on every wrong try.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(horizontal = 4.dp),
        ) {
            when {
                heldSeconds > 0 -> Text(
                    text = "${EMERGENCY_HOLD_MILLIS / 1000 - heldSeconds}",
                    color = palette.accent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )

                // `liveRegion`, or the reason stays silent: the dot row says there is no
                // digit left, and the announcement was that alone, so anyone not looking
                // types the same wrong pin again.
                wrong -> Text(
                    text = wrongText,
                    color = palette.dangerText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )

                explainer != null -> Text(
                    text = explainer,
                    color = palette.onBackground,
                    fontSize = 15.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // the focus must go into the keypad, or the lock cannot be opened by key.
        //
        // as an activity's screen that happens by itself; as an *overlay* over the home
        // screen it does not: measured eleven clickable areas, zero reached, which left the
        // app behind the lock unreachable too.
        //
        // `takesFocus` puts it on the one, and the keypad leads the movement from there.
        // both belong together: a focus that cannot move is half the way.
        Box(Modifier.weight(1f)) {
            BigKeypad(
                takesFocus = true,
                below = doneAnchor,
                onDigit = { digit ->
                    wrong = false
                    if (entered.length < Pin.MAX_LENGTH) entered += digit
                    if (acceptOnComplete && onCheck(entered)) onAccept(entered)
                },
                onBackspace = { entered = entered.dropLast(1); wrong = false },
            )
        }

        // without a single digit, done is not a button. it used to look like one and
        // answered a tap with this pin is wrong, which was untrue: nothing was entered. the
        // same grip as sending without text and calling without a number.
        BigRow(
            label = confirmLabel,
            modifier = Modifier.focusRequester(doneAnchor),
            surface = if (entered.isEmpty()) palette.surfaceDefault else palette.surfaceAccent,
            onClick = if (entered.isEmpty()) {
                null
            } else {
                {
                    if (onCheck(entered)) {
                        onAccept(entered)
                    } else {
                        // clear the entry: left standing, the next pin was appended to the
                        // wrong one and every further digit fell away silently.
                        wrong = true
                        entered = ""
                    }
                }
            },
        )
    }
    }
}
