package routeberekening.editor

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow

/** Het midden van het venster: toont één plan en verwerkt de klikken op de kaart. */
class KaartPaneel(private val app: Hoofdvenster) : JPanel() {
    var modus = Modus.SELECTEREN
        set(v) {
            field = v
            schaalPunt = null
            repaint()
            app.toonHulp()
        }
    private var zoom = 1.0
    private var ox = 20.0
    private var oy = 20.0
    private var schaalPunt: Point2D.Double? = null
    private var muis: Point2D.Double? = null
    private var panStart: Point? = null
    private var panOx = 0.0
    private var panOy = 0.0

    private val plan get() = app.huidigPlan
    private val project get() = app.project

    init {
        background = Color(205, 207, 212)
        isFocusable = true
        val muisLuisteraar = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) = ingedrukt(e)
            override fun mouseDragged(e: MouseEvent) = gesleept(e)
            override fun mouseReleased(e: MouseEvent) {
                panStart = null
            }
            override fun mouseMoved(e: MouseEvent) {
                muis = naarPlan(e.point)
                app.toonPositie(muis)
                if (modus != Modus.SELECTEREN) repaint()
            }
            override fun mouseExited(e: MouseEvent) { muis = null; app.toonPositie(null); repaint() }
            override fun mouseWheelMoved(e: MouseWheelEvent) {
                val f = 1.15.pow(-e.preciseWheelRotation)
                val nieuw = (zoom * f).coerceIn(0.03, 40.0)
                val echt = nieuw / zoom
                ox = e.x - (e.x - ox) * echt
                oy = e.y - (e.y - oy) * echt
                zoom = nieuw
                repaint()
            }
        }
        addMouseListener(muisLuisteraar)
        addMouseMotionListener(muisLuisteraar)
        addMouseWheelListener(muisLuisteraar)

        val toetsen = getInputMap(JComponent.WHEN_FOCUSED)
        toetsen.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "annuleer")
        actionMap.put("annuleer", actie {
            schaalPunt = null
            app.toonHulp()
            repaint()
        })
    }

    private fun actie(blok: () -> Unit) = object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) = blok()
    }

    /** Schermpunt naar pixel op het plan. */
    fun naarPlan(p: Point) = Point2D.Double((p.x - ox) / zoom, (p.y - oy) / zoom)

    /** Zoomt zodat het hele plan zichtbaar is. */
    fun passend() {
        val pl = plan ?: run { repaint(); return }
        if (width <= 0 || height <= 0) return
        zoom = min((width - 40.0) / pl.breedte, (height - 40.0) / pl.hoogte).coerceIn(0.03, 40.0)
        ox = (width - pl.breedte * zoom) / 2
        oy = (height - pl.hoogte * zoom) / 2
        repaint()
    }

    private fun ingedrukt(e: MouseEvent) {
        requestFocusInWindow()
        if (SwingUtilities.isRightMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) { begin(e); return }
        val pl = plan ?: return
        val p = naarPlan(e.point)
        when (modus) {
            Modus.SELECTEREN -> begin(e)
            Modus.SCHAAL -> {
                val eerste = schaalPunt
                if (eerste == null) schaalPunt = p
                else { schaalPunt = null; app.stelSchaalIn(pl, hypot(p.x - eerste.x, p.y - eerste.y)) }
            }
            else -> {}
        }
        repaint()
    }

    private fun begin(e: MouseEvent) {
        panStart = e.point; panOx = ox; panOy = oy
    }

    private fun gesleept(e: MouseEvent) {
        val start = panStart
        if (start != null) {
            ox = panOx + (e.x - start.x)
            oy = panOy + (e.y - start.y)
        }
        muis = naarPlan(e.point)
        repaint()
    }

    override fun paintComponent(g0: Graphics) {
        super.paintComponent(g0)
        val g = g0.create() as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        val pl = plan
        if (pl == null) {
            g.color = Color(60, 60, 60)
            g.font = Font(Font.SANS_SERIF, Font.PLAIN, 15)
            g.drawString("Nog geen plan. Kies Kaart > Plan toevoegen.", 30, 40)
            g.dispose()
            return
        }
        g.translate(ox, oy)
        g.scale(zoom, zoom)
        val r = 6 / zoom
        val tekstGrootte = 11 / zoom
        val m = muis
        tekenAchtergrond(g, pl)
        schaalPunt?.let { s ->
            g.color = Color(0, 120, 255)
            g.stroke = BasicStroke((2 / zoom).toFloat())
            g.draw(Ellipse2D.Double(s.x - r, s.y - r, 2 * r, 2 * r))
            if (m != null) {
                g.draw(Line2D.Double(s.x, s.y, m.x, m.y))
                Tekenaar.label(g, "${Math.round(hypot(m.x - s.x, m.y - s.y))} px", m.x + r, m.y - r, tekstGrootte)
            }
        }
        g.dispose()
    }
}
