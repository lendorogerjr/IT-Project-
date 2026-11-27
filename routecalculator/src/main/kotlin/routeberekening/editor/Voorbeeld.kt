package routeberekening.editor

import routeberekening.model.NodeType

/** Het testnetwerk (T1 tot T8) als kaart: 3 verdiepingen, 2 trappen, 1 lift, 5 lokalen. 1 m = 10 pixels. */
object Voorbeeld {
    fun project(): Project {
        val p = Project()
        val g = "kaai"
        p.gebouwNamen[g] = "Campus Nijverheidskaai (voorbeeld)"
        for (v in 0..2) p.plannen += Plan(g, v, null, 750, 350, 0.1, true)
        val alle = p.profielen.toSet()
        val geenRolstoel = setOf("leerling", "leerkracht")
        fun n(id: String, t: NodeType, v: Int, x: Double, y: Double, naam: String? = null, toegang: Set<String> = alle) {
            p.nodes += ENode(id, g, t, v, x * 10, (y + 10) * 10, naam, toegang.toMutableSet())
        }
        n("0_lokA", NodeType.EINDPUNT, 0, 10.0, 20.0, "Lokaal A")
        n("0_deurA", NodeType.DEUR, 0, 10.0, 15.0)
        n("0_g1", NodeType.ROUTE, 0, 10.0, 10.0)
        n("0_g2", NodeType.ROUTE, 0, 30.0, 10.0)
        n("0_g3", NodeType.ROUTE, 0, 60.0, 10.0)
        n("0_deurB", NodeType.DEUR, 0, 30.0, 15.0)
        n("0_lokB", NodeType.EINDPUNT, 0, 30.0, 20.0, "Lokaal B")
        n("0_deurF", NodeType.DEUR, 0, 60.0, 15.0, toegang = setOf("leerkracht"))
        n("0_lokF", NodeType.EINDPUNT, 0, 60.0, 20.0, "Leraarskamer F", setOf("leerkracht"))
        n("0_trapA", NodeType.TRAP, 0, 10.0, 0.0, "Trap A", geenRolstoel)
        n("0_trapB", NodeType.TRAP, 0, 60.0, -5.0, "Trap B", geenRolstoel)
        n("0_lift", NodeType.LIFT, 0, 30.0, 0.0, "Lift")
        n("1_g1", NodeType.ROUTE, 1, 10.0, 10.0)
        n("1_g2", NodeType.ROUTE, 1, 30.0, 10.0)
        n("1_g3", NodeType.ROUTE, 1, 60.0, 10.0)
        n("1_trapA", NodeType.TRAP, 1, 10.0, 0.0, "Trap A", geenRolstoel)
        n("1_trapB", NodeType.TRAP, 1, 60.0, -5.0, "Trap B", geenRolstoel)
        n("1_lift", NodeType.LIFT, 1, 30.0, 0.0, "Lift")
        n("1_deurE", NodeType.DEUR, 1, 60.0, 15.0)
        n("1_lokE", NodeType.EINDPUNT, 1, 60.0, 20.0, "Lokaal E")
        n("2_g1", NodeType.ROUTE, 2, 10.0, 10.0)
        n("2_g3", NodeType.ROUTE, 2, 60.0, 10.0)
        n("2_trapA", NodeType.TRAP, 2, 10.0, 0.0, "Trap A", geenRolstoel)
        n("2_trapB", NodeType.TRAP, 2, 60.0, -5.0, "Trap B", geenRolstoel)
        n("2_deurG", NodeType.DEUR, 2, 60.0, 15.0)
        n("2_lokG", NodeType.EINDPUNT, 2, 60.0, 20.0, "Lokaal G")
        listOf(
            "0_lokA" to "0_deurA", "0_deurA" to "0_g1", "0_g1" to "0_g2", "0_g2" to "0_g3",
            "0_g2" to "0_deurB", "0_deurB" to "0_lokB", "0_g3" to "0_deurF", "0_deurF" to "0_lokF",
            "0_g1" to "0_trapA", "0_g3" to "0_trapB", "0_g2" to "0_lift",
            "1_g1" to "1_g2", "1_g2" to "1_g3", "1_g3" to "1_deurE", "1_deurE" to "1_lokE",
            "1_g1" to "1_trapA", "1_g3" to "1_trapB", "1_g2" to "1_lift",
            "2_g1" to "2_g3", "2_g1" to "2_trapA", "2_g3" to "2_trapB", "2_g3" to "2_deurG", "2_deurG" to "2_lokG",
            "0_trapA" to "1_trapA", "1_trapA" to "2_trapA", "0_trapB" to "1_trapB", "1_trapB" to "2_trapB",
            "0_lift" to "1_lift"
        ).forEach { (a, b) -> p.verbindingen += EVerbinding("$g/$a", "$g/$b") }
        p.gewijzigd = false
        return p
    }
}
