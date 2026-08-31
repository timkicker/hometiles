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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * Was zu sehen ist, wenn BigLau zweimal hintereinander nicht bis zum Zeichnen kam.
 *
 * Bewusst ohne die Konfiguration: die koennte ja gerade das Problem sein. Deshalb feste
 * Farben statt der Palette und nur die Wege, die zurueckfuehren.
 *
 * Die Texte kommen aus den Ressourcen wie ueberall sonst. Sie standen hier einmal fest
 * auf Deutsch - ausgerechnet auf dem Bildschirm, den jemand sieht, dessen Telefon gerade
 * nicht mehr startet, und der die Sprache dann am wenigsten raten kann.
 */
@Composable
fun EmergencyScreen(
    lastCrash: String?,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
    onChooseOtherLauncher: () -> Unit,
    onResetConfig: () -> Unit,
) {
    val ink = Color(0xFFF2F4F5)
    val ground = Color(0xFF0A0A0A)
    val surface = Color(0xFF161616)

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
            // Zuerst der harmloseste Weg. Der naechste Start zaehlt ohnehin wieder als
            // normal, also kostet ein zweiter Versuch nichts - und wer das nicht weiss,
            // greift sonst gleich zum Zuruecksetzen und verliert seine Belegung.
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
                label = stringResource(R.string.emergency_reset),
                secondary = stringResource(R.string.emergency_reset_hint),
                icon = Icons.Filled.RestartAlt,
                onClick = onResetConfig,
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
