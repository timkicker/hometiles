package org.biglau.sms

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
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
     * and once BigLau is the default app the number would hang on its own notice.
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

    suspend fun load(limit: Int = 500): List<SmsMessage> = withContext(Dispatchers.IO) {
        if (!hasReadPermission()) return@withContext emptyList()
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
        messages
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
