package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Kein Bereich kennt einen anderen — ausser dem Einstellungsbaum.
 *
 * `PLAN.md` 2.1 will die App in `feature:*`-Module schneiden, und die Regel dort lautet:
 * ein `feature:*` importiert nie ein anderes. Am 3.9.2026 waren es 58 Verstösse; 45 davon
 * waren gar keine, sondern Baukastenteile, die noch im Bildschirm-Paket lagen. Nach ihrem
 * Umzug nach `core:ui` und `core:model` blieben **13** übrig, und alle gehen vom
 * Einstellungsbaum aus.
 *
 * Der ist die eine offene Entwurfsfrage (siehe `STATUS.md`): entweder darf `feature:settings`
 * alle Bereiche kennen, oder jeder Bereich bringt seine eigene Einstellungsseite mit. Diese
 * Regel entscheidet sie **nicht**. Sie hält nur fest, was ohne Entscheidung gilt: alles
 * andere ist frei, und soll es bleiben.
 *
 * Was hier umfällt, ist meistens kein Fehler, sondern ein neues Teil am falschen Platz.
 * Gemeinsames gehört nach `core:*` — der Weg dorthin hat heute Nacht kein einziges Mal eine
 * Import-Zeile gekostet, weil das Paket bleibt und nur das Modul wechselt.
 */
class FeatureKantenTest {

    /** Bereich in `:app` → das Modul, in das er nach PLAN.md 2.1 gehört. */
    private val gehoertZu = mapOf(
        "tiles" to "home", "apps" to "home", "widgets" to "home", "shortcuts" to "home",
        "ui" to "home",
        "phone" to "phone", "contacts" to "phone",
        "sms" to "sms", "notify" to "sms",
        "settings" to "settings", "wizard" to "settings",
        "toggles" to "sos", "actions" to "sos", "safety" to "sos",
        // a11y, search, security, web, info sind Querschnitt und keinem Modul zugeordnet.
    )

    @Test
    fun `nur der einstellungsbaum kennt andere bereiche`() {
        // MainActivity ist die Hülle und darf jeden Bildschirm kennen.
        val kanten = Bereiche.kanten(ohne = setOf("MainActivity.kt"))
        val verstoesse = kanten.entries.flatMap { (paar, stellen) ->
            val von = gehoertZu[paar.first]
            val nach = gehoertZu[paar.second]
            if (von == null || nach == null || von == nach || von == "settings") {
                emptyList()
            } else {
                stellen.map { "$von -> $nach  $it" }
            }
        }.sorted()

        assertEquals(
            "Ein Bereich kennt einen anderen, der später ein eigenes Modul wird. " +
                "Gemeinsames gehört nach core:* — Paket behalten, Modul wechseln, dann " +
                "ändert sich keine Import-Zeile.",
            emptyList<String>(),
            verstoesse,
        )
    }

    /**
     * Und der Einstellungsbaum wächst nicht unbemerkt weiter. Die Zahl ist kein Ziel,
     * sondern ein Merkposten: wer sie erhöht, macht die offene Frage teurer.
     */
    @Test
    fun `die kanten des einstellungsbaums bleiben gezaehlt`() {
        val kanten = Bereiche.kanten(ohne = setOf("MainActivity.kt"))
        val ausSettings = kanten.entries
            .filter { (paar, _) -> gehoertZu[paar.first] == "settings" && gehoertZu[paar.second] != null && gehoertZu[paar.second] != "settings" }
            .flatMap { it.value }
            .map { it.substringAfter(": ") }
            .toSortedSet()

        assertEquals(
            "Der Einstellungsbaum kennt mehr oder weniger Bereiche als am 3.9.2026 " +
                "gezählt. Weniger ist gut - dann diese Liste kürzen. Mehr macht die " +
                "offene Frage aus STATUS.md teurer.",
            sortedSetOf(
                "AppDrawer", "AppLock", "WidgetHostController",
                "ConversationText", "MessageReminderReceiver", "NotificationRepository",
                "SmsFilter", "SmsNotifications", "SmsReminder",
                "SosActivity", "SosAlarm", "SosCountdown", "SosNumbers",
            ),
            ausSettings,
        )
    }
}
