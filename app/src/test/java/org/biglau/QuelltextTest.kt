package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Liste der Quelltextwurzeln ist vollstaendig.
 *
 * Ohne diese Regel waere der Modulschnitt aus `PLAN.md` 2.1 eine leise Entwertung: wer
 * Dateien nach `:core:model` schoebe, haette danach dieselbe Zahl gruener Tests - nur
 * pruefen wuerden sie den verschobenen Teil nicht mehr. Deshalb sucht dieser Test die
 * Module selbst und vergleicht.
 */
class QuelltextTest {

    /** Jedes Verzeichnis mit einer `build.gradle.kts`, ohne die Bauverzeichnisse. */
    private fun module(): List<File> = File("..").walkTopDown()
        .onEnter { it.name != "build" && it.name != ".git" && it.name != ".gradle" }
        .filter { it.name == "build.gradle.kts" }
        .map { it.parentFile }
        .toList()

    private fun quellorte(unterordner: String): List<File> = module()
        .flatMap { modul -> listOf("java", "kotlin").map { File(modul, "src/$unterordner/$it") } }
        .filter { it.isDirectory }

    private fun schluessel(dateien: List<File>) = dateien.map { it.canonicalPath }.sorted()

    @Test
    fun `jedes Modul mit Hauptquelltext steht in den Wurzeln`() {
        assertEquals(
            "Ein Modul fehlt in Quelltext.wurzeln - seine Dateien pruefte keine Regel mehr",
            schluessel(quellorte("main")),
            schluessel(Quelltext.wurzeln),
        )
    }

    @Test
    fun `jedes Modul mit Testquelltext steht in den Testwurzeln`() {
        assertEquals(
            "Ein Modul fehlt in Quelltext.testWurzeln",
            schluessel(quellorte("test")),
            schluessel(Quelltext.testWurzeln),
        )
    }

    /**
     * Der Fehler, gegen den das alles steht: eine Wurzel, die es nicht gibt, faellt nicht
     * auf. Sie muss deshalb hier auffallen.
     */
    @Test
    fun `keine Wurzel zeigt ins Leere`() {
        (Quelltext.wurzeln + Quelltext.testWurzeln).forEach {
            assertTrue("Wurzel gibt es nicht: ${it.path}", it.isDirectory)
        }
    }

    @Test
    fun `die Wurzeln tragen tatsaechlich Quelltext`() {
        assertTrue("kein Hauptquelltext gefunden", Quelltext.dateien().size > 50)
        assertTrue("kein Testquelltext gefunden", Quelltext.testDateien().size > 50)
    }

    /**
     * Keine Regel geht am Verzeichnis vorbei selbst los.
     *
     * Genau das war heute Nacht die Luecke: die Wurzelliste war da, und `SlopRulesTest`
     * lief trotzdem weiter ueber `File("src/main/java/org/biglau")`. Beim Umzug nach
     * `:core:system` hat sie ihre fuenfunddreissig Dateien einfach nicht mehr gesehen -
     * ohne einen roten Test, denn `walkTopDown` auf einem Pfad, der weniger enthaelt,
     * liefert eben weniger.
     *
     * Aufgefallen ist es nur, weil ich beim Aufraeumen nochmal gesucht habe. Deshalb sucht
     * jetzt diese Regel.
     */
    @Test
    fun `keine Regel baut sich ihren Quellpfad selbst`() {
        val treffer = Quelltext.testDateien()
            .filterNot { it.name == "Quelltext.kt" || it.name == "QuelltextTest.kt" }
            .flatMap { datei ->
                datei.readLines().withIndex()
                    .filter { zeile ->
                        // Die Ressourcen bleiben in :app - nur die Quellverzeichnisse
                        // wandern, und nur um die geht es hier.
                        listOf("File(\"src/main/java", "File(\"src/test/java")
                            .any { it in zeile.value }
                    }
                    .map { "${datei.name}:${it.index + 1}  ${it.value.trim()}" }
            }
        assertEquals("liest am Verzeichnis vorbei: $treffer", emptyList<String>(), treffer)
    }

    /**
     * Auch die Ressourcen liegen inzwischen in mehreren Modulen - `:core:ui` bringt die
     * Schriftdateien mit. Eine Regel, die sie sucht, soll das Modul nicht wissen muessen.
     */
    @Test
    fun `jedes Modul mit Ressourcen steht in den Ressourcenwurzeln`() {
        val vorhanden = module().map { File(it, "src/main/res") }.filter { it.isDirectory }
        assertEquals(
            "Ein Modul fehlt in Quelltext.resWurzeln",
            schluessel(vorhanden),
            schluessel(Quelltext.resWurzeln),
        )
    }

    /**
     * Und keine Regel sucht die Texte selbst.
     *
     * Zehn Regeln lasen `src/main/res/values/strings.xml` und meinten „alle Texte". Sobald
     * ein Text mit seinem Modul umzieht, pruefen sie ihn nicht mehr - lautlos, denn eine
     * Datei, die es gibt, liest sich weiterhin gut. `Quelltext.texte` fragt alle Module.
     *
     * `themes.xml` bleibt ausgenommen: das Thema der Anwendung liegt in `:app` und nirgends
     * sonst.
     */
    @Test
    fun `keine Regel sucht die Texte selbst`() {
        val treffer = Quelltext.testDateien()
            .filterNot { it.name == "Quelltext.kt" || it.name == "QuelltextTest.kt" }
            .flatMap { datei ->
                datei.readLines().withIndex()
                    .filter { "src/main/res/values" in it.value && "themes.xml" !in it.value }
                    .map { "${datei.name}:${it.index + 1}  ${it.value.trim()}" }
            }
        assertEquals("liest Texte am Verzeichnis vorbei: $treffer", emptyList<String>(), treffer)
    }

    /**
     * Die Siebe, auf denen die anderen Regeln stehen, fangen noch etwas.
     *
     * Rund dreissig Regeln fangen mit `Quelltext.dateien().filter { … }` an - nach
     * `Activity.kt`, nach `@Composable`, nach `Repository`. `QuelltextTest` sichert bisher
     * nur, dass die **Wurzeln** stimmen. Das genuegt nicht: ein Sieb, dessen Muster nicht
     * mehr passt, liefert nichts, und die Regel darueber ist still gruen.
     *
     * Am 03.09.2026 in einer Nacht mit dreiundzwanzig Dateiumzuegen gemessen: 142
     * Kotlin-Dateien, 13 Activities, 29 Dateien mit `@Composable`. Die Untergrenzen hier
     * sind bewusst grosszuegig - sie sollen einen **Zusammenbruch** melden, nicht jedes
     * Aufraeumen. Wer eine davon reisst, hat entweder etwas Grosses geloescht oder ein Sieb
     * kaputtgemacht; beides will man wissen.
     */
    @Test
    fun `die Siebe der anderen Regeln fangen noch etwas`() {
        val alle = Quelltext.dateien()
        assertTrue("nur ${alle.size} Kotlin-Dateien gefunden", alle.size >= 100)
        val activities = alle.filter { it.name.endsWith("Activity.kt") }
        assertTrue("nur ${activities.size} Activities gefunden", activities.size >= 10)
        val composables = alle.filter { "@Composable" in it.readText() }
        assertTrue("nur ${composables.size} Dateien mit @Composable", composables.size >= 20)
    }
}
