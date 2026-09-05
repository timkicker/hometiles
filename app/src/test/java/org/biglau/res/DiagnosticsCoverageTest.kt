package org.biglau.res

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Diagnoseseite muss jedes gefährliche Recht nennen.
 *
 * Sie ist die Antwort auf „es geht nicht" — auf dem Gerät eines Menschen gibt es kein `adb`.
 * Ein Recht, das dort fehlt, ist eine fehlende Antwort: in der Nacht vom 02.09.2026 war die
 * Anrufliste zu sehen, aber nichts daraus zu löschen, weil `WRITE_CALL_LOG` fehlte — und die
 * Seite, die man dafür aufschlägt, zeigte genau diese Zeile nicht.
 */
class DiagnosticsCoverageTest {

    private val manifest = File("src/main/AndroidManifest.xml")
    private val seite = Quelltext.file("org/biglau/settings/Diagnostics.kt")

    /** Nur diese Gruppe wird zur Laufzeit erteilt; der Rest kommt beim Installieren. */
    private val gefaehrlich = setOf(
        "CALL_PHONE", "READ_CALL_LOG", "WRITE_CALL_LOG", "READ_PHONE_STATE",
        "READ_CONTACTS", "WRITE_CONTACTS", "SEND_SMS", "READ_SMS", "RECEIVE_SMS",
        "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION",
    )

    /**
     * Rechte, die die Seite nicht einzeln nennen muss.
     *
     * `ACCESS_COARSE_LOCATION` steht mit dem feinen zusammen in einer Zeile — beide werden
     * gemeinsam erfragt, und zwei Zeilen für dieselbe Frage sind auf drei Zoll Ballast.
     * `WRITE_CONTACTS` und `RECEIVE_SMS` hängen an einer Rolle, die die Seite ohnehin zeigt.
     *
     * `READ_PHONE_STATE` stand hier bis zum 04.09.2026 mit dem Grund, es werde „bewusst
     * nicht erteilt". Am Gerät nachgesehen war es erteilt — BigLau fragt eigens danach,
     * damit die Empfangsbalken etwas anzeigen. Der Grund war eine gemessene Tatsache, die
     * gealtert ist; die Zeile steht jetzt auf der Seite.
     */
    private val ausnahmen = mapOf(
        "ACCESS_COARSE_LOCATION" to "steht zusammen mit ACCESS_FINE_LOCATION in einer Zeile",
        "WRITE_CONTACTS" to "haengt an derselben Frage wie READ_CONTACTS",
        "RECEIVE_SMS" to "haengt an der SMS-Rolle, die die Seite als Standard-SMS-App zeigt",
    )

    private fun angemeldet(): Set<String> =
        Regex("""uses-permission android:name="android\.permission\.([A-Z_]+)"""")
            .findAll(manifest.readText())
            .map { it.groupValues[1] }
            .filter { it in gefaehrlich }
            .toSet()

    @Test
    fun `jedes gefaehrliche Recht steht auf der Diagnoseseite`() {
        val text = seite.readText()
        val fehlt = (angemeldet() - ausnahmen.keys)
            .filterNot { text.contains("Manifest.permission.$it") }
            .sorted()
        assertEquals(
            "Diese Rechte nennt die Diagnose nicht - wer wissen will, warum etwas nicht " +
                "geht, findet die Antwort dort nicht: $fehlt",
            emptyList<String>(),
            fehlt,
        )
    }

    @Test
    fun `jede Ausnahme nennt ihren Grund und gibt es wirklich`() {
        val vorhanden = angemeldet()
        ausnahmen.forEach { (name, grund) ->
            assertTrue("$name steht nicht mehr im Manifest", name in vorhanden)
            assertTrue("$name braucht einen Grund", grund.length > 20)
        }
    }
}
