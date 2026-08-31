package org.biglau.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Aktuelle Schema-Version. Bei jedem Bruch erhoehen und eine Migration ergaenzen. */
const val CONFIG_VERSION = 1

/** Eingebaute Aktionen ohne eigene Parameter. */
@Serializable
enum class Builtin {
    DIALER, MESSAGES, CONTACTS, CAMERA, CLOCK, APP_LIST, SETTINGS,
    FLASHLIGHT, SOS, NEXT_SCREEN, PREV_SCREEN, HOME_SCREEN, BATTERY,
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

@Serializable
data class Screen(
    val id: String,
    val name: String,
    val cols: Int = 2,
    val rows: Int = 3,
    val cells: List<Cell> = emptyList(),
    val background: Background = Background.Theme,
) {
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
data class Behaviour(
    val hapticFeedback: Boolean = true,
    val blinkOnNotification: Boolean = true,
    val swipeBetweenScreens: Boolean = false,
    val homeKeyReturnsToStart: Boolean = true,
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
    val message: String = "Ich brauche Hilfe.",
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
) {
    fun screenById(id: String): Screen? = screens.firstOrNull { it.id == id }

    val homeScreen: Screen get() = screenById(homeScreenId) ?: screens.first()
}
