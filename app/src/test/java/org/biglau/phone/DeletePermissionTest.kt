package org.biglau.phone

import org.junit.Assert.assertEquals
import org.junit.Test

class DeletePermissionTest {

    @Test
    fun `mit Recht wird geloescht`() {
        assertEquals(
            DeleteStep.DELETE,
            DeletePermission.next(canWrite = true, deniedOnce = false, canAskAgain = true),
        )
    }

    @Test
    fun `ohne Recht wird zuerst gefragt`() {
        assertEquals(
            DeleteStep.ASK,
            DeletePermission.next(canWrite = false, deniedOnce = false, canAskAgain = true),
        )
    }

    @Test
    fun `nach einer Ablehnung wird noch einmal gefragt`() {
        // Die erste Ablehnung ist keine Sackgasse - Android stellt die Frage erneut.
        assertEquals(
            DeleteStep.ASK,
            DeletePermission.next(canWrite = false, deniedOnce = true, canAskAgain = true),
        )
    }

    @Test
    fun `fragt Android nicht mehr, fuehrt der Weg in die Einstellungen`() {
        assertEquals(
            DeleteStep.GATE,
            DeletePermission.next(canWrite = false, deniedOnce = true, canAskAgain = false),
        )
    }

    @Test
    fun `das Recht schlaegt jede Ablehnung`() {
        // Wer das Recht in den Systemeinstellungen nachtraegt, darf nicht weiter auf der
        // Sperrseite haengen, nur weil er vorher zweimal abgelehnt hat.
        assertEquals(
            DeleteStep.DELETE,
            DeletePermission.next(canWrite = true, deniedOnce = true, canAskAgain = false),
        )
    }
}
