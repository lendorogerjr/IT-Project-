package routeberekening.invoer

import routeberekening.model.Profiel
import routeberekening.netwerk.Netwerk
import routeberekening.zoeken.Route
import routeberekening.zoeken.Routezoeker

data class Aanvraag(val profiel: Profiel, val start: String, val bestemming: String)

sealed class Antwoord {
    data class Gevonden(val route: Route) : Antwoord()
    data class Mislukt(val reden: String) : Antwoord()
}

object AanvraagInvoer {
        fun laad(json: String): List<Aanvraag> {
        val ruw = Json.lees(json)
        val items = if (ruw is List<*>) ruw else listOf(ruw)
        return items.mapIndexed { i, o ->
            val m = o.alsObject("aanvraag ${i + 1}")
            val naam = m.tekst("profiel").lowercase()
            require(Profiel(naam) in Profiel.ALLE) {
                "Aanvraag ${i + 1}: onbekend profiel '$naam' (kies uit ${Profiel.ALLE.joinToString()})"
            }
            Aanvraag(Profiel(naam), m.tekst("start"), m.tekst("bestemming"))
        }
    }
}

class Navigator(private val netwerk: Netwerk, private val zoeker: Routezoeker = Routezoeker(netwerk)) {

    fun bereken(a: Aanvraag): Antwoord {
        val start = zoekNode(a.start)
        if (start is Gevonden.Geen) return Antwoord.Mislukt("Startpunt niet gevonden: ${a.start}")
        val doel = zoekNode(a.bestemming)
        when (doel) {
            is Gevonden.Geen -> return Antwoord.Mislukt("Bestemming niet gevonden: ${a.bestemming}")
            is Gevonden.Meerdere -> return Antwoord.Mislukt(
                "Bestemming '${a.bestemming}' is niet uniek: ${doel.sleutels.joinToString()}. Geef de volledige sleutel.")
            else -> {}
        }
        if (start is Gevonden.Meerdere) return Antwoord.Mislukt("Startpunt '${a.start}' is niet uniek: ${start.sleutels.joinToString()}")
        val route = zoeker.zoek((start as Gevonden.Een).sleutel, (doel as Gevonden.Een).sleutel, a.profiel)
            ?: return Antwoord.Mislukt("Geen route voor profiel ${a.profiel} (geen toegang of een afgesloten deur of lift)")
        return Antwoord.Gevonden(route)
    }

    private sealed class Gevonden {
        object Geen : Gevonden()
        data class Een(val sleutel: String) : Gevonden()
        data class Meerdere(val sleutels: List<String>) : Gevonden()
    }

    private fun zoekNode(tekst: String): Gevonden {
        if (netwerk.bestaat(tekst)) return Gevonden.Een(tekst)
        val treffers = netwerk.nodes().filter { it.naam.equals(tekst, ignoreCase = true) }.map { it.sleutel }
        return when (treffers.size) { 0 -> Gevonden.Geen; 1 -> Gevonden.Een(treffers[0]); else -> Gevonden.Meerdere(treffers) }
    }
}
