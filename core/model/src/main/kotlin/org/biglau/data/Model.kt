package org.biglau.data

import org.biglau.toggles.SosCountdown
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Aktuelle Schema-Version. Bei jedem Bruch erhoehen und eine Migration ergaenzen. */
const val CONFIG_VERSION = 1

/** Eingebaute Aktionen ohne eigene Parameter. */
@Serializable
enum class Builtin {
    DIALER, MESSAGES, CONTACTS, CAMERA, CLOCK, CALCULATOR, APP_LIST, SETTINGS,
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
    /**
     * Selbst gewaehltes Symbol, `null` = aus der Aktion abgeleitet. `PLAN.md` 2.2 und 3.4.
     *
     * Gespeichert wird der **Name** aus [org.biglau.ui.IconCatalogue], nicht das Bild: eine
     * Sicherung soll auch dann lesbar bleiben, wenn die Symbolbibliothek eine andere ist.
     * Ein unbekannter Name faellt auf das abgeleitete Symbol zurueck, statt die Kachel leer
     * zu lassen.
     */
    val iconName: String? = null,
    /** Index in die Kachelpalette; -1 = automatisch aus der Position. */
    val colorIndex: Int = -1,
    /**
     * Frei gewaehlter Farbton in Grad, die dritte Art aus PLAN.md 4.2 neben Auto und
     * Palette. Schlaegt [colorIndex], wird aber vom Kontrast-Thema uebergangen.
     *
     * Gespeichert wird der Ton und nicht die fertige Farbe: die Helligkeit dazu haengt am
     * Thema, und ein im dunklen Thema ausgerechneter Wert kann im hellen die Schwelle fuer
     * die Kachel gegen den Hintergrund verfehlen. Siehe [org.biglau.ui.theme.FreeTileColor].
     *
     * Der Vorlaeufer hiess `customColor` und trug einen fertigen ARGB-Wert; er wurde *vor*
     * der Palette gelesen und waere damit auch im Kontrast-Thema durchgeschlagen.
     */
    val colorHue: Float? = null,
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

    fun covers(cx: Int, cy: Int): Boolean = cx in x until (x + w) && cy in y until (y + h)
}

/**
 * Der Hintergrund eines Screens. PLAN.md 4.1.
 *
 * **Ohne Bild, und das mit Absicht** - siehe [org.biglau.ui.theme.ScreenBackground]. Der
 * Typ stand hier von Anfang an und wurde nie gemalt: ein `else`-Zweig verschluckte ihn.
 * Ein Fall im Modell, den niemand behandelt, ist eine Zusage, die in der Sicherungsdatei
 * steht und nichts tut.
 */
@Serializable
sealed interface Background {
    @Serializable @SerialName("theme") data object Theme : Background
    @Serializable @SerialName("color") data class Solid(val argb: Long) : Background
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

    /**
     * Wie viele **Kacheln** hier liegen - nicht wie viele Zellen.
     *
     * Der Unterschied ist der zwischen dem Modell und dem, was jemand sieht: eine Zelle
     * ohne Aktion ist ein freier Platz, keine Kachel. An drei Stellen wurde `cells.size`
     * gezaehlt und "Kacheln" dazu gesagt - beim Laden einer Sicherung, beim Aufraeumen
     * eines verwaisten Ordners und in der Rueckfrage vor dem Loeschen. Die Rueckfrage vor
     * dem Zuruecksetzen zaehlte richtig, und so standen zwei Zahlen fuer dieselbe
     * Einrichtung nebeneinander. Deshalb gibt es die Zahl jetzt nur einmal.
     */
    val tileCount: Int get() = cells.count { it.button.action != ButtonAction.None }

    /** Rasterplaetze, die keine Zelle belegt - dort zeichnen wir Platzhalter. */
    fun freeSlots(): List<Pair<Int, Int>> =
        (0 until rows).flatMap { y -> (0 until cols).map { x -> x to y } }
            .filter { (x, y) -> cellAt(x, y) == null }
}

@Serializable
/**
 * SYSTEM ist kein eigenes Aussehen, sondern eine Frage ans Telefon: dunkel oder hell.
 * Aufgeloest wird es in [org.biglau.ui.theme.paletteFor], damit an keiner Stelle eine
 * Palette fuer SYSTEM gesucht wird, die es nicht gibt.
 */
enum class ThemeName { DARK, HIGH_CONTRAST, LIGHT, SYSTEM }

@Serializable
enum class LabelPosition { BOTTOM_LEFT, BOTTOM_CENTER, TOP_LEFT, HIDDEN }

