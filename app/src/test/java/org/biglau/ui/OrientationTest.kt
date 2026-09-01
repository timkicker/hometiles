package org.biglau.ui

import android.content.pm.ActivityInfo
import org.biglau.data.Appearance
import org.biglau.data.ScreenOrientation
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.2: „Bildschirmausrichtung: automatisch / hoch / quer".
 *
 * Stand zwölfmal als `portrait` im Manifest — eine Entscheidung, die im Plan als
 * Einstellung zugesagt war und nirgends zu ändern.
 */
class OrientationTest {

    // Die Vorgabe ist, was bisher fest verdrahtet war: niemandem soll sich das Telefon
    // drehen, nur weil es die Einstellung jetzt gibt.
    @Test
    fun `die vorgabe bleibt hochkant`() {
        assertEquals(ScreenOrientation.PORTRAIT, Appearance().orientation)
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            Orientation.requested(ScreenOrientation.PORTRAIT),
        )
    }

    @Test
    fun `quer ist quer`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            Orientation.requested(ScreenOrientation.LANDSCAPE),
        )
    }

    /**
     * „Automatisch" heißt SENSOR, nicht UNSPECIFIED. UNSPECIFIED überlässt die Sache der
     * Drehsperre des Systems — und wer die an hat, hätte hier eine Wahl getroffen, die
     * folgenlos bleibt. Dann sucht man den Fehler bei uns.
     */
    @Test
    fun `automatisch dreht sich auch bei gesperrter systemdrehung`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR,
            Orientation.requested(ScreenOrientation.AUTO),
        )
    }

    @Test
    fun `es gibt genau drei moeglichkeiten`() {
        assertEquals(3, ScreenOrientation.entries.size)
    }
}
