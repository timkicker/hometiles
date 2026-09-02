package org.biglau.sms

import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build
import android.content.ContentValues
import android.provider.Telephony
import android.os.Bundle
import android.telephony.SmsManager
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.ui.theme.LocalCornerRadius
import org.biglau.ui.BigLauActivity
import org.biglau.ui.currentLocale
import org.biglau.R
import org.biglau.contacts.ContactRepository
import org.biglau.data.ConfigStore
import org.biglau.phone.PhoneNumbers
import org.biglau.toggles.SosMessage
import org.biglau.ui.BigHeading
import org.biglau.actions.Intents
import org.biglau.ui.Notice
import org.biglau.ui.PermissionGate
import org.biglau.ui.PermissionState
import org.biglau.ui.BigRow
import org.biglau.ui.ScrollButtons
import org.biglau.notify.SmsNotifications
import org.biglau.ui.dpSp
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.style.TextAlign
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Nachrichten: Liste der Gespraeche, eine Unterhaltung, und das Schreiben.
 *
 * Lesen geht mit READ_SMS auch ohne die Standard-SMS-Rolle. Senden geht ebenfalls, aber die
 * gesendete Nachricht landet dann nicht in der Datenbank - das kann nur die Standard-App.
 * Die Oberflaeche sagt das, statt eine Nachricht zu zeigen, die nach dem Neustart weg ist.
 */
