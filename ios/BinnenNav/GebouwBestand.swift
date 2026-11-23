import Foundation
import simd

/// Het JSON-bestand van een gebouw, formaat "binnennav-graaf" versie 1.
/// Hetzelfde bestand wordt later door de Android-app gelezen; zie FORMAAT.md.
/// Het wordt gemaakt met de plattegrondeditor (editor/index.html).
struct GebouwBestand: Codable {
    struct Gebouw: Codable {
        let id: String
        let naam: String
    }

    /// Het plan waarmee de editor gewerkt heeft. De app gebruikt het (nog) niet,
    /// maar we lezen het mee zodat een 2D-plan later kan.
    struct Plan: Codable {
        let bestand: String?
        let breedtePx: Double?
        let hoogtePx: Double?
        let meterPerPixel: Double?
        let oorsprongPx: [Double]?
    }

    struct Verdieping: Codable {
        let niveau: Int
        let naam: String
        let plan: Plan?
    }

    struct QR: Codable {
        /// Tekst in de QR-code, altijd "BINNENNAV:<id>".
        let code: String?
        /// Richting [x, z] van de muur de gang in (eenheidsvector).
        let normaal: [Float]
    }

    struct KnoopData: Codable {
        let id: String
        let naam: String
        let soort: KnoopSoort
        let verdieping: Int
        let x: Float
        let z: Float
        let qr: QR?
    }

    struct VerbindingData: Codable {
        let van: String
        let naar: String
        /// Optioneel: vaste kost in "meter" (bv. voor een trap). Zonder kost telt de lengte.
        let kost: Float?
    }

    static let formaatNaam = "binnennav-graaf"
    static let ondersteundeVersie = 1

    let formaat: String
    let versie: Int
    let gebouw: Gebouw
    let verdiepingen: [Verdieping]
    let knopen: [KnoopData]
    let verbindingen: [VerbindingData]
}

/// Leest gebouwbestanden en maakt er een Graaf van.
///
/// Volgorde bij het opstarten:
///  1. een geïmporteerd bestand in Documenten (gebouw.json), als dat er is en geldig is;
///  2. anders het bestand gebouw.json dat in de app zit (bundle).
enum GebouwLader {
    enum Fout: LocalizedError {
        case nietGevonden
        case verkeerdFormaat(String)
        case nieuwereVersie(Int)
        case ongeldig(String)
        case onleesbaar(String)

        var errorDescription: String? {
            switch self {
            case .nietGevonden:
                return "gebouw.json zit niet in de app. Voeg het toe aan het Xcode-project."
            case .verkeerdFormaat(let formaat):
                return "Dit is geen BinnenNav-plattegrond (formaat „\(formaat)”)."
            case .nieuwereVersie(let versie):
                return "Dit bestand heeft versie \(versie); deze app kent enkel versie \(GebouwBestand.ondersteundeVersie). Werk de app bij."
            case .ongeldig(let reden):
                return "Ongeldige plattegrond: \(reden)"
            case .onleesbaar(let reden):
                return "Het bestand kon niet gelezen worden: \(reden)"
            }
        }
    }

    /// Waar een geïmporteerde plattegrond bewaard wordt.
    static var eigenBestand: URL {
        URL.documentsDirectory.appending(path: "gebouw.json")
    }

    static var heeftEigenBestand: Bool {
        FileManager.default.fileExists(atPath: eigenBestand.path())
    }

    /// Laadt de plattegrond bij het opstarten. `bron` zegt waar hij vandaan komt.
    static func laadBijStart() throws -> (graaf: Graaf, bron: String) {
        if heeftEigenBestand {
            if let graaf = try? lees(url: eigenBestand) {
                return (graaf, "geïmporteerd bestand")
            }
            // Kapot bestand: weg ermee, anders blijft de app het proberen.
            try? FileManager.default.removeItem(at: eigenBestand)
        }
        return (try standaard(), "ingebouwd")
    }

    /// Het gebouw.json-bestand dat in de app zit.
    static func standaard() throws -> Graaf {
        guard let url = Bundle.main.url(forResource: "gebouw", withExtension: "json") else {
            throw Fout.nietGevonden
        }
        return try lees(url: url)
    }

