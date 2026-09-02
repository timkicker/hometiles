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
}
