package org.biglau.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Ob eine Berührung spürbar quittiert wird.
 *
 * Für unsichere Hände ist das mehr als Zierrat: eine Kachel, die sich beim Treffen meldet,
 * sagt „angekommen", bevor irgendetwas auf dem Bildschirm passiert. Wer daneben tippt,
 * merkt es sofort statt erst an der App, die nicht aufgeht.
 *
 * Die Einstellung stand seit dem ersten Tag im Modell und wurde nirgends gelesen.
 */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/** Kurzer Stups beim Antippen - nur, wenn eingeschaltet. */
fun HapticFeedback.tap(enabled: Boolean) {
    if (enabled) performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

/** Deutlicher Stups beim langen Druck: hier passiert gleich etwas anderes als beim Tippen. */
fun HapticFeedback.longPress(enabled: Boolean) {
    if (enabled) performHapticFeedback(HapticFeedbackType.LongPress)
}
