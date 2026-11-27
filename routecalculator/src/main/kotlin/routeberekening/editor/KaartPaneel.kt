package routeberekening.editor

import java.awt.AlphaComposite
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
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import routeberekening.model.NodeType

/** Het midden van het venster: toont één plan en verwerkt de klikken op de kaart. */
class KaartPaneel(private val app: Hoofdvenster) : JPanel() {
    var modus = Modus.SELECTEREN
        set(v) {
            field = v
            schaalPunt = null
            app.verbindVan = null
            repaint()
            app.toonHulp()
        }
    var plaatsType = NodeType.ROUTE
    var autoVerbinden = true
    private var sleepNode: ENode? = null
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
                sleepNode = null
            }
            override fun mouseMoved(e: MouseEvent) {
                muis = naarPlan(e.point)
                app.toonPositie(muis)
                if (modus != Modus.SELECTEREN || app.verbindVan != null) repaint()
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
            if (schaalPunt != null || app.verbindVan != null) { schaalPunt = null; app.verbindVan = null; app.toonHulp() }
            else app.selecteer(null)
            repaint()
        })
        toetsen.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "wis")
        toetsen.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "wis")
        actionMap.put("wis", actie { app.verwijderGeselecteerd() })
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

    private fun raak(p: Point2D.Double): ENode? {
        val pl = plan ?: return null
        val grens = 9 / zoom
        return project.nodesOp(pl)
            .minByOrNull { hypot(it.x - p.x, it.y - p.y) }
            ?.takeIf { hypot(it.x - p.x, it.y - p.y) <= grens }
    }

    /** Lijnt een nieuw punt uit op de geselecteerde node, zodat gangen recht blijven (90°). */
    private fun uitlijnen(p: Point2D.Double, rechteHoek: Boolean): Point2D.Double {
        val pl = plan ?: return p
        val s = app.geselecteerd?.takeIf { project.opPlan(it, pl) } ?: return p
        var x = p.x
        var y = p.y
        if (rechteHoek) {
            if (abs(x - s.x) < abs(y - s.y)) x = s.x else y = s.y
        } else {
            if (abs(x - s.x) * zoom < 10) x = s.x
            if (abs(y - s.y) * zoom < 10) y = s.y
        }
        return Point2D.Double(x, y)
    }

    private fun ingedrukt(e: MouseEvent) {
        requestFocusInWindow()
        if (SwingUtilities.isRightMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) { begin(e); return }
        val pl = plan ?: return
        val p = naarPlan(e.point)
        val geraakt = raak(p)
        when (modus) {
            Modus.SELECTEREN ->
                if (geraakt != null) { app.selecteer(geraakt); sleepNode = geraakt }
                else { app.selecteer(null); begin(e) }
            Modus.PLAATSEN ->
                if (geraakt != null) app.selecteer(geraakt)
                else app.plaatsNode(uitlijnen(p, e.isShiftDown), pl)
            Modus.VERBINDEN -> if (geraakt != null) app.verbindKlik(geraakt)
            Modus.SCHAAL -> {
                val eerste = schaalPunt
                if (eerste == null) schaalPunt = p
                else { schaalPunt = null; app.stelSchaalIn(pl, hypot(p.x - eerste.x, p.y - eerste.y)) }
            }
        }
        repaint()
    }

    private fun begin(e: MouseEvent) {
        panStart = e.point; panOx = ox; panOy = oy
    }

    private fun gesleept(e: MouseEvent) {
        val start = panStart
        val n = sleepNode
        if (start != null) {
            ox = panOx + (e.x - start.x)
            oy = panOy + (e.y - start.y)
        }
        else if (n != null) {
            val p = naarPlan(e.point)
            n.x = p.x; n.y = p.y
            app.nodeVerplaatst()
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
            g.drawString("Nog geen plan. Kies Kaart > Plan toevoegen, of Bestand > Voorbeeld openen.", 30, 40)
            g.dispose()
            return
        }
        g.translate(ox, oy)
        g.scale(zoom, zoom)
        val r = 6 / zoom
        val tekstGrootte = 11 / zoom
        val m = muis
        tekenAchtergrond(g, pl)
        tekenVerbindingen(g, project, pl, Color(50, 50, 60, 200), 2 / zoom)

        for (n in project.nodesOp(pl)) {
            tekenNode(g, n, r)
            if (project.gaatNaarAnderPlan(n)) {
                g.color = Color(30, 30, 30)
                g.stroke = BasicStroke((1.2 / zoom).toFloat(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                    floatArrayOf((3 / zoom).toFloat(), (2 / zoom).toFloat()), 0f)
                g.draw(Ellipse2D.Double(n.x - r * 1.7, n.y - r * 1.7, r * 3.4, r * 3.4))
            }
            val naam = n.naam
            if (!naam.isNullOrBlank()) Tekenaar.label(g, naam, n.x + r * 1.6, n.y - r * 1.2, tekstGrootte)
        }

        app.geselecteerd?.takeIf { project.opPlan(it, pl) }?.let { s ->
            g.color = Color(255, 190, 0)
            g.stroke = BasicStroke((3 / zoom).toFloat())
            g.draw(Ellipse2D.Double(s.x - r * 2, s.y - r * 2, r * 4, r * 4))
            Tekenaar.label(g, s.id, s.x + r * 1.6, s.y + r * 2.4, tekstGrootte, Color(120, 80, 0))
        }

        val strepen = BasicStroke((1.5 / zoom).toFloat(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
            floatArrayOf((5 / zoom).toFloat(), (4 / zoom).toFloat()), 0f)
        app.verbindVan?.let { v ->
            if (project.opPlan(v, pl)) {
                g.color = Color(0, 170, 210)
                g.stroke = BasicStroke((3 / zoom).toFloat())
                g.draw(Ellipse2D.Double(v.x - r * 2, v.y - r * 2, r * 4, r * 4))
                if (m != null) { g.stroke = strepen; g.draw(Line2D.Double(v.x, v.y, m.x, m.y)) }
            }
        }
        // voorproefje van de node die je gaat plaatsen
        if (modus == Modus.PLAATSEN && m != null && raak(m) == null) {
            val p = uitlijnen(m, false)
            val s = app.geselecteerd
            if (autoVerbinden && s != null && project.opPlan(s, pl)) {
                g.color = Color(90, 90, 90, 160); g.stroke = strepen
                g.draw(Line2D.Double(s.x, s.y, p.x, p.y))
            }
            val vorm = g.composite
            g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)
            Tekenaar.node(g, plaatsType, p.x, p.y, r)
            g.composite = vorm
        }
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
