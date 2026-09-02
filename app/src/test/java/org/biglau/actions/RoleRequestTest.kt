package org.biglau.actions

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein Rollendialog braucht einen Aufrufer.
 *
 * Am Emulator gefunden: der Knopf „BigLau zur Telefon-App machen" tat **nichts**. Im
 * Protokoll stand der Grund: `RequestRoleActivity: Package name cannot be null or empty:
 * null`. Android liest den Aufrufer über `startActivityForResult`; wird der Dialog mit
 * `startActivity` (und erst recht mit `FLAG_ACTIVITY_NEW_TASK`) geöffnet, bricht er ab,
 * bevor er zu sehen ist — auf dem Bildschirm passiert gar nichts, und der Nutzer hält es
 * für einen Fehlgriff.
 *
 * Das betraf beide Rollen: Telefon **und** Startbildschirm. Nach der Änderung erscheint der
 * Dialog („Set BigLau as your default phone app?", am Emulator gesehen).
 */
class RoleRequestTest {

    private val intents = File("src/main/java/org/biglau/actions/Intents.kt").readText()

    private fun rollenTeil(): String =
        intents.substringAfter("fun homeRoleIntent").substringBefore("private inline fun start")

    /**
     * `createRequestRoleIntent` darf in dieser Datei nur als **Absicht zurückgegeben**
     * werden, damit der Aufrufer sie über einen Launcher startet.
     */
    @Test
    fun `die Rollenabsicht wird nicht selbst gestartet`() {
        val zeilen = intents.lines()
        val gestartet = zeilen.indices.filter { index ->
            "createRequestRoleIntent" in zeilen[index] &&
                (index until minOf(index + 6, zeilen.size)).any { "startActivity" in zeilen[it] }
        }
        assertEquals(emptyList<Int>(), gestartet)
        assertTrue("NEW_TASK an einer Rollenabsicht", "FLAG_ACTIVITY_NEW_TASK" !in rollenTeil())
    }

    /**
     * Und die Zeilen sagen den Zustand, statt eine erfüllte Aufforderung zu wiederholen.
     *
     * „Als Telefon-App verwenden" stand auch dann da, wenn BigLau es längst war — ein Tipp
     * darauf tat sichtbar nichts, weil der Rollendialog sich sofort wieder schloss
     * („Application is already a role holder", im Protokoll gesehen).
     */
    @Test
    fun `die Rollenzeilen nennen den Zustand`() {
        val einstellungen = File("src/main/java/org/biglau/settings/SettingsActivity.kt").readText()
        listOf("R.string.is_home", "R.string.is_dialer", "istStartbildschirm", "istTelefonApp")
            .forEach { assertTrue("$it fehlt", it in einstellungen) }
        listOf("src/main/res/values/strings.xml", "src/main/res/values-de/strings.xml").forEach { pfad ->
            val texte = File(pfad).readText()
            listOf("is_home", "is_dialer", "role_change_hint").forEach { name ->
                assertTrue("$pfad: $name fehlt", "\"$name\"" in texte)
            }
        }
    }

    /** Und die Aufrufer nehmen wirklich einen Launcher. */
    @Test
    fun `beide Rollen werden ueber einen Launcher gefragt`() {
        val einstellungen = File("src/main/java/org/biglau/settings/SettingsActivity.kt").readText()
        assertTrue("Telefon-Rolle ohne Launcher", "askDialerRole.launch(" in einstellungen)
        assertTrue("Startbildschirm-Rolle ohne Launcher", "Intents.homeRoleIntent(" in einstellungen)
        val assistent = File("src/main/java/org/biglau/wizard/WizardActivity.kt").readText()
        assertTrue("Assistent ohne Launcher", "askHomeRole.launch(" in assistent)
    }
}
