package org.biglau.tiles

import org.biglau.data.ButtonAction
import org.biglau.data.ContactMode
import org.biglau.phone.PhoneNumbers

/**
 * a tile that starts a message to a fixed number: the pharmacy, the ride service, the
 * neighbour across the road. going through the contact picker would presuppose a contact.
 *
 * it writes the same action a contact in sms mode gets, so the existing compose screen
 * opens. a separate action kind would be a second path to the same place, and the second
 * one never gets the first one's fixes.
 *
 * **nothing is sent here.** the tile opens the compose screen with the recipient filled in.
 */
object MessageTile {

    /** `null` when there is no digit: a tile opening an empty compose screen never does anything. */
    fun actionFor(input: String): ButtonAction.Contact? {
        val number = PhoneNumbers.clean(input)
        if (!PhoneNumbers.isDialable(number)) return null
        return ButtonAction.Contact(
            // the number in blocks is the only thing known about this recipient; "message"
            // alone would look the same on two tiles. renaming is possible in the editor.
            name = PhoneNumbers.forDisplay(number),
            number = number,
            photoUri = null,
            mode = ContactMode.SMS,
        )
    }
}
