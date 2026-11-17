package dev.kicker.hometiles.contacts

/**
 * the large copy of a contact photo.
 *
 * the contact list hands out the thumbnail, which android keeps at 96 px. stretched over a
 * tile it came out blurred (issue #14). tiles store the address they were made with, so the
 * large one is worked out here at drawing time and old tiles get sharp too. a contact with
 * only a small photo has no large one: then the caller falls back to the given address.
 */
object ContactPhotoUri {

    private val thumbnail = Regex("""^content://com\.android\.contacts/contacts/(\d+)/photo$""")

    /** null where nothing larger is known. */
    fun large(uri: String?): String? {
        val id = uri?.let { thumbnail.matchEntire(it) }?.groupValues?.get(1) ?: return null
        return "content://com.android.contacts/contacts/$id/display_photo"
    }
}
