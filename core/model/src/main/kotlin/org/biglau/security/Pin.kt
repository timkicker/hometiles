package org.biglau.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Betreuer-PIN fuer die Einstellungen.
 *
 * Das ist kein Geheimnisschutz gegen einen Angreifer mit dem Geraet in der Hand - dagegen
 * hilft die Bildschirmsperre. Es ist eine Huerde gegen versehentliche Aenderungen. Trotzdem
 * wird gesalzen und gestreckt gespeichert: die Datei ist zugleich das Sicherungsformat und
 * landet damit irgendwann auf einem Rechner.
 */
object Pin {

    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8

    private const val ITERATIONS = 20_000
    private const val KEY_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    /**
     * Muss vor dem Kachel-Editor eine PIN abgefragt werden?
     *
     * Nur, wenn es überhaupt eine gibt **und** der Schutz eingeschaltet ist. Der Sinn ist
     * nicht Geheimhaltung, sondern dass die Belegung nicht versehentlich zerlegt wird -
     * ein langer Druck passiert schneller, als man denkt.
     *
     * Bewusst **ohne** eigenen Notausstieg: den gibt es genau einmal, auf der PIN-Eingabe
     * der Einstellungen. Zwei Hintertüren an einem Schloss sind eine zu viel, und von den
     * Einstellungen aus lässt sich dieser Schutz abschalten.
     */
    fun protectsEditor(stored: String?, enabled: Boolean): Boolean = protects(stored, enabled)

    /**
     * Die allgemeine Frage: schuetzt die PIN diesen Schritt?
     *
     * Ohne gesetzte PIN schuetzt nichts - ein eingeschalteter Schutz ohne Schloss waere
     * eine Zusage, die beim ersten Antippen zerfaellt.
     */
    fun protects(stored: String?, enabled: Boolean): Boolean = usable(stored) && enabled

    /**
     * Laesst sich der gespeicherte Wert ueberhaupt pruefen?
     *
     * Gespeichert wird "Runden:Salz:Hash". Steht dort etwas anderes - eine von Hand
     * bearbeitete Sicherung mit `"pin": "1234"` etwa -, dann lehnt [verify] **jede**
     * Eingabe ab. Das ist kein Schloss mehr, sondern eine zugemauerte Tuer: am Emulator
     * gesehen, wo die App-Sperre danach jede App verweigerte und die richtige PIN nichts
     * half. Die Einstellungen haetten noch den Notausstieg, die Sperre auf dem
     * Startbildschirm hat keinen.
     *
     * Deshalb zaehlt ein unpruefbarer Wert als „keine PIN". Wer die Datei so weit
     * bearbeiten kann, koennte die Zeile ohnehin auch ganz loeschen - es geht kein Schutz
     * verloren, der vorher gewirkt haette.
     */
    fun usable(stored: String?): Boolean {
        val parts = stored?.split(":") ?: return false
        if (parts.size != 3) return false
        if (parts[0].toIntOrNull() == null) return false
        val decoder = Base64.getDecoder()
        return parts.drop(1).all { runCatching { decoder.decode(it) }.isSuccess }
    }

    /** Nur Ziffern, und zwar zwischen vier und acht davon. */
    fun isValid(pin: String): Boolean =
        pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it.isDigit() }

    /** Ergibt "iterationen:salz:hash", jeweils Base64. Null bei ungueltiger Eingabe. */
    fun hash(pin: String, salt: ByteArray = randomSalt()): String? {
        if (!isValid(pin)) return null
        val encoder = Base64.getEncoder()
        return listOf(
            ITERATIONS.toString(),
            encoder.encodeToString(salt),
            encoder.encodeToString(derive(pin, salt, ITERATIONS)),
        ).joinToString(":")
    }

    /** Prueft eine Eingabe gegen einen gespeicherten Wert. */
    fun verify(pin: String, stored: String?): Boolean {
        if (stored == null) return true // keine PIN gesetzt: alles offen
        val parts = stored.split(":")
        if (parts.size != 3) return false
        val iterations = parts[0].toIntOrNull() ?: return false
        val decoder = Base64.getDecoder()
        val salt = runCatching { decoder.decode(parts[1]) }.getOrNull() ?: return false
        val expected = runCatching { decoder.decode(parts[2]) }.getOrNull() ?: return false
        if (!pin.all { it.isDigit() } || pin.isEmpty()) return false
        return constantTimeEquals(derive(pin, salt, iterations), expected)
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }

    private fun randomSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    /** Vergleicht ohne frueh abzubrechen - sonst verraet die Laufzeit, wie weit man kam. */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var difference = 0
        for (i in a.indices) difference = difference or (a[i].toInt() xor b[i].toInt())
        return difference == 0
    }
}
