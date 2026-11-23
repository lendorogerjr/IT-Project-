import Foundation
import simd

/// Soort punt op de plattegrond.
enum KnoopSoort: String, Codable {
    case lokaal   // deur van een lokaal, met een QR-code ernaast
    case gang     // punt in een gang (kruispunt, bocht, punt voor een deur)
    case trap
    case lift
}

/// Eén punt van de navigatiegraaf.
/// Coördinaten in meter op de plattegrond: x naar rechts, z naar beneden
/// (zoals je een papieren plan van bovenaf bekijkt).
struct Knoop: Identifiable, Codable, Hashable {
    let id: String
    let naam: String
    let soort: KnoopSoort
    let x: Float
    let z: Float
    let verdieping: Int
    /// Enkel voor knopen met een QR-code: de richting (x, z) waarin de QR-code kijkt,
    /// dus loodrecht op de muur, de gang in. Hiermee kennen we na het scannen de oriëntatie.
    var qrNormaal: SIMD2<Float>? = nil

    var positie: SIMD2<Float> { SIMD2(x, z) }
}

/// Graaf van het gebouw: knopen + verbindingen (gangen) met A* om de kortste route te vinden.
struct Graaf {
    struct Buur {
        let id: String
        let kost: Float
    }

    /// Een gang tussen twee knopen. Zonder kost telt de lengte in meter.
    struct Verbinding {
        let van: String
        let naar: String
        var kost: Float? = nil
    }

    let knopen: [String: Knoop]
    private let buren: [String: [Buur]]
    let gebouwNaam: String
    let verdiepingNamen: [Int: String]

    /// Extra kost (in "meter") om één verdieping te wisselen via trap of lift,
    /// als het bestand zelf geen kost opgeeft.
    static let kostPerVerdieping: Float = 10

    init(knopen lijst: [Knoop], verbindingen: [Verbinding],
         gebouwNaam: String = "Gebouw", verdiepingNamen: [Int: String] = [:]) {
        var knopen: [String: Knoop] = [:]
        for knoop in lijst { knopen[knoop.id] = knoop }

        var buren: [String: [Buur]] = [:]
        for verbinding in verbindingen {
            // Onbekende knopen worden al door GebouwLader geweigerd; hier slaan we ze gewoon over.
            guard let ka = knopen[verbinding.van], let kb = knopen[verbinding.naar] else { continue }
            let kost = verbinding.kost ?? Graaf.afstand(ka, kb)
            buren[verbinding.van, default: []].append(Buur(id: verbinding.naar, kost: kost))
            buren[verbinding.naar, default: []].append(Buur(id: verbinding.van, kost: kost))
        }
        self.knopen = knopen
        self.buren = buren
        self.gebouwNaam = gebouwNaam
        self.verdiepingNamen = verdiepingNamen
    }

    /// Lege graaf, enkel als er helemaal geen plattegrond geladen kon worden.
    static let leeg = Graaf(knopen: [], verbindingen: [], gebouwNaam: "geen plattegrond")

    /// Kost van een gang tussen twee knopen.
    /// Zelfde verdieping: lengte in meter. Andere verdieping (trap/lift): enkel een vaste straf,
    /// want de plannen van twee verdiepingen liggen niet altijd netjes op elkaar.
    static func afstand(_ a: Knoop, _ b: Knoop) -> Float {
        if a.verdieping != b.verdieping {
            return Float(abs(a.verdieping - b.verdieping)) * kostPerVerdieping
        }
        return simd_distance(a.positie, b.positie)
    }

    func naam(verdieping: Int) -> String {
        verdiepingNamen[verdieping] ?? "verdieping \(verdieping)"
    }

    /// Kortste route met A*. De heuristiek (rechte lijn) overschat nooit, dus de route is optimaal.
    func kortsteRoute(van startID: String, naar doelID: String) -> [Knoop]? {
        guard let start = knopen[startID], let doel = knopen[doelID] else { return nil }
        if startID == doelID { return [start] }

        func heuristiek(_ id: String) -> Float {
            guard let k = knopen[id] else { return .infinity }
            // Op een andere verdieping weten we niets zeker: 0 overschat nooit.
            guard k.verdieping == doel.verdieping else { return 0 }
            return simd_distance(k.positie, doel.positie)
        }

        var open: Set<String> = [startID]
        var komtVan: [String: String] = [:]
        var g: [String: Float] = [startID: 0]
        var f: [String: Float] = [startID: heuristiek(startID)]

        while let huidig = open.min(by: { (f[$0] ?? .infinity) < (f[$1] ?? .infinity) }) {
            if huidig == doelID {
                // Route terug opbouwen van doel naar start.
                var pad = [huidig]
                var stap = huidig
                while let vorige = komtVan[stap] {
                    pad.append(vorige)
                    stap = vorige
                }
                return pad.reversed().compactMap { knopen[$0] }
            }
            open.remove(huidig)
            let gHuidig = g[huidig] ?? .infinity
            for buur in buren[huidig] ?? [] {
                let nieuweG = gHuidig + buur.kost
                if nieuweG < (g[buur.id] ?? .infinity) {
                    komtVan[buur.id] = huidig
                    g[buur.id] = nieuweG
                    f[buur.id] = nieuweG + heuristiek(buur.id)
                    open.insert(buur.id)
                }
            }
        }
        return nil
    }

    /// Lengte van een route in meter (een trap of lift telt als 0 m).
    func lengte(_ route: [Knoop]) -> Float {
        zip(route, route.dropFirst()).reduce(0) {
            $1.0.verdieping == $1.1.verdieping ? $0 + simd_distance($1.0.positie, $1.1.positie) : $0
        }
    }

    /// Alle lokalen, alfabetisch (B2 voor B10).
    var lokalen: [Knoop] {
        knopen.values
            .filter { $0.soort == .lokaal }
            .sorted { $0.naam.localizedStandardCompare($1.naam) == .orderedAscending }
    }

    /// Alle verbindingen tussen twee knopen op dezelfde verdieping (elk één keer), voor de 2D-kaart.
    func verbindingenOp(verdieping: Int) -> [(Knoop, Knoop)] {
        var resultaat: [(Knoop, Knoop)] = []
        for (id, lijst) in buren {
            guard let a = knopen[id], a.verdieping == verdieping else { continue }
            for buur in lijst where buur.id > id {
                if let b = knopen[buur.id], b.verdieping == verdieping { resultaat.append((a, b)) }
            }
        }
        return resultaat
    }

    /// Alle verdiepingen die knopen hebben, van laag naar hoog.
    var verdiepingen: [Int] {
        Set(knopen.values.map(\.verdieping)).sorted()
    }

    /// Alle knopen met een QR-code (mogelijke startpunten).
    var qrKnopen: [Knoop] {
        knopen.values
            .filter { $0.qrNormaal != nil }
            .sorted { $0.naam.localizedStandardCompare($1.naam) == .orderedAscending }
    }

    /// Zoekt lokalen op (een deel van) de naam. "b 4", "B4" en "b.4" geven hetzelfde.
    func zoekLokalen(_ tekst: String) -> [Knoop] {
        let sleutel = Graaf.normaliseer(tekst)
        guard !sleutel.isEmpty else { return lokalen }
        return lokalen.filter {
            Graaf.normaliseer($0.naam).contains(sleutel) || Graaf.normaliseer($0.id).contains(sleutel)
        }
    }

    static func normaliseer(_ tekst: String) -> String {
        tekst.lowercased().filter { $0.isLetter || $0.isNumber }
    }
}
