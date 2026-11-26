package routeberekening.netwerk

import routeberekening.model.Node
import routeberekening.model.NodeType

data class Koppeling(val van: String, val naar: String, val lengte: Double?)

data class Pad(val naar: String, val lengte: Double?)

class Netwerk(nodes: List<Node>, koppelingen: List<Koppeling>) {
    private val perSleutel: Map<String, Node> = nodes.associateBy { it.sleutel }
    private val padenPerNode: Map<String, List<Pad>>

    init {
        require(perSleutel.size == nodes.size) { "Dubbele node-sleutels in het netwerk" }
        val lijst = HashMap<String, MutableList<Pad>>()
        nodes.forEach { lijst[it.sleutel] = mutableListOf() }
        for (koppeling in koppelingen) {
            val van = perSleutel[koppeling.van]
                ?: throw IllegalArgumentException("Onbekende node in verbinding: ${koppeling.van}")
            val naar = perSleutel[koppeling.naar]
                ?: throw IllegalArgumentException("Onbekende node in verbinding: ${koppeling.naar}")
            require(van.gebouw == naar.gebouw || koppeling.lengte != null) {
                "Verbinding tussen gebouwen heeft een lengte nodig: ${koppeling.van} - ${koppeling.naar}"
            }
            lijst.getValue(koppeling.van).add(Pad(koppeling.naar, koppeling.lengte))
            lijst.getValue(koppeling.naar).add(Pad(koppeling.van, koppeling.lengte))
        }
        padenPerNode = lijst
    }

    fun bestaat(sleutel: String) = sleutel in perSleutel

    fun node(sleutel: String): Node =
        perSleutel[sleutel] ?: throw IllegalArgumentException("Onbekende node: $sleutel")

    fun paden(sleutel: String): List<Pad> = padenPerNode[sleutel] ?: emptyList()

    fun nodes(): Collection<Node> = perSleutel.values

    fun validatie(): List<String> {
        val fouten = mutableListOf<String>()
        for (node in perSleutel.values) {
            val aantal = paden(node.sleutel).size
            if (node.type == NodeType.EINDPUNT && aantal != 1)
                fouten += "Eindpunt ${node.sleutel} heeft $aantal verbindingen (moet 1 zijn)"
            if (aantal == 0)
                fouten += "Node ${node.sleutel} heeft geen enkele verbinding"
        }
        return fouten
    }
}
