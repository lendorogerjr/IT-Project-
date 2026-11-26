package routeberekening.model

data class Gebouw(
    val id: String,
    val naam: String,
    val nodes: List<Node>,
    val verbindingen: List<Verbinding>
) {
    init {
        require(nodes.all { it.gebouw == id }) { "Gebouw $id bevat nodes van een ander gebouw" }
    }
}
