package routeberekening

import routeberekening.editor.Json
import routeberekening.editor.Plan
import routeberekening.editor.Project
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import routeberekening.editor.ENode
import routeberekening.editor.EVerbinding
import routeberekening.editor.Voorbeeld
import routeberekening.editor.nieuweId
import routeberekening.editor.verbindTrappenEnLiften
import routeberekening.model.NodeType

// Testen voor de editor, per stap.

fun kaartTests() {
    println("--- Stap 4: kaarten inladen en opslaan")
    val json = mapOf("a" to listOf(1.0, "twee \"x\"", null, true), "b" to mapOf("c" to 2.5))
    controleer("Json heen en terug", Json.lees(Json.schrijf(json)) == json)

    val map = kotlin.io.path.createTempDirectory("kaart").toFile()
    ImageIO.write(BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB), "png", File(map, "plan0.png"))
    val p = Project()
    p.gebouwNamen["B"] = "Blok B"
    val plan = Plan("B", 0, File(map, "plan0.png"), 0, 0, 0.025, true)
    controleer("Afbeelding inladen", plan.laadBeeld() && plan.breedte == 400 && plan.hoogte == 300)
    p.plannen += plan
    val f = File(map, "kaart.json")
    p.opslaan(f)
    controleer("Afbeelding relatief opgeslagen", f.readText().contains("\"afbeelding\":\"plan0.png\""))
    val (terug, waarschuwingen) = Project.open(f)
    val t = terug.plannen.single()
    controleer("Plan terug geopend", waarschuwingen.isEmpty() && t.beeld != null && t.metersPerPixel == 0.025 && terug.gebouwNaam("B") == "Blok B")
    map.deleteRecursively()
}

fun nodeTests() {
    println("--- Stap 5: nodes")
    val v = Voorbeeld.project()
    controleer("Voorbeeld: 26 nodes en 28 verbindingen", v.nodes.size == 26 && v.verbindingen.size == 28)
    controleer("Nieuwe id", v.nieuweId("kaai", 1, NodeType.ROUTE) == "1_r1")

    val p = Project()
    p.plannen += Plan("A", 0, null, 100, 100, 0.1, true)
    for (vd in 0..2) p.nodes += ENode(p.nieuweId("A", vd, NodeType.TRAP), "A", NodeType.TRAP, vd, 10.0, 10.0, "Trap A")
    controleer("Trappen met dezelfde naam verbinden", p.verbindTrappenEnLiften() == 2 && p.verbindTrappenEnLiften() == 0)

    val f = File.createTempFile("kaart", ".json")
    v.nodes.first { it.id == "0_deurB" }.gesloten = true
    v.opslaan(f)
    val (terug, _) = Project.open(f)
    controleer("Nodes opslaan en openen", terug.nodes.size == 26 && terug.verbindingen.size == 28 &&
        terug.nodes.first { it.id == "0_lokF" }.toegang == setOf("leerkracht") && terug.nodes.first { it.id == "0_deurB" }.gesloten)
    f.delete()
}
