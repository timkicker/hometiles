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
     * Der Hauptquelltext von `:app` allein.
     *
     * Eine Regel, die nur diesen braucht - etwa die Kreise zwischen den Bereichen -, soll
     * ihn nicht selbst hinschreiben muessen; genau das verbietet `QuelltextTest`.
     */
    val appWurzel: File = File("src/main/java")

    /**
     * Alle Wurzeln mit Hauptquelltext, relativ zum Arbeitsverzeichnis der Tests (`app/`).
     *
     * Neue Module gehoeren hier hinein. `QuelltextTest` faellt um, wenn eines fehlt.
     */
    val wurzeln: List<File> = listOf(
        appWurzel,
        File("../core/model/src/main/kotlin"),
        File("../core/data/src/main/kotlin"),
        File("../core/system/src/main/kotlin"),
        File("../core/ui/src/main/kotlin"),
    )

    /**
     * Die Ressourcenwurzeln aller Module.
     *
     * Beim Umzug von `:core:ui` sind die beiden Schriftdateien mitgewandert, und zwei
     * Regeln suchten sie weiter unter `app/src/main/res`. Sie fielen laut um - aber
     * dieselbe Liste, die den Quelltext zusammenhaelt, taugt auch dafuer.
     */
    val resWurzeln: List<File> = listOf(
        File("src/main/res"),
        File("../core/ui/src/main/res"),
    )

    /**
     * Alle Textdateien eines Sprachverzeichnisses, ueber alle Module.
     *
     * Zehn Regeln lasen `app/src/main/res/values/strings.xml` und meinten damit „alle
     * Texte". Solange die Texte nur dort liegen, stimmt das. Zieht ein Text mit seinem
     * Modul um - `:core:ui` bringt schon Schriftdateien mit -, dann pruefen sie ihn
     * lautlos nicht mehr. Deshalb fragen sie jetzt hier.
     *
     * Gibt es die Datei in einem Modul nicht, faellt sie weg statt zu stoeren: nicht jedes
     * Modul hat Texte, und schon gar nicht jede Sorte.
     */
    fun texte(verzeichnis: String, name: String = "strings.xml"): List<File> =
        resWurzeln.map { File(it, "$verzeichnis/$name") }.filter { it.isFile }

    /** Jede Textdatei ueber alle Module und beide Sprachen. */
    fun alleTexte(): List<File> =
        listOf("values", "values-de").flatMap { v ->
            listOf("strings.xml", "plurals.xml").flatMap { texte(v, it) }
        }

    /** Eine Ressource ueber ihren Pfad ab `res/`, z. B. `font/atkinson_bold.ttf`. */
    fun ressource(pfad: String): File =
        resWurzeln.map { File(it, pfad) }.firstOrNull { it.exists() }
            ?: throw AssertionError("Ressource nicht gefunden: $pfad")

    /** Die Wurzeln mit Testquelltext. */
    val testWurzeln: List<File> = listOf(
        File("src/test/java"),
        File("../core/system/src/test/kotlin"),
        File("../core/model/src/test/kotlin"),
        File("../core/ui/src/test/kotlin"),
    )

    /** Jede Kotlin-Datei des Hauptquelltexts, ueber alle Module. */
    fun dateien(): List<File> = kt(wurzeln)

    /** Jede Kotlin-Datei des Testquelltexts. */
    fun testDateien(): List<File> = kt(testWurzeln)

    /**
     * Eine einzelne Datei ueber ihren Paketpfad, z. B. `org/biglau/data/Model.kt`.
     *
     * Wer stattdessen den Pfad eines Moduls hinschreibt - `src/main/java/…` -, bindet die
     * Regel an dieses Modul. Hier faellt ein Umzug hoechstens laut auf, und meistens gar
     * nicht.
     */
    fun datei(pfad: String): File =
        // Ein Pfad, den es so schon gibt, wird genommen wie er ist: die Ressourcen liegen
        // weiter in :app, und dieselbe Regel liest oft beides - Quelltext und strings.xml.
        File(pfad).takeIf { it.isFile }
            ?: (wurzeln + testWurzeln).map { File(it, pfad) }.firstOrNull { it.isFile }
            ?: throw AssertionError("Quelltext nicht gefunden: $pfad")

    /**
     * Eine Datei ohne ihre Kommentarzeilen.
     *
     * Fuer Regeln, die mit `indexOf` oder `substringAfter` nach **Aufrufen** suchen. Ein
     * Kommentar, der denselben Namen nur erwaehnt, verschiebt sonst die gefundene Stelle.
     * Am 3.9.2026 nachgestellt: mit einem Kommentar ueber der Weiche `if (probe)` haette
     * die Regel, die den scharfen Notruf-Alarm in der Probe verhindert, einen echten
     * Fehler **durchgewinkt**.
     *
     * Nur ganze Kommentarzeilen fliegen heraus - ein `// ...` hinter Quelltext bleibt, denn
     * dort steht die Stelle ja wirklich.
     */
    fun ohneKommentare(pfad: String): String = datei(pfad)
        .readLines()
        .filterNot { it.trim().startsWith("//") || it.trim().startsWith("*") || it.trim().startsWith("/*") }
        .joinToString("\n")

    private fun kt(orte: List<File>): List<File> =
        orte.flatMap { it.walkTopDown().filter { datei -> datei.extension == "kt" } }
}
