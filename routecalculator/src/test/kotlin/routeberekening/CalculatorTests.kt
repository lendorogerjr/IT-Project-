package routeberekening

import routeberekening.aanwijzingen.Actie
import routeberekening.model.Campus
import routeberekening.model.Gebouw
import routeberekening.model.Node
import routeberekening.model.NodeType
import routeberekening.model.Profiel
import routeberekening.model.Verbinding
import routeberekening.netwerk.Netwerk
import routeberekening.zoeken.Routezoeker

// Stap 1: de calculator zelf, met het testnetwerk (3 verdiepingen, 2 trappen, 1 lift, 5 lokalen).

private val GEEN_ROLSTOEL = setOf(Profiel.LEERLING, Profiel.LEERKRACHT)

fun testNodes(): List<Node> = listOf(
    Node("0_lokA", "kaai", NodeType.EINDPUNT, 0, 10.0, 20.0, naam = "Lokaal A"),
    Node("0_deurA", "kaai", NodeType.DEUR, 0, 10.0, 15.0),
    Node("0_g1", "kaai", NodeType.ROUTE, 0, 10.0, 10.0),
    Node("0_g2", "kaai", NodeType.ROUTE, 0, 30.0, 10.0),
    Node("0_g3", "kaai", NodeType.ROUTE, 0, 60.0, 10.0),
    Node("0_deurB", "kaai", NodeType.DEUR, 0, 30.0, 15.0),
    Node("0_lokB", "kaai", NodeType.EINDPUNT, 0, 30.0, 20.0, naam = "Lokaal B"),
    Node("0_deurF", "kaai", NodeType.DEUR, 0, 60.0, 15.0, toegang = setOf(Profiel.LEERKRACHT)),
    Node("0_lokF", "kaai", NodeType.EINDPUNT, 0, 60.0, 20.0, toegang = setOf(Profiel.LEERKRACHT), naam = "Leraarskamer F"),
    Node("0_trapA", "kaai", NodeType.TRAP, 0, 10.0, 0.0, toegang = GEEN_ROLSTOEL, naam = "Trap A"),
    Node("0_trapB", "kaai", NodeType.TRAP, 0, 60.0, -5.0, toegang = GEEN_ROLSTOEL, naam = "Trap B"),
    Node("0_lift", "kaai", NodeType.LIFT, 0, 30.0, 0.0, naam = "Lift"),
    Node("1_g1", "kaai", NodeType.ROUTE, 1, 10.0, 10.0),
    Node("1_g2", "kaai", NodeType.ROUTE, 1, 30.0, 10.0),
    Node("1_g3", "kaai", NodeType.ROUTE, 1, 60.0, 10.0),
    Node("1_trapA", "kaai", NodeType.TRAP, 1, 10.0, 0.0, toegang = GEEN_ROLSTOEL, naam = "Trap A"),
    Node("1_trapB", "kaai", NodeType.TRAP, 1, 60.0, -5.0, toegang = GEEN_ROLSTOEL, naam = "Trap B"),
    Node("1_lift", "kaai", NodeType.LIFT, 1, 30.0, 0.0, naam = "Lift"),
    Node("1_deurE", "kaai", NodeType.DEUR, 1, 60.0, 15.0),
    Node("1_lokE", "kaai", NodeType.EINDPUNT, 1, 60.0, 20.0, naam = "Lokaal E"),
    Node("2_g1", "kaai", NodeType.ROUTE, 2, 10.0, 10.0),
    Node("2_g3", "kaai", NodeType.ROUTE, 2, 60.0, 10.0),
    Node("2_trapA", "kaai", NodeType.TRAP, 2, 10.0, 0.0, toegang = GEEN_ROLSTOEL, naam = "Trap A"),
    Node("2_trapB", "kaai", NodeType.TRAP, 2, 60.0, -5.0, toegang = GEEN_ROLSTOEL, naam = "Trap B"),
    Node("2_deurG", "kaai", NodeType.DEUR, 2, 60.0, 15.0),
    Node("2_lokG", "kaai", NodeType.EINDPUNT, 2, 60.0, 20.0, naam = "Lokaal G")
)

