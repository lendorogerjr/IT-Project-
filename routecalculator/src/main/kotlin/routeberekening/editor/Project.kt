package routeberekening.editor

import routeberekening.model.NodeType
import java.io.File

/** De kaart die de beheerder maakt: gebouwen, plannen (en vanaf stap 5 nodes en verbindingen). */
class Project {
    val profielen = mutableListOf("leerling", "leerkracht", "rolstoel")
    val gebouwNamen = LinkedHashMap<String, String>()
    val plannen = mutableListOf<Plan>()
    val nodes = mutableListOf<ENode>()
    val verbindingen = mutableListOf<EVerbinding>()
    var bestand: File? = null
    var gewijzigd = false

    fun gebouwNaam(id: String) = gebouwNamen[id] ?: id
    fun planLabel(p: Plan) = "${gebouwNaam(p.gebouw)} – verdieping ${p.verdieping}"
    fun plan(gebouw: String, verdieping: Int) = plannen.firstOrNull { it.gebouw == gebouw && it.verdieping == verdieping }

    // ---------- opslaan en openen (json) ----------

    fun opslaan(f: File) {
        val map = f.absoluteFile.parentFile
        val inhoud = linkedMapOf<String, Any?>(
            "formaat" to "routecalculator-kaart",
            "versie" to 1,
            "profielen" to profielen.toList(),
            "gebouwen" to gebouwNamen.map { (id, naam) -> linkedMapOf("id" to id, "naam" to naam) },
            "plannen" to plannen.map { p ->
                linkedMapOf(
                    "gebouw" to p.gebouw, "verdieping" to p.verdieping,
                    "afbeelding" to p.afbeelding?.let { relatief(map, it) },
                    "breedte" to p.breedte, "hoogte" to p.hoogte,
                    "metersPerPixel" to p.metersPerPixel, "schaalIngesteld" to p.schaalIngesteld
                )
            },
            "nodes" to nodes.map { n ->
                linkedMapOf(
                    "id" to n.id, "gebouw" to n.gebouw, "type" to n.type.name, "verdieping" to n.verdieping,
                    "x" to afronden(n.x), "y" to afronden(n.y), "naam" to n.naam,
                    "toegang" to profielen.filter { it in n.toegang }, "gesloten" to n.gesloten
                )
            },
            "verbindingen" to verbindingen.map { v ->
                linkedMapOf("van" to v.van, "naar" to v.naar, "lengte" to v.lengte)
            }
        )
        f.writeText(Json.schrijf(inhoud))
        bestand = f
        gewijzigd = false
    }

    companion object {
        private fun afronden(d: Double) = Math.round(d * 10) / 10.0

        private fun relatief(map: File, f: File): String = try {
            map.toPath().relativize(f.absoluteFile.toPath()).toString().replace('\\', '/')
        } catch (e: IllegalArgumentException) {
            f.absolutePath
        }

        /** Leest een kaart. Geeft het project en een lijst waarschuwingen (bv. ontbrekende afbeeldingen). */
        fun open(f: File): Pair<Project, List<String>> {
            val root = Json.lees(f.readText()) as? Map<*, *> ?: throw IllegalArgumentException("Geen geldige kaart")
            val p = Project()
            val waarschuwingen = mutableListOf<String>()
            val map = f.absoluteFile.parentFile
            (root["profielen"] as? List<*>)?.let { l -> p.profielen.clear(); p.profielen += l.map { it.toString() } }
            for (g in root["gebouwen"] as? List<*> ?: emptyList<Any>()) {
                g as Map<*, *>
                p.gebouwNamen[g["id"].toString()] = g["naam"]?.toString() ?: g["id"].toString()
            }
            for (o in root["plannen"] as? List<*> ?: emptyList<Any>()) {
                o as Map<*, *>
                val afb = (o["afbeelding"] as? String)?.let { val a = File(it); if (a.isAbsolute) a else File(map, it) }
                val plan = Plan(
                    o["gebouw"].toString(), (o["verdieping"] as Double).toInt(), afb,
                    (o["breedte"] as? Double)?.toInt() ?: 1200, (o["hoogte"] as? Double)?.toInt() ?: 800,
                    o["metersPerPixel"] as? Double ?: 0.05, o["schaalIngesteld"] as? Boolean ?: false
                )
                if (afb != null && (!afb.exists() || !plan.laadBeeld()))
                    waarschuwingen += "Afbeelding niet gevonden of onleesbaar: $afb"
                p.plannen += plan
            }
            for (o in root["nodes"] as? List<*> ?: emptyList<Any>()) {
                o as Map<*, *>
                p.nodes += ENode(
                    o["id"].toString(), o["gebouw"].toString(), NodeType.valueOf(o["type"].toString()),
                    (o["verdieping"] as Double).toInt(), o["x"] as Double, o["y"] as Double,
                    o["naam"] as? String,
                    (o["toegang"] as? List<*>)?.map { it.toString() }?.toMutableSet() ?: p.profielen.toMutableSet(),
                    o["gesloten"] as? Boolean ?: false
                )
            }
            for (o in root["verbindingen"] as? List<*> ?: emptyList<Any>()) {
                o as Map<*, *>
                p.verbindingen += EVerbinding(o["van"].toString(), o["naar"].toString(), o["lengte"] as? Double)
            }
            p.bestand = f
            p.gewijzigd = false
            return p to waarschuwingen
        }
    }
}
