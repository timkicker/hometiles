package org.biglau.res

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jeder Text muss irgendwo stehen.
 *
 * Anlass: „Diesen Eintrag löschen" lag übersetzt in beiden Sprachen und wurde von keiner
 * Zeile jemals angezeigt - das Löschen eines einzelnen Anrufs war nur durch langes Halten
 * erreichbar, und das stand nirgends. Ein Text ohne Fundstelle ist entweder ein Rest oder
 * ein Versprechen, das die Oberfläche nicht einlöst; beides fällt sonst niemandem auf,
 * weil ein ungenutzter Text nichts kaputt macht - er fehlt nur da, wo er hingehörte.
 *
 * Geprüft wird gegen den englischen Satz, weil beide Sprachen dieselben Schlüssel führen
 * (siehe [TranslationsTest]); ein deutscher Rest fiele dort auf.
 */
class UnusedStringsTest {

    private val res = File("src/main/res")

    /**
     * Namen, die es zu Recht ohne Fundstelle im Quelltext gibt.
     *
     * `app_name` steht im Manifest, nicht im Kotlin-Code - und wird dort über `@string/`
     * gefunden, weshalb die Liste heute leer bleiben würde. Sie steht trotzdem hier, damit
     * ein künftiger Sonderfall benannt werden muss statt die Regel zu lockern.
     */
    private val ohneFundstelle = emptySet<String>()

    private fun namen(tag: String): List<String> {
        val datei = File(res, "values/${if (tag == "plurals") "plurals" else "strings"}.xml")
        assertTrue("$datei fehlt", datei.exists())
        return Regex("<$tag name=\"([^\"]+)\"").findAll(datei.readText())
            .map { it.groupValues[1] }.toList()
    }

    /** Alles, was auf einen Ressourcennamen zeigt: `R.string.x`, `R.plurals.x`, `@string/x`. */
    private fun verwendet(): Set<String> {
        val treffer = mutableSetOf<String>()
        val zeiger = Regex("""R\.(?:string|plurals)\.([A-Za-z0-9_]+)|@(?:string|plurals)/([A-Za-z0-9_]+)""")
        (Quelltext.wurzeln + res + File("src/main/AndroidManifest.xml")).forEach { ort ->
            ort.walkTopDown().filter { it.isFile }.forEach { datei ->
                zeiger.findAll(datei.readText()).forEach {
                    treffer += it.groupValues[1].ifEmpty { it.groupValues[2] }
                }
            }
        }
        return treffer
    }

    @Test
    fun `kein Text steht ungenutzt herum`() {
        val benutzt = verwendet()
        val verwaist = (namen("string") + namen("plurals"))
            .filterNot { it in benutzt || it in ohneFundstelle }
        assertEquals(
            "Diese Texte zeigt niemand an - entweder fehlt die Stelle, an der sie stehen " +
                "sollten, oder sie sind ein Rest und gehören gelöscht: $verwaist",
            emptyList<String>(),
            verwaist,
        )
    }

    @Test
    fun `die Regel findet einen erfundenen Rest`() {
        // Gegenprobe: ohne sie würde ein kaputter Suchausdruck alles durchwinken.
        val benutzt = verwendet()
        assertTrue("erfundener Name darf nicht als benutzt gelten", "biglau_gibt_es_nicht" !in benutzt)
    }
}
