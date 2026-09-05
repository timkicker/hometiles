package org.biglau.safety

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.biglau.actions.Intents
import org.biglau.ui.theme.BigSurface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.res.stringResource
import org.biglau.R
import org.biglau.ui.BigHeading
import org.biglau.ui.BigRow

/**
 * what is shown when BigLau failed to reach drawing twice in a row.
 *
 * without the config, which could be the problem itself: fixed colours instead of the
 * palette, and only the ways that lead back.
 */
@Composable
fun EmergencyScreen(
    lastCrash: String?,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
    onChooseOtherLauncher: () -> Unit,
    onResetConfig: () -> Unit,
) {
    val context = LocalContext.current
    val ink = Color(0xFFF2F4F5)
    val ground = Color(0xFF0A0A0A)
    val surface = Color(0xFF161616)
    val danger = Color(0xFFC62828)
    // the only button here that takes something away, and it used to take it on one tap.
    // whoever lands on this screen taps around because their phone is not working; a tap
    // must not cost the whole setup. two steps, and the first says what it costs.
    var armed by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(ground)
            .safeDrawingPadding()
            .padding(horizontal = 8.dp),
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BigHeading(stringResource(R.string.emergency_title))
            Text(
                text = stringResource(R.string.emergency_body),
                color = ink,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            // at the very top, what a phone is for: someone whose home screen failed twice
            // may want to call now, not learn how to pick another launcher.
            //
            // the *system's* apps and not our own, since BigLau has just crashed twice.
            // `ACTION_DIAL` never dials by itself, it only opens.
            BigRow(
                label = stringResource(R.string.emergency_phone),
                icon = Icons.Filled.Call,
                onClick = { Intents.openDialer(context) },
            )
            BigRow(
                label = stringResource(R.string.emergency_contacts),
                icon = Icons.Filled.Person,
                onClick = { Intents.openContacts(context) },
            )
            // then the most harmless way back: the next start counts as normal anyway, so
            // a second try costs nothing, and without knowing that one reaches for the reset.
            BigRow(
                label = stringResource(R.string.emergency_retry),
                icon = Icons.Filled.Refresh,
                onClick = onRetry,
            )
            BigRow(
                label = stringResource(R.string.emergency_settings),
                icon = Icons.Filled.Settings,
                onClick = onSettings,
            )
            BigRow(
                label = stringResource(R.string.emergency_other_launcher),
                icon = Icons.Filled.Home,
                onClick = onChooseOtherLauncher,
            )
            BigRow(
                label = stringResource(
                    if (armed) R.string.emergency_reset_now else R.string.emergency_reset,
                ),
                secondary = stringResource(
                    if (armed) R.string.emergency_reset_warning else R.string.emergency_reset_hint,
                ),
                icon = Icons.Filled.RestartAlt,
                surface = BigSurface(if (armed) danger else surface, ink),
                onClick = { if (armed) onResetConfig() else armed = true },
            )
            if (lastCrash != null) {
                Text(
                    text = stringResource(R.string.emergency_last_error),
                    color = ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
                Text(
                    text = lastCrash,
                    color = ink,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .background(surface)
                        .padding(8.dp),
                )
            }
        }
    }
}
