package org.biglau.res

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jede Sprache muss dieselben Texte kennen wie Englisch.
 *
 * Anlass: der Notfall-Bildschirm - der, den jemand sieht, dessen Telefon gerade nicht mehr
 * startet - stand fest auf Deutsch im Quelltext. Auf einem englischen Gerät war ausgerechnet
 * die Rettung unlesbar. Ein fehlender Schlüssel fällt sonst erst dem Nutzer auf, und zwar
 * genau dann, wenn er ihn am wenigsten gebrauchen kann.
 */
class TranslationsTest {

    /** Der Name der App wird nicht übersetzt - er ist in jeder Sprache derselbe. */
    private val absichtlichNurEnglisch = setOf("app_name")

    /**
     * Die uebersetzten Sprachen, gesucht statt aufgezaehlt.
     *
     * Bis zum 04.09.2026 stand in jeder Regel dieser Klasse `"values-de"` fest. Eine dritte
     * Sprache waere angelegt worden und von keiner Regel hier angesehen: keine Schluessel,
     * keine Platzhalter, keine Leerstellen. Gemerkt beim Planen von PLAN.md 10.4, bevor die
     * erste neue Sprache da war.
     */
    private val uebersetzt = Quelltext.uebersetzungen()

    /**
     * Die Sprachen, die ausgeliefert werden, ohne die Grundsprache.
     *
     * Vollstaendig sein muss nur, was mitgeht. Eine Sprache in Arbeit steht im Baum, aber
     * nicht in `resourceConfigurations`; Android faellt fuer ihre fehlenden Texte auf
     * Englisch zurueck, und das ist waehrend der Uebersetzung der richtige Zustand. Alles
     * andere hier gilt auch fuer sie: keine fremden Schluessel, keine leeren Texte, gleiche
     * Platzhalter. Nur die Frage, ob schon alles da ist, waere verfrueht.
     */
    private val fertig = Quelltext.ausgeliefert().filter { it != "values" }

    private fun keys(dir: String, tag: String): Set<String> {
        val dateien = Quelltext.texte(dir, if (tag == "plurals") "plurals.xml" else "strings.xml")
        assertTrue("$dir/$tag fehlt in jedem Modul", dateien.isNotEmpty())
        return dateien.flatMap { datei ->
            Regex("<$tag name=\"([^\"]+)\"").findAll(datei.readText()).map { it.groupValues[1] }
        }.toSet()
    }

    @Test
    fun `jeder uebersetzte Text hat einen englischen`() {
        val en = keys("values", "string")
        uebersetzt.forEach { sprache ->
            assertEquals("nur in $sprache vorhanden", emptySet<String>(), keys(sprache, "string") - en)
        }
    }

    @Test
    fun `jeder englische Text ist uebersetzt`() {
        val en = keys("values", "string")
        fertig.forEach { sprache ->
            assertEquals(
                "fehlt in $sprache",
                emptySet<String>(),
                en - keys(sprache, "string") - absichtlichNurEnglisch,
            )
        }
    }

    @Test
    fun `auch die Mehrzahlformen stehen in jeder Sprache`() {
        fertig.forEach { sprache ->
            assertEquals("Mehrzahlformen in $sprache", keys("values", "plurals"), keys(sprache, "plurals"))
        }
    }

    /**
     * Die andere Seite derselben Frage: was in `resourceConfigurations` steht, muss es
     * auch als Verzeichnis geben.
     *
     * Sonst traegt das Archiv eine Sprache im Schild, die keine Texte hat, und jeder
     * Bildschirm darin ist englisch. Die Regeln darueber pruefen nur die Richtung
     * Verzeichnis nach Auslieferung; ohne diese hier waere die Rueckrichtung ungemessen.
     */
    @Test
    fun `jede ausgelieferte Sprache steht auch im Baum`() {
        assertEquals(
            "In resourceConfigurations steht eine Sprache, zu der es keine Texte gibt",
            emptyList<String>(),
            Quelltext.ausgeliefert().filterNot { it in Quelltext.sprachen() },
        )
    }

    @Test
    fun `kein Text ist leer`() {
        Quelltext.sprachen().forEach { dir ->
            val text = Quelltext.texte(dir).joinToString("\n") { it.readText() }
            val leer = Regex("<string name=\"([^\"]+)\"></string>").findAll(text).map { it.groupValues[1] }.toList()
            assertEquals("leere Texte in $dir", emptyList<String>(), leer)
        }
    }

