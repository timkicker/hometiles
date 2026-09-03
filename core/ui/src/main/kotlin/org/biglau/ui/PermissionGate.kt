package org.biglau.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.biglau.core.ui.R
import org.biglau.ui.theme.LocalBigPalette

/**
 * Was zu sehen ist, solange eine Berechtigung fehlt.
 *
 * Bewusst kein Dialog, der beim Öffnen von selbst aufspringt: wer ihn zweimal wegdrückt,
 * bekommt von Android keinen dritten - und stünde dann vor einem Satz ohne Knopf. Hier
 * steht immer ein Knopf. Fragt Android nicht mehr, führt er in die Systemeinstellungen,
 * denn nur dort lässt sich die Entscheidung noch ändern.
 */
@Composable
fun PermissionGate(
    title: String,
    explanation: String,
    blocked: Boolean,
    onAsk: () -> Unit,
    onSettings: () -> Unit,
) {
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(title)
        Text(
            text = explanation,
            color = palette.onBackground,
            fontSize = bigSp(17f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (blocked) {
            Text(
                text = stringResource(R.string.permission_blocked),
                color = palette.danger,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            BigRow(
                label = stringResource(R.string.permission_open_settings),
                icon = Icons.Filled.Settings,
                surface = palette.surfaceAccent,
                onClick = onSettings,
            )
        } else {
            BigRow(
                label = stringResource(R.string.permission_allow),
                surface = palette.surfaceAccent,
                onClick = onAsk,
            )
        }
    }
}

/**
 * Ob Android die Frage noch stellt.
 *
 * Nach der zweiten Ablehnung sagt das System nichts mehr - der Knopf würde dann gedrückt
 * und nichts geschähe. Genau dieser stumme Knopf ist die Falle, die hier vermieden wird.
 */
object PermissionState {
    fun blocked(deniedOnce: Boolean, canAskAgain: Boolean): Boolean = deniedOnce && !canAskAgain
}
