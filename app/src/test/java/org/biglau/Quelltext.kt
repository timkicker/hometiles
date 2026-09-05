package org.biglau

import java.io.File

/**
 * Wo der Quelltext von BigLau liegt - fuer die Regeln, die ihn lesen.
 *
 * Knapp die Haelfte der Tests hier prueft nicht Verhalten, sondern Quelltext: tote Felder,
 * unbenutzte Texte, Sprachregeln, Dokumentation am falschen Platz. Sie alle liefen bisher
 * ueber `File("src/main/java")`, den Hauptquelltext von `:app`.
 *
 * Solange alles in `:app` liegt, stimmt das. Beim Modulschnitt aus `PLAN.md` 2.1 stimmt es
 * nicht mehr - und zwar **lautlos**: `walkTopDown()` auf einem Verzeichnis, das es nicht
 * gibt, liefert keine Datei und keinen Fehler. Die Regel bliebe gruen und pruefte nichts
 * mehr. Genau deshalb steht die Liste der Wurzeln an einer Stelle, und ein Test sieht nach,
 * dass sie vollstaendig ist.
 */
object Quelltext {

    /**
     * Der Hauptquelltext von `:app` allein.
     *
     * Eine Regel, die nur diesen braucht - etwa die Kreise zwischen den Bereichen -, soll
     * ihn nicht selbst hinschreiben muessen; genau das verbietet `QuelltextTest`.
     */
    val appWurzel: File = File("src/main/java")

    /**
     * Alle Wurzeln mit Hauptquelltext, relativ zum Arbeitsverzeichnis der Tests (`app/`).
     *
     * Neue Module gehoeren hier hinein. `QuelltextTest` faellt um, wenn eines fehlt.
     */
    val wurzeln: List<File> = listOf(
        appWurzel,
        File("../core/model/src/main/kotlin"),
        File("../core/data/src/main/kotlin"),
        File("../core/system/src/main/kotlin"),
        File("../core/ui/src/main/kotlin"),
    )

    /**
     * Die Ressourcenwurzeln aller Module.
     *
     * Beim Umzug von `:core:ui` sind die beiden Schriftdateien mitgewandert, und zwei
     * Regeln suchten sie weiter unter `app/src/main/res`. Sie fielen laut um - aber
     * dieselbe Liste, die den Quelltext zusammenhaelt, taugt auch dafuer.
     */
    val resWurzeln: List<File> = listOf(
        File("src/main/res"),
        File("../core/ui/src/main/res"),
        // Am 04.09.2026 dazugekommen: die sechs Woerter fuer die Richtung eines Anrufs
        // liegen bei `CallDirection`, damit sie nicht zweimal gefuehrt werden muessen.
        File("../core/system/src/main/res"),
    )

    /**
     * Alle Textdateien eines Sprachverzeichnisses, ueber alle Module.
     *
     * Zehn Regeln lasen `app/src/main/res/values/strings.xml` und meinten damit „alle
     * Texte". Solange die Texte nur dort liegen, stimmt das. Zieht ein Text mit seinem
     * Modul um - `:core:ui` bringt schon Schriftdateien mit -, dann pruefen sie ihn
     * lautlos nicht mehr. Deshalb fragen sie jetzt hier.
     *
     * Gibt es die Datei in einem Modul nicht, faellt sie weg statt zu stoeren: nicht jedes
     * Modul hat Texte, und schon gar nicht jede Sorte.
     */
    fun texte(verzeichnis: String, name: String = "strings.xml"): List<File> =
        resWurzeln.map { File(it, "$verzeichnis/$name") }.filter { it.isFile }
            // Leer heisst nicht "nichts zu pruefen", sondern "hier stimmt etwas nicht":
            // eine Regel, die ueber null Dateien laeuft, ist gruen und hat nichts
            // angesehen. Kein Modul mit Texten zu finden ist immer ein Fehler.
            .also {
                if (it.isEmpty()) {
                    throw AssertionError(
                        "Kein Modul hat $verzeichnis/$name. Jede Regel, die hier nachsieht, " +
                            "waere von jetzt an gruen, ohne etwas zu pruefen.",
                    )
                }
            }

