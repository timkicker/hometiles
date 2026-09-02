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

    /**
     * Merkt sich, dass diese Unterhaltung gelesen wurde.
     *
     * Ohne das bleibt die Zahl neben dem Namen für immer stehen: „Oma (3)" auch dann noch,
     * wenn man alle drei gelesen hat. Am Emulator gesehen - eine geöffnete Unterhaltung
     * zählte weiter als ungelesen, und damit wäre auch jede Erinnerung an ungelesene
     * Nachrichten eine, die nie aufhört.
     *
     * Schreiben darf nur die Standard-SMS-App. Ist BigLau es nicht, führt eine andere App
     * diesen Zustand, und dann ist er nicht unserer - siehe [SmsDelivery.mayWrite].
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
