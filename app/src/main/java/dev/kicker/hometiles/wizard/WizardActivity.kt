package dev.kicker.hometiles.wizard

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
import dev.kicker.hometiles.a11y.LongPress
import dev.kicker.hometiles.ui.bigSp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import dev.kicker.hometiles.ui.HomeTilesActivity
import dev.kicker.hometiles.ui.ScrollButtonPair
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
import dev.kicker.hometiles.R
import dev.kicker.hometiles.actions.Intents
import dev.kicker.hometiles.data.ConfigStore
import dev.kicker.hometiles.data.ThemeName
import dev.kicker.hometiles.ui.BigHeading
import dev.kicker.hometiles.ui.BigRow
import dev.kicker.hometiles.ui.dpSp
import dev.kicker.hometiles.ui.theme.HomeTilesTheme
import dev.kicker.hometiles.ui.theme.LocalBigPalette

/**
 * the first-start wizard.
 *
 * it asks exactly the questions that must be answered at the beginning and skips what is
 * done. text size and appearance take effect on the wizard itself at once, so one sees the
 * choice instead of imagining it.
 */
class WizardActivity : HomeTilesActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            var step by remember { mutableStateOf(WizardStep.WELCOME) }
            // read again on every return: the state used to come back only from the two
            // dialogs, so granting a permission in the system settings after tapping later
            // left the finished step standing. see `HomeTilesActivity.resumes`.
            var state by remember(resumes.intValue) { mutableStateOf(readState()) }

            val askPermissions = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { state = readState() }

            // the role dialog needs a caller or it aborts before it is seen; see
            // Intents.dialerRoleIntent.
            val askHomeRole = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { state = readState() }

            fun advance() {
                val next = WizardSteps.next(step, state)
                if (next == null) finishWizard(store) else step = next
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
                // back goes one step back, and out of the first step. the wizard used to
                // swallow the back key entirely, so anyone opening it again from the
                // settings got out only by the home key or through all four steps.
                //
                // closing on the very first start loses nothing: `wizardDone` stays false,
                // the wizard returns on the next start, and the home screen meanwhile
                // invites with tap to assign.
                BackHandler(enabled = true) {
                    val back = WizardSteps.previous(step, state)
                    if (back != null) step = back else finish()
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
                            // whoever lands here because their stored setup could not be
                            // read would otherwise see a welcome and think the phone was
                            // reset, without learning that the old file still lies beside it.
                            WizardStep.WELCOME -> Simple(
                                title = stringResource(R.string.wizard_welcome_title),
                                body = if (store.startedFromBrokenFile) {
                                    stringResource(R.string.wizard_welcome_after_broken)
                                } else {
                                    stringResource(R.string.wizard_welcome_body)
                                },
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
                                    stringResource(R.string.theme_system) to ThemeName.SYSTEM,
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
                                onAction = {
                                    val intent = Intents.homeRoleIntent(this@WizardActivity)
                                    if (intent != null) {
                                        askHomeRole.launch(intent)
                                    } else {
                                        Intents.chooseHomeApp(this@WizardActivity)
                                    }
                                },
                                secondaryAction = stringResource(R.string.wizard_later),
                                onSecondary = ::advance,
                            )

                            WizardStep.DONE -> Simple(
                                title = stringResource(R.string.wizard_done_title),
                                // the wizard's last sentence is the one that sticks, and it
                                // is not always true: with speech or the long-press popup on
                                // (exactly this app's audience) the long press no longer
                                // reaches the editor. `LongPress.needsEditModeEntry` knows.
                                body = stringResource(
                                    if (LongPress.needsEditModeEntry(
                                            config.behaviour.accessibility,
                                            config.behaviour.pressMode,
                                        )
                                    ) {
                                        R.string.wizard_done_body_edit_mode
                                    } else {
                                        R.string.wizard_done_body
                                    },
                                ),
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
        // a trip into the system settings can have changed the state.
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        BigHeading(title)
        // the text grows with the chosen size, since that is what is being chosen here; a
        // sentence that ignores the change does not show it. it scrolls in a field of its
        // own so it never pushes the buttons out.
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = body,
                color = palette.onBackground,
                fontSize = bigSp(17f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
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
    // next stands fixed at the bottom, not at the end of the list: with five rows and a
    // large font it sat below the fold, and whoever cannot find it cannot leave the wizard.
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(title)
        Text(
            text = body,
            color = palette.onBackground,
            fontSize = bigSp(16f),
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
                    selected = value == selected,
                    onClick = { onPick(value) },
                )
            }
        }
        // when not everything fits - and at 200 % it never does - two paging buttons join
        // next. otherwise the largest font size hangs below the fold, for exactly the person
        // looking for it. not in a row of its own, which would cost another choice row.
        val scrollable = listState.canScrollForward || listState.canScrollBackward
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (scrollable) {
                // height only. the width comes from the floor in `PageButton` (48 dp, the
                // minimum for a fingertip): setting a larger one left so little room for
                // next at 200 % that the word broke mid-way.
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
