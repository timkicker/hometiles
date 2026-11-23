package dev.kicker.hometiles.phone

import dev.kicker.hometiles.contacts.ContactMerge
import dev.kicker.hometiles.contacts.PhoneContact

/** where the contact list stands next to the keypad, if anywhere. */
enum class KeypadSide { NONE, BESIDE, ABOVE }

/**
 * the contacts beside the keypad (issue #23).
 *
 * on a tablet the keypad alone leaves half the screen empty, and the way to a person ran
 * through a second tile. on a phone there is no room for both: the keys would shrink below
 * what a shaky finger hits, so there it stays the keypad alone, whatever the switch says.
 */
object KeypadContacts {

    /** android's own line for a tablet, the smaller side in dp. */
    const val MIN_SIDE_DP = 600

    /** fewer typed digits match half the phone book and only push the list around. */
    const val MIN_DIGITS = 3

    fun side(on: Boolean, widthDp: Int, heightDp: Int): KeypadSide = when {
        !on || minOf(widthDp, heightDp) < MIN_SIDE_DP -> KeypadSide.NONE
        widthDp > heightDp -> KeypadSide.BESIDE
        else -> KeypadSide.ABOVE
    }

    data class Entry(
        val contactId: Long,
        val name: String,
        val number: String,
        val label: String?,
        val photoUri: String?,
    )

    /**
     * one row per number, narrowed by what was typed.
     *
     * a typed leading zero is the national way of writing what the phone book may hold with
     * a country code, so it is matched without it too.
     */
    fun entries(contacts: List<PhoneContact>, typed: String): List<Entry> {
        val all = contacts.flatMap { contact ->
            contact.numbers.map { Entry(contact.id, contact.name, it.number, it.label, contact.photoUri) }
        }
        val digits = typed.filter { it.isDigit() }
        if (digits.length < MIN_DIGITS) return all
        val national = digits.trimStart('0')
        return all.filter { entry ->
            val stored = ContactMerge.normalizeNumber(entry.number).filter { it.isDigit() }
            stored.contains(digits) || (national.length >= MIN_DIGITS && national != digits && stored.contains(national))
        }
    }
}
