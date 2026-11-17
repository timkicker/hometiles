package dev.kicker.hometiles.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactPhotoUriTest {

    @Test
    fun `the thumbnail points to the large photo of the same contact`() {
        assertEquals(
            "content://com.android.contacts/contacts/42/display_photo",
            ContactPhotoUri.large("content://com.android.contacts/contacts/42/photo"),
        )
    }

    @Test
    fun `anything else stays as it is`() {
        assertNull(ContactPhotoUri.large(null))
        assertNull(ContactPhotoUri.large("content://com.android.contacts/display_photo/7"))
        assertNull(ContactPhotoUri.large("content://com.android.contacts/contacts/42/photo/extra"))
        assertNull(ContactPhotoUri.large("content://media/external/images/media/42"))
    }
}