/** Die Sprache der App - unabhaengig von der des Telefons. Siehe PLAN.md 4.9. */
@Serializable
enum class Language { SYSTEM, GERMAN, ENGLISH }

/**
 * Wie sich der Bildschirm dreht. PLAN.md 4.2.
 *
 * [PORTRAIT] ist die Vorgabe und war bisher fest verdrahtet. Auf drei Zoll ist quer
 * schmal: die Zeilen werden flach, und die Beschriftung bekommt kaum noch Hoehe. Wer das
 * Telefon aber in einer Halterung hat - am Fahrrad, am Rollator, am Bett -, braucht es
 * vielleicht genau so.
 */
@Serializable
enum class ScreenOrientation { PORTRAIT, LANDSCAPE, AUTO }

/**
 * Was die Uhr zeigt - in der Kopfzeile und auf der Uhr-Kachel. PLAN.md 4.2.
 */
@Serializable
enum class ClockDisplay { OFF, TIME, TIME_DATE, TIME_DATE_WEEKDAY }

/**
 * Ob ein Symbol auf der Kachel steht. PLAN.md 4.2.
 *
 * [IF_ROOM] ist die interessante Stufe: auf einer flachen Kachel drueckt ein Symbol die
 * Beschriftung an den Rand, und ein halbes Wort ist schlechter als gar kein Bild. Wo es
 * eng wird, faellt das Symbol weg und das Wort bekommt den Platz.
 */
@Serializable
enum class IconVisibility { ALWAYS, IF_ROOM, NEVER }

/**
 * Die Schrift. Atkinson Hyperlegible ist die Vorgabe, weil sie fuers Lesen bei schlechten
 * Augen gezeichnet wurde - siehe [org.biglau.ui.theme.Hyperlegible].
 */
@Serializable
enum class FontChoice { HYPERLEGIBLE, SYSTEM }

@Serializable
data class Appearance(
    val theme: ThemeName = ThemeName.DARK,
    val language: Language = Language.SYSTEM,
    val font: FontChoice = FontChoice.HYPERLEGIBLE,
    /** Faktor auf unsere eigene, aus der Zellhoehe berechnete Schriftgroesse. */
    val textScale: Float = 1.0f,
    val labelPosition: LabelPosition = LabelPosition.BOTTOM_LEFT,
    /** Beschriftung auf der Kachel, 0,5-1,5 - zusaetzlich zur globalen Textgroesse. */
    val labelScale: Float = 1.0f,
    /** Icongroesse als Prozent der kuerzeren Zellenkante, 20-60. */
    val iconPercent: Int = 40,
    /**
     * Alt: nur ja oder nein. Bleibt stehen, damit eine gesicherte Konfiguration aus einer
     * frueheren Fassung nicht stumm auf die Vorgabe zurueckfaellt. [Appearance.icons] zaehlt.
     */
    val showIcons: Boolean = true,
    val iconVisibility: IconVisibility? = null,
    /**
     * Beschriftung weglassen, wenn sie nicht in zwei Zeilen passt. `PLAN.md` 3.2.
     *
     * Von Haus aus aus. Bei vier Spalten schneidet „WhatsApp" ab, und ein abgeschnittenes
     * Wort ist auf drei Zoll schlimmer als gar keins - aber das ist eine Abwaegung, die
     * dem Nutzer gehoert: manche erkennen die Kachel lieber an drei Buchstaben als am Bild.
     */
    val hideCutLabels: Boolean = false,
    val gutterDp: Int = 4,
    /** Aussenrand in Prozent der Bildschirmbreite. */
    val safeBorderPercent: Int = 2,
    val cornerRadiusDp: Int = 12,
    val fullScreen: Boolean = false,
    /**
     * Alt: nur mit oder ohne Datum. Bleibt stehen, damit eine gesicherte Konfiguration aus
     * einer frueheren Fassung nicht stumm auf die Vorgabe zurueckfaellt. [Appearance.clock]
     * zaehlt.
     */
    val clockShowsDate: Boolean = true,
    val clockDisplay: ClockDisplay? = null,
    /** Groesse der Uhr in der Kopfzeile, 0,75-2,0 - zusaetzlich zur globalen Textgroesse. */
    val clockScale: Float = 1.0f,
    val orientation: ScreenOrientation = ScreenOrientation.PORTRAIT,
    /** Zeile ueber dem Raster mit Uhrzeit, Datum und Ladestand. */
    val showHeader: Boolean = true,
) {
    /** Die geltende Uhr-Stufe - aus der neuen Angabe, sonst aus dem alten Schalter. */
    val clock: ClockDisplay
        get() = clockDisplay
            ?: if (clockShowsDate) ClockDisplay.TIME_DATE_WEEKDAY else ClockDisplay.TIME

    /** Setzt beide Felder, damit alt und neu nie widersprechen. */
    fun withClock(display: ClockDisplay): Appearance = copy(
        clockDisplay = display,
        clockShowsDate = display == ClockDisplay.TIME_DATE ||
            display == ClockDisplay.TIME_DATE_WEEKDAY,
    )

    /** Die geltende Wahl - aus der neuen Angabe, sonst aus dem alten Schalter. */
    val icons: IconVisibility
        get() = iconVisibility ?: if (showIcons) IconVisibility.ALWAYS else IconVisibility.NEVER

    /** Setzt beide Felder, damit alt und neu nie widersprechen. */
    fun withIcons(choice: IconVisibility): Appearance = copy(
        iconVisibility = choice,
        showIcons = choice != IconVisibility.NEVER,
    )
}

