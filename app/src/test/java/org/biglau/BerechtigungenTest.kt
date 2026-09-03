package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jede Berechtigung im Manifest wird auch gebraucht.
 *
 * Eine Berechtigung ist eine Zusage nach aussen: sie steht in der App-Info des Telefons als
 * „Kann: …", und sie ist das Erste, was jemand ansieht, der einem Startbildschirm seine
 * Kontakte und SMS anvertrauen soll. Eine, die nie benutzt wird, ist damit eine Behauptung
 * über Fähigkeiten, die es nicht gibt — und in einer App, die mit „ohne Konto, ohne Werbung,
 * ohne Netzwerkzugriff" wirbt, ausgerechnet die falsche Stelle zum Schlampen.
 *
 * Am 3.9.2026 waren es zwei von sechzehn:
 *
 * * `EXPAND_STATUS_BAR` — nirgends benutzt, und `PLAN.md` 6 sagt zum Herunterziehen der
 *   Statusleiste ausdrücklich „**nicht anbieten**". Die Berechtigung stand da, das Verbot
 *   auch.
 * * `SET_WALLPAPER` — nirgends benutzt, und es gibt gar keine Hintergrund**bilder**: ein
 *   Bildschirm trägt das Thema oder eine Farbe. Die Berechtigung gehörte zu einer Funktion,
 *   die nie gebaut wurde.
 */
class BerechtigungenTest {

    /** Berechtigung → warum sie nötig ist, obwohl kein Quelltext sie beim Namen nennt. */
    private val ohneNennungNoetig = mapOf(
        "RECEIVE_SMS" to
            "Pflicht für die Standard-SMS-Rolle. Ohne sie erscheint BigLau in der Auswahl " +
            "gar nicht; benutzt wird sie vom System, nicht von uns.",
        "USE_FULL_SCREEN_INTENT" to
            "trägt `setFullScreenIntent` in SmsNotifications. Ab Android 14 muss sie " +
            "angemeldet sein, sonst erscheint die Meldung still als gewöhnliche.",
    )

    private val manifest = File("src/main/AndroidManifest.xml").readText()

    private fun rechte(): List<String> =
        Regex("""uses-permission android:name="android\.permission\.(\w+)"""")
            .findAll(manifest).map { it.groupValues[1] }.toList()

    @Test
    fun `keine Berechtigung ohne Verwendung`() {
        val quelle = Quelltext.dateien().joinToString("\n") { it.readText() }
        val unbenutzt = rechte()
            .filterNot { it in quelle }
            .filterNot { it in ohneNennungNoetig }
            .sorted()
        assertEquals(
            "Diese Berechtigung steht im Manifest, wird aber nirgends benutzt. Sie steht " +
                "in der App-Info des Nutzers als „Kann: …\" - entweder weg damit, oder mit " +
                "Grund in die Liste in BerechtigungenTest.",
            emptyList<String>(),
            unbenutzt,
        )
    }

    @Test
    fun `jede Ausnahme nennt ihren Grund`() {
        ohneNennungNoetig.forEach { (name, grund) ->
            assertTrue("$name: Grund fehlt oder ist zu knapp", grund.length > 40)
        }
        val unbekannt = ohneNennungNoetig.keys.filterNot { it in rechte() }
        assertEquals(
            "In der Liste steht eine Berechtigung, die es im Manifest nicht mehr gibt - " +
                "dann kann die Ausnahme auch weg.",
            emptyList<String>(),
            unbekannt,
        )
    }

    /** Und die Zahl selbst ist ein Merkposten: sie soll nicht unbemerkt wachsen. */
    @Test
    fun `es sind vierzehn Berechtigungen`() {
        assertEquals(
            "Die Zahl der Berechtigungen hat sich geändert. Jede einzelne ist eine Zusage " +
                "nach aussen - das gehört gesehen, nicht nur übersetzt.",
            14,
            rechte().size,
        )
    }
}
