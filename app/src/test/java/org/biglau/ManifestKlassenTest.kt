package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Jede Klasse, die im Manifest steht, gibt es auch.
 *
 * Das Manifest nennt Activities, Dienste und Empfänger als **Zeichenkette**. Wer eine
 * Klasse verschiebt oder umbenennt, merkt davon beim Übersetzen nichts: der Quelltext ist
 * in Ordnung, das Manifest zeigt ins Leere, und aufgefallen wäre es erst auf dem Telefon —
 * beim Tippen auf eine Kachel, oder gar nicht, weil ein Empfänger einfach nichts mehr tut.
 *
 * Am 3.9.2026 ist genau das beinahe passiert: `MessageReminderReceiver` wanderte von
 * `notify` nach `sms`, weil er von SMS handelt. Der Wecker für die wiederholte Erinnerung
 * hätte danach auf eine Klasse gezeigt, die es unter dem Namen nicht mehr gibt.
 */
class ManifestKlassenTest {

    // Das Manifest gibt es nur einmal und nur in :app; es wandert bei keinem Modulschnitt.
    private val manifest = java.io.File("src/main/AndroidManifest.xml")

    @Test
    fun `jede im manifest genannte klasse steht im quelltext`() {
        val genannt = Regex("""android:name="(\.[\w.]+)"""")
            .findAll(manifest.readText())
            .map { it.groupValues[1].removePrefix(".") }
            .toList()
        assertEquals("das Manifest nennt keine einzige eigene Klasse mehr - Regel kaputt?", true, genannt.size >= 10)

        val vorhanden = Quelltext.dateien().map { it.nameWithoutExtension }.toSet()
        val quelltexte = Quelltext.dateien().associate { it.nameWithoutExtension to it.readText() }
        val fehlend = genannt.filter { name ->
            val einfach = name.substringAfterLast('.')
            // Die Datei heisst meistens wie die Klasse; sonst muss die Klasse wenigstens
            // irgendwo deklariert sein.
            einfach !in vorhanden &&
                quelltexte.values.none { Regex("""(class|object) $einfach\b""").containsMatchIn(it) }
        }
        assertEquals("im Manifest genannt, im Quelltext nicht gefunden", emptyList<String>(), fehlend)
    }

    @Test
    fun `jede im manifest genannte klasse liegt im angegebenen paket`() {
        val falsch = Regex("""android:name="\.([\w.]+)"""")
            .findAll(manifest.readText())
            .map { it.groupValues[1] }
            .filter { it.contains('.') }
            .mapNotNull { voll ->
                val paket = "org.biglau." + voll.substringBeforeLast('.')
                val klasse = voll.substringAfterLast('.')
                val datei = Quelltext.dateien().firstOrNull { d ->
                    Regex("""(class|object) $klasse\b""").containsMatchIn(d.readText())
                } ?: return@mapNotNull "$voll (nicht gefunden)"
                val stehtIn = Regex("""^package ([\w.]+)""", RegexOption.MULTILINE)
                    .find(datei.readText())?.groupValues?.get(1)
                if (stehtIn == paket) null else "$voll steht in Wahrheit in $stehtIn"
            }.toList()
        assertEquals("Manifest und Paket gehen auseinander", emptyList<String>(), falsch)
    }
}
