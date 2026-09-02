package org.biglau.phone

import org.biglau.ui.PermissionState

/** Was als Nächstes passiert, wenn jemand das Löschen eines Anrufs bestätigt hat. */
enum class DeleteStep {
    /** Das Recht ist da - löschen. */
    DELETE,

    /** Android fragt noch - den Systemdialog öffnen. */
    ASK,

    /** Android fragt nicht mehr - den Weg in die Systemeinstellungen zeigen. */
    GATE,
}

/**
 * Der Weg zum Schreibrecht auf der Anrufliste.
 *
 * `WRITE_CALL_LOG` stand im Manifest und wurde vor jedem Löschen geprüft, aber nie erfragt;
 * am Telefon des Nutzers stand es auf `granted=false`. Die Rückfrage kam, die Bestätigung
 * ging durch, und die Zeile blieb stehen.
 *
 * In der Praxis erteilt Android das Recht ohne Dialog, sobald `READ_CALL_LOG` schon da ist -
 * beide gehören zur Gruppe CALL_LOG. Für den Nutzer heißt das: das erste Löschen wirkt
 * einfach. [GATE] ist der Fall, den keine Shell herstellen kann und der trotzdem nicht
 * unbeantwortet bleiben darf.
 */
object DeletePermission {

    fun next(canWrite: Boolean, deniedOnce: Boolean, canAskAgain: Boolean): DeleteStep = when {
        canWrite -> DeleteStep.DELETE
        PermissionState.blocked(deniedOnce, canAskAgain) -> DeleteStep.GATE
        else -> DeleteStep.ASK
    }
}