    /**
     * Jedes Modul mit Texten hat beide Sprachen.
     *
     * Beim Umzug von sieben Texten nach `core:ui` am 3.9.2026 ist dort zum ersten Mal ein
     * `values-de` entstanden. Hätte ich es vergessen, wäre die Oberfläche auf einem
     * deutschen Telefon an diesen Stellen englisch geblieben — die Schlüsselvergleiche oben
     * hätten es gemeldet, aber als Liste fehlender Schlüssel, nicht als das, was es ist.
     * Diese Regel sagt es beim Namen des Moduls.
     */
    @Test
    fun `jedes Modul mit Texten hat beide Sprachen`() {
        val englisch = Quelltext.texte("values").map { it.parentFile.parentFile.parentFile }
        fertig.forEach { sprache ->
            assertEquals(
                "Ein Modul hat englische Texte und keine in $sprache",
                englisch.map { it.canonicalPath }.sorted(),
                Quelltext.texte(sprache).map { it.parentFile.parentFile.parentFile.canonicalPath }.sorted(),
            )
        }
    }

    @Test
    fun `Platzhalter stimmen ueberein`() {
        // Ein %1$s auf Deutsch und keiner auf Englisch wirft zur Laufzeit - und zwar erst
        // auf dem Geraet mit der anderen Sprache.
        val pattern = Regex("<string name=\"([^\"]+)\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
        fun placeholders(dir: String): Map<String, Int> =
            pattern.findAll(Quelltext.texte(dir).joinToString("\n") { it.readText() })
                .associate { m -> m.groupValues[1] to Regex("%\\d+\\$[sd]").findAll(m.groupValues[2]).count() }
        val en = placeholders("values")
        uebersetzt.forEach { sprache ->
            val andere = placeholders(sprache)
            val abweichend = en.filter { (key, count) -> andere[key] != null && andere[key] != count }.keys
            assertEquals("unterschiedlich viele Platzhalter in $sprache", emptySet<String>(), abweichend)
        }
    }

    /**
     * Auch die Wortlisten muessen ueberall gleich lang sein.
     *
     * `string-array` ist die dritte Sorte Text, und keine Regel hat sie bisher angesehen.
     * Sie ist die gefaehrlichste von den dreien: eine Wortliste wird ueber ihren **Index**
     * gelesen, `tile_colors[colorIndex]`. Fehlt darin ein Eintrag, heisst die Kachel nicht
     * nur falsch, sondern die Zugriffe verschieben sich alle um eins, und der letzte greift
     * ins Leere.
     *
     * Bei Texten und Mehrzahlformen faellt ein fehlender Eintrag auf `values` zurueck. Eine
     * Wortliste faellt als **ganze** zurueck oder gar nicht; halb uebersetzt gibt es nicht.
     * Deshalb gilt hier auch fuer eine Sprache in Arbeit: entweder ganz oder keine.
     *
     * Am 04.09.2026 beim Uebersetzen aufgefallen, als die franzoesische Fassung von
     * `tile_colors` zu schreiben war und auffiel, dass niemand nachsehen wuerde.
     */
    @Test
    fun `Wortlisten sind ueberall gleich lang`() {
        fun listen(dir: String): Map<String, Int> =
            Regex("""<string-array name="([^"]+)">(.*?)</string-array>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(Quelltext.texte(dir).joinToString("\n") { it.readText() })
                .associate { it.groupValues[1] to Regex("<item>").findAll(it.groupValues[2]).count() }
        val en = listen("values")
        assertTrue("Es gibt gar keine Wortliste mehr - dann prueft diese Regel nichts", en.isNotEmpty())
        uebersetzt.forEach { sprache ->
            val andere = listen(sprache)
            val falsch = en.keys.intersect(andere.keys)
                .filter { en[it] != andere[it] }
                .map { "$sprache: $it hat ${andere[it]} statt ${en[it]} Eintraege" }
            assertEquals("Eine Wortliste ist unterschiedlich lang", emptyList<String>(), falsch)
        }
    }

    /**
     * Auch in den Mehrzahlformen muessen die Platzhalter stimmen.
     *
     * Die Regel darueber liest nur `<string>`. In einer Mehrzahlform ist ein fehlender
     * `%1$d` aber **wahrscheinlicher**, nicht unwahrscheinlicher: die Form fuer eins schreibt
     * die Zahl oft aus ("An einen Kontakt gesendet"), die fuer viele braucht sie
     * ("An %1$d Kontakte gesendet"). Wer uebersetzt, sieht zwei aehnliche Zeilen und
     * vergisst leicht die eine Ziffer. Zur Laufzeit wirft das, und zwar nur in der Sprache,
     * die man selbst nicht liest.
     *
     * Verglichen wird deshalb **Form gegen gleiche Form**, nicht Eintrag gegen Eintrag: dass
     * `one` und `other` sich unterscheiden, ist richtig und kein Befund.
     *
     * Am 04.09.2026 beim Vorbereiten von PLAN.md 10.4 aufgefallen, wieder als die eine
     * gemessene und die andere ungemessene Seite derselben Frage.
     */
    @Test
    fun `Platzhalter stimmen auch in den Mehrzahlformen`() {
        fun formen(dir: String): Map<Pair<String, String>, Set<String>> {
            val inhalt = Quelltext.texte(dir, "plurals.xml").joinToString("\n") { it.readText() }
            return Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(inhalt)
                .flatMap { eintrag ->
                    Regex("""<item quantity="([^"]+)">(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
                        .findAll(eintrag.groupValues[2])
                        .map { stueck ->
                            (eintrag.groupValues[1] to stueck.groupValues[1]) to
                                Regex("""%\d+\$[sd]""").findAll(stueck.groupValues[2])
                                    .map { it.value }.toSet()
                        }
                }.toMap()
        }
        val en = formen("values")
        uebersetzt.forEach { sprache ->
            val andere = formen(sprache)
            val abweichend = en.keys.intersect(andere.keys)
                .filter { en[it] != andere[it] }
                .map { (name, menge) -> "$sprache: $name/$menge ${en[name to menge]} statt ${andere[name to menge]}" }
            assertEquals("Platzhalter in einer Mehrzahlform", emptyList<String>(), abweichend)
        }
    }

    /**
     * Ein längerer Text, der in beiden Dateien wortgleich steht, ist fast immer eine
     * vergessene Übersetzung. Kurze Wörter wie „SOS" oder „OK" dürfen gleich sein.
     *
     * Diese Prüfung stand bis zum 3.9.2026 in einer zweiten Klasse `TranslationTest`, deren
     * drei andere Prüfungen wortgleich hier schon standen. Zwei Stellen, die dasselbe
     * zählen, sind keine doppelte Sicherheit: sie sind der Ort, an dem eines Tages die eine
     * repariert wird und die andere nicht.
     */
    @Test
    fun `kein laengerer Text steht unuebersetzt da`() {
        val pattern = Regex("<string name=\"([^\"]+)\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
        fun texte(dir: String): Map<String, String> =
            pattern.findAll(Quelltext.texte(dir).joinToString("\n") { it.readText() })
                .associate { m -> m.groupValues[1] to m.groupValues[2] }
        val en = texte("values")
        uebersetzt.forEach { sprache ->
            val andere = texte(sprache)
            val gleich = en.keys.intersect(andere.keys).filter { name ->
                en.getValue(name).length > 12 && en.getValue(name) == andere.getValue(name)
            }
            assertEquals("wortgleich mit dem Englischen in $sprache - übersetzt?", emptyList<String>(), gleich)
        }
    }
}

