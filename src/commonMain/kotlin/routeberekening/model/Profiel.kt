package routeberekening.model

data class Profiel(val naam: String) {
    override fun toString() = naam

    companion object {
        val LEERLING = Profiel("leerling")
        val LEERKRACHT = Profiel("leerkracht")
        val ROLSTOEL = Profiel("rolstoel")
        val ALLE: Set<Profiel> = setOf(LEERLING, LEERKRACHT, ROLSTOEL)
    }
}
