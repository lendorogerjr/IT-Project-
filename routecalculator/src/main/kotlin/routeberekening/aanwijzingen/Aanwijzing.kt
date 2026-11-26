package routeberekening.aanwijzingen

import routeberekening.model.Node

enum class Actie {
    START,
    LINKS,
    RECHTS,
    KEREN,
    DEUR,
    TRAP_OP,
    TRAP_NEER,
    LIFT_OP,
    LIFT_NEER,
    GEBOUW_BINNEN,
    GEBOUW_VERLATEN,
    AANGEKOMEN
}

data class Aanwijzing(val node: Node, val actie: Actie, val tekst: String)
