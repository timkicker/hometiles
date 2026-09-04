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
import org.biglau.ui.bestDatePattern
import org.biglau.ui.bigSp
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
import androidx.compose.material.icons.filled.History
import org.biglau.ui.BigIconButton
import org.biglau.ui.BigRow
import org.biglau.ui.SettingsLink
import org.biglau.ui.ScrollButtons
import org.biglau.ui.tabellenZiffern
import org.biglau.ui.dpSp
import org.biglau.ui.fittedSingleLineDp
import org.biglau.ui.theme.LocalTextScale
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
                        // `fortsetzungen` als Schluessel: dieser Bildschirm schickt den Nutzer bei
            // dauerhaft verweigerter Berechtigung in die **App-Einstellungen**, und von dort
            // kommt kein Ergebnis zurueck. Ohne das Neulesen beim Wiederkommen stuende hier
            // weiter „keine Berechtigung" - auf einem Bildschirm, der einen selbst dorthin
            // geschickt hat. Siehe `BigLauActivity.fortsetzungen`.
var logGranted by remember(fortsetzungen.intValue) { mutableStateOf(callLog.hasPermission()) }
            var assigningKey by remember { mutableStateOf<Char?>(null) }
            var missedOnly by rememberSaveable { mutableStateOf(intent?.getBooleanExtra(EXTRA_MISSED, false) == true) }
            var pendingDelete by remember { mutableStateOf<Pair<String, List<Long>>?>(null) }
            // Die Rueckfrage vor einem Anruf aus dem Verlauf. PLAN.md 3.1, Leitsatz 5.
            var pendingCall by remember { mutableStateOf<Pair<String, String>?>(null) }
            // Erst die Rueckfrage - sie sagt, was verschwindet -, dann die PIN. So steht
            // das Schloss unmittelbar vor dem Schritt, der nicht rueckgaengig zu machen ist,
            // und man weiss beim Eintippen, wofuer.
            var pinFor by remember { mutableStateOf<Pair<String, List<Long>>?>(null) }
            val scope = rememberCoroutineScope()
            var contacts by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
            val contactRepo = remember { ContactRepository.get(this@DialerActivity) }

            // Das Recht, die Anrufliste zu *aendern*, ist ein zweites neben dem Lesen, und
            // niemand hat es je erfragt: am 03.09.2026 stand es auf dem Geraet des Nutzers
            // auf granted=false. Wer dort einen Eintrag loeschte, bestaetigte die
            // Rueckfrage und sah die Zeile danach unveraendert stehen - eine Sackgasse ohne
            // Ausweg, denn nichts fragte.
            //
            // **Nachtrag 04.09.2026:** seit BigLau die Telefon-Rolle haelt, erteilt Android
            // das Recht mit. Der Weg hier bleibt trotzdem: er darf an keiner Rolle haengen.
            var writeDeniedOnce by remember { mutableStateOf(false) }
            var writeCanAskAgain by remember { mutableStateOf(true) }
            // Wenn Android nicht mehr fragt, bleibt nur der Weg ueber die Systemeinstellungen.
            var writeGate by remember { mutableStateOf(false) }
            var wartetAufSchreibrecht by remember { mutableStateOf<Pair<String, List<Long>>?>(null) }

            /** Das eigentliche Loeschen - hinter Rueckfrage, Schreibrecht und, wenn gesetzt, PIN. */
            fun deleteNow(pending: Pair<String, List<Long>>) {
                scope.launch {
                    val removed = if (pending.second.isEmpty()) {
                        callLog.deleteAll()
                    } else {
                        callLog.delete(pending.second)
                    }
                    // Die Liste in jedem Fall neu lesen: sie ist die Wahrheit, nicht die
                    // Zahl, die der Anbieter zurueckgibt.
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
                val pending = wartetAufSchreibrecht
                wartetAufSchreibrecht = null
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
             * Erst fragen, dann loeschen.
             *
             * Gefragt wird hier und nicht beim Oeffnen der Liste: wer nur nachsieht, wer
             * angerufen hat, soll nicht gefragt werden, ob BigLau die Liste aendern darf.
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
                        wartetAufSchreibrecht = pending
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

            // Beim ersten Blick in die Anrufliste fragt das System von selbst. Nach einer
            // Ablehnung nicht mehr - sonst stuende dort nur ein Satz und kein Knopf.
            // **Solange gelesen wird, sagt die Liste nicht, sie sei leer.**
            //
            // `groups` faengt leer an, und `CallLogEmpty.reason` macht daraus „Noch keine
            // Anrufe." - eine Falschaussage, solange der Anbieter noch liest. Auf diesem
            // Telefon ist das kurz; auf einem mit tausend Eintraegen und eingeschalteter
            // Gruppierung ist es zu sehen. Dieselbe Luecke wie in der Nachrichtenliste,
            // dieselbe Loesung. Siehe LadenTest.
            var laedt by remember { mutableStateOf(true) }
            LaunchedEffect(tab, logGranted) {
                if (tab != Tab.LOG) return@LaunchedEffect
                if (logGranted) {
                    groups = callLog.load(mode = config.phone.callGrouping)
                    laedt = false
                    // Gesehen ist gesehen: sonst stuende die Zahl weiter auf der Kachel,
                    // obwohl der Nutzer die Liste gerade gelesen hat.
                    //
                    // Zweimal, weil das eine ohne Schreibrecht nichts tut: `markMissedSeen`
                    // raeumt das Kennzeichen des Systems auf, wenn wir duerfen - und der
                    // gemerkte Zeitpunkt sorgt dafuer, dass die Zahl auch dann erlischt,
                    // wenn wir nicht duerfen. Siehe MissedCalls.
                    callLog.markMissedSeen()
                    val gesehen = MissedCalls.seenUpTo(
                        config.phone.lastSeenMissedAt,
                        groups.map { it.latest },
                    )
                    if (gesehen != config.phone.lastSeenMissedAt) {
                        store.update { it.copy(phone = it.phone.copy(lastSeenMissedAt = gesehen)) }
                    }
                } else {
                    // Ohne Recht wird nicht gelesen - dann ist die Liste nicht am Laden,
                    // sondern gesperrt, und darueber steht ohnehin ein eigener Bildschirm.
                    laedt = false
                    if (!logDeniedOnce) askLog.launch(Manifest.permission.READ_CALL_LOG)
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
                hideCutLabels = config.appearance.hideCutLabels,
                cornerRadiusDp = config.appearance.cornerRadiusDp,
            ) {
                // In Stufen zurueck, wie auf dem Startbildschirm: erst die Rueckfrage weg,
                // dann die Liste. Vorher sprang Zurueck aus der Rueckfrage gleich zur
                // Waehltastatur - man landete zwei Schritte weiter weg, als man wollte.
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
                                deleteAfterPermission(zuBestaetigen)
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
                            kurzwahlBelegt = SpeedDial.anyAssigned(config.phone),
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
                            laedt = laedt,
                            scrollButtons = config.behaviour.accessibility.scrollButtons,
                            // Ungefiltert hinein: die Liste muss unterscheiden koennen, ob
                            // sie leer ist oder leer gefiltert wurde.
                            alle = groups,
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
                                val gewaehlt = pendingCall?.second
                                pendingCall = null
                                if (gewaehlt != null) {
                                    dial(gewaehlt) { askCall.launch(Manifest.permission.CALL_PHONE) }
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
            // `isEmergencyNumber` ist seit Android 12 zugunsten von
            // `TelephonyManager.isEmergencyNumber` abgelöst - das braucht READ_PHONE_STATE
            // und gibt es auf Android 11 noch nicht. Das Ergebnis ist ohnehin nur ein
            // Hinweis, siehe `PhoneNumbers.looksLikeEmergency`.
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

        // Erst nach dem Notruf-Zweig: eine gesperrte Nummer haelt niemanden vom Notruf ab.
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
    kurzwahlBelegt: Boolean,
    onDigit: (Char) -> Unit,
    onLongDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onCall: () -> Unit,
    onLog: () -> Unit,
) {
    val palette = LocalBigPalette.current
    val waehlbar = PhoneNumbers.isDialable(typed)
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
                        // Bewusst **ohne** die eingestellte Textgroesse: dieser Kopf sitzt
                        // in einem festen Aufbau ueber der Tastatur, seine 28 dp sind aus
                        // der Flaeche gerechnet. Mit 200 % ausprobiert - dann stand dort
                        // "Nummer" und der Rest lag ausserhalb des Bildes, und der Hinweis
                        // darunter war ganz verschwunden.
                        fontSize = dpSp(28f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        // Was das Halten **jetzt** tut, nicht was die Funktion heisst: auf
                        // einer leeren Taste fuehrt es ins Belegen, auf einer belegten
                        // waehlt es sofort. Bis zum 04.09.2026 stand in beiden Faellen
                        // derselbe Satz - im harmlosen Zustand derselbe wie im
                        // gefaehrlichen.
                        text = stringResource(
                            if (kurzwahlBelegt) {
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
                // Gemessen wird der Text, der auch dasteht: gerechnet wurde bisher mit
                // `typed`, gezeichnet aber die gruppierte Fassung - die ist um jede Luecke
                // laenger. Dazu eine feste Breite von 330 dp statt der wirklichen. Beides
                // schnitt die Nummer ab, ohne ein Zeichen dafuer zu setzen.
                val gezeigt = PhoneNumbers.forDisplay(typed)
                val nummerStil = tabellenZiffern().copy(fontWeight = FontWeight.Bold)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    Text(
                        text = gezeigt,
                        color = palette.onBackground,
                        // Vorgelesen ziffernweise: als gewoehnlicher Text wuerde aus "123"
                        // ein "einhundertdreiundzwanzig". Siehe PhoneNumbers.forSpeech.
                        modifier = Modifier.semantics {
                            contentDescription = PhoneNumbers.forSpeech(gezeigt)
                        },
                        fontSize = dpSp(
                            fittedSingleLineDp(gezeigt, nummerStil, 40f, maxWidth),
                        ),
                        // Beim Tippen soll die Zahl nicht bei jeder Ziffer springen. PLAN.md 3.7.
                        style = nummerStil,
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
                // Ohne Nummer ist der Knopf keiner.
                //
                // `dial` weigert sich bei etwas, das keine Nummer ist
                // (`isDialable`) - richtig, aber bis zum 04.09.2026 sah man das nicht: die
                // Zeile stand in voller Akzentfarbe da, man tippte, und es geschah
                // schweigend nichts. Auf der Waehltastatur ist das die Zeile, auf die man
                // sich am meisten verlaesst. Derselbe Fall wie beim Senden-Knopf in den
                // Nachrichten.
                surface = if (waehlbar) palette.surfaceAccent else palette.surfaceDefault,
                modifier = Modifier.weight(1f),
                onClick = if (waehlbar) onCall else null,
            )
            // Ein Symbol statt eines Wortes: "Anrufliste" brach hier mitten im Wort um,
            // sobald eine Kurzwahl die Tastatur hoeher macht. Zwei beschriftete Knoepfe
            // passen auf drei Zoll nicht nebeneinander - genau der Fall, fuer den es
            // BigIconButton gibt. Das Wort steht in der Vorlese-Beschreibung.
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
    alle: List<CallGroup>,
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
    /** Angetippt wurde ein Eintrag ohne waehlbare Nummer - eine unterdrueckte etwa. */
    onNotCallable: () -> Unit,
    scrollButtons: Boolean,
    laedt: Boolean,
) {
    // Steht in der Zeile, wenn die Nummer unterdrueckt war - vorher ein festes "?".
    val unbekannt = stringResource(R.string.call_unknown)
    // Erst die dauerhafte Auswahl der Arten, dann der schnelle Filter "nur verpasste" -
    // der ist eine Ansicht, keine Einstellung, und darf die andere nicht ueberschreiben.
    val groups = CallLogGrouping.visible(
        if (missedOnly) CallLogGrouping.onlyMissed(alle) else alle,
        allowed,
    )
    val leerWeil = CallLogEmpty.reason(alle, missedOnly, allowed)
    val palette = LocalBigPalette.current
    val locale = currentLocale()
    val format = remember(locale) {
        // Bestandteile statt festem Muster - siehe bestDatePattern. Das "j" ist die
        // Stunde **in der Schreibweise der Sprache**: ein "H" erzwaengt 24 Stunden, und
        // genau das stand hier bis zum 03.09.2026. Auf diesem Telefon, das auf
        // 12 Stunden steht, hiess dieselbe Minute in der Kopfzeile "5:39 PM" und in der
        // Liste "17:39".
        SimpleDateFormat(bestDatePattern("EEEdMMMjmm", locale), locale)
    }

    // Android fragt nicht mehr nach dem Schreibrecht. Ohne diesen Bildschirm bliebe es
    // dabei, dass Loeschen bestaetigt wird und nichts geschieht.
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

    // Dieselbe Form wie beim Loeschen: die Frage verdeckt die Liste, damit niemand
    // nebenbei auf eine andere Zeile tippt.
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
                // Einen einzelnen Anruf loeschte man bisher nur durch langes Halten, und
                // das stand nirgends. Der Hinweis sitzt an derselben Stelle wie der fuer
                // die Kurzwahl auf der Wähltastatur.
                item {
                    Text(
                        text = stringResource(R.string.calllog_delete_hint),
                        color = palette.onBackground,
                        fontSize = bigSp(14f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            }
            if (granted && leerWeil != null) {
                item {
                    Text(
                        // Solange gelesen wird, ist die Liste nicht leer, sondern noch
                        // nicht da. Der Grund kommt danach.
                        text = stringResource(
                            if (laedt) {
                                R.string.calllog_loading
                            } else {
                                when (leerWeil) {
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
                // Der Weg zurueck gehoert an die Stelle, an der man merkt, dass man ihn
                // braucht - nicht in eine Einstellung, die man erst finden muss.
                if (leerWeil == EmptyCallLog.HIDDEN_BY_TYPE) {
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
                        append(group.name ?: PhoneNumbers.forDisplay(group.number).ifBlank { unbekannt })
                        if (group.count > 1) append(" (${group.count})")
                    },
                    // Steht statt eines Namens eine **Nummer** da, wird sie ziffernweise
                    // gelesen: sonst macht ein Vorleseprogramm aus "222222" eine Zahl,
                    // und wer vor dem Rueckruf nachhoeren will, wen er da anruft, erfaehrt
                    // es nicht. Ein Name bleibt ein Name. Am 04.09.2026 am Emulator gesehen.
                    labelSpeech = if (group.name == null) {
                        PhoneNumbers.forSpeech(group.number).ifBlank { unbekannt }
                    } else {
                        null
                    },
                    secondary = format.format(Date(group.latest.timestamp)),
                    secondaryMaxLines = 1,
                    // Erschoepfend, und zwar aus einem handfesten Grund: der else-Zweig
                    // gab abgewiesenen und blockierten Anrufen den Pfeil fuer ausgehende.
                    // Ein Anruf, den man weggedrueckt hat, stand da als einer, den man
                    // selbst gefuehrt hat - in der einen Liste, in der die Richtung alles
                    // ist. Aufgefallen erst am Emulator mit einer echten Anrufliste.
                    icon = when (group.latest.direction) {
                        CallDirection.MISSED -> Icons.Filled.CallMissed
                        CallDirection.INCOMING -> Icons.Filled.CallReceived
                        CallDirection.OUTGOING -> Icons.Filled.CallMade
                        CallDirection.REJECTED -> Icons.Filled.CallEnd
                        CallDirection.BLOCKED -> Icons.Filled.Block
                        CallDirection.OTHER -> Icons.Filled.QuestionMark
                    },
                    surface = if (group.hasMissed) palette.surfaceDanger else palette.surfaceDefault,
                    // Der Pfeil sagt die Richtung, aber nur dem Auge. Vorgelesen hiess die
                    // Zeile bis zum 04.09.2026 nur "Mama (3), 02:31" - in der einen Liste,
                    // in der die Richtung alles ist.
                    state = stringResource(callDirectionSpeech(group.latest.direction)),
                    // Nicht sofort waehlen: PLAN.md 3.1, Leitsatz 5 nennt "Anrufen aus dem
                    // Verlauf" ausdruecklich unter dem, was eine Rueckfrage braucht. In
                    // einer Liste, die man mit zittriger Hand durchsieht, ist ein Tipp
                    // daneben sonst ein Anruf bei jemandem.
                    onClick = {
                        // Eine unterdrueckte Nummer laesst sich nicht zurueckrufen. Vorher
                        // stand hier die Rueckfrage `„" jetzt anrufen?` - mit leeren
                        // Anfuehrungszeichen, und ein Ja haette nichts gewaehlt.
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
        // Derselbe Satz wie auf dem Tastenfeld - aber **hier**, wo entschieden wird. Auf dem
        // Tastenfeld steht er erst, wenn schon eine Kurzwahl belegt ist; wer die erste
        // einrichtet, erfuhr also erst hinterher, worauf er sich einlaesst. Und ausgerechnet
        // hier weicht BigLau von seiner eigenen Regel ab: ueberall sonst wird vor dem
        // Anrufen gefragt (`PLAN.md` 3.1, Leitsatz 5), beim Langdruck auf eine Kurzwahl
        // nicht.
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
