package org.biglau.tiles

import org.biglau.data.Cell
import org.biglau.data.Screen

/** Die vier Richtungen des Steuerkreuzes. PLAN.md 10.3.2. */
enum class Richtung { LINKS, RECHTS, HOCH, RUNTER }

/**
 * Wohin der Fokus geht, gerechnet aus den Rechtecken.
 *
 * Die Zellen stehen in `config.json` in der Reihenfolge, in der sie angelegt wurden. Nach der
 * zu gehen waere einfach und falsch: der Fokus spraenge ueber den Bildschirm, und zwar bei
 * jedem Nutzer anders, je nachdem in welcher Reihenfolge er seine Kacheln belegt hat.
 *
 * Deshalb entscheidet die Geometrie. Nach rechts gilt die naechste Zelle, deren linke Kante
 * rechts der eigenen rechten liegt und deren Zeilen sich mit den eigenen ueberschneiden; nach
 * unten dasselbe eine Achse weiter.
 *
 * Reine Rechnung ohne Android, damit sie ohne Geraet zu pruefen ist. Wer den Fokus dann
 * wirklich setzt, ist die Oberflaeche.
 */
object Fokusfolge {

    /**
     * Der Nachbar in dieser Richtung, oder `null` am Rand.
     *
     * [merkspalte] ist die Spalte, aus der der Fokus urspruenglich kam. Sie zaehlt nur bei
     * [Richtung.HOCH] und [Richtung.RUNTER] und loest den Fall, der sonst erst am Geraet
     * auffaellt: wer von einer schmalen Kachel nach unten auf eine breite geht und wieder
     * hoch, will zurueck auf die schmale. Ohne Gedaechtnis gilt die linke Kante der breiten,
     * und der Fokus rutscht bei jedem Auf und Ab weiter nach links, bis er in der ersten
     * Spalte klebt.
     *
     * Am Rand kommt `null` zurueck, und dort passiert dann nichts. Der Screenwechsel bekommt
     * eigene Tasten: sonst rutscht man beim Blaettern in einer Kachelreihe unversehens auf
     * einen anderen Screen.
     */
    fun nachbar(
        zellen: List<Cell>,
        von: Cell,
        richtung: Richtung,
        merkspalte: Int? = null,
    ): Cell? = when (richtung) {
        // Die naechste Kante zuerst, bei Gleichstand die obere Zeile. Sortiert statt
        // gerechnet: eine Formel wie x * 100 + y traegt eine stille Annahme darueber, wie
        // gross ein Raster werden darf.
        Richtung.RECHTS -> waagerecht(zellen, von) { it.x >= von.x + von.w }
            .minWithOrNull(compareBy({ it.x }, { it.y }))
        Richtung.LINKS -> waagerecht(zellen, von) { it.x + it.w <= von.x }
            .minWithOrNull(compareBy({ -(it.x + it.w) }, { it.y }))
        Richtung.RUNTER -> senkrecht(zellen, von, merkspalte, { it.y >= von.y + von.h }) { it.y }
        Richtung.HOCH -> senkrecht(zellen, von, merkspalte, { it.y + it.h <= von.y }) { -(it.y + it.h) }
    }

    /**
     * Alles, was auf diesem Screen den Fokus bekommen darf: die Kacheln **und die leeren
     * Plaetze**.
     *
     * Ein leerer Platz traegt ein "Antippen" und oeffnet den Editor. Mit dem Finger erreicht
     * man ihn sofort, und genau deshalb faellt es nicht auf, wenn er mit Tasten unerreichbar
     * ist. Am 04.09.2026 am Emulator gemessen: bei offenem Ordner mit einer belegten Kachel
     * blieb der Fokus acht Tastendruecke lang stehen, weil die Rechnung nur die belegten
     * kannte.
     *
     * Das ist die Frage aus PLAN.md 10.3.6: nicht "gross genug", sondern **erreichbar**.
     */
    fun ziele(screen: Screen): List<Cell> =
        screen.cells + screen.freeSlots().map { (x, y) -> Cell(x = x, y = y) }

    /**
     * Der Platz mit dieser Nummer, gezaehlt wie gelesen. PLAN.md 10.3.3.
     *
     * Eins ist oben links, dann nach rechts, dann die naechste Zeile. Gezaehlt werden
     * **Plaetze**, nicht belegte Kacheln: wer sich merkt, dass die Apotheke die Vier ist,
     * soll die Vier behalten, auch wenn daneben eine Kachel dazukommt oder wegfaellt.
     *
     * Nur eins bis neun. Mehr Ziffern hat eine Tastatur nicht, und eine zweistellige Eingabe
     * mit Wartezeit waere eine Falle fuer langsame Haende. Bei einem groesseren Raster sind
     * die hinteren Plaetze eben nur ueber die Pfeiltasten zu erreichen.
     */
    fun nummer(zellen: List<Cell>, ziffer: Int): Cell? {
        if (ziffer !in 1..9) return null
        return zellen.sortedWith(compareBy({ it.y }, { it.x })).getOrNull(ziffer - 1)
    }

    /** Die erste Kachel, oben links. Nicht die, die zufaellig zuerst in der Liste steht. */
    fun erste(zellen: List<Cell>): Cell? = zellen.minWithOrNull(compareBy({ it.y }, { it.x }))

    /** Zeilen ueberschneiden sich: fuer die Bewegung nach links und rechts. */
    private fun waagerecht(zellen: List<Cell>, von: Cell, jenseits: (Cell) -> Boolean): List<Cell> =
        zellen.filter { it != von && jenseits(it) && it.y < von.y + von.h && von.y < it.y + it.h }

    /**
     * Die naechste Zeile in dieser Richtung, und darin die Zelle unter der gemerkten Spalte.
     *
     * Gibt es die Spalte dort nicht, gilt die naechstgelegene. Ins Leere zu laufen waere das
     * Schlimmste: ein Bildschirm ohne Fokus ist an einem Tastentelefon dasselbe wie ein
     * eingefrorener.
     */
    private fun senkrecht(
        zellen: List<Cell>,
        von: Cell,
        merkspalte: Int?,
        jenseits: (Cell) -> Boolean,
        naehe: (Cell) -> Int,
    ): Cell? {
        val kandidaten = zellen.filter { it != von && jenseits(it) }
        if (kandidaten.isEmpty()) return null
        val naechste = kandidaten.minOf(naehe)
        val zeile = kandidaten.filter { naehe(it) == naechste }
        val spalte = merkspalte ?: von.x
        return zeile.firstOrNull { spalte >= it.x && spalte < it.x + it.w }
            ?: zeile.minByOrNull { abstand(spalte, it) }
    }

    /** Wie weit die Spalte von dieser Zelle entfernt ist, null wenn sie darin liegt. */
    private fun abstand(spalte: Int, zelle: Cell): Int = when {
        spalte < zelle.x -> zelle.x - spalte
        spalte >= zelle.x + zelle.w -> spalte - (zelle.x + zelle.w - 1)
        else -> 0
    }
}
