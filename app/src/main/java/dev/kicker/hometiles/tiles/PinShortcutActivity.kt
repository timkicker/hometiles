package dev.kicker.hometiles.tiles

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kicker.hometiles.R
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.ConfigStore
import dev.kicker.hometiles.security.Pin
import dev.kicker.hometiles.ui.BigHeading
import dev.kicker.hometiles.ui.BigRow
import dev.kicker.hometiles.ui.HomeTilesActivity
import dev.kicker.hometiles.ui.PinGate
import dev.kicker.hometiles.ui.theme.HomeTilesTheme
import dev.kicker.hometiles.ui.theme.LocalBigPalette

/**
 * "add to home screen" from another app, a web app from the browser among them.
 *
 * android sends the request to the default launcher only, and nothing lands unless it is
 * accepted here. one question, then the first free tile of the home screen; the tile editor's
 * pin guards this door too, or any app could put a tile past it.
 */
class PinShortcutActivity : HomeTilesActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val request = runCatching { launcherApps.getPinItemRequest(intent) }.getOrNull()
        val info = request?.shortcutInfo
        if (request == null || !request.isValid ||
            request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT || info == null
        ) {
            return finish()
        }
        val label = (info.shortLabel ?: info.longLabel)?.toString()?.takeIf { it.isNotBlank() }
            ?: return finish()
        val action = ButtonAction.Shortcut(info.`package`, info.id, label)
        val store = ConfigStore.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            var locked by remember {
                mutableStateOf(Pin.protectsEditor(config.security.pin, config.security.pinProtectsEditor))
            }
            HomeTilesTheme(
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
                        .background(palette.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    if (locked) {
                        PinGate(
                            title = stringResource(R.string.editor_locked),
                            explainer = stringResource(R.string.editor_locked_hint),
                            wrongText = stringResource(R.string.security_wrong_pin),
                            confirmLabel = stringResource(R.string.editor_done),
                            onCheck = { entered -> Pin.verify(entered, config.security.pin) },
                            onAccept = { locked = false },
                            acceptOnComplete = true,
                        )
                        return@Box
                    }
                    val full = PinPlacement.spotFor(config) == null
                    Column(Modifier.fillMaxSize()) {
                        BigHeading(
                            if (full) {
                                stringResource(R.string.pin_shortcut_full)
                            } else {
                                stringResource(R.string.pin_shortcut_question, label)
                            },
                        )
                        Spacer(Modifier.weight(1f))
                        if (!full) {
                            BigRow(
                                label = stringResource(R.string.pin_shortcut_add),
                                icon = Icons.Filled.Add,
                                surface = palette.surfaceAccent,
                                onClick = {
                                    // accepted only once the tile is sure to have a place: an
                                    // accepted pin without a tile is a shortcut nobody sees.
                                    val placed = PinPlacement.place(store.current, action)
                                    if (placed != null && runCatching { request.accept() }.getOrDefault(false)) {
                                        store.update { PinPlacement.place(it, action) ?: it }
                                    }
                                    finish()
                                },
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        BigRow(
                            label = stringResource(R.string.dialog_cancel),
                            icon = Icons.Filled.Close,
                            onClick = { finish() },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
