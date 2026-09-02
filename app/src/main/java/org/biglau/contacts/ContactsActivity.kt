package org.biglau.contacts

import android.Manifest
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
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
import org.biglau.ui.BigLauActivity
import org.biglau.R
import kotlinx.coroutines.launch
import org.biglau.actions.Intents
import org.biglau.data.ConfigStore
import org.biglau.settings.SettingsActivity
import org.biglau.search.TextSearch
import org.biglau.phone.PhoneNumbers
import org.biglau.ui.BigHeading
import org.biglau.ui.BigIconButton
import org.biglau.ui.Notice
import org.biglau.ui.PermissionGate
import org.biglau.ui.PermissionState
import org.biglau.ui.BigRow
import org.biglau.ui.BigSearchField
import org.biglau.ui.ScrollButtons
import org.biglau.ui.ContactAvatar
import org.biglau.ui.dpSp
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

/**
 * Eigene Kontaktliste. Grosse Zeilen mit Foto, Suche, und eine Detailansicht, in der
 * Anrufen und Schreiben je eine ganze Zeile bekommen statt eines kleinen Symbols.
 */
class ContactsActivity : BigLauActivity() {

    companion object {
        /** Nur die Favoriten zeigen - von der Favoritenkachel aus. */
        const val EXTRA_FAVOURITES = "favouritesOnly"
    }

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
            // Von der Favoritenkachel aus: nur die mit Stern, ohne Suche davor.
            var favouritesOnly by rememberSaveable {
                mutableStateOf(intent?.getBooleanExtra(EXTRA_FAVOURITES, false) == true)
            }
            var selected by remember { mutableStateOf<PhoneContact?>(null) }
            var loading by remember { mutableStateOf(true) }

