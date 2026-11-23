package dev.kicker.hometiles.contacts

import android.provider.ContactsContract.CommonDataKinds.Phone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** issue #22: which numbers lose the message row once the switch is on. */
class NumberKindTest {

    @Test
    fun `home work and fax take no texts`() {
        listOf(Phone.TYPE_HOME, Phone.TYPE_WORK, Phone.TYPE_FAX_HOME, Phone.TYPE_FAX_WORK)
            .forEach { assertFalse("$it", NumberKind.takesTexts(it)) }
    }

    @Test
    fun `mobile and everything unclear keep them`() {
        // a missing row is worse than a needless one: only a kind that says landline counts.
        listOf(null, Phone.TYPE_MOBILE, Phone.TYPE_WORK_MOBILE, Phone.TYPE_MMS, Phone.TYPE_OTHER, Phone.TYPE_CUSTOM, Phone.TYPE_MAIN)
            .forEach { assertTrue("$it", NumberKind.takesTexts(it)) }
    }

    @Test
    fun `the kind survives the merge`() {
        val merged = ContactMerge.merge(
            listOf(
                ContactRow(1, "Anna", "+447700900123", "Mobile", null, type = Phone.TYPE_MOBILE),
                ContactRow(1, "Anna", "+447700900124", "Home", null, type = Phone.TYPE_HOME),
            ),
        ).single()
        assertEquals(listOf(true, false), merged.numbers.map { it.takesTexts })
    }
}
