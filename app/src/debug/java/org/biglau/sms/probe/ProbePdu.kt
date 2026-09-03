package org.biglau.sms.probe

import java.util.Calendar

/**
 * Baut ein SMS-DELIVER-PDU nach GSM 03.40 - eine Nachricht, die nie durch ein Funknetz kam.
 *
 * Warum so umstaendlich und nicht einfach eine Zeile in die Datenbank geschrieben: der Weg,
 * den eine eingehende SMS in BigLau nimmt, faengt bei `SmsDeliverReceiver` an und geht ueber
 * `Telephony.Sms.Intents.getMessagesFromIntent`. Wer die Datenbankzeile direkt schreibt,
 * prueft das Ende des Weges und laesst den Anfang aus - also genau die Stelle, an der eine
 * echte Nachricht verlorengehen kann.
 *
 * **Nur im Debug-Bau.** Ein Programm, das sich selbst SMS unterschieben kann, gehoert nicht
 * in fremde Hand.
 *
 * Reine Rechnerei, keine Android-Klasse - deshalb in `ProbePduTest` Byte fuer Byte gegen
 * von Hand ausgerechnete Werte gehalten.
 */
object ProbePdu {

    /**
     * Ziffern einer Rufnummer, paarweise vertauscht (GSM 03.40, 9.1.2.5).
     *
     * Bei ungerader Anzahl wird mit `F` aufgefuellt - nicht mit `0`, das waere eine Ziffer.
     */
    fun adresse(nummer: String): ByteArray {
        val ziffern = nummer.filter { it.isDigit() }
        val gefuellt = if (ziffern.length % 2 == 0) ziffern else ziffern + "F"
        val bytes = ByteArray(gefuellt.length / 2)
        for (i in bytes.indices) {
            val links = gefuellt[2 * i + 1].let { if (it == 'F') 0x0F else it - '0' }
            val rechts = gefuellt[2 * i] - '0'
            bytes[i] = ((links shl 4) or rechts).toByte()
        }
        return bytes
    }

    /**
     * Packt Text in Septetts (GSM 03.38, 7-Bit).
     *
     * Sieben Bit pro Zeichen, dicht aneinander - deshalb wandern die Bitgrenzen durch die
     * Bytes. Pruefwert: „hello" wird zu E8 32 9B FD 06.
     */
    fun packen(text: String): ByteArray {
        val septetts = text.map { it.code and 0x7F }
        val heraus = ArrayList<Int>()
        var uebertrag = 0
        var bits = 0
        septetts.forEach { s ->
            uebertrag = uebertrag or (s shl bits)
            bits += 7
            while (bits >= 8) {
                heraus += uebertrag and 0xFF
                uebertrag = uebertrag ushr 8
                bits -= 8
            }
        }
        if (bits > 0) heraus += uebertrag and 0xFF
        return heraus.map { it.toByte() }.toByteArray()
    }

    /** Zeitstempel, sieben Bytes, jedes Paar vertauscht. Das letzte ist die Zeitzone. */
    fun zeitstempel(zeit: Long, zonenViertelstunden: Int = 0): ByteArray {
        val kalender = Calendar.getInstance().apply { timeInMillis = zeit }
        val teile = intArrayOf(
            kalender.get(Calendar.YEAR) % 100,
            kalender.get(Calendar.MONTH) + 1,
            kalender.get(Calendar.DAY_OF_MONTH),
            kalender.get(Calendar.HOUR_OF_DAY),
            kalender.get(Calendar.MINUTE),
            kalender.get(Calendar.SECOND),
            zonenViertelstunden,
        )
        return teile.map { wert ->
            val zehner = wert / 10
            val einer = wert % 10
            ((einer shl 4) or zehner).toByte()
        }.toByteArray()
    }

    /**
     * Das ganze PDU.
     *
     * Ohne SMSC-Teil (erstes Byte 0): den setzt das Geraet selbst ein. Erstes Oktett 0x04 -
     * SMS-DELIVER, keine weiteren Nachrichten, kein Benutzerdatenkopf.
     */
    fun baue(absender: String, text: String, zeit: Long = System.currentTimeMillis()): ByteArray {
        val ziffern = absender.filter { it.isDigit() }
        val heraus = ArrayList<Byte>()
        heraus += 0x00 // kein SMSC
        heraus += 0x04 // SMS-DELIVER
        heraus += ziffern.length.toByte() // Laenge in Ziffern, nicht in Bytes
        heraus += 0x91.toByte() // international, ISDN
        heraus += adresse(absender).toList()
        heraus += 0x00 // PID
        heraus += 0x00 // DCS: GSM 7-Bit
        heraus += zeitstempel(zeit).toList()
        heraus += text.length.toByte() // Laenge in Septetts
        heraus += packen(text).toList()
        return heraus.toByteArray()
    }
}
