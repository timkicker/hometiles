package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Angaben für F-Droid — vollständig, in beiden Sprachen, und nicht abgeschrieben.
 *
 * Was hier schiefgeht, sieht niemand beim Bauen: die Auflistung im Laden entsteht aus
 * Dateien, die kein Übersetzer anfasst. Am 3.9.2026 gefunden: die **deutschen**
 * Bildschirmfotos waren Byte für Byte die englischen. Für eine App, deren ganzer Zweck ist,
 * dass jemand die Sprache lesen kann, ist das der falscheste Fehler von allen — wer sie in
 * F-Droid ansieht, sieht eine fremde Sprache und geht weiter.
 *
 * Ebenso still: eine neue Fassung ohne `changelogs/<versionCode>.txt`. F-Droid zeigt dann
 * gar nichts an, ohne sich zu beschweren.
 */
class LadenTexteTest {

    private val wurzel = File("../fastlane/metadata/android")

    /**
     * Die Sprachen kommen aus dem Bau, nicht aus einer Liste hier.
     *
     * Am 04.09.2026 stand hier `listOf("de-DE", "en-US")`, waehrend die App laengst fuenf
     * Sprachen ausliefert. Die Auflistung im Laden haette drei davon nie gezeigt, und keine
     * Regel haette es gesagt: dieselbe Sorte Fehler wie bei den Textregeln, die zwei
     * Verzeichnisse fest kannten. Was ausgeliefert wird, steht in `resourceConfigurations`;
     * von dort wird es gelesen, damit die naechste Sprache nicht wieder vergessen wird.
     */
    private val sprachen: List<String> = Regex("""resourceConfigurations \+= listOf\(([^)]*)\)""")
        .find(File("build.gradle.kts").readText())
        ?.groupValues?.get(1)
        ?.split(",")
        ?.mapNotNull { Regex(""""(\w+)"""").find(it)?.groupValues?.get(1) }
        ?.map { kuerzel -> ladenName(kuerzel) }
        .orEmpty()

    /**
     * Von `de` zu `de-DE`. F-Droid will das Land dazu, der Bau nicht.
     *
     * Nur die fuenf, die es gibt; eine sechste faellt hier laut auf, statt still ein
     * Verzeichnis zu meinen, das niemand angelegt hat.
     */
    private fun ladenName(kuerzel: String): String = when (kuerzel) {
        "en" -> "en-US"
        "de" -> "de-DE"
        "fr" -> "fr-FR"
        "es" -> "es-ES"
        "it" -> "it-IT"
        else -> throw AssertionError(
            "Die Sprache \"$kuerzel\" steht im Bau, aber hier ist nicht gesagt, wie ihr " +
                "Verzeichnis unter fastlane heisst. F-Droid will Sprache und Land.",
        )
    }

    /**
     * Bilder gehoeren zu jeder Sprache, nicht zu einer Auswahl.
     *
     * Fuer ein paar Stunden am 04.09.2026 stand hier eine kuerzere Liste als [sprachen]: die
     * Texte waren in fuenf Sprachen da, die Bilder in zweien. Das war als Zettel gedacht und
     * ist der Zustand, in dem so etwas liegen bleibt. Seit die Bilder in allen fuenf Sprachen
     * aufgenommen sind, gibt es keinen Grund mehr fuer zwei Listen - und wer eine sechste
     * Sprache anlegt, sieht hier sofort, dass Bilder dazugehoeren.
     */
    private val mitBildern get() = sprachen

    private fun datei(sprache: String, name: String) = File(wurzel, "$sprache/$name")

    private fun bilder(sprache: String): List<File> =
        File(wurzel, "$sprache/images/phoneScreenshots")
            .listFiles { d -> d.extension == "png" }?.sortedBy { it.name }.orEmpty()

    /**
     * Zuerst: dass ueberhaupt etwas gelesen wurde.
     *
     * Alle Regeln hier laufen mit `forEach` ueber [sprachen]. Bliebe die Liste leer - weil
     * `resourceConfigurations` umbenannt wurde oder anders geschrieben ist -, liefen sie
     * durch und prueften **nichts**, ohne ein Wort zu sagen. Genau die Falle, die dieser
     * Baum schon dreimal gestellt hat.
     */
    @Test
    fun `die Sprachen kommen wirklich aus dem Bau`() {
        assertTrue(
            "Aus resourceConfigurations wurde keine Sprache gelesen. Dann laufen alle " +
                "Regeln hier ins Leere und bleiben gruen.",
            sprachen.size >= 2,
        )
        assertEquals(
            "Die Sprachen des Ladens sind nicht die des Baus.",
            listOf("en-US", "de-DE", "fr-FR", "es-ES", "it-IT").sorted(),
            sprachen.sorted(),
        )
    }

    @Test
    fun `beide sprachen haben dieselben angaben`() {
        sprachen.forEach { sprache ->
            listOf("title.txt", "short_description.txt", "full_description.txt").forEach {
                assertTrue("$sprache/$it fehlt", datei(sprache, it).isFile)
            }
        }
    }

    @Test
    fun `die kurzbeschreibung passt in die vorgegebene laenge`() {
        sprachen.forEach { sprache ->
            val text = datei(sprache, "short_description.txt").readText().trim()
            assertTrue("$sprache: Kurzbeschreibung leer", text.isNotEmpty())
            assertTrue(
                "$sprache: Kurzbeschreibung ist ${text.length} Zeichen lang, F-Droid " +
                    "schneidet bei 80 ab",
                text.length <= 80,
            )
            val lang = datei(sprache, "full_description.txt").readText().trim()
            assertTrue("$sprache: Beschreibung ist zu lang (${lang.length} > 4000)", lang.length <= 4000)
        }
    }

    @Test
    fun `zur gebauten fassung gibt es einen aenderungstext`() {
        val bau = File("build.gradle.kts").readText()
        val version = Regex("""versionCode\s*=\s*(\d+)""").find(bau)?.groupValues?.get(1)
        assertTrue("versionCode nicht gefunden", version != null)
        sprachen.forEach { sprache ->
            val eintrag = datei(sprache, "changelogs/$version.txt")
            assertTrue(
                "$sprache: kein Änderungstext für versionCode $version. F-Droid zeigt " +
                    "dann nichts an und sagt nichts dazu.",
                eintrag.isFile && eintrag.readText().isNotBlank(),
            )
        }
    }

    @Test
    fun `die bildschirmfotos sind luecklos durchnummeriert`() {
        mitBildern.forEach { sprache ->
            val namen = bilder(sprache).map { it.nameWithoutExtension }
            assertTrue("$sprache: keine Bildschirmfotos", namen.isNotEmpty())
            assertEquals(
                "$sprache: Lücke in der Nummerierung",
                (1..namen.size).map { it.toString() },
                namen.sortedBy { it.toIntOrNull() ?: 0 },
            )
        }
    }

    /**
     * Jedes Paar, nicht nur Deutsch gegen Englisch.
     *
     * Am 3.9.2026 waren die deutschen Bildschirmfotos Byte für Byte die englischen, und
     * diese Regel entstand daraufhin — für genau dieses eine Paar. Als am 04.09.2026 drei
     * Sprachen dazukamen, hätte sie neun neue Paare nicht angesehen: dieselbe Sorte Fehler
     * wie bei der Sprachliste selbst, eine Regel, die zwei Seiten kennt und nicht alle.
     */
    @Test
    fun `keine sprache zeigt die bilder einer anderen`() {
        val fotos = mitBildern.associateWith { sprache ->
            bilder(sprache).associate { it.name to it.readBytes().toList() }
        }
        val paare = mitBildern.flatMapIndexed { nr, eine ->
            mitBildern.drop(nr + 1).map { andere -> eine to andere }
        }
        assertTrue("Zu wenige Sprachen mit Bildern, die Regel misst nichts", paare.isNotEmpty())
        val gleich = paare.flatMap { (eine, andere) ->
            assertEquals(
                "$eine und $andere haben verschieden viele Bildschirmfotos",
                fotos.getValue(eine).keys,
                fotos.getValue(andere).keys,
            )
            fotos.getValue(eine)
                .filter { (name, inhalt) -> fotos.getValue(andere)[name] == inhalt }
                .keys.map { "$eine/$it = $andere/$it" }
        }.sorted()
        assertEquals(
            "Diese Bildschirmfotos sind Byte für Byte dieselben wie in einer anderen " +
                "Sprache. Wer die App in F-Droid ansieht, sieht eine Sprache, die er " +
                "vielleicht nicht liest.",
            emptyList<String>(),
            gleich,
        )
    }
}
