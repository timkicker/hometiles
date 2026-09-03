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

    /**
     * Nach dem Schreiben liegt nur die Einrichtung da - kein `.tmp`, kein `.old`.
     *
     * Die beiden Hilfsdateien sind der Preis dafuer, dass nie ein halbes Dokument sichtbar
     * wird. Bleiben sie liegen, sammelt sich im Verzeichnis Zeug an, das beim naechsten
     * Blick niemand mehr einordnen kann - und `.old` sieht aus wie eine Sicherung, ist aber
     * eine Leiche.
     */
    @Test
    fun `nach dem Schreiben bleibt keine Hilfsdatei liegen`() {
        val ziel = File(folder.root, "config.json")
        val datei = ConfigFile(ziel)
        datei.write(LauncherConfig())
        datei.write(LauncherConfig(wizardDone = true))
        val uebrig = folder.root.list()?.sorted().orEmpty()
        assertEquals(listOf("config.json"), uebrig)
    }

    /**
     * Und die alte Einrichtung ueberlebt, wenn das Schreiben mittendrin nicht weitergeht.
     *
     * Herstellen laesst sich das hier nur zur Haelfte - ein fehlschlagendes `renameTo`
     * gibt es im Test nicht. Geprueft wird deshalb das, was daraus folgen muss: nach einem
     * Schreibvorgang steht immer eine **lesbare** Einrichtung da, nie eine leere Stelle.
     */
    @Test
    fun `nach dem Schreiben steht immer eine lesbare Einrichtung da`() {
        val ziel = File(folder.root, "config.json")
        val datei = ConfigFile(ziel)
        datei.write(LauncherConfig(wizardDone = true))
        assertEquals(true, ziel.exists())
        assertEquals(true, datei.read().wizardDone)
    }

    /**
     * Liegt die alte Einrichtung noch daneben und die richtige fehlt, wird sie zurückgeholt.
     *
     * Das ist der Zustand nach einem Absturz mitten im Rückfall des Schreibens: `.old`
     * existiert, `config.json` nicht. Ohne diesen Griff stünde der Nutzer vor dem
     * Assistenten und hielte sein Telefon für zurückgesetzt — **während seine Einrichtung
     * einen Dateinamen weiter liegt.**
     */
    @Test
    fun `eine beiseite gelegte Einrichtung wird zurueckgeholt`() {
        val ziel = File(folder.root, "config.json")
        val datei = ConfigFile(ziel)
        datei.write(LauncherConfig(wizardDone = true))
        // Den Absturz nachstellen: die Einrichtung liegt nur noch als `.old` da.
        assertEquals(true, ziel.renameTo(datei.asideFile))
        assertEquals(false, ziel.exists())

        val gelesen = datei.read()

        assertEquals(true, gelesen.wizardDone)
        assertEquals("die Einrichtung steht nicht wieder an ihrem Platz", true, ziel.exists())
        assertEquals("die Ausweichdatei bleibt liegen", false, datei.asideFile.exists())
    }

    /** Ohne `.old` bleibt es beim Rückfall auf die Vorgabe - nichts wird erfunden. */
    @Test
    fun `ohne Datei und ohne Ausweichdatei kommt die Vorgabe`() {
        val datei = ConfigFile(File(folder.root, "config.json"))
        assertEquals(LauncherConfig(), datei.read())
    }
}
