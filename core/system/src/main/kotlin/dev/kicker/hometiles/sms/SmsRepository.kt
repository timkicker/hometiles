package dev.kicker.hometiles.sms

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** reads messages from the provider; READ_SMS is enough, writing needs the default role. */
class SmsRepository(context: Context) {

    private val appContext = context.applicationContext

    fun hasReadPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    fun isDefaultSmsApp(): Boolean =
        Telephony.Sms.getDefaultSmsPackage(appContext) == appContext.packageName

    /**
     * marks this conversation as read, so the count beside the name stops standing forever
     * and the reminder chain ends.
     *
     * only the default sms app may write; otherwise another app keeps this state and it is
     * not ours. see [SmsDelivery.mayWrite].
     */
    suspend fun markRead(threadId: Long): Boolean = withContext(Dispatchers.IO) {
        if (!isDefaultSmsApp()) return@withContext false
        runCatching {
            appContext.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                ContentValues().apply {
                    put(Telephony.Sms.READ, 1)
                    put(Telephony.Sms.SEEN, 1)
                },
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                arrayOf(threadId.toString()),
            ) > 0
        }.getOrDefault(false)
    }

    /**
     * how many incoming messages are still unread.
     *
     * counted from the provider, not from notices: those need access to foreign notices,
     * and once HomeTiles is the default app the number would hang on its own notice.
     */
    suspend fun unreadCount(): Int = withContext(Dispatchers.IO) {
        if (!hasReadPermission()) return@withContext 0
        runCatching {
            appContext.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms._ID),
                "${Telephony.Sms.READ} = 0",
                null,
                null,
            )?.use { it.count } ?: 0
        }.getOrDefault(0)
    }

    /**
     * every message of both kinds, newest first.
     *
     * sms and mms live in two tables with ids of their own, so the two lists are merged here
     * and not by the screen. the word for a picture comes in ready: a repository is no
     * activity, and reading it from `resources` here would give the phone's language instead
     * of the app's. see AppLanguageTest.
     */
    suspend fun load(pictureWord: String, limit: Int = 500): List<SmsMessage> =
        withContext(Dispatchers.IO) {
            (loadSms(limit) + loadMms(pictureWord, limit))
                .sortedByDescending { it.timestamp }
                .take(limit)
        }

    private fun loadSms(limit: Int): List<SmsMessage> {
        if (!hasReadPermission()) return emptyList()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
        )
        val messages = mutableListOf<SmsMessage>()
        runCatching {
            appContext.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC",
            )?.use { cursor ->
                while (cursor.moveToNext() && messages.size < limit) {
                    messages += SmsMessage(
                        id = cursor.getLong(0),
                        threadId = cursor.getLong(1),
                        address = cursor.getString(2).orEmpty(),
                        body = cursor.getString(3).orEmpty(),
                        timestamp = cursor.getLong(4),
                        incoming = cursor.getInt(5) == Telephony.Sms.MESSAGE_TYPE_INBOX,
                        read = cursor.getInt(6) == 1,
                        failed = cursor.getInt(5) == Telephony.Sms.MESSAGE_TYPE_FAILED,
                    )
                }
            }
        }
        return messages
    }

    /**
     * the picture messages.
     *
     * three things about this table are not visible when reading it: the date counts in
     * seconds while the sms table counts milliseconds; the sender is not in the row but in the
     * `addr` sub-table; and the text sits in parts, see [MmsParts].
     */
    private fun loadMms(pictureWord: String, limit: Int): List<SmsMessage> {
        if (!hasReadPermission()) return emptyList()
        val messages = mutableListOf<SmsMessage>()
        runCatching {
            appContext.contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf(
                    Telephony.Mms._ID,
                    Telephony.Mms.THREAD_ID,
                    Telephony.Mms.DATE,
                    Telephony.Mms.MESSAGE_BOX,
                    Telephony.Mms.READ,
                ),
                null,
                null,
                "${Telephony.Mms.DATE} DESC",
            )?.use { cursor ->
                val rows = mutableListOf<LongArray>()
                while (cursor.moveToNext() && rows.size < limit) {
                    rows += longArrayOf(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getLong(2),
                        cursor.getInt(3).toLong(),
                        cursor.getInt(4).toLong(),
                    )
                }
                // all parts in one query instead of one per message: with a few hundred
                // picture messages that was a few hundred round trips to the provider.
                val partsById = partsFor(rows.map { it[0] })
                rows.forEach { row ->
                    val id = row[0]
                    val parts = partsById[id].orEmpty()
                    messages += SmsMessage(
                        // negative: the screen keys its list by the id, and an mms id may be
                        // the same number as an sms id. two equal keys crash the list.
                        id = -id,
                        threadId = row[1],
                        address = senderOf(id),
                        body = MmsParts.preview(parts, pictureWord),
                        // seconds here, milliseconds in the sms table: unconverted every
                        // picture message stands in january 1970.
                        timestamp = row[2] * 1000L,
                        incoming = row[3].toInt() == Telephony.Mms.MESSAGE_BOX_INBOX,
                        read = row[4].toInt() == 1,
                        // the provider serves the picture itself; loading it here would put
                        // a full sized bitmap in memory for a row that may never be seen.
                        imageUri = MmsParts.imageId(parts)?.let { "content://mms/part/$it" },
                    )
                }
            }
        }
        return messages
    }

    /**
     * the parts of several picture messages, grouped by their message.
     *
     * the ids go into the selection as text and not as arguments: a provider takes a fixed
     * number of `?` places, and the count here is the number of messages. they come from the
     * `_id` column of the same provider, so nothing foreign can get in.
     *
     * the uri is a provider path and stays as it is.
     */
    private fun partsFor(mmsIds: List<Long>): Map<Long, List<MmsParts.Part>> {
        if (mmsIds.isEmpty()) return emptyMap()
        val parts = mutableMapOf<Long, MutableList<MmsParts.Part>>()
        runCatching {
            appContext.contentResolver.query(
                Uri.parse("content://mms/part"),
                arrayOf("_id", "mid", "ct", "text"),
                "mid IN (${mmsIds.joinToString(",")})",
                null,
                null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    parts.getOrPut(cursor.getLong(1)) { mutableListOf() } += MmsParts.Part(
                        contentType = cursor.getString(2).orEmpty(),
                        text = cursor.getString(3),
                        id = cursor.getLong(0),
                    )
                }
            }
        }
        return parts
    }

    /**
     * who sent it.
     *
     * the row itself has no address; the `addr` sub-table holds one per party, and 137 is the
     * sender (`PduHeaders.FROM`, which is not in the public api).
     */
    private fun senderOf(mmsId: Long): String {
        val from = 137
        return runCatching {
            appContext.contentResolver.query(
                Telephony.Mms.CONTENT_URI.buildUpon().appendPath(mmsId.toString()).appendPath("addr").build(),
                arrayOf(Telephony.Mms.Addr.ADDRESS),
                "${Telephony.Mms.Addr.TYPE} = ?",
                arrayOf(from.toString()),
                null,
            )?.use { if (it.moveToFirst()) it.getString(0).orEmpty() else "" }.orEmpty()
        }.getOrDefault("")
    }

    companion object {
        private val _changes = MutableStateFlow(0L)

        /** the screen listens on this: a new message reloads the list. */
        val changes: StateFlow<Long> = _changes.asStateFlow()

        fun notifyChanged() {
            _changes.value = System.currentTimeMillis()
        }

        @Volatile
        private var instance: SmsRepository? = null

        fun get(context: Context): SmsRepository =
            instance ?: synchronized(this) {
                instance ?: SmsRepository(context).also { instance = it }
            }
    }
}
