package routeberekening

import routeberekening.zoeken.Route
import kotlin.math.abs
import kotlin.system.exitProcess

// Eenvoudige testrunner zonder extra bibliotheken: bash run.sh test
// Elke stap voegt een eigen testbestand toe en een regel in main().

var geslaagd = 0
var mislukt = 0

fun controleer(naam: String, ok: Boolean, uitleg: String = "") {
    if (ok) geslaagd++ else mislukt++
    println("${if (ok) "OK  " else "FOUT"} $naam  $uitleg")
}

fun controleer(naam: String, r: Route?, kost: Double?, ids: List<String>?) {
    val ok = if (kost == null) r == null else r != null && abs(r.totaleKost - kost) < 1e-9 && r.nodes.map { it.id } == ids
    controleer(naam, ok, if (r == null) "(geen route)" else "(${r.totaleKost} via ${r.nodes.map { it.id }})")
}

fun main() {
    calculatorTests()
    kaartTests()
    nodeTests()
    println("\nResultaat: $geslaagd geslaagd, $mislukt mislukt")
    if (mislukt > 0) exitProcess(1)
}
