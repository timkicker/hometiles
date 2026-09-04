package org.biglau.a11y

/** Was in der Liste der Menuetaste steht. PLAN.md 10.3.4. */
enum class Menuepunkt { ZWEITE_AKTION, BEARBEITEN, VORLESEN, GROSS_ZEIGEN }

/**
 * Die kleine Liste, die die linke Softkey-Taste zu der Kachel unter dem Fokus oeffnet.
 *
 * Der lange Druck kann nur eines von dreien: bearbeiten, vorlesen oder die Beschriftung gross
 * zeigen. Welches, entscheiden die Einstellungen ([LongPress.decide]). Am Finger geht das,
 * weil man die Einstellung findet und umstellt.
 *
 * Mit Tasten geht es nicht. Wer das Vorlesen eingeschaltet hat, kommt ueber den langen Druck
 * nie mehr in den Editor, und in den Einstellungen steht die Zeile, die das aendern wuerde,
 * hinter genau diesem Editor. Deshalb zeigt die Liste **alle drei**, unabhaengig davon, was
 * eingestellt ist. Das sind mehr Tastendruecke als ein langer Druck und dafuer ist es
 * sichtbar; ein Tastentelefon hat nichts, was eine verborgene Geste ankuendigt.
 *
 * Die Zweitbelegung steht oben, wenn es sie gibt: sie ist eine Entscheidung fuer genau diese
 * Kachel und geht den allgemeinen Punkten vor, wie schon in [LongPress.decide].
 */
object Kachelmenue {

    fun punkte(hasSecondAction: Boolean = false): List<Menuepunkt> = buildList {
        if (hasSecondAction) add(Menuepunkt.ZWEITE_AKTION)
        add(Menuepunkt.BEARBEITEN)
        add(Menuepunkt.VORLESEN)
        add(Menuepunkt.GROSS_ZEIGEN)
    }
}