class SmsActivity : BigLauActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)
        val repository = SmsRepository.get(this)
        val contacts = ContactRepository.get(this)

        val prefilledAddress = intent?.data?.schemeSpecificPart?.let(PhoneNumbers::clean)
        // Aus der Meldung ueber eine neue Nachricht: dann soll genau diese Unterhaltung
        // aufgehen und nicht die Liste, in der man sie erst suchen muss.
        val gemeldeteNummer = intent?.getStringExtra(EXTRA_ADDRESS)
        // Nur wenn die Meldung selbst den Bildschirm genommen hat. Fest im Manifest waere
        // es eine andere Zusage: dann laege jede Unterhaltung ueber dem Sperrbildschirm,
        // auch die, die jemand von Hand geoeffnet und liegen gelassen hat.
        if (intent?.getBooleanExtra(EXTRA_FULL_SCREEN, false) == true) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            val changes by SmsRepository.changes.collectAsStateWithLifecycle()
            var messages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
            var names by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
            var openThread by remember { mutableStateOf<Long?>(null) }
            var draft by remember { mutableStateOf("") }
            var granted by remember { mutableStateOf(repository.hasReadPermission()) }

            // Ohne Rueckfrage-Oberflaeche: sagt jemand nein, bleibt die Liste die Stelle,
            // an der er nachsieht. Ein zweiter Sackgassen-Bildschirm dafuer waere zu viel.
            val fragenWegenMeldungen = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { }

            var deniedOnce by remember { mutableStateOf(false) }
            var canAskAgain by remember { mutableStateOf(true) }
            val ask = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { result ->
                granted = result
                if (!result) {
                    deniedOnce = true
                    canAskAgain = shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)
                }
            }

            // Beim ersten Oeffnen fragt das System von selbst - das erwartet man so. Nach
            // einer Ablehnung nicht mehr: dann steht der Knopf da und der Nutzer entscheidet,
            // wann er es noch einmal versucht.
            LaunchedEffect(Unit) {
                if (!granted && !deniedOnce) ask.launch(Manifest.permission.READ_SMS)
            }

            // Ab Android 13 darf ohne diese Zusage keine Meldung erscheinen - eine neue
            // Nachricht kaeme dann still an. Das Zielgeraet laeuft auf Android 11, wo das
            // System sie beim Installieren erteilt; gefragt wird trotzdem, weil die App
            // auch auf neueren Geraeten laufen soll.
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@SmsActivity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    fragenWegenMeldungen.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            LaunchedEffect(granted, changes) {
                if (!granted) return@LaunchedEffect
                // Gefiltertes gar nicht erst in die Liste lassen - PLAN.md 4.7.
                messages = SmsFilter.apply(
                    repository.load(),
                    config.sms.hiddenNumbers,
                    config.sms.hiddenWords,
                )
                names = contacts.load().flatMap { contact ->
                    contact.numbers.map { PhoneNumbers.clean(it.number) to contact.name }
                }.toMap()
            }

            val threads = remember(messages, names) {
                SmsThreads.group(messages) { names[PhoneNumbers.clean(it)] }
            }

            // Gelesen ist gelesen: sonst stuende die Zahl neben dem Namen fuer immer da.
            // Die Meldung dazu geht mit weg - wer die Unterhaltung offen hat, hat sie
            // gesehen.
            LaunchedEffect(openThread) {
                val offen = openThread ?: return@LaunchedEffect
                if (repository.markRead(offen)) SmsRepository.notifyChanged()
                threads.firstOrNull { it.threadId == offen }?.let {
                    SmsNotifications.clear(this@SmsActivity, it.address)
                }
            }

            // Erst wenn die Nachrichten da sind, laesst sich die gemeldete Nummer einer
            // Unterhaltung zuordnen. Die Meldung selbst geht dabei weg - gesehen ist gesehen.
            LaunchedEffect(threads, gemeldeteNummer) {
                val nummer = gemeldeteNummer ?: return@LaunchedEffect
                if (openThread != null) return@LaunchedEffect
                val passend = threads.firstOrNull {
                    PhoneNumbers.clean(it.address) == PhoneNumbers.clean(nummer)
                }
                if (passend != null) {
                    openThread = passend.threadId
                    SmsNotifications.clear(this@SmsActivity, nummer)
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
                val palette = LocalBigPalette.current
                BackHandler(enabled = openThread != null) { openThread = null }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(palette.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    val thread = openThread
                    when {
                        !granted -> PermissionGate(
                            title = stringResource(R.string.messages),
                            explanation = stringResource(R.string.sms_permission),
                            blocked = PermissionState.blocked(deniedOnce, canAskAgain),
                            onAsk = { ask.launch(Manifest.permission.READ_SMS) },
                            onSettings = { Intents.appSettings(this@SmsActivity) },
                        )

                        thread != null -> Conversation(
                            messages = SmsThreads.conversation(messages, thread),
                            title = threads.firstOrNull { it.threadId == thread }?.title.orEmpty(),
                            draft = draft,
                            isDefaultApp = repository.isDefaultSmsApp(),
                            onDraft = { draft = it },
                            onSend = {
                                val address = messages.firstOrNull { it.threadId == thread }?.address
                                if (address != null) send(address, draft) { draft = "" }
                            },
                            conversationScale = config.sms.conversationScale,
                            confirmBeforeSending = config.sms.confirmBeforeSending,
                            sendButtonAbove = config.sms.sendButtonAbove,
                            sendButtonLarge = config.sms.sendButtonLarge,
                        )

                        else -> ThreadList(
                            threads = threads,
                            prefilled = prefilledAddress,
                            scrollButtons = config.behaviour.accessibility.scrollButtons,
                            onOpen = { openThread = it },
                        )
                    }
                }
            }
        }
    }

    companion object {
        /** Die Nummer, deren Unterhaltung aufgehen soll. Siehe [org.biglau.notify.SmsNotifications]. */
        const val EXTRA_ADDRESS = "biglau.sms.address"

        /** Kommt die Nachricht als Vollbild-Meldung, darf sie ueber den Sperrbildschirm. */
        const val EXTRA_FULL_SCREEN = "biglau.sms.fullscreen"
    }

    private fun send(address: String, body: String, onSent: () -> Unit) {
        if (body.isBlank()) return
        val manager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        val sent = runCatching {
            val parts = manager.divideMessage(body)
            if (parts.size > 1) {
                manager.sendMultipartTextMessage(address, null, parts, null, null)
            } else {
                manager.sendTextMessage(address, null, body, null, null)
            }
            true
        }.getOrDefault(false)

        if (sent) {
            // Mit der Rolle legt Android die gesendete Nachricht nicht mehr selbst ab.
            // Ohne diese Zeilen zeigte die Unterhaltung nur noch die Gegenseite.
            if (SmsDelivery.mayWrite(Telephony.Sms.getDefaultSmsPackage(this), packageName)) {
                runCatching {
                    contentResolver.insert(
                        Telephony.Sms.Sent.CONTENT_URI,
                        ContentValues().apply {
                            SmsOutbox.values(address, body, System.currentTimeMillis())
                                .forEach { (spalte, wert) ->
                                    when (wert) {
                                        is Long -> put(spalte, wert)
                                        is Int -> put(spalte, wert)
                                        else -> put(spalte, wert.toString())
                                    }
                                }
                        },
                    )
                }
                SmsRepository.notifyChanged()
            }
            onSent()
            Notice.show(this, R.string.sms_sent)
        } else {
            Notice.show(this, R.string.sms_send_failed)
        }
    }
}

