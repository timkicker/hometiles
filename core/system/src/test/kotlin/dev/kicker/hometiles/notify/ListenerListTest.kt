package dev.kicker.hometiles.notify

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the list of allowed notification services.
 *
 * the system writes our own service sometimes in full, sometimes shortened. both must count -
 * and a foreign service in the same package must not.
 */
class ListenerListTest {

    private val pkg = "dev.kicker.hometiles.debug"
    private val className = "dev.kicker.hometiles.notify.BigNotificationListener"

    private val realList =
        "com.google.android.projection.gearhead/com.google.android.gearhead.notifications." +
            "SharedNotificationListenerManager\$ListenerService:" +
            "dev.kicker.hometiles.debug/dev.kicker.hometiles.notify.BigNotificationListener:" +
            "com.android.launcher3/com.android.launcher3.notification.NotificationListener"

    @Test
    fun `the list as the system writes it counts`() {
        assertTrue(NotificationRepository.ListenerList.contains(realList, pkg, className))
    }

    @Test
    fun `the short form counts just the same`() {
        // the short form only appears when the class name starts with the app's package name.
        // in the debug build it does not (dev.kicker.hometiles.debug against dev.kicker.hometiles.notify...), in
        // the release build it does - there the app is called dev.kicker.hometiles.
        val short = "dev.kicker.hometiles/.notify.BigNotificationListener"
        assertTrue(NotificationRepository.ListenerList.contains(short, "dev.kicker.hometiles", className))
    }

    @Test
    fun `in the release build the long spelling counts too`() {
        val long = "dev.kicker.hometiles/dev.kicker.hometiles.notify.BigNotificationListener"
        assertTrue(NotificationRepository.ListenerList.contains(long, "dev.kicker.hometiles", className))
    }

    @Test
    fun `another service in the same package does not count`() {
        // the reason for this change: before, only the package was compared.
        val other = "dev.kicker.hometiles.debug/dev.kicker.hometiles.notify.AnotherService"
        assertFalse(NotificationRepository.ListenerList.contains(other, pkg, className))
    }

    @Test
    fun `an empty list does not count`() {
        assertFalse(NotificationRepository.ListenerList.contains("", pkg, className))
    }

    @Test
    fun `a foreign package does not count`() {
        val foreign = "com.android.launcher3/com.android.launcher3.notification.NotificationListener"
        assertFalse(NotificationRepository.ListenerList.contains(foreign, pkg, className))
    }

    @Test
    fun `spaces around the entries do not disturb`() {
        val withSpaces = "com.other/A : dev.kicker.hometiles.debug/dev.kicker.hometiles.notify.BigNotificationListener"
        assertTrue(NotificationRepository.ListenerList.contains(withSpaces, pkg, className))
    }
}
