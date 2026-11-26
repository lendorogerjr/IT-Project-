package routeberekening.model

data class Verbinding(val van: String, val naar: String)

data class Overgang(val van: String, val naar: String, val lengte: Double)
