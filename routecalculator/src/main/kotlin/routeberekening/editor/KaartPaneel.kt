package routeberekening.editor

import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JPanel

/** Het midden van het venster: toont één plan en verwerkt de klikken op de kaart. */
class KaartPaneel(private val app: Hoofdvenster) : JPanel() {
    var modus = Modus.SELECTEREN
        set(v) {
            field = v
            repaint()
            app.toonHulp()
        }

    init {
        background = Color(205, 207, 212)
    }

    override fun paintComponent(g0: Graphics) {
        super.paintComponent(g0)
        val g = g0.create() as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color.DARK_GRAY
        g.font = Font(Font.SANS_SERIF, Font.PLAIN, 15)
        g.drawString("Hier komt de plattegrond (stap 4). Gekozen modus: ${modus.label}", 30, 40)
        g.dispose()
    }
}
