package dev.kicker.hometiles.phone

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kicker.hometiles.ui.bestDatePattern
import dev.kicker.hometiles.ui.bigSp
import dev.kicker.hometiles.ui.HomeTilesActivity
import dev.kicker.hometiles.ui.currentLocale
import dev.kicker.hometiles.R
import kotlinx.coroutines.launch
import dev.kicker.hometiles.contacts.ContactRepository
import dev.kicker.hometiles.contacts.PhoneContact
import dev.kicker.hometiles.data.ConfigStore
import dev.kicker.hometiles.data.SpeedDialTarget
import dev.kicker.hometiles.ui.ContactAvatar
import dev.kicker.hometiles.ui.BigHeading
import dev.kicker.hometiles.ui.BigKeypad
import dev.kicker.hometiles.actions.Intents
import dev.kicker.hometiles.security.Pin
import dev.kicker.hometiles.ui.PinGate
import dev.kicker.hometiles.ui.Notice
import dev.kicker.hometiles.ui.PermissionGate
import dev.kicker.hometiles.ui.PermissionState
import androidx.compose.material.icons.filled.History
import dev.kicker.hometiles.ui.BigIconButton
import dev.kicker.hometiles.ui.BigRow
import dev.kicker.hometiles.ui.SettingsLink
import dev.kicker.hometiles.ui.ScrollButtons
import dev.kicker.hometiles.ui.tabularFigures
import dev.kicker.hometiles.ui.dpSp
import dev.kicker.hometiles.ui.fittedSingleLineDp
import dev.kicker.hometiles.ui.theme.LocalTextScale
import dev.kicker.hometiles.ui.theme.HomeTilesTheme
import dev.kicker.hometiles.ui.theme.LocalBigPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Tab { KEYPAD, LOG, ASSIGN }

/**
 * keypad and call log.
 *
 * dialling goes through ACTION_CALL. emergency numbers explicitly do *not* take this way,
 * see [dial].
 */
class DialerActivity : HomeTilesActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)
        val callLog = CallLogRepository.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            var tab by rememberSaveable { mutableStateOf(Tab.KEYPAD) }
            // arriving through a tel: link, the number is already there.
            var typed by rememberSaveable {
                mutableStateOf(intent?.data?.schemeSpecificPart?.let(PhoneNumbers::clean).orEmpty())
            }
            var groups by remember { mutableStateOf<List<CallGroup>>(emptyList()) }
            // `resumes` as the key: on a permanently refused permission this screen sends
            // people into the app settings, and nothing comes back from there.
