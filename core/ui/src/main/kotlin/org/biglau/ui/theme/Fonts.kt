package org.biglau.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import org.biglau.core.ui.R
import org.biglau.data.FontChoice

/**
 * Atkinson Hyperlegible, mitgeliefert. PLAN.md 4.2.
 *
 * Vom Braille Institute genau dafuer gezeichnet: die Buchstaben, die sich sonst gleichen,
 * werden auseinandergezogen - grosses I, kleines l und Eins; O und Null; b, d, p und q.
 * Wer schlecht sieht, liest nicht kleiner, sondern raet haeufiger; diese Schrift nimmt das
 * Raten weg. Deshalb ist sie die Vorgabe und nicht eine Zierde in den Einstellungen.
 *
 * Lizenz: SIL Open Font License 1.1, siehe LICENSE-Atkinson-Hyperlegible.txt.
 */
val Hyperlegible = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

/** null heisst: die Schrift des Telefons, wie sie der Nutzer dort eingestellt hat. */
fun familyFor(choice: FontChoice): FontFamily? = when (choice) {
    FontChoice.HYPERLEGIBLE -> Hyperlegible
    FontChoice.SYSTEM -> null
}

/**
 * Setzt die Schrift auf alle Textstile durch. Einzelne Stile auszulassen hiesse, dass ein
 * Teil der App in der einen und ein Teil in der anderen Schrift steht - und ein Wechsel
 * mitten auf der Seite liest sich wie ein Fehler.
 *
 * Und: **keine festen Zeilenhoehen**. Material gibt bodyLarge 24 sp mit, und diese Zahl
 * bleibt stehen, wenn eine Stelle nur `fontSize` setzt - was in dieser App an 94 Stellen
 * passiert, weil hier fast jede Groesse aus der Zellgroesse gerechnet wird. Bei
 * eingestellter 150-Prozent-Schrift legte sich die zweite Zeile einer Ueberschrift ueber
 * die erste. `Unspecified` heisst: die Zeilenhoehe der Schrift selbst, und die waechst mit.
 */
fun typographyFor(choice: FontChoice): Typography {
    val family = familyFor(choice)
    val base = Typography()
    fun stil(vorlage: TextStyle) =
        vorlage.copy(fontFamily = family ?: vorlage.fontFamily, lineHeight = TextUnit.Unspecified)
    return Typography(
        displayLarge = stil(base.displayLarge),
        displayMedium = stil(base.displayMedium),
        displaySmall = stil(base.displaySmall),
        headlineLarge = stil(base.headlineLarge),
        headlineMedium = stil(base.headlineMedium),
        headlineSmall = stil(base.headlineSmall),
        titleLarge = stil(base.titleLarge),
        titleMedium = stil(base.titleMedium),
        titleSmall = stil(base.titleSmall),
        bodyLarge = stil(base.bodyLarge),
        bodyMedium = stil(base.bodyMedium),
        bodySmall = stil(base.bodySmall),
        labelLarge = stil(base.labelLarge),
        labelMedium = stil(base.labelMedium),
        labelSmall = stil(base.labelSmall),
    )
}
