package org.biglau.security

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * „Fertig" ohne eine einzige Ziffer ist kein Knopf.
 *
 * Am 04.09.2026 am Emulator: die App-Sperre stand da, kein Punkt gefüllt, ein Tipp auf
 * „Fertig" — und die Antwort war „Diese PIN stimmt nicht." Das stimmt nicht: eingegeben war
 * gar nichts. Wer die Meldung ernst nimmt, sucht den Fehler bei sich und probiert seine
 * richtige PIN gar nicht erst.
 *
 * Derselbe Griff wie beim Senden ohne Text (`SmsActivity`) und beim Anrufen ohne Nummer
 * (`DialerActivity`): keine Farbe, kein `onClick`. Was nichts tun kann, sieht auch nicht
 * danach aus — und sagt vor allem nichts Falsches.
 *
 * Bei einer **zu kurzen** Eingabe bleibt es beim Knopf: drei Ziffern sind eine Eingabe, und
 * „stimmt nicht" ist dann wahr. Die Grenze liegt bei null, nicht bei [org.biglau.security.Pin.MIN_LENGTH]
 * — sonst verriete der Knopf, wie lang eine PIN mindestens ist, an jeden, der das Telefon
 * in der Hand hat.
 */
class LeererPinKnopfTest {

    private val knopf = Quelltext.ausschnitt(
        Quelltext.ohneKommentare("org/biglau/ui/PinGate.kt"),
        von = "BigRow(\n            label = confirmLabel,",
        bis = "\n        )",
    )

    @Test
    fun `ohne ziffer keine farbe`() {
        assertTrue(
            "Der Fertig-Knopf traegt immer die Akzentfarbe: $knopf\n" +
                "Ohne Eingabe kann er nichts tun; dann darf er auch nicht danach aussehen.",
            "surface = if (entered.isEmpty())" in knopf,
        )
    }

    @Test
    fun `ohne ziffer kein onClick`() {
        assertTrue(
            "Der Fertig-Knopf laesst sich auch ohne Eingabe druecken: $knopf\n" +
                "Er antwortet dann \"Diese PIN stimmt nicht\" auf eine PIN, die niemand " +
                "eingegeben hat.",
            "onClick = if (entered.isEmpty())" in knopf && "null" in knopf,
        )
    }
}