val TEST_VERBINDINGEN = listOf(
    "0_lokA" to "0_deurA", "0_deurA" to "0_g1", "0_g1" to "0_g2", "0_g2" to "0_g3",
    "0_g2" to "0_deurB", "0_deurB" to "0_lokB", "0_g3" to "0_deurF", "0_deurF" to "0_lokF",
    "0_g1" to "0_trapA", "0_g3" to "0_trapB", "0_g2" to "0_lift",
    "1_g1" to "1_g2", "1_g2" to "1_g3", "1_g3" to "1_deurE", "1_deurE" to "1_lokE",
    "1_g1" to "1_trapA", "1_g3" to "1_trapB", "1_g2" to "1_lift",
    "2_g1" to "2_g3", "2_g1" to "2_trapA", "2_g3" to "2_trapB", "2_g3" to "2_deurG", "2_deurG" to "2_lokG",
    "0_trapA" to "1_trapA", "1_trapA" to "2_trapA", "0_trapB" to "1_trapB", "1_trapB" to "2_trapB",
    "0_lift" to "1_lift"
)

private fun netwerk(nodes: List<Node>, verbindingen: List<Pair<String, String>>, gebouw: String = "kaai"): Netwerk =
    Campus(listOf(Gebouw(gebouw, "Test", nodes, verbindingen.map { Verbinding(it.first, it.second) }))).netwerk()

