package org.biglau.tiles

import org.biglau.data.Builtin
import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.biglau.data.Cell
import org.biglau.data.LauncherConfig
import org.biglau.data.Screen

/**
 * Screens anlegen, umbenennen und loeschen.
 *
 * Zwei Dinge sind hier heikel und deshalb hier und nicht in der Oberflaeche geloest:
 * ein neuer Screen ohne Rueckweg waere eine Sackgasse, weil ein Launcher die
 * Zurueck-Geste nicht abfangen darf; und ein geloeschter Screen laesst Kacheln
 * zurueck, die ins Leere zeigen.
 */
object ScreenEdits {

    /**
     * Neuer Screen im selben Raster wie der Ausgangsscreen, mit einer Heim-Kachel
     * auf dem letzten Platz - sonst kommt der Nutzer dort nie wieder weg.
     */
    fun newScreen(id: String, name: String, like: Screen): Screen {
        val lastX = like.cols - 1
        val lastY = like.rows - 1
        return Screen(
            id = id,
            name = name,
            cols = like.cols,
            rows = like.rows,
            cells = listOf(
                Cell(
                    x = lastX,
                    y = lastY,
                    button = Button(action = ButtonAction.Action(Builtin.HOME_SCREEN)),
                ),
            ),
        )
    }

    /**
     * Raster, die auf drei Zoll aufgehen. Mehr als drei Spalten wird auf 349 dp Breite
     * zur Briefmarke - deshalb endet die Liste dort und nicht bei einer freien Eingabe,
     * mit der man sich den Startbildschirm unbrauchbar machen koennte.
     */
    val GRID_PRESETS: List<Pair<Int, Int>> = listOf(
        1 to 2,
        2 to 2,
        2 to 3,
        2 to 4,
        3 to 4,
        3 to 5,
    )

    /**
     * Welche Kacheln ein kleineres Raster nicht mehr fasst.
     *
     * Wird vor dem Umstellen gefragt und dem Nutzer gezeigt: ein Raster zu wechseln und
     * dabei stillschweigend vier Kacheln zu verlieren, waere derselbe Fehler wie eine
     * ausgeblendete App ohne Weg zurueck - nur unwiderruflich.
     */
    fun dropped(screen: Screen, cols: Int, rows: Int): List<Cell> =
        screen.cells.filter { it.x >= cols || it.y >= rows }

    /**
     * Neues Raster. Kacheln ausserhalb fallen weg, Kacheln die ueber den neuen Rand
     * ragen werden beschnitten statt hinauszuragen.
     */
    fun setGrid(config: LauncherConfig, id: String, cols: Int, rows: Int): LauncherConfig {
        if (cols < 1 || rows < 1) return config
        return config.copy(
            screens = config.screens.map { screen ->
                if (screen.id != id) {
                    screen
                } else {
                    // Dieselbe Rechnung stand hier ein zweites Mal, Zeile fuer Zeile wie
                    // in CellLayout.fitToGrid - nur dass die getestete Fassung niemand
                    // aufrief und diese hier lief. Zwei Kopien einer Regel heisst: eine
                    // wird irgendwann repariert und die andere nicht.
                    CellLayout.fitToGrid(screen.copy(cols = cols, rows = rows))
                }
            },
        )
    }

    /** Eine Kennung, die auf keinen bestehenden Screen faellt. */
    fun freeId(config: LauncherConfig, base: String = "screen"): String {
        var index = config.screens.size + 1
        while (config.screens.any { it.id == "$base$index" }) index++
        return "$base$index"
    }

    fun add(config: LauncherConfig, screen: Screen): LauncherConfig =
        if (config.screens.any { it.id == screen.id }) config
        else config.copy(screens = config.screens + screen)

    fun rename(config: LauncherConfig, id: String, name: String): LauncherConfig {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return config
        return config.copy(
            screens = config.screens.map { if (it.id == id) it.copy(name = trimmed) else it },
        )
    }

    /**
     * Was ein Loeschen kostet: belegte Kacheln des Screens und Ordner, die mit ihm gehen.
     *
     * Steht in der Rueckfrage. Seit das Loeschen auch die Ordner des Screens abraeumt, ist
     * die Zahl groesser als das, was man auf dem Screen sieht - der Inhalt eines Ordners
     * ist ja zugeklappt. Genau dann muss sie dastehen.
     */
    fun deletionLosses(config: LauncherConfig, id: String): Pair<Int, Int> {
        val screen = config.screens.firstOrNull { it.id == id } ?: return 0 to 0
        val kacheln = screen.cells.count { it.button.action != ButtonAction.None }
        val ordner = screen.cells
            .mapNotNull { (it.button.action as? ButtonAction.Folder)?.screenId }
            .distinct()
            .count { ordnerId ->
                // Nur die, auf die sonst nichts mehr zeigt.
                config.screens.flatMap { it.cells }.count { zelle ->
                    (zelle.button.action as? ButtonAction.Folder)?.screenId == ordnerId
                } == 1
            }
        return kacheln to ordner
    }

