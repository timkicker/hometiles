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
    val context = LocalContext.current
    val ink = Color(0xFFF2F4F5)
    val ground = Color(0xFF0A0A0A)
    val surface = Color(0xFF161616)
    val danger = Color(0xFFC62828)
    // Der einzige Knopf hier, der etwas wegnimmt - und er nahm es mit einem einzigen Tipp,
    // waehrend dasselbe Zuruecksetzen in den Einstellungen aufzaehlt, was verlorengeht, und
    // vorher eine Sicherung anbietet. Wer auf diesem Bildschirm landet, tippt herum, weil
    // sein Telefon gerade nicht geht; genau dort darf ein Tipp nicht die ganze Einrichtung
    // kosten. Zweistufig wie ueberall sonst: der erste Tipp sagt, was es kostet.
    var scharf by remember { mutableStateOf(false) }

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
            // Ganz oben das, wofuer ein Telefon da ist. Wessen Startbildschirm zweimal
            // hintereinander nicht hochkam, will vielleicht gerade jetzt jemanden anrufen -
            // und nicht erst lernen, wie man einen anderen Startbildschirm waehlt.
            //
            // Bewusst die Apps des **Systems** und nicht die eigenen: BigLau ist hier
            // gerade zweimal abgestuerzt, und die eigene Wähltastatur ist genau das, worauf
            // man sich in diesem Moment nicht verlassen sollte. `ACTION_DIAL` waehlt von
            // sich aus nie - es oeffnet nur.
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
            // Danach der harmloseste Weg zurueck. Der naechste Start zaehlt ohnehin wieder
            // als normal, also kostet ein zweiter Versuch nichts - und wer das nicht weiss,
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
                label = stringResource(
                    if (scharf) R.string.emergency_reset_now else R.string.emergency_reset,
                ),
                secondary = stringResource(
                    if (scharf) R.string.emergency_reset_warning else R.string.emergency_reset_hint,
                ),
                icon = Icons.Filled.RestartAlt,
                surface = BigSurface(if (scharf) danger else surface, ink),
                onClick = { if (scharf) onResetConfig() else scharf = true },
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
