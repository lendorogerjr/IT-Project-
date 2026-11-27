package routeberekening.editor

import routeberekening.model.NodeType
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.geom.Point2D
import java.io.File
import javax.swing.ButtonGroup
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTabbedPane
import javax.swing.JTextField
import javax.swing.JToggleButton
import javax.swing.JToolBar
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.SpinnerNumberModel
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.system.exitProcess

/** Het hoofdvenster: werkbalk, lijst met plannen, kaart, tabbladen Node en Route, statusbalk. */
class Hoofdvenster : JFrame() {
    var project = Project()
        private set
    var huidigPlan: Plan? = null
    private var laatsteMap: File? = null
    var geselecteerd: ENode? = null
        private set
    var verbindVan: ENode? = null

    val kaart = KaartPaneel(this)
    private val nodePaneel = NodePaneel(this)
    private val planModel = DefaultListModel<Plan>()
    private val planLijst = JList(planModel)
    private val status = JLabel(" ")
    private val positie = JLabel(" ")
    private val tabs = JTabbedPane()
    private val modusKnoppen = LinkedHashMap<Modus, JToggleButton>()
    private val typeKeuze = JComboBox(NodeType.values())

    init {
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) { if (magVerdergaan()) exitProcess(0) }
        })
        jMenuBar = menubalk()

        planLijst.selectionMode = ListSelectionModel.SINGLE_SELECTION
        planLijst.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, sel: Boolean, focus: Boolean): Component {
                super.getListCellRendererComponent(list, value, index, sel, focus)
                val p = value as Plan
                val opRoute = false
                text = "<html><b>Verdieping ${p.verdieping}</b>" +
                    (if (opRoute) " <font color='#e10078'>● route</font>" else "") +
                    "<br><font color='#666666'>${project.gebouwNaam(p.gebouw)}</font>" +
                    (if (!p.schaalIngesteld) "<br><font color='#b00000'>schaal nog instellen</font>" else "") + "</html>"
                border = EmptyBorder(4, 6, 4, 6)
                return this
            }
        }
        planLijst.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                val p = planLijst.selectedValue
                if (p != null && p !== huidigPlan) { huidigPlan = p; kaart.passend(); toonHulp() }
            }
        }
        val links = JPanel(BorderLayout()).apply {
            preferredSize = Dimension(230, 0)
            add(JLabel("  Plannen").apply { border = EmptyBorder(6, 0, 6, 0) }, BorderLayout.NORTH)
            add(JScrollPane(planLijst).apply { horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER }, BorderLayout.CENTER)
            add(Legende(), BorderLayout.SOUTH)
        }

        tabs.addTab("Node", JScrollPane(nodePaneel).apply {
            border = null
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        })
        tabs.addTab("Route", JLabel("Vertrek, bestemming en profiel (stap 7)"))
        tabs.preferredSize = Dimension(330, 0)

        val onder = JPanel(BorderLayout()).apply {
            border = EmptyBorder(4, 10, 4, 10)
            add(status, BorderLayout.CENTER)
            add(positie, BorderLayout.EAST)
        }
        layout = BorderLayout()
        add(werkbalk(), BorderLayout.NORTH)
        add(links, BorderLayout.WEST)
        add(kaart, BorderLayout.CENTER)
        add(tabs, BorderLayout.EAST)
        add(onder, BorderLayout.SOUTH)
        setSize(1400, 860)
        setLocationRelativeTo(null)
        laad(Project())
        zetModus(Modus.PLAATSEN)
    }

    // ---------- opbouw van het venster ----------

    private fun menubalk(): JMenuBar {
        val cmd = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
        fun item(naam: String, toets: Int? = null, blok: () -> Unit) = JMenuItem(naam).apply {
            if (toets != null) accelerator = KeyStroke.getKeyStroke(toets, cmd)
            addActionListener { blok() }
        }
        val bestand = JMenu("Bestand").apply {
            add(item("Nieuwe kaart", KeyEvent.VK_N) { if (magVerdergaan()) laad(Project()) })
            add(item("Openen…", KeyEvent.VK_O) { openen() })
            add(item("Opslaan", KeyEvent.VK_S) { opslaan(false) })
            add(item("Opslaan als…") { opslaan(true) })
            addSeparator()
            add(item("Voorbeeld openen (testnetwerk)") { if (magVerdergaan()) laad(Voorbeeld.project()) })
            addSeparator()
            add(item("Afsluiten", KeyEvent.VK_Q) { if (magVerdergaan()) exitProcess(0) })
        }
        val kaartMenu = JMenu("Kaart").apply {
            add(item("Plan toevoegen…", KeyEvent.VK_T) { planToevoegen() })
            add(item("Afbeelding van dit plan wijzigen…") { afbeeldingWijzigen() })
            add(item("Schaal van dit plan instellen") { zetModus(Modus.SCHAAL) })
            add(item("Dit plan verwijderen…") { planVerwijderen() })
            addSeparator()
            add(item("Trappen en liften tussen verdiepingen verbinden") {
                val n = project.verbindTrappenEnLiften()
                wijzig()
                melding("$n verbinding(en) tussen verdiepingen toegevoegd.")
            })
            add(item("Profielen…") { profielen() })
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
            if (m == Modus.PLAATSEN) {
                typeKeuze.renderer = TypeWeergave()
                typeKeuze.selectedItem = NodeType.ROUTE
                typeKeuze.maximumSize = Dimension(170, 30)
                typeKeuze.toolTipText = "Soort node die je plaatst"
                typeKeuze.addActionListener {
                    kaart.plaatsType = typeKeuze.selectedItem as NodeType
                    zetModus(Modus.PLAATSEN)
                }
                balk.add(typeKeuze)
                balk.addSeparator()
            }
        }
        balk.addSeparator()
        val auto = JCheckBox("Automatisch verbinden", true)
        auto.toolTipText = "Een nieuwe node wordt verbonden met de geselecteerde node, zo teken je een gang in één keer"
        auto.addActionListener { kaart.autoVerbinden = auto.isSelected }
        balk.add(auto)
        balk.addSeparator()
        balk.add(JButton("Passend").apply { addActionListener { kaart.passend() } })
        return balk
    }

    fun zetModus(m: Modus) {
        modusKnoppen[m]?.isSelected = true
        kaart.modus = m
        kaart.requestFocusInWindow()
    }

    fun toonHulp() {
        status.text = when {
            huidigPlan == null -> "Voeg eerst een plan toe (Kaart > Plan toevoegen) of open het voorbeeld (Bestand > Voorbeeld openen)."
            kaart.modus == Modus.SELECTEREN -> "Klik een node om hem te selecteren, sleep om te verplaatsen. Rechts slepen = schuiven, scrollwiel = zoomen, Delete = verwijderen."
            kaart.modus == Modus.PLAATSEN -> "Klik om een ${Tekenaar.typeNaam(kaart.plaatsType).lowercase()} te plaatsen. Lijnt uit op de geselecteerde node, Shift = rechte hoek. Klik een bestaande node om van daar verder te tekenen."
            kaart.modus == Modus.VERBINDEN && verbindVan == null -> "Klik de eerste node. Twee verbonden nodes aanklikken wist de verbinding."
            kaart.modus == Modus.VERBINDEN -> "Klik de tweede node. Je mag eerst een ander plan kiezen (trap, lift of ander gebouw). Esc = annuleren."
            kaart.modus == Modus.SCHAAL -> "Klik twee punten waarvan je de echte afstand kent, bijvoorbeeld de lengte van een gang of muur."
            else -> "${kaart.modus.label}: komt in een volgende stap."
        }
    }

    private fun melding(s: String) { status.text = s }

    // ---------- toestand ----------

    private fun laad(p: Project) {
        project = p
        geselecteerd = null
        verbindVan = null
        vulPlannen()
        huidigPlan = null
        planModel.elements().toList().firstOrNull()?.let { planLijst.setSelectedValue(it, true) }
        huidigPlan = planLijst.selectedValue
        nodePaneel.toon(null)
        titel()
        SwingUtilities.invokeLater { kaart.passend() }
        toonHulp()
    }

    private fun vulPlannen() {
        val huidig = huidigPlan
        planModel.clear()
        project.plannen.sortedWith(compareBy({ it.gebouw }, { it.verdieping })).forEach { planModel.addElement(it) }
        if (huidig != null && project.plannen.contains(huidig)) planLijst.setSelectedValue(huidig, true)
    }

    private fun titel() {
        val naam = project.bestand?.name ?: "naamloze kaart"
        title = "Routecalculator – $naam${if (project.gewijzigd) " *" else ""}"
    }

    /** Na elke wijziging aan de kaart. Vanaf stap 6: de berekende route klopt dan niet meer. */
    fun wijzig() {
        project.gewijzigd = true
        titel()
        kaart.repaint()
        planLijst.repaint()
    }

    fun toonPositie(p: Point2D.Double?) {
        val pl = huidigPlan
        positie.text = if (p == null || pl == null) " " else
            String.format("x %.1f m   y %.1f m", p.x * pl.metersPerPixel, p.y * pl.metersPerPixel)
    }

    fun stelSchaalIn(plan: Plan, pixels: Double) {
        if (pixels < 2) return
        val meters = leesGetal(JOptionPane.showInputDialog(this, "Hoeveel meter is deze lijn van ${Math.round(pixels)} pixels in werkelijkheid?", "Schaal instellen", JOptionPane.QUESTION_MESSAGE))
        if (meters == null || meters <= 0) { toonHulp(); return }
        plan.metersPerPixel = meters / pixels
        plan.schaalIngesteld = true
        wijzig()
        zetModus(Modus.PLAATSEN)
        melding(String.format("Schaal ingesteld: 1 m = %.1f pixels.", 1 / plan.metersPerPixel))
    }

    // ---------- plannen ----------

    private fun kiesAfbeelding(): File? {
        val kiezer = JFileChooser(laatsteMap)
        kiezer.dialogTitle = "Kies de plattegrond"
        kiezer.fileFilter = FileNameExtensionFilter("Afbeelding (png, jpg)", "png", "jpg", "jpeg", "gif", "bmp")
        if (kiezer.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null
        val f = kiezer.selectedFile
        laatsteMap = f.parentFile
        if (f.extension.lowercase() == "pdf") {
            JOptionPane.showMessageDialog(this, "Een pdf kan nog niet rechtstreeks ingeladen worden.\nZet de pagina eerst om naar png (op de Mac: open in Voorvertoning > Archief > Exporteer > PNG).")
            return null
        }
        return f
    }

    private fun planToevoegen() {
        val gebouwVeld = JTextField(project.gebouwNamen.keys.firstOrNull() ?: "A", 8)
        val naamVeld = JTextField(project.gebouwNamen.values.firstOrNull() ?: "Gebouw A", 18)
        val volgende = (project.plannen.filter { it.gebouw == gebouwVeld.text }.maxOfOrNull { it.verdieping } ?: -1) + 1
        val verdieping = JSpinner(SpinnerNumberModel(volgende, -5, 60, 1))
        var bestand: File? = null
        val bestandLabel = JLabel("geen (leeg raster)")
        val kies = JButton("Kies afbeelding…").apply {
            addActionListener { kiesAfbeelding()?.let { bestand = it; bestandLabel.text = it.name } }
        }
        val paneel = formulier(
            "Gebouwcode", gebouwVeld, "Gebouwnaam", naamVeld, "Verdieping", verdieping,
            "Plattegrond", JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply { add(kies); add(JLabel("  ")); add(bestandLabel) }
        )
        if (JOptionPane.showConfirmDialog(this, paneel, "Plan toevoegen", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return
        val g = gebouwVeld.text.trim()
        val v = verdieping.value as Int
        if (g.isBlank() || '/' in g) { JOptionPane.showMessageDialog(this, "Geef een gebouwcode zonder \"/\"."); return }
        if (project.plan(g, v) != null) { JOptionPane.showMessageDialog(this, "Er is al een plan voor gebouw $g, verdieping $v."); return }
        val plan = Plan(g, v, bestand, 1200, 800, 0.05, bestand == null)
        if (!plan.laadBeeld()) { JOptionPane.showMessageDialog(this, "De afbeelding kan niet gelezen worden."); return }
        project.gebouwNamen[g] = naamVeld.text.trim().ifBlank { g }
        project.plannen += plan
        huidigPlan = plan
        vulPlannen()
        planLijst.setSelectedValue(plan, true)
        wijzig()
        kaart.passend()
        if (plan.beeld != null) {
            zetModus(Modus.SCHAAL)
            melding("Stel eerst de schaal in: klik twee punten waarvan je de echte afstand kent.")
        } else {
            melding("Leeg plan van 60 × 40 m toegevoegd (1 m = 20 pixels).")
        }
    }

    private fun afbeeldingWijzigen() {
        val plan = huidigPlan ?: return
        val f = kiesAfbeelding() ?: return
        val oud = plan.afbeelding
        plan.afbeelding = f
        if (!plan.laadBeeld()) {
            plan.afbeelding = oud; plan.laadBeeld()
            JOptionPane.showMessageDialog(this, "De afbeelding kan niet gelezen worden.")
            return
        }
        plan.schaalIngesteld = false
        wijzig()
        kaart.passend()
        zetModus(Modus.SCHAAL)
    }

    private fun planVerwijderen() {
        val plan = huidigPlan ?: return
        val vraag = "${project.planLabel(plan)} verwijderen, samen met ${project.nodesOp(plan).size} node(s)?"
        if (JOptionPane.showConfirmDialog(this, vraag, "Plan verwijderen", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return
        project.nodesOp(plan).forEach { project.verwijder(it) }
        project.plannen.remove(plan)
        huidigPlan = null
        vulPlannen()
        planModel.elements().toList().firstOrNull()?.let { planLijst.setSelectedValue(it, true) }
        huidigPlan = planLijst.selectedValue
        selecteer(null)
        wijzig()
        kaart.passend()
    }

    // ---------- bestanden ----------

    private fun jsonKiezer() = JFileChooser(project.bestand?.parentFile ?: laatsteMap).apply {
        fileFilter = FileNameExtensionFilter("Kaart (json)", "json")
    }

    private fun openen() {
        if (!magVerdergaan()) return
        val kiezer = jsonKiezer()
        if (kiezer.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return
        try {
            val (p, waarschuwingen) = Project.open(kiezer.selectedFile)
            laatsteMap = kiezer.selectedFile.parentFile
            laad(p)
            if (waarschuwingen.isNotEmpty()) JOptionPane.showMessageDialog(this, waarschuwingen.joinToString("\n"), "Let op", JOptionPane.WARNING_MESSAGE)
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Kan de kaart niet openen: ${e.message}", "Fout", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun opslaan(vraagNaam: Boolean): Boolean {
        var f = project.bestand
        if (vraagNaam || f == null) {
            val kiezer = jsonKiezer()
            if (kiezer.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return false
            f = kiezer.selectedFile
            if (f.extension.lowercase() != "json") f = File(f.path + ".json")
        }
        return try {
            project.opslaan(f!!)
            laatsteMap = f.parentFile
            titel()
            melding("Opgeslagen: ${f.path}")
            true
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Opslaan mislukt: ${e.message}", "Fout", JOptionPane.ERROR_MESSAGE)
            false
        }
    }

    private fun magVerdergaan(): Boolean {
        if (!project.gewijzigd) return true
        return when (JOptionPane.showConfirmDialog(this, "De kaart is gewijzigd. Eerst opslaan?", "Niet opgeslagen", JOptionPane.YES_NO_CANCEL_OPTION)) {
            JOptionPane.YES_OPTION -> opslaan(false)
            JOptionPane.NO_OPTION -> true
            else -> false
        }
    }

    // ---------- nodes ----------

    fun selecteer(n: ENode?) {
        geselecteerd = n
        nodePaneel.toon(n)
        kaart.repaint()
    }

    fun nodeVerplaatst() {
        wijzig()
        nodePaneel.werkInfoBij()
    }

    fun plaatsNode(p: Point2D.Double, plan: Plan) {
        val type = kaart.plaatsType
        var naam: String? = null
        if (type == NodeType.EINDPUNT || type == NodeType.TRAP || type == NodeType.LIFT || type == NodeType.INGANG) {
            val vraag = when (type) {
                NodeType.EINDPUNT -> "Lokaalcode of naam van het eindpunt (bv. B2.14):"
                NodeType.INGANG -> "Naam van de ingang (bv. Hoofdingang):"
                else -> "Naam van de ${Tekenaar.typeNaam(type).lowercase()}. Geef op elke verdieping dezelfde naam (bv. ${if (type == NodeType.TRAP) "Trap A" else "Lift 1"}):"
            }
            naam = JOptionPane.showInputDialog(this, vraag, Tekenaar.typeNaam(type), JOptionPane.QUESTION_MESSAGE) ?: return
            naam = naam.trim().ifBlank { null }
        }
        val toegang = project.profielen.toMutableSet()
        if (type == NodeType.TRAP) toegang.remove("rolstoel")
        val n = ENode(project.nieuweId(plan.gebouw, plan.verdieping, type), plan.gebouw, type, plan.verdieping, p.x, p.y, naam, toegang)
        project.nodes += n
        val vorige = geselecteerd
        if (kaart.autoVerbinden && vorige != null && project.opPlan(vorige, plan)) project.verbindingen += EVerbinding(vorige.sleutel, n.sleutel)
        val extra = if (type == NodeType.TRAP || type == NodeType.LIFT) project.verbindTrappenEnLiften() else 0
        selecteer(n)
        wijzig()
        if (extra > 0) melding("${n.naam} is automatisch verbonden met dezelfde ${Tekenaar.typeNaam(type).lowercase()} op een andere verdieping.")
        else if (type == NodeType.TRAP && "rolstoel" in project.profielen) melding("Trap geplaatst. Het profiel rolstoel heeft standaard geen toegang tot een trap.")
    }

    fun verbindKlik(n: ENode) {
        val a = verbindVan
        when {
            a == null -> { verbindVan = n; toonHulp() }
            a === n -> { verbindVan = null; toonHulp() }
            project.verbonden(a.sleutel, n.sleutel) -> {
                project.verbindingen.removeAll { it.verbindt(a.sleutel, n.sleutel) }
                verbindVan = null
                wijzig()
                melding("Verbinding ${a.id} – ${n.id} verwijderd.")
            }
            else -> {
                var lengte: Double? = null
                if (a.gebouw != n.gebouw) {
                    lengte = leesGetal(JOptionPane.showInputDialog(this, "Deze verbinding gaat tussen twee gebouwen.\nHoeveel meter is het lopen van ${a.naam ?: a.id} naar ${n.naam ?: n.id}?", "Afstand tussen gebouwen", JOptionPane.QUESTION_MESSAGE))
                    if (lengte == null || lengte <= 0) { verbindVan = null; toonHulp(); return }
                }
                project.verbindingen += EVerbinding(a.sleutel, n.sleutel, lengte)
                verbindVan = null
                wijzig()
                val waarschuwing = if (a.gebouw == n.gebouw && a.verdieping != n.verdieping && !(a.type == n.type && (a.type == NodeType.TRAP || a.type == NodeType.LIFT)))
                    " Let op: tussen verdiepingen hoort een verbinding van trap naar trap of lift naar lift." else ""
                melding("Verbinding ${a.id} – ${n.id} gemaakt.$waarschuwing")
            }
        }
        kaart.repaint()
    }

    fun verwijderGeselecteerd() {
        val n = geselecteerd ?: return
        project.verwijder(n)
        if (verbindVan === n) verbindVan = null
        selecteer(null)
        wijzig()
        melding("Node ${n.id} verwijderd.")
    }

    private fun profielen() {
        val antwoord = JOptionPane.showInputDialog(
            this, "Profielen, gescheiden door komma's (bv. leerling, leerkracht, rolstoel, personeel).\nEen nieuw profiel krijgt overal toegang; pas daarna per node aan.",
            project.profielen.joinToString(", ")
        ) ?: return
        val nieuw = antwoord.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (nieuw.isEmpty()) return
        val toegevoegd = nieuw - project.profielen.toSet()
        val weg = project.profielen.toSet() - nieuw.toSet()
        for (n in project.nodes) { n.toegang += toegevoegd; n.toegang -= weg }
        project.profielen.clear()
        project.profielen += nieuw
        nodePaneel.toon(geselecteerd)
        wijzig()
    }
}
