package routeberekening.editor

import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    System.setProperty("apple.awt.application.name", "Routecalculator")
    SwingUtilities.invokeLater {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) } catch (_: Exception) {}
        Hoofdvenster().isVisible = true
    }
}
