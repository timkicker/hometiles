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

    /** Gibt die Vorgabe zurueck, wenn nichts da oder nichts lesbar ist. */
    fun read(): LauncherConfig = runCatching {
        if (!file.exists()) return@runCatching LauncherConfig()
        json.decodeFromString<LauncherConfig>(file.readText())
    }.getOrElse { LauncherConfig() }

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
