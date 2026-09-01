package org.biglau.wizard

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import org.biglau.ui.BigLauActivity
import org.biglau.ui.ScrollButtonPair
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.R
import org.biglau.actions.Intents
import org.biglau.data.ConfigStore
import org.biglau.data.ThemeName
import org.biglau.ui.BigHeading
import org.biglau.ui.BigRow
import org.biglau.ui.dpSp
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

/**
 * Der Erststart-Assistent.
 *
 * Er stellt genau die Fragen, die man am Anfang beantworten muss, und ueberspringt, was schon
 * erledigt ist. Bei der Textgroesse und beim Aussehen wirkt die Wahl sofort auf den Assistenten
 * selbst - man sieht also, was man waehlt, statt es sich vorzustellen.
 */
class WizardActivity : BigLauActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            var step by remember { mutableStateOf(WizardStep.WELCOME) }
            var state by remember { mutableStateOf(readState()) }
            val palette = LocalBigPalette.current

            val askPermissions = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { state = readState() }

            fun advance() {
                val next = WizardSteps.next(step, state)
                if (next == null) finishWizard(store) else step = next
            }

            BigLauTheme(
                config.appearance.theme,
                config.appearance.textScale,
                haptics = config.behaviour.haptics,
                font = config.appearance.font,
                labelScale = config.appearance.labelScale,
                iconPercent = config.appearance.iconPercent,
                icons = config.appearance.icons,
                cornerRadiusDp = config.appearance.cornerRadiusDp,
            ) {
                BackHandler(enabled = true) {
                    WizardSteps.previous(step, state)?.let { step = it }
                }

                val (position, total) = WizardSteps.position(step, state)

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(palette.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.wizard_position, position, total),
                            color = palette.onBackground,
                            fontSize = dpSp(14f),
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )

                        when (step) {
                            WizardStep.WELCOME -> Simple(
                                title = stringResource(R.string.wizard_welcome_title),
                                body = stringResource(R.string.wizard_welcome_body),
                                action = stringResource(R.string.wizard_start),
                                onAction = ::advance,
                            )

                            WizardStep.TEXT_SIZE -> Choice(
                                title = stringResource(R.string.appearance_text_size),
                                body = stringResource(R.string.wizard_text_body),
                                options = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).map {
                                    "${(it * 100).toInt()} %" to it
                                },
                                selected = config.appearance.textScale,
                                onPick = { value ->
                                    store.update { it.copy(appearance = it.appearance.copy(textScale = value)) }
                                },
                                onNext = ::advance,
                            )

                            WizardStep.THEME -> Choice(
                                title = stringResource(R.string.settings_appearance),
                                body = stringResource(R.string.wizard_theme_body),
                                options = listOf(
                                    stringResource(R.string.theme_dark) to ThemeName.DARK,
                                    stringResource(R.string.theme_contrast) to ThemeName.HIGH_CONTRAST,
                                    stringResource(R.string.theme_light) to ThemeName.LIGHT,
                                ),
                                selected = config.appearance.theme,
                                onPick = { value ->
                                    store.update { it.copy(appearance = it.appearance.copy(theme = value)) }
                                },
                                onNext = ::advance,
                            )

                            WizardStep.PERMISSIONS -> Simple(
                                title = stringResource(R.string.wizard_permissions_title),
                                body = stringResource(R.string.wizard_permissions_body),
                                action = stringResource(R.string.wizard_permissions_grant),
                                onAction = {
                                    askPermissions.launch(
                                        arrayOf(
                                            Manifest.permission.READ_CONTACTS,
                                            Manifest.permission.CALL_PHONE,
                                        ),
                                    )
                                },
                                secondaryAction = stringResource(R.string.wizard_later),
                                onSecondary = ::advance,
                            )

                            WizardStep.HOME_ROLE -> Simple(
                                title = stringResource(R.string.wizard_home_title),
                                body = stringResource(R.string.wizard_home_body),
                                action = stringResource(R.string.set_as_home),
                                onAction = { Intents.chooseHomeApp(this@WizardActivity) },
                                secondaryAction = stringResource(R.string.wizard_later),
                                onSecondary = ::advance,
                            )

                            WizardStep.DONE -> Simple(
                                title = stringResource(R.string.wizard_done_title),
                                body = stringResource(R.string.wizard_done_body),
                                action = stringResource(R.string.wizard_finish),
                                onAction = { finishWizard(store) },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Nach einem Ausflug in die Systemeinstellungen kann sich der Zustand geaendert haben.
        recreateIfNeeded()
    }

    private fun recreateIfNeeded() = Unit

    private fun finishWizard(store: ConfigStore) {
        store.update { it.copy(wizardDone = true) }
        finish()
    }

    private fun readState() = WizardState(
        isHomeApp = isHomeApp(),
        hasContacts = granted(Manifest.permission.READ_CONTACTS),
        hasCallPhone = granted(Manifest.permission.CALL_PHONE),
    )

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun isHomeApp(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roles = getSystemService(RoleManager::class.java) ?: return false
        return roles.isRoleAvailable(RoleManager.ROLE_HOME) && roles.isRoleHeld(RoleManager.ROLE_HOME)
    }
}

@Composable
private fun Simple(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        BigHeading(title)
        Text(
            text = body,
            color = palette.onBackground,
            fontSize = dpSp(17f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        BigRow(label = action, surface = palette.surfaceAccent, onClick = onAction)
        if (secondaryAction != null && onSecondary != null) {
            BigRow(label = secondaryAction, onClick = onSecondary)
        }
    }
}

@Composable
private fun <T> Choice(
    title: String,
    body: String,
    options: List<Pair<String, T>>,
    selected: T,
    onPick: (T) -> Unit,
    onNext: () -> Unit,
) {
    val palette = LocalBigPalette.current
    // "Weiter" steht fest am unteren Rand, nicht am Ende der Liste. Bei fuenf Auswahlzeilen
    // und grosser Schrift war der Knopf sonst unterhalb der Falz - und wer ihn nicht findet,
    // kommt aus dem Assistenten nicht heraus.
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(title)
        Text(
            text = body,
            color = palette.onBackground,
            fontSize = dpSp(16f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(options) { (label, value) ->
                BigRow(
                    label = label,
                    surface = if (value == selected) palette.surfaceAccent else palette.surfaceDefault,
                    onClick = { onPick(value) },
                )
            }
        }
        // Passt nicht alles auf den Schirm - und bei 200 % Schrift passt es nie -, kommen
        // zwei Blaetterknoepfe neben "Weiter". Sonst haengt die groesste Schriftgroesse
        // unter der Falz, ausgerechnet fuer den, der sie sucht. Nicht in eine eigene Zeile:
        // die kostete wieder eine Auswahlzeile.
        val scrollable = listState.canScrollForward || listState.canScrollBackward
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (scrollable) {
                ScrollButtonPair(listState) { up, down ->
                    up(Modifier.height(72.dp))
                    down(Modifier.height(72.dp))
                }
            }
            BigRow(
                label = stringResource(R.string.wizard_next),
                surface = palette.surfaceAccent,
                modifier = Modifier.weight(1f),
                onClick = onNext,
            )
        }
    }
}
