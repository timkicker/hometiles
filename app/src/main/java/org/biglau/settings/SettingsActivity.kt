package org.biglau.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.R
import org.biglau.actions.Intents
import org.biglau.apps.AppRepository
import org.biglau.data.ConfigStore
import org.biglau.data.LabelPosition
import org.biglau.data.Screen
import org.biglau.data.ThemeName
import org.biglau.notify.NotificationRepository
import org.biglau.security.Pin
import org.biglau.tiles.ScreenEdits
import org.biglau.ui.BigHeading
import org.biglau.ui.BigRow
import org.biglau.ui.PinGate
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

private enum class Page { GATE, MAIN, SCREENS, APPEARANCE, BEHAVIOUR, SECURITY, SET_PIN, DIAGNOSTICS, RENAME, HIDDEN_APPS }

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val bypassPin = intent.getBooleanExtra(EXTRA_BYPASS_PIN, false)
        val store = ConfigStore.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            val locked = config.security.pin != null && !bypassPin
            var page by remember { mutableStateOf(if (locked) Page.GATE else Page.MAIN) }
            var renaming by remember { mutableStateOf<Screen?>(null) }

            BigLauTheme(config.appearance.theme, config.appearance.textScale) {
                BackHandler(enabled = page != Page.MAIN && page != Page.GATE) { page = Page.MAIN }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(LocalBigPalette.current.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    when (page) {
                        Page.GATE -> PinGate(
                            title = stringResource(R.string.settings_locked),
                            explainer = stringResource(R.string.security_explainer),
                            wrongText = stringResource(R.string.security_wrong_pin),
                            confirmLabel = stringResource(R.string.editor_done),
                            onCheck = { entered -> Pin.verify(entered, config.security.pin) },
                            onAccept = { page = Page.MAIN },
                            acceptOnComplete = true,
                            onEmergencyExit = { page = Page.MAIN },
                        )

                        Page.MAIN -> MainList(
                            onScreens = { page = Page.SCREENS },
                            onAppearance = { page = Page.APPEARANCE },
                            onBehaviour = { page = Page.BEHAVIOUR },
                            onHiddenApps = { page = Page.HIDDEN_APPS },
                            onSecurity = { page = Page.SECURITY },
                            onDiagnostics = { page = Page.DIAGNOSTICS },
                            onHomeApp = { Intents.chooseHomeApp(this@SettingsActivity) },
                            onDone = { finish() },
                        )

                        Page.SCREENS -> ScreenList(
                            screens = config.screens,
                            homeId = config.homeScreenId,
                            onRename = { renaming = it; page = Page.RENAME },
                            onDelete = { store.update { current -> ScreenEdits.delete(current, it.id) } },
                            onMakeHome = { target ->
                                store.update { current -> current.copy(homeScreenId = target.id) }
                            },
                        )

                        Page.RENAME -> RenamePanel(
                            screen = renaming,
                            onDone = { name ->
                                val target = renaming
                                if (target != null) {
                                    store.update { ScreenEdits.rename(it, target.id, name) }
                                }
                                page = Page.SCREENS
                            },
                        )

                        Page.APPEARANCE -> AppearanceList(
                            themeName = config.appearance.theme,
                            textScale = config.appearance.textScale,
                            labelPosition = config.appearance.labelPosition,
                            showIcons = config.appearance.showIcons,
                            onTheme = { next ->
                                store.update { it.copy(appearance = it.appearance.copy(theme = next)) }
                            },
                            onTextScale = { next ->
                                store.update { it.copy(appearance = it.appearance.copy(textScale = next)) }
                            },
                            onLabelPosition = { next ->
                                store.update { it.copy(appearance = it.appearance.copy(labelPosition = next)) }
                            },
                            showHeader = config.appearance.showHeader,
                            onToggleHeader = {
                                store.update {
                                    it.copy(appearance = it.appearance.copy(showHeader = !it.appearance.showHeader))
                                }
                            },
                            clockShowsDate = config.appearance.clockShowsDate,
                            onToggleClockDate = {
                                store.update {
                                    it.copy(appearance = it.appearance.copy(clockShowsDate = !it.appearance.clockShowsDate))
                                }
                            },
                            onToggleIcons = {
                                store.update {
                                    it.copy(appearance = it.appearance.copy(showIcons = !it.appearance.showIcons))
                                }
                            },
                        )

                        Page.BEHAVIOUR -> BehaviourList(
                            blinkOn = config.behaviour.blinkOnNotification,
                            accessGranted = NotificationRepository.isEnabled(this@SettingsActivity),
                            onToggleBlink = {
                                store.update {
                                    it.copy(
                                        behaviour = it.behaviour.copy(
                                            blinkOnNotification = !it.behaviour.blinkOnNotification,
                                        ),
                                    )
                                }
                            },
                            onGrantAccess = { Intents.notificationListenerSettings(this@SettingsActivity) },
                        )

                        Page.HIDDEN_APPS -> HiddenAppsList(
                            hidden = config.apps.hidden,
                            labelFor = { key ->
                                val parts = key.split("/")
                                if (parts.size == 2) {
                                    AppRepository.get(this@SettingsActivity).labelFor(parts[0], parts[1]) ?: parts[0]
                                } else {
                                    key
                                }
                            },
                            onShowAgain = { key ->
                                store.update { it.copy(apps = it.apps.copy(hidden = it.apps.hidden - key)) }
                            },
                        )

                        Page.SECURITY -> SecurityList(
                            hasPin = config.security.pin != null,
                            onSetPin = { page = Page.SET_PIN },
                            onRemovePin = {
                                store.update { it.copy(security = it.security.copy(pin = null)) }
                            },
                        )

                        Page.SET_PIN -> PinGate(
                            title = stringResource(R.string.security_new_pin),
                            explainer = null,
                            wrongText = stringResource(R.string.security_pin_rules),
                            confirmLabel = stringResource(R.string.editor_done),
                            onCheck = { entered -> Pin.isValid(entered) },
                            onAccept = { entered ->
                                Pin.hash(entered)?.let { hashed ->
                                    store.update { it.copy(security = it.security.copy(pin = hashed)) }
                                }
                                page = Page.SECURITY
                            },
                            acceptOnComplete = false,
                        )

                        Page.DIAGNOSTICS -> DiagnosticsList(this@SettingsActivity)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_BYPASS_PIN = "bypassPin"
    }
}

@Composable
private fun MainList(
    onScreens: () -> Unit,
    onAppearance: () -> Unit,
    onBehaviour: () -> Unit,
    onHiddenApps: () -> Unit,
    onSecurity: () -> Unit,
    onDiagnostics: () -> Unit,
    onHomeApp: () -> Unit,
    onDone: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings)) }
        item { BigRow(stringResource(R.string.settings_screens), icon = Icons.Filled.Home, onClick = onScreens) }
        item { BigRow(stringResource(R.string.settings_appearance), icon = Icons.Filled.Palette, onClick = onAppearance) }
        item { BigRow(stringResource(R.string.settings_behaviour), icon = Icons.Filled.NotificationsActive, onClick = onBehaviour) }
        item { BigRow(stringResource(R.string.settings_hidden_apps), icon = Icons.Filled.VisibilityOff, onClick = onHiddenApps) }
        item { BigRow(stringResource(R.string.settings_security), icon = Icons.Filled.Lock, onClick = onSecurity) }
        item { BigRow(stringResource(R.string.set_as_home), icon = Icons.Filled.Home, onClick = onHomeApp) }
        item { BigRow(stringResource(R.string.settings_diagnostics), icon = Icons.Filled.Info, onClick = onDiagnostics) }
        item {
            BigRow(
                label = stringResource(R.string.editor_done),
                surface = palette.surfaceAccent,
                onClick = onDone,
            )
        }
    }
}

