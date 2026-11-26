package routeberekening.editor

/** Wat een klik op de kaart doet. */
enum class Modus(val label: String) {
    SELECTEREN("Selecteren"),
    PLAATSEN("Nodes plaatsen"),
    VERBINDEN("Verbinden"),
    SCHAAL("Schaal instellen")
}