/**
 * Zahlen in Worten, die zur Zahl passen.
 *
 * Die Rückfrage vor dem Zurücksetzen sagte "1 folders". Wer eine Warnung schlampig
 * findet, nimmt sie nicht ernst - und das ist die eine Warnung, die man ernst nehmen muss.
 */
class PluralsTest {

    private fun plurale(verzeichnis: String): Map<String, Set<String>> {
        val dateien = Quelltext.texte(verzeichnis)
        return Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(dateien.joinToString("\n") { it.readText() })
            .associate { treffer ->
                treffer.groupValues[1] to Regex("""quantity="([^"]+)"""")
                    .findAll(treffer.groupValues[2])
                    .map { it.groupValues[1] }
                    .toSet()
            }
    }

    @Test
    fun `jede sprache kennt dieselben plurale`() {
        Quelltext.ausgeliefert().filter { it != "values" }.forEach { sprache ->
            assertEquals("Mehrzahlformen in $sprache", plurale("values").keys, plurale(sprache).keys)
        }
    }

    @Test
    fun `jedes plural hat einzahl und mehrzahl`() {
        val unvollstaendig = (plurale("values") + plurale("values-de"))
            .filterValues { !it.containsAll(setOf("one", "other")) }
            .keys
        assertEquals(emptySet<String>(), unvollstaendig)
    }
}
