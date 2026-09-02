package org.biglau.phone

/** Warum die Anrufliste leer aussieht. */
enum class EmptyCallLog {
    /** Es gab wirklich keine Anrufe. */
    NO_CALLS,

    /** Es gab welche, aber alle sind von einer ausgeblendeten Art. */
    HIDDEN_BY_TYPE,

    /** Es gab welche, aber gerade steht der Filter auf „nur verpasste". */
    NO_MISSED,
}

/**
 * Eine leere Liste hat drei verschiedene Gründe, und sie dürfen nicht denselben Satz teilen.
 *
 * „Noch keine Anrufe." stand auch dann da, wenn sieben Anrufe im Protokoll lagen und der
 * Nutzer in den Einstellungen jede Art ausgeblendet hatte. Das ist keine Auskunft, sondern
 * eine Falschaussage - und aus ihr führte nichts heraus: mit leerer Liste verschwinden auch
 * der Hinweis und der Knopf darunter. Wer sich nicht erinnert, die Arten je angefasst zu
 * haben, hält das Telefon für kaputt.
 */
object CallLogEmpty {

    /** `null`, wenn etwas zu sehen ist. */
    fun reason(
        alle: List<CallGroup>,
        missedOnly: Boolean,
        allowed: Set<CallDirection>,
    ): EmptyCallLog? {
        val sichtbar = CallLogGrouping.visible(
            if (missedOnly) CallLogGrouping.onlyMissed(alle) else alle,
            allowed,
        )
        if (sichtbar.isNotEmpty()) return null
        if (alle.isEmpty()) return EmptyCallLog.NO_CALLS
        // Die Reihenfolge ist die des Nutzers, nicht die des Codes: die Artenwahl steht in
        // den Einstellungen und ist vergessen, der Filter "nur verpasste" steht als Knopf
        // ueber der Liste und ist zu sehen. Deshalb zuerst der versteckte Grund.
        if (CallLogGrouping.visible(alle, allowed).isEmpty()) return EmptyCallLog.HIDDEN_BY_TYPE
        return EmptyCallLog.NO_MISSED
    }
}
