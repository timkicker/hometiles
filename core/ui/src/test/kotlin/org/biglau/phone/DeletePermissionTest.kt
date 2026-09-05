package org.biglau.phone

import org.junit.Assert.assertEquals
import org.junit.Test

class DeletePermissionTest {

    @Test
    fun `with the right it deletes`() {
        assertEquals(
            DeleteStep.DELETE,
            DeletePermission.next(canWrite = true, deniedOnce = false, canAskAgain = true),
        )
    }

    @Test
    fun `without the right it asks first`() {
        assertEquals(
            DeleteStep.ASK,
            DeletePermission.next(canWrite = false, deniedOnce = false, canAskAgain = true),
        )
    }

    @Test
    fun `after one refusal it asks once more`() {
        // the first refusal is no dead end - android puts the question again.
        assertEquals(
            DeleteStep.ASK,
            DeletePermission.next(canWrite = false, deniedOnce = true, canAskAgain = true),
        )
    }

    @Test
    fun `when android stops asking the way leads to the settings`() {
        assertEquals(
            DeleteStep.GATE,
            DeletePermission.next(canWrite = false, deniedOnce = true, canAskAgain = false),
        )
    }

    @Test
    fun `the right beats every refusal`() {
        // whoever grants the right in the system settings must not stay stuck on the gate
        // page just because they refused twice before.
        assertEquals(
            DeleteStep.DELETE,
            DeletePermission.next(canWrite = true, deniedOnce = true, canAskAgain = false),
        )
    }
}
