package org.biglau.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * the carer pin for the settings.
 *
 * not secrecy against someone holding the device; the lock screen is for that. it is a hurdle
 * against accidental changes. salted and stretched all the same, because the config file is
 * also the backup format and ends up on a computer sooner or later.
 */
object Pin {

    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8

    private const val ITERATIONS = 20_000
    private const val KEY_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    /**
     * deliberately **without** an escape hatch of its own: there is exactly one, on the pin
     * entry of the settings, and from there this protection can be switched off. two back
     * doors on one lock are one too many.
     */
    fun protectsEditor(stored: String?, enabled: Boolean): Boolean = protects(stored, enabled)

    /** with no pin set nothing protects: an enabled lock without a key would fall apart on first use. */
    fun protects(stored: String?, enabled: Boolean): Boolean = usable(stored) && enabled

    /**
     * can the stored value be checked at all?
     *
     * stored as "rounds:salt:hash". anything else - a hand-edited backup with
     * `"pin": "1234"` - makes [verify] reject **every** input, which is not a lock but a
     * bricked-up door: the settings still have their escape hatch, the home screen lock does
     * not. so an uncheckable value counts as "no pin". whoever can edit the file that far
     * could delete the line anyway, so no protection is lost that ever worked.
     */
    fun usable(stored: String?): Boolean {
        val parts = stored?.split(":") ?: return false
        if (parts.size != 3) return false
        if (parts[0].toIntOrNull() == null) return false
        val decoder = Base64.getDecoder()
        return parts.drop(1).all { runCatching { decoder.decode(it) }.isSuccess }
    }

    fun isValid(pin: String): Boolean =
        pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it.isDigit() }

    /** "iterations:salt:hash", each base64. null on invalid input. */
    fun hash(pin: String, salt: ByteArray = randomSalt()): String? {
        if (!isValid(pin)) return null
        val encoder = Base64.getEncoder()
        return listOf(
            ITERATIONS.toString(),
            encoder.encodeToString(salt),
            encoder.encodeToString(derive(pin, salt, ITERATIONS)),
        ).joinToString(":")
    }

    fun verify(pin: String, stored: String?): Boolean {
        if (stored == null) return true // no pin set: everything open
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

    /** no early exit, or the running time would give away how far one got. */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var difference = 0
        for (i in a.indices) difference = difference or (a[i].toInt() xor b[i].toInt())
        return difference == 0
    }
}
