package org.biglau.tiles

import org.biglau.data.ButtonAction
import org.biglau.data.ContactMode
import org.biglau.phone.PhoneNumbers

/**
 * Eine Kachel, die eine neue Nachricht an eine feste Nummer beginnt.
 *
 * `PLAN.md` 4.3 sagt sie unter „Nachrichten" zu, und sie war der letzte offene Punkt dieser
 * Liste. Gebraucht wird sie fuer Nummern, die in keinem Adressbuch stehen - die Nummer der
 * Apotheke, des Fahrdienstes, der Nachbarin von gegenueber -, denn der Weg ueber die
 * Kontaktauswahl setzt einen Kontakt voraus.
 *
 * Geschrieben wird bewusst dieselbe Aktion wie bei einem Kontakt im SMS-Betrieb: der
 * Startbildschirm oeffnet damit den Schreiben-Bildschirm, den es laengst gibt. Eine eigene
 * Aktionsart daneben waere ein zweiter Weg zum selben Ziel - und der zweite bekommt
 * erfahrungsgemaess die Fehlerbehebungen des ersten nicht mit.
 *
 * **Geschrieben wird hier nichts.** Die Kachel oeffnet den Schreiben-Bildschirm mit
 * eingetragenem Empfaenger; abgeschickt wird erst, wenn jemand auf Senden tippt.
 */
object MessageTile {

    /**
     * Die Aktion zur Eingabe, oder `null`, wenn darin keine Ziffer steht.
     *
     * Ohne Ziffer entstuende eine Kachel, die einen leeren Schreiben-Bildschirm oeffnet -
     * eine Kachel, die nie etwas tut, ist schlechter als gar keine.
     */
    fun actionFor(eingabe: String): ButtonAction.Contact? {
        val nummer = PhoneNumbers.clean(eingabe)
        if (!PhoneNumbers.isDialable(nummer)) return null
        return ButtonAction.Contact(
            // Als Name steht die Nummer in Bloecken da. Sie ist das Einzige, was ueber
            // diesen Empfaenger bekannt ist; "Nachricht" allein saehe auf zwei Kacheln
            // gleich aus. Umbenennen geht im Editor.
            name = PhoneNumbers.forDisplay(nummer),
            number = nummer,
            photoUri = null,
            mode = ContactMode.SMS,
        )
    }
}
