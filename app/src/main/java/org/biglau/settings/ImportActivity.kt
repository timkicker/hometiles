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
 * takes a backup file opened from outside: a file manager, a cloud app, a mail attachment.
 *
 * this is the path when moving to a new phone, and one walks it exactly once, so it should
 * be obvious. imported only after an explicit confirmation: it replaces the whole setup.
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

            // not read on the main thread: a file on the device would not show it, but the
            // path the class doc describes goes through a foreign provider that may fetch
            // it from the network first, and then the screen stands until android calls the
            // app hung.
            var content by remember { mutableStateOf<String?>(null) }
            var loading by remember { mutableStateOf(uri != null) }
            LaunchedEffect(uri) {
                if (uri == null) {
                    loading = false
                    return@LaunchedEffect
                }
                content = read(uri)
                loading = false
            }
            val loaded: LauncherConfig? = remember(content) { content?.let(ConfigTransfer::import) }
            val fromNewer = remember(content) {
                content?.let { ConfigTransfer.isFromNewerVersion(it) } == true
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
                                // two different faults, two different sentences: a file
                                // that cannot be opened is not the same as one that is no
                                // backup, and both under one sentence sent people looking
                                // for a mistake in a file they had just written themselves.
                                loading -> stringResource(R.string.transfer_reading)
                                content == null -> stringResource(R.string.transfer_unreadable)
                                loaded == null -> stringResource(R.string.transfer_bad_file)
                                done && fromNewer -> stringResource(R.string.transfer_imported_older)
                                done -> stringResource(R.string.transfer_imported)
                                // the same count as the reset, for the same reason: a folder
                                // is no screen to the user, and an empty cell no tile.
                                else -> {
                                    val loss = Reset.losses(loaded)
                                    val screens = pluralStringResource(
                                        R.plurals.reset_screens, loss.screens, loss.screens,
                                    )
                                    val tiles = pluralStringResource(
                                        R.plurals.reset_tiles, loss.tiles, loss.tiles,
                                    )
                                    if (loss.folders == 0) {
                                        stringResource(R.string.transfer_confirm_plain, screens, tiles)
                                    } else {
                                        stringResource(
                                            R.string.transfer_confirm_folders,
                                            screens,
                                            tiles,
                                            pluralStringResource(
                                                R.plurals.reset_folders, loss.folders, loss.folders,
                                            ),
                                        )
                                    }
                                }
                            },
                            color = palette.onBackground,
                            fontSize = bigSp(17f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        )
                        // the warning belongs *before* the decision: it existed already,
                        // but only afterwards, when the whole setup was replaced and the
                        // old arrangement gone.
                        if (loaded != null && !done && fromNewer) {
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
     * reads the file and switches threads itself: a function that reads from disk and leaves
     * that to the caller is called from the main thread sooner or later.
     */
    private suspend fun read(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
    }
}
