package org.biglau.ui

/**
 * Die drei Zahlen, die das Raster ausmessen. PLAN.md 3.2 und 4.1.
 *
 * Sie standen seit dem ersten Tag im Modell, wurden ueberall gelesen - achtzehn Stellen -
 * und waren nirgends zu aendern. Der Plan sagt sie als Einstellung zu, und die Tabelle in
 * 3.2 nennt auch die Grenzen; hier stehen sie.
 */
object GridLooks {

    /** Abstand zwischen den Kacheln. */
    val GUTTERS = listOf(0, 2, 4, 8, 12)

    /**
     * Aussenrand in Prozent der Bildschirmbreite. Fuer Geraete mit gerundeten Ecken, wo
     * eine Kachel sonst unter die Rundung laeuft.
     */
    val BORDERS = listOf(0, 2, 5, 10, 15)

    /**
     * Eckenradius. Ein einziger Wert fuer alle Flaechen - PLAN.md 3.7 verbietet einen
     * zweiten Radius daneben, nicht das Verstellen dieses einen.
     */
    val RADII = listOf(0, 6, 12, 18, 24)

    fun gutter(dp: Int): Int = dp.coerceIn(0, 12)

    fun border(percent: Int): Int = percent.coerceIn(0, 15)

    fun radius(dp: Int): Int = dp.coerceIn(0, 24)
}
