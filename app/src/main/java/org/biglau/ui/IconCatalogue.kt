package org.biglau.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import org.biglau.R

/**
 * Die Symbole, die man einer Kachel selbst geben kann. `PLAN.md` 2.2 und 3.4.
 *
 * Bis hierher leitete die App jedes Symbol aus der Aktion ab. Das trifft meistens, aber nicht
 * immer: der Ordner mit den Bankgeschaeften traegt keinen Ordner, sondern eine Karte, und die
 * Kachel fuer den Hausarzt kein Personensymbol, sondern ein Kreuz. Ohne Wahl bleibt in so
 * einem Fall nur die Beschriftung - und die liest man langsamer als ein Bild.
 *
 * **Abweichung mit Grund:** `PLAN.md` 3.4 nennt "grob 120 Symbole". Es sind hier 50, in neun
 * Gruppen. Auf drei Zoll passen vier nebeneinander; 120 waeren dreissig Zeilen zum
 * Durchsehen, und die Wahl wird mit jedem aehnlichen Symbol schwerer statt leichter. Wer
 * eines vermisst, bekommt es dazu - eine Zeile hier.
 *
 * Gespeichert wird der **Name**, nicht das Bild: eine Sicherung soll auch dann noch lesbar
 * sein, wenn die Symbolbibliothek eine andere ist.
 */
object IconCatalogue {

    data class Group(val titleRes: Int, val names: List<String>)

    val GROUPS: List<Group> = listOf(
        Group(R.string.icons_group_communication, listOf("Call", "Message", "Email", "Chat", "Videocam", "Voicemail")),
        Group(R.string.icons_group_people, listOf("Person", "Group", "Favorite", "Star", "Elderly", "ChildCare")),
        Group(R.string.icons_group_media, listOf("MusicNote", "PhotoCamera", "Photo", "Headphones", "Movie", "Radio")),
        Group(R.string.icons_group_system, listOf("Settings", "Apps", "Wifi", "BatteryFull", "Lock", "PowerSettingsNew")),
        Group(R.string.icons_group_places, listOf("Home", "Map", "LocationOn", "Church", "Apartment", "Store")),
        Group(R.string.icons_group_health, listOf("LocalHospital", "Medication", "MedicalServices", "LocalPharmacy", "Emergency", "MonitorHeart")),
        Group(R.string.icons_group_time, listOf("Schedule", "AccessAlarm", "CalendarMonth", "Timer", "Today", "History")),
        Group(R.string.icons_group_weather, listOf("WbSunny", "Cloud", "Umbrella", "AcUnit", "Thunderstorm", "Air")),
        Group(R.string.icons_group_everyday, listOf("ShoppingCart", "Restaurant", "DirectionsBus", "DirectionsCar", "LocalTaxi", "Newspaper", "Book", "Key")),
    )

    /** Alle Namen, in der Reihenfolge der Gruppen. */
    val NAMES: List<String> = GROUPS.flatMap { it.names }

    /**
     * Das Wort zum Symbol.
     *
     * `PLAN.md` 3.6: „Keine reinen Icon-Buttons ohne Label irgendwo in der App." Eine Wand
     * aus Symbolen ohne Wort ist ein Ratespiel - fuer jemanden, der schlecht sieht, und fuer
     * einen Screenreader erst recht. Null bei einem unbekannten Namen.
     */
    fun labelFor(name: String): Int? = when (name) {
        "Call" -> R.string.icon_call
        "Message" -> R.string.icon_message
        "Email" -> R.string.icon_email
        "Chat" -> R.string.icon_chat
        "Videocam" -> R.string.icon_videocam
        "Voicemail" -> R.string.icon_voicemail
        "Person" -> R.string.icon_person
        "Group" -> R.string.icon_group
        "Favorite" -> R.string.icon_favorite
        "Star" -> R.string.icon_star
        "Elderly" -> R.string.icon_elderly
        "ChildCare" -> R.string.icon_childcare
        "MusicNote" -> R.string.icon_musicnote
        "PhotoCamera" -> R.string.icon_photocamera
        "Photo" -> R.string.icon_photo
        "Headphones" -> R.string.icon_headphones
        "Movie" -> R.string.icon_movie
        "Radio" -> R.string.icon_radio
        "Settings" -> R.string.icon_settings
        "Apps" -> R.string.icon_apps
        "Wifi" -> R.string.icon_wifi
        "BatteryFull" -> R.string.icon_batteryfull
        "Lock" -> R.string.icon_lock
        "PowerSettingsNew" -> R.string.icon_powersettingsnew
        "Home" -> R.string.icon_home
        "Map" -> R.string.icon_map
        "LocationOn" -> R.string.icon_locationon
        "Church" -> R.string.icon_church
        "Apartment" -> R.string.icon_apartment
        "Store" -> R.string.icon_store
        "LocalHospital" -> R.string.icon_localhospital
        "Medication" -> R.string.icon_medication
        "MedicalServices" -> R.string.icon_medicalservices
        "LocalPharmacy" -> R.string.icon_localpharmacy
        "Emergency" -> R.string.icon_emergency
        "MonitorHeart" -> R.string.icon_monitorheart
        "Schedule" -> R.string.icon_schedule
        "AccessAlarm" -> R.string.icon_accessalarm
        "CalendarMonth" -> R.string.icon_calendarmonth
        "Timer" -> R.string.icon_timer
        "Today" -> R.string.icon_today
        "History" -> R.string.icon_history
        "WbSunny" -> R.string.icon_wbsunny
        "Cloud" -> R.string.icon_cloud
        "Umbrella" -> R.string.icon_umbrella
        "AcUnit" -> R.string.icon_acunit
        "Thunderstorm" -> R.string.icon_thunderstorm
        "Air" -> R.string.icon_air
        "ShoppingCart" -> R.string.icon_shoppingcart
        "Restaurant" -> R.string.icon_restaurant
        "DirectionsBus" -> R.string.icon_directionsbus
        "DirectionsCar" -> R.string.icon_directionscar
        "LocalTaxi" -> R.string.icon_localtaxi
        "Newspaper" -> R.string.icon_newspaper
        "Book" -> R.string.icon_book
        "Key" -> R.string.icon_key
        else -> null
    }

