package dev.kicker.hometiles.contacts.probe

import android.app.Activity
import android.content.ContentProviderOperation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * a contact with a large photo of fine lines, for issue #14: a tile drawn from the small
 * copy shows the lines as grey mush.
 *
 * debug build only, emulator only. the number is from a range that is never assigned.
 *
 *     adb shell pm grant <pkg> android.permission.WRITE_CONTACTS
 *     adb shell am start -n <pkg>/dev.kicker.hometiles.contacts.probe.ProbeContactActivity
 */
class ProbeContactActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread {
            val result = runCatching { contentResolver.applyBatch(ContactsContract.AUTHORITY, operations()) }
            Log.i(TAG, "probe contact: ${result.exceptionOrNull() ?: "written"}")
            runOnUiThread { finish() }
        }.start()
    }

    private fun operations(): ArrayList<ContentProviderOperation> = arrayListOf(
        ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build(),
        data(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, "Photo Probe")
            .build(),
        data(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, "+447700900125")
            .withValue(
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
            )
            .build(),
        data(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, lines())
            .build(),
    )

    private fun data(type: String) = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
        .withValue(ContactsContract.Data.MIMETYPE, type)

    /** 720 px, black lines two pixels apart: only a sharp copy keeps them apart. */
    private fun lines(): ByteArray {
        val size = 720
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply { color = Color.BLACK; strokeWidth = 2f }
        for (x in 0 until size step 4) canvas.drawLine(x.toFloat(), 0f, x.toFloat(), size / 2f, paint)
        paint.textSize = 64f
        canvas.drawText("HomeTiles 14", 40f, size * 0.75f, paint)
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }.toByteArray()
    }

    private companion object {
        const val TAG = "HomeTilesProbe"
    }
}
