package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Jeder Bildschirm, der nach einer Berechtigung fragt, kennt den Fall „Android fragt nicht
 * mehr".
 *
 * Nach der zweiten Ablehnung kehrt `requestPermissions` sofort zurück, ohne dass etwas zu
 * sehen wäre. Ein Knopf „Jetzt fragen" tut dann nichts — und die Kachel sagt weiter, man
 * solle ihn antippen. `PermissionGate` löst das seit jeher, und sein eigener Kommentar nennt
 * die Falle beim Namen: „Genau dieser stumme Knopf ist die Falle, die hier vermieden wird."
 *
 * Am 04.09.2026 am Emulator nachgestellt: die Empfangs-Kachel angetippt, zweimal abgelehnt,
 * dann wieder „Jetzt fragen" — der Bildschirm schloss sich, sonst nichts. Endlos wiederholbar.
 * Der Erklärbildschirm zum Empfang war der eine, der `PermissionState.blocked` nicht benutzt
 * hat; die Regel dafür stand nirgends, sie war nur an vier von fünf Stellen befolgt.
 *
 * Die Regel zählt die Fragesteller: wer `RequestPermission` startet, muss auch wissen, wann
 * das nichts mehr bewirkt.
 */
class StummerBerechtigungsknopfTest {

    /** Datei → warum sie eine Berechtigung erfragt. */
    private val frager = mapOf(
        "org/biglau/MainActivity.kt" to "die Empfangs-Kachel",
        "org/biglau/contacts/ContactsActivity.kt" to "die Kontaktliste",
        "org/biglau/phone/DialerActivity.kt" to "die Anrufliste",
        "org/biglau/sms/SmsActivity.kt" to "die Nachrichtenliste",
    )

    @Test
    fun `wer fragt kennt auch das nein fuer immer`() {
        val ohne = frager.filter { (pfad, _) ->
            val text = Quelltext.ohneKommentare(pfad)
            "RequestPermission()" in text && "PermissionState.blocked" !in text
        }
        assertEquals(
            "Diese Bildschirme fragen nach einer Berechtigung, kennen aber den Fall " +
                "\"Android fragt nicht mehr\" nicht: " + ohne.values.joinToString(", ") +
                ". Der Knopf tut dann nichts, und es gibt keinen Weg mehr heraus.",
            emptyMap<String, String>(),
            ohne,
        )
    }
}
