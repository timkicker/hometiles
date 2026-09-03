package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Prüfsumme im README nennt den Commit, zu dem sie gehört.
 *
 * Eine Prüfsumme ohne Commit ist keine Zusicherung, sondern eine Zahl: sie gilt für einen
 * Stand des Quelltexts, und schon der nächste Commit macht sie unprüfbar, ohne dass sie
 * falsch *aussähe*. Bis zum 3.9.2026 stand dort nur ein Datum — und die Zahl war da bereits
 * seit mehreren Commits überholt.
 *
 * Diese Regel prüft nicht die Zahl selbst (das kostet zwei volle Neubauten,
 * `tools/nachbauen.sh`), sondern dass sie nachrechenbar **angeschrieben** ist.
 */
class NachbauZahlTest {

    private val readme = File("../README.md").readText()

    @Test
    fun `zur pruefsumme steht ein commit dabei`() {
        val summe = Regex("""\b[0-9a-f]{64}\b""").find(readme)
        assertTrue("keine SHA-256-Prüfsumme im README gefunden", summe != null)

        val umfeld = readme.substring(
            (summe!!.range.first - 400).coerceAtLeast(0),
            (summe.range.last + 400).coerceAtMost(readme.length),
        )
        val commit = Regex("""Commit\s+`([0-9a-f]{7,40})`""").find(umfeld)
        assertTrue(
            "Die Prüfsumme im README nennt keinen Commit. Ohne ihn kann sie niemand " +
                "nachrechnen, und sie veraltet lautlos.",
            commit != null,
        )
    }

    /**
     * Und den genannten Commit gibt es auch wirklich.
     *
     * „Commit `b753657`" nachzuschlagen ist die halbe Zusicherung; die andere Hälfte ist,
     * dass man ihn auschecken kann. Ein Tippfehler in sieben Zeichen fällt niemandem auf,
     * und wer es merkt, merkt es erst beim Nachrechnen - also genau dann, wenn er der Zahl
     * misstrauen wollte.
     *
     * Ohne `.git` - etwa in einem Quelltext-Archiv, wie F-Droid es baut - wird nicht
     * geprüft. Eine Regel, die dort fällt, sagt nichts über den Quelltext aus.
     */
    @Test
    fun `den genannten commit gibt es`() {
        if (!File("../.git").exists()) return
        val commit = Regex("""Commit\s+`([0-9a-f]{7,40})`""").find(readme)?.groupValues?.get(1)
        assertTrue("kein Commit im README - siehe die Regel darüber", commit != null)
        val gefunden = runCatching {
            ProcessBuilder("git", "cat-file", "-e", "$commit^{commit}")
                .directory(File(".."))
                .start()
                .waitFor()
        }.getOrNull()
        assertEquals(
            "Das README nennt Commit $commit zur Prüfsumme, aber den gibt es in diesem " +
                "Verzeichnis nicht. Damit ist die Zahl nicht nachrechenbar.",
            0,
            gefunden,
        )
    }

    @Test
    fun `das werkzeug zum nachrechnen gibt es`() {
        val werkzeug = File("../tools/nachbauen.sh")
        assertTrue("tools/nachbauen.sh fehlt, das README nennt es", werkzeug.isFile)
        assertEquals(true, werkzeug.canExecute())
        assertTrue("README nennt das Werkzeug nicht", "tools/nachbauen.sh" in readme)
    }
}
