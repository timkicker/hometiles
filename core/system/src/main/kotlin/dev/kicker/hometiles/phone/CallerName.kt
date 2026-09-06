package dev.kicker.hometiles.phone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract

/** the name behind a number; the network almost never sends `callerDisplayName`. */
object CallerName {

    /** null when nothing matches or the permission is missing: the number stays. */
    fun lookup(context: Context, number: String): String? =
        column(context, number, ContactsContract.PhoneLookup.DISPLAY_NAME)

    /** `PLAN.md` 4.6: a face is recognised where a name is too small. */
    fun photo(context: Context, number: String): String? =
        column(context, number, ContactsContract.PhoneLookup.PHOTO_URI)

    private fun column(context: Context, number: String, column: String): String? {
        if (number.isBlank()) return null
        val allowed = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) return null
        val uri = android.net.Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(number),
        )
        return runCatching {
            context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() } else null
            }
        }.getOrNull()
    }
}
