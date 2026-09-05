package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein Zustand, den nur die Farbe oder ein Symbol traegt, ist beim Vorlesen nicht da.
 *
 * Zwei Listen hatten das: die Nachrichtenliste haengte ein blosses "(2)" an den Namen - eine
 * Zahl ohne Sache -, und die Anrufliste zeigte die Richtung als Pfeil ohne Namen, in der
 * einen Liste, in der die Richtung alles ist. Auf dem Startbildschirm sagte die Kachel
 * daneben laengst "2 sind ungelesen".
 *
 * Auswahl und Schalter haben dafuer ihre eigenen Angaben; das hier ist der dritte Fall, und
 * er hat keine feste Formel - deshalb ein Satz, den die Liste selbst mitbringt.
 */
class ZustandVorlesenTest {

    private val zeile = Quelltext.ohneKommentare("org/biglau/ui/BigRow.kt")

    @Test
    fun `eine Zeile kann ihren Zustand mitgeben`() {
        assertTrue(
            "BigRow kennt keinen freien Zustand - dann bleibt so etwas wie \"ungelesen\" " +
                "in der Farbe stecken.",
            zeile.contains("state: String? = null"),
        )
        assertTrue(
            "Der mitgegebene Zustand wird nicht angesagt.",
            zeile.contains("state != null -> stringResource(R.string.a11y_state"),
        )
    }

    @Test
    fun `die Nachrichtenliste sagt, was ungelesen ist`() {
        val liste = Quelltext.ohneKommentare("org/biglau/sms/SmsActivity.kt")
        assertTrue(
            "Die Nachrichtenliste nennt die ungelesenen nur als Zahl in Klammern.",
            liste.contains("a11y_unread"),
        )
    }

    @Test
    fun `die Anrufliste sagt die Richtung`() {
        val liste = Quelltext.ohneKommentare("org/biglau/phone/DialerActivity.kt")
        assertTrue(
            "Die Anrufliste zeigt die PadDirection nur als Pfeil. Ein Pfeil hat keinen Namen.",
            liste.contains("state = stringResource(callDirectionSpeech("),
        )
    }

    /**
     * Und die Woerter dazu stehen an einer Stelle.
     *
     * Der Einstellungsbaum listet dieselben Arten zum Ein- und Ausschalten, und zwar mit
     * Kategorienamen im Plural; die Zeile braucht das Adjektiv. Zwei Saetze, zwei Zwecke -
     * aber beide Zuordnungen in **einer** Datei, sonst laufen sie auseinander.
     */
    @Test
    fun `die Richtungswoerter stehen nur einmal`() {
        val stellen = Quelltext.dateien().filter {
            val t = it.readText()
            "CallDirection.MISSED -> R.string.call_type_missed" in t ||
                "CallDirection.MISSED -> R.string.call_dir_missed" in t
        }
        assertEquals(
            "Die Zuordnung PadDirection -> Wort steht mehr als einmal: " +
                stellen.map { it.name },
            1,
            stellen.size,
        )
    }

    /**
     * Und eine Zahl wird genau einmal gesagt.
     *
     * Die Nachrichtenliste malt die Zahl in Klammern hinter den Namen und sagt sie im
     * Zustand als Satz - vorgelesen kam sie zweimal. Die Anrufliste malt sie ebenfalls,
     * sagt sie aber nirgends sonst; dort traegt die Klammer sie.
     */
    @Test
    fun `die Nachrichtenliste sagt die Zahl nicht zweimal`() {
        val liste = Quelltext.ohneKommentare("org/biglau/sms/SmsActivity.kt")
        assertTrue(
            "Die Zeile gibt ihre Beschriftung samt Klammerzahl zum Vorlesen weiter, " +
                "obwohl der Zustand dieselbe Zahl schon als Satz sagt.",
            liste.contains("labelSpeech = thread.titleOr("),
        )
    }

    /**
     * Wer eine Nachricht geschrieben hat, steht in der Blase nirgends.
     *
     * Sie haengt links oder rechts und hat die eine oder die andere Farbe - beim Vorlesen
     * ist beides nichts. Man hoerte eine Reihe von Saetzen ohne Absender, und
     * "bin unterwegs" ist ohne Absender das Gegenteil.
     */
    @Test
    fun `jede Nachrichtenblase sagt, von wem sie ist`() {
        val liste = Quelltext.ohneKommentare("org/biglau/sms/SmsActivity.kt")
        assertTrue(
            "Die Nachrichtenblasen sagen nicht, ob sie empfangen oder gesendet sind.",
            liste.contains("a11y_message_in") && liste.contains("a11y_message_out"),
        )
        assertTrue(
            "Die PadDirection wird gebaut, aber nicht angesagt.",
            liste.contains("semantics(mergeDescendants = true)"),
        )
    }

    /**
     * Die Punktreihe sagt, wie viele Ziffern schon dastehen.
     *
     * Auf dem PIN-Bildschirm gab es sonst gar keine Rueckmeldung: BigLau macht absichtlich
     * keinen Ton, und ob eine Taste angekommen ist, war nur zu sehen. Die **Zahl** der
     * Ziffern verraet die PIN nicht - sie zu verschweigen hilft niemandem.
     *
     * Als `liveRegion`, weil es beim Tippen gesagt werden muss und nicht erst, wenn man die
     * Punkte antastet.
     */
    @Test
    fun `die Punktreihe sagt, wie viele Ziffern dastehen`() {
        val tastatur = Quelltext.ohneKommentare("org/biglau/ui/BigKeypad.kt")
        val punkte = Quelltext.ausschnitt(tastatur, "fun PinDots(")
        assertTrue(
            "Die Punktreihe sagt nichts. Wer sie nicht sieht, weiss auf dem " +
                "PIN-Bildschirm nicht, ob eine Taste angekommen ist.",
            punkte.contains("a11y_pin_digits") && punkte.contains("a11y_pin_empty"),
        )
        assertTrue(
            "Die Ansage kommt erst beim Antasten, nicht beim Tippen.",
            punkte.contains("liveRegion = LiveRegionMode.Polite"),
        )
    }
}
