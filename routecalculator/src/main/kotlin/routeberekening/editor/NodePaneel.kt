package routeberekening.editor

import routeberekening.model.NodeType
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/** Tabblad "Node": eigenschappen van de geselecteerde node. */
class NodePaneel(private val app: Hoofdvenster) : JPanel(BorderLayout()) {
    private val id = JLabel("–")
    private val naam = JTextField()
    private val type = JComboBox(NodeType.values())
    private val gesloten = JCheckBox("Gesloten (route loopt eromheen)")
    private val toegang = JPanel()
    private val info = JLabel(" ")
    private val wis = JButton("Verwijder node")
    private var node: ENode? = null
    private var laden = false

    init {
        border = EmptyBorder(8, 8, 8, 8)
        toegang.layout = BoxLayout(toegang, BoxLayout.Y_AXIS)
        type.renderer = TypeWeergave()
        naam.document.addDocumentListener(object : DocumentListener {
            fun bij() { if (!laden) node?.let { it.naam = naam.text.trim().ifBlank { null }; app.wijzig() } }
            override fun insertUpdate(e: DocumentEvent) = bij()
            override fun removeUpdate(e: DocumentEvent) = bij()
            override fun changedUpdate(e: DocumentEvent) = bij()
        })
        type.addActionListener { if (!laden) node?.let { it.type = type.selectedItem as NodeType; app.wijzig() } }
        gesloten.addActionListener { if (!laden) node?.let { it.gesloten = gesloten.isSelected; app.wijzig() } }
        wis.addActionListener { app.verwijderGeselecteerd() }
        val uitleg = JLabel(
            "<html><body style='width:200px;color:#555'>Naam: lokaalcode voor een eindpunt, " +
                "of dezelfde naam voor één trap of lift op elke verdieping (bv. \"Trap A\"), " +
                "dan worden de verdiepingen automatisch verbonden.</body></html>"
        )
        add(formulier("Id", id, "Naam", naam, "Type", type, "Toegang", toegang, "", gesloten, "", info, "", wis, "", uitleg), BorderLayout.NORTH)
        toon(null)
    }

    fun toon(n: ENode?) {
        laden = true
        node = n
        listOf(naam, type, gesloten, wis).forEach { it.isEnabled = n != null }
        id.text = n?.id ?: "geen node geselecteerd"
        naam.text = n?.naam ?: ""
        if (n != null) type.selectedItem = n.type
        gesloten.isSelected = n?.gesloten ?: false
        toegang.removeAll()
        for (p in app.project.profielen) {
            val vak = JCheckBox(p, n?.toegang?.contains(p) ?: false)
            vak.isEnabled = n != null
            vak.addActionListener {
                if (!laden) node?.let { if (vak.isSelected) it.toegang += p else it.toegang -= p; app.wijzig() }
            }
            toegang.add(vak)
        }
        toegang.revalidate()
        toegang.repaint()
        werkInfoBij()
        laden = false
    }

    fun werkInfoBij() {
        val n = node
        info.text = if (n == null) " " else {
            val mpp = app.project.planVan(n)?.metersPerPixel ?: 1.0
            "<html>Positie: ${String.format("%.1f", n.x * mpp)} m, ${String.format("%.1f", n.y * mpp)} m<br>" +
                "Verbindingen: ${app.project.aantalVerbindingen(n)}</html>"
        }
    }
}
