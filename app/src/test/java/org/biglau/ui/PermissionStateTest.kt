package org.biglau.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wann ein Knopf, der um eine Berechtigung bittet, nichts mehr ausrichtet.
 *
 * Android stellt die Frage nach der zweiten Ablehnung nicht mehr. Ein Knopf, der dann noch
 * "Zugriff erlauben" verspricht, wird gedrueckt und es geschieht nichts - dieselbe Sorte
 * Sackgasse wie die ausgeblendete App ohne Weg zurueck. Ab hier fuehrt der Weg nur noch
 * ueber die Systemeinstellungen, und genau das muss dort stehen.
 */
class PermissionStateTest {

    @Test
    fun `vor der ersten Frage ist nichts blockiert`() {
        assertFalse(PermissionState.blocked(deniedOnce = false, canAskAgain = true))
        // Auch wenn das System schon "nicht mehr fragen" meldet, aber noch nie gefragt wurde:
        // dann hat der Nutzer in dieser Sitzung nichts abgelehnt, der Knopf darf es versuchen.
        assertFalse(PermissionState.blocked(deniedOnce = false, canAskAgain = false))
    }

    @Test
    fun `eine einzelne Ablehnung blockiert noch nicht`() {
        assertFalse(PermissionState.blocked(deniedOnce = true, canAskAgain = true))
    }

    @Test
    fun `abgelehnt und keine Frage mehr heisst blockiert`() {
        assertTrue(PermissionState.blocked(deniedOnce = true, canAskAgain = false))
    }
}
