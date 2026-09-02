package org.biglau.contacts

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Liest Kontakte mit Telefonnummer. Die Zusammenfuehrung liegt bewusst in [ContactMerge],
 * damit sie ohne Geraet pruefbar ist - hier bleibt nur das Cursor-Lesen.
 */
class ContactRepository(context: Context) {

    private val appContext = context.applicationContext

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Der Name zu einer Nummer, oder null.
     *
     * Ein einzelner Treffer aus `PhoneLookup` statt der ganzen Kontaktliste: das hier läuft
     * in einem Broadcast-Empfänger, wenn eine Nachricht ankommt, und dort ist kein Platz
     * für eine Abfrage über alle Kontakte. Ohne Berechtigung oder ohne Treffer steht in der
     * Meldung eben die Nummer.
     */
    fun nameFor(number: String): String? {
        if (!hasPermission() || number.isBlank()) return null
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number),
        )
        return runCatching {
            appContext.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    suspend fun load(): List<PhoneContact> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        ContactMerge.merge(readRows())
    }

    private fun readRows(): List<ContactRow> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.LABEL,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED,
        )
        val rows = mutableListOf<ContactRow>()
        appContext.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(projection[0])
            val nameIndex = cursor.getColumnIndexOrThrow(projection[1])
            val numberIndex = cursor.getColumnIndexOrThrow(projection[2])
            val labelIndex = cursor.getColumnIndexOrThrow(projection[3])
            val typeIndex = cursor.getColumnIndexOrThrow(projection[4])
            val photoIndex = cursor.getColumnIndexOrThrow(projection[5])
            val starredIndex = cursor.getColumnIndexOrThrow(projection[6])

            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIndex) ?: continue
                val customLabel = cursor.getString(labelIndex)
                val type = cursor.getInt(typeIndex)
                rows += ContactRow(
                    contactId = cursor.getLong(idIndex),
                    name = cursor.getString(nameIndex).orEmpty(),
                    number = number,
                    label = customLabel ?: typeLabel(type),
                    photoUri = cursor.getString(photoIndex),
                    starred = cursor.getInt(starredIndex) == 1,
                )
            }
        }
        return rows
    }

    private fun typeLabel(type: Int): String? = when (type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobil"
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Privat"
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Arbeit"
        else -> null
    }

    fun canWrite(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Favoritenkennzeichen setzen. Gibt zurueck, ob es geklappt hat - die Oberflaeche soll
     * nicht so tun, als waere etwas passiert, wenn der Anbieter es abgelehnt hat.
     */
    suspend fun setStarred(contactId: Long, starred: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!canWrite()) return@withContext false
        runCatching {
            val values = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
            }
            val rows = appContext.contentResolver.update(
                ContactsContract.Contacts.CONTENT_URI,
                values,
                "${ContactsContract.Contacts._ID} = ?",
                arrayOf(contactId.toString()),
            )
            rows > 0
        }.getOrDefault(false)
    }

    fun editIntent(contactId: Long): Intent = Intent(Intent.ACTION_EDIT).apply {
        setDataAndType(
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId),
            ContactsContract.Contacts.CONTENT_ITEM_TYPE,
        )
        putExtra("finishActivityOnSaveCompleted", true)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun createIntent(): Intent = Intent(Intent.ACTION_INSERT).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    companion object {
        @Volatile
        private var instance: ContactRepository? = null

        fun get(context: Context): ContactRepository =
            instance ?: synchronized(this) {
                instance ?: ContactRepository(context).also { instance = it }
            }
    }
}
