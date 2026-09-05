package org.biglau.design

import org.biglau.Quelltext
import org.biglau.ui.Notice
import org.biglau.ui.SettingsLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Sprung in die Einstellungen läuft über eine Absicht, nicht über die Klasse.
 *
 * Vier Bildschirme aus vier Ecken springen auf eine Unterseite: Wähltastatur → Anrufarten,
 * Kontakte → Sortierung, App-Liste → ausgeblendete Apps, Notruf → Notfallkontakte. Jeder
 * nannte `SettingsActivity` beim Namen. `PLAN.md` 2.1 sagt aber, dass ein `feature:*` nie
 * ein anderes importiert — mit vier solchen Verweisen wäre der Schnitt gar nicht möglich.
 *
 * Der Preis einer Absicht ist, dass der Compiler sie nicht mehr prüft: Manifest und
 * Quelltext können auseinanderlaufen, und der Sprung führt dann **stumm** ins Leere. Genau
 * dagegen steht diese Regel.
 */
class SettingsLinkTest {

    private val manifest = Quelltext.file("src/main/AndroidManifest.xml").readText()

    @Test
    fun `die Einstellungen beantworten die Absicht`() {
        val block = Quelltext.cut(manifest, ".settings.SettingsActivity", "</activity>")
        assertTrue(
            "SettingsActivity hat keinen Filter fuer ${SettingsLink.ACTION}: $block",
            SettingsLink.ACTION in block,
        )
        assertTrue("ohne DEFAULT-Kategorie startet keine implizite Absicht", "category.DEFAULT" in block)
    }

    /**
     * Nur die Hülle darf die Einstellungen beim Namen kennen. `MainActivity` ist diese
     * Hülle: sie hält die HOME-Rolle und den Notmodus und kennt ohnehin jeden Bildschirm.
     */
    @Test
    fun `ausser der Huelle nennt niemand SettingsActivity`() {
        val nenner = Quelltext.files()
            .filter { "org.biglau.settings.SettingsActivity" in it.readText() }
            .map { it.name }
            .filterNot { it == "MainActivity.kt" || it == "SettingsActivity.kt" }
        assertEquals("springt an SettingsLink vorbei: $nenner", emptyList<String>(), nenner)
    }

    /** Und der Weg dorthin wird auch benutzt - sonst prüfte die Regel eine tote Klasse. */
    @Test
    fun `die vier Springer benutzen den Weg`() {
        val springer = listOf(
            "org/biglau/toggles/SosActivity.kt",
            "org/biglau/phone/DialerActivity.kt",
            "org/biglau/contacts/ContactsActivity.kt",
            "org/biglau/apps/AppDrawerActivity.kt",
        )
        val ohne = springer.filterNot { "SettingsLink.toPage(" in Quelltext.file(it).readText() }
        assertEquals("springt nicht ueber SettingsLink: $ohne", emptyList<String>(), ohne)
    }

    /** Die Absicht bleibt im eigenen Programm - sonst könnte ein fremdes sie beantworten. */
    @Test
    fun `die Absicht bleibt im eigenen Programm`() {
        val quelle = Quelltext.file("org/biglau/ui/SettingsLink.kt").readText()
        assertTrue("ohne setPackage waere die Absicht offen", "setPackage(" in quelle)
    }

    /**
     * Dasselbe für den wartenden Hinweis: `Notice` gehört zum Design-System und wird von
     * überall benutzt, der Bildschirm dazu ist eine Activity der Anwendung. Nennte `Notice`
     * die Klasse, zöge es die halbe App ins Design-System.
     */
    @Test
    fun `der wartende Hinweis wird ueber eine Absicht geoeffnet`() {
        val block = Quelltext.cut(manifest, ".ui.NoticeActivity", "</activity>")
        assertTrue("NoticeActivity hat keinen Filter fuer ${Notice.ACTION}: $block", Notice.ACTION in block)
        assertTrue("ohne DEFAULT-Kategorie startet keine implizite Absicht", "category.DEFAULT" in block)
        val quelle = Quelltext.file("org/biglau/ui/Notice.kt").readLines()
        // Nur Code: im Kommentar darf die Activity vorkommen - dort steht ja gerade, wo
        // die Gegenstelle wohnt. Ein Test, der Kommentare mitliest, erzieht zum Schweigen.
        val code = quelle.filterNot { it.trimStart().let { z -> z.startsWith("*") || z.startsWith("//") || z.startsWith("/*") } }
        assertTrue(
            "Notice nennt die Activity im Code: ${code.filter { "NoticeActivity" in it }}",
            code.none { "NoticeActivity" in it },
        )
        assertTrue("ohne setPackage waere die Absicht offen", code.any { "setPackage(" in it })
    }
}
