package routeberekening.editor

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** Eén plattegrond: één verdieping van één gebouw. Coördinaten op het plan zijn in pixels. */
class Plan(
    var gebouw: String,
    var verdieping: Int,
    var afbeelding: File?,
    var breedte: Int,
    var hoogte: Int,
    var metersPerPixel: Double,
    var schaalIngesteld: Boolean
) {
    var beeld: BufferedImage? = null

    /** Laadt de afbeelding. Geeft false als het bestand er is maar niet gelezen kan worden. */
    fun laadBeeld(): Boolean {
        val f = afbeelding ?: return true
        beeld = try { ImageIO.read(f) } catch (e: Exception) { null }
        beeld?.let { breedte = it.width; hoogte = it.height }
        return beeld != null
    }
}

/** Tekent de plattegrond, of een raster van 1 m als het plan geen afbeelding heeft. */
fun tekenAchtergrond(g: Graphics2D, plan: Plan) {
    val beeld = plan.beeld
    if (beeld != null) {
        g.drawImage(beeld, 0, 0, null)
        return
    }
    g.color = Color.WHITE
    g.fill(Rectangle2D.Double(0.0, 0.0, plan.breedte.toDouble(), plan.hoogte.toDouble()))
    var stap = 1.0 / plan.metersPerPixel
    while (stap < 8) stap *= 5
    g.color = Color(234, 236, 240)
    g.stroke = BasicStroke(1f)
    var x = 0.0
    while (x <= plan.breedte) { g.draw(Line2D.Double(x, 0.0, x, plan.hoogte.toDouble())); x += stap }
    var y = 0.0
    while (y <= plan.hoogte) { g.draw(Line2D.Double(0.0, y, plan.breedte.toDouble(), y)); y += stap }
    g.color = Color(190, 190, 190)
    g.draw(Rectangle2D.Double(0.0, 0.0, plan.breedte.toDouble(), plan.hoogte.toDouble()))
}
