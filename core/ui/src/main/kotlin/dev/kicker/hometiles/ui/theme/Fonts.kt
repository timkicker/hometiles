package dev.kicker.hometiles.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import dev.kicker.hometiles.core.ui.R
import dev.kicker.hometiles.data.FontChoice

/**
 * Atkinson Hyperlegible, shipped with the app. `PLAN.md` 4.2.
 *
 * drawn by the braille institute to pull apart the letters that otherwise look alike: I, l
 * and 1; O and 0; b, d, p and q. poor sight guesses more often, and this takes the guessing
 * away, which is why it is the default and not an ornament in the settings.
 *
 * licence: SIL Open Font License 1.1, see LICENSE-Atkinson-Hyperlegible.txt.
 */
val Hyperlegible = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

/** null means the phone's own font. */
fun familyFor(choice: FontChoice): FontFamily? = when (choice) {
    FontChoice.HYPERLEGIBLE -> Hyperlegible
    FontChoice.SYSTEM -> null
}

/**
 * puts the font on every text style; a change of face mid-page reads like a fault.
 *
 * and no fixed line heights: material carries 24 sp on bodyLarge, and that number stays
 * wherever a place sets only `fontSize`, which happens in 94 places here because almost
 * every size is computed from the cell. at 150 percent font scale the second line of a
 * heading lay on the first. `Unspecified` takes the font's own line height, which grows.
 */
fun typographyFor(choice: FontChoice): Typography {
    val family = familyFor(choice)
    val base = Typography()
    fun style(template: TextStyle) =
        template.copy(fontFamily = family ?: template.fontFamily, lineHeight = TextUnit.Unspecified)
    return Typography(
        displayLarge = style(base.displayLarge),
        displayMedium = style(base.displayMedium),
        displaySmall = style(base.displaySmall),
        headlineLarge = style(base.headlineLarge),
        headlineMedium = style(base.headlineMedium),
        headlineSmall = style(base.headlineSmall),
        titleLarge = style(base.titleLarge),
        titleMedium = style(base.titleMedium),
        titleSmall = style(base.titleSmall),
        bodyLarge = style(base.bodyLarge),
        bodyMedium = style(base.bodyMedium),
        bodySmall = style(base.bodySmall),
        labelLarge = style(base.labelLarge),
        labelMedium = style(base.labelMedium),
        labelSmall = style(base.labelSmall),
    )
}