@Serializable
data class Accessibility(
    /** Langdruck liest die Beschriftung vor. */
    val speakOnLongPress: Boolean = false,
    /** Langdruck zeigt die Beschriftung gross ueber dem ganzen Bildschirm. */
    val popupOnLongPress: Boolean = false,
    /** Zwei Knöpfe unter langen Listen statt Wischen - für unruhige Hände. */
    val scrollButtons: Boolean = false,
)

/**
 * Womit eine Kachel ausgelöst wird.
 *
 * Für zittrige Hände ist [LONG] die wichtigste Einstellung der ganzen App: ein
 * versehentliches Streifen startet dann nichts mehr. Der Preis ist, dass jeder Start eine
 * halbe Sekunde länger dauert - deshalb ist es eine Entscheidung und keine Vorgabe.
 */
@Serializable
enum class PressMode { SHORT, LONG }

/**
 * Wie deutlich sich eine Berührung meldet.
 *
 * [LIGHT] ist ein kurzer Stups, [STRONG] ein spürbarer Schlag. Wer dicke Finger, dicke
 * Handschuhe oder wenig Gefühl in den Händen hat, merkt den leichten Stups nicht - und
 * hält den Treffer dann für einen Fehlgriff. Beim langen Druck ist auch [LIGHT] deutlich:
 * dort meldet die Stärke nicht das Treffen, sondern dass gleich etwas anderes passiert.
 */
@Serializable
enum class HapticStrength { OFF, LIGHT, STRONG }

@Serializable
data class Behaviour(
    val pressMode: PressMode = PressMode.SHORT,
    /**
     * Alt: nur an oder aus. Bleibt stehen, damit eine Konfiguration aus einer früheren
     * Fassung nicht stumm auf die Vorgabe zurückfällt, und damit ein Export von hier in
     * einer früheren Fassung noch etwas bedeutet. [haptics] ist die Frage, die zählt.
     */
    val hapticFeedback: Boolean = true,
    val hapticStrength: HapticStrength? = null,
    /**
     * Meldungen bleiben stehen, bis sie weggetippt werden. Eine kurze Einblendung ist nach
     * zwei Sekunden weg - wer langsam liest, erfaehrt nur, dass etwas aufgeblitzt ist.
     */
    val confirmMessages: Boolean = false,
    val blinkOnNotification: Boolean = true,
    val swipeBetweenScreens: Boolean = false,
    val homeKeyReturnsToStart: Boolean = true,
    val accessibility: Accessibility = Accessibility(),
) {
    /** Die geltende Stärke - aus der neuen Angabe, sonst aus dem alten Schalter. */
    val haptics: HapticStrength
        get() = hapticStrength
            ?: if (hapticFeedback) HapticStrength.LIGHT else HapticStrength.OFF

    /** Setzt beide Felder, damit alt und neu nie widersprechen. */
    fun withHaptics(strength: HapticStrength): Behaviour = copy(
        hapticStrength = strength,
        hapticFeedback = strength != HapticStrength.OFF,
    )
}

@Serializable
data class Security(
    /** Salted Hash, null = keine PIN gesetzt. */
    val pin: String? = null,
    val pinProtectsEditor: Boolean = true,
    /** Die App-Liste ist der Weg zu jeder App, die auf keiner Kachel liegt. */
    val pinProtectsAppList: Boolean = false,
    /**
     * Die Anrufliste zu leeren ist nicht rueckgaengig zu machen. Steht eine PIN, wird sie
     * hier von selbst gefragt - wer eine PIN setzt, will genau solche Schritte sichern.
     */
    val pinProtectsCallLogDelete: Boolean = true,
)

