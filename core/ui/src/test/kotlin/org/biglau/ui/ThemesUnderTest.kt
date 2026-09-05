package org.biglau.ui

import org.biglau.data.ThemeName

/**
 * every theme in both states of the system.
 *
 * `ThemeName.SYSTEM` is not a palette of its own but a question to the phone. checking it
 * once would mean never looking at half of it - the half that happens on the device of
 * someone whose phone stands differently than mine.
 */
fun themesAndSystem(): List<Pair<ThemeName, Boolean>> =
    ThemeName.entries.flatMap { theme -> listOf(theme to true, theme to false) }
