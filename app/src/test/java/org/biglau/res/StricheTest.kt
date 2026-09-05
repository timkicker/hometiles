package org.biglau.res

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Keine Gedankenstriche, keine Mittelpunkte in den Texten.
 *
 * Der lange Strich und der Trennpunkt sind das Erkennungszeichen maschinengeschriebener
 * Texte. Der Nutzer hat es am 04.09.2026 so gesagt, und es stimmt auch fuer sich: ein Satz,
 * der einen Strich braucht, besteht meistens aus zwei Saetzen, die noch nicht getrennt sind.
 * Getrennt gelesen werden sie leichter, und wer gross schreibt, hat wenig Platz.
 *
 * Ersatz ist ein Komma, ein Doppelpunkt, ein Punkt oder eine zweite Zeile. Gezaehlt waren es
 * 51 lange Striche, 2 kurze und 16 Mittelpunkte.
 *
 * Der Bindestrich bleibt: "SMS-App" ist ein Wort, kein Einschub.
 */
class StricheTest {

    private val verboten = mapOf(
        '—' to "langer Gedankenstrich",
        '–' to "kurzer Gedankenstrich",
        '·' to "Mittelpunkt",
    )

    @Test
    fun `kein Text traegt einen Strich oder Mittelpunkt`() {
        val treffer = Quelltext.allTexts().flatMap { datei ->
            val inhalt = datei.readText()
            Regex("""<(?:string|item)[^>]*>(.*?)</(?:string|item)>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(inhalt)
                .flatMap { stelle ->
                    verboten.entries
                        .filter { (zeichen, _) -> zeichen in stelle.groupValues[1] }
                        .map { (_, name) ->
                            "${datei.parentFile.name}: $name in " +
                                stelle.groupValues[1].take(60)
                        }
                }
        }
        assertEquals(
            "Ersatz ist ein Komma, ein Doppelpunkt, ein Punkt oder eine zweite Zeile",
            emptyList<String>(),
            treffer,
        )
    }

    @Test
    fun `die Regel wuerde einen Strich finden`() {
        // Gegenprobe: sonst haette ein falscher Suchausdruck alles durchgewunken.
        val probe = "<string name=\"probe\">Ein Satz — mit Strich</string>"
        val gefunden = Regex("""<string[^>]*>(.*?)</string>""").find(probe)
        assertEquals(true, gefunden != null && '—' in gefunden.groupValues[1])
    }
}
