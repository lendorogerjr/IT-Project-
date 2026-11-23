package routeberekening.kosten

import routeberekening.model.Node
import routeberekening.model.NodeType
import kotlin.math.abs
import kotlin.math.hypot

fun interface Kostenmodel {
    fun stap(van: Node, naar: Node, lengte: Double?): Double
}

class StandaardKostenmodel(private val gewichten: Gewichten = Gewichten()) : Kostenmodel {

    override fun stap(van: Node, naar: Node, lengte: Double?): Double {
        if (van.gebouw == naar.gebouw && van.verdieping != naar.verdieping) {
            return gewichten.perVerdieping * abs(van.verdieping - naar.verdieping)
        }
        val meters = lengte ?: hypot(van.x - naar.x, van.y - naar.y)
        return meters / gewichten.metersPerPunt + toeslag(van, naar)
    }

    private fun toeslag(van: Node, naar: Node): Double = when {
        naar.type == NodeType.DEUR || naar.type == NodeType.INGANG -> gewichten.deur
        naar.type == NodeType.TRAP && van.type != NodeType.TRAP -> gewichten.trapInstap
        naar.type == NodeType.LIFT && van.type != NodeType.LIFT -> gewichten.liftInstap
        else -> 0.0
    }
}
