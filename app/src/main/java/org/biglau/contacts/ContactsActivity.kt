package org.biglau.contacts

import android.Manifest
import android.os.Bundle
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.biglau.R
import kotlinx.coroutines.launch
import org.biglau.actions.Intents
import org.biglau.data.ConfigStore
import org.biglau.search.TextSearch
import org.biglau.ui.BigHeading
import org.biglau.ui.BigRow
import org.biglau.ui.BigSearchField
import org.biglau.ui.ContactAvatar
import org.biglau.ui.dpSp
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

/**
 * Eigene Kontaktliste. Grosse Zeilen mit Foto, Suche, und eine Detailansicht, in der
 * Anrufen und Schreiben je eine ganze Zeile bekommen statt eines kleinen Symbols.
 */
class ContactsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)
        val repository = ContactRepository.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            var granted by remember { mutableStateOf(repository.hasPermission()) }
            var all by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
            var query by rememberSaveable { mutableStateOf("") }
            var selected by remember { mutableStateOf<PhoneContact?>(null) }
            var loading by remember { mutableStateOf(true) }

            val scope = rememberCoroutineScope()
            val askWrite = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { }
            val ask = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { result -> granted = result; if (!result) finish() }

            LaunchedEffect(granted) {
                if (granted) {
                    loading = true
                    all = repository.load()
                    loading = false
                } else {
                    ask.launch(Manifest.permission.READ_CONTACTS)
                }
            }

            val order = if (config.contacts.sortBySurname) ContactOrder.SURNAME else ContactOrder.FIRST_NAME
            val shown = remember(all, query, order, config.contacts) {
                val filtered = TextSearch.filter(all, query) {
                    ContactSort.searchText(it, config.contacts.searchNumbers)
                }
                if (query.isEmpty()) {
                    ContactSort.sorted(filtered, order, config.contacts.favouritesFirst)
                } else {
                    filtered
                }
            }

            BigLauTheme(config.appearance.theme, config.appearance.textScale) {
                BackHandler(enabled = selected != null) { selected = null }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(LocalBigPalette.current.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    val current = selected
                    if (current != null) {
                        ContactDetail(
                            contact = current,
                            onCall = { Intents.call(this@ContactsActivity, it) },
                            onSms = { Intents.sms(this@ContactsActivity, it) },
                            onToggleStar = {
                                if (repository.canWrite()) {
                                    scope.launch {
                                        val ok = repository.setStarred(current.id, !current.starred)
                                        if (ok) {
                                            all = repository.load()
                                            selected = all.firstOrNull { it.id == current.id }
                                        } else {
                                            Toast.makeText(
                                                this@ContactsActivity,
                                                R.string.contacts_star_failed,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    }
                                } else {
                                    askWrite.launch(Manifest.permission.WRITE_CONTACTS)
                                }
                            },
                            onEdit = { startActivity(repository.editIntent(current.id)) },
                        )
                    } else {
                        ContactList(
                            contacts = shown,
                            loading = loading,
                            query = query,
                            sortBySurname = config.contacts.sortBySurname,
                            onQuery = { query = it },
                            onToggleSort = {
                                store.update {
                                    it.copy(
                                        contacts = it.contacts.copy(
                                            sortBySurname = !it.contacts.sortBySurname,
                                        ),
                                    )
                                }
                            },
                            onPick = { selected = it },
                            onCreate = { startActivity(repository.createIntent()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactList(
    contacts: List<PhoneContact>,
    loading: Boolean,
    query: String,
    sortBySurname: Boolean,
    onQuery: (String) -> Unit,
    onToggleSort: () -> Unit,
    onPick: (PhoneContact) -> Unit,
    onCreate: () -> Unit,
) {
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (query.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BigHeading(stringResource(R.string.contacts), modifier = Modifier.weight(1f))
            }
        }
        BigSearchField(
            value = query,
            onValueChange = onQuery,
            hint = stringResource(R.string.search_contacts),
        )
        if (query.isEmpty()) {
            // Bewusst hier und nicht am Listenende: bei 338 Kontakten waere er dort
            // nach unten gescrollt und praktisch unerreichbar.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BigRow(
                    label = stringResource(
                        if (sortBySurname) R.string.contacts_sort_surname else R.string.contacts_sort_first,
                    ),
                    icon = Icons.Filled.SortByAlpha,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleSort,
                )
                BigRow(
                    label = stringResource(R.string.contacts_new),
                    icon = Icons.Filled.PersonAdd,
                    modifier = Modifier.weight(0.55f),
                    onClick = onCreate,
                )
            }
        }
        if (loading) {
            Text(
                text = stringResource(R.string.contacts_loading),
                color = palette.onBackground,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
        } else if (contacts.isEmpty()) {
            Text(
                text = stringResource(R.string.contacts_no_match),
                color = palette.onBackground,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(contacts, key = { it.id }) { contact ->
                BigRow(
                    label = contact.name,
                    secondary = contact.numbers.firstOrNull()?.number,
                    leading = { ContactAvatar(contact.name, contact.photoUri) },
                    onClick = { onPick(contact) },
                )
            }
        }
    }
}

/**
 * Detailansicht. Jede Nummer bekommt zwei ganze Zeilen - Anrufen und Schreiben - statt
 * zweier kleiner Symbole nebeneinander. Auf drei Zoll ist das der Unterschied zwischen
 * treffen und danebentippen.
 */
@Composable
private fun ContactDetail(
    contact: PhoneContact,
    onCall: (String) -> Unit,
    onSms: (String) -> Unit,
    onToggleStar: () -> Unit,
    onEdit: () -> Unit,
) {
    val palette = LocalBigPalette.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ContactAvatar(contact.name, contact.photoUri, size = 72.dp)
                Text(
                    text = contact.name,
                    color = palette.onBackground,
                    fontSize = dpSp(26f),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        item {
            BigRow(
                label = stringResource(
                    if (contact.starred) R.string.contacts_unstar else R.string.contacts_star,
                ),
                icon = if (contact.starred) Icons.Filled.Star else Icons.Filled.StarBorder,
                surface = if (contact.starred) palette.surfaceAccent else palette.surfaceDefault,
                onClick = onToggleStar,
            )
        }
        item {
            BigRow(
                label = stringResource(R.string.contacts_edit),
                icon = Icons.Filled.Edit,
                onClick = onEdit,
            )
        }
        contact.numbers.forEach { number ->
            item {
                BigRow(
                    label = stringResource(R.string.contacts_action_call),
                    secondary = listOfNotNull(number.number, number.label).joinToString(" · "),
                    icon = Icons.Filled.Call,
                    onClick = { onCall(number.number) },
                )
            }
            item {
                BigRow(
                    label = stringResource(R.string.contacts_action_sms),
                    secondary = number.number,
                    icon = Icons.AutoMirrored.Filled.Message,
                    onClick = { onSms(number.number) },
                )
            }
        }
    }
}
