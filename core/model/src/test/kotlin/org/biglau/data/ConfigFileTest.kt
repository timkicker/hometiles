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
    fun `a missing file gives the default`() {
        val (config, _) = store()
        assertEquals(LauncherConfig(), config.read())
    }

    @Test
    fun `what was written comes back unchanged`() {
        val (config, _) = store()
        val original = configWith(3)
        config.write(original)
        assertEquals(original, config.read())
    }

    @Test
    fun `a broken file falls back to the default instead of crashing`() {
        val (config, file) = store()
        file.writeText("{ das ist kein JSON")
        assertEquals(LauncherConfig(), config.read())
    }

    @Test
    fun `an unreadable file is set aside instead of overwritten`() {
        // otherwise a person's whole setup is gone for good with the next write - and before
        // that it looked like a freshly installed BigLau.
        val (config, file) = store()
        val content = "{ das ist kein JSON"
        file.writeText(content)
        assertEquals(LauncherConfig(), config.read())
        assertTrue("the broken file has to be rescued", config.rescueFile.exists())
        assertEquals(content, config.rescueFile.readText())
        assertTrue(config.rescuedBroken)
    }

    @Test
    fun `after the rescue writing only overwrites the new file`() {
        val (config, file) = store()
        file.writeText("{ kaputt")
        config.read()
        config.write(LauncherConfig())
        assertTrue("the new file stands", file.exists())
        assertEquals("{ kaputt", config.rescueFile.readText())
    }

    @Test
    fun `a readable file is not touched`() {
        val (config, file) = store()
        config.write(LauncherConfig())
        config.read()
        assertTrue(file.exists())
        assertTrue("nothing to rescue", !config.rescueFile.exists())
        assertTrue(!config.rescuedBroken)
    }

    @Test
    fun `a missing file is no damage`() {
        val (config, _) = store()
        assertEquals(LauncherConfig(), config.read())
        assertTrue(!config.rescuedBroken)
        assertTrue(!config.rescueFile.exists())
    }

    @Test
    fun `a shorter document leaves no remains of the longer one`() {
        // the fault from the device: the file ended on "}}" because a short write overwrote a
        // long one without truncating it.
        val (config, file) = store()
        config.write(configWith(8))
        val long = file.length()
        config.write(configWith(1))
        assertTrue("the file did not shrink", file.length() < long)
        assertEquals(1, config.read().screens.size)
        assertTrue("the file ends with remains", file.readText().trimEnd().endsWith("}"))
    }

    @Test
    fun `simultaneous writes always leave a readable document`() {
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

        // whichever write won last - the file has to be readable.
        val text = file.readText()
        assertTrue("the file is empty", text.isNotBlank())
        val loaded = config.read()
        assertTrue("the file could not be read", loaded.screens.isNotEmpty())
        assertEquals(1, text.count { it == '{' } - text.count { it == '}' } + 1)
    }

    @Test
    fun `after writing no side file is left`() {
        val (config, file) = store()
        config.write(configWith(2))
        assertTrue(
            "a side file is still there",
            folder.root.listFiles().orEmpty().none { it.name.endsWith(".tmp") },
        )
        assertTrue(file.exists())
    }

    /**
     * after writing only the setup lies there - no `.tmp`, no `.old`.
     *
     * the two helper files are the price for never showing half a document. left lying they
     * pile up as things nobody can place at the next look - and `.old` looks like a backup
     * while being a corpse.
     */
    @Test
    fun `after writing no helper file is left`() {
        val target = File(folder.root, "config.json")
        val file = ConfigFile(target)
        file.write(LauncherConfig())
        file.write(LauncherConfig(wizardDone = true))
        val left = folder.root.list()?.sorted().orEmpty()
        assertEquals(listOf("config.json"), left)
    }

    /**
     * and the old setup survives when writing stops halfway. only half of that can be
     * produced here - a failing `renameTo` does not exist in a test. so what must follow is
     * checked: after a write a **readable** setup always stands there, never an empty place.
     */
    @Test
    fun `after writing a readable setup always stands there`() {
        val target = File(folder.root, "config.json")
        val file = ConfigFile(target)
        file.write(LauncherConfig(wizardDone = true))
        assertEquals(true, target.exists())
        assertEquals(true, file.read().wizardDone)
    }

    /**
     * if the old setup still lies beside it and the real one is missing, it is fetched back.
     *
     * that is the state after a crash in the middle of the write's fallback: `.old` exists,
     * `config.json` does not. without this the user would stand in front of the wizard and
     * think the phone reset - **while their setup lies one file name away.**
     */
    @Test
    fun `a setup put aside is fetched back`() {
        val target = File(folder.root, "config.json")
        val file = ConfigFile(target)
        file.write(LauncherConfig(wizardDone = true))
        // reproduce the crash: the setup lies there only as `.old`.
        assertEquals(true, target.renameTo(file.asideFile))
        assertEquals(false, target.exists())

        val read = file.read()

        assertEquals(true, read.wizardDone)
        assertEquals("the setup does not stand in its place again", true, target.exists())
        assertEquals("the aside file is still lying there", false, file.asideFile.exists())
    }

    /** without `.old` it stays at the fallback to the default - nothing is invented. */
    @Test
    fun `without a file and without an aside file the default comes`() {
        val file = ConfigFile(File(folder.root, "config.json"))
        assertEquals(LauncherConfig(), file.read())
    }
}