    /// Importeert een bestand dat de gebruiker koos (Bestanden, AirDrop…) en bewaart het in Documenten.
    static func importeer(van url: URL) throws -> Graaf {
        let toegang = url.startAccessingSecurityScopedResource()
        defer { if toegang { url.stopAccessingSecurityScopedResource() } }

        let data: Data
        do {
            data = try Data(contentsOf: url)
        } catch {
            throw Fout.onleesbaar(error.localizedDescription)
        }
        let graaf = try maakGraaf(uit: data)   // eerst controleren, pas daarna bewaren
        try data.write(to: eigenBestand, options: .atomic)
        return graaf
    }

    /// Vergeet de geïmporteerde plattegrond.
    static func verwijderEigenBestand() {
        try? FileManager.default.removeItem(at: eigenBestand)
    }

    static func lees(url: URL) throws -> Graaf {
        do {
            return try maakGraaf(uit: Data(contentsOf: url))
        } catch let fout as Fout {
            throw fout
        } catch {
            throw Fout.onleesbaar(error.localizedDescription)
        }
    }

    /// JSON → Graaf, met controles zodat een fout in het bestand geen crash geeft.
    static func maakGraaf(uit data: Data) throws -> Graaf {
        let bestand: GebouwBestand
        do {
            bestand = try JSONDecoder().decode(GebouwBestand.self, from: data)
        } catch let DecodingError.keyNotFound(sleutel, context) {
            throw Fout.ongeldig("veld „\(sleutel.stringValue)” ontbreekt (\(pad(context))).")
        } catch let DecodingError.typeMismatch(_, context), let DecodingError.valueNotFound(_, context) {
            throw Fout.ongeldig("verkeerde waarde bij \(pad(context)).")
        } catch let DecodingError.dataCorrupted(context) {
            if context.codingPath.last?.stringValue == "soort" {
                throw Fout.ongeldig("onbekende soort bij \(pad(context)) (lokaal, gang, trap of lift).")
            }
            throw Fout.ongeldig("geen geldige JSON.")
        } catch {
            throw Fout.onleesbaar(error.localizedDescription)
        }

        guard bestand.formaat == GebouwBestand.formaatNaam else { throw Fout.verkeerdFormaat(bestand.formaat) }
        guard bestand.versie <= GebouwBestand.ondersteundeVersie else { throw Fout.nieuwereVersie(bestand.versie) }

        var knopen: [Knoop] = []
        var ids = Set<String>()
        for data in bestand.knopen {
            guard ids.insert(data.id).inserted else { throw Fout.ongeldig("id „\(data.id)” komt twee keer voor.") }
            var normaal: SIMD2<Float>?
            if let qr = data.qr {
                guard qr.normaal.count == 2 else { throw Fout.ongeldig("QR-richting van \(data.id) moet 2 getallen hebben.") }
                let n = SIMD2<Float>(qr.normaal[0], qr.normaal[1])
                guard simd_length(n) > 0.001 else { throw Fout.ongeldig("QR-richting van \(data.id) is nul.") }
                normaal = simd_normalize(n)
            }
            knopen.append(Knoop(id: data.id, naam: data.naam, soort: data.soort,
                                x: data.x, z: data.z, verdieping: data.verdieping, qrNormaal: normaal))
        }

        var verbindingen: [Graaf.Verbinding] = []
        for v in bestand.verbindingen {
            guard ids.contains(v.van), ids.contains(v.naar) else {
                throw Fout.ongeldig("verbinding \(v.van)–\(v.naar) verwijst naar een onbekend punt.")
            }
            verbindingen.append(Graaf.Verbinding(van: v.van, naar: v.naar, kost: v.kost))
        }

        guard !knopen.isEmpty else { throw Fout.ongeldig("er staan geen punten in.") }

        var verdiepingNamen: [Int: String] = [:]
        for v in bestand.verdiepingen { verdiepingNamen[v.niveau] = v.naam }

        return Graaf(knopen: knopen, verbindingen: verbindingen,
                     gebouwNaam: bestand.gebouw.naam.isEmpty ? bestand.gebouw.id : bestand.gebouw.naam,
                     verdiepingNamen: verdiepingNamen)
    }

    private static func pad(_ context: DecodingError.Context) -> String {
        let delen = context.codingPath.map { $0.intValue.map { "[\($0)]" } ?? $0.stringValue }
        return delen.isEmpty ? "begin" : delen.joined(separator: ".")
    }
}
