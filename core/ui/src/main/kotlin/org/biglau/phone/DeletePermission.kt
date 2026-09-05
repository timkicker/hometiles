package org.biglau.phone

import org.biglau.ui.PermissionState

enum class DeleteStep {
    /** the right is there, delete. */
    DELETE,

    /** android still asks, open the system dialog. */
    ASK,

    /** android asks no more, show the way into the system settings. */
    GATE,
}

/**
 * the path to the write right on the call log.
 *
 * `WRITE_CALL_LOG` was checked before every delete but never asked for, so a confirmed
 * delete left the row standing. android grants it without a dialog once `READ_CALL_LOG` is
 * there, both being in the CALL_LOG group; [GATE] is the case no shell can produce and
 * that must still have an answer.
 */
object DeletePermission {

    fun next(canWrite: Boolean, deniedOnce: Boolean, canAskAgain: Boolean): DeleteStep = when {
        canWrite -> DeleteStep.DELETE
        PermissionState.blocked(deniedOnce, canAskAgain) -> DeleteStep.GATE
        else -> DeleteStep.ASK
    }
}
