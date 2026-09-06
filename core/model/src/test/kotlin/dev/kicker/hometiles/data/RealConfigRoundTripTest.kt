package dev.kicker.hometiles.data

import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * does this version read a real, grown configuration without loss?
 *
 * the file deliberately does **not** lie in the repository: it holds a person's app list and
 * screen names. without it the test skips itself - and that it did from the day it was
 * written until 3.9.2026, without standing out anywhere.
 *
 * it stays, because it sees more on the real file than any copy; the always-running copy
 * beside it is [GrownConfigTest]. to run this one:
 *
 *     HOMETILES_REAL_CONFIG=/path/config.json ./gradlew :core:model:test
 */
class RealConfigRoundTripTest {

    @Test
    fun `a real configuration survives the move`() {
        val path = System.getenv("HOMETILES_REAL_CONFIG")
        assumeTrue("HOMETILES_REAL_CONFIG not set", path != null)
        val file = File(path!!)
        assumeTrue("file not there: $path", file.exists())

        val text = file.readText()
        val loaded = ConfigTransfer.import(text)
        assertEquals(true, loaded != null)
        loaded!!

        // counter-check straight from the json, without the import way.
        val original = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString(LauncherConfig.serializer(), text)
        assertEquals(original.screens.size, loaded.screens.size)
        assertEquals(original.screens.map { it.name }, loaded.screens.map { it.name })
        assertEquals(
            original.screens.sumOf { it.cells.size },
            loaded.screens.sumOf { it.cells.size },
        )
        assertEquals(original.appearance, loaded.appearance)
        assertEquals(original.behaviour, loaded.behaviour)
        assertEquals(original.homeScreenId, loaded.homeScreenId)
    }
}
