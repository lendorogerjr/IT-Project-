package routeberekening.model

enum class NodeType { EINDPUNT, ROUTE, HULP, DEUR, TRAP, LIFT, INGANG }

data class Node(
    val id: String,
    val gebouw: String,
    val type: NodeType,
    val verdieping: Int,
    val x: Double,
    val y: Double,
    val toegang: Set<Profiel> = Profiel.ALLE,
    val naam: String? = null,
    val gesloten: Boolean = false
) {
    val sleutel: String get() = "$gebouw/$id"
    val weergavenaam: String get() = naam ?: sleutel
}
