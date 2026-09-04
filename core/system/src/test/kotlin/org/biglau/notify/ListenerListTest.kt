package org.biglau.notify

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Liste der erlaubten Benachrichtigungsdienste.
 *
 * Am Gerät sieht sie so aus:
 * `com.google.android.projection.gearhead/…$ListenerService:org.biglau.debug/org.biglau.notify.BigNotificationListener:com.android.launcher3/…NotificationListener`
 *
 * Das System schreibt den eigenen Dienst mal voll, mal verkürzt. Beides muss zählen -
 * und ein fremder Dienst im selben Paket darf nicht mitzählen.
 */
class ListenerListTest {

    private val paket = "org.biglau.debug"
    private val klasse = "org.biglau.notify.BigNotificationListener"

    private val echteListe =
        "com.google.android.projection.gearhead/com.google.android.gearhead.notifications." +
            "SharedNotificationListenerManager\$ListenerService:" +
            "org.biglau.debug/org.biglau.notify.BigNotificationListener:" +
            "com.android.launcher3/com.android.launcher3.notification.NotificationListener"

    @Test
    fun `die Liste vom Geraet zaehlt`() {
        assertTrue(NotificationRepository.ListenerList.contains(echteListe, paket, klasse))
    }

    @Test
    fun `die Kurzform zaehlt genauso`() {
        // Die Kurzform entsteht nur, wenn der Klassenname mit dem Paketnamen der App
        // beginnt. Beim Debug-Bau tut er das nicht (org.biglau.debug gegen
        // org.biglau.notify...), beim Release-Bau schon - dort heisst die App org.biglau.
        val kurz = "org.biglau/.notify.BigNotificationListener"
        assertTrue(NotificationRepository.ListenerList.contains(kurz, "org.biglau", klasse))
    }

    @Test
    fun `im Release-Bau zaehlt auch die lange Schreibweise`() {
        val lang = "org.biglau/org.biglau.notify.BigNotificationListener"
        assertTrue(NotificationRepository.ListenerList.contains(lang, "org.biglau", klasse))
    }

    @Test
    fun `ein anderer Dienst im selben Paket zaehlt nicht`() {
        // Der Grund für diese Änderung: vorher wurde nur das Paket verglichen.
        val anderer = "org.biglau.debug/org.biglau.notify.EinAndererDienst"
        assertFalse(NotificationRepository.ListenerList.contains(anderer, paket, klasse))
    }

    @Test
    fun `eine leere Liste zaehlt nicht`() {
        assertFalse(NotificationRepository.ListenerList.contains("", paket, klasse))
    }

    @Test
    fun `ein fremdes Paket zaehlt nicht`() {
        val fremd = "com.android.launcher3/com.android.launcher3.notification.NotificationListener"
        assertFalse(NotificationRepository.ListenerList.contains(fremd, paket, klasse))
    }

    @Test
    fun `Leerzeichen um die Eintraege stoeren nicht`() {
        val mitLuft = "com.fremd/A : org.biglau.debug/org.biglau.notify.BigNotificationListener"
        assertTrue(NotificationRepository.ListenerList.contains(mitLuft, paket, klasse))
    }
}
