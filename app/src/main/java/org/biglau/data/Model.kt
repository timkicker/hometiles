package org.biglau.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Aktuelle Schema-Version. Bei jedem Bruch erhoehen und eine Migration ergaenzen. */
const val CONFIG_VERSION = 1

/** Eingebaute Aktionen ohne eigene Parameter. */
@Serializable
enum class Builtin {
    DIALER, MESSAGES, CONTACTS, CAMERA, CLOCK, APP_LIST, SETTINGS,
    FLASHLIGHT, SOS, NEXT_SCREEN, PREV_SCREEN, HOME_SCREEN, BATTERY, MISSED_CALLS,
    WIFI, BLUETOOTH, AIRPLANE, RINGER, SIGNAL,
    MOBILE_DATA, LOCATION, BRIGHTNESS, ANDROID_SETTINGS, CALL_LOG,
    FAVOURITES, RECENT_APPS,
}

/** Was beim Antippen einer Kontaktkachel passiert. */
@Serializable
enum class ContactMode { CALL, SMS, ASK }

@Serializable
sealed interface ButtonAction {

    @Serializable
    @SerialName("none")
    data object None : ButtonAction

    @Serializable
    @SerialName("app")
    data class App(val packageName: String, val activityName: String) : ButtonAction

    @Serializable
    @SerialName("contact")
    data class Contact(
        val name: String,
        val number: String,
        val photoUri: String? = null,
        val mode: ContactMode = ContactMode.CALL,
    ) : ButtonAction

    @Serializable
    @SerialName("screen")
    data class GoToScreen(val screenId: String) : ButtonAction

    /**
     * Ein Ordner. Sein Inhalt ist ein ganz gewoehnlicher [Screen] mit [ScreenKind.FOLDER] -
     * damit gilt jede Regel, die fuer Screens schon geprueft ist, hier ohne Zutun weiter.
     */
    @Serializable
    @SerialName("folder")
    data class Folder(val screenId: String) : ButtonAction

    /**
     * Eine Webseite. Die Adresse steht in der Kachel, nicht in einer Liste - sonst müsste
     * man sie doppelt pflegen.
     */
    @Serializable
    @SerialName("link")
    data class Link(val url: String) : ButtonAction

    @Serializable
    @SerialName("shortcut")
    data class Shortcut(
        val packageName: String,
        val shortcutId: String,
        val label: String,
    ) : ButtonAction

    @Serializable
    @SerialName("widget")
    data class Widget(
        /** Flach geschriebene ComponentName des Anbieters. */
        val provider: String,
        /** Vom AppWidgetHost vergebene Kennung. */
        val widgetId: Int,
        val label: String = "",
    ) : ButtonAction

    @Serializable
    @SerialName("builtin")
    data class Action(val builtin: Builtin) : ButtonAction
}

@Serializable
data class Button(
    val action: ButtonAction = ButtonAction.None,
    /** null = Beschriftung automatisch aus der Aktion ableiten. */
    val label: String? = null,
    /** Index in die Kachelpalette; -1 = automatisch aus der Position. */
    val colorIndex: Int = -1,
    /** Eigene Farbe als ARGB; ueberschreibt colorIndex. */
    val customColor: Long? = null,
    val blink: Boolean = true,
    val longPress: ButtonAction? = null,
)

/**
 * Eine Zelle belegt ein Rechteck im Raster. w und h groesser 1 sind das, was im Original
 * "join" und "stretch" heisst - deshalb steckt die Spannweite von Anfang an im Modell.
 */
@Serializable
data class Cell(
    val x: Int,
    val y: Int,
    val w: Int = 1,
    val h: Int = 1,
    val button: Button = Button(),
) {
    val area: Int get() = w * h

    fun covers(cx: Int, cy: Int): Boolean = cx in x until (x + w) && cy in y until (y + h)
}

@Serializable
sealed interface Background {
    @Serializable @SerialName("theme") data object Theme : Background
    @Serializable @SerialName("color") data class Solid(val argb: Long) : Background
    @Serializable @SerialName("image") data class Image(val uri: String) : Background
}

/**
 * Ein Screen steht fuer sich; ein Ordner gehoert der Kachel, die ihn oeffnet, und wird als
 * Ueberlagerung gezeigt. Der Unterschied ist bewusst nur eine Marke und kein eigener Typ:
 * eine zweite Sorte Kachelraster hiesse jede Regel zweimal zu pflegen.
 */
@Serializable
enum class ScreenKind { SCREEN, FOLDER }

