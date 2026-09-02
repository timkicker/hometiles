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
        if (!file.exists()) return LauncherConfig()
        val gelesen = runCatching { json.decodeFromString<LauncherConfig>(file.readText()) }
        gelesen.getOrNull()?.let { return it }
        rescuedBroken = runCatching { file.renameTo(rescueFile) }.getOrDefault(false)
        return LauncherConfig()
    }

    /** Schreibt vollstaendig oder gar nicht - ein halbes Dokument darf nie sichtbar werden. */
    fun write(config: LauncherConfig) {
        val text = json.encodeToString(LauncherConfig.serializer(), config)
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(text)
        if (!temporary.renameTo(file)) {
            // Manche Dateisysteme benennen nicht ueber eine bestehende Datei hinweg um.
            file.delete()
            if (!temporary.renameTo(file)) {
                file.writeText(text)
                temporary.delete()
            }
        }
    }

    fun encode(config: LauncherConfig): String =
        json.encodeToString(LauncherConfig.serializer(), config)
}
