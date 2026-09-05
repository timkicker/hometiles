package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kein Satz troestet mit einem Weg, den die Rolle gerade zugemacht hat.
 *
 * Zwei Texte sagten „die Nachrichten-App des Telefons kann sie weiterhin oeffnen" - der
 * Hinweis, wenn eine Bildnachricht ankommt, und die Erklaerung auf der
 * Nachrichten-Einstellungsseite. Beide waren als Warnung **vor** der Rollenvergabe
 * geschrieben und stimmten genau bis dahin.
 *
 * Eine MMS holt nur die Standard-SMS-App: der WAP-Push geht nur an sie, der Download auch.
 * Sobald BigLau die Rolle haelt und sie nicht holt, kann es auch keine andere. Ein Trost,
 * der nach der Entscheidung falsch wird, ist schlimmer als keiner - der Nutzer sucht dann
 * eine App, die es nicht mehr geben kann.
 *
 * Beide Stellen haben jetzt zwei Fassungen, und die Regel haelt fest, dass die Wahl an der
 * Rolle haengt und nicht an einer Vermutung.
 */
class RolleImTextTest {

    private val seite = Quelltext.withoutComments("org/biglau/sms/MessagesSettingsList.kt")

    @Test
    fun `die MMS-Erklaerung richtet sich nach der Rolle`() {
        assertTrue(
            "Es gibt nur noch eine Fassung der MMS-Erklaerung - dann stimmt sie vor oder " +
                "nach der Rollenvergabe, aber nicht beides.",
            "R.string.sms_no_mms_default" in seite && "R.string.sms_no_mms" in seite,
        )
        val ab = seite.indexOf("R.string.sms_no_mms_default")
        assertTrue(
            "Die Wahl haengt nicht an der Rolle.",
            "holdsSmsRole" in seite.substring(maxOf(0, ab - 120), ab),
        )
    }

    /** Und der Filterhinweis genauso - der hatte es schon vorher richtig. */
    @Test
    fun `der Filterhinweis richtet sich nach der Rolle`() {
        assertTrue(
            "Der Filterhinweis hat nur noch eine Fassung.",
            "R.string.sms_filter_hint_default" in seite && "R.string.sms_filter_hint" in seite,
        )
    }

    /**
     * Und kein Text fuer den Fall „BigLau haelt die Rolle" wiederholt den Trost von vorher.
     *
     * Die erste Fassung dieser Regel nagelte den Satz fest („die Nachrichten-App des
     * Telefons kann sie weiterhin oeffnen"). Damit hing sie am Wortlaut: eine bessere
     * Formulierung desselben falschen Trostes waere durchgegangen, und eine harmlose
     * Umformulierung des **richtigen** haette sie rot gemacht.
     *
     * So, wie der Fehler entstanden ist, entsteht er wieder: derselbe Satz stand in beiden
     * Fassungen. Geprueft wird deshalb, dass der Schluss - was nach dem Gedankenstrich
     * steht, sonst der letzte Satz - sich unterscheidet. Was in einer Fassung stimmt, darf
     * in der anderen nicht wortgleich wiederkehren.
     */
    @Test
    fun `die Fassung fuer die gehaltene Rolle sagt etwas anderes`() {
        listOf("values", "values-de").forEach { sprache ->
            val vorher = schluss(sprache, "sms_no_mms")
            assertTrue(
                "$sprache/sms_no_mms hat keinen Schluss mehr - dann steht dort nur das " +
                    "Problem.",
                vorher.isNotBlank(),
            )
            listOf("sms_no_mms_default", "mms_arrived_body").forEach { name ->
                assertEquals(
                    "$sprache/$name endet wortgleich wie sms_no_mms. Der Satz stimmt nur, " +
                        "solange eine **andere** App die Rolle haelt; hier haelt BigLau sie, " +
                        "und der Nutzer sucht dann eine App, die es nicht mehr geben kann.",
                    false,
                    schluss(sprache, name) == vorher,
                )
            }
        }
    }

    /**
     * Der Schluss eines Textes: was nach dem Gedankenstrich steht, sonst der letzte Satz.
     *
     * Dort steht in diesen Texten die Folgerung - der Teil, der vor und nach der
     * Rollenvergabe verschieden ausfaellt.
     */
    private fun schluss(sprache: String, name: String): String {
        val wert = Quelltext.textValue(name, sprache)
        val nachStrich = wert.substringAfterLast("\u2014", "")
        if (nachStrich.isNotBlank()) return nachStrich.trim()
        val saetze = wert.split(". ").filter { it.isNotBlank() }
        return if (saetze.size < 2) "" else saetze.last().trim()
    }
}
