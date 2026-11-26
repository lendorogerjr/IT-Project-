package routeberekening.editor

import javax.swing.SwingUtilities

fun main() {
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    System.setProperty("apple.awt.application.name", "Routecalculator")
    SwingUtilities.invokeLater {
        Hoofdvenster().isVisible = true
    }
}
