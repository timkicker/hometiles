package org.biglau.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wer die Lupentaste nennt, muss sie auch hoeren.
 *
 * `search_matches` sagt bei genau einem Treffer: "die Lupentaste oeffnet ihn". Gehoert hat
 * das Feld bis zum 04.09.2026 nur die Eingabetaste, ueber `ImeAction.Search`. Am Emulator
 * gemessen: nach `camer` steht `1 Treffer, die Lupentaste oeffnet ihn` da, `KEYCODE_SEARCH`
 * tut nichts, und `KEYCODE_ENTER` startet die Kamera.
 *
 * Auf einem Telefon mit Tastatur ist das kein Schoenheitsfehler. Die Lupentaste gibt es dort
 * wirklich, und ein Text, der auf eine Taste zeigt, die nichts tut, ist schlimmer als gar
 * kein Hinweis: er laesst den Nutzer glauben, er habe etwas falsch gemacht.
 *
 * Die Eingabetaste bleibt, sie ist auf einer Bildschirmtastatur der Weg.
 */
class LupentasteTest {

    private val quelle = File("src/main/kotlin/org/biglau/ui/BigSearchField.kt").readText()

    @Test
    fun `das Suchfeld hoert die Lupentaste`() {
        assertTrue(
            "BigSearchField reagiert nicht auf Key.Search. Der Text search_matches nennt " +
                "die Lupentaste; auf einem Tastentelefon zeigt er damit auf eine Taste, die " +
                "nichts tut.",
            "Key.Search" in quelle,
        )
    }

    @Test
    fun `die Eingabetaste bleibt auch`() {
        assertTrue(
            "ImeAction.Search ist weg. Auf einer Bildschirmtastatur gibt es keine Lupentaste.",
            "ImeAction.Search" in quelle && "onSearch" in quelle,
        )
    }
}

/**
 * Aus dem Suchfeld kommt man mit dem Steuerkreuz auch wieder heraus.
 *
 * Am 04.09.2026 in vier Listen gemessen, am Emulator und am Jelly 2. App-Liste und Kontakte
 * haben ein Suchfeld am Kopf; dort blieb der Fokus darin stehen, vier Druecke nach unten ohne
 * jede Bewegung. Nachrichten und Anrufliste haben keins und laufen ohne Zutun sauber durch
 * ihre Eintraege. Es sind also nicht die Listen, es ist das Feld.
 *
 * Ein `EditText` nimmt den Fokus und gibt ihn nicht weiter. Fuer den Finger faellt das nicht
 * auf, denn der tippt einfach den Eintrag an. Mit Tasten ist die ganze Liste unerreichbar,
 * und es blieb nur der gebaute Weg ueber Tippen und Lupentaste. Das ist die Frage aus
 * PLAN.md 10.3.6 an einem konkreten Ort: nicht "gross genug", sondern **erreichbar**.
 *
 * Nach unten ist die einzige Richtung, die hier etwas heisst. Das Feld ist einzeilig, ein
 * Druck nach unten kann darin keinen Text erreichen, also gehoert er der Liste.
 */
class SuchfeldVerlassenTest {

    private val quelle = File("src/main/kotlin/org/biglau/ui/BigSearchField.kt").readText()

    @Test
    fun `nach unten gibt das Feld den Fokus weiter`() {
        assertTrue(
            "BigSearchField behandelt Key.DirectionDown nicht. Dann sitzt der Fokus im Feld " +
                "fest und die Liste darunter ist mit Tasten unerreichbar.",
            "Key.DirectionDown" in quelle,
        )
        assertTrue(
            "Der Fokus wird nicht weitergereicht. Die Taste zu verbrauchen reicht nicht, " +
                "er muss auch irgendwo ankommen.",
            "FocusDirection.Down" in quelle,
        )
    }
}
