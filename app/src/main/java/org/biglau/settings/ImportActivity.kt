package org.biglau.settings

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.ui.bigSp
import org.biglau.ui.BigLauActivity
import org.biglau.R
import org.biglau.data.ConfigStore
import org.biglau.data.ConfigTransfer
import org.biglau.data.LauncherConfig
import org.biglau.ui.BigHeading
import org.biglau.ui.BigRow
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

/**
 * Nimmt eine Sicherungsdatei entgegen, die von aussen geoeffnet wird - aus einem
 * Dateimanager, einer Cloud-App oder einem Mailanhang.
 *
 * Das ist der Weg beim Wechsel auf ein neues Telefon: Datei drauf, antippen, fertig.
 * Ueber den Dateidialog in den Einstellungen geht es auch, aber diesen Weg geht man
 * genau einmal - und dann soll er offensichtlich sein.
 *
 * Importiert wird erst nach ausdruecklicher Bestaetigung: das Einlesen ersetzt die
 * gesamte bestehende Einrichtung.
 */
class ImportActivity : BigLauActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val uri: Uri? = intent?.data
        val store = ConfigStore.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            var done by remember { mutableStateOf(false) }

            // **Die Datei wird nicht im Hauptthread gelesen.**
            //
            // Bis zum 04.09.2026 stand `read(uri)` in `onCreate`, direkt und blockierend.
            // Bei einer Datei auf dem Geraet faellt das nicht auf; der Weg, den der
            // Klassenkopf beschreibt - Sicherung aus einer Cloud-App oder einem
            // Mailanhang - geht aber ueber einen fremden Anbieter, und der holt sie unter
            // Umstaenden erst aus dem Netz. Dann steht der Bildschirm, bis Android die App
            // fuer haengend erklaert. Jede andere Stelle im Programm liest ueber
            // `withContext(Dispatchers.IO)`; diese war die Ausnahme.
            var inhalt by remember { mutableStateOf<String?>(null) }
            var laedt by remember { mutableStateOf(uri != null) }
            LaunchedEffect(uri) {
                if (uri == null) {
                    laedt = false
                    return@LaunchedEffect
                }
                inhalt = read(uri)
                laedt = false
            }
            val loaded: LauncherConfig? = remember(inhalt) { inhalt?.let(ConfigTransfer::import) }
            val vonNeuerer = remember(inhalt) {
                inhalt?.let { ConfigTransfer.isFromNewerVersion(it) } == true
            }

            BigLauTheme(
                config.appearance.theme,
                config.appearance.textScale,
                haptics = config.behaviour.haptics,
                font = config.appearance.font,
                labelScale = config.appearance.labelScale,
                iconPercent = config.appearance.iconPercent,
                icons = config.appearance.icons,
                hideCutLabels = config.appearance.hideCutLabels,
                cornerRadiusDp = config.appearance.cornerRadiusDp,
            ) {
                val palette = LocalBigPalette.current
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(LocalBigPalette.current.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BigHeading(stringResource(R.string.settings_transfer))
                        Text(
                            text = when {
                                // Zwei verschiedene Fehler, zwei verschiedene Saetze: eine
                                // Datei, die sich nicht oeffnen laesst, ist nicht dasselbe
                                // wie eine, die keine Sicherung ist. Vorher stand beides
                                // unter "Das ist keine BigLau-Sicherung" - und wer die Datei
                                // gerade selbst geschrieben hatte, suchte den Fehler bei ihr.
                                laedt -> stringResource(R.string.transfer_reading)
                                inhalt == null -> stringResource(R.string.transfer_unreadable)
                                loaded == null -> stringResource(R.string.transfer_bad_file)
                                done && vonNeuerer -> stringResource(R.string.transfer_imported_older)
                                done -> stringResource(R.string.transfer_imported)
                                // Dieselbe Zaehlung wie beim Zuruecksetzen, und zwar aus
                                // demselben Grund: hier stand "3 Screens mit 14 Kacheln",
                                // dort "2 Screens mit 14 Kacheln und 1 Ordner" - dieselbe
                                // Einrichtung, zwei Zahlen. Fuer den Nutzer ist ein Ordner
                                // kein Screen, und eine leere Zelle keine Kachel.
                                else -> {
                                    val verlust = Reset.losses(loaded)
                                    val bildschirme = pluralStringResource(
                                        R.plurals.reset_screens, verlust.screens, verlust.screens,
                                    )
                                    val kacheln = pluralStringResource(
                                        R.plurals.reset_tiles, verlust.tiles, verlust.tiles,
                                    )
                                    if (verlust.folders == 0) {
                                        stringResource(R.string.transfer_confirm_plain, bildschirme, kacheln)
                                    } else {
                                        stringResource(
                                            R.string.transfer_confirm_folders,
                                            bildschirme,
                                            kacheln,
                                            pluralStringResource(
                                                R.plurals.reset_folders, verlust.folders, verlust.folders,
                                            ),
                                        )
                                    }
                                }
                            },
                            color = palette.onBackground,
                            fontSize = bigSp(17f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        )
                        // Die Warnung gehoert **vor** die Entscheidung.
                        //
                        // `transfer_imported_older` gab es schon, aber erst danach: erst
                        // war die ganze Einrichtung ersetzt, dann stand da, dass etwas
                        // fehlt. Am 04.09.2026 am Emulator nachgestellt, mit einer
                        // Sicherung, die `"version": 99` trug - die Rueckfrage sagte nur
                        // "2 Bildschirme mit 12 Kacheln", geladen, und **danach** kam der
                        // Satz. Wer sie danach liest, kann nichts mehr entscheiden; seine
                        // alte Belegung ist weg.
                        if (loaded != null && !done && vonNeuerer) {
                            Text(
                                text = stringResource(R.string.transfer_confirm_newer),
                                color = palette.dangerText,
                                fontSize = bigSp(16f),
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                        if (loaded != null && !done) {
                            BigRow(
                                label = stringResource(R.string.transfer_confirm_yes),
                                surface = palette.surfaceAccent,
                                onClick = {
                                    store.update { loaded }
                                    done = true
                                },
                            )
                        }
                        BigRow(
                            label = stringResource(if (done) R.string.editor_done else R.string.dialog_cancel),
                            onClick = { finish() },
                        )
                    }
                }
            }
        }
    }

    /**
     * Liest die Datei - und wechselt den Faden selbst.
     *
     * Nicht am Aufrufer, sondern hier: eine Funktion, die von der Platte liest und das dem
     * Aufrufer ueberlaesst, wird irgendwann aus dem Hauptthread gerufen. Als `suspend` mit
     * eigenem `withContext` kann sie es gar nicht.
     */
    private suspend fun read(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
    }
}
