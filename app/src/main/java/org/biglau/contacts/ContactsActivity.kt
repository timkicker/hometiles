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
import org.biglau.ui.bigSp
import org.biglau.ui.BigLauActivity
import org.biglau.R
import kotlinx.coroutines.launch
import org.biglau.actions.Intents
import org.biglau.data.ConfigStore
import org.biglau.search.TextSearch
import org.biglau.phone.PhoneNumbers
import org.biglau.ui.BigHeading
import org.biglau.ui.BigIconButton
import org.biglau.ui.Notice
import org.biglau.ui.PermissionGate
import org.biglau.ui.PermissionState
import org.biglau.ui.BigRow
import org.biglau.ui.SettingsLink
import org.biglau.ui.BigSearchField
import org.biglau.ui.ScrollButtons
import org.biglau.ui.ContactAvatar
import org.biglau.ui.dpSp
import org.biglau.ui.theme.BigLauTheme
import org.biglau.ui.theme.LocalBigPalette

/**
 * our own contact list: large rows with a photo, a search, and a detail view where calling
 * and writing each get a whole row instead of a small icon.
 */
class ContactsActivity : BigLauActivity() {

    companion object {
        /** show only the favourites, from the favourites tile. */
        const val EXTRA_FAVOURITES = "favouritesOnly"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ConfigStore.get(this)
        val repository = ContactRepository.get(this)

        setContent {
            val config by store.config.collectAsStateWithLifecycle()
            // `resumes` as the key: on a permanently refused permission this screen sends
            // people into the app settings, and nothing comes back from there. see
            // `BigLauActivity.resumes`.
var granted by remember(resumes.intValue) { mutableStateOf(repository.hasPermission()) }
            var all by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
            var query by rememberSaveable { mutableStateOf("") }
            // from the favourites tile: only the starred ones, with no search in front.
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

            // the system asks by itself on the first open. after a refusal the screen used
            // to close without a word: something had been tapped and simply vanished.
            LaunchedEffect(Unit) {
                if (!granted && !deniedOnce) ask.launch(Manifest.permission.READ_CONTACTS)
            }

            // on every return, not only the first time: a row leads into the phone book's
            // editor, which sends no result back, and a changed number stayed old here -
            // call straight away dialled it too.
            LaunchedEffect(granted, resumes.intValue) {
                if (granted) {
                    // on returning the list already stands there; a second loading would
                    // look as if the screen started over.
                    if (all.isEmpty()) loading = true
                    all = repository.load(resources)
                    loading = false
                    // the open contact comes from the same list: deleted in the phone book,
                    // there is nothing left to show, so back to the list.
                    selected = selected?.let { offen -> all.firstOrNull { it.id == offen.id } }
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
                            // not `contacts_permission`: that sentence gives the tile
                            // editor's reason. here one arrives from the contacts tile
                            // wanting to call or write, and a true sentence on the wrong
                            // screen makes the question look unnecessary.
                            explanation = stringResource(R.string.contacts_permission_list),
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
                                            all = repository.load(resources)
                                            selected = all.firstOrNull { it.id == current.id }
                                        } else {
                                            Notice.show(this@ContactsActivity, R.string.contacts_star_failed)
                                        }
                                    }
                                } else {
                                    askWrite.launch(Manifest.permission.WRITE_CONTACTS)
                                }
                            },
                            onEdit = { Intents.open(this@ContactsActivity, repository.editIntent(current.id)) },
                        )
                    } else {
                        ContactList(
                            contacts = shown,
                            // `all` and not `shown`: whether there are contacts at all
                            // decides the sentence for an empty list.
                            hatKontakte = all.isNotEmpty(),
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
                                    SettingsLink.toPage(
                                        this@ContactsActivity,
                                        SettingsLink.PAGE_CONTACTS,
                                    ),
                                )
                            },
                            onShowAll = { favouritesOnly = false },
                            onCreate = { Intents.open(this@ContactsActivity, repository.createIntent()) },
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
    /**
     * is there anything in the phone book at all?
     *
     * apart from [contacts], which is already filtered. otherwise an empty list always says
     * no contact matches, even when nobody searched and the phone simply has none.
     */
    hatKontakte: Boolean,
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
                // sorting is a side matter and may be an icon; adding a contact is an
                // action and keeps its word.
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
            // without this way an empty favourites list is a dead end, and a full one lets
            // nobody reach the remaining contacts.
            //
            // and not while reading: above the loading check, the screen first said no
            // favourites yet and then they jumped in. see LadenTest.
            if (contacts.isEmpty() && !loading) {
                Text(
                    text = stringResource(R.string.favourites_none),
                    color = palette.onBackground,
                    fontSize = bigSp(17f),
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
            // here and not at the end of the list: at 338 contacts it would have scrolled
            // out of reach.
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
                fontSize = bigSp(18f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
        } else if (contacts.isEmpty()) {
            Text(
                // no match only when there was something to match, as the app list does
                // with `all.isNotEmpty() && shown.isEmpty()`.
                text = stringResource(
                    if (hatKontakte) R.string.contacts_no_match else R.string.contacts_none,
                ),
                color = palette.onBackground,
                fontSize = bigSp(18f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
            // typing a number while only names are searched otherwise gives a true answer
            // without its reason.
            if (!searchNumbers && ContactSort.looksLikeNumber(query)) {
                Text(
                    text = stringResource(R.string.contacts_numbers_not_searched),
                    color = palette.onBackground,
                    fontSize = bigSp(15f),
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
                    // in blocks, as in the call log and the messages: the same number used
                    // to look different in three places.
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
 * the detail view. each number gets two whole rows, call and write, instead of two small
 * icons side by side: on three inches that is the difference between hitting and missing.
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