@Composable
private fun ScreenList(
    screens: List<Screen>,
    homeId: String,
    onRename: (Screen) -> Unit,
    onDelete: (Screen) -> Unit,
    onMakeHome: (Screen) -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_screens)) }
        items(screens, key = { it.id }) { screen ->
            val isHome = screen.id == homeId
            BigRow(
                label = screen.name,
                secondary = stringResource(
                    if (isHome) R.string.screen_is_home else R.string.screen_grid,
                    screen.cols,
                    screen.rows,
                ),
                icon = Icons.Filled.Edit,
                surface = if (isHome) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onRename(screen) },
            )
            if (!isHome) {
                BigRow(
                    label = stringResource(R.string.screen_make_home),
                    icon = Icons.Filled.Home,
                    onClick = { onMakeHome(screen) },
                )
                BigRow(
                    label = stringResource(R.string.screen_delete, screen.name),
                    icon = Icons.Filled.Delete,
                    surface = palette.surfaceDanger,
                    onClick = { onDelete(screen) },
                )
            }
        }
    }
}

@Composable
private fun RenamePanel(screen: Screen?, onDone: (String) -> Unit) {
    if (screen == null) return
    var text by remember { mutableStateOf(screen.name) }
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(stringResource(R.string.screen_rename))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth(),
        )
        BigRow(
            label = stringResource(R.string.editor_done),
            surface = palette.surfaceAccent,
            onClick = { onDone(text) },
        )
    }
}

