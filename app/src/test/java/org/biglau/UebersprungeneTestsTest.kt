package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Welche Tests dürfen sich selbst überspringen?
 *
 * `Assume.assumeTrue` macht einen Test grün, ohne dass er läuft, und Gradle sagt dazu
 * nichts, was in der Zusammenfassung auffiele. Am 3.9.2026 fiel auf, dass
 * `RealConfigRoundTripTest` seit seiner Entstehung nie gelaufen war: die Datei, die er
 * braucht, liegt aus gutem Grund nicht im Repository. Der Umzug einer gewachsenen
 * Konfiguration - der Grund, aus dem es die Sicherung gibt - war also ungeprüft.
 *
 * Der Test bleibt, weil er an der echten Datei mehr sieht als jede Abschrift. Aber die
 * Liste derer, die sich überspringen dürfen, steht hier, und jeder Eintrag braucht einen
 * Grund. Was neu dazukommt, fällt hier auf, statt lautlos grün zu sein.
 */
class UebersprungeneTestsTest {

    /** Datei → warum dieser Test sich überspringen darf. */
    private val erlaubt = mapOf(
        "RealConfigRoundTripTest.kt" to
            "braucht die echte Konfiguration eines Telefons; die darf nicht ins " +
            "Repository. Die immer laufende Abschrift daneben: GewachseneFassungTest.",
    )

    @Test
    fun `nur benannte tests duerfen sich selbst ueberspringen`() {
        val ueberspringend = Quelltext.testFiles()
            // Die Regel selbst schreibt den gesuchten Namen hin und faende sonst sich.
            .filter { it.name != "UebersprungeneTestsTest.kt" }
            .filter { it.readText().contains("assumeTrue") }
            .map { it.name }
            .sorted()

        assertEquals(
            "Ein Test, der sich selbst überspringt, schützt nichts. " +
                "Entweder er läuft immer, oder er kommt mit Begründung in die Liste in " +
                "UebersprungeneTestsTest.",
            erlaubt.keys.sorted(),
            ueberspringend,
        )
    }

    @Test
    fun `zu jedem uebersprungenen test gibt es eine immer laufende abschrift`() {
        val namen = Quelltext.testFiles().map { it.name }.toSet()
        assertEquals(true, "GewachseneFassungTest.kt" in namen)
    }
}
