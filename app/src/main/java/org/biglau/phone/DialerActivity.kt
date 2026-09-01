package org.biglau.phone

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
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.ui.BigLauActivity
import org.biglau.ui.currentLocale
import org.biglau.R
import kotlinx.coroutines.launch
import org.biglau.contacts.ContactRepository
import org.biglau.contacts.PhoneContact
import org.biglau.data.ConfigStore
import org.biglau.data.SpeedDialTarget
import org.biglau.ui.ContactAvatar
import org.biglau.ui.BigHeading
import org.biglau.ui.BigKeypad
import org.biglau.actions.Intents
import org.biglau.security.Pin
import org.biglau.ui.PinGate
import org.biglau.ui.Notice
import org.biglau.ui.PermissionGate
import org.biglau.ui.PermissionState
import org.biglau.ui.BigRow
import org.biglau.ui.ScrollButtons
import org.biglau.ui.dpSp
import org.biglau.ui.singleLineSizeSp
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Tab { KEYPAD, LOG, ASSIGN }

/**
 * Waehltastatur und Anrufliste.
 *
 * Gewaehlt wird ueber ACTION_CALL, die Gespraechsansicht bleibt vorerst die des Systems.
 * Notrufnummern gehen ausdruecklich *nicht* diesen Weg, siehe [dial].
 */
