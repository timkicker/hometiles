package org.biglau.sms

import android.Manifest
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.R
import org.biglau.contacts.ContactRepository
import org.biglau.data.ConfigStore
import org.biglau.phone.PhoneNumbers
import org.biglau.toggles.SosMessage
import org.biglau.ui.BigHeading
import org.biglau.actions.Intents
import org.biglau.ui.PermissionGate
import org.biglau.ui.PermissionState
import org.biglau.ui.BigRow
import org.biglau.ui.ScrollButtons
import org.biglau.ui.dpSp
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette
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
class SmsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)
        val repository = SmsRepository.get(this)
        val contacts = ContactRepository.get(this)

        val prefilledAddress = intent?.data?.schemeSpecificPart?.let(PhoneNumbers::clean)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            val changes by SmsRepository.changes.collectAsStateWithLifecycle()
            var messages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
            var names by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
            var openThread by remember { mutableStateOf<Long?>(null) }
            var draft by remember { mutableStateOf("") }
            var granted by remember { mutableStateOf(repository.hasReadPermission()) }
            val palette = LocalBigPalette.current

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

            LaunchedEffect(granted, changes) {
                if (!granted) return@LaunchedEffect
                messages = repository.load()
                names = contacts.load().flatMap { contact ->
                    contact.numbers.map { PhoneNumbers.clean(it.number) to contact.name }
                }.toMap()
            }

            val threads = remember(messages, names) {
                SmsThreads.group(messages) { names[PhoneNumbers.clean(it)] }
            }

            BigLauTheme(
                config.appearance.theme,
                config.appearance.textScale,
                haptics = config.behaviour.hapticFeedback,
            ) {
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
            onSent()
            Toast.makeText(this, R.string.sms_sent, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.sms_send_failed, Toast.LENGTH_LONG).show()
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
    val format = remember { SimpleDateFormat("EEE d. MMM, HH:mm", Locale.getDefault()) }
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
) {
    val palette = LocalBigPalette.current
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
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (message.incoming) 0.dp else 40.dp,
                            end = if (message.incoming) 40.dp else 0.dp,
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (message.incoming) palette.emptyTile else palette.surfaceAccent.fill,
                        )
                        .padding(12.dp),
                ) {
                    Text(
                        text = message.body,
                        color = if (message.incoming) palette.onBackground else palette.surfaceAccent.ink,
                        fontSize = dpSp(18f),
                    )
                }
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = onDraft,
            textStyle = TextStyle(fontSize = 18.sp),
            modifier = Modifier.fillMaxWidth(),
        )
        BigRow(
            label = stringResource(R.string.sms_send),
            secondary = if (draft.isNotBlank()) {
                stringResource(R.string.sms_parts, SosMessage.partsNeeded(draft))
            } else {
                null
            },
            icon = Icons.AutoMirrored.Filled.Send,
            surface = palette.surfaceAccent,
            onClick = onSend,
        )
    }
}
