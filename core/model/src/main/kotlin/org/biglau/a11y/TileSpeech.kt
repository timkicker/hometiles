package org.biglau.a11y

/**
 * Was eine Kachel dem Screenreader sagt.
 *
 * `PLAN.md` 3.6 gibt das Muster vor: „*Label* (App). 3 neue Nachrichten." Gesagt wurde
 * bisher nur das Label. Alles, was die Kachel darüber hinaus zeigt - der Zähler an der
 * Ecke, die Empfangsbalken, der Ladestand -, ist gezeichnet und trägt keinen Text; wer
 * nicht hinsieht, erfuhr davon nichts. Eine Anzeige, die nur für Sehende da ist, ist auf
 * einem Gerät für schwache Augen die falsche Hälfte.
 */
object TileSpeech {

    /**
     * Setzt Label, Zustand und Zähler zu einem Satz zusammen.
     *
     * Leere Teile fallen weg, statt eine Lücke oder einen doppelten Punkt zu hinterlassen.
     * Getrennt wird mit Punkt und Leerzeichen: der Screenreader macht dort eine Pause, und
     * ohne sie liefe „Empfang3 von 4" ineinander.
     */
    fun describe(label: String, state: String? = null, badge: String? = null): String =
        listOfNotNull(label, state, badge)
            .map { it.trim().trimEnd('.') }
            .filter { it.isNotEmpty() }
            .joinToString(". ")
}
