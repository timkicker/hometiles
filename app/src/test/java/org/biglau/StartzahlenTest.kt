package org.biglau

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine Startzeit ohne Verfahren ist keine Messung.
 *
 * Am 03.09.2026 standen in `PLAN.md` zwei Zahlen — 2275 ms und 670 ms — und beide trugen
 * ein falsches Schild. Aufgefallen ist das erst am 04.09.2026, beim Nachmessen:
 *
 * * `am start -W` misst bei einer **Home-App** keinen Kaltstart. Android zieht BigLau nach
 *   `force-stop` binnen einer Sekunde wieder hoch; gemessen wird dann ein Prozess, der
 *   schon steht. Am Jelly 2 waren das 1189 ms zu wenig. `-S` stoppt die App als Teil des
 *   Starts, da kommt nichts dazwischen.
 * * `cmd package compile -m speed` meldet auf einer **debuggable** App `Success` und lässt
 *   den Zustand auf `quicken` stehen. „Nach voller Übersetzung" war die Zahl also nie.
 *
 * Beides sind Fehler, die sich nicht ansehen lassen: der Befehl läuft durch, die Zahl sieht
 * plausibel aus. Deshalb steht hier die Bedingung, unter der eine solche Zahl im Plan
 * stehen darf — sie muss ihr Verfahren mitbringen.
 */
class StartzahlenTest {

    private val BEFEHL = "compile -m speed"

    private val plan = File("../PLAN.md").readText()
    private val status = File("../STATUS.md").readText()

    /** Absätze: durch Leerzeilen getrennt. So weit reicht ein Schild. */
    private fun absaetze(text: String): List<String> = text.split(Regex("\n[ \t]*\n"))

    @Test
    fun `der plan misst nur mit ausdruecklichem stopp`() {
        val ohneStopp = absaetze(plan)
            .filter { "am start -W" in it }
            .filterNot { "-S" in it }
        assertTrue(
            "In PLAN.md steht `am start -W`, und im selben Absatz kommt `-S` nicht vor: " +
                ohneStopp.joinToString("\n---\n") +
                "\nBigLau ist die Home-App. Android zieht sie nach `force-stop` sofort " +
                "wieder hoch, und `am start -W` misst dann einen laufenden Prozess — am " +
                "Jelly 2 1189 ms zu billig. Mit `-S` gehoert das Stoppen zum Start.",
            ohneStopp.isEmpty(),
        )
    }

    @Test
    fun `wo eine startzeit steht steht der uebersetzungszustand daneben`() {
        val zahl = Regex("\\*\\*[0-9]{3,4} ms\\*\\*")
        val nackt = absaetze(plan)
            .filter { zahl.containsMatchIn(it) && "start" in it.lowercase() }
            .filterNot { "run-from-apk" in it && "quicken" in it }
        assertTrue(
            "Eine Startzeit in PLAN.md nennt ihren Uebersetzungszustand nicht: " +
                nackt.joinToString("\n---\n") +
                "\nDieselbe App startet am Jelly 2 in 3018 ms (`run-from-apk`) oder in " +
                "688 ms (`quicken`). Ohne den Zustand ist die Zahl beliebig.",
            nackt.isEmpty(),
        )
    }

    @Test
    fun `niemand behauptet volle uebersetzung fuer die debug-fassung`() {
        val luegen = mutableListOf<String>()
        for (text in listOf(plan, status)) {
            var i = text.indexOf(BEFEHL)
            while (i >= 0) {
                // Ein Fenster statt eines Absatzes: die Richtigstellung steht mal in
                // derselben Zeile, mal in der Ueberschrift darueber, mal im Absatz danach.
                val umfeld = text.substring(
                    maxOf(0, i - 400),
                    minOf(text.length, i + 800),
                )
                if ("quicken" !in umfeld) luegen += umfeld
                i = text.indexOf(BEFEHL, i + 1)
            }
        }
        assertTrue(
            "Ein Absatz nennt `cmd package compile -m speed`, ohne `quicken` dazuzusagen: " +
                luegen.joinToString("\n---\n") +
                "\nAuf einer debuggable App meldet der Befehl `Success` und aendert nichts; " +
                "der Zustand bleibt `quicken`. Wer das nicht dazuschreibt, notiert eine " +
                "Uebersetzung, die nie stattgefunden hat.",
            luegen.isEmpty(),
        )
    }
}
