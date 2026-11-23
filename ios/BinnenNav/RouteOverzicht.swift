import Foundation
import simd

/// Samenvatting van een route voor het overzichtsscherm vóór de start.
/// Pure logica op de graaf (geen ARKit, geen SwiftUI), zodat Android exact hetzelfde
/// kan berekenen. De regels staan ook in FORMAAT.md.
struct RouteOverzicht {
    /// Een stuk van de route op één verdieping.
    struct Deel: Identifiable {
        let verdieping: Int
        let knopen: [Knoop]
        /// Gewandelde meters op deze verdieping.
        let meter: Float
        var id: Int { verdieping }
    }

    /// Gemiddelde wandelsnelheid in een gebouw (m/s).
    static let loopsnelheid: Float = 1.3
    /// Extra tijd per verdieping die je wisselt (trap of lift), in seconden.
    static let secondenPerVerdieping: Float = 30

    let route: [Knoop]
    let delen: [Deel]
    /// Totale wandelafstand in meter (trap/lift telt als 0 m).
    let meter: Float
    let verdiepingWissels: Int
    /// Geschatte looptijd in seconden.
    let seconden: Int

    init(route: [Knoop], graaf: Graaf) {
        self.route = route
        var delen: [Deel] = []
        var huidig: [Knoop] = []
        for knoop in route {
            if let laatste = huidig.last, laatste.verdieping != knoop.verdieping {
                delen.append(Deel(verdieping: laatste.verdieping, knopen: huidig, meter: graaf.lengte(huidig)))
                huidig = []
            }
            huidig.append(knoop)
        }
        if let laatste = huidig.last {
            delen.append(Deel(verdieping: laatste.verdieping, knopen: huidig, meter: graaf.lengte(huidig)))
        }
        self.delen = delen

        meter = graaf.lengte(route)
        verdiepingWissels = zip(route, route.dropFirst())
            .reduce(0) { $0 + abs($1.0.verdieping - $1.1.verdieping) }
        let tijd = meter / RouteOverzicht.loopsnelheid
            + Float(verdiepingWissels) * RouteOverzicht.secondenPerVerdieping
        seconden = Int(tijd.rounded())
    }

    var start: Knoop? { route.first }
    var doel: Knoop? { route.last }

    /// "38 m"
    var afstandTekst: String { "\(Int(meter.rounded())) m" }

    /// "< 1 min" of "± 3 min" (naar boven afgerond).
    var tijdTekst: String {
        seconden < 60 ? "< 1 min" : "± \(Int((Float(seconden) / 60).rounded(.up))) min"
    }

    /// "1 trap" / "2 verdiepingen", of nil zonder verdiepingswissel.
    var verdiepingTekst: String? {
        guard verdiepingWissels > 0 else { return nil }
        return verdiepingWissels == 1 ? "1 verdieping" : "\(verdiepingWissels) verdiepingen"
    }
}
