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
 * what is shown while a permission is missing.
 *
 * not a dialog that springs open by itself: android grants no third one after two
 * refusals. there is always a button here, and once android stops asking it leads into the
 * system settings, the only place left to change the decision.
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
                color = palette.dangerText,
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

/** whether android still asks; after the second refusal the button would be silent. */
object PermissionState {
    fun blocked(deniedOnce: Boolean, canAskAgain: Boolean): Boolean = deniedOnce && !canAskAgain
}
