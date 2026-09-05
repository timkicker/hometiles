package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Drei Rollen, ein Ort.
 *
 * BigLau kann drei Rollen des Systems halten: Startbildschirm, Telefon, Nachrichten. Zwei
 * davon standen im Einstellungsbaum, mit ihrem Zustand und einem Weg, ihn zu aendern. Die
 * dritte stand nur im Nachrichten-Bildschirm - und dort nur, solange BigLau sie **nicht**
 * hatte (`sms_not_default`). Wer sie hatte, erfuhr es nirgends, und von hier aus kam er
 * nicht mehr davon los.
 *
 * Aufgefallen am 03.09.2026, als BigLau die Rolle bekam: der Baum sagte „BigLau ist Ihre
 * Telefon-App" und schwieg ueber die Nachrichten.
 */
class DreiRollenTest {

    private val baum = Quelltext.ohneKommentare("org/biglau/settings/SettingsActivity.kt")

    @Test
    fun `alle drei Rollen stehen im Einstellungsbaum`() {
        listOf(
            "is_home" to "set_as_home",
            "is_dialer" to "set_as_dialer",
            "is_sms" to "set_as_sms",
        ).forEach { (gehalten, offen) ->
            assertTrue(
                "Die Rolle $gehalten fehlt im Einstellungsbaum - dann ist sie von hier aus " +
                    "weder zu sehen noch zu aendern.",
                "R.string.$gehalten" in baum && "R.string.$offen" in baum,
            )
        }
    }

    /**
     * Und jede sagt, wie es **jetzt** steht.
     *
     * Rollen vergibt das System, und von dort kommt nichts zurueck. Wird der Zustand einmal
     * gelesen, steht nach der Rueckkehr die Aufforderung da, die man gerade erfuellt hat -
     * siehe [SystemzustandTest].
     */
    @Test
    fun `jede Rolle wird beim Wiederkommen neu gelesen`() {
        listOf(
            "istStartbildschirm = remember",
            "istTelefonApp = remember",
            "istNachrichtenApp = remember",
        ).forEach { anfang ->
            val stelle = baum.indexOf(anfang)
            assertTrue("$anfang gibt es nicht mehr", stelle > 0)
            assertTrue(
                "$anfang liest den Zustand nur einmal - dann steht nach dem Erteilen " +
                    "weiter die Aufforderung da.",
                "resumes.intValue" in baum.substring(stelle, stelle + 60),
            )
        }
    }

    @Test
    fun `die sechs Saetze gibt es in beiden Sprachen`() {
        val fehlt = listOf(
            "is_home", "set_as_home", "is_dialer", "set_as_dialer", "is_sms", "set_as_sms",
        ).flatMap { name ->
            listOf("values", "values-de").filterNot { sprache ->
                Quelltext.texte(sprache).any { "name=\"$name\"" in it.readText() }
            }.map { "$name in $it" }
        }
        assertTrue("Ein Satz fehlt in einer Sprache: $fehlt", fehlt.isEmpty())
    }
}
