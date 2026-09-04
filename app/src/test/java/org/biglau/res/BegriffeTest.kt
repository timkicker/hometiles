package org.biglau.res

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dieselbe Sache heisst in einer Sprache dasselbe.
 *
 * PLAN.md 10.4. Fuenf Sprachen sind an einem Abend entstanden, und die Gefahr dabei ist
 * nicht der grobe Fehler - den faengt `TranslationsTest` -, sondern die **Uneinigkeit**: der
 * Lautsprecher heisst einmal so und einmal anders, und wer die App nicht am Stueck liest,
 * haelt zwei Namen fuer zwei Dinge.
 *
 * Ich spreche drei der fuenf Sprachen nicht. Was ein Werkzeug kann, ist dies: **zwei Texte
 * vergleichen, die dieselbe Sache meinen**, und melden, wo eine Sprache sich uneins ist.
 * Beurteilen, ob eine Formulierung gut klingt, kann es nicht, und diese Regel tut auch nicht
 * so. Was sie am 04.09.2026 beim ersten Lauf gefunden hat, stand in **drei von vier Faellen
 * im Englischen**, also in der Sprache, die ich am besten kann:
 *
 * * `Loudspeaker` und `Speaker` fuer denselben Knopf. Alle vier Uebersetzungen hatten
 *   laengst ein Wort dafuer; nur die Vorgabe hatte zwei.
 * * `Jump to a screen` und `Jump to screen` fuer dieselbe Aktion - und im Deutschen dahinter
 *   `Bildschirm` gegen `Screen`, also zwei Woerter fuer dasselbe in einer App, in der
 *   `Screen` sonst durchgehend die Kachelseite meint.
 * * Das Italienische sagte `Avanti` fuer `More` **und** fuer `Next`. Die drei anderen
 *   Sprachen unterscheiden beides; hier war die Uebersetzung ungenauer als die Vorlage.
 */
class BegriffeTest {

    private fun texte(sprache: String): Map<String, String> = buildMap {
        Quelltext.resWurzeln.map { File(it, "$sprache/strings.xml") }.filter { it.isFile }
            .forEach { datei ->
                Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
                    .findAll(datei.readText())
                    .forEach { put(it.groupValues[1], it.groupValues[2].trim()) }
            }
    }

    private val vorgabe = texte("values")

    /**
     * Wo eine Sprache feiner ist als das Englische, mit dem Grund.
     *
     * Diese Ausnahmen sind keine Nachlaessigkeit, sondern Sprachen, die einen Unterschied
     * machen, den das Englische nicht macht. Jede steht mit ihrem Grund da, damit die
     * naechste nicht einfach dazugeschrieben wird.
     */
    private val darfAbweichen = mapOf(
        "Home" to "Der Startbildschirm und das Haus-Symbol teilen sich im Englischen ein " +
            "Wort. Jede andere Sprache trennt das, und sie hat recht.",
        "Time" to "Die Uhrzeit und die Gruppe der Zeit-Symbole. Deutsch trennt Uhrzeit " +
            "und Zeit, Englisch nicht.",
        "none" to "Grammatisches Geschlecht: `keine Meldung`, `keiner`. Das haengt am " +
            "Bezugswort und laesst sich nicht gleichmachen.",
    )

    @Test
    fun `gleicher englischer Text, gleiche Uebersetzung`() {
        assertTrue("Keine Texte gelesen, dann misst diese Regel nichts", vorgabe.size > 100)
        val mehrfach = vorgabe.entries.groupBy({ it.value }, { it.key })
            .filterValues { it.size > 1 }
            .filterKeys { it !in darfAbweichen }
        val funde = Quelltext.uebersetzungen().flatMap { sprache ->
            val d = texte(sprache)
            mehrfach.mapNotNull { (englisch, schluessel) ->
                val werte = schluessel.mapNotNull { d[it] }.toSet()
                if (werte.size > 1) "$sprache: \"$englisch\" -> $werte ($schluessel)" else null
            }
        }.sorted()
        assertEquals(
            "Hier heisst dieselbe Sache in einer Sprache zweierlei. Entweder ist die " +
                "Uebersetzung uneins, oder der englische Text ist zweideutig und gehoert " +
                "praeziser - dann gehoert er geaendert und nicht die Ausnahmeliste:\n" +
                funde.joinToString("\n"),
            emptyList<String>(),
            funde,
        )
    }

    /**
     * Und die Gegenrichtung, die mehr gefunden hat.
     *
     * Zwei verschiedene englische Texte, in einer Sprache zu einem verschmolzen: entweder
     * sagt das Englische zweimal dasselbe mit verschiedenen Worten, oder die Uebersetzung
     * hat einen Unterschied verloren. Beides gehoert angesehen.
     */
    private val darfVerschmelzen = mapOf(
        ("values-de" to "Anrufliste") to
            "Der Bildschirm und die Ueberschrift seiner Einstellungen. Englisch sagt " +
            "`Call log` und `Call list`; im Deutschen ist beides die Anrufliste.",
    )

    @Test
    fun `verschiedene englische Texte bleiben verschieden`() {
        val funde = Quelltext.uebersetzungen().flatMap { sprache ->
            texte(sprache).entries.groupBy({ it.value }, { it.key })
                .filterValues { it.size > 1 }
                .filterNot { (text, _) -> (sprache to text) in darfVerschmelzen }
                .mapNotNull { (text, schluessel) ->
                    val englisch = schluessel.mapNotNull { vorgabe[it] }.toSet()
                    if (englisch.size > 1) "$sprache: \"$text\" <- $englisch" else null
                }
        }.sorted()
        assertEquals(
            "Hier fasst eine Sprache zwei verschiedene englische Texte zu einem zusammen. " +
                "Entweder sagt das Englische dasselbe zweimal verschieden - dann gehoert es " +
                "vereinheitlicht -, oder die Uebersetzung hat einen Unterschied verloren:\n" +
                funde.joinToString("\n"),
            emptyList<String>(),
            funde,
        )
    }
}
