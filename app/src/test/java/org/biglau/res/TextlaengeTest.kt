package org.biglau.res

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Kein Text laenger als 160 Zeichen.
 *
 * Der Nutzer hat am 04.09.2026 gesagt, die Texte seien zu lang. Sie waren es: der laengste
 * hatte 240 Zeichen und stand auf einem Bildschirm, der nicht scrollt. Bei Schriftgroesse
 * 135 Prozent sind 160 Zeichen schon sieben Zeilen; was darueber hinausgeht, liest niemand
 * mehr, und wo kein Scrollen ist, faellt es stumm unten heraus.
 *
 * Beim Kuerzen ist zweimal derselbe Fehler passiert, beide Male hat ihn eine Regel gefangen:
 * Als erstes faellt das **Wo** weg. "Geben Sie die Rolle zurueck" statt "in den
 * Einstellungen zurueck", "geben Sie dem Screen mehr Felder" statt "in den Einstellungen
 * mehr Felder". Der Satz wird kuerzer und der Ausweg unbrauchbar. Deshalb steht diese Regel
 * neben [org.biglau.tiles.KeinPlatzTest] und [org.biglau.sms.MmsHintTest], die genau das
 * pruefen: kuerzen darf, wer den Weg stehen laesst.
 *
 * 160 ist der laengste Text, der nach dem Durchgang noch dastand, nicht eine gewuenschte
 * Zahl. Die Grenze ist eine Sperrklinke: sie haelt fest, was erreicht ist. Wer sie
 * herunterschraubt, muss vorher kuerzen.
 *
 * Gilt fuer beide Sprachen. Deutsch ist regelmaessig laenger als Englisch, deshalb eine
 * gemeinsame Zahl statt zweier: sie bindet dort, wo es schwerer ist.
 *
 * Was diese Regel **nicht** misst, sind Zeilen. "Andere Apps brauchen
 * Benachrichtigungszugriff" ist zehn Zeichen kuerzer als "Andere Apps brauchen Zugriff auf
 * die Benachrichtigungen" und brauchte am Emulator eine Zeile **mehr**: das Wort mit 24
 * Buchstaben passte in keine angefangene Zeile und liess davor eine halbe leer. Kuerzer
 * gezaehlt, laenger gesetzt. Deshalb bleibt der Blick aufs Geraet Teil des Kuerzens; die
 * Zahl hier findet nur die Texte, bei denen er sich lohnt.
 */
class TextlaengeTest {

    private val grenze = 160

    @Test
    fun `kein Text ist laenger als die Grenze`() {
        val zulang = Quelltext.allTexts().flatMap { datei ->
            Regex("""<(?:string|item)[^>]*?(?:name="([^"]*)")?[^>]*>(.*?)</(?:string|item)>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(datei.readText())
                .map { it.groupValues[1] to it.groupValues[2] }
                .filter { (_, text) -> text.length > grenze }
                .map { (name, text) ->
                    "${datei.parentFile.name}/$name: ${text.length} Zeichen"
                }
        }
        assertEquals(
            "Zu lang. Kuerzen, aber das Wo stehen lassen: ein Satz ohne den Weg ist kein " +
                "kuerzerer Satz, sondern ein nutzloser",
            emptyList<String>(),
            zulang,
        )
    }

    @Test
    fun `die Regel wuerde einen zu langen Text finden`() {
        // Gegenprobe: sonst haette ein falscher Suchausdruck alles durchgewunken.
        val probe = "<string name=\"probe\">" + "x".repeat(grenze + 1) + "</string>"
        val gefunden = Regex("""<string[^>]*>(.*?)</string>""").find(probe)
        assertEquals(true, gefunden != null && gefunden.groupValues[1].length > grenze)
    }
}