@Serializable
data class SosConfig(
    val numbers: List<String> = emptyList(),
    /** Leer heisst: noch nicht gesetzt - dann gilt der Text in der Sprache des Telefons. */
    val message: String = "",
    /**
     * Der Vorgabewert steht in [SosCountdown], nicht hier.
     *
     * Bis zum 3.9.2026 stand die 5 an beiden Stellen, und `SosCountdown.DEFAULT_SECONDS`
     * hatte keinen einzigen Aufrufer: eine benannte Zahl, die niemand benutzt, neben
     * derselben Zahl ohne Namen. Wer die eine ändert, ändert die andere nicht mit.
     */
    val countdownSeconds: Int = SosCountdown.DEFAULT_SECONDS,
    val sendLocation: Boolean = true,
    /**
     * Lauter Alarmton während des Notrufs. `PLAN.md` 4.8.
     *
     * **Von Haus aus aus**: eine Sirene, die man nicht erwartet, ist der Grund, aus dem
     * Leute den Notrufknopf abschalten. Siehe [org.biglau.toggles.SosAlarm].
     */
    val alarmSound: Boolean = false,
    /** Blinkendes Licht während des Notrufs, aus demselben Grund von Haus aus aus. */
    val alarmFlash: Boolean = false,
)

@Serializable
data class AppsConfig(
    /** Schluessel oder Paketnamen, die in der Liste nicht erscheinen. */
    val hidden: Set<String> = emptySet(),
    /** Zuletzt gestartet, neueste zuerst. */
    val recent: List<String> = emptyList(),
    /** Wie viele davon oben in der Liste stehen; 0 blendet die Reihe aus. */
    val recentCount: Int = 4,
    /** Apps, die ohne PIN starten. Gilt nur, wenn [lockOthers] an und eine PIN gesetzt ist. */
    val allowed: Set<String> = emptySet(),
    /** Alles ausser [allowed] fragt nach der PIN. PLAN.md 4.5. */
    val lockOthers: Boolean = false,
)

@Serializable
data class SpeedDialTarget(val name: String, val number: String)

/** Nachrichten (`PLAN.md` 4.7). */
@Serializable
data class SmsConfig(
    /**
     * Nummern, deren Nachrichten nicht in der Liste stehen.
     *
     * Getrennt von der Anrufsperre und nicht mit ihr verschmolzen: wer eine Nummer nicht
     * mehr sprechen will, will ihre Nachrichten vielleicht trotzdem lesen - und umgekehrt.
     */
    val hiddenNumbers: List<String> = emptyList(),
    /** Woerter, die eine Nachricht aus der Liste nehmen. */
    val hiddenWords: List<String> = emptyList(),
    /**
     * Schriftgroesse **nur** im Gespraech. `PLAN.md` 4.7 nennt sie ausdruecklich getrennt
     * von der globalen, und das hat einen Grund: eine Nachricht liest man am Stueck und
     * aus der Hand, eine Kachel erkennt man im Vorbeigehen. Wer die Kacheln gross mag,
     * braucht deshalb nicht auch grosse Nachrichten - und umgekehrt.
     */
    val conversationScale: Float = 1.0f,
    /**
     * Wie lange es bei einer neuen Nachricht vibriert, in Millisekunden. `PLAN.md` 4.7.
     *
     * Steht im Benachrichtigungskanal und nicht in einem eigenen Vibrationsaufruf - nur so
     * hält sich die Meldung an „Bitte nicht stören". Siehe
     * [org.biglau.notify.SmsNotifications].
     */
    val vibrationMs: Int = 500,
    /**
     * Bei einer neuen Nachricht den ganzen Bildschirm nehmen. `PLAN.md` 4.7.
     *
     * **Von Haus aus aus**, und das ist eine Abwägung: eine Meldung, die alles übernimmt und
     * bei gesperrtem Bildschirm den Text zeigt, sieht auch jeder, der das Telefon in dem
     * Moment in der Hand hält. Wer sie will, schaltet sie ein - wer sie nicht kennt, wird
     * nicht überrascht.
     */
    val fullScreenAlert: Boolean = false,
    /**
     * Alle wie viele Minuten an ungelesene Nachrichten erinnert wird. Null heisst: gar
     * nicht. `PLAN.md` 4.7, siehe [org.biglau.notify.SmsReminder].
     */
    val repeatMinutes: Int = 0,
    /**
     * Nachfragen, bevor eine Nachricht hinausgeht. `PLAN.md` 4.7.
     *
     * **Von Haus aus aus**, und das ist eine Abwaegung: eine versehentlich gesendete halbe
     * Nachricht ist peinlich, eine zusaetzliche Frage vor *jeder* Nachricht ist eine
     * dauernde Muehe. Wer zittrige Haende hat, schaltet sie ein - dann steht sie da, wo sie
     * gebraucht wird, statt allen im Weg zu sein.
     */
    val confirmBeforeSending: Boolean = false,
    /** Sendeknopf ueber statt unter dem Textfeld. `PLAN.md` 4.7. */
    val sendButtonAbove: Boolean = false,
    /** Groesserer Sendeknopf - fuer Haende, die zittern. `PLAN.md` 4.7. */
    val sendButtonLarge: Boolean = false,
)