@Composable
private fun AppearanceList(
    themeName: ThemeName,
    textScale: Float,
    labelPosition: LabelPosition,
    showIcons: Boolean,
    showHeader: Boolean,
    clockShowsDate: Boolean,
    onToggleHeader: () -> Unit,
    onToggleClockDate: () -> Unit,
    onTheme: (ThemeName) -> Unit,
    onTextScale: (Float) -> Unit,
    onLabelPosition: (LabelPosition) -> Unit,
    onToggleIcons: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_appearance)) }
        items(ThemeName.entries.toList()) { entry ->
            BigRow(
                label = stringResource(themeLabel(entry)),
                icon = Icons.Filled.Palette,
                surface = if (entry == themeName) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onTheme(entry) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_text_size)) }
        items(listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)) { scale ->
            BigRow(
                label = "${(scale * 100).toInt()} %",
                icon = Icons.Filled.TextFields,
                surface = if (scale == textScale) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onTextScale(scale) },
            )
        }
        item { BigHeading(stringResource(R.string.appearance_label_position)) }
        items(LabelPosition.entries.toList()) { position ->
            BigRow(
                label = stringResource(labelPositionLabel(position)),
                surface = if (position == labelPosition) palette.surfaceAccent else palette.surfaceDefault,
                onClick = { onLabelPosition(position) },
            )
        }
        item {
            BigRow(
                label = stringResource(if (showIcons) R.string.appearance_icons_on else R.string.appearance_icons_off),
                onClick = onToggleIcons,
            )
        }
        item { BigHeading(stringResource(R.string.appearance_header)) }
        item {
            BigRow(
                label = stringResource(if (showHeader) R.string.header_on else R.string.header_off),
                secondary = stringResource(R.string.header_explainer),
                icon = Icons.Filled.Schedule,
                surface = if (showHeader) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleHeader,
            )
        }
        item {
            BigRow(
                label = stringResource(if (clockShowsDate) R.string.header_date_on else R.string.header_date_off),
                onClick = onToggleClockDate,
            )
        }
    }
}

/**
 * Ausgeblendete Apps wieder einblenden. Ohne diese Seite waere das Ausblenden eine
 * Einbahnstrasse - eine Aktion ohne Rueckweg ist ein Fehler, auch wenn sie tut, was sie soll.
 */
@Composable
private fun HiddenAppsList(
    hidden: Set<String>,
    labelFor: (String) -> String,
    onShowAgain: (String) -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_hidden_apps)) }
        if (hidden.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.hidden_apps_none),
                    color = palette.onBackground,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
        items(hidden.toList().sortedBy { labelFor(it).lowercase() }) { key ->
            BigRow(
                label = labelFor(key),
                secondary = stringResource(R.string.hidden_apps_show_again),
                icon = Icons.Filled.Visibility,
                onClick = { onShowAgain(key) },
            )
        }
    }
}

@Composable
private fun BehaviourList(
    blinkOn: Boolean,
    accessGranted: Boolean,
    onToggleBlink: () -> Unit,
    onGrantAccess: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_behaviour)) }
        item {
            Text(
                text = stringResource(R.string.blink_explainer),
                color = palette.onBackground,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
        item {
            BigRow(
                label = stringResource(if (blinkOn) R.string.blink_on else R.string.blink_off),
                icon = Icons.Filled.NotificationsActive,
                surface = if (blinkOn) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleBlink,
            )
        }
        item {
            BigRow(
                label = stringResource(
                    if (accessGranted) R.string.blink_access_granted else R.string.blink_access_missing,
                ),
                icon = Icons.Filled.Lock,
                surface = if (accessGranted) palette.surfaceDefault else palette.surfaceDanger,
                onClick = onGrantAccess,
            )
        }
    }
}

@Composable
private fun SecurityList(hasPin: Boolean, onSetPin: () -> Unit, onRemovePin: () -> Unit) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_security)) }
        item {
            Text(
                text = stringResource(R.string.security_explainer),
                color = palette.onBackground,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
        item {
            BigRow(
                label = stringResource(if (hasPin) R.string.security_change_pin else R.string.security_set_pin),
                icon = Icons.Filled.Lock,
                onClick = onSetPin,
            )
        }
        if (hasPin) {
            item {
                BigRow(
                    label = stringResource(R.string.security_remove_pin),
                    icon = Icons.Filled.Delete,
                    surface = palette.surfaceDanger,
                    onClick = onRemovePin,
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsList(activity: ComponentActivity) {
    val lines = remember { Diagnostics.collect(activity) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.settings_diagnostics)) }
        items(lines) { line ->
            BigRow(label = line.first, secondary = line.second, onClick = {})
        }
    }
}

private fun themeLabel(theme: ThemeName) = when (theme) {
    ThemeName.DARK -> R.string.theme_dark
    ThemeName.HIGH_CONTRAST -> R.string.theme_contrast
    ThemeName.LIGHT -> R.string.theme_light
}

private fun labelPositionLabel(position: LabelPosition) = when (position) {
    LabelPosition.BOTTOM_LEFT -> R.string.label_bottom_left
    LabelPosition.BOTTOM_CENTER -> R.string.label_bottom_center
    LabelPosition.TOP_LEFT -> R.string.label_top_left
    LabelPosition.HIDDEN -> R.string.label_hidden
}