    /**
     * Das Bild zu einem Namen, oder `null`.
     *
     * Null bei einem unbekannten Namen: eine Sicherung aus einer spaeteren Fassung darf eine
     * Kachel nicht leer lassen, sondern faellt auf das abgeleitete Symbol zurueck.
     */
    fun vectorFor(name: String?): ImageVector? = when (name) {
        "Call" -> Icons.Filled.Call
        "Message" -> Icons.AutoMirrored.Filled.Message
        "Email" -> Icons.Filled.Email
        "Chat" -> Icons.AutoMirrored.Filled.Chat
        "Videocam" -> Icons.Filled.Videocam
        "Voicemail" -> Icons.Filled.Voicemail
        "Person" -> Icons.Filled.Person
        "Group" -> Icons.Filled.Group
        "Favorite" -> Icons.Filled.Favorite
        "Star" -> Icons.Filled.Star
        "Elderly" -> Icons.Filled.Elderly
        "ChildCare" -> Icons.Filled.ChildCare
        "MusicNote" -> Icons.Filled.MusicNote
        "PhotoCamera" -> Icons.Filled.PhotoCamera
        "Photo" -> Icons.Filled.Photo
        "Headphones" -> Icons.Filled.Headphones
        "Movie" -> Icons.Filled.Movie
        "Radio" -> Icons.Filled.Radio
        "Settings" -> Icons.Filled.Settings
        "Apps" -> Icons.Filled.Apps
        "Wifi" -> Icons.Filled.Wifi
        "BatteryFull" -> Icons.Filled.BatteryFull
        "Lock" -> Icons.Filled.Lock
        "PowerSettingsNew" -> Icons.Filled.PowerSettingsNew
        "Home" -> Icons.Filled.Home
        "Map" -> Icons.Filled.Map
        "LocationOn" -> Icons.Filled.LocationOn
        "Church" -> Icons.Filled.Church
        "Apartment" -> Icons.Filled.Apartment
        "Store" -> Icons.Filled.Store
        "LocalHospital" -> Icons.Filled.LocalHospital
        "Medication" -> Icons.Filled.Medication
        "MedicalServices" -> Icons.Filled.MedicalServices
        "LocalPharmacy" -> Icons.Filled.LocalPharmacy
        "Emergency" -> Icons.Filled.Emergency
        "MonitorHeart" -> Icons.Filled.MonitorHeart
        "Schedule" -> Icons.Filled.Schedule
        "AccessAlarm" -> Icons.Filled.AccessAlarm
        "CalendarMonth" -> Icons.Filled.CalendarMonth
        "Timer" -> Icons.Filled.Timer
        "Today" -> Icons.Filled.Today
        "History" -> Icons.Filled.History
        "WbSunny" -> Icons.Filled.WbSunny
        "Cloud" -> Icons.Filled.Cloud
        "Umbrella" -> Icons.Filled.Umbrella
        "AcUnit" -> Icons.Filled.AcUnit
        "Thunderstorm" -> Icons.Filled.Thunderstorm
        "Air" -> Icons.Filled.Air
        "ShoppingCart" -> Icons.Filled.ShoppingCart
        "Restaurant" -> Icons.Filled.Restaurant
        "DirectionsBus" -> Icons.Filled.DirectionsBus
        "DirectionsCar" -> Icons.Filled.DirectionsCar
        "LocalTaxi" -> Icons.Filled.LocalTaxi
        "Newspaper" -> Icons.Filled.Newspaper
        "Book" -> Icons.Filled.Book
        "Key" -> Icons.Filled.Key
        else -> null
    }
}
