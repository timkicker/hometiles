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

    /** Höhe einer Knopfzeile - dieselben 72 dp wie in `ActionRow`. */
    const val ROW_DP = 72f

    /** Abstand zwischen den Zeilen - dasselbe `spacedBy(8.dp)` wie im Aufbau. */
    const val ROW_GAP_DP = 8f

    /** Was Name und Zustand darüber brauchen (Name bis zu zwei Zeilen). */
    const val HEADER_DP = 120f

    /**
     * Der Leerraum über den Knöpfen, `PLAN.md` 4.6 („Anzahl leerer Zeilen … gegen
     * Fehlbedienung mit dem Ohr").
     *
     * Er ist keine Einstellung, sondern eine Untergrenze - und die gehört hierher, weil
     * das Foto das Einzige ist, was dafür Platz abgeben kann.
     */
    const val EAR_GAP_DP = 56f

    /** Unter dieser Höhe lohnt kein Foto mehr; ein Streifen zeigt kein Gesicht. */
    const val MIN_PHOTO_DP = 64f

    /** Die Zeile „Gespräch mit … läuft", wenn es während eines Gesprächs klingelt. */
    const val NOTICE_DP = 44f

    /**
     * Was unter dem Foto stehen muss, bei [buttons] Knopfzeilen.
     *
     * **Das war der Fehler:** hier stand eine feste Zahl (220 dp), während die Zahl der
     * Knöpfe zwischen zwei (es klingelt) und fünf (Gespräch läuft) schwankt. Am Emulator
     * mit „halbes Display": beim Klingeln passte es knapp, nach dem Annehmen standen von
     * fünf Knöpfen nur noch zwei im Bild — Lautsprecher, Halten und Tastenfeld waren
     * unerreichbar, und gescrollt wird auf diesem Bildschirm nicht. Ausgerechnet der
     * Lautsprecher, den ein schwerhöriger Mensch im Gespräch braucht.
     */
    fun reservedDp(buttons: Int, notice: Boolean = false): Float =
        HEADER_DP + buttons * ROW_DP + (buttons + 1) * ROW_GAP_DP + EAR_GAP_DP +
            if (notice) NOTICE_DP else 0f

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
     * [availableDp] ist die ganze nutzbare Höhe, [buttons] die Zahl der Knopfzeilen
     * darunter. Abgezogen wird, was der Rest braucht — im Zweifel weicht das Foto und
     * nicht der Knopf. Auf drei Zoll heißt das: während des Gesprächs bleibt für ein Foto
     * kein Platz mehr. Das ist die richtige Reihenfolge; ein Bild, das den Lautsprecher
     * verdeckt, nützt niemandem.
     */
    fun heightDp(
        size: CallerPhoto,
        availableDp: Float,
        buttons: Int,
        notice: Boolean = false,
    ): Float {
        if (size == CallerPhoto.OFF) return 0f
        val platz = (availableDp - reservedDp(buttons, notice)).coerceAtLeast(0f)
        if (platz < MIN_PHOTO_DP) return 0f
        return (availableDp * fractionOf(size)).coerceAtMost(platz)
    }
}