@Serializable
data class PhoneConfig(
    /** Taste (als Zeichenkette, damit JSON es mag) auf Ziel. */
    val speedDial: Map<String, SpeedDialTarget> = emptyMap(),
    /**
     * Anrufarten, die in der Liste nicht erscheinen - als Namen, damit die Datenschicht
     * nichts von der Telefonschicht wissen muss.
     *
     * Ausblendliste und keine Einblendliste: eine Art, die Android spaeter dazunimmt, ist
     * dann von selbst sichtbar statt still zu fehlen.
     */
    val hiddenCallTypes: Set<String> = emptySet(),
    /** Wie die Anrufliste zusammenfasst. `PLAN.md` 4.6. */
    val callGrouping: CallGrouping = CallGrouping.NUMBER,
    /** Wie gross das Foto des Anrufers auf dem Anrufbildschirm ist. `PLAN.md` 4.6. */
    val callerPhoto: CallerPhoto = CallerPhoto.SMALL,
    /** Wohin der Ton beim Verbinden geht. `PLAN.md` 4.6. */
    val audioRoute: AudioRoute = AudioRoute.EARPIECE,
    /** Lautsprecher bei selbst gewaehlten Anrufen. `PLAN.md` 4.6. */
    val speakerOnOutgoing: Boolean = false,
    /** Gesperrte Nummern, eingehend wie ausgehend. `PLAN.md` 4.6. */
    val blockedNumbers: List<String> = emptyList(),
    /**
     * Wann die Anrufliste zuletzt offen war, in Millisekunden seit 1970.
     *
     * **Damit ein Abzeichen kein Schreibrecht braucht.** Bisher zaehlte BigLau die vom
     * System als „neu" gefuehrten verpassten Anrufe und setzte dieses Kennzeichen beim
     * Oeffnen zurueck - das verlangt `WRITE_CALL_LOG`, und auf dem Telefon des Nutzers ist
     * es nicht erteilt. Die Zahl auf der Kachel waere dort nie erloschen, egal wie oft er
     * die Liste liest.
     *
     * Gezaehlt werden jetzt nur Anrufe, die **juenger** sind als dieser Zeitpunkt. Das
     * Kennzeichen des Systems bleibt zusaetzlich in der Bedingung: raeumt die
     * System-Telefon-App auf, verschwindet die Zahl hier ebenfalls.
     */
    val lastSeenMissedAt: Long = 0L,
)

/** Standard-Audioausgabe (`PLAN.md` 4.6). */
@Serializable
enum class AudioRoute { EARPIECE, SPEAKER, BLUETOOTH }

/**
 * Groesse des Kontaktfotos beim Anruf (`PLAN.md` 4.6).
 *
 * Vier Stufen und kein Schalter: wer schlecht sieht, will das Gesicht gross; wer das
 * Telefon in der Hosentasche hat, will vor allem den Namen lesen koennen, und ein
 * bildschirmfuellendes Foto draengt ihn nach unten.
 */
@Serializable
enum class CallerPhoto { OFF, SMALL, HALF, FULL }

/**
 * Wonach die Anrufliste zusammenfasst (`PLAN.md` 4.6).
 *
 * Bewusst als Aufzaehlung und nicht als Schalter: „nach nichts" ist kein Aus-Zustand von
 * „nach Nummer", sondern eine eigene Ansicht - wer wissen will, wann genau jemand dreimal
 * angerufen hat, braucht die drei Zeilen einzeln.
 */
@Serializable
enum class CallGrouping { NONE, NUMBER, DIRECTION }

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
    /**
     * Screens, die nicht in der Wischkette liegen. PLAN.md 4.1 „welche Screens per Wischen
     * erreichbar sind". Ausdruecklich als Ausnahmeliste und nicht als Mitgliederliste,
     * damit ein spaeter angelegter Screen von selbst dabei ist statt still zu fehlen.
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
    /** Ist der Erststart-Assistent durchlaufen? */
    val wizardDone: Boolean = false,
) {
    fun screenById(id: String): Screen? = screens.firstOrNull { it.id == id }

    val homeScreen: Screen get() = screenById(homeScreenId) ?: screens.first()
}
