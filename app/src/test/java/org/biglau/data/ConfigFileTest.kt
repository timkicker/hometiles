package org.biglau.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ConfigFileTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun store(): Pair<ConfigFile, File> {
        val file = File(folder.root, "config.json")
        return ConfigFile(file) to file
    }

    private fun configWith(screens: Int) = LauncherConfig(
        screens = (1..screens).map { Screen(id = "s$it", name = "Screen $it") },
        homeScreenId = "s1",
    )

    @Test
    fun `eine fehlende Datei ergibt die Vorgabe`() {
        val (config, _) = store()
        assertEquals(LauncherConfig(), config.read())
    }

    @Test
    fun `Geschriebenes kommt unveraendert zurueck`() {
        val (config, _) = store()
        val original = configWith(3)
        config.write(original)
        assertEquals(original, config.read())
    }

    @Test
    fun `eine zerstoerte Datei faellt auf die Vorgabe zurueck statt abzustuerzen`() {
        val (config, file) = store()
        file.writeText("{ das ist kein JSON")
        assertEquals(LauncherConfig(), config.read())
    }

    @Test
    fun `eine unlesbare Datei wird zur Seite gelegt statt ueberschrieben`() {
        // Sonst ist die Einrichtung eines Menschen mit dem naechsten Schreibvorgang
        // endgueltig weg - und vorher sah es aus wie ein frisch installiertes BigLau.
        val (config, file) = store()
        val inhalt = "{ das ist kein JSON"
        file.writeText(inhalt)
        assertEquals(LauncherConfig(), config.read())
        assertTrue("die kaputte Datei muss gerettet sein", config.rescueFile.exists())
        assertEquals(inhalt, config.rescueFile.readText())
        assertTrue(config.rescuedBroken)
    }

    @Test
    fun `nach der Rettung ueberschreibt das Schreiben nur die neue Datei`() {
        val (config, file) = store()
        file.writeText("{ kaputt")
        config.read()
        config.write(LauncherConfig())
        assertTrue("die neue Datei steht", file.exists())
        assertEquals("{ kaputt", config.rescueFile.readText())
    }

    @Test
    fun `eine lesbare Datei wird nicht angefasst`() {
        val (config, file) = store()
        config.write(LauncherConfig())
        config.read()
        assertTrue(file.exists())
        assertTrue("nichts zu retten", !config.rescueFile.exists())
        assertTrue(!config.rescuedBroken)
    }

    @Test
    fun `eine fehlende Datei ist kein Schaden`() {
        val (config, _) = store()
        assertEquals(LauncherConfig(), config.read())
        assertTrue(!config.rescuedBroken)
        assertTrue(!config.rescueFile.exists())
    }

    @Test
    fun `ein kuerzeres Dokument laesst keinen Rest des laengeren stehen`() {
        // Genau der Fehler vom Geraet: die Datei endete auf "}}", weil ein kurzer
        // Schreibvorgang einen langen ueberschrieb, ohne ihn abzuschneiden.
        val (config, file) = store()
        config.write(configWith(8))
        val long = file.length()
        config.write(configWith(1))
        assertTrue("Datei ist nicht geschrumpft", file.length() < long)
        assertEquals(1, config.read().screens.size)
        assertTrue("Datei endet mit Resten", file.readText().trimEnd().endsWith("}"))
    }

    @Test
    fun `gleichzeitige Schreibvorgaenge hinterlassen immer ein lesbares Dokument`() {
        val (config, file) = store()
        val threads = 8
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)

        repeat(threads) { index ->
            pool.submit {
                start.await()
                repeat(20) { config.write(configWith(index + 1)) }
                done.countDown()
            }
        }
        start.countDown()
        assertTrue(done.await(30, TimeUnit.SECONDS))
        pool.shutdown()

        // Egal welcher Schreibvorgang zuletzt gewann - die Datei muss lesbar sein.
        val text = file.readText()
        assertTrue("Datei ist leer", text.isNotBlank())
        val loaded = config.read()
        assertTrue("Datei liess sich nicht lesen", loaded.screens.isNotEmpty())
        assertEquals(1, text.count { it == '{' } - text.count { it == '}' } + 1)
    }

    @Test
    fun `nach dem Schreiben bleibt keine Nebendatei liegen`() {
        val (config, file) = store()
        config.write(configWith(2))
        assertTrue(
            "Nebendatei liegt noch da",
            folder.root.listFiles().orEmpty().none { it.name.endsWith(".tmp") },
        )
        assertTrue(file.exists())
    }
}
