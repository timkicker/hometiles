package org.biglau.ui

import java.io.File
import org.biglau.data.Appearance
import org.biglau.data.FontChoice
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.2: „Atkinson Hyperlegible (Standard, mitgeliefert) / Systemschrift".
 *
 * Vom Braille Institute genau dafür gezeichnet: die Buchstaben, die sich sonst gleichen,
 * werden auseinandergezogen. Wer schlecht sieht, liest nicht kleiner, sondern rät
 * häufiger; diese Schrift nimmt das Raten weg.
 */
class FontChoiceTest {

    // Vorgabe, nicht Zierde in den Einstellungen. Wer sie braucht, findet sie sonst nicht.
    @Test
    fun `die vorgabe ist die lesbare schrift`() {
        assertEquals(FontChoice.HYPERLEGIBLE, Appearance().font)
    }

    // "Mitgeliefert" heisst mitgeliefert: keine Schrift aus dem Netz, kein Nachladen beim
    // ersten Start, kein leerer Text, wenn gerade kein Netz da ist.
    @Test
    fun `beide schnitte liegen im apk`() {
        val ordner = File("src/main/res/font")
        val dateien = ordner.list()?.toSet().orEmpty()
        assertEquals(true, dateien.contains("atkinson_regular.ttf"))
        assertEquals(true, dateien.contains("atkinson_bold.ttf"))
    }

    // SIL Open Font License 1.1. Eine mitgelieferte Schrift ohne beiliegende Lizenz waere
    // ein Rechtsfehler in einem Programm, das sich GPL nennt.
    @Test
    fun `die lizenz liegt bei`() {
        val lizenz = File("../LICENSE-Atkinson-Hyperlegible.txt")
        assertEquals(true, lizenz.exists())
        assertEquals(true, lizenz.readText().contains("SIL OPEN FONT LICENSE"))
    }

    @Test
    fun `es gibt genau zwei moeglichkeiten`() {
        assertEquals(2, FontChoice.entries.size)
    }
}

/**
 * Keine festen Zeilenhöhen in der Typografie.
 *
 * Material gibt `bodyLarge` 24 sp Zeilenhöhe mit. Diese Zahl bleibt stehen, wenn eine
 * Stelle nur `fontSize` setzt — und das tut diese App an rund neunzig Stellen, weil fast
 * jede Größe aus der Zellgröße gerechnet wird. Bei eingestellter 150-Prozent-Schrift legte
 * sich die zweite Zeile der Überschrift „Beschriftung auf der Kachel" über die erste.
 */
class TypographyLineHeightTest {

    @Test
    fun `kein stil schreibt eine zeilenhoehe vor`() {
        for (choice in org.biglau.data.FontChoice.entries) {
            val typo = org.biglau.ui.theme.typographyFor(choice)
            val stile = listOf(
                "displayLarge" to typo.displayLarge,
                "headlineLarge" to typo.headlineLarge,
                "titleLarge" to typo.titleLarge,
                "bodyLarge" to typo.bodyLarge,
                "bodyMedium" to typo.bodyMedium,
                "labelLarge" to typo.labelLarge,
                "labelSmall" to typo.labelSmall,
            )
            val fest = stile.filter { it.second.lineHeight != androidx.compose.ui.unit.TextUnit.Unspecified }
            assertEquals(emptyList<String>(), fest.map { "$choice/${it.first}" })
        }
    }
}
