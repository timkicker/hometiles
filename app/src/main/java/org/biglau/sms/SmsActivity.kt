package org.biglau.sms

import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.database.ContentObserver
import android.provider.Telephony
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.ui.ClockFormat
import org.biglau.ui.bestDatePattern
import org.biglau.ui.bigSp
import org.biglau.ui.theme.LocalCornerRadius
import org.biglau.ui.BigLauActivity
import org.biglau.ui.currentLocale
import org.biglau.R
import org.biglau.contacts.ContactRepository
import org.biglau.data.ConfigStore
import org.biglau.phone.PhoneNumbers
import org.biglau.actions.SosMessage
import org.biglau.ui.BigHeading
import org.biglau.actions.Intents
import org.biglau.ui.Notice
import org.biglau.ui.PermissionGate
import org.biglau.ui.PermissionState
import org.biglau.ui.BigRow
import org.biglau.ui.ScrollButtons
import org.biglau.ui.dpSp
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.style.TextAlign
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * messages: the list of conversations, one conversation, and writing.
 *
 * reading works with READ_SMS without the default sms role. sending does too, but the sent
 * message then never reaches the database, which only the default app can write. the screen
 * says so instead of showing a message that is gone after a restart.
 */
class SmsActivity : BigLauActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)
        val repository = SmsRepository.get(this)
        val contacts = ContactRepository.get(this)

        val prefilledAddress = intent?.data?.schemeSpecificPart?.let(PhoneNumbers::clean)
        // from the notice about a new message: that conversation should open, not the list
        // in which it would have to be found first.
        val announcedNumber = intent?.getStringExtra(EXTRA_ADDRESS)
        // only when the notice itself took the screen. fixed in the manifest it would be a
        // different promise: then every conversation would lie over the lock screen.
        if (intent?.getBooleanExtra(EXTRA_FULL_SCREEN, false) == true) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            val changes by SmsRepository.changes.collectAsStateWithLifecycle()
            // listen to the database, not only to ourselves: `changes` ticked only when
            // BigLau wrote, so a message deleted elsewhere stayed in this list until the app
            // was restarted.
            DisposableEffect(Unit) {
                val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) = SmsRepository.notifyChanged()
                }
                contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer)
                onDispose { contentResolver.unregisterContentObserver(observer) }
            }
            // the *system* grants the sms role and sends no result back. read once while
            // drawing, the hint would keep saying BigLau is not your messaging app right
            // after it was made one. see `BigLauActivity.resumes`.
            val holdsSmsRole = remember(resumes.intValue) { repository.isDefaultSmsApp() }
            var messages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
            var names by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
            var openThread by remember { mutableStateOf<Long?>(null) }
            // who the first message just went to. see the effect further down.
            var justSentTo by remember { mutableStateOf<String?>(null) }
            // a conversation with someone there is none with yet. this case used to lead
            // nowhere: the row had no action at all, and an smsto: link landed on the list.
            var openAddress by remember { mutableStateOf<String?>(null) }
            // prefilled when another app handed us a message to send (reject a call with a
            // message). see RespondViaMessageService.
            var draft by remember { mutableStateOf(intent?.getStringExtra(EXTRA_BODY).orEmpty()) }
            // `resumes` as the key: on a permanently refused permission this screen sends
            // people into the app settings, and nothing comes back from there.
