package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jede Ablage wohnt in `core:system`.
 *
 * Acht Dateien heissen `…Repository`: sie holen etwas vom Gerät - Apps, Kontakte, Akku,
 * Empfang, Anrufliste, Verknüpfungen, Nachrichten, den laufenden Anruf. Sieben lagen in
 * `core:system`, eine in `:app`, und zwar ohne Grund: `InCallRepository` hielt unsere eigene
 * Dienstklasse `BigInCallService` im Typ, obwohl es davon nur `setMuted` und
 * `setAudioRoute` benutzt - beides vom System. Ein zu enger Typ, mehr war es nicht.
 *
 * Warum das zählt: eine Ablage, die in `:app` liegt, kann eine Activity anfassen, und dann
 * ist sie keine Ablage mehr. In `core:system` gibt es keine.
 *
 * Die erste Fassung dieser Regel sah nur **Dateinamen** und übersah damit
 * `NotificationRepository`, die mitten in `BigNotificationListener.kt` stand. Eine Regel, die
 * nur den Umschlag liest, findet nichts, was jemand hineingelegt hat. Jetzt sucht sie die
 * Deklaration.
 */
class AblagenTest {

    /** Ablage → warum sie doch in `:app` bleibt. */
    private val darfBleiben = mapOf(
        "NotificationRepository" to
            "`isEnabled` muss unseren eigenen Dienst beim Namen nennen: das System führt in " +
                "`enabled_notification_listeners` genau diese Klasse, keine Oberklasse tut es.",
    )

    private fun ablagenIn(dateien: Sequence<java.io.File>): Set<String> =
        dateien.flatMap { datei ->
            Regex("""^(?:internal )?(?:object|class) (\w*Repository)\b""", RegexOption.MULTILINE)
                .findAll(datei.readText())
                .map { it.groupValues[1] }
        }.toSortedSet()

    @Test
    fun `keine Ablage bleibt im Programmodul`() {
        val imApp = ablagenIn(
            Quelltext.appWurzel.walkTopDown().filter { it.extension == "kt" },
        )
        assertEquals(
            "Eine Ablage gehört nach core:system. Hängt sie an einer Klasse aus :app, ist " +
                "meist der Typ zu eng gewählt - siehe InCallRepository am 3.9.2026. Muss sie " +
                "doch bleiben, mit Grund in die Liste in AblagenTest.",
            darfBleiben.keys.toSortedSet(),
            imApp,
        )
    }

    @Test
    fun `jede Ausnahme nennt ihren Grund`() {
        darfBleiben.forEach { (name, grund) ->
            assertTrue("$name: Grund fehlt oder ist zu knapp", grund.length > 40)
        }
    }

    @Test
    fun `es sind noch alle neun da`() {
        val alle = ablagenIn(Quelltext.dateien().asSequence())
        assertEquals(
            "Eine Ablage ist verschwunden oder dazugekommen - dann diesen Merkposten " +
                "nachziehen, damit die Regel oben nicht ins Leere prüft.",
            sortedSetOf(
                "AppRepository", "BatteryRepository", "CallLogRepository",
                "ContactRepository", "InCallRepository", "NotificationRepository",
                "ShortcutRepository", "SignalRepository", "SmsRepository",
            ),
            alle,
        )
    }
}
