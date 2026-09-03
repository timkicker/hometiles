package org.biglau.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.biglau.data.HapticStrength

/**
 * Ob eine Berührung spürbar quittiert wird.
 *
 * Für unsichere Hände ist das mehr als Zierrat: eine Kachel, die sich beim Treffen meldet,
 * sagt „angekommen", bevor irgendetwas auf dem Bildschirm passiert. Wer daneben tippt,
 * merkt es sofort statt erst an der App, die nicht aufgeht.
 *
 * Die Einstellung stand seit dem ersten Tag im Modell und wurde nirgends gelesen.
 */
val LocalHaptics = staticCompositionLocalOf { HapticStrength.LIGHT }

/** Quittung beim Antippen. */
fun HapticFeedback.tap(strength: HapticStrength) {
    when (strength) {
        HapticStrength.OFF -> Unit
        HapticStrength.LIGHT -> performHapticFeedback(HapticFeedbackType.TextHandleMove)
        HapticStrength.STRONG -> performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

/**
 * Quittung beim langen Druck. Hier passiert gleich etwas anderes als beim Tippen, deshalb
 * ist auch die leichte Stufe deutlich - sonst bliebe der Unterschied ungemeldet.
 */
fun HapticFeedback.longPress(strength: HapticStrength) {
    if (strength != HapticStrength.OFF) performHapticFeedback(HapticFeedbackType.LongPress)
}

/** Die eine Einstellung, drei Stufen: aus, leicht, kräftig, und wieder von vorn. */
object Haptics {
    fun next(current: HapticStrength): HapticStrength = when (current) {
        HapticStrength.OFF -> HapticStrength.LIGHT
        HapticStrength.LIGHT -> HapticStrength.STRONG
        HapticStrength.STRONG -> HapticStrength.OFF
    }
}
