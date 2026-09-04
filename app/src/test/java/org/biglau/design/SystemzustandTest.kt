package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Was das System vergibt, wird beim Wiederkommen neu nachgesehen.
 *
 * Eine Berechtigung, die über `rememberLauncherForActivityResult` läuft, meldet sich zurück
 * — der Rückruf setzt einen Zustand, die Oberfläche zeichnet neu. Der
 * **Benachrichtigungszugriff** läuft anders: er wird in einer Systemeinstellung erteilt, und
 * von dort kommt nichts zurück.
 *
 * Bis zum 3.9.2026 stand deshalb in den Einstellungen
 * `accessGranted = NotificationRepository.isEnabled(this)` — einmal beim Zeichnen gelesen.
 * Wer den Zugriff erteilte und zurückkam, sah weiter „Benachrichtigungszugriff erteilen" und
 * hielt es für misslungen. Eine Zeile, die lügt, ist schlimmer als eine, die fehlt.
 *
 * `BigLauActivity.fortsetzungen` zählt jetzt die Rückkehr; wer solche Zustände liest, packt
 * sie in `remember(fortsetzungen.intValue) { … }`.
 */
class SystemzustandTest {

    private val basis = Quelltext.datei("org/biglau/ui/BigLauActivity.kt").readText()

    @Test
    fun `die gemeinsame Basis zaehlt die Rueckkehr`() {
        assertTrue("BigLauActivity kennt `fortsetzungen` nicht mehr", "fortsetzungen" in basis)
        val resume = Quelltext.ausschnitt(basis, "override fun onResume()", "\n    }")
        assertTrue(
            "onResume zählt nicht mehr hoch - dann merkt niemand, dass der Bildschirm " +
                "wieder vorn ist.",
            "fortsetzungen.intValue += 1" in resume,
        )
    }

    @Test
    fun `der Benachrichtigungszugriff wird beim Wiederkommen neu gelesen`() {
        val stelle = Quelltext.datei("org/biglau/settings/SettingsActivity.kt").readText()
            .let { Quelltext.ausschnitt(it, "accessGranted =") }
            .take(200)
        assertTrue(
            "accessGranted wird wieder einmalig gelesen. Wer den Zugriff erteilt und " +
                "zurückkommt, sieht dann weiter „erteilen\".",
            "remember(fortsetzungen.intValue)" in stelle,
        )
    }

    /**
     * Der Assistent fragt bei jeder Rückkehr neu.
     *
     * Er überspringt, was schon erledigt ist — aber er las den Zustand **einmal** und ließ
     * ihn nur von seinen beiden Dialogen auffrischen. Wer „Später" tippte, die Berechtigung
     * in den Systemeinstellungen erteilte und zurückkam, stand weiter vor demselben
     * Schritt. Derselbe Weg an der Prüfung vorbei ist der Rückfall bei der
     * Startbildschirm-Rolle: der geht über `startActivity` und liefert gar kein Ergebnis.
     */
    @Test
    fun `der Assistent liest seinen Zustand beim Wiederkommen neu`() {
        val stelle = Quelltext.datei("org/biglau/wizard/WizardActivity.kt").readText()
            .let { Quelltext.ausschnitt(it, "var state by") }
            .take(120)
        assertTrue(
            "Der Assistent liest seinen Zustand wieder nur einmal - dann steht ein " +
                "erledigter Schritt weiter da: $stelle",
            "remember(fortsetzungen.intValue)" in stelle,
        )
    }

    /**
     * Und der Zähler ist ein Zähler, kein `Boolean`: derselbe Wert zweimal gesetzt löst
     * keine Neuzeichnung aus.
     */
    @Test
    fun `es ist ein Zaehler und kein Schalter`() {
        assertEquals(
            "fortsetzungen muss ein Int-Zustand sein - ein Boolean, zweimal auf true " +
                "gesetzt, zeichnet nicht neu.",
            true,
            "mutableIntStateOf(0)" in basis,
        )
    }

