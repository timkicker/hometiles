package org.biglau.ui

import org.biglau.data.ThemeName

/**
 * Jedes Thema in beiden Zuständen des Systems.
 *
 * `ThemeName.SYSTEM` ist keine eigene Palette, sondern eine Frage ans Telefon. Es nur
 * einmal zu prüfen hieße, die Hälfte davon nie anzusehen — und zwar genau die Hälfte, die
 * auf dem Gerät eines Menschen eintritt, dessen Telefon gerade anders steht als meines.
 */
fun themenUndSystem(): List<Pair<ThemeName, Boolean>> =
    ThemeName.entries.flatMap { thema -> listOf(thema to true, thema to false) }