var logGranted by remember(resumes.intValue) { mutableStateOf(callLog.hasPermission()) }
            var assigningKey by remember { mutableStateOf<Char?>(null) }
            var missedOnly by rememberSaveable { mutableStateOf(intent?.getBooleanExtra(EXTRA_MISSED, false) == true) }
            var pendingDelete by remember { mutableStateOf<Pair<String, List<Long>>?>(null) }
            // the confirmation before a call from the log. `PLAN.md` 3.1, principle 5.
            var pendingCall by remember { mutableStateOf<Pair<String, String>?>(null) }
            // the question first, which says what disappears, then the pin: the lock then
            // stands directly before the irreversible step, and one knows what for.
            var pinFor by remember { mutableStateOf<Pair<String, List<Long>>?>(null) }
            val scope = rememberCoroutineScope()
            var contacts by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
            val contactRepo = remember { ContactRepository.get(this@DialerActivity) }

            // the right to *change* the call log is a second one beside reading, and it was
            // never asked for: deleting an entry confirmed the question and left the row
            // standing, a dead end with no way out. the phone role grants it along, but this
            // way must not hang on a role.
            var writeDeniedOnce by remember { mutableStateOf(false) }
            var writeCanAskAgain by remember { mutableStateOf(true) }
            // once android stops asking, only the system settings are left.
            var writeGate by remember { mutableStateOf(false) }
            var awaitingWriteRight by remember { mutableStateOf<Pair<String, List<Long>>?>(null) }

            /** the deletion itself, behind the question, the write right and, if set, the pin. */
            fun deleteNow(pending: Pair<String, List<Long>>) {
                scope.launch {
                    val removed = if (pending.second.isEmpty()) {
                        callLog.deleteAll()
                    } else {
                        callLog.delete(pending.second)
                    }
                    // reload the list in any case: it is the truth, not the provider's count.
                    groups = callLog.load(mode = config.phone.callGrouping)
                    if (removed > 0) {
                        Notice.show(
                            this@DialerActivity,
                            resources.getQuantityString(R.plurals.calllog_deleted, removed, removed),
                        )
                    } else {
                        Notice.show(this@DialerActivity, R.string.calllog_delete_failed)
                    }
                }
            }

            val askWrite = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                val pending = awaitingWriteRight
                awaitingWriteRight = null
                if (granted) {
                    pending?.let(::deleteNow)
                } else {
                    writeDeniedOnce = true
                    writeCanAskAgain =
                        shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CALL_LOG)
                    writeGate = DeletePermission.next(
                        canWrite = false,
                        deniedOnce = true,
                        canAskAgain = writeCanAskAgain,
                    ) == DeleteStep.GATE
                }
            }

            /**
             * ask first, then delete. asked here and not when opening the list: looking up
             * who called should not raise a question about changing it.
             */
            fun deleteAfterPermission(pending: Pair<String, List<Long>>) {
                when (
                    DeletePermission.next(
                        canWrite = callLog.canWrite(),
                        deniedOnce = writeDeniedOnce,
                        canAskAgain = writeCanAskAgain,
                    )
                ) {
                    DeleteStep.DELETE -> deleteNow(pending)
                    DeleteStep.GATE -> writeGate = true
                    DeleteStep.ASK -> {
                        awaitingWriteRight = pending
                        askWrite.launch(Manifest.permission.WRITE_CALL_LOG)
                    }
                }
            }

            LaunchedEffect(tab) {
                if (tab == Tab.ASSIGN && contacts.isEmpty()) contacts = contactRepo.load(resources)
            }

            var logDeniedOnce by remember { mutableStateOf(false) }
            var logCanAskAgain by remember { mutableStateOf(true) }
            val askLog = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { result ->
                logGranted = result
                if (!result) {
                    logDeniedOnce = true
                    logCanAskAgain =
                        shouldShowRequestPermissionRationale(Manifest.permission.READ_CALL_LOG)
                }
            }
            val askCall = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { }

            LaunchedEffect(Unit) {
                if (intent?.getBooleanExtra(EXTRA_MISSED, false) == true ||
                    intent?.getBooleanExtra(EXTRA_LOG, false) == true
                ) {
                    tab = Tab.LOG
                }
            }

            // the system asks by itself on the first look. not after a refusal, or a
            // sentence would stand there without a button.
            //
            // and while reading, the list does not say it is empty: `groups` starts empty and
            // `CallLogEmpty.reason` would turn that into no calls yet, which is false while
            // the provider is still reading. see LoadingTest.
            var loading by remember { mutableStateOf(true) }
            LaunchedEffect(tab, logGranted) {
                if (tab != Tab.LOG) return@LaunchedEffect
                if (logGranted) {
                    groups = callLog.load(mode = config.phone.callGrouping)
                    loading = false
                    // seen is seen, or the count stays on the tile although the list was
                    // just read.
                    //
                    // twice, because one of them does nothing without the write right:
                    // `markMissedSeen` clears the system's flag when we may, and the
                    // remembered moment makes the count go out when we may not.
                    callLog.markMissedSeen()
                    val seenAt = MissedCalls.seenUpTo(
                        config.phone.lastSeenMissedAt,
                        groups.map { it.latest },
                    )
                    if (seenAt != config.phone.lastSeenMissedAt) {
                        store.update { it.copy(phone = it.phone.copy(lastSeenMissedAt = seenAt)) }
                    }
                } else {
                    // without the right nothing is read: the list is not loading then but
                    // locked, and a screen of its own says so.
                    loading = false
                    if (!logDeniedOnce) askLog.launch(Manifest.permission.READ_CALL_LOG)
                }
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
                // back in steps, as on the home screen: first the question, then the list.
                // back used to jump from the question straight to the keypad.
                BackHandler(enabled = tab != Tab.KEYPAD || pendingCall != null || pendingDelete != null) {
                    when {
                        pendingCall != null -> pendingCall = null
                        pendingDelete != null -> pendingDelete = null
                        else -> tab = Tab.KEYPAD
                    }
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(LocalBigPalette.current.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    val toConfirm = pinFor
                    if (toConfirm != null) {
                        PinGate(
                            title = stringResource(R.string.calllog_locked),
                            explainer = stringResource(R.string.calllog_locked_hint),
                            wrongText = stringResource(R.string.security_wrong_pin),
                            confirmLabel = stringResource(R.string.editor_done),
                            onCheck = { entered -> Pin.verify(entered, config.security.pin) },
                            onAccept = {
                                pinFor = null
                                deleteAfterPermission(toConfirm)
                            },
                            acceptOnComplete = true,
                        )
                        return@Box
                    }
                    when (tab) {
                        Tab.KEYPAD -> Keypad(
                            typed = typed,
                            hintFor = { key ->
                                // only while nothing is typed, or the speed-dial name lies
                                // over the number being entered.
                                if (typed.isEmpty()) SpeedDial.targetFor(config.phone, key)?.name else null
                            },
                            anySpeedDial = SpeedDial.anyAssigned(config.phone),
                            onDigit = { typed += it },
                            onLongDigit = { key ->
                                val target = SpeedDial.targetFor(config.phone, key)
                                when {
                                    target != null && typed.isEmpty() ->
                                        dial(target.number) { askCall.launch(Manifest.permission.CALL_PHONE) }
                                    SpeedDial.isAssignable(key) && typed.isEmpty() -> {
                                        assigningKey = key
                                        tab = Tab.ASSIGN
                                    }
                                    else -> Unit
                                }
                            },
                            onBackspace = { typed = typed.dropLast(1) },
                            onCall = { dial(typed) { askCall.launch(Manifest.permission.CALL_PHONE) } },
                            onLog = { tab = Tab.LOG },
                        )

                        Tab.ASSIGN -> AssignList(
                            key = assigningKey,
                            contacts = contacts,
                            assigned = assigningKey?.let { SpeedDial.targetFor(config.phone, it) },
                            onPick = { contact ->
                                val key = assigningKey
                                val number = contact.numbers.firstOrNull()?.number
                                if (key != null && number != null) {
                                    store.update {
                                        it.copy(
                                            phone = SpeedDial.assign(
                                                it.phone, key, SpeedDialTarget(contact.name, number),
                                            ),
                                        )
                                    }
                                }
                                tab = Tab.KEYPAD
                            },
                            onClear = {
                                assigningKey?.let { key ->
                                    store.update { it.copy(phone = SpeedDial.clear(it.phone, key)) }
                                }
                                tab = Tab.KEYPAD
                            },
                        )

                        Tab.LOG -> CallList(
                            loading = loading,
                            scrollButtons = config.behaviour.accessibility.scrollButtons,
                            // unfiltered in: the list must tell being empty from being
                            // filtered empty.
                            all = groups,
                            allowed = CallLogGrouping.allowedFrom(config.phone.hiddenCallTypes),
                            granted = logGranted,
                            blocked = PermissionState.blocked(logDeniedOnce, logCanAskAgain),
                            onAskLog = { askLog.launch(Manifest.permission.READ_CALL_LOG) },
                            onLogSettings = { Intents.appSettings(this@DialerActivity) },
                            writeGate = writeGate,
                            onWriteSettings = { Intents.appSettings(this@DialerActivity) },
                            onCloseWriteGate = { writeGate = false },
                            onNotCallable = {
                                Notice.show(
                                    this@DialerActivity,
                                    R.string.calllog_not_callable,
                                )
                            },
                            onCallTypeSettings = {
                                startActivity(
                                    SettingsLink.toPage(
                                        this@DialerActivity,
                                        SettingsLink.PAGE_CALL_TYPES,
                                    ),
                                )
                            },
                            missedOnly = missedOnly,
                            pendingDelete = pendingDelete,
                            pendingCall = pendingCall,
                            onToggleFilter = { missedOnly = !missedOnly },
                            onAskCall = { label, number -> pendingCall = label to number },
                            onCancelCall = { pendingCall = null },
                            onConfirmCall = {
                                val chosen = pendingCall?.second
                                pendingCall = null
                                if (chosen != null) {
                                    dial(chosen) { askCall.launch(Manifest.permission.CALL_PHONE) }
                                }
                            },
                            onAskDelete = { label, ids -> pendingDelete = label to ids },
                            onCancelDelete = { pendingDelete = null },
                            onConfirmDelete = {
                                val pending = pendingDelete ?: return@CallList
                                pendingDelete = null
                                if (Pin.protects(
                                        config.security.pin,
                                        config.security.pinProtectsCallLogDelete,
                                    )
                                ) {
                                    pinFor = pending
                                } else {
                                    deleteAfterPermission(pending)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    /**
     * dials, except for emergency numbers.
     *
     * those go to the system dialer, prefilled, and wait for one key press. some devices
     * refuse an emergency call from a third-party app outright and would swallow it
     * silently; one more press is the right price for that.
     */
    companion object {
        /** show the call log, every entry, unlike EXTRA_MISSED which filters. */
        const val EXTRA_LOG = "showLog"
        const val EXTRA_MISSED = "missedOnly"
    }

    private fun dial(number: String, onMissingPermission: () -> Unit) {
        if (!PhoneNumbers.isDialable(number)) return
        val platformSaysEmergency = runCatching {
            // the replacement needs READ_PHONE_STATE and does not exist on android 11. the
            // result is a hint anyway, see `PhoneNumbers.looksLikeEmergency`.
            @Suppress("DEPRECATION")
            PhoneNumberUtils.isEmergencyNumber(PhoneNumbers.clean(number))
        }.getOrDefault(false)

        if (PhoneNumbers.looksLikeEmergency(number, platformSaysEmergency)) {
            Notice.show(this, R.string.dialer_emergency_handover)
            startActivity(
                Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", PhoneNumbers.clean(number), null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }

        // after the emergency branch: a blocked number keeps nobody from an emergency call.
        if (CallBlocking.isBlocked(number, ConfigStore.get(this).current.phone.blockedNumbers)) {
            Notice.show(this, R.string.blocked_outgoing)
            return
        }

        val call = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", PhoneNumbers.clean(number), null))
        try {
            startActivity(call)
        } catch (e: SecurityException) {
            onMissingPermission()
        }
    }
}

@Composable
private fun Keypad(
    typed: String,
    hintFor: (Char) -> String?,
    anySpeedDial: Boolean,
    onDigit: (Char) -> Unit,
    onLongDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onCall: () -> Unit,
    onLog: () -> Unit,
) {
    val palette = LocalBigPalette.current
    val dialable = PhoneNumbers.isDialable(typed)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            // the row is measured for a phone number: a whole sentence in the same size ran
            // off the right edge, so it stands small below instead.
            if (typed.isEmpty()) {
                Column {
                    Text(
                        text = stringResource(R.string.dialer_hint),
                        color = palette.onBackground,
                        // deliberately without the text size setting: this head sits in a
                        // fixed layout over the keypad and its 28 dp come from the area. at
                        // 200 % only the first word was on screen.
                        fontSize = dpSp(28f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        // what holding does *now*, not what the feature is called: on an
                        // empty key it leads to assigning, on an assigned one it dials at
                        // once. one sentence covered both, the harmless and the dangerous.
                        text = stringResource(
                            if (anySpeedDial) {
                                R.string.dialer_speeddial_hint_call
                            } else {
                                R.string.dialer_speeddial_hint_assign
                            },
                        ),
                        color = palette.onBackground,
                        fontSize = dpSp(14f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                // measure the text that is actually drawn: the computation used `typed`
                // while the grouped version was drawn, longer by every gap, against a fixed
                // 330 dp instead of the real width. both cut the number without a mark.
                val shown = PhoneNumbers.forDisplay(typed)
                val numberStyle = tabularFigures().copy(fontWeight = FontWeight.Bold)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    Text(
                        text = shown,
                        color = palette.onBackground,
                        // read out digit by digit: as ordinary text "123" would become
                        // "one hundred twenty three". see PhoneNumbers.forSpeech.
                        modifier = Modifier.semantics {
                            contentDescription = PhoneNumbers.forSpeech(shown)
                        },
                        fontSize = dpSp(
                            fittedSingleLineDp(shown, numberStyle, 40f, maxWidth),
                        ),
                        // the number must not jump on every digit. `PLAN.md` 3.7.
                        style = numberStyle,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
        Box(Modifier.weight(1f)) {
            BigKeypad(
                onDigit = onDigit,
                onBackspace = onBackspace,
                onLongDigit = onLongDigit,
                hintFor = hintFor,
                extraKey = '+',
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigRow(
                label = stringResource(R.string.dialer_call),
                icon = Icons.Filled.Call,
                // without a number this is not a button.
                //
                // `dial` refuses anything that is not one (`isDialable`), which was right but
                // invisible: the row stood in full accent colour, one tapped, and nothing
                // happened silently. on a keypad that is the row one relies on most.
                surface = if (dialable) palette.surfaceAccent else palette.surfaceDefault,
                modifier = Modifier.weight(1f),
                onClick = if (dialable) onCall else null,
            )
            // an icon instead of a word: the label broke mid-word as soon as a speed dial
            // made the keypad taller. two labelled buttons do not fit side by side on three
            // inches, which is what BigIconButton is for; the word is in the description.
            BigIconButton(
                icon = Icons.Filled.History,
                description = stringResource(R.string.calllog),
                onClick = onLog,
            )
        }
    }
}

@Composable
private fun CallList(
    all: List<CallGroup>,
    allowed: Set<CallDirection>,
    granted: Boolean,
    blocked: Boolean,
    onAskLog: () -> Unit,
    onLogSettings: () -> Unit,
    missedOnly: Boolean,
    pendingDelete: Pair<String, List<Long>>?,
    pendingCall: Pair<String, String>?,
    onToggleFilter: () -> Unit,
    onAskCall: (String, String) -> Unit,
    onCancelCall: () -> Unit,
    onConfirmCall: () -> Unit,
    onAskDelete: (String, List<Long>) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    writeGate: Boolean,
    onWriteSettings: () -> Unit,
    onCloseWriteGate: () -> Unit,
    onCallTypeSettings: () -> Unit,
    /** an entry without a dialable number was tapped, a withheld one for instance. */
    onNotCallable: () -> Unit,
    scrollButtons: Boolean,
    loading: Boolean,
) {
    // stands in the row for a withheld number; a fixed "?" used to.
    val unknown = stringResource(R.string.call_unknown)
    // the lasting choice of kinds first, then the quick missed-only filter: that one is a
    // view, not a setting, and must not overwrite the other.
    val groups = CallLogGrouping.visible(
        if (missedOnly) CallLogGrouping.onlyMissed(all) else all,
        allowed,
    )
    val emptyBecause = CallLogEmpty.reason(all, missedOnly, allowed)
    val palette = LocalBigPalette.current
    val locale = currentLocale()
    val format = remember(locale) {
        // parts instead of a fixed pattern; see bestDatePattern. "j" is the hour *in the
        // language's own spelling*, while "H" forces 24 hours: the same minute read
        // "5:39 PM" in the header and "17:39" in the list.
        SimpleDateFormat(bestDatePattern("EEEdMMMjmm", locale), locale)
    }

    // android no longer asks for the write right. without this screen it would stay at
    // deleting being confirmed and nothing happening.
    if (writeGate) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                PermissionGate(
                    title = stringResource(R.string.calllog),
                    explanation = stringResource(R.string.calllog_write_permission),
                    blocked = true,
                    onAsk = onWriteSettings,
                    onSettings = onWriteSettings,
                )
            }
            item {
                BigRow(
                    label = stringResource(R.string.calllog_confirm_keep),
                    onClick = onCloseWriteGate,
                )
            }
        }
        return
    }

    // deleting cannot be undone, so it is asked, and the question covers the list: nobody
    // confirming should hit a row in passing.
    if (pendingDelete != null) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item { BigHeading(stringResource(R.string.calllog)) }
            item {
                Text(
                    text = if (pendingDelete.second.isEmpty()) {
                        stringResource(R.string.calllog_confirm_all)
                    } else {
                        stringResource(R.string.calllog_confirm_one, pendingDelete.first)
                    },
                    color = palette.onBackground,
                    fontSize = bigSp(17f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
            item {
                BigRow(
                    label = stringResource(R.string.calllog_confirm_delete),
                    surface = palette.surfaceDanger,
                    onClick = onConfirmDelete,
                )
            }
            item { BigRow(label = stringResource(R.string.calllog_confirm_keep), onClick = onCancelDelete) }
        }
        return
    }

    // the same shape as for deleting: the question covers the list.
    if (pendingCall != null) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item { BigHeading(stringResource(R.string.calllog)) }
            item {
                Text(
                    text = stringResource(R.string.calllog_call_confirm, pendingCall.first),
                    color = palette.onBackground,
                    fontSize = bigSp(17f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
            item {
                BigRow(
                    label = stringResource(R.string.calllog_call_yes),
                    icon = Icons.Filled.Call,
                    surface = palette.surfaceAccent,
                    onClick = onConfirmCall,
                )
            }
            item { BigRow(label = stringResource(R.string.calllog_call_no), onClick = onCancelCall) }
        }
        return
    }

    val listState = rememberLazyListState()
    Column {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            item { BigHeading(stringResource(if (missedOnly) R.string.missed_calls else R.string.calllog)) }
            item {
                BigRow(
                    label = stringResource(
                        if (missedOnly) R.string.calllog_show_missed else R.string.calllog_show_all,
                    ),
                    icon = Icons.Filled.CallMissed,
                    surface = if (missedOnly) palette.surfaceAccent else palette.surfaceDefault,
                    onClick = onToggleFilter,
                )
            }
            if (!granted) {
                item {
                    PermissionGate(
                        title = stringResource(R.string.calllog),
                        explanation = stringResource(R.string.calllog_permission),
                        blocked = blocked,
                        onAsk = onAskLog,
                        onSettings = onLogSettings,
                    )
                }
            } else if (groups.isNotEmpty()) {
                // deleting a single call worked only by holding, and that stood nowhere.
                // the hint sits where the speed-dial hint sits on the keypad.
                item {
                    Text(
                        text = stringResource(R.string.calllog_delete_hint),
                        color = palette.onBackground,
                        fontSize = bigSp(14f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            }
            if (granted && emptyBecause != null) {
                item {
                    Text(
                        // while reading, the list is not empty but not there yet.
                        text = stringResource(
                            if (loading) {
                                R.string.calllog_loading
                            } else {
                                when (emptyBecause) {
                                    EmptyCallLog.NO_CALLS -> R.string.calllog_empty
                                    EmptyCallLog.HIDDEN_BY_TYPE -> R.string.calllog_all_hidden
                                    EmptyCallLog.NO_MISSED -> R.string.calllog_no_missed
                                }
                            },
                        ),
                        color = palette.onBackground,
                        fontSize = bigSp(16f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
                // the way back belongs where one notices needing it, not in a setting that
                // has to be found first.
                if (emptyBecause == EmptyCallLog.HIDDEN_BY_TYPE) {
                    item {
                        BigRow(
                            label = stringResource(R.string.calllog_all_hidden_open),
                            icon = Icons.Filled.Settings,
                            surface = palette.surfaceAccent,
                            onClick = onCallTypeSettings,
                        )
                    }
                }
            }
            items(groups, key = { it.latest.id }) { group ->
                BigRow(
                    label = buildString {
                        append(group.name ?: PhoneNumbers.forDisplay(group.number).ifBlank { unknown })
                        if (group.count > 1) append(" (${group.count})")
                    },
                    // a *number* in place of a name is read digit by digit: otherwise a
                    // screen reader turns 222222 into one number, and someone checking whom
                    // they are about to call back learns nothing. a name stays a name.
                    labelSpeech = if (group.name == null) {
                        PhoneNumbers.forSpeech(group.number).ifBlank { unknown }
                    } else {
                        null
                    },
                    secondary = format.format(Date(group.latest.timestamp)),
                    secondaryMaxLines = 1,
                    // exhaustive for a solid reason: an else branch gave rejected and
                    // blocked calls the outgoing arrow, so a call one had pushed away stood
                    // there as one made oneself, in the one list where direction is
                    // everything.
                    icon = when (group.latest.direction) {
                        CallDirection.MISSED -> Icons.Filled.CallMissed
                        CallDirection.INCOMING -> Icons.Filled.CallReceived
                        CallDirection.OUTGOING -> Icons.Filled.CallMade
                        CallDirection.REJECTED -> Icons.Filled.CallEnd
                        CallDirection.BLOCKED -> Icons.Filled.Block
                        CallDirection.OTHER -> Icons.Filled.QuestionMark
                    },
                    surface = if (group.hasMissed) palette.surfaceDanger else palette.surfaceDefault,
                    // the arrow says the direction to the eye only: read aloud the row was
                    // just a name, a count and a time.
                    state = stringResource(callDirectionSpeech(group.latest.direction)),
                    // no immediate dialling: `PLAN.md` 3.1, principle 5 names calling from
                    // the log among what needs a question. in a list scanned with an unsteady
                    // hand a missed tap would otherwise be a call to somebody.
                    onClick = {
                        // a withheld number cannot be called back: the question used to
                        // name an empty pair of quotes, and a yes would have dialled nothing.
                        if (!PhoneNumbers.isDialable(group.number)) {
                            onNotCallable()
                        } else {
                            onAskCall(
                                group.name ?: PhoneNumbers.forDisplay(group.number),
                                group.number,
                            )
                        }
                    },
                    onLongClick = {
                        onAskDelete(
                            group.name ?: PhoneNumbers.forDisplay(group.number),
                            CallLogGrouping.idsOf(group),
                        )
                    },
                )
            }
            if (granted && groups.isNotEmpty()) {
                item {
                    BigRow(
                        label = stringResource(R.string.calllog_delete_all),
                        icon = Icons.Filled.Delete,
                        surface = palette.surfaceDanger,
                        // an empty id list means everything; the confirmation text tells them apart.
                        onClick = { onAskDelete("", emptyList()) },
                    )
                }
            }
        }
        if (scrollButtons) {
            ScrollButtons(listState, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** assign a speed dial: pick a contact, or free the key again. */
@Composable
private fun AssignList(
    key: Char?,
    contacts: List<PhoneContact>,
    assigned: SpeedDialTarget?,
    onPick: (PhoneContact) -> Unit,
    onClear: () -> Unit,
) {
    if (key == null) return
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BigHeading(stringResource(R.string.speeddial_assign, key.toString())) }
        // the same sentence as on the keypad, but *here*, where the decision is made: on
        // the keypad it appears only once a speed dial exists, so whoever sets up the first
        // one learnt afterwards what they were in for. and here HomeTiles departs from its own
        // rule, since a long press on a speed dial dials without asking.
        item {
            Text(
                text = stringResource(R.string.dialer_speeddial_hint_call),
                color = palette.onBackground,
                fontSize = bigSp(15f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
        if (assigned != null) {
            item {
                BigRow(
                    label = stringResource(R.string.speeddial_clear, assigned.name),
                    surface = palette.surfaceDanger,
                    onClick = onClear,
                )
            }
        }
        items(contacts, key = { it.id }) { contact ->
            BigRow(
                label = contact.name,
                secondary = contact.numbers.firstOrNull()?.number?.let(PhoneNumbers::forDisplay),
                secondaryMaxLines = 1,
                leading = { ContactAvatar(contact.name, contact.photoUri) },
                onClick = { onPick(contact) },
            )
        }
    }
}
