package routeberekening.model

import routeberekening.netwerk.Koppeling
import routeberekening.netwerk.Netwerk

class Campus(val gebouwen: List<Gebouw>, val overgangen: List<Overgang> = emptyList()) {
    init {
        require(gebouwen.map { it.id }.toSet().size == gebouwen.size) { "Dubbele gebouw-id's" }
    }

    fun gebouw(id: String): Gebouw =
        gebouwen.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Onbekend gebouw: $id")

    fun netwerk(gebouwIds: Set<String> = gebouwen.map { it.id }.toSet()): Netwerk {
        val gekozen = gebouwen.filter { it.id in gebouwIds }
        val nodes = gekozen.flatMap { it.nodes }
        val binnen = gekozen.flatMap { gebouw ->
            gebouw.verbindingen.map { Koppeling("${gebouw.id}/${it.van}", "${gebouw.id}/${it.naar}", null) }
        }
        val buiten = overgangen
            .filter { gebouwVan(it.van) in gebouwIds && gebouwVan(it.naar) in gebouwIds }
            .map { Koppeling(it.van, it.naar, it.lengte) }
        return Netwerk(nodes, binnen + buiten)
    }

    private fun gebouwVan(sleutel: String) = sleutel.substringBefore('/')
}
