package dev.kicker.hometiles.data

import dev.kicker.hometiles.toggles.SosCountdown
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** raise on every break and add a migration. */
const val CONFIG_VERSION = 1

@Serializable
enum class Builtin {
    DIALER, MESSAGES, CONTACTS, CAMERA, CLOCK, CALCULATOR, APP_LIST, SETTINGS,
    FLASHLIGHT, SOS, NEXT_SCREEN, PREV_SCREEN, HOME_SCREEN, BATTERY, MISSED_CALLS,
    WIFI, BLUETOOTH, AIRPLANE, RINGER, SIGNAL,
    MOBILE_DATA, LOCATION, BRIGHTNESS, ANDROID_SETTINGS, CALL_LOG,
    FAVOURITES, RECENT_APPS,
}

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
     * a folder. its contents are an ordinary [Screen] with [ScreenKind.FOLDER], so every rule
     * already proven for screens keeps holding here without extra work.
     */
    @Serializable
    @SerialName("folder")
    data class Folder(val screenId: String) : ButtonAction

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
        /** the provider's flattened component name. */
        val provider: String,
        /** handed out by the widget host. */
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
    /** null derives the label from the action. */
    val label: String? = null,
    /**
     * the icon **name** from `IconCatalogue`, not the image, so a backup stays readable with a
     * different icon set. an unknown name falls back to the derived icon.
     */
    val iconName: String? = null,
    /** index into the tile palette; -1 derives it from the position. */
    val colorIndex: Int = -1,
    /**
     * a free hue in degrees. beats [colorIndex] and is itself overruled by the contrast theme.
     *
     * the hue is stored, not the finished colour: the brightness that goes with it depends on
     * the theme, and a value computed in the dark theme can miss the tile-against-background
     * threshold in the light one.
     */
    val colorHue: Float? = null,
    val blink: Boolean = true,
    val longPress: ButtonAction? = null,
)

/** w and h above 1 are what the original calls "join" and "stretch". */
@Serializable
data class Cell(
    val x: Int,
    val y: Int,
    val w: Int = 1,
    val h: Int = 1,
    val button: Button = Button(),
) {

    fun covers(cx: Int, cy: Int): Boolean = cx in x until (x + w) && cy in y until (y + h)
}

/** a screen background. deliberately **without** an image, see `ScreenBackground`. */
@Serializable
sealed interface Background {
    @Serializable @SerialName("theme") data object Theme : Background
    @Serializable @SerialName("color") data class Solid(val argb: Long) : Background
}

/**
 * a screen stands on its own; a folder belongs to the tile that opens it. the difference is
 * deliberately a marker and not a separate type, or every rule would need maintaining twice.
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

    fun cellAt(x: Int, y: Int): Cell? = cells.firstOrNull { it.covers(x, y) }

    /**
     * how many **tiles** lie here, not how many cells: a cell without an action is a free
     * slot. counting `cells.size` and calling it tiles put two different numbers on the same
     * setup in three places.
     */
    val tileCount: Int get() = cells.count { it.button.action != ButtonAction.None }

    fun freeSlots(): List<Pair<Int, Int>> =
        (0 until rows).flatMap { y -> (0 until cols).map { x -> x to y } }
            .filter { (x, y) -> cellAt(x, y) == null }
}

/** SYSTEM is a question to the phone, not a look; `paletteFor` resolves it. */
@Serializable
enum class ThemeName { DARK, HIGH_CONTRAST, LIGHT, SYSTEM }

@Serializable
enum class LabelPosition { BOTTOM_LEFT, BOTTOM_CENTER, TOP_LEFT, HIDDEN }

/** the app's language, independent of the phone's. `PLAN.md` 4.9. */
@Serializable
enum class Language { SYSTEM, GERMAN, ENGLISH, FRENCH, SPANISH, ITALIAN }

/** landscape is narrow on three inches, but a phone in a holder may need it. */
@Serializable
enum class ScreenOrientation { PORTRAIT, LANDSCAPE, AUTO }

@Serializable
enum class ClockDisplay { OFF, TIME, TIME_DATE, TIME_DATE_WEEKDAY }

/**
 * [IF_ROOM] is the interesting one: on a flat tile an icon pushes the label to the edge, and
 * half a word is worse than no picture.
 */
@Serializable
enum class IconVisibility { ALWAYS, IF_ROOM, NEVER }

/** atkinson hyperlegible is the default because it was drawn for reading with poor eyes. */
@Serializable
enum class FontChoice { HYPERLEGIBLE, SYSTEM }