class DialerActivity : BigLauActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)
        val callLog = CallLogRepository.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            var tab by rememberSaveable { mutableStateOf(Tab.KEYPAD) }
            // Kommt die App ueber tel:… herein, steht die Nummer schon da.
            var typed by rememberSaveable {
                mutableStateOf(intent?.data?.schemeSpecificPart?.let(PhoneNumbers::clean).orEmpty())
            }
            var groups by remember { mutableStateOf<List<CallGroup>>(emptyList()) }
            var logGranted by remember { mutableStateOf(callLog.hasPermission()) }
            var assigningKey by remember { mutableStateOf<Char?>(null) }
            var missedOnly by rememberSaveable { mutableStateOf(intent?.getBooleanExtra(EXTRA_MISSED, false) == true) }
            var pendingDelete by remember { mutableStateOf<Pair<String, List<Long>>?>(null) }
            // Erst die Rueckfrage - sie sagt, was verschwindet -, dann die PIN. So steht
            // das Schloss unmittelbar vor dem Schritt, der nicht rueckgaengig zu machen ist,
            // und man weiss beim Eintippen, wofuer.
            var pinFor by remember { mutableStateOf<Pair<String, List<Long>>?>(null) }
            val scope = rememberCoroutineScope()
            var contacts by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
            val contactRepo = remember { ContactRepository.get(this@DialerActivity) }

            /** Das eigentliche Loeschen - hinter Rueckfrage und, wenn gesetzt, PIN. */
            fun deleteNow(pending: Pair<String, List<Long>>) {
                scope.launch {
                    val removed = if (pending.second.isEmpty()) {
                        callLog.deleteAll()
                    } else {
                        callLog.delete(pending.second)
                    }
                    if (removed > 0) {
                        groups = callLog.load()
                        Notice.show(
                            this@DialerActivity,
                            getString(R.string.calllog_deleted, removed),
                        )
                    } else {
                        Notice.show(this@DialerActivity, R.string.calllog_delete_denied)
                    }
                }
            }

            LaunchedEffect(tab) {
                if (tab == Tab.ASSIGN && contacts.isEmpty()) contacts = contactRepo.load()
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

            // Beim ersten Blick in die Anrufliste fragt das System von selbst. Nach einer
            // Ablehnung nicht mehr - sonst stuende dort nur ein Satz und kein Knopf.
            LaunchedEffect(tab, logGranted) {
                if (tab != Tab.LOG) return@LaunchedEffect
                if (logGranted) {
                    groups = callLog.load()
                } else if (!logDeniedOnce) {
                    askLog.launch(Manifest.permission.READ_CALL_LOG)
                }
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
                BackHandler(enabled = tab != Tab.KEYPAD) { tab = Tab.KEYPAD }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(LocalBigPalette.current.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    val zuBestaetigen = pinFor
                    if (zuBestaetigen != null) {
                        PinGate(
                            title = stringResource(R.string.calllog_locked),
                            explainer = stringResource(R.string.calllog_locked_hint),
                            wrongText = stringResource(R.string.security_wrong_pin),
                            confirmLabel = stringResource(R.string.editor_done),
                            onCheck = { eingabe -> Pin.verify(eingabe, config.security.pin) },
                            onAccept = {
                                pinFor = null
                                deleteNow(zuBestaetigen)
                            },
                            acceptOnComplete = true,
                        )
                        return@Box
                    }
                    when (tab) {
                        Tab.KEYPAD -> Keypad(
                            typed = typed,
                            hintFor = { key ->
                                // Nur solange nichts getippt ist: sonst ueberlagert der
                                // Kurzwahlname die Nummer, die gerade entsteht.
                                if (typed.isEmpty()) SpeedDial.targetFor(config.phone, key)?.name else null
                            },
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
                            scrollButtons = config.behaviour.accessibility.scrollButtons,
                            groups = if (missedOnly) CallLogGrouping.onlyMissed(groups) else groups,
                            granted = logGranted,
                            blocked = PermissionState.blocked(logDeniedOnce, logCanAskAgain),
                            onAskLog = { askLog.launch(Manifest.permission.READ_CALL_LOG) },
                            onLogSettings = { Intents.appSettings(this@DialerActivity) },
                            missedOnly = missedOnly,
                            pendingDelete = pendingDelete,
                            onToggleFilter = { missedOnly = !missedOnly },
                            onCall = { number -> dial(number) { askCall.launch(Manifest.permission.CALL_PHONE) } },
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
                                    deleteNow(pending)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    /**
     * Waehlt - ausser bei Notrufnummern.
     *
     * Die gehen an den System-Dialer, vorbelegt, mit einem Tastendruck des Nutzers. Manche
     * Geraete lassen einen Notruf aus einer Fremd-App gar nicht erst zu und wuerden ihn
     * still verschlucken. Ein Tastendruck mehr ist der richtige Preis dafuer.
     */
    companion object {
        /** Anrufliste zeigen, alle Eintraege - anders als EXTRA_MISSED, das filtert. */
        const val EXTRA_LOG = "showLog"
        const val EXTRA_MISSED = "missedOnly"
    }

    private fun dial(number: String, onMissingPermission: () -> Unit) {
        if (!PhoneNumbers.isDialable(number)) return
        val platformSaysEmergency = runCatching {
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
    onDigit: (Char) -> Unit,
    onLongDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onCall: () -> Unit,
    onLog: () -> Unit,
) {
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            // Die Zeile ist auf eine Rufnummer bemessen. Ein ganzer Hinweissatz in
            // derselben Groesse lief rechts aus dem Bild - er steht deshalb klein darunter
            // und nicht mehr an der Stelle der Nummer.
            if (typed.isEmpty()) {
                Column {
                    Text(
                        text = stringResource(R.string.dialer_hint),
                        color = palette.onBackground,
                        fontSize = dpSp(28f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(R.string.dialer_speeddial_hint),
                        color = palette.onBackground,
                        fontSize = dpSp(14f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Text(
                    text = PhoneNumbers.forDisplay(typed),
                    color = palette.onBackground,
                    fontSize = dpSp(singleLineSizeSp(typed, 330f, 64f, 1f, maxSp = 40f)),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
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
                surface = palette.surfaceAccent,
                modifier = Modifier.weight(1f),
                onClick = onCall,
            )
            BigRow(
                label = stringResource(R.string.calllog),
                icon = Icons.Filled.Dialpad,
                modifier = Modifier.weight(0.7f),
                onClick = onLog,
            )
        }
    }
}

@Composable
private fun CallList(
    groups: List<CallGroup>,
    granted: Boolean,
    blocked: Boolean,
    onAskLog: () -> Unit,
    onLogSettings: () -> Unit,
    missedOnly: Boolean,
    pendingDelete: Pair<String, List<Long>>?,
    onToggleFilter: () -> Unit,
    onCall: (String) -> Unit,
    onAskDelete: (String, List<Long>) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    scrollButtons: Boolean,
) {
    val palette = LocalBigPalette.current
    val locale = currentLocale()
    val format = remember(locale) { SimpleDateFormat("EEE d. MMM, HH:mm", locale) }

    // Loeschen ist nicht rueckgaengig zu machen, also wird gefragt - und zwar so, dass
    // die Frage die Liste verdeckt: wer bestaetigt, soll nicht nebenbei auf eine Zeile tippen.
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
                    fontSize = 17.sp,
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
            } else if (groups.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.calllog_empty),
                        color = palette.onBackground,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }
            items(groups, key = { it.latest.id }) { group ->
                BigRow(
                    label = buildString {
                        append(group.name ?: PhoneNumbers.forDisplay(group.number).ifBlank { "?" })
                        if (group.count > 1) append(" (${group.count})")
                    },
                    secondary = format.format(Date(group.latest.timestamp)),
                    secondaryMaxLines = 1,
                    icon = when (group.latest.direction) {
                        CallDirection.MISSED -> Icons.Filled.CallMissed
                        CallDirection.INCOMING -> Icons.Filled.CallReceived
                        else -> Icons.Filled.CallMade
                    },
                    surface = if (group.hasMissed) palette.surfaceDanger else palette.surfaceDefault,
                    onClick = { onCall(group.number) },
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
                        // Leere Kennungsliste heisst "alles" - der Bestaetigungstext unterscheidet.
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

/** Kurzwahl belegen: Kontakt waehlen, oder die Taste wieder frei machen. */
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
                secondary = contact.numbers.firstOrNull()?.number,
                secondaryMaxLines = 1,
                leading = { ContactAvatar(contact.name, contact.photoUri) },
                onClick = { onPick(contact) },
            )
        }
    }
}
