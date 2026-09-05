package org.biglau.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.biglau.data.HapticStrength

/**
 * whether a touch is answered by feel.
 *
 * for unsteady hands this says arrived before anything happens on screen, so a miss is
 * noticed at once rather than at the app that fails to open.
 */
val LocalHaptics = staticCompositionLocalOf { HapticStrength.LIGHT }

fun HapticFeedback.tap(strength: HapticStrength) {
    when (strength) {
        HapticStrength.OFF -> Unit
        HapticStrength.LIGHT -> performHapticFeedback(HapticFeedbackType.TextHandleMove)
        HapticStrength.STRONG -> performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

/** a long press does something else than a tap, so even the light step is distinct. */
fun HapticFeedback.longPress(strength: HapticStrength) {
    if (strength != HapticStrength.OFF) performHapticFeedback(HapticFeedbackType.LongPress)
}

object Haptics {
    fun next(current: HapticStrength): HapticStrength = when (current) {
        HapticStrength.OFF -> HapticStrength.LIGHT
        HapticStrength.LIGHT -> HapticStrength.STRONG
        HapticStrength.STRONG -> HapticStrength.OFF
    }
}
