package org.biglau.res

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Startbildschirm muss undurchsichtig sein — sonst führt die Heim-Taste nirgendwohin.
 *
 * Am Jelly 2 blieb die vorherige App **auf dem Bildschirm stehen**, wenn man heim ging. Im
 * Task-Auszug stand `type=home … translucent=true`: eine durchsichtige Heim-Aufgabe verdeckt
 * die App dahinter nicht, also blieb sichtbar, was vorher da war. Am Emulator, mit derselben
 * Fassung, stand dort `translucent=false` — der Unterschied kam aus dem Thema des Herstellers.
 *
 * Deshalb steht die Undurchsichtigkeit jetzt im eigenen Thema statt geerbt zu werden. Für
 * einen Startbildschirm ist das kein Detail: die Heim-Taste ist der Weg zurück, auf den sich
 * jemand verlässt, der sich sonst nirgends zurechtfindet.
 */
class HomeOpaqueTest {

    private val thema = File("src/main/res/values/themes.xml").readText()

    @Test
    fun `das Thema sagt ausdruecklich, dass es nicht durchsichtig ist`() {
        assertTrue(
            "windowIsTranslucent fehlt",
            """<item name="android:windowIsTranslucent">false</item>""" in thema,
        )
        assertTrue(
            "windowIsFloating fehlt",
            """<item name="android:windowIsFloating">false</item>""" in thema,
        )
    }

    @Test
    fun `es zeigt auch kein Hintergrundbild durch`() {
        // windowShowWallpaper macht die Aufgabe ebenfalls durchsichtig - und BigLau malt
        // seinen Hintergrund selbst.
        assertTrue(
            "windowShowWallpaper fehlt",
            """<item name="android:windowShowWallpaper">false</item>""" in thema,
        )
    }

    @Test
    fun `der Fensterhintergrund ist eine deckende Farbe`() {
        val zeile = thema.lines().first { "android:windowBackground" in it }
        assertTrue("kein Farbwert: $zeile", Regex("""#[0-9a-fA-F]{6}""").containsMatchIn(zeile))
        assertTrue("mit Alpha statt deckend: $zeile", !Regex("""#[0-9a-fA-F]{8}""").containsMatchIn(zeile))
    }
}
