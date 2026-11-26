package routeberekening.editor

import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingUtilities

fun main() {
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    System.setProperty("apple.awt.application.name", "Routecalculator")
    SwingUtilities.invokeLater {
        // Stap 1: alleen een leeg venster om te testen dat Kotlin en Swing werken.
        val venster = JFrame("Routecalculator")
        venster.add(JLabel("De opzet werkt. De editor volgt in stap 2.", JLabel.CENTER))
        venster.setSize(600, 300)
        venster.setLocationRelativeTo(null)
        venster.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        venster.isVisible = true
    }
}