var granted by remember(resumes.intValue) { mutableStateOf(repository.hasReadPermission()) }

            // no explainer screen: after a no, the list stays the place one looks.
            val askForNotices = rememberLauncherForActivityResult(
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

            // the system asks by itself on the first open, as expected. not after a refusal:
            // then the button stands there and the moment is the user's.
            LaunchedEffect(Unit) {
                if (!granted && !deniedOnce) ask.launch(Manifest.permission.READ_SMS)
            }

            // from android 13 on no notice may appear without this, and a new message would
            // arrive silently. the device runs 11, where installing grants it; asked anyway,
            // because the app should run on newer ones too.
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@SmsActivity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    askForNotices.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // while reading, the list does not say no messages yet: the load below fetches
            // the messages *and* all contacts, which takes visibly long at 338 contacts. a
            // true sentence at the wrong moment is a false statement.
            var loading by remember { mutableStateOf(true) }
            LaunchedEffect(granted, changes) {
                if (!granted) {
                    loading = false
                    return@LaunchedEffect
                }
                // filtered messages never reach the list. `PLAN.md` 4.7.
                messages = SmsFilter.apply(
                    repository.load(),
                    config.sms.hiddenNumbers,
                    config.sms.hiddenWords,
                )
                names = contacts.load(resources).flatMap { contact ->
                    contact.numbers.map { PhoneNumbers.clean(it.number) to contact.name }
                }.toMap()
                loading = false
            }

            val threads = remember(messages, names) {
                SmsThreads.group(messages) { names[PhoneNumbers.clean(it)] }
            }

            // read is read, or the count beside the name would stand forever. the notice
            // goes with it: an open conversation has been seen.
            LaunchedEffect(openThread) {
                val offen = openThread ?: return@LaunchedEffect
                if (repository.markRead(offen)) SmsRepository.notifyChanged()
                threads.firstOrNull { it.threadId == offen }?.let {
                    SmsNotifications.clear(this@SmsActivity, it.address)
                }
            }

            // only once the messages are there can the announced number be matched to a
            // conversation.
            /**
             * after the first message the new conversation is a real one.
             *
             * sending the first message to a number emptied the field and left the screen
             * *blank*: the message existed, but in a conversation this screen did not know,
             * because it still hung on the number. someone facing a blank screen after
             * sending sends it again.
             */
            LaunchedEffect(threads, justSentTo) {
                val number = justSentTo ?: return@LaunchedEffect
                val passend = threads.firstOrNull {
                    PhoneNumbers.clean(it.address) == PhoneNumbers.clean(number)
                } ?: return@LaunchedEffect
                openThread = passend.threadId
                openAddress = null
                justSentTo = null
            }

            LaunchedEffect(threads, announcedNumber) {
                val number = announcedNumber ?: return@LaunchedEffect
                if (openThread != null) return@LaunchedEffect
                val passend = threads.firstOrNull {
                    PhoneNumbers.clean(it.address) == PhoneNumbers.clean(number)
                }
                if (passend != null) {
                    openThread = passend.threadId
                    SmsNotifications.clear(this@SmsActivity, number)
                } else {
                    // no conversation with this number yet, so a new one.
                    openAddress = number
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
                // back closes the new conversation too, or it would lead out of the app
                // while something is visibly open.
                BackHandler(enabled = openThread != null || openAddress != null) {
                    openThread = null
                    openAddress = null
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(palette.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    val thread = openThread
                    val newNumber = openAddress
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
                            title = threads.firstOrNull { it.threadId == thread }
                                ?.titleOr(stringResource(R.string.call_unknown))
                                .orEmpty(),
                            draft = draft,
                            isDefaultApp = holdsSmsRole,
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

                        newNumber != null -> Conversation(
                            messages = emptyList(),
                            title = PhoneNumbers.forDisplay(newNumber)
                                .ifBlank { stringResource(R.string.call_unknown) },
                            draft = draft,
                            isDefaultApp = holdsSmsRole,
                            onDraft = { draft = it },
                            onSend = {
                                send(newNumber, draft) {
                                    draft = ""
                                    justSentTo = newNumber
                                }
                            },
                            conversationScale = config.sms.conversationScale,
                            confirmBeforeSending = config.sms.confirmBeforeSending,
                            sendButtonAbove = config.sms.sendButtonAbove,
                            sendButtonLarge = config.sms.sendButtonLarge,
                        )

                        else -> ThreadList(
                            loading = loading,
                            threads = threads,
                            // once the conversation exists, this row is a second entry for
                            // the same person, one carrying the contact name and the other
                            // the number.
                            prefilled = prefilledAddress?.takeIf { number ->
                                threads.none { PhoneNumbers.clean(it.address) == PhoneNumbers.clean(number) }
                            },
                            scrollButtons = config.behaviour.accessibility.scrollButtons,
                            onOpen = { openThread = it },
                            onCompose = { number ->
                                val passend = threads.firstOrNull {
                                    PhoneNumbers.clean(it.address) == PhoneNumbers.clean(number)
                                }
                                if (passend != null) openThread = passend.threadId
                                else openAddress = number
                            },
                        )
                    }
                }
            }
        }
    }

    companion object {
        /** the number whose conversation should open. see [org.biglau.notify.SmsNotifications]. */
        const val EXTRA_ADDRESS = "biglau.sms.address"

        /** as a full-screen notice the message may show over the lock screen. */
        const val EXTRA_FULL_SCREEN = "biglau.sms.fullscreen"

        /** prefilled text in the field. see [org.biglau.sms.RespondViaMessageService]. */
        const val EXTRA_BODY = "biglau.sms.body"
    }

    private fun send(address: String, body: String, onSent: () -> Unit) {
        if (body.isBlank()) return
        val manager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            getSystemService(SmsManager::class.java)
        } else {
            // as in `Sos.smsManager`: before android 12 there is only `getDefault()`.
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        // write first, then send: the network receipt arrives seconds later and must say
        // *which* message failed, which needs the row to exist already. with the role,
        // android no longer stores the sent message itself.
        val row = if (SmsDelivery.mayWrite(Telephony.Sms.getDefaultSmsPackage(this), packageName)) {
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
            }.getOrNull()
        } else {
            null
        }

        // the receipt: without it, sent only meant the call had not thrown.
        val receipt = row?.let {
            PendingIntent.getBroadcast(
                this,
                it.hashCode(),
                Intent(this, SmsSentReceiver::class.java).setData(it),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val sent = runCatching {
            val parts = manager.divideMessage(body)
            if (parts.size > 1) {
                // one receipt per part: a single refused part makes the message incomplete.
                manager.sendMultipartTextMessage(
                    address,
                    null,
                    parts,
                    ArrayList(List(parts.size) { receipt }),
                    null,
                )
            } else {
                manager.sendTextMessage(address, null, body, receipt, null)
            }
            true
        }.getOrDefault(false)

        if (sent) {
            SmsRepository.notifyChanged()
            onSent()
            Notice.show(this, R.string.sms_sent)
        } else {
            // the call itself failed, so the row stands there for nothing.
            row?.let { runCatching { contentResolver.delete(it, null, null) } }
            SmsRepository.notifyChanged()
            // `runCatching` swallows every reason. the one that can be fixed is the missing
            // permission, where `sendTextMessage` throws a `SecurityException`; could not be
            // sent would be half an answer, since it does not say it hangs on something
            // grantable.
            //
            // no permission dialog from here: this place sends, it should not also procure
            // the right to. the sentence names the place, the person decides.
            val darfSenden = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS,
            ) == PackageManager.PERMISSION_GRANTED
            Notice.show(
                this,
                if (darfSenden) R.string.sms_send_failed else R.string.sms_send_no_permission,
            )
        }
    }
}