@Serializable
data class Screen(
    val id: String,
    val name: String,
    val cols: Int = 2,
    val rows: Int = 3,
    val cells: List<Cell> = emptyList(),
    val background: Background = Background.Theme,
    val kind: ScreenKind = ScreenKind.SCREEN,
) {
    val isFolder: Boolean get() = kind == ScreenKind.FOLDER

    /** Belegte Zelle an dieser Rasterposition, sofern eine sie ueberdeckt. */
    fun cellAt(x: Int, y: Int): Cell? = cells.firstOrNull { it.covers(x, y) }

    /** Rasterplaetze, die keine Zelle belegt - dort zeichnen wir Platzhalter. */
    fun freeSlots(): List<Pair<Int, Int>> =
        (0 until rows).flatMap { y -> (0 until cols).map { x -> x to y } }
            .filter { (x, y) -> cellAt(x, y) == null }
}

@Serializable
enum class ThemeName { DARK, HIGH_CONTRAST, LIGHT }

@Serializable
enum class LabelPosition { BOTTOM_LEFT, BOTTOM_CENTER, TOP_LEFT, HIDDEN }

@Serializable
data class Appearance(
    val theme: ThemeName = ThemeName.DARK,
    /** Faktor auf unsere eigene, aus der Zellhoehe berechnete Schriftgroesse. */
    val textScale: Float = 1.0f,
    val labelPosition: LabelPosition = LabelPosition.BOTTOM_LEFT,
    val showIcons: Boolean = true,
    val gutterDp: Int = 4,
    /** Aussenrand in Prozent der Bildschirmbreite. */
    val safeBorderPercent: Int = 2,
    val cornerRadiusDp: Int = 12,
    val fullScreen: Boolean = false,
    /** Zeigt die Uhr-Kachel auch Wochentag und Datum? */
    val clockShowsDate: Boolean = true,
    /** Zeile ueber dem Raster mit Uhrzeit, Datum und Ladestand. */
    val showHeader: Boolean = true,
)

@Serializable
data class Accessibility(
    /** Langdruck liest die Beschriftung vor. */
    val speakOnLongPress: Boolean = false,
    /** Langdruck zeigt die Beschriftung gross ueber dem ganzen Bildschirm. */
    val popupOnLongPress: Boolean = false,
    /** Zwei Knöpfe unter langen Listen statt Wischen - für unruhige Hände. */
    val scrollButtons: Boolean = false,
)

@Serializable
data class Behaviour(
    val hapticFeedback: Boolean = true,
    val blinkOnNotification: Boolean = true,
    val swipeBetweenScreens: Boolean = false,
    val homeKeyReturnsToStart: Boolean = true,
    val accessibility: Accessibility = Accessibility(),
)

@Serializable
data class Security(
    /** Salted Hash, null = keine PIN gesetzt. */
    val pin: String? = null,
    val pinProtectsEditor: Boolean = true,
)

@Serializable
data class SosConfig(
    val numbers: List<String> = emptyList(),
    /** Leer heisst: noch nicht gesetzt - dann gilt der Text in der Sprache des Telefons. */
    val message: String = "",
    val countdownSeconds: Int = 5,
    val sendLocation: Boolean = true,
    val callAfterSms: String? = null,
)

@Serializable
data class AppsConfig(
    /** Schluessel oder Paketnamen, die in der Liste nicht erscheinen. */
    val hidden: Set<String> = emptySet(),
    /** Zuletzt gestartet, neueste zuerst. */
    val recent: List<String> = emptyList(),
    /** Wie viele davon oben in der Liste stehen; 0 blendet die Reihe aus. */
    val recentCount: Int = 4,
)

@Serializable
data class SpeedDialTarget(val name: String, val number: String)

@Serializable
data class PhoneConfig(
    /** Taste (als Zeichenkette, damit JSON es mag) auf Ziel. */
    val speedDial: Map<String, SpeedDialTarget> = emptyMap(),
)

@Serializable
data class ContactsConfig(
    /** Sortierung: nach Vornamen oder nach Nachnamen. */
    val sortBySurname: Boolean = false,
    /** Sucht die Kontaktsuche auch in den Telefonnummern? */
    val searchNumbers: Boolean = true,
    val favouritesFirst: Boolean = true,
)

@Serializable
data class LauncherConfig(
    val version: Int = CONFIG_VERSION,
    val screens: List<Screen> = listOf(Defaults.mainScreen()),
    val homeScreenId: String = Defaults.MAIN_ID,
    val swipeOrder: List<String> = emptyList(),
    val appearance: Appearance = Appearance(),
    val behaviour: Behaviour = Behaviour(),
    val security: Security = Security(),
    val sos: SosConfig = SosConfig(),
    val apps: AppsConfig = AppsConfig(),
    val contacts: ContactsConfig = ContactsConfig(),
    val phone: PhoneConfig = PhoneConfig(),
    /** Ist der Erststart-Assistent durchlaufen? */
    val wizardDone: Boolean = false,
) {
    fun screenById(id: String): Screen? = screens.firstOrNull { it.id == id }

    val homeScreen: Screen get() = screenById(homeScreenId) ?: screens.first()
}