            val scope = rememberCoroutineScope()
            val askWrite = rememberLauncherForActivityResult(
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
                    canAskAgain = shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)
                }
            }

            // Beim ersten Oeffnen fragt das System von selbst. Nach einer Ablehnung schloss
            // sich der Bildschirm frueher wortlos - man hatte etwas angetippt, und es
            // verschwand einfach wieder. Jetzt bleibt er stehen und erklaert sich.
            LaunchedEffect(Unit) {
                if (!granted && !deniedOnce) ask.launch(Manifest.permission.READ_CONTACTS)
            }

            LaunchedEffect(granted) {
                if (granted) {
                    loading = true
                    all = repository.load()
                    loading = false
                }
            }

            val order = if (config.contacts.sortBySurname) ContactOrder.SURNAME else ContactOrder.FIRST_NAME
            val shown = remember(all, query, order, config.contacts) {
                val filtered = TextSearch.filter(all, query) {
                    ContactSort.searchText(it, config.contacts.searchNumbers)
                }
                when {
                    favouritesOnly -> ContactSort.favouritesOnly(filtered, order)
                    query.isEmpty() -> ContactSort.sorted(filtered, order, config.contacts.favouritesFirst)
                    else -> filtered
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
                BackHandler(enabled = selected != null) { selected = null }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(LocalBigPalette.current.background)
                        .safeDrawingPadding()
                        .padding(horizontal = 8.dp),
                ) {
                    val current = selected
                    if (!granted) {
                        PermissionGate(
                            title = stringResource(R.string.contacts),
                            explanation = stringResource(R.string.contacts_permission),
                            blocked = PermissionState.blocked(deniedOnce, canAskAgain),
                            onAsk = { ask.launch(Manifest.permission.READ_CONTACTS) },
                            onSettings = { Intents.appSettings(this@ContactsActivity) },
                        )
                    } else if (current != null) {
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
                                            Notice.show(this@ContactsActivity, R.string.contacts_star_failed)
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
                            scrollButtons = config.behaviour.accessibility.scrollButtons,
                            favouritesOnly = favouritesOnly,
                            searchNumbers = config.contacts.searchNumbers,
                            onSearchSettings = {
                                startActivity(
                                    android.content.Intent(
                                        this@ContactsActivity,
                                        SettingsActivity::class.java,
                                    ).putExtra(SettingsActivity.EXTRA_PAGE, SettingsActivity.PAGE_CONTACTS),
                                )
                            },
                            onShowAll = { favouritesOnly = false },
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
    scrollButtons: Boolean,
    favouritesOnly: Boolean,
    onShowAll: () -> Unit,
    searchNumbers: Boolean,
    onSearchSettings: () -> Unit,
) {
    val palette = LocalBigPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (query.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BigHeading(
                    stringResource(if (favouritesOnly) R.string.favourites else R.string.contacts),
                    modifier = Modifier.weight(1f),
                )
                // Sortierung ist eine Nebensache und darf ein Symbol sein; einen Kontakt
                // anzulegen ist eine Handlung und behaelt sein Wort.
                BigIconButton(
                    icon = Icons.Filled.SortByAlpha,
                    description = stringResource(
                        if (sortBySurname) R.string.contacts_sort_surname else R.string.contacts_sort_first,
                    ),
                    onClick = onToggleSort,
                )
            }
        }
        BigSearchField(
            value = query,
            onValueChange = onQuery,
            hint = stringResource(R.string.search_contacts),
        )
        if (query.isEmpty() && favouritesOnly) {
            // Ohne diesen Weg waere eine leere Favoritenliste eine Sackgasse - und auch
            // eine gefuellte laesst sonst niemanden zu den uebrigen Kontakten.
            if (contacts.isEmpty()) {
                Text(
                    text = stringResource(R.string.favourites_none),
                    color = palette.onBackground,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
            BigRow(
                label = stringResource(R.string.favourites_show_all),
                icon = Icons.Filled.Person,
                surface = palette.surfaceAccent,
                onClick = onShowAll,
            )
        } else if (query.isEmpty()) {
            // Bewusst hier und nicht am Listenende: bei 338 Kontakten waere er dort
            // nach unten gescrollt und praktisch unerreichbar.
            BigRow(
                label = stringResource(R.string.contacts_new),
                icon = Icons.Filled.PersonAdd,
                onClick = onCreate,
            )
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
            // Wer eine Nummer eintippt, waehrend nur Namen durchsucht werden, bekommt
            // sonst eine wahre Auskunft ohne ihren Grund - und sucht den Fehler bei sich.
            if (!searchNumbers && ContactSort.looksLikeNumber(query)) {
                Text(
                    text = stringResource(R.string.contacts_numbers_not_searched),
                    color = palette.onBackground,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                BigRow(
                    label = stringResource(R.string.contacts_search_numbers_open),
                    icon = Icons.Filled.Settings,
                    surface = palette.surfaceAccent,
                    onClick = onSearchSettings,
                )
            }
        }
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            items(contacts, key = { it.id }) { contact ->
                BigRow(
                    label = contact.name,
                    // In Bloecken, wie in der Anrufliste und in den Nachrichten.
                    // Dieselbe Nummer sah an drei Stellen verschieden aus.
                    secondary = contact.numbers.firstOrNull()?.number
                        ?.let(PhoneNumbers::forDisplay),
                    secondaryMaxLines = 1,
                    leading = { ContactAvatar(contact.name, contact.photoUri) },
                    onClick = { onPick(contact) },
                )
            }
        }
        if (scrollButtons) {
            ScrollButtons(listState)
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
                    secondary = listOfNotNull(PhoneNumbers.forDisplay(number.number), number.label)
                        .joinToString(" · "),
                    icon = Icons.Filled.Call,
                    onClick = { onCall(number.number) },
                )
            }
            item {
                BigRow(
                    label = stringResource(R.string.contacts_action_sms),
                    secondary = PhoneNumbers.forDisplay(number.number),
                    icon = Icons.AutoMirrored.Filled.Message,
                    onClick = { onSms(number.number) },
                )
            }
        }
    }
}
