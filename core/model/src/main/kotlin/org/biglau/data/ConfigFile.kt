package org.biglau.data

import kotlinx.serialization.json.Json
import java.io.File

/**
 * reads and writes the config file.
 *
 * written through a side file that is then renamed. without that, two writes triggered close
 * together overlapped: both start at zero, the shorter one ends first, and the tail of the
 * longer stays behind, leaving two documents written into each other.
 *
 * the file is also the backup format, hence pretty-printed.
 */
class ConfigFile(private val file: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /**
     * where an unreadable file is rescued to.
     *
     * set aside rather than overwritten: otherwise somebody's whole setup is gone with the
     * first write. from here it can be recovered with a text editor or a backup import.
     */
    val rescueFile: File get() = File(file.parentFile, "${file.name}.unreadable")

    /**
     * where the old setup waits during the write fallback.
     *
     * it exists for a fraction of one write. finding it at the next start with no setup
     * beside it means the process died exactly in between; see [read].
     */
    val asideFile: File get() = File(file.parentFile, "${file.name}.old")

    var rescuedBroken: Boolean = false
        private set

    /**
     * falls back to the defaults when there is nothing readable. crashing would be worse,
     * but the fallback used to look exactly like a fresh install: every tile gone, the
     * wizard running, and not a word about it. so the broken file is renamed first.
     */
    fun read(): LauncherConfig {
        rescuedBroken = false
        // an `.old` with no setup beside it means the process died mid-fallback in [write].
        // bringing it back beats greeting the user with the wizard.
        if (!file.exists() && asideFile.exists()) asideFile.renameTo(file)
        if (!file.exists()) return LauncherConfig()
        val parsed = runCatching { json.decodeFromString<LauncherConfig>(file.readText()) }
        parsed.getOrNull()?.let { return it }
        rescuedBroken = runCatching { file.renameTo(rescueFile) }.getOrDefault(false)
        return LauncherConfig()
    }

    /**
     * writes completely or not at all; half a document must never become visible.
     *
     * **the fallback was the dangerous part.** it used to delete the old file and try again,
     * and between deleting and renaming there is a moment with **no setup at all**: a process
     * dying there loses screens, tiles and folders. now the old file steps aside and comes
     * back if the second attempt also fails. the new setting is then lost, the old setup is
     * not, and of those two losses that is the smaller one.
     */
    fun write(config: LauncherConfig) {
        val text = json.encodeToString(LauncherConfig.serializer(), config)
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(text)
        if (temporary.renameTo(file)) return

        // some filesystems will not rename over an existing file.
        val aside = asideFile
        aside.delete()
        val saved = file.renameTo(aside)
        if (temporary.renameTo(file)) {
            aside.delete()
        } else {
            if (saved) aside.renameTo(file)
            temporary.delete()
        }
    }

    fun encode(config: LauncherConfig): String =
        json.encodeToString(LauncherConfig.serializer(), config)
}
