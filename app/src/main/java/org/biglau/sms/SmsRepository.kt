package org.biglau.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Liest Nachrichten aus der Anbieter-Datenbank. Das geht auch ohne die Standard-SMS-Rolle,
 * solange READ_SMS erteilt ist - schreiben duerfte nur die Standard-App.
 */
class SmsRepository(context: Context) {

    private val appContext = context.applicationContext

    fun hasReadPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    fun isDefaultSmsApp(): Boolean =
        Telephony.Sms.getDefaultSmsPackage(appContext) == appContext.packageName

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
                    )
                }
            }
        }
        messages
    }

    companion object {
        private val _changes = MutableStateFlow(0L)

        /** Zaehler, auf den die Oberflaeche horcht - eine neue Nachricht laedt die Liste neu. */
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