    /**
     * Und die Regel gilt für die **ganze Klasse**, nicht für die eine Zeile.
     *
     * Drei Zustände vergibt das System und meldet nichts zurück: der Benachrichtigungszugriff,
     * die SMS-Rolle, die Telefon-Rolle. Wer einen davon in einer Oberfläche liest, muss ihn
     * beim Wiederkommen neu lesen — sonst steht dort eine Zeile, die lügt.
     *
     * Am 3.9.2026 waren es drei Stellen: die Einstellungen (Benachrichtigungszugriff), die
     * Nachrichtenliste und die Nachrichten-Einstellungen (beide SMS-Rolle). Die letzte
     * bekommt den Wert als Parameter — eine Seite für sich weiss nicht, dass sie wieder vorn
     * ist; die Activity weiss es.
     *
     * Ausgenommen sind Stellen **ohne Oberfläche**: ein Empfänger, der beim Auslösen prüft,
     * liest ohnehin frisch.
     */
    @Test
    fun `kein Systemzustand wird in einer Oberflaeche einmalig gelesen`() {
        // Stellen, die **beim Tippen** lesen und deshalb ohnehin frisch sind.
        //
        // Die erste Fassung nannte eine davon woertlich
        // (`"val absicht = if (DialerRole.held("`). Als am 03.09.2026 dieselbe Sache fuer
        // die SMS-Rolle dazukam, brach sie eine Zeile anders um - und die Regel meldete
        // einen Fehler, den es nicht gab. Zum wiederholten Mal hing eine Ausnahme an der
        // Form statt an der Sache. Gefragt ist: steht der Zugriff in einem Handler, der
        // beim Antippen laeuft? Dann ist er frisch, ganz gleich wie er geschrieben ist.
        val handler = Regex("""on[A-Z]\w* = \{""")
        val ohneOberflaeche = setOf(
            "MessageReminderReceiver.kt", "SmsRepository.kt",
            // Die Diagnose sammelt die Zeilen; der Aufruf **an sie** ist gesichert.
            "Diagnostics.kt", "WizardActivity.kt", "MainActivity.kt",
        )
        // Vier Zustaende vergibt das System: Benachrichtigungszugriff, SMS-Rolle,
        // Telefon-Rolle, Startbildschirm-Rolle.
        val muster = Regex(
            """(isDefaultSmsApp\(\)|NotificationRepository\.isEnabled\(|""" +
                """Diagnostics\.isDefaultHome\(|DialerRole\.held\()""",
        )
        val stellen = Quelltext.dateien()
            .filterNot { it.name in ohneOberflaeche }
            .flatMap { datei ->
                val zeilen = datei.readLines()
                zeilen.withIndex()
                    .filter { (_, zeile) ->
                        muster.containsMatchIn(zeile) && !zeile.trim().startsWith("*") &&
                            !zeile.trim().startsWith("//") && "fun " !in zeile
                    }
                    .filterNot { (i, _) ->
                        // Der Aufruf steht in `remember(fortsetzungen…)` - hier oder eine
                        // Zeile darueber, je nachdem wie es umbricht.
                        (i downTo maxOf(0, i - 2)).any {
                            "remember(fortsetzungen.intValue)" in zeilen[it]
                        }
                    }
                    .filterNot { (i, _) ->
                        // Nach oben bis zum naechsten Handler-Anfang - hoechstens zwoelf
                        // Zeilen, sonst waere jede Stelle irgendwann "in einem Handler".
                        (i downTo maxOf(0, i - 12)).any { handler.containsMatchIn(zeilen[it]) }
                    }
                    .map { (i, _) -> "${datei.name}:${i + 1}" }
            }
        assertEquals(
            "Hier wird ein Zustand gelesen, den das System vergibt - einmal, beim Zeichnen. " +
                "In `remember(fortsetzungen.intValue) { … }` packen, oder von der Activity " +
                "durchreichen lassen.",
            emptyList<String>(),
            stellen,
        )
    }

    /**
     * Und dasselbe für Berechtigungen, die der Nutzer in den **App-Einstellungen** erteilt.
     *
     * Eine Berechtigung, die über `rememberLauncherForActivityResult` erfragt wird, meldet
     * sich zurück. Ist sie dauerhaft verweigert, gibt es diesen Weg nicht mehr — dann
     * schickt BigLau den Nutzer in die App-Einstellungen (`Intents.appSettings`), und von
     * dort kommt nichts zurück.
     *
     * Vier Bildschirme taten das am 3.9.2026 und lasen ihren Zustand trotzdem nur einmal:
     * Kontakte, Nachrichten, Wähltastatur und der Kachel-Editor. Wer die Berechtigung dort
     * erteilte und zurückkam, las weiter „keine Berechtigung" — **auf einem Bildschirm, der
     * ihn selbst dorthin geschickt hatte.** Das ist die schlimmste Form einer Sackgasse: der
     * Ausweg ist beschildert, und am Ende steht dasselbe Schild noch einmal.
     */
    @Test
    fun `wer in die App-Einstellungen schickt, liest die Berechtigung neu`() {
        val stellen = Quelltext.dateien()
            .filter { "Intents.appSettings(" in it.readText() }
            .flatMap { datei ->
                datei.readLines().withIndex()
                    .filter { (_, zeile) ->
                        Regex("""remember \{ mutableStateOf\(\w+\.has\w*Permission\(\)\)""")
                            .containsMatchIn(zeile)
                    }
                    .map { (i, _) -> "${datei.name}:${i + 1}" }
            }
        assertEquals(
            "Dieser Bildschirm schickt in die App-Einstellungen, liest die Berechtigung " +
                "danach aber nicht neu. `remember(fortsetzungen.intValue) { … }` benutzen.",
            emptyList<String>(),
            stellen,
        )
    }
}