@Serializable
data class Appearance(
    val theme: ThemeName = ThemeName.DARK,
    val language: Language = Language.SYSTEM,
    val font: FontChoice = FontChoice.HYPERLEGIBLE,
    /** factor on our own font size, which is computed from the cell height. */
    val textScale: Float = 1.0f,
    val labelPosition: LabelPosition = LabelPosition.BOTTOM_LEFT,
    /** tile label, 0.5-1.5, on top of the global text scale. */
    val labelScale: Float = 1.0f,
    /** icon size as a percentage of the shorter cell edge, 20-60. */
    val iconPercent: Int = 40,
    /** old yes/no. kept so a saved config does not fall back silently; [icons] counts. */
    val showIcons: Boolean = true,
    val iconVisibility: IconVisibility? = null,
    /**
     * drop the label when it does not fit in two lines. `PLAN.md` 3.2. off by default: a cut
     * word is worse than none on three inches, but that trade belongs to the user - some
     * recognise a tile by three letters rather than by its picture.
     */
    val hideCutLabels: Boolean = false,
    val gutterDp: Int = 4,
    /** outer margin as a percentage of the screen width. */
    val safeBorderPercent: Int = 2,
    val cornerRadiusDp: Int = 12,
    val fullScreen: Boolean = false,
    /** old with/without date. kept for the same reason; [clock] counts. */
    val clockShowsDate: Boolean = true,
    val clockDisplay: ClockDisplay? = null,
    /** clock in the header, 0.75-2.0, on top of the global text scale. */
    val clockScale: Float = 1.0f,
    val orientation: ScreenOrientation = ScreenOrientation.PORTRAIT,
    val showHeader: Boolean = true,
) {
    val clock: ClockDisplay
        get() = clockDisplay
            ?: if (clockShowsDate) ClockDisplay.TIME_DATE_WEEKDAY else ClockDisplay.TIME

    /** sets both fields, so old and new never contradict. */
    fun withClock(display: ClockDisplay): Appearance = copy(
        clockDisplay = display,
        clockShowsDate = display == ClockDisplay.TIME_DATE ||
            display == ClockDisplay.TIME_DATE_WEEKDAY,
    )

    val icons: IconVisibility
        get() = iconVisibility ?: if (showIcons) IconVisibility.ALWAYS else IconVisibility.NEVER

    fun withIcons(choice: IconVisibility): Appearance = copy(
        iconVisibility = choice,
        showIcons = choice != IconVisibility.NEVER,
    )
}

@Serializable
data class Accessibility(
    val speakOnLongPress: Boolean = false,
    val popupOnLongPress: Boolean = false,
    /** two buttons under long lists instead of swiping, for unsteady hands. */
    val scrollButtons: Boolean = false,
)

/**
 * how a tile is triggered.
 *
 * for shaky hands [LONG] is the most important setting in the app: brushing past starts
 * nothing any more. the price is half a second on every launch, so it is a decision and not
 * a default.
 */
@Serializable
enum class PressMode { SHORT, LONG }

/**
 * [LIGHT] is a nudge, [STRONG] a noticeable thump. thick fingers, gloves or little feeling in
 * the hands miss the nudge and take a hit for a miss.
 */
@Serializable
enum class HapticStrength { OFF, LIGHT, STRONG }

@Serializable
data class Behaviour(
    val pressMode: PressMode = PressMode.SHORT,
    /** old on/off. kept so a saved config does not fall back silently; [haptics] counts. */
    val hapticFeedback: Boolean = true,
    val hapticStrength: HapticStrength? = null,
    /**
     * notices stay until tapped away. a two-second flash tells a slow reader only that
     * something flickered.
     */
    val confirmMessages: Boolean = false,
    val blinkOnNotification: Boolean = true,
    val swipeBetweenScreens: Boolean = false,
    val homeKeyReturnsToStart: Boolean = true,
    val accessibility: Accessibility = Accessibility(),
) {
    val haptics: HapticStrength
        get() = hapticStrength
            ?: if (hapticFeedback) HapticStrength.LIGHT else HapticStrength.OFF

    fun withHaptics(strength: HapticStrength): Behaviour = copy(
        hapticStrength = strength,
        hapticFeedback = strength != HapticStrength.OFF,
    )
}

@Serializable
data class Security(
    /** salted hash; null means no pin. */
    val pin: String? = null,
    val pinProtectsEditor: Boolean = true,
    /** the app list is the way to every app that lies on no tile. */
    val pinProtectsAppList: Boolean = false,
    /** clearing the call log cannot be undone. */
    val pinProtectsCallLogDelete: Boolean = true,
)

@Serializable
data class SosConfig(
    val numbers: List<String> = emptyList(),
    /** empty means unset, and then the text in the phone's language applies. */
    val message: String = "",
    /** the default lives in [SosCountdown], not here, or the two would drift apart. */
    val countdownSeconds: Int = SosCountdown.DEFAULT_SECONDS,
    val sendLocation: Boolean = true,
    /** **off by default**: an unexpected siren is why people switch the sos button off. */
    val alarmSound: Boolean = false,
    val alarmFlash: Boolean = false,
)

