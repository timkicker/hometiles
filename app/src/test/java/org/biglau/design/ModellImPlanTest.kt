package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Modell-Entwurf in `PLAN.md` 2.2 nennt die Felder, die es wirklich gibt.
 *
 * Am 3.9.2026 nachgezählt — vier Klassen, acht Abweichungen:
 *
 * * `LauncherConfig`: vier Felder fehlten (`swipeExcluded`, `apps`, `contacts`,
 *   `wizardDone`).
 * * `Screen`: der Plan hatte ein `icon`, das es nie gab; das echte `kind` (`SCREEN` oder
 *   `FOLDER`) fehlte — **der Plan konnte Ordner gar nicht ausdrücken**, obwohl es sie gibt.
 * * `Button`: `icon`/`color` im Plan gegen `iconName`/`colorIndex`/`colorHue` im Code — der
 *   frei wählbare Farbton aus 3.3 kam im Modell nicht vor.
 *
 * Wer den Abschnitt liest, um das Datenmodell zu verstehen, liest sonst eines, das seit
 * Wochen nicht mehr stimmt — und ein Modell ist das Erste, was man liest.
 */
class ModellImPlanTest {

    private val plan = File("../PLAN.md").readText()
    private val modell = Quelltext.datei("org/biglau/data/Model.kt").readText()

    private fun felder(text: String, klasse: String): List<String> {
        val block = Regex("""data class $klasse\((.*?)\n\)""", RegexOption.DOT_MATCHES_ALL)
            .find(text)
            ?: throw AssertionError("`data class $klasse` nicht gefunden")
        return Regex("""val (\w+):""").findAll(block.groupValues[1]).map { it.groupValues[1] }.toList()
    }

    @Test
    fun `die vier Kernklassen nennen dieselben Felder`() {
        listOf("LauncherConfig", "Screen", "Cell", "Button").forEach { klasse ->
            assertEquals(
                "PLAN.md 2.2 beschreibt $klasse anders, als es gebaut ist. Wer das Modell " +
                    "dort nachliest, liest ein falsches.",
                felder(modell, klasse),
                felder(plan, klasse),
            )
        }
    }

    /**
     * Und `Background` kennt kein Bild — der Plan sagt das inzwischen selbst.
     *
     * Der erste Entwurf hatte `Image(uri, scale)`. Daran hingen zwei Dinge, die es bis heute
     * gab: eine Zeile „Hintergrundbilder herunterskalieren" bei den Fallstricken und die
     * Berechtigung `SET_WALLPAPER` im Manifest.
     */
    @Test
    fun `der Plan verspricht keine Hintergrundbilder mehr`() {
        assertTrue(
            "Im Modell gibt es Background.Image - dann darf der Plan es auch nennen.",
            "Image(" !in modell.substringAfter("interface Background").take(400),
        )
        // Nur im **Codeblock**, nicht in der Prosa: der Absatz darunter erklaert, dass der
        // erste Entwurf `Image(uri, scale)` vorsah und was daran hing. Eine Regel, die auch
        // das verbietet, zwingt den Plan, seine eigene Geschichte zu verschweigen - derselbe
        // Fehler, den `VerweiseTest` heute schon einmal gemacht hat.
        val block = plan.substringAfter("### 2.2").substringAfter("```kotlin").substringBefore("```")
        assertTrue(
            "Der Modell-Entwurf in PLAN.md 2.2 nennt wieder ein Hintergrundbild, das es " +
                "nicht gibt.",
            "Image(" !in block,
        )
    }
}