@Composable
private fun ThreadList(
    threads: List<SmsThread>,
    prefilled: String?,
    scrollButtons: Boolean,
    onOpen: (Long) -> Unit,
) {
    val palette = LocalBigPalette.current
    val locale = currentLocale()
    val format = remember(locale) { SimpleDateFormat("EEE d. MMM, HH:mm", locale) }
    val listState = rememberLazyListState()

    Column {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            item { BigHeading(stringResource(R.string.messages)) }
            if (prefilled != null) {
                item {
                    BigRow(
                        label = stringResource(R.string.sms_new_to, PhoneNumbers.forDisplay(prefilled)),
                        surface = palette.surfaceAccent,
                        onClick = {},
                    )
                }
            }
            if (threads.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.sms_empty),
                        color = palette.onBackground,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                    )
                }
            }
            items(threads, key = { it.threadId }) { thread ->
                BigRow(
                    label = thread.title + if (thread.hasUnread) " (${thread.unreadCount})" else "",
                    secondary = SmsThreads.preview(thread.lastMessage) + " · " +
                        format.format(Date(thread.lastMessage.timestamp)),
                    secondaryMaxLines = 1,
                    surface = if (thread.hasUnread) palette.surfaceAccent else palette.surfaceDefault,
                    onClick = { onOpen(thread.threadId) },
                )
            }
        }
        if (scrollButtons) {
            ScrollButtons(listState, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun Conversation(
    messages: List<SmsMessage>,
    title: String,
    draft: String,
    isDefaultApp: Boolean,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    conversationScale: Float,
    confirmBeforeSending: Boolean,
    sendButtonAbove: Boolean,
    sendButtonLarge: Boolean,
) {
    // Die Rueckfrage lebt hier und nicht in der Konfiguration: sie gilt fuer diesen einen
    // Entwurf. Wer den Text aendert, faengt von vorn an.
    var fragtNach by remember(draft) { mutableStateOf(false) }
    val skala = ConversationText.scale(conversationScale)
    val palette = LocalBigPalette.current
    val locale = currentLocale()
    val uhrFormat = remember(locale) { SimpleDateFormat("HH:mm", locale) }
    val tagFormat = remember(locale) { SimpleDateFormat("EEEE, d. MMMM", locale) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigHeading(title)
        if (!isDefaultApp) {
            // Ehrlich sein statt eine Nachricht zu zeigen, die nach dem Neustart weg ist.
            Text(
                text = stringResource(R.string.sms_not_default),
                color = palette.danger,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        // Eine Unterhaltung faengt unten an. Oeffnete sie oben, muesste man erst zur
        // neuesten Nachricht scrollen - und die ist der Grund, aus dem man sie oeffnet.
        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                // Der Tag ueber der ersten Nachricht des Tages - nicht an jeder Zeile.
                if (MessageStamps.startsNewDay(messages.getOrNull(index - 1)?.timestamp, message.timestamp)) {
                    Text(
                        text = tagFormat.format(Date(message.timestamp)),
                        color = palette.onBackground,
                        fontSize = dpSp(14f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (message.incoming) 0.dp else 40.dp,
                            end = if (message.incoming) 40.dp else 0.dp,
                        )
                        .clip(RoundedCornerShape(LocalCornerRadius.current))
                        .background(
                            if (message.incoming) palette.emptyTile else palette.surfaceAccent.fill,
                        )
                        .padding(12.dp),
                ) {
                    Column {
                        Text(
                            text = message.body,
                            color = if (message.incoming) palette.onBackground else palette.surfaceAccent.ink,
                            fontSize = dpSp(18f * skala),
                        )
                        // Die Uhrzeit unter jeder Nachricht: ob sie von eben ist oder von
                        // letzter Woche, ist bei "bin unterwegs" der ganze Unterschied.
                        Text(
                            text = uhrFormat.format(Date(message.timestamp)),
                            color = if (message.incoming) {
                                palette.onBackground
                            } else {
                                palette.surfaceAccent.ink
                            },
                            fontSize = dpSp(13f * skala),
                        )
                    }
                }
            }
        }
        val feld = @Composable {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraft,
                textStyle = TextStyle(fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val knopf = @Composable {
            BigRow(
                label = if (fragtNach) {
                    stringResource(R.string.sms_send_confirm)
                } else {
                    stringResource(R.string.sms_send)
                },
                secondary = when {
                    fragtNach -> stringResource(R.string.sms_send_confirm_hint)
                    draft.isNotBlank() -> pluralStringResource(
                        R.plurals.sms_parts,
                        SosMessage.partsNeeded(draft),
                        SosMessage.partsNeeded(draft),
                    )
                    else -> null
                },
                icon = Icons.AutoMirrored.Filled.Send,
                surface = if (fragtNach) palette.surfaceDanger else palette.surfaceAccent,
                modifier = if (sendButtonLarge) Modifier.height(96.dp) else Modifier,
                onClick = {
                    // Erst fragen, dann senden - und nur, wenn ueberhaupt etwas dasteht.
                    // Eine Rueckfrage zu einer leeren Nachricht waere eine Frage ohne Folge.
                    if (confirmBeforeSending && !fragtNach && draft.isNotBlank()) {
                        fragtNach = true
                    } else {
                        fragtNach = false
                        onSend()
                    }
                },
            )
        }
        if (sendButtonAbove) {
            knopf()
            feld()
        } else {
            feld()
            knopf()
        }
    }
}
