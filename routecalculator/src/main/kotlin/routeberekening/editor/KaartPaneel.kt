package routeberekening.editor

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
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
    }

    override fun paintComponent(g0: Graphics) {
        super.paintComponent(g0)
        val g = g0.create() as Graphics2D
        g.color = Color.DARK_GRAY
        g.drawString("Hier komt de plattegrond (stap 4). Gekozen modus: ${modus.label}", 30, 40)
        g.dispose()
    }
}
