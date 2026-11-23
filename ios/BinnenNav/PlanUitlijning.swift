import Foundation
import simd

/// Zet punten van de plattegrond (meter, x/z) om naar het AR-wereldassenstelsel.
///
/// Na het scannen van een QR-code kennen we twee dingen:
///  - waar de QR-code in de AR-wereld hangt,
///  - in welke richting de muur kijkt (de normaal).
/// Op het plan kennen we dezelfde twee dingen voor die knoop. Eén draaiing rond
/// de verticale as + één verschuiving brengt dan het hele plan op zijn plaats.
struct PlanUitlijning {
    private let rotatie: simd_quatf
    private let planAnker: SIMD2<Float>
    private let wereldAnker: SIMD3<Float>

    init?(knoop: Knoop, meting: StartpuntMeting, vloerHoogte: Float) {
        guard let planNormaal = knoop.qrNormaal, simd_length(planNormaal) > 0.001 else { return nil }
        let wereldNormaal = SIMD2<Float>(meting.wereldNormaal.x, meting.wereldNormaal.z)
        guard simd_length(wereldNormaal) > 0.001 else { return nil }

        let hoek = PlanUitlijning.hoek(wereldNormaal) - PlanUitlijning.hoek(planNormaal)
        rotatie = simd_quatf(angle: hoek, axis: SIMD3<Float>(0, 1, 0))
        planAnker = knoop.positie
        // De QR-code hangt op ooghoogte; de route tekenen we op de vloer.
        wereldAnker = SIMD3<Float>(meting.wereldPositie.x, vloerHoogte, meting.wereldPositie.z)
    }

    /// Hoek van een horizontale richting (x, z) rond de y-as, in dezelfde draaizin
    /// als simd_quatf(angle:axis: [0, 1, 0]). Daardoor tellen hoeken gewoon op.
    static func hoek(_ richting: SIMD2<Float>) -> Float {
        atan2(-richting.y, richting.x)
    }

    /// Punt op het plan → punt in de AR-wereld, op de gegeven hoogte boven de vloer.
    func wereldPunt(_ planPunt: SIMD2<Float>, hoogteBovenVloer: Float = 0) -> SIMD3<Float> {
        let verschil = planPunt - planAnker
        return wereldAnker + rotatie.act(SIMD3<Float>(verschil.x, hoogteBovenVloer, verschil.y))
    }
}
