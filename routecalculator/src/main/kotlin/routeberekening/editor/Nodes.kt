package routeberekening.editor

import routeberekening.model.NodeType
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D

/** Een node zoals de editor hem kent: x en y in pixels op het plan. */
class ENode(
    val id: String,
    val gebouw: String,
    var type: NodeType,
    val verdieping: Int,
    var x: Double,
    var y: Double,
    var naam: String? = null,
    val toegang: MutableSet<String> = mutableSetOf(),
    var gesloten: Boolean = false
) {
    val sleutel: String get() = "$gebouw/$id"
}

/** Verbinding tussen twee node-sleutels. Een lengte (in meter) is alleen nodig tussen gebouwen. */
data class EVerbinding(val van: String, val naar: String, val lengte: Double? = null) {
    fun verbindt(a: String, b: String) = (van == a && naar == b) || (van == b && naar == a)
    fun raakt(s: String) = van == s || naar == s
}

// ---------- hulpfuncties op de kaart ----------

fun Project.planVan(n: ENode) = plan(n.gebouw, n.verdieping)
fun Project.opPlan(n: ENode, p: Plan) = n.gebouw == p.gebouw && n.verdieping == p.verdieping
fun Project.nodesOp(p: Plan) = nodes.filter { opPlan(it, p) }
fun Project.node(sleutel: String) = nodes.firstOrNull { it.sleutel == sleutel }
fun Project.verbonden(a: String, b: String) = verbindingen.any { it.verbindt(a, b) }
fun Project.aantalVerbindingen(n: ENode) = verbindingen.count { it.raakt(n.sleutel) }

fun Project.nodeLabel(n: ENode): String {
    val meerdereGebouwen = plannen.map { it.gebouw }.distinct().size > 1
    return "${n.naam ?: n.id}  (${if (meerdereGebouwen) "${n.gebouw}, " else ""}verdieping ${n.verdieping})"
}

/** Heeft de node een verbinding naar een ander plan (trap, lift of ander gebouw)? */
fun Project.gaatNaarAnderPlan(n: ENode) = verbindingen.any { v ->
    v.raakt(n.sleutel) && node(if (v.van == n.sleutel) v.naar else v.van)
        ?.let { !(it.gebouw == n.gebouw && it.verdieping == n.verdieping) } == true
}

/** Maakt een id zoals 0_r3 of 2_lok1: verdieping, soort en een volgnummer. */
fun Project.nieuweId(gebouw: String, verdieping: Int, type: NodeType): String {
    val voorvoegsel = "${verdieping}_" + when (type) {
        NodeType.EINDPUNT -> "lok"
        NodeType.ROUTE -> "r"
        NodeType.HULP -> "h"
        NodeType.DEUR -> "deur"
        NodeType.TRAP -> "trap"
        NodeType.LIFT -> "lift"
        NodeType.INGANG -> "in"
    }
    var n = 1
    while (nodes.any { it.gebouw == gebouw && it.id == "$voorvoegsel$n" }) n++
    return "$voorvoegsel$n"
}

fun Project.verwijder(n: ENode) {
    nodes.remove(n)
    verbindingen.removeAll { it.raakt(n.sleutel) }
}

/**
 * Verbindt trappen (en liften) met dezelfde naam in hetzelfde gebouw met de eerstvolgende
 * verdieping waar die trap ook staat. Zo wordt één trappenhal één doorlopende trap.
 */
fun Project.verbindTrappenEnLiften(): Int {
    var aantal = 0
    for (a in nodes.toList()) {
        if (a.type != NodeType.TRAP && a.type != NodeType.LIFT) continue
        val naam = a.naam?.takeIf { it.isNotBlank() } ?: continue
        val b = nodes
            .filter { it.gebouw == a.gebouw && it.type == a.type && it.naam == naam && it.verdieping > a.verdieping }
            .minByOrNull { it.verdieping } ?: continue
        if (!verbonden(a.sleutel, b.sleutel)) {
            verbindingen += EVerbinding(a.sleutel, b.sleutel)
            aantal++
        }
    }
    return aantal
}

// ---------- tekenen ----------

fun tekenNode(g: Graphics2D, n: ENode, r: Double) = Tekenaar.node(g, n.type, n.x, n.y, r, n.gesloten)

/** Tekent de verbindingen die volledig op dit plan liggen. */
fun tekenVerbindingen(g: Graphics2D, project: Project, plan: Plan, kleur: Color, dikte: Double) {
    g.color = kleur
    g.stroke = BasicStroke(dikte.toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    for (v in project.verbindingen) {
        val a = project.node(v.van) ?: continue
        val b = project.node(v.naar) ?: continue
        if (project.opPlan(a, plan) && project.opPlan(b, plan)) g.draw(Line2D.Double(a.x, a.y, b.x, b.y))
    }
}
