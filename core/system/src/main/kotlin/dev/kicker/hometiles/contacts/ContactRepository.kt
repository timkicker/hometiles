package dev.kicker.hometiles.contacts

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** reads contacts with a phone number; the folding lives in [ContactMerge] so it is testable. */
class ContactRepository(context: Context) {

    private val appContext = context.applicationContext

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /** one `PhoneLookup` hit: this runs in a broadcast receiver, where a full query has no room. */
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

    /**
     * [resources] carries the number labels. they come from the system, not from our texts,
     * so the caller says which language: an activity passes its own, already in the app's.
     */
    suspend fun load(resources: Resources = appContext.resources): List<PhoneContact> =
        withContext(Dispatchers.IO) {
            if (!hasPermission()) return@withContext emptyList()
            ContactMerge.merge(readRows(resources))
        }

    private fun readRows(resources: Resources): List<ContactRow> {
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
                    label = typeLabel(resources, type, customLabel),
                    photoUri = cursor.getString(photoIndex),
                    starred = cursor.getInt(starredIndex) == 1,
                )
            }
        }
        return rows
    }

    /**
     * the label of a number.
     *
     * `getTypeLabel` translates into the language of the passed resources and prefers a
     * custom label; an own mapping covered three types in one hard-wired language.
     */
    private fun typeLabel(resources: Resources, type: Int, custom: String?): String? =
        runCatching {
            ContactsContract.CommonDataKinds.Phone
                .getTypeLabel(resources, type, custom)
                ?.toString()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: custom?.takeIf { it.isNotBlank() }

    fun canWrite(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /** returns whether it worked: the screen must not pretend when the provider refused. */
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
    }

    fun createIntent(): Intent = Intent(Intent.ACTION_INSERT).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE
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
