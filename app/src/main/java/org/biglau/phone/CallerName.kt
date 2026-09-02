package org.biglau.phone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract

/**
 * Wer da anruft - der Name statt der Nummer.
 *
 * Auf dem Anrufbildschirm stand bisher nur, was das Netz mitschickt
 * (`callerDisplayName`), und das ist fast immer nichts. Wer eine Nummer nicht auswendig
 * kennt - und wer kennt heute noch Nummern auswendig -, sah beim Klingeln eine
 * Ziffernfolge und musste raten, ob er drangehen will. „Oma" statt „+436 641 234 5" ist
 * auf diesem Bildschirm der ganze Unterschied.
 */
object CallerName {

    /**
     * Sucht die Nummer im Adressbuch. Gibt null zurueck, wenn nichts passt oder die
     * Berechtigung fehlt - dann bleibt es bei der Nummer, was immer noch besser ist als
     * ein leerer Bildschirm.
     */
    fun lookup(context: Context, number: String): String? =
        spalte(context, number, ContactsContract.PhoneLookup.DISPLAY_NAME)

    /**
     * Das Foto zu dieser Nummer, oder null.
     *
     * `PLAN.md` 4.6 sieht es vor, und auf diesem Bildschirm ist es mehr als Schmuck: ein
     * Gesicht erkennt man auch dann noch, wenn der Name zu klein oder die Brille im
     * anderen Zimmer ist.
     */
    fun photo(context: Context, number: String): String? =
        spalte(context, number, ContactsContract.PhoneLookup.PHOTO_URI)

    private fun spalte(context: Context, number: String, spalte: String): String? {
        if (number.isBlank()) return null
        val erlaubt = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (!erlaubt) return null
        val uri = android.net.Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(number),
        )
        return runCatching {
            context.contentResolver.query(uri, arrayOf(spalte), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() } else null
            }
        }.getOrNull()
    }
}
