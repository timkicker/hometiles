package org.biglau

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein Modus, den man nicht sieht, ist eine Falle.
 *
 * Der Bearbeitungsmodus ist für die da, die den langen Druck nicht schaffen: ein kurzer
 * Tipp macht dann den Kachel-Editor auf statt die App. Damit das keine Überraschung ist,
 * steht ein Streifen oben: „Bearbeiten — tippe eine Kachel an…". Er ist zugleich der Weg
 * hinaus, denn ein Tipp auf ihn beendet den Modus.
 *
 * Am 04.09.2026 am Jelly 2 nachgestellt: aus dem offenen Ordner „Mehr" in die Einstellungen,
 * dort „Kacheln ändern", zurück — und der Ordner stand noch offen, ohne Streifen. Der
 * Modus war an (der nächste Tipp machte den Editor auf, auf der richtigen Kachel sogar),
 * aber nichts sagte es, und der Weg hinaus lag unter derselben Überlagerung. Der Streifen
 * steht in der Spalte des Startbildschirms, und die ist bei offenem Ordner nicht nur
 * verdeckt, sondern per `clearAndSetSemantics` auch stumm — er war also weder zu sehen noch
 * zu hören.
 *
 * Deshalb bekommt die Überlagerung ihren eigenen Streifen. Nicht: „dann eben den Ordner
 * schliessen" — im Ordner **funktioniert** der Bearbeitungsmodus, und genau die Leute, für
 * die er gebaut ist, kämen sonst an die Kacheln im Ordner nie heran.
 */
class BearbeitungsstreifenTest {

    private val haupt = Quelltext.ohneKommentare("org/biglau/MainActivity.kt")

    @Test
    fun `der ordner zeigt den streifen selbst`() {
        val aufruf = Quelltext.ausschnitt(
            haupt,
            von = "                    FolderOverlay(",
            bis = "                    ) {",
        )
        assertTrue(
            "Die Ordner-Ueberlagerung bekommt keinen Bearbeiten-Streifen: $aufruf",
            "banner" in aufruf && "editMode" in aufruf,
        )
    }

    @Test
    fun `der streifen steht an beiden stellen`() {
        val wieOft = Regex("EditModeBanner \\{").findAll(haupt).count()
        assertTrue(
            "EditModeBanner wird $wieOft-mal gezeigt. Zweimal muss es sein: einmal auf dem " +
                "Startbildschirm und einmal in der Ordner-Ueberlagerung, die ihn sonst " +
                "verdeckt.",
            wieOft >= 2,
        )
    }
}
