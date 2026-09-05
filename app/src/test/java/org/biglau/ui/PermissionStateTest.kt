package org.biglau.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * when a button asking for a permission can no longer do anything.
 *
 * android stops putting the question after the second refusal. a button still promising
 * access is pressed and nothing happens - the same kind of dead end as a hidden app with no
 * way back. from there the way leads only through the system settings, and that is what must
 * stand there.
 */
class PermissionStateTest {

    @Test
    fun `before the first question nothing is blocked`() {
        assertFalse(PermissionState.blocked(deniedOnce = false, canAskAgain = true))
        // even when the system already reports "do not ask again" but nothing was ever asked:
        // then nothing was refused in this session and the button may try.
        assertFalse(PermissionState.blocked(deniedOnce = false, canAskAgain = false))
    }

    @Test
    fun `a single refusal does not block yet`() {
        assertFalse(PermissionState.blocked(deniedOnce = true, canAskAgain = true))
    }

    @Test
    fun `refused and no more questions means blocked`() {
        assertTrue(PermissionState.blocked(deniedOnce = true, canAskAgain = false))
    }
}
