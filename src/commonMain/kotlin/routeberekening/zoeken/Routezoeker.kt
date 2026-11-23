package routeberekening.zoeken

import routeberekening.aanwijzingen.AanwijzingMaker
import routeberekening.kosten.Kostenmodel
import routeberekening.kosten.StandaardKostenmodel
import routeberekening.model.NodeType
import routeberekening.model.Profiel
import routeberekening.netwerk.Netwerk
import routeberekening.toegang.StandaardToegang
import routeberekening.toegang.Toegangsregel

class Routezoeker(
    private val netwerk: Netwerk,
    private val kosten: Kostenmodel = StandaardKostenmodel(),
    private val toegang: Toegangsregel = StandaardToegang,
    private val aanwijzingen: AanwijzingMaker = AanwijzingMaker()
) {

    fun zoek(start: String, doel: String, profiel: Profiel): Route? {
        val van = netwerk.node(start)
        val naar = netwerk.node(doel)
        if (!toegang.mag(van, profiel) || !toegang.mag(naar, profiel)) return null
        return Zoekbereiken.voor(van, naar).firstNotNullOfOrNull { zoekIn(start, doel, profiel, it) }
    }

    private fun zoekIn(start: String, doel: String, profiel: Profiel, bereik: Zoekbereik): Route? {
        val kost = HashMap<String, Double>()
        val vorige = HashMap<String, String>()
        val wachtrij = MinHeap()
        kost[start] = 0.0
        wachtrij.add(start, 0.0)

        while (!wachtrij.isEmpty()) {
            val (huidige, gekend) = wachtrij.pop()
            if (gekend > (kost[huidige] ?: Double.POSITIVE_INFINITY)) continue
            if (huidige == doel) break
            val node = netwerk.node(huidige)
            if (node.type == NodeType.EINDPUNT && huidige != start) continue

            for (pad in netwerk.paden(huidige)) {
                val buur = netwerk.node(pad.naar)
                if (!toegang.mag(buur, profiel) || !bereik.toegestaan(node, buur)) continue
                val nieuw = gekend + kosten.stap(node, buur, pad.lengte)
                if (nieuw < (kost[pad.naar] ?: Double.POSITIVE_INFINITY)) {
                    kost[pad.naar] = nieuw
                    vorige[pad.naar] = huidige
                    wachtrij.add(pad.naar, nieuw)
                }
            }
        }

        val totaal = kost[doel] ?: return null
        val sleutels = generateSequence(doel) { if (it == start) null else vorige.getValue(it) }
            .toList()
            .reversed()
        val nodes = sleutels.map { netwerk.node(it) }
        return Route(nodes, totaal, aanwijzingen.maak(nodes))
    }
}
