package routeberekening

import routeberekening.invoer.Aanvraag
import routeberekening.invoer.Antwoord
import routeberekening.invoer.KaartInvoer
import routeberekening.invoer.Navigator
import routeberekening.model.Profiel
import routeberekening.netwerk.Netwerk

class Routecalculator private constructor(
    private val netwerk: Netwerk,
        val waarschuwingen: List<String>
) {
    private val navigator = Navigator(netwerk)

        fun bestemmingen(): List<Bestemming> =
        netwerk.nodes()
            .filter { it.type == routeberekening.model.NodeType.EINDPUNT && !it.gesloten }
            .sortedBy { it.weergavenaam }
            .map { Bestemming(it.sleutel, it.weergavenaam, it.gebouw, it.verdieping) }

        fun profielen(): List<String> = Profiel.ALLE.map { it.naam }.sorted()

        fun route(profiel: String, start: String, bestemming: String): RouteResultaat {
        return try {
            val p = Profiel(profiel.lowercase())
            if (p !in Profiel.ALLE) return RouteResultaat.fout("Onbekend profiel: $profiel")
            when (val a = navigator.bereken(Aanvraag(p, start, bestemming))) {
                is Antwoord.Mislukt -> RouteResultaat.fout(a.reden)
                is Antwoord.Gevonden -> {
                    val r = a.route
                    RouteResultaat(
                        gelukt = true, fout = null, totaleKost = r.totaleKost,
                        stappen = r.aanwijzingen.map { Stap(it.tekst, it.actie.name, it.node.sleutel) },
                        punten = r.nodes.map { Punt(it.sleutel, it.gebouw, it.verdieping, it.x, it.y) }
                    )
                }
            }
        } catch (e: Exception) {
            RouteResultaat.fout("Onverwachte fout: ${e.message}")
        }
    }

    companion object {
                fun vanKaart(kaartJson: String, standaardGebouw: String = "gebouw"): Routecalculator {
            val kaart = KaartInvoer.laad(kaartJson, standaardGebouw)
            return Routecalculator(kaart.campus.netwerk(), kaart.waarschuwingen)
        }

                fun probeerVanKaart(kaartJson: String, standaardGebouw: String = "gebouw"): Routecalculator? =
            try { vanKaart(kaartJson, standaardGebouw) } catch (e: Exception) { null }
    }
}

data class Bestemming(val sleutel: String, val naam: String, val gebouw: String, val verdieping: Int)

data class Stap(val tekst: String, val actie: String, val nodeSleutel: String)

data class Punt(val sleutel: String, val gebouw: String, val verdieping: Int, val x: Double, val y: Double)

data class RouteResultaat(
    val gelukt: Boolean,
    val fout: String?,
    val totaleKost: Double,
    val stappen: List<Stap>,
    val punten: List<Punt>
) {
    companion object {
        internal fun fout(tekst: String) = RouteResultaat(false, tekst, 0.0, emptyList(), emptyList())
    }
}
