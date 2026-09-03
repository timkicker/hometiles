package org.biglau.phone.probe

import org.biglau.phone.PhoneNumbers

/**
 * Ein Anruf, den niemand fuehrt.
 *
 * Der Anrufbildschirm ist der eine Teil von BigLau, den man nicht ausprobieren kann, ohne
 * jemanden anzurufen - und der zugleich der ist, bei dem ein Fehler heisst, dass man nicht
 * abheben kann. Deshalb stellt sich BigLau den Anruf selbst: eine eigene
 * `ConnectionService` meldet Telecom einen Anruf, Telecom bindet den `InCallService`, und
 * von da an laeuft alles wie im Ernstfall - Klingeln, Annehmen, Halten, Stumm, Tastenfeld,
 * und der Tonweg zwischen Hoermuschel, Lautsprecher und Bluetooth. Nur das Funkmodul ist
 * nie beteiligt.
 *
 * **Nur im Debug-Bau.** Diese Datei liegt in `src/debug` und nicht in `src/main`: eine
 * Probe, die im ausgelieferten Programm steckt, ist ein Weg, einen Anruf vorzutaeuschen.
 */
object Probeanruf {

    /** Die Kennung des Kontos, unter dem Telecom die Probe fuehrt. */
    const val KONTO = "biglau-probe"

    /**
     * Die Nummer, die im Probeanruf steht.
     *
     * Aus dem Bereich, den Ofcom fuer Film und Fernsehen reserviert hat (07700 900000 bis
     * 900999). Diese Nummern sind niemandem zugeteilt und koennen es nicht werden. Der
     * Probeanruf erreicht ohnehin kein Netz - aber eine erfundene Nummer, die es
     * irgendwann doch gibt, waere eine unnoetige Wette.
     */
    const val NUMMER = "+447700900123"

    const val NAME = "Probeanruf"

    /**
     * Soll die Probe klingeln?
     *
     * Ohne dieses Feld faengt sie als **laufendes** Gespraech an: der Anrufbildschirm
     * steht sofort da, mit Tonweg, Stumm, Halten und Tastenfeld - und ohne einen Ton. Das
     * ist der Zustand, in dem fast alles zu pruefen ist. Das Klingeln ist die andere
     * Haelfte und braucht einen Menschen, der sie hoeren will.
     */
    const val EXTRA_KLINGELN = "org.biglau.probe.KLINGELN"

    /**
     * Bitte an Telecom, diesen Anruf nicht in die Anrufliste zu schreiben.
     *
     * Am 03.09.2026 standen nach sechs Proben „+44 7700 900123 (6)“ oben in der
     * Anrufliste des Nutzers - eine Probe, die Spuren in echten Daten hinterlaesst, ist
     * keine mehr. Die Konstante steht in `TelecomManager` nur als verborgenes Feld;
     * deshalb hier der Name als Zeichenkette, und deshalb wird am Geraet **nachgesehen**,
     * ob sie wirkt, statt es anzunehmen.
     */
    const val EXTRA_NICHT_PROTOKOLLIEREN = "android.telecom.extra.DO_NOT_LOG_CALL"

    /**
     * Darf mit dieser Nummer geprobt werden?
     *
     * Der Probeanruf geht nie ins Netz, und trotzdem steht diese Pruefung hier: sie kostet
     * nichts und schliesst die eine Moeglichkeit aus, in der aus einer Probe ein Notruf
     * wird. Siehe `KeinNotrufTest`.
     */
    fun erlaubt(nummer: String): Boolean =
        nummer.isNotBlank() && !PhoneNumbers.looksLikeEmergency(nummer)
}
