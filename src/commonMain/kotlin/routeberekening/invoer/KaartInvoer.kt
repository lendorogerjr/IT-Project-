package routeberekening.invoer

import routeberekening.model.Campus
import routeberekening.model.Gebouw
import routeberekening.model.Node
import routeberekening.model.NodeType
import routeberekening.model.Overgang
import routeberekening.model.Profiel
import routeberekening.model.Verbinding

class KaartResultaat(val campus: Campus, val waarschuwingen: List<String>)

object KaartInvoer {

    fun laad(json: String, standaardGebouw: String = "gebouw"): KaartResultaat {
        val wortel = Json.lees(json).alsObject("de kaart")
        val gebouwen = if ("gebouwen" in wortel)
            wortel.lijst("gebouwen").mapIndexed { i, g -> gebouw(g.alsObject("gebouw ${i + 1}")) }
        else
            listOf(gebouw(wortel + mapOf("id" to (wortel["id"] ?: standaardGebouw),
                "naam" to (wortel["naam"] ?: standaardGebouw))))
        val overgangen = (wortel["overgangen"] as? List<*>).orEmpty().mapIndexed { i, o ->
            val m = o.alsObject("overgang ${i + 1}")
            Overgang(m.tekst("van"), m.tekst("naar"), m.getal("lengte"))
        }
        val campus = Campus(gebouwen, overgangen)
        val waarschuwingen = campus.netwerk().validatie()
        return KaartResultaat(campus, waarschuwingen)
    }

    private fun gebouw(m: Map<String, Any?>): Gebouw {
        val id = m.tekst("id")
        val nodes = m.lijst("nodes").mapIndexed { i, n -> node(id, n.alsObject("node ${i + 1} in $id")) }
        val verbindingen = m.lijst("verbindingen").mapIndexed { i, v ->
            val vm = v.alsObject("verbinding ${i + 1} in $id")
            Verbinding(vm.tekst("van"), vm.tekst("naar"))
        }
        return Gebouw(id, (m["naam"] as? String) ?: id, nodes, verbindingen)
    }

    private fun node(gebouw: String, m: Map<String, Any?>): Node {
        val id = m.tekst("id")
        val typeTekst = m.tekst("type")
        val type = NodeType.values().firstOrNull { it.name.equals(typeTekst, ignoreCase = true) }
            ?: throw IllegalArgumentException("Node $id: onbekend type '$typeTekst' (kies uit ${NodeType.values().joinToString { it.name.lowercase() }})")
        val toegang = (m["toegang"] as? List<*>)?.map { Profiel(it.toString().lowercase()) }?.toSet() ?: Profiel.ALLE
        return Node(
            id = id, gebouw = gebouw, type = type,
            verdieping = m.getal("verdieping").toInt(),
            x = m.getal("x"), y = m.getal("y"),
            toegang = toegang,
            naam = m["naam"] as? String,
            gesloten = m["gesloten"] as? Boolean ?: false
        )
    }
}

internal fun Any?.alsObject(wat: String): Map<String, Any?> {
    @Suppress("UNCHECKED_CAST")
    return this as? Map<String, Any?> ?: throw IllegalArgumentException("Verwachtte een object voor $wat")
}

internal fun Map<String, Any?>.tekst(sleutel: String): String =
    (this[sleutel] as? String)?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Veld '$sleutel' ontbreekt of is geen tekst")

internal fun Map<String, Any?>.getal(sleutel: String): Double =
    (this[sleutel] as? Double) ?: throw IllegalArgumentException("Veld '$sleutel' ontbreekt of is geen getal")

internal fun Map<String, Any?>.lijst(sleutel: String): List<Any?> =
    (this[sleutel] as? List<*>) ?: throw IllegalArgumentException("Veld '$sleutel' ontbreekt of is geen lijst")