@Serializable
data class AppsConfig(
    val hidden: Set<String> = emptySet(),
    /** most recently started first. */
    val recent: List<String> = emptyList(),
    /** how many of them head the list; 0 hides the row. */
    val recentCount: Int = 4,
    /** apps that start without the pin. only applies with [lockOthers] on and a pin set. */
    val allowed: Set<String> = emptySet(),
    val lockOthers: Boolean = false,
)

@Serializable
data class SpeedDialTarget(val name: String, val number: String)

@Serializable
data class SmsConfig(
    /**
     * numbers whose messages stay out of the list. separate from the call block list: not
     * wanting to speak to someone does not mean not wanting to read them, or the reverse.
     */
    val hiddenNumbers: List<String> = emptyList(),
    val hiddenWords: List<String> = emptyList(),
    val conversationScale: Float = 1.0f,
    /**
     * vibration for a new message, in milliseconds. it lives in the notification channel and
     * not in a vibrate call of our own, because only that respects do-not-disturb.
     */
    val vibrationMs: Int = 500,
    /**
     * **off by default**: a notice that takes the whole screen and shows the text on the lock
     * screen is also seen by whoever is holding the phone at that moment.
     */
    val fullScreenAlert: Boolean = false,
    /** minutes between reminders about unread messages; 0 means never. */
    val repeatMinutes: Int = 0,
    /**
     * **off by default**: an accidentally sent half message is embarrassing, an extra question
     * before *every* message is a constant chore. shaky hands switch it on.
     */
    val confirmBeforeSending: Boolean = false,
    val sendButtonAbove: Boolean = false,
    val sendButtonLarge: Boolean = false,
)

@Serializable
data class PhoneConfig(
    /** key as a string, because json likes it that way. */
    val speedDial: Map<String, SpeedDialTarget> = emptyMap(),
    /**
     * call types kept out of the list, as names, so the data layer needs nothing from the
     * phone layer. a hide list, not a show list: a type android adds later is then visible by
     * itself instead of silently missing.
     */
    val hiddenCallTypes: Set<String> = emptySet(),
    val callGrouping: CallGrouping = CallGrouping.NUMBER,
    val callerPhoto: CallerPhoto = CallerPhoto.SMALL,
    val audioRoute: AudioRoute = AudioRoute.EARPIECE,
    val speakerOnOutgoing: Boolean = false,
    val blockedNumbers: List<String> = emptyList(),
    /**
     * when the call log was last open, so the badge needs no write permission.
     *
     * counting the system's "new" flag and clearing it on open requires `WRITE_CALL_LOG`, and
     * without that the number on the tile would never go out however often the list is read.
     * the system flag stays in the condition as well, so tidying up in the system phone app
     * clears the number here too.
     */
    val lastSeenMissedAt: Long = 0L,
)

@Serializable
enum class AudioRoute { EARPIECE, SPEAKER, BLUETOOTH }

/**
 * four steps and no switch: poor eyes want the face large, while a phone in a pocket needs
 * the name readable, and a full-screen photo pushes it down.
 */
@Serializable
enum class CallerPhoto { OFF, SMALL, HALF, FULL }

/**
 * an enum and not a switch: "by nothing" is not the off state of "by number" but its own
 * view, for anyone who wants to know when exactly somebody called three times.
 */
@Serializable
enum class CallGrouping { NONE, NUMBER, DIRECTION }

@Serializable
data class ContactsConfig(
    val sortBySurname: Boolean = false,
    val searchNumbers: Boolean = true,
    val favouritesFirst: Boolean = true,
)

@Serializable
data class LauncherConfig(
    val version: Int = CONFIG_VERSION,
    val screens: List<Screen> = listOf(Defaults.mainScreen()),
    val homeScreenId: String = Defaults.MAIN_ID,
    val swipeOrder: List<String> = emptyList(),
    /**
     * screens outside the swipe chain. an exception list, not a membership list, so a screen
     * created later is part of it by itself instead of silently missing.
     */
    val swipeExcluded: Set<String> = emptySet(),
    val appearance: Appearance = Appearance(),
    val behaviour: Behaviour = Behaviour(),
    val security: Security = Security(),
    val sos: SosConfig = SosConfig(),
    val apps: AppsConfig = AppsConfig(),
    val contacts: ContactsConfig = ContactsConfig(),
    val phone: PhoneConfig = PhoneConfig(),
    val sms: SmsConfig = SmsConfig(),
    val wizardDone: Boolean = false,
) {
    fun screenById(id: String): Screen? = screens.firstOrNull { it.id == id }

    val homeScreen: Screen get() = screenById(homeScreenId) ?: screens.first()
}
