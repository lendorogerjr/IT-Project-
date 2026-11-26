package routeberekening.zoeken

import routeberekening.aanwijzingen.Aanwijzing
import routeberekening.model.Node

data class Route(
    val nodes: List<Node>,
    val totaleKost: Double,
    val aanwijzingen: List<Aanwijzing>
)
