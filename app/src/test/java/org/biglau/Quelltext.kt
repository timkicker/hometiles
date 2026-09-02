package org.biglau

import java.io.File

/**
 * Wo der Quelltext von BigLau liegt - fuer die Regeln, die ihn lesen.
 *
 * Knapp die Haelfte der Tests hier prueft nicht Verhalten, sondern Quelltext: tote Felder,
 * unbenutzte Texte, Sprachregeln, Dokumentation am falschen Platz. Sie alle liefen bisher
 * ueber `File("src/main/java")`, den Hauptquelltext von `:app`.
 *
 * Solange alles in `:app` liegt, stimmt das. Beim Modulschnitt aus `PLAN.md` 2.1 stimmt es
 * nicht mehr - und zwar **lautlos**: `walkTopDown()` auf einem Verzeichnis, das es nicht
 * gibt, liefert keine Datei und keinen Fehler. Die Regel bliebe gruen und pruefte nichts
 * mehr. Genau deshalb steht die Liste der Wurzeln an einer Stelle, und ein Test sieht nach,
 * dass sie vollstaendig ist.
 */
object Quelltext {

    /**
     * Alle Wurzeln mit Hauptquelltext, relativ zum Arbeitsverzeichnis der Tests (`app/`).
     *
     * Neue Module gehoeren hier hinein. `QuelltextTest` faellt um, wenn eines fehlt.
     */
    val wurzeln: List<File> = listOf(
        File("src/main/java"),
        File("../core/model/src/main/kotlin"),
        File("../core/data/src/main/kotlin"),
        File("../core/system/src/main/kotlin"),
    )

    /** Die Wurzeln mit Testquelltext. */
    val testWurzeln: List<File> = listOf(
        File("src/test/java"),
        File("../core/system/src/test/kotlin"),
    )

    /** Jede Kotlin-Datei des Hauptquelltexts, ueber alle Module. */
    fun dateien(): List<File> = kt(wurzeln)

    /** Jede Kotlin-Datei des Testquelltexts. */
    fun testDateien(): List<File> = kt(testWurzeln)

    /**
     * Eine einzelne Datei ueber ihren Paketpfad, z. B. `org/biglau/data/Model.kt`.
     *
     * Wer stattdessen `File("src/main/java/org/biglau/data/Model.kt")` schreibt, bindet die
     * Regel an ein Modul. Hier faellt ein Umzug hoechstens laut auf.
     */
    fun datei(pfad: String): File =
        (wurzeln + testWurzeln).map { File(it, pfad) }.firstOrNull { it.isFile }
            ?: throw AssertionError("Quelltext nicht gefunden: $pfad")

    private fun kt(orte: List<File>): List<File> =
        orte.flatMap { it.walkTopDown().filter { datei -> datei.extension == "kt" } }
}
