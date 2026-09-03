package org.biglau.sms

/**
 * Die Schriftgröße im Gespräch (`PLAN.md` 4.7).
 *
 * Ausdrücklich getrennt von der globalen: eine Nachricht liest man am Stück und aus der
 * Hand, eine Kachel erkennt man im Vorbeigehen. Wer die Kacheln groß mag, braucht deshalb
 * nicht auch große Nachrichten — und umgekehrt.
 */
object ConversationText {

    val CHOICES = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    /**
     * Begrenzt den Wert. Nötig, weil eine Sicherung aus einer späteren Fassung eine Zahl
     * mitbringen kann, die es hier nicht zur Auswahl gibt — und eine Schrift mit Faktor 12
     * ließe von der Nachricht einen Buchstaben übrig.
     */
    fun scale(value: Float): Float = value.coerceIn(CHOICES.first(), CHOICES.last())
}
