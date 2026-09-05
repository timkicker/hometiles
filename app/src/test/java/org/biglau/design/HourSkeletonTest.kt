package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ein Muster, das die Stunde **vorschreibt**, statt sie der Sprache zu überlassen.
 *
 * `DateFormat.getBestDateTimePattern` nimmt Bestandteile entgegen: `H` heisst „Stunde 0–23",
 * `j` heisst „die Stunde so, wie dieses Telefon sie schreibt". Die Anrufliste und die
 * Nachrichtenliste baten bis zum 3.9.2026 um `H` — und bekamen 24-Stunden-Zeit, auch auf
 * einem Gerät, das überall sonst „5:39 PM" schreibt.
 *
 * Am Gerät gesehen: in der Liste stand „17:39", in der Kopfzeile dieselbe Minute als
 * „5:39 PM".
 */
class HourSkeletonTest {

    private val quellen = Quelltext.files()

    @Test
    fun `kein Muster erzwingt vierundzwanzig Stunden`() {
        val treffer = quellen.flatMap { datei ->
            datei.readLines().withIndex()
                .filter { (_, zeile) ->
                    Regex("""bestDatePattern\("[^"]*H""").containsMatchIn(zeile)
                }
                .map { (i, _) -> "${datei.name}:${i + 1}" }
        }
        assertEquals(
            "Ein Datumsmuster verlangt „H“ statt „j“ - das erzwingt 24-Stunden-Zeit, auch " +
                "wenn das Telefon AM/PM schreibt.",
            emptyList<String>(),
            treffer,
        )
    }
}
