package routeberekening.kosten

data class Gewichten(
    val metersPerPunt: Double = 5.0,
    val deur: Double = 3.0,
    val trapInstap: Double = 15.0,
    val liftInstap: Double = 50.0,
    val perVerdieping: Double = 10.0
)
