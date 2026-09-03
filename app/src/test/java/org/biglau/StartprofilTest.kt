package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Das mitgelieferte Startprofil ist da, und es beschreibt den Start.
 *
 * Frisch installiert übersetzt Android nichts: die App steht auf `run-from-apk` und
 * dolmetscht beim ersten Start jede Zeile. Am 3.9.2026 am Jelly 2 gemessen — je fünf
 * Kaltstarts mit `am start -W`, Median: **2275 ms** so, **670 ms** nach einer vollen
 * Übersetzung. Das ist der Abstand, um den es hier geht.
 *
 * Zwei Dinge können daran lautlos kaputtgehen: die Datei landet am falschen Platz (AGP 8
 * liest `src/main/baselineProfiles/`, nicht mehr `src/main/baseline-prof.txt` — genau darauf
 * bin ich zuerst hereingefallen), oder `profileinstaller` fehlt, und dann liegt das Profil
 * zwar im Archiv, wird auf Android 9 bis 11 aber nie eingespielt.
 */
class StartprofilTest {

    private val profile: List<File> = File("src/main/baselineProfiles")
        .listFiles { d -> d.extension == "txt" }?.toList().orEmpty()

    private val regeln: List<String> = profile
        .flatMap { it.readLines() }
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }

    @Test
    fun `es gibt ein startprofil an dem platz den AGP 8 liest`() {
        assertTrue(
            "kein Startprofil in src/main/baselineProfiles/ — AGP 8 liest nur dort, " +
                "src/main/baseline-prof.txt wird stillschweigend ignoriert",
            profile.isNotEmpty(),
        )
        assertTrue("Startprofil ohne Regeln", regeln.size >= 10)
    }

    @Test
    fun `der einstieg steht im profil`() {
        listOf("MainActivity", "BigLauApp").forEach { klasse ->
            assertTrue(
                "$klasse fehlt im Startprofil — genau das ist der Start",
                regeln.any { klasse in it },
            )
        }
    }

    @Test
    fun `das profil beschreibt nur eigenen quelltext`() {
        val fremd = regeln.filterNot { it.contains("org/biglau/") }
        assertEquals(
            "Fremde Pakete im eigenen Startprofil. Die Bibliotheken bringen ihre Profile " +
                "selbst mit (Compose tut es); doppelt gepflegt wird daraus eine Liste, " +
                "die niemand nachzieht.",
            emptyList<String>(),
            fremd,
        )
    }

    @Test
    fun `profileinstaller ist eingebunden`() {
        val bau = File("build.gradle.kts").readText()
        assertTrue(
            "Ohne androidx.profileinstaller liegt das Profil im Archiv und wird auf " +
                "Android 9 bis 11 nie eingespielt — auf dem Zielgerät also nie.",
            "profileinstaller" in bau,
        )
    }
}
