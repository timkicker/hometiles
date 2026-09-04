package org.biglau.phone

/**
 * Umgang mit Rufnummern - insbesondere mit Notrufnummern.
 *
 * Warum das hier und nicht nur ueber die Android-API laeuft: manche Geraete lassen einen
 * Notruf aus einer Fremd-App gar nicht zu, andere leiten ihn still um. Das Original hat
 * dafuer eine Einstellung. Wir wollen keine Einstellung, sondern eine Regel: eine Nummer,
 * die *irgendwie* nach Notruf aussieht, waehlt BigLau nie selbst, sondern uebergibt sie an
 * den System-Dialer. Lieber ein Tastendruck mehr als ein verschluckter Notruf.
 *
 * Die Liste ergaenzt die Plattformpruefung, sie ersetzt sie nicht.
 */
object PhoneNumbers {

    /** Notrufnummern, die ohne Netz und ohne SIM gelten sollen. Bewusst grosszuegig. */
    val WELL_KNOWN_EMERGENCY = setOf(
        "112", "911", "999", "000", "110", "118", "119", "115", "122", "133", "144", "911",
        "08", "999", "112",
    )

    /** Nur Ziffern, Plus und die Waehlzeichen, die das Netz kennt. */
    fun clean(number: String): String =
        number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }

    /**
     * Sieht die Nummer nach Notruf aus? Bewusst grosszuegig: ein falsch positiver Treffer
     * kostet einen Tastendruck im System-Dialer, ein falsch negativer kann einen Notruf kosten.
     */
    fun looksLikeEmergency(number: String, platformSaysYes: Boolean = false): Boolean {
        if (platformSaysYes) return true
        val digits = clean(number).removePrefix("+")
        if (digits.isEmpty()) return false
        return digits in WELL_KNOWN_EMERGENCY
    }

    /** Sinnvoll waehlbar? Leere und reine Zeichenfolgen ohne Ziffer sind es nicht. */
    fun isDialable(number: String): Boolean = clean(number).any { it.isDigit() }

    /**
     * Das Land, in dem eine Nummer ohne Vorwahl gilt (ISO, z. B. "at").
     *
     * Wird einmal beim Start aus der SIM gelesen. Null heisst: unbekannt - dann bleibt es
     * bei der blossen Gruppierung, statt eine Vorwahl zu raten.
     */
    @Volatile
    var region: String? = null

    /**
     * Wie das System die Nummer schreibt, oder null, wenn es das nicht kann.
     *
     * Von aussen gesetzt, und deshalb liegt diese Datei im reinen Kotlin-Modul: Android
     * bringt die Vorwahltabellen mit, aber `PhoneNumberUtils` ist Framework, und dieses
     * Modul soll davon nichts wissen. `SystemNumbers.install` in `core:system` haengt die
     * echte Schreibweise ein; `StartAufgabenTest` sorgt dafuer, dass das beim Start
     * geschieht.
     *
     * Die Vorgabe ist **null**, also der Rueckfall auf blosse Gruppierung. Das ist die
     * ehrliche Vorgabe: wer die Systemteile nicht einhaengt, bekommt lesbare Bloecke - und
     * nicht eine geratene Laendervorwahl.
     */
    @Volatile
    var systemFormat: (String, String?) -> String? = { _, _ -> null }

    /**
     * Die Nummer, wie sie am Bildschirm steht.
     *
     * **Zuerst fragt sie das System.** Android bringt die Vorwahltabellen mit, und ohne sie
     * wird die Gruppierung falsch: eine oesterreichische Mobilnummer stand auf dem
     * Startbildschirm als „+436 804 …" da - „+436" ist kein Land, Oesterreich ist „+43".
     * Wer so etwas abschreibt oder vorliest, schreibt es falsch ab. Das ist am Geraet des
     * Nutzers aufgefallen, an seinen eigenen Kontakten.
     *
     * Bleibt das System die Antwort schuldig, wird gruppiert wie bisher - lesbare Bloecke
     * ohne geratene Laendervorwahl.
     *
     * **Nicht jeder Absender ist eine Nummer.** Banken, Paketdienste und Anmeldecodes
     * kommen als Buchstabenkennung ("ADAC", "Bank"), und [clean] laesst davon nichts
     * uebrig: in der Nachrichtenliste stand dann eine **leere Zeile**. Bleibt nach dem
     * Saeubern nichts, steht deshalb der Text selbst da.
     */
    fun forDisplay(number: String): String {
        val cleaned = clean(number)
        if (cleaned.isEmpty()) return number.trim()
        if (cleaned.length <= 6) return cleaned
        systemFormat(cleaned, region)?.takeIf { it.isNotBlank() }?.let { return it }
        return grouped(cleaned)
    }

    /**
     * Dieselbe Nummer, aber zum **Vorlesen**: jedes Zeichen fuer sich.
     *
     * Eine Rufnummer ist keine Zahl. Als gewoehnlicher Text gelesen macht ein
     * Vorleseprogramm aus "123" ein "einhundertdreiundzwanzig" und aus "111001" ein
     * Wortungetuem - und wer die Nummer nachpruefen will, kann es nicht. Am 04.09.2026 am
     * Emulator gesehen: die Wähltastatur zeigt die getippte Nummer als schlichten Text,
     * ohne eine eigene Beschreibung.
     *
     * Ziffernweise mit Leerzeichen ist die Fassung, die jedes Vorleseprogramm einzeln
     * liest. Das Plus bleibt stehen, Gruppierungsluecken fallen weg - sie stehen fuers
     * Auge da, und das Ohr bekommt ohnehin nach jeder Ziffer eine Pause.
     *
     * Buchstabenkennungen ("ADAC") bleiben, wie sie sind: die sind ein Wort und werden als
     * Wort gelesen.
     */
    fun forSpeech(number: String): String {
        val cleaned = clean(number)
        if (cleaned.isEmpty()) return number.trim()
        return cleaned.map { it.toString() }.joinToString(" ")
    }

    /** Der Rueckfall: Dreierbloecke, das Plus bleibt am Anfang stehen. */
    private fun grouped(cleaned: String): String {
        val plus = cleaned.startsWith("+")
        val body = if (plus) cleaned.drop(1) else cleaned
        val bloecke = body.chunked(3).joinToString(" ")
        return if (plus) "+$bloecke" else bloecke
    }
}
