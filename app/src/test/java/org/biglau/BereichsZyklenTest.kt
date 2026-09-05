package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Welche Bereiche von `:app` zeigen im Kreis aufeinander?
 *
 * Der Modulschnitt aus `PLAN.md` 2.1 hängt genau daran. Ein Modul kann nicht von einem
 * abhängen, das wieder von ihm abhängt - solange `sms` und `notify` sich gegenseitig
 * brauchen, gibt es kein `feature:sms`. Das war bisher eine Vermutung; am 3.9.2026
 * nachgemessen waren es vier Zyklen, von denen zwei sich in einer halben Stunde auflösen
 * liessen, die beiden anderen kurz darauf (siehe STATUS.md). Seitdem gibt es **keinen**
 * mehr, und diese Regel hält das so: jeder neue Kreis fällt hier um, bevor er den
 * Modulschnitt wieder blockiert.
 *
 * Gezählt werden nur Kanten zwischen Bereichen **innerhalb** von `:app`. Ein Import, dessen
 * Ziel längst in `core:*` liegt, ist keine - beim ersten Messen wurden dadurch zwei Zyklen
 * zu viel gemeldet, weil `org.biglau.phone.PhoneNumbers` dem Namen nach in `:app` liegt und
 * in Wahrheit in `core:system`.
 */
class BereichsZyklenTest {

    /**
     * Zyklus → warum er noch steht und was ihn auflösen würde.
     *
     * Am 3.9.2026 um 04:30 leer geworden. Bleibt sie leer, kann jeder Bereich für sich ein
     * Modul werden; ein Eintrag hier ist immer eine Schuld, kein Zustand.
     */
    private val bekannt = emptyMap<Set<String>, String>()

    @Test
    fun `es gibt keine neuen kreise zwischen den bereichen`() {
        assertEquals(
            "Ein neuer Kreis zwischen zwei Bereichen. Jeder davon verhindert einen " +
                "feature:*-Schnitt (PLAN.md 2.1). Entweder auflösen, oder mit Grund in " +
                "die Liste in BereichsZyklenTest.",
            bekannt.keys,
            zyklen(),
        )
    }

    private fun zyklen(): Set<Set<String>> {
        val kanten = Areas.edges()
        return kanten.filter { (paar, _) -> kanten.containsKey(paar.second to paar.first) }
            .keys.map { setOf(it.first, it.second) }.toSet()
    }

}
