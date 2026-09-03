package org.biglau.data

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Liest und schreibt die Konfigurationsdatei.
 *
 * Geschrieben wird ueber eine Nebendatei, die anschliessend umbenannt wird. Ohne das
 * ueberlappten sich zwei kurz hintereinander ausgeloeste Schreibvorgaenge: beide beginnen
 * bei Position null, der kuerzere endet frueher, und der Rest des laengeren bleibt stehen -
 * die Datei enthielt danach zwei ineinander geschriebene Dokumente und war unlesbar.
 *
 * Die Datei ist zugleich das Sicherungsformat, deshalb lesbar formatiert.
 */
class ConfigFile(private val file: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /**
     * Wohin eine unlesbare Datei gerettet wird.
     *
     * Nicht ueberschreiben, sondern zur Seite legen: sonst ist die Einrichtung eines
     * Menschen mit dem ersten Schreibvorgang endgueltig weg. Von hier laesst sie sich mit
     * einem Texteditor oder ueber das Einlesen einer Sicherung wiederholen.
     */
    val rescueFile: File get() = File(file.parentFile, "${file.name}.unreadable")

    /**
     * Wohin die alte Einrichtung waehrend des Rueckfalls beim Schreiben ausweicht.
     *
     * Sie existiert nur fuer den Bruchteil eines Schreibvorgangs. Liegt sie beim naechsten
     * Start trotzdem da und die Einrichtung fehlt, ist der Prozess genau dazwischen
     * gestorben - siehe [read].
     */
    val asideFile: File get() = File(file.parentFile, "${file.name}.old")

    /** Musste beim letzten [read] eine unlesbare Datei zur Seite gelegt werden? */
    var rescuedBroken: Boolean = false
        private set

    /**
     * Gibt die Vorgabe zurueck, wenn nichts da oder nichts lesbar ist.
     *
     * Der Rueckfall selbst ist Absicht - abzustuerzen waere schlimmer. Aber er sah bis
     * hierher aus wie ein frisch installiertes BigLau: alle Kacheln weg, der Assistent
     * laeuft wieder, und kein Wort dazu. Beim naechsten Schreiben war die alte Datei
     * ueberschrieben und die Einrichtung endgueltig verloren. Deshalb wird sie jetzt
     * vorher [rescueFile] genannt.
     */
    fun read(): LauncherConfig {
        rescuedBroken = false
        // Ein `.old` ohne Einrichtung heisst: der Prozess ist mitten im Rueckfall des
        // Schreibens gestorben (siehe [write]). Die alte Einrichtung liegt dann noch da.
        // Sie zurueckzuholen ist besser, als den Nutzer mit dem Assistenten zu begruessen
        // und ihn glauben zu lassen, sein Telefon sei zurueckgesetzt.
        if (!file.exists() && asideFile.exists()) asideFile.renameTo(file)
        if (!file.exists()) return LauncherConfig()
        val gelesen = runCatching { json.decodeFromString<LauncherConfig>(file.readText()) }
        gelesen.getOrNull()?.let { return it }
        rescuedBroken = runCatching { file.renameTo(rescueFile) }.getOrDefault(false)
        return LauncherConfig()
    }

    /**
     * Schreibt vollstaendig oder gar nicht - ein halbes Dokument darf nie sichtbar werden.
     *
     * **Der Rueckfall war der gefaehrliche Teil.** Hier stand vorher: laesst sich die neue
     * Datei nicht ueber die alte legen, dann `file.delete()` und noch einmal versuchen.
     * Zwischen dem Loeschen und dem Umbenennen gibt es einen Augenblick, in dem **gar
     * keine Einrichtung** existiert - stirbt der Prozess genau dort, ist alles weg: Screens,
     * Kacheln, Ordner. Wie sich das anfuehlt, steht in `STATUS.md` vom 03.09.2026, als eine
     * unlesbare Datei den Assistenten aufgehen liess.
     *
     * Jetzt geht die alte Datei erst **zur Seite** und kommt zurueck, wenn der zweite
     * Versuch auch scheitert. Die neue Einstellung ist dann verloren - die alte
     * Einrichtung nicht. Von den beiden Verlusten ist das der kleinere.
     */
    fun write(config: LauncherConfig) {
        val text = json.encodeToString(LauncherConfig.serializer(), config)
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(text)
        if (temporary.renameTo(file)) return

        // Manche Dateisysteme benennen nicht ueber eine bestehende Datei hinweg um.
        val beiseite = asideFile
        beiseite.delete()
        val gerettet = file.renameTo(beiseite)
        if (temporary.renameTo(file)) {
            beiseite.delete()
        } else {
            if (gerettet) beiseite.renameTo(file)
            temporary.delete()
        }
    }

    fun encode(config: LauncherConfig): String =
        json.encodeToString(LauncherConfig.serializer(), config)
}
