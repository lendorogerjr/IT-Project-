package routeberekening

import routeberekening.editor.Json
import routeberekening.editor.Plan
import routeberekening.editor.Project
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

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
