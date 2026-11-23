package routeberekening.aanwijzingen

import routeberekening.model.Node
import routeberekening.model.NodeType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

class AanwijzingMaker(
    private val rechtdoorGrens: Double = 30.0,
    private val keerGrens: Double = 150.0
) {

    fun maak(route: List<Node>): List<Aanwijzing> {
        val uit = mutableListOf(Aanwijzing(route.first(), Actie.START, "Start bij ${route.first().weergavenaam}"))
        var i = 1
        while (i < route.size - 1) {
            val einde = eindeVerticaleRit(route, i)
            if (einde != null) {
                uit += verticaleRit(route[i], route[einde])
                i = einde + 1
                continue
            }
            uit += aanwijzingenBij(route[i - 1], route[i], route[i + 1])
            i++
        }
        uit += Aanwijzing(route.last(), Actie.AANGEKOMEN, "Je bent aangekomen bij ${route.last().weergavenaam}")
        return uit
    }

    private fun verticaal(a: Node, b: Node) = a.gebouw == b.gebouw && a.verdieping != b.verdieping

    private fun eindeVerticaleRit(route: List<Node>, begin: Int): Int? {
        if (!verticaal(route[begin], route[begin + 1])) return null
        var einde = begin + 1
        while (einde + 1 < route.size && verticaal(route[einde], route[einde + 1])) einde++
        return einde
    }

    private fun verticaleRit(begin: Node, einde: Node): Aanwijzing {
        val omhoog = einde.verdieping > begin.verdieping
        val actie = when {
            begin.type == NodeType.LIFT && omhoog -> Actie.LIFT_OP
            begin.type == NodeType.LIFT -> Actie.LIFT_NEER
            omhoog -> Actie.TRAP_OP
            else -> Actie.TRAP_NEER
        }
        return Aanwijzing(begin, actie, "Neem ${begin.weergavenaam} naar verdieping ${einde.verdieping}")
    }

    private fun aanwijzingenBij(vorige: Node, node: Node, volgende: Node): List<Aanwijzing> = when {
        node.gebouw != vorige.gebouw ->
            listOf(Aanwijzing(node, Actie.GEBOUW_BINNEN, "Ga gebouw ${node.gebouw} binnen"))
        node.gebouw != volgende.gebouw ->
            listOf(Aanwijzing(node, Actie.GEBOUW_VERLATEN, "Verlaat gebouw ${node.gebouw}"))
        node.type == NodeType.DEUR ->
            listOf(Aanwijzing(node, Actie.DEUR, "Ga door de deur"))
        node.type == NodeType.ROUTE && vorige.verdieping == node.verdieping ->
            listOfNotNull(bocht(vorige, node, volgende))
        else -> emptyList()
    }

    private fun bocht(vorige: Node, node: Node, volgende: Node): Aanwijzing? {
        val x1 = node.x - vorige.x
        val y1 = node.y - vorige.y
        val x2 = volgende.x - node.x
        val y2 = volgende.y - node.y
        val hoek = atan2(x1 * y2 - y1 * x2, x1 * x2 + y1 * y2) * 180.0 / PI
        return when {
            abs(hoek) < rechtdoorGrens -> null
            abs(hoek) > keerGrens -> Aanwijzing(node, Actie.KEREN, "Keer om")
            hoek > 0 -> Aanwijzing(node, Actie.RECHTS, "Sla rechts af")
            else -> Aanwijzing(node, Actie.LINKS, "Sla links af")
        }
    }
}
