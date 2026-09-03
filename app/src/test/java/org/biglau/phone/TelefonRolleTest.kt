package org.biglau.phone

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Was BigLau mitbringen muss, um überhaupt als Telefon-App wählbar zu sein.
 *
 * Nebenan prüft `RoleRequestTest`, dass der Rollendialog richtig geöffnet wird — er war
 * einmal wirkungslos, weil Android den Aufrufer über `startActivityForResult` liest. Diese
 * Regel prüft die andere Hälfte: **dass es überhaupt etwas zu wählen gibt.**
 *
 * Android verlangt für die Telefon-Rolle einen `ACTION_DIAL`-Einstieg; ohne ihn steht BigLau
 * in der Auswahl der Standard-Telefon-App gar nicht erst. Für den eigenen Anrufbildschirm
 * kommt ein `InCallService` dazu, und zwar mit der Berechtigung und dem Merkmal
 * `IN_CALL_SERVICE_UI` — fehlt das Merkmal, zeigt Android weiter seine eigene Ansicht, ohne
 * ein Wort dazu.
 *
 * Genau wie bei den vier SMS-Komponenten stand das bis zum 3.9.2026 nur im Manifest und in
 * einem Kommentar daneben. Ein Umbau, der eine Zeile davon verliert, fällt sonst erst
 * jemandem auf, der die Rolle vergeben will und BigLau nicht findet.
 */
class TelefonRolleTest {

    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun `es gibt einen Einstieg fuer ACTION_DIAL`() {
        val dialer = manifest.substringAfter("\".phone.DialerActivity\"").substringBefore("</activity>")
        assertTrue("ACTION_DIAL fehlt - BigLau steht dann nicht zur Wahl", "android.intent.action.DIAL" in dialer)
        assertTrue("der Einstieg ist nicht exportiert - Android sieht ihn dann nicht", "android:exported=\"true\"" in dialer)
        assertTrue("tel: fehlt - ein Anruf aus einer anderen App landet nirgends", "\"tel\"" in dialer)
    }

    @Test
    fun `der eigene Anrufbildschirm ist als solcher angemeldet`() {
        // Ab dem Namensattribut, nicht ab dem ersten Vorkommen: einen Absatz weiter oben
        // *erwähnt* ein Kommentar den Dienst, und der Schnitt landete dort - die Regel las
        // den Nachbardienst und meldete einen Fehler, den es nicht gab.
        val service = manifest.substringAfter("\".phone.BigInCallService\"").substringBefore("</service>")
        assertTrue("InCallService-Filter fehlt", "android.telecom.InCallService" in service)
        assertTrue("BIND_INCALL_SERVICE fehlt", "android.permission.BIND_INCALL_SERVICE" in service)
        assertTrue(
            "IN_CALL_SERVICE_UI fehlt - Android zeigt dann seine eigene Ansicht, wortlos",
            "android.telecom.IN_CALL_SERVICE_UI" in service && "android:value=\"true\"" in service,
        )
    }

    @Test
    fun `der Startbildschirm meldet sich als Startbildschirm an`() {
        val haupt = manifest.substringAfter("\".MainActivity\"").substringBefore("</activity>")
        listOf(
            "android.intent.action.MAIN",
            "android.intent.category.HOME",
            "android.intent.category.DEFAULT",
        ).forEach { assertTrue("$it fehlt - ohne das ist BigLau kein Startbildschirm", it in haupt) }
    }
}
