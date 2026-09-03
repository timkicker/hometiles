package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kein Verweis auf eine Datei, die es nicht gibt.
 *
 * `PLAN.md` nannte am 3.9.2026 zwei: `docs/sms-role.md` („Manifest-Checkliste") und
 * `docs/release-checklist.md` („Manuelle Checkliste pro Release"). Ein `docs/`-Verzeichnis
 * gab es nie. Wer die Checkliste sucht, sucht sie eine Weile und hält dann sich selbst für
 * blind — ein toter Verweis ist schlimmer als keiner, weil er Arbeit verspricht, die jemand
 * geleistet haben soll.
 *
 * Geprüft werden nur Pfade **mit Verzeichnis** auf `.md`, `.sh` oder `.py`: Dokumente und
 * Werkzeuge. Bei einem `strings.xml` ohne Pfad wäre nicht klar, welches gemeint ist.
 *
 * **`STATUS.md` ist ausgenommen, und zwar mit Grund.** Es ist ein Protokoll: es berichtet
 * über umgezogene Dateien, gelöschte Werkzeuge und — wie am 3.9.2026 — über genau diese
 * beiden toten Verweise. Ein Protokoll, das nur Pfade nennen darf, die es heute noch gibt,
 * müsste seine eigene Geschichte umschreiben. Die Regel fiel bei ihrem ersten Lauf genau
 * daran, und das war ihr Fehler, nicht der des Eintrags.
 */
class VerweiseTest {

    /** Pfad → warum er trotzdem dastehen darf. */
    private val erlaubt = mapOf<String, String>()

    // Ohne Backticks: in `tools/README.md` stehen die Werkzeuge in Befehlszeilen
    // (```sh …), nicht in Backticks. Die erste Fassung fand dort nichts und meldete
    // „nur zwei Pfade gefunden" - richtig, aber am Zweck vorbei.
    private val muster = Regex("""([A-Za-z0-9_./-]+/[A-Za-z0-9_.-]+\.(?:md|sh|py))""")

    @Test
    fun `jeder genannte Pfad existiert`() {
        val tot = listOf("PLAN.md", "README.md", "tools/README.md")
            .flatMap { name ->
                val text = File("../$name").readText()
                muster.findAll(text).map { it.groupValues[1] }.map { name to it }
            }
            .filterNot { (_, pfad) -> pfad in erlaubt }
            .filterNot { (_, pfad) -> File("../$pfad").exists() }
            .map { (wo, pfad) -> "$wo nennt $pfad" }
            .distinct()
            .sorted()

        assertEquals(
            "Ein Verweis auf eine Datei, die es nicht gibt. Entweder die Datei anlegen, " +
                "oder den Verweis durch das ersetzen, was es wirklich gibt.",
            emptyList<String>(),
            tot,
        )
    }

    @Test
    fun `die Regel findet ueberhaupt Pfade`() {
        val zahl = listOf("PLAN.md", "README.md", "tools/README.md")
            .sumOf { muster.findAll(File("../$it").readText()).count() }
        assertTrue("nur $zahl Pfade gefunden - sucht die Regel noch?", zahl >= 5)
    }

    /**
     * Und `QUERY_ALL_PACKAGES` bleibt eine Erwähnung, keine Berechtigung.
     *
     * `PLAN.md` 6 sagt ausdrücklich „**kein** `QUERY_ALL_PACKAGES`": ein Launcher kommt mit
     * `<queries>` und `LauncherApps` aus, und diese Berechtigung ist die, wegen der Apps aus
     * dem Play Store fliegen. Im Manifest steht sie heute nur in einem Kommentar, der
     * erklärt, warum es sie nicht braucht — ein `grep` allein würde das nicht unterscheiden.
     */
    @Test
    fun `QUERY_ALL_PACKAGES steht nur im Kommentar`() {
        val manifest = File("src/main/AndroidManifest.xml").readLines()
        val echt = manifest.withIndex()
            .filter { (_, zeile) -> "QUERY_ALL_PACKAGES" in zeile }
            .filter { (_, zeile) -> "uses-permission" in zeile }
            .map { (i, _) -> "AndroidManifest.xml:${i + 1}" }
        assertEquals(
            "PLAN.md 6: kein QUERY_ALL_PACKAGES. Ein Launcher kommt mit <queries> und " +
                "LauncherApps aus.",
            emptyList<String>(),
            echt,
        )
        assertTrue(
            "Der Kommentar, der erklärt warum es die Berechtigung nicht braucht, ist weg - " +
                "dann fragt der nächste Leser sich, ob es Absicht war.",
            manifest.any { "QUERY_ALL_PACKAGES" in it },
        )
    }
}
