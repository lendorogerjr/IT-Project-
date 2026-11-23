package routeberekening.toegang

import routeberekening.model.Node
import routeberekening.model.Profiel

fun interface Toegangsregel {
    fun mag(node: Node, profiel: Profiel): Boolean
}

object StandaardToegang : Toegangsregel {
    override fun mag(node: Node, profiel: Profiel) = profiel in node.toegang && !node.gesloten
}