    /**
     * Der Wert eines Textes, ueber alle Module und mit lautem Nein, wenn es ihn nicht gibt.
     *
     * Fuenf Regeln lasen dafuer `texte(sprache).first()` - also **nur** `:app`. Heute Nacht
     * sind Texte dreimal in ein anderes Modul gezogen (`a11y_chosen` nach `core:ui`, die
     * Anrufarten nach `core:system`); danach haetten diese Regeln den Text nicht mehr
     * gefunden und je nach Schreibweise laut gestolpert oder still nichts mehr geprueft.
     */
    fun textWert(name: String, sprache: String): String =
        texte(sprache).firstNotNullOfOrNull { datei ->
            Regex("""<string name="$name">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
                .find(datei.readText())?.groupValues?.get(1)
        } ?: throw AssertionError("Den Text $name gibt es in $sprache in keinem Modul.")

    /**
     * Die Sprachverzeichnisse, wie sie im Baum stehen: `values` und jedes `values-xx`.
     *
     * Gesucht statt aufgezaehlt, und das ist der Punkt. Bis zum 04.09.2026 stand hier
     * `listOf("values", "values-de")`, und dieselben zwei Namen standen fest in jeder
     * Regel von `TranslationsTest`. Eine dritte Sprache waere damit angelegt worden und
     * **von keiner einzigen Regel angesehen**: keine Schluesselpruefung, keine
     * Platzhalter, keine Striche, keine Laenge. Gruen, und nichts geprueft.
     *
     * Aufgefallen beim Planen von PLAN.md 10.4, also bevor die erste neue Sprache da war.
     * Das ist der einzige Zeitpunkt, zu dem so etwas billig ist.
     */
    fun sprachen(): List<String> =
        resWurzeln.flatMap { wurzel ->
            (wurzel.listFiles() ?: emptyArray()).filter { ordner ->
                ordner.isDirectory &&
                    (ordner.name == "values" || ordner.name.startsWith("values-")) &&
                    (File(ordner, "strings.xml").isFile || File(ordner, "plurals.xml").isFile)
            }.map { it.name }
        }.distinct().sorted()

    /** Die Sprachen ohne die Grundsprache: das, was uebersetzt sein will. */
    fun uebersetzungen(): List<String> = sprachen().filter { it != "values" }

    /**
     * Die Sprachen, die wirklich ausgeliefert werden, aus `resourceConfigurations` gelesen.
     *
     * Diese Liste im Baugeruest entscheidet, was im Archiv landet; was nicht drinsteht, wirft
     * der Bau wieder heraus. Sie ist damit die einzige Stelle, die weiss, ob eine Sprache
     * fertig ist oder noch in Arbeit.
     *
     * Der Unterschied zaehlt: Android faellt fuer einen fehlenden Text auf `values` zurueck,
     * eine halb uebersetzte Sprache **funktioniert** also und ist teils englisch. Waehrend
     * der Arbeit ist das der normale Zustand. Ausgeliefert werden darf sie so nicht. Die
     * Vollstaendigkeitsregeln haengen deshalb hieran und nicht am blossen Vorhandensein
     * eines Verzeichnisses.
     */
    fun ausgeliefert(): List<String> =
        Regex("""resourceConfigurations\s*\+=\s*listOf\(([^)]*)\)""")
            .find(File("build.gradle.kts").readText())
            ?.groupValues?.get(1)
            ?.let { Regex(""""([^"]+)"""").findAll(it).map { m -> m.groupValues[1] }.toList() }
            ?.map { if (it == "en") "values" else "values-$it" }
            ?: throw AssertionError(
                "resourceConfigurations steht nicht mehr in app/build.gradle.kts. Ohne sie " +
                    "weiss keine Regel mehr, welche Sprache ausgeliefert wird.",
            )

    /** Jede Textdatei ueber alle Module und alle Sprachen. */
    fun alleTexte(): List<File> =
        sprachen().flatMap { v ->
            listOf("strings.xml", "plurals.xml").flatMap { name ->
                resWurzeln.map { File(it, "$v/$name") }.filter { it.isFile }
            }
        }

    /** Eine Ressource ueber ihren Pfad ab `res/`, z. B. `font/atkinson_bold.ttf`. */
    fun ressource(pfad: String): File =
        resWurzeln.map { File(it, pfad) }.firstOrNull { it.exists() }
            ?: throw AssertionError("Ressource nicht gefunden: $pfad")

    /** Die Wurzeln mit Testquelltext. */
    val testWurzeln: List<File> = listOf(
        File("src/test/java"),
        File("../core/system/src/test/kotlin"),
        File("../core/model/src/test/kotlin"),
        File("../core/ui/src/test/kotlin"),
    )

    /** Jede Kotlin-Datei des Hauptquelltexts, ueber alle Module. */
    fun dateien(): List<File> = kt(wurzeln)

    /** Jede Kotlin-Datei des Testquelltexts. */
    fun testDateien(): List<File> = kt(testWurzeln)

    /**
     * Dieselbe Liste, aber mit der Zusage, dass etwas darin steht.
     *
     * Fuer Regeln, die eine Auswahl treffen und dann ueber sie laufen: trifft die Auswahl
     * nichts, laeuft die Schleife nicht, und die Regel ist gruen. Wer hier fragt, sagt
     * damit, wie viele Treffer er mindestens erwartet.
     */
    fun mindestens(treffer: List<*>, wieviele: Int, was: String): List<*> {
        if (treffer.size < wieviele) {
            throw AssertionError(
                "$was: $wieviele erwartet, ${treffer.size} gefunden. Die Regel liefe ueber " +
                    "eine zu kurze Liste und bliebe gruen, ohne das Gemeinte zu pruefen.",
            )
        }
        return treffer
    }

    /**
     * Eine einzelne Datei ueber ihren Paketpfad, z. B. `org/biglau/data/Model.kt`.
     *
     * Wer stattdessen den Pfad eines Moduls hinschreibt - `src/main/java/…` -, bindet die
     * Regel an dieses Modul. Hier faellt ein Umzug hoechstens laut auf, und meistens gar
     * nicht.
     */
    fun datei(pfad: String): File =
        // Ein Pfad, den es so schon gibt, wird genommen wie er ist: die Ressourcen liegen
        // weiter in :app, und dieselbe Regel liest oft beides - Quelltext und strings.xml.
        File(pfad).takeIf { it.isFile }
            ?: (wurzeln + testWurzeln).map { File(it, pfad) }.firstOrNull { it.isFile }
            ?: throw AssertionError("Quelltext nicht gefunden: $pfad")

    /**
     * Eine Datei ohne ihre Kommentarzeilen.
     *
     * Fuer Regeln, die mit `indexOf` oder `substringAfter` nach **Aufrufen** suchen. Ein
     * Kommentar, der denselben Namen nur erwaehnt, verschiebt sonst die gefundene Stelle.
     * Am 3.9.2026 nachgestellt: mit einem Kommentar ueber der Weiche `if (probe)` haette
     * die Regel, die den scharfen Notruf-Alarm in der Probe verhindert, einen echten
     * Fehler **durchgewinkt**.
     *
     * Nur ganze Kommentarzeilen fliegen heraus - ein `// ...` hinter Quelltext bleibt, denn
     * dort steht die Stelle ja wirklich.
     */
    fun ohneKommentare(pfad: String): String = datei(pfad)
        .readLines()
        .filterNot { istKommentarzeile(it) }
        .joinToString("\n")

    /**
     * Eine Zeile, die nur Kommentar ist.
     *
     * Die dritte Form ist die, die immer vergessen wird: der einzeilige Kommentar, der mit
     * einem Schraegstrich und zwei Sternen beginnt. Vierzehn Regeln hatten nur die ersten
     * beiden Formen und haben deshalb einen einzeiligen Kommentar fuer Quelltext gehalten.
     */
    fun istKommentarzeile(zeile: String): Boolean {
        val nackt = zeile.trim()
        return nackt.startsWith("//") || nackt.startsWith("*") || nackt.startsWith("/*")
    }

    /**
     * Der Ausschnitt zwischen zwei Marken - und ein lautes Nein, wenn eine fehlt.
     *
     * `substringAfter` gibt bei fehlender Marke **den ganzen Text** zurueck, `substringBefore`
     * auch. Eine Regel, die so schneidet, prueft danach nicht mehr das, was sie meint,
     * sondern irgendetwas - und bleibt dabei gruen. In der Nacht auf den 04.09.2026 ist das
     * zweimal passiert: `AuswahlAnsageTest` nahm den Farbtonwaehler mit, weil die Endmarke
     * hinter dem Abschnitt lag, und `FremdeAbsichtTest` haette bei einer umbenannten
     * Variablen den ganzen Rest der Datei durchsucht.
     *
     * [von] leer heisst "vom Anfang", [bis] `null` heisst "bis zum Ende". [hoechstens]
     * begrenzt zusaetzlich - ein Fenster, das nicht an einem Namen haengt.
     */
    fun ausschnitt(
        text: String,
        von: String,
        bis: String? = null,
        hoechstens: Int = Int.MAX_VALUE,
        /**
         * Die Anfangsmarke darf mehrfach vorkommen; gemeint ist die erste.
         *
         * Nur setzen, wenn das wirklich so gemeint ist. Sonst entscheidet die Reihenfolge
         * im Quelltext, welche Stelle geprueft wird - und die aendert sich beim naechsten
         * Umsortieren, ohne dass jemand es merkt.
         */
        mehrfach: Boolean = false,
    ): String {
        val ab = text.indexOf(von)
        if (von.isNotEmpty() && !mehrfach) {
            val wieOft = Regex(Regex.escape(von)).findAll(text).count()
            if (wieOft > 1) {
                throw AssertionError(
                    "Die Marke \"$von\" steht ${wieOft}mal im Text. Welche Stelle die Regel " +
                        "ansieht, entscheidet dann die Reihenfolge - und die aendert sich " +
                        "beim Umsortieren. Genauer schneiden, oder mehrfach = true setzen.",
                )
            }
        }
        if (ab < 0) {
            throw AssertionError(
                "Die Marke \"$von\" steht nicht mehr im Text. Die Regel wuerde ins Leere " +
                    "schneiden und danach gruen bleiben, ohne noch etwas zu pruefen.",
            )
        }
        val rest = text.substring(ab + von.length)
        val ende = if (bis == null) {
            rest.length
        } else {
            rest.indexOf(bis).also {
                if (it < 0) {
                    throw AssertionError(
                        "Die Endmarke \"$bis\" steht nicht mehr hinter \"$von\". Der " +
                            "Ausschnitt liefe bis zum Dateiende.",
                    )
                }
            }
        }
        return rest.take(minOf(ende, hoechstens))
    }

    private fun kt(orte: List<File>): List<File> =
        orte.flatMap { it.walkTopDown().filter { datei -> datei.extension == "kt" } }
}