    /**
     * Loescht einen Screen und raeumt hinter ihm auf: Kacheln, die dorthin sprangen,
     * werden entfernt, und die Wischreihenfolge verliert den Eintrag. Der Startscreen
     * laesst sich nicht loeschen - sonst haette der Launcher kein Zuhause mehr.
     */
    fun delete(config: LauncherConfig, id: String): LauncherConfig {
        if (id == config.homeScreenId) return config
        if (config.screens.none { it.id == id }) return config
        if (config.screens.size <= 1) return config

        val remaining = config.screens
            .filter { it.id != id }
            .map { screen ->
                screen.copy(cells = screen.cells.filterNot { it.button.action == ButtonAction.GoToScreen(id) })
            }
        val ohneScreen = config.copy(
            screens = remaining,
            swipeOrder = config.swipeOrder.filterNot { it == id },
        )
        // Trug der geloeschte Screen eine Ordnerkachel, blieb der Ordner liegen - fuer
        // niemanden erreichbar, in keiner Liste sichtbar, nicht mehr zu loeschen. Er
        // gehoerte zum Inhalt dieses Screens und geht mit ihm.
        //
        // Nur diese: ein Ordner, der schon vorher verwaist war, hat mit diesem Loeschen
        // nichts zu tun und verschwindet nicht als Nebenwirkung.
        val ordnerVonHier = config.screens.first { it.id == id }.cells
            .mapNotNull { (it.button.action as? ButtonAction.Folder)?.screenId }
            .toSet()
        return FolderEdits.orphaned(ohneScreen)
            .filter { it.id in ordnerVonHier }
            .fold(ohneScreen) { stand, ordner -> FolderEdits.delete(stand, ordner.id) }
    }

    /**
     * Screens, zu denen keine einzige Kachel führt.
     *
     * Entstanden ist er meist so - angelegt, die Sprungkachel spaeter
     * mit etwas anderem belegt, und seither ist er nur noch in der Konfiguration. Der
     * Startbildschirm zaehlt nie dazu; zu ihm fuehrt immer die Zurueck-Geste.
     */
    fun unreachable(config: LauncherConfig): List<Screen> {
        val reached = config.screens
            .flatMap { it.cells }
            .mapNotNull { (it.button.action as? ButtonAction.GoToScreen)?.screenId }
            .toSet() +
            // Wischen zaehlt mit, wenn es eingeschaltet ist. Ohne das warnte die App vor
            // Screens, die man mit einer Handbewegung erreicht - und eine Warnung, die
            // nicht stimmt, nimmt man auch dort nicht mehr ernst, wo sie stimmt.
            if (config.behaviour.swipeBetweenScreens) {
                ScreenOrder.ordered(config).map { it.id }.toSet()
            } else {
                emptySet()
            }
        // Ordner sind hier nicht gemeint: zu ihnen fuehrt eine Ordnerkachel, keine
        // Sprungkachel, und ob eine fehlt, prueft FolderEdits.orphaned.
        return config.screens.filter {
            it.id != config.homeScreenId && it.id !in reached && !it.isFolder
        }
    }

    /**
     * Sind die Einstellungen von diesem Screen aus erreichbar?
     *
     * Die schlimmste Sackgasse der ganzen App: wer einen Screen zum Startbildschirm macht,
     * auf dem keine Einstellungen-Kachel liegt, kommt nie wieder in die Einstellungen -
     * und damit auch nie wieder zurück. Es hilft dann nur noch ein anderer Launcher oder
     * ein Rechner mit adb. Genau das ist beim Ausprobieren am 01.09.2026 passiert.
     *
     * Gezählt wird über Sprung- und Ordnerkacheln hinweg, denn ein Weg über zwei Ecken ist
     * auch ein Weg. Die App-Liste zählt **nicht**: BigLau steht zwar darin, aber ein Start
     * von dort führt auf den Startbildschirm und nicht in die Einstellungen.
     */
    fun settingsReachable(config: LauncherConfig, fromScreenId: String): Boolean {
        val besucht = mutableSetOf<String>()
        val offen = ArrayDeque(listOf(fromScreenId))
        while (offen.isNotEmpty()) {
            val id = offen.removeFirst()
            if (!besucht.add(id)) continue
            val screen = config.screens.firstOrNull { it.id == id } ?: continue
            screen.cells.forEach { zelle ->
                when (val aktion = zelle.button.action) {
                    is ButtonAction.Action ->
                        if (aktion.builtin == Builtin.SETTINGS) return true
                    is ButtonAction.GoToScreen -> offen.addLast(aktion.screenId)
                    is ButtonAction.Folder -> offen.addLast(aktion.screenId)
                    else -> Unit
                }
            }
        }
        return false
    }

    /**
     * Legt eine Einstellungen-Kachel auf den ersten freien Platz - der Ausweg aus der
     * Sackgasse oben. Gibt `null` zurueck, wenn kein Platz frei ist; dann darf der Wechsel
     * nicht stattfinden, denn danach gaebe es keinen Weg mehr zurueck.
     */
    fun withSettingsTile(config: LauncherConfig, screenId: String): LauncherConfig? {
        val screen = config.screens.firstOrNull { it.id == screenId } ?: return null
        val platz = screen.freeSlots().firstOrNull() ?: return null
        return config.copy(
            screens = config.screens.map {
                if (it.id != screenId) {
                    it
                } else {
                    it.copy(
                        cells = it.cells + Cell(
                            x = platz.first,
                            y = platz.second,
                            button = Button(action = ButtonAction.Action(Builtin.SETTINGS)),
                        ),
                    )
                }
            },
        )
    }
}
