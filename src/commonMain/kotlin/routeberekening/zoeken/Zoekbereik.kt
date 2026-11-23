package routeberekening.zoeken

import routeberekening.model.Node

fun interface Zoekbereik {
    fun toegestaan(van: Node, naar: Node): Boolean
}

object Zoekbereiken {
    val zelfdeVerdieping = Zoekbereik { a, b -> a.gebouw == b.gebouw && a.verdieping == b.verdieping }
    val zelfdeGebouw = Zoekbereik { a, b -> a.gebouw == b.gebouw }
    val overal = Zoekbereik { _, _ -> true }

    fun voor(start: Node, doel: Node): List<Zoekbereik> = when {
        start.gebouw != doel.gebouw -> listOf(overal)
        start.verdieping != doel.verdieping -> listOf(zelfdeGebouw, overal)
        else -> listOf(zelfdeVerdieping, zelfdeGebouw, overal)
    }
}