fun calculatorTests() {
    println("--- Stap 1: calculator")
    val net = netwerk(testNodes(), TEST_VERBINDINGEN)
    val z = Routezoeker(net)
    fun zoek(a: String, b: String, p: Profiel) = z.zoek("kaai/$a", "kaai/$b", p)
    controleer("Validatie testnetwerk", net.validatie().isEmpty(), net.validatie().toString())

    val t1 = zoek("0_lokA", "0_lokB", Profiel.LEERLING)
    controleer("T1  A -> B, zelfde verdieping (14)", t1, 14.0, listOf("0_lokA", "0_deurA", "0_g1", "0_g2", "0_deurB", "0_lokB"))
    controleer("T1  aanwijzingen", t1?.aanwijzingen?.map { it.actie } ==
        listOf(Actie.START, Actie.DEUR, Actie.RECHTS, Actie.RECHTS, Actie.DEUR, Actie.AANGEKOMEN))
    controleer("T2  A -> E, leerling met de trap (49)", zoek("0_lokA", "1_lokE", Profiel.LEERLING), 49.0,
        listOf("0_lokA", "0_deurA", "0_g1", "0_trapA", "1_trapA", "1_g1", "1_g2", "1_g3", "1_deurE", "1_lokE"))
    controleer("T3  A -> E, rolstoel met de lift (84)", zoek("0_lokA", "1_lokE", Profiel.ROLSTOEL), 84.0,
        listOf("0_lokA", "0_deurA", "0_g1", "0_g2", "0_lift", "1_lift", "1_g2", "1_g3", "1_deurE", "1_lokE"))
    controleer("T4a leraarskamer, leerling: geen route", zoek("0_lokA", "0_lokF", Profiel.LEERLING), null, null)
    controleer("T4b leraarskamer, leerkracht (20)", zoek("0_lokA", "0_lokF", Profiel.LEERKRACHT), 20.0,
        listOf("0_lokA", "0_deurA", "0_g1", "0_g2", "0_g3", "0_deurF", "0_lokF"))
    controleer("T5  start = bestemming (0)", zoek("0_lokA", "0_lokA", Profiel.LEERLING), 0.0, listOf("0_lokA"))
    val t6 = zoek("0_lokA", "2_lokG", Profiel.LEERLING)
    controleer("T6  één doorlopende trap 0 -> 2 (59)", t6, 59.0,
        listOf("0_lokA", "0_deurA", "0_g1", "0_trapA", "1_trapA", "2_trapA", "2_g1", "2_g3", "2_deurG", "2_lokG"))
    controleer("T6  precies één trap-aanwijzing", t6?.aanwijzingen?.count { it.actie == Actie.TRAP_OP } == 1)

    val dicht = testNodes().map { if (it.id == "0_deurB") it.copy(gesloten = true) else it }
    controleer("T7  gesloten deur: geen route",
        Routezoeker(netwerk(dicht, TEST_VERBINDINGEN)).zoek("kaai/0_lokA", "kaai/0_lokB", Profiel.LEERLING), null, null)
    val fout = netwerk(testNodes(), TEST_VERBINDINGEN + ("0_lokA" to "0_g1")).validatie()
    controleer("T8  validatie: eindpunt met 2 verbindingen", fout.isNotEmpty(), fout.toString())
    controleer("T9  rolstoel naar verdieping 2 (geen lift): geen route", zoek("0_lokA", "2_lokG", Profiel.ROLSTOEL), null, null)

    // T10: een kortere route via een andere verdieping moet gevonden worden
    val n10 = listOf(
        Node("a", "g", NodeType.EINDPUNT, 0, 0.0, 5.0), Node("da", "g", NodeType.DEUR, 0, 0.0, 0.0),
        Node("b", "g", NodeType.EINDPUNT, 0, 20.0, 5.0), Node("db", "g", NodeType.DEUR, 0, 20.0, 0.0),
        Node("omweg", "g", NodeType.ROUTE, 0, 10.0, -500.0),
        Node("t0a", "g", NodeType.TRAP, 0, 0.0, -1.0), Node("t1a", "g", NodeType.TRAP, 1, 0.0, -1.0),
        Node("t0b", "g", NodeType.TRAP, 0, 20.0, -1.0), Node("t1b", "g", NodeType.TRAP, 1, 20.0, -1.0),
        Node("g1", "g", NodeType.ROUTE, 1, 10.0, -1.0)
    )
    val r10 = Routezoeker(netwerk(n10, listOf("a" to "da", "da" to "omweg", "omweg" to "db", "db" to "b", "da" to "t0a",
        "t0a" to "t1a", "t1a" to "g1", "g1" to "t1b", "t1b" to "t0b", "t0b" to "db"), "g")).zoek("g/a", "g/b", Profiel.LEERLING)
    controleer("T10 kortere route via verdieping 1 i.p.v. omweg", r10 != null && r10.nodes.none { it.id == "omweg" },
        "(${r10?.nodes?.map { it.id }})")

    // T11: bochten op hulpnodes geven ook een aanwijzing
    val n11 = listOf(
        Node("a", "g", NodeType.EINDPUNT, 0, 0.0, 5.0), Node("da", "g", NodeType.DEUR, 0, 0.0, 0.0),
        Node("h1", "g", NodeType.HULP, 0, 0.0, -10.0), Node("h2", "g", NodeType.HULP, 0, 20.0, -10.0),
        Node("db", "g", NodeType.DEUR, 0, 20.0, -15.0), Node("b", "g", NodeType.EINDPUNT, 0, 20.0, -20.0)
    )
    val r11 = Routezoeker(netwerk(n11, listOf("a" to "da", "da" to "h1", "h1" to "h2", "h2" to "db", "db" to "b"), "g"))
        .zoek("g/a", "g/b", Profiel.LEERLING)
    controleer("T11 bocht op hulpnode", r11?.aanwijzingen?.map { it.actie } ==
        listOf(Actie.START, Actie.DEUR, Actie.RECHTS, Actie.LINKS, Actie.DEUR, Actie.AANGEKOMEN),
        "(${r11?.aanwijzingen?.map { it.actie }})")
}