@Composable
private fun ThreadList(
    threads: List<SmsThread>,
    /** still reading? then no messages yet is not true, only not there yet. */
    loading: Boolean,
    prefilled: String?,
    onCompose: (String) -> Unit,
    scrollButtons: Boolean,
    onOpen: (Long) -> Unit,
) {
    val palette = LocalBigPalette.current
    val locale = currentLocale()
    val format = remember(locale) {
        // parts instead of a fixed pattern; see bestDatePattern. "j" is the hour *in the
        // language's own spelling*, while "H" forces 24 hours: the same minute read
        // "5:39 PM" in the header and "17:39" in the list.
        SimpleDateFormat(bestDatePattern("EEEdMMMjmm", locale), locale)
    }
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
                        onClick = { onCompose(prefilled) },
                    )
                }
            }
            if (threads.isEmpty()) {
                item {
                    Text(
                        text = stringResource(if (loading) R.string.sms_loading else R.string.sms_empty),
                        color = palette.onBackground,
                        fontSize = bigSp(17f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                    )
                }
            }
            items(threads, key = { it.threadId }) { thread ->
                BigRow(
                    label = thread.titleOr(stringResource(R.string.call_unknown)) +
                        if (thread.hasUnread) " (${thread.unreadCount})" else "",
                    // without the bracketed count: it stands as a sentence in the state.
                    labelSpeech = thread.titleOr(stringResource(R.string.call_unknown)),
                    secondary = SmsThreads.preview(thread.lastMessage) + " · " +
                        format.format(Date(thread.lastMessage.timestamp)),
                    secondaryMaxLines = 1,
                    surface = if (thread.hasUnread) palette.surfaceAccent else palette.surfaceDefault,
                    // a bare (2) hung on the name, which read aloud is a number without a
                    // thing. the home screen tile long said two are unread.
                    state = if (thread.hasUnread) {
                        pluralStringResource(
                            R.plurals.a11y_unread,
                            thread.unreadCount,
                            thread.unreadCount,
                        )
                    } else {
                        null
                    },
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
    // the confirmation lives here and not in the config: it holds for this one draft.
    var asking by remember(draft) { mutableStateOf(false) }
    val skala = ConversationText.scale(conversationScale)
    val palette = LocalBigPalette.current
    val locale = currentLocale()
    // the same clock as in the header: a fixed "HH:mm" here put "2:30 PM" above and
    // "14:30" below on a phone set to twelve hours.
    val zwoelfStunden = !android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val uhrFormat = remember(locale, zwoelfStunden) {
        SimpleDateFormat(ClockFormat.timePattern(!zwoelfStunden), locale)
    }
    val tagFormat = remember(locale) { SimpleDateFormat(bestDatePattern("EEEEdMMMM", locale), locale) }
    // the heading goes while the keyboard is open. measured at 200 % system font: heading
    // 63, field 92, send row 90 pixels, together more than is left above the keyboard, and
    // the send button was half covered. of the three the heading is the most dispensable:
    // one has just chosen whom to write to.
    val tastaturOffen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!tastaturOffen) BigHeading(title)
        // a conversation starts at the bottom: opening at the top would mean scrolling to
        // the newest message, which is the reason for opening it.
        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!isDefaultApp) {
                // honest instead of showing a message that is gone after a restart, but
                // *inside* the scroll and not above it: fixed, the hint took five lines at
                // 200 % and left two pixels of the conversation.
                item {
                    Text(
                        text = stringResource(R.string.sms_not_default),
                        color = palette.dangerText,
                        fontSize = bigSp(15f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            }
            itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                // the day over the first message of the day, not on every row.
                if (MessageStamps.startsNewDay(messages.getOrNull(index - 1)?.timestamp, message.timestamp)) {
                    Text(
                        text = tagFormat.format(Date(message.timestamp)),
                        color = palette.onBackground,
                        fontSize = dpSp(14f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                // who wrote a message stands nowhere in the bubble: it hangs left or right
                // and carries one colour or the other, and read aloud both are nothing. "on
                // my way" without a sender is the opposite of itself.
                val wer = stringResource(
                    if (message.incoming) R.string.a11y_message_in else R.string.a11y_message_out,
                )
                val wann = if (message.failed) {
                    stringResource(R.string.sms_not_sent)
                } else {
                    uhrFormat.format(Date(message.timestamp))
                }
                Box(
                    modifier = Modifier
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$wer: ${message.body}. $wann"
                        }
                        .fillMaxWidth()
                        .padding(
                            start = if (message.incoming) 0.dp else 40.dp,
                            end = if (message.incoming) 40.dp else 0.dp,
                        )
                        .clip(RoundedCornerShape(LocalCornerRadius.current))
                        .background(
                            when {
                                // a message that did not arrive must not look like one that
                                // did: otherwise the only difference is that no answer comes.
                                message.failed -> palette.surfaceDanger.fill
                                message.incoming -> palette.emptyTile
                                else -> palette.surfaceAccent.fill
                            },
                        )
                        .padding(12.dp),
                ) {
                    Column {
                        Text(
                            text = message.body,
                            color = when {
                                message.failed -> palette.surfaceDanger.ink
                                message.incoming -> palette.onBackground
                                else -> palette.surfaceAccent.ink
                            },
                            fontSize = dpSp(18f * skala),
                        )
                        // the time under each message: just now or last week is the whole
                        // difference for "on my way". for one that never went out, that
                        // stands instead: the time of a message that never was says nothing.
                        Text(
                            text = if (message.failed) {
                                stringResource(R.string.sms_not_sent)
                            } else {
                                uhrFormat.format(Date(message.timestamp))
                            },
                            color = when {
                                message.failed -> palette.surfaceDanger.ink
                                message.incoming -> palette.onBackground
                                else -> palette.surfaceAccent.ink
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
                textStyle = TextStyle(fontSize = bigSp(18f)),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val knopf = @Composable {
            BigRow(
                label = if (asking) {
                    stringResource(R.string.sms_send_confirm)
                } else {
                    stringResource(R.string.sms_send)
                },
                secondary = when {
                    asking -> stringResource(R.string.sms_send_confirm_hint)
                    draft.isNotBlank() -> pluralStringResource(
                        R.plurals.sms_parts,
                        SosMessage.partsNeeded(draft),
                        SosMessage.partsNeeded(draft),
                    )
                    else -> null
                },
                icon = Icons.AutoMirrored.Filled.Send,
                // with nothing written, this is not a button.
                //
                // `send` refuses an empty message, which was right but invisible: the button
                // stood in full accent colour, one tapped, and *nothing happened silently*.
                // see `BigRow` on rows with an empty action.
                surface = when {
                    asking -> palette.surfaceDanger
                    draft.isBlank() -> palette.surfaceDefault
                    else -> palette.surfaceAccent
                },
                modifier = if (sendButtonLarge) Modifier.height(96.dp) else Modifier,
                onClick = if (draft.isBlank()) {
                    null
                } else {
                    {
                        // ask first, then send. a question about an empty message would be
                        // one without a consequence, and cannot arise here.
                        if (confirmBeforeSending && !asking) {
                            asking = true
                        } else {
                            asking = false
                            onSend()
                        }
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
