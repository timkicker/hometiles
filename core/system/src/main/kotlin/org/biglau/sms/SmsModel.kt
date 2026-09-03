package org.biglau.sms

/** Eine einzelne Nachricht. */
data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val incoming: Boolean,
    val read: Boolean,
    /**
     * Das Netz hat die Nachricht nicht genommen.
     *
     * Bis zum 03.09.2026 gab es diesen Zustand nicht: `sendTextMessage` bekam keine
     * Quittung, und „gesendet" hiess nur, dass der Aufruf keine Ausnahme geworfen hat. Eine
     * Nachricht, die das Netz ablehnt - kein Empfang, Funk aus, kein Guthaben -, stand
     * danach genauso da wie eine angekommene. Wer sich darauf verlaesst, wartet auf eine
     * Antwort auf etwas, das nie losgegangen ist.
     */
    val failed: Boolean = false,
)

/** Ein Gespraech, wie es in der Liste steht. */
data class SmsThread(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val lastMessage: SmsMessage,
    val unreadCount: Int,
) {
    val hasUnread: Boolean get() = unreadCount > 0
    /**
     * Der Name, sonst die Nummer - und die in derselben Schreibweise wie in der
     * Anrufliste. Dieselbe Nummer sah in den beiden Listen verschieden aus, und wer
     * vergleicht, vergleicht dann zwei Schreibweisen statt zweier Nummern.
     */
    val title: String
        get() = contactName?.takeIf { it.isNotBlank() }
            ?: org.biglau.phone.PhoneNumbers.forDisplay(address)

    /**
     * Wie [title], aber nie leer.
     *
     * Ohne Absender (der Anbieter laesst das Feld bei manchen Nachrichten leer) stuende
     * in der Liste sonst eine Zeile ohne Beschriftung - anzutippen, ohne zu wissen, was
     * sich oeffnet.
     */
    fun titleOr(unbekannt: String): String = title.ifBlank { unbekannt }
}
