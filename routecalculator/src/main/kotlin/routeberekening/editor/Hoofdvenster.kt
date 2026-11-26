package routeberekening.editor

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.KeyEvent
import javax.swing.ButtonGroup
import javax.swing.DefaultListModel
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JToggleButton
import javax.swing.JToolBar
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import kotlin.system.exitProcess

/** Het hoofdvenster: werkbalk, lijst met plannen, kaart, tabbladen Node en Route, statusbalk. */
class Hoofdvenster : JFrame() {

    val kaart = KaartPaneel(this)
    private val planModel = DefaultListModel<String>()
    private val planLijst = JList(planModel)
    private val status = JLabel(" ")
    private val tabs = JTabbedPane()
    private val modusKnoppen = LinkedHashMap<Modus, JToggleButton>()

    init {
        title = "Routecalculator"
        defaultCloseOperation = EXIT_ON_CLOSE
        jMenuBar = menubalk()

        planLijst.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val links = JPanel(BorderLayout()).apply {
            preferredSize = Dimension(230, 0)
            add(JLabel("Plannen"), BorderLayout.NORTH)
            add(JScrollPane(planLijst).apply { horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER }, BorderLayout.CENTER)
        }

        tabs.addTab("Node", JLabel("Eigenschappen van de geselecteerde node (stap 5)"))
        tabs.addTab("Route", JLabel("Vertrek, bestemming en profiel (stap 7)"))
        tabs.preferredSize = Dimension(330, 0)

        val onder = JPanel(BorderLayout()).apply {
            add(status, BorderLayout.CENTER)
        }
        layout = BorderLayout()
        add(werkbalk(), BorderLayout.NORTH)
        add(links, BorderLayout.WEST)
        add(kaart, BorderLayout.CENTER)
        add(tabs, BorderLayout.EAST)
        add(onder, BorderLayout.SOUTH)
        setSize(1400, 860)
        setLocationRelativeTo(null)
        zetModus(Modus.SELECTEREN)
    }

    // ---------- opbouw van het venster ----------

    private fun menubalk(): JMenuBar {
        val cmd = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
        fun item(naam: String, toets: Int? = null, blok: () -> Unit) = JMenuItem(naam).apply {
            if (toets != null) accelerator = KeyStroke.getKeyStroke(toets, cmd)
            addActionListener { blok() }
        }
        val bestand = JMenu("Bestand").apply {
            add(item("Afsluiten", KeyEvent.VK_Q) { exitProcess(0) })
        }
        val kaartMenu = JMenu("Kaart").apply {
            add(item("(komt in stap 4)") {}.apply { isEnabled = false })
        }
        return JMenuBar().apply { add(bestand); add(kaartMenu) }
    }

    private fun werkbalk(): JToolBar {
        val balk = JToolBar()
        balk.isFloatable = false
        val groep = ButtonGroup()
        for (m in Modus.values()) {
            val knop = JToggleButton(m.label)
            knop.addActionListener { zetModus(m) }
            groep.add(knop)
            modusKnoppen[m] = knop
            balk.add(knop)
        }
        return balk
    }

    fun zetModus(m: Modus) {
        modusKnoppen[m]?.isSelected = true
        kaart.modus = m
        kaart.requestFocusInWindow()
    }

    fun toonHulp() {
        status.text = when {
            else -> "${kaart.modus.label}: komt in een volgende stap."
        }
    }

    private fun melding(s: String) { status.text = s }
}
