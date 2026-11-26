package routeberekening.editor

import routeberekening.model.NodeType
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel

/** Kleuren, symbolen en tekst: de opmaak die het venster en de export delen. */
object Tekenaar {
    val routeKleur = Color(225, 0, 120)
    val startKleur = Color(0, 160, 70)

    fun kleur(t: NodeType): Color = when (t) {
        NodeType.EINDPUNT -> Color(35, 35, 35)
        NodeType.ROUTE -> Color(80, 80, 80)
        NodeType.HULP -> Color(140, 140, 140)
        NodeType.DEUR -> Color(215, 40, 40)
        NodeType.TRAP -> Color(30, 100, 225)
        NodeType.LIFT -> Color(25, 155, 70)
        NodeType.INGANG -> Color(235, 140, 0)
    }

    fun typeNaam(t: NodeType): String = when (t) {
        NodeType.EINDPUNT -> "Eindpunt (lokaal)"
        NodeType.ROUTE -> "Route"
        NodeType.HULP -> "Hulpnode"
        NodeType.DEUR -> "Deur"
        NodeType.TRAP -> "Trap"
        NodeType.LIFT -> "Lift"
        NodeType.INGANG -> "Ingang (gebouw)"
    }

    /** Symbolen uit het document "Node Plaatsing": vierkant = eindpunt, cirkel = route, streepje = hulpnode, … */
    fun node(g: Graphics2D, type: NodeType, x: Double, y: Double, r: Double, gesloten: Boolean = false) {
        val k = kleur(type)
        g.stroke = BasicStroke((r * 0.3).toFloat())
        when (type) {
            NodeType.EINDPUNT -> { g.color = k; g.fill(Rectangle2D.Double(x - r, y - r, 2 * r, 2 * r)) }
            NodeType.ROUTE -> {
                val c = Ellipse2D.Double(x - r * 0.8, y - r * 0.8, 1.6 * r, 1.6 * r)
                g.color = Color.WHITE; g.fill(c); g.color = k; g.draw(c)
            }
            NodeType.HULP -> {
                g.color = k
                g.stroke = BasicStroke((r * 0.45).toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                g.draw(Line2D.Double(x - r * 0.6, y, x + r * 0.6, y))
            }
            NodeType.DEUR -> { g.color = k; g.fill(Ellipse2D.Double(x - r * 0.8, y - r * 0.8, 1.6 * r, 1.6 * r)) }
            NodeType.TRAP, NodeType.LIFT -> {
                g.color = k
                g.fill(RoundRectangle2D.Double(x - r, y - r, 2 * r, 2 * r, r, r))
                g.color = Color.WHITE
                letter(g, if (type == NodeType.TRAP) "T" else "L", x, y, r * 1.5)
            }
            NodeType.INGANG -> {
                g.color = k
                val p = Path2D.Double()
                p.moveTo(x, y - r * 1.2); p.lineTo(x + r * 1.2, y); p.lineTo(x, y + r * 1.2); p.lineTo(x - r * 1.2, y); p.closePath()
                g.fill(p)
            }
        }
        if (gesloten) {
            g.color = Color.BLACK
            g.stroke = BasicStroke((r * 0.35).toFloat())
            g.draw(Line2D.Double(x - r * 1.2, y - r * 1.2, x + r * 1.2, y + r * 1.2))
            g.draw(Line2D.Double(x - r * 1.2, y + r * 1.2, x + r * 1.2, y - r * 1.2))
        }
    }

    fun letter(g: Graphics2D, s: String, x: Double, y: Double, grootte: Double) {
        g.font = Font(Font.SANS_SERIF, Font.BOLD, 12).deriveFont(grootte.toFloat())
        val fm = g.fontMetrics
        g.drawString(s, (x - fm.stringWidth(s) / 2.0).toFloat(), (y + fm.ascent / 2.0 - fm.descent / 3.0).toFloat())
    }

    /** Tekst met een lichte achtergrond, zodat hij leesbaar blijft op een plan. */
    fun label(g: Graphics2D, s: String, x: Double, y: Double, grootte: Double, kleur: Color = Color(20, 20, 20)) {
        g.font = Font(Font.SANS_SERIF, Font.PLAIN, 12).deriveFont(grootte.toFloat())
        val fm = g.fontMetrics
        g.color = Color(255, 255, 255, 210)
        g.fill(Rectangle2D.Double(x - 2, y - fm.ascent, fm.stringWidth(s) + 4.0, fm.height.toDouble()))
        g.color = kleur
        g.drawString(s, x.toFloat(), y.toFloat())
    }

    /** Genummerd bolletje voor een stap uit de aanwijzingen. */
    fun nummer(g: Graphics2D, s: String, x: Double, y: Double, r: Double) {
        g.font = Font(Font.SANS_SERIF, Font.BOLD, 12).deriveFont((r * 1.5).toFloat())
        val b = maxOf(r * 2.4, g.fontMetrics.stringWidth(s) + r)
        g.color = Color(30, 30, 30)
        g.fill(RoundRectangle2D.Double(x - b / 2, y - r * 1.2, b, r * 2.4, r * 2.4, r * 2.4))
        g.color = Color.WHITE
        letter(g, s, x, y, r * 1.5)
    }
}

/** Toont een NodeType met zijn leesbare naam in een keuzelijst. */
class TypeWeergave : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, sel: Boolean, focus: Boolean): Component {
        super.getListCellRendererComponent(list, value, index, sel, focus)
        if (value is NodeType) text = Tekenaar.typeNaam(value)
        return this
    }
}

/** Legende met dezelfde symbolen als op de kaart. */
class Legende : JPanel() {
    private val typen = NodeType.values().toList()

    init {
        preferredSize = Dimension(200, typen.size * 20 + 34)
        border = BorderFactory.createTitledBorder("Legende")
    }

    override fun paintComponent(g0: Graphics) {
        super.paintComponent(g0)
        val g = g0 as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        typen.forEachIndexed { i, t ->
            val y = 32.0 + i * 20
            Tekenaar.node(g, t, 22.0, y, 6.0)
            g.color = foreground
            g.font = font
            g.drawString(Tekenaar.typeNaam(t), 40, (y + 5).toInt())
        }
    }
}

/** Formulier met labels links en velden rechts: formulier("Naam", veld, "Type", keuze, …). */
fun formulier(vararg delen: Any): JPanel {
    val p = JPanel(GridBagLayout())
    val c = GridBagConstraints().apply { insets = Insets(3, 3, 3, 3) }
    for (i in delen.indices step 2) {
        c.gridy = i / 2
        c.gridx = 0; c.weightx = 0.0; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.NORTHWEST
        p.add(JLabel(delen[i] as String), c)
        c.gridx = 1; c.weightx = 1.0; c.fill = GridBagConstraints.HORIZONTAL
        p.add(delen[i + 1] as Component, c)
    }
    return p
}

/** Leest een getal, ook met een komma (3,5). */
fun leesGetal(s: String?): Double? = s?.trim()?.replace(',', '.')?.toDoubleOrNull()
