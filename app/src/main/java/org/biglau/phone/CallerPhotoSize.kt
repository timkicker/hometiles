package org.biglau.phone

import org.biglau.data.CallerPhoto

/**
 * Wie viel Höhe das Foto des Anrufers bekommt (`PLAN.md` 4.6).
 *
 * Gerechnet als Anteil der Bildschirmhöhe und **nach oben begrenzt**: was übrig bleibt,
 * muss den Namen, den Zustand und die Knöpfe tragen. Ein Foto, das den Annehmen-Knopf aus
 * dem Bild schiebt, wäre auf genau dem Bildschirm ein Fehler, auf dem ein Fehler bedeutet,
 * dass jemand einen Anruf nicht annehmen kann.
 */
object CallerPhotoSize {

    /** Was Name, Zustand und die Knöpfe mindestens brauchen. */
    const val RESERVED_DP = 220f

    fun fractionOf(size: CallerPhoto): Float = when (size) {
        CallerPhoto.OFF -> 0f
        CallerPhoto.SMALL -> 0.18f
        CallerPhoto.HALF -> 0.5f
        CallerPhoto.FULL -> 1f
    }

    /** Was auf dem Anrufbildschirm über dem Namen steht. */
    enum class Image { NONE, PHOTO, INITIALS }

    /**
     * Foto, Initialen oder nichts.
     *
     * Am Emulator gesehen: mit „halber Bildschirm" und einem Kontakt **ohne** Foto blieb die
     * halbe Fläche des wichtigsten Bildschirms schwarz. Die Initialen stehen überall sonst
     * in der App an dieser Stelle - ein farbiges Feld mit „AB" ist auf drei Zoll schneller
     * erkannt als ein Name gelesen.
     *
     * Bei einer unbekannten Nummer bleibt es leer: aus „+43" ließe sich kein Zeichen machen,
     * das etwas bedeutet.
     */
    fun imageFor(heightDp: Float, photoUri: String?, name: String?): Image = when {
        heightDp <= 0f -> Image.NONE
        photoUri != null -> Image.PHOTO
        !name.isNullOrBlank() -> Image.INITIALS
        else -> Image.NONE
    }

    /**
     * Die Höhe in dp. Null heißt: kein Foto zeigen.
     *
     * [availableDp] ist die ganze nutzbare Höhe; abgezogen wird, was der Rest braucht.
     */
    fun heightDp(size: CallerPhoto, availableDp: Float): Float {
        if (size == CallerPhoto.OFF) return 0f
        val platz = (availableDp - RESERVED_DP).coerceAtLeast(0f)
        return (availableDp * fractionOf(size)).coerceAtMost(platz)
    }
}
