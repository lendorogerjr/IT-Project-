import Foundation
import ARKit
import RealityKit
import simd

/// Fases van het navigatiescherm.
enum NavFase: Equatable {
    case scannen            // wachten op een QR-code
    case bestemmingKiezen   // startpunt gekend, gebruiker typt een lokaal
    case overzicht          // route op de 2D-kaart, wachten op "Start"
    case navigeren          // pijlen op de vloer
    case aangekomen
}

/// Hart van de app: verbindt startpuntbronnen, plattegrond, uitlijning en het tekenen van de route.
@MainActor
final class Navigator: ObservableObject {
    @Published private(set) var fase: NavFase = .scannen
    @Published private(set) var startKnoop: Knoop?
    @Published private(set) var bestemming: Knoop?
    @Published private(set) var resterendeMeters: Float = 0
    /// Route, afstand en looptijd voor het overzichtsscherm.
    @Published private(set) var overzicht: RouteOverzicht?
    @Published private(set) var melding = Navigator.scanMelding

    static let scanMelding = "Richt je camera op de QR-code naast een deur."

    /// De plattegrond, uit gebouw.json (ingebouwd of geïmporteerd).
    @Published private(set) var graaf: Graaf
    /// "ingebouwd" of "geïmporteerd bestand", voor in de interface.
    @Published private(set) var gebouwBron: String

    private let qrScanner = QRStartPointProvider()
    private let handmatig = HandmatigStartPointProvider()
    private var bronnen: [StartPointProvider] { [qrScanner, handmatig] }

    private var uitlijning: PlanUitlijning?
    private var renderer: RouteRenderer?
    private var vloerHoogte: Float?
    private var vanRoute = false
    /// Als de route naar een andere verdieping gaat, tekenen we eerst enkel het stuk tot de trap/lift.
    /// Dit is de knoop waar dat stuk eindigt (de trap of lift).
    private var tussenDoel: Knoop?
    private var volgendeVerdieping: Int?
    /// Meters die na de trap/lift nog volgen (voor de afstand onderaan).
    private var restNaTussenDoel: Float = 0

    /// De vloervlakken tonen we enkel zolang er nog geen pijlen zijn.
    var toonVlakken: Bool { fase == .scannen || fase == .bestemmingKiezen || fase == .overzicht }

    init() {
        do {
            let resultaat = try GebouwLader.laadBijStart()
            graaf = resultaat.graaf
            gebouwBron = resultaat.bron
        } catch {
            graaf = .leeg
            gebouwBron = "fout"
            melding = error.localizedDescription
        }
        for bron in bronnen {
            bron.opStartpunt = { [weak self] meting in self?.startpuntGemeten(meting) }
        }
        qrScanner.opVoortgang = { [weak self] tekst in
            guard let self, self.fase == .scannen else { return }
            self.melding = tekst
        }
    }

    func koppel(_ arView: ARView) {
        renderer = RouteRenderer(arView: arView)
    }

    // MARK: - Plattegrond wisselen

    /// Laadt een JSON-bestand dat de gebruiker koos (Bestanden-app, AirDrop…).
    func importeerGebouw(van url: URL) {
        do {
            let nieuw = try GebouwLader.importeer(van: url)
            wisselGebouw(nieuw, bron: "geïmporteerd bestand")
        } catch {
            melding = error.localizedDescription
        }
    }

    /// Terug naar het gebouw.json dat in de app zit.
    func standaardGebouw() {
        GebouwLader.verwijderEigenBestand()
        do {
            wisselGebouw(try GebouwLader.standaard(), bron: "ingebouwd")
        } catch {
            melding = error.localizedDescription
        }
    }

    func toonMelding(_ tekst: String) {
        melding = tekst
    }

    private func wisselGebouw(_ nieuw: Graaf, bron: String) {
        opnieuwScannen()
        graaf = nieuw
        gebouwBron = bron
        melding = "Plattegrond „\(nieuw.gebouwNaam)” geladen (\(nieuw.lokalen.count) lokalen). Scan een QR-code."
    }

    // MARK: - Acties vanuit de interface

    func kiesBestemming(_ doel: Knoop) {
        guard let startKnoop else { return }
        guard doel.id != startKnoop.id else {
            melding = "Je staat al bij \(doel.naam)."
            return
        }
        guard let route = graaf.kortsteRoute(van: startKnoop.id, naar: doel.id) else {
            melding = "Geen route gevonden naar \(doel.naam)."
            return
        }
        // Eerst het overzicht tonen; de AR-pijlen komen pas na "Start".
        bestemming = doel
        overzicht = RouteOverzicht(route: route, graaf: graaf)
        fase = .overzicht
        melding = "Je route naar \(doel.naam)"
    }

    /// "Start" op het overzichtsscherm.
    func startNavigatie() {
        guard fase == .overzicht, let bestemming else { return }
        if tekenRoute(naar: bestemming) {
            vanRoute = false
            fase = .navigeren
            melding = volgMelding(naar: bestemming)
        }
    }

    /// Terug naar het kiezen van een bestemming.
    func annuleerOverzicht() {
        overzicht = nil
        bestemming = nil
        fase = .bestemmingKiezen
        melding = "Je staat bij \(startKnoop?.naam ?? "?"). Waar wil je naartoe?"
    }

    /// Zonder QR-code: de gebruiker kiest zelf voor welke deur hij staat.
    func handmatigeStart(knoopID: String) {
        if !handmatig.kies(knoopID: knoopID) {
            melding = "Richt je iPhone recht naar de muur en probeer opnieuw."
        }
    }

    func stop() {
        renderer?.wis()
        overzicht = nil
        bestemming = nil
        tussenDoel = nil
        volgendeVerdieping = nil
        fase = startKnoop == nil ? .scannen : .bestemmingKiezen
        melding = startKnoop.map { "Je staat bij \($0.naam). Waar wil je naartoe?" } ?? Navigator.scanMelding
    }

    /// Na aankomst ben je bij de doeldeur: die wordt het nieuwe startpunt (zelfde uitlijning).
    func nieuweBestemming() {
        renderer?.wis()
        if let bestemming { startKnoop = bestemming }
        bestemming = nil
        overzicht = nil
        fase = .bestemmingKiezen
        melding = "Je staat bij \(startKnoop?.naam ?? "?"). Waar wil je naartoe?"
    }

    func opnieuwScannen() {
        renderer?.wis()
        overzicht = nil
        startKnoop = nil
        bestemming = nil
        tussenDoel = nil
        volgendeVerdieping = nil
        uitlijning = nil
        fase = .scannen
        melding = Navigator.scanMelding
    }

    // MARK: - Per camerabeeld (vanuit de ARSessionDelegate)

    func verwerk(frame: ARFrame, sessie: ARSession) {
        werkVloerBij(frame)
        handmatig.verwerk(frame: frame, sessie: sessie)
        // Ook tijdens het navigeren blijven we QR-codes lezen: een andere code = herijken.
        if fase != .aangekomen {
            qrScanner.verwerk(frame: frame, sessie: sessie)
        }

        guard fase == .navigeren, let renderer, let bestemming else { return }
        let t = frame.camera.transform.columns.3
        let stand = renderer.werkBij(camera: SIMD3<Float>(t.x, t.y, t.z))

        let afgerond = (stand.resterend + restNaTussenDoel).rounded()
        if afgerond != resterendeMeters { resterendeMeters = afgerond }

        if stand.resterend < 1.5, let tussenDoel, let volgendeVerdieping {
            // Bij de trap/lift: op de andere verdieping hoort een ander stuk plan,
            // dus daar moet een nieuwe QR-code de uitlijning geven.
            renderer.wis()
            fase = .scannen
            melding = "Neem de \(tussenDoel.soort == .lift ? "lift" : "trap") naar \(graaf.naam(verdieping: volgendeVerdieping)) en scan daar een QR-code."
            return
        }

        if stand.resterend < 1.5 {
            fase = .aangekomen
            melding = "Je bent aangekomen bij \(bestemming.naam) 🎉"
            return
        }

        let nuVanRoute = stand.afwijking > 3
        if nuVanRoute != vanRoute {
            vanRoute = nuVanRoute
            melding = nuVanRoute
                ? "Je wijkt af van de route. Scan een QR-code om te herijken."
                : volgMelding(naar: bestemming)
        }
    }

    // MARK: - Intern

    private func startpuntGemeten(_ meting: StartpuntMeting) {
        guard let knoop = graaf.knopen[meting.knoopID] else {
            melding = "QR-code „\(meting.knoopID)” hoort niet bij dit gebouw."
            return
        }
        // Dezelfde code nog eens zien verandert niets.
        if fase != .scannen, knoop.id == startKnoop?.id { return }

        let vloer = vloerHoogte ?? (meting.wereldPositie.y - 1.4)
        guard let nieuweUitlijning = PlanUitlijning(knoop: knoop, meting: meting, vloerHoogte: vloer) else {
            melding = "Knoop \(knoop.naam) heeft geen QR-richting op het plan."
            return
        }
        uitlijning = nieuweUitlijning
        startKnoop = knoop

        if fase == .scannen, let bestemming, volgendeVerdieping != nil {
            // Na een trap/lift: verder naar dezelfde bestemming vanaf deze QR-code.
            if knoop.id == bestemming.id {
                fase = .aangekomen
                melding = "Je bent aangekomen bij \(bestemming.naam) 🎉"
            } else if tekenRoute(naar: bestemming) {
                vanRoute = false
                fase = .navigeren
                melding = volgMelding(naar: bestemming)
            }
        } else if fase == .navigeren, let bestemming {
            // Herijken onderweg: route opnieuw tekenen vanaf deze code.
            if knoop.id == bestemming.id {
                renderer?.wis()
                fase = .aangekomen
                melding = "Je bent aangekomen bij \(bestemming.naam) 🎉"
            } else if tekenRoute(naar: bestemming) {
                melding = "Herijkt bij \(knoop.naam) ✅"
            }
        } else {
            renderer?.wis()
            overzicht = nil
            bestemming = nil
            fase = .bestemmingKiezen
            melding = "Je staat bij \(knoop.naam). Waar wil je naartoe?"
        }
    }

    @discardableResult
    private func tekenRoute(naar doel: Knoop) -> Bool {
        guard let startKnoop, let uitlijning, let renderer,
              let route = graaf.kortsteRoute(van: startKnoop.id, naar: doel.id) else {
            melding = "Geen route gevonden naar \(doel.naam)."
            return false
        }
        // Enkel het stuk op de huidige verdieping: de uitlijning geldt maar voor dit plan.
        let stuk = Array(route.prefix { $0.verdieping == startKnoop.verdieping })
        if stuk.count < route.count, let laatste = stuk.last {
            tussenDoel = laatste
            volgendeVerdieping = route[stuk.count].verdieping
            restNaTussenDoel = graaf.lengte(Array(route[stuk.count...]))
        } else {
            tussenDoel = nil
            volgendeVerdieping = nil
            restNaTussenDoel = 0
        }

        let wereldPunten = stuk.map { uitlijning.wereldPunt($0.positie) }
        let markerTekst: String
        if let tussenDoel, let volgendeVerdieping {
            markerTekst = "\(tussenDoel.soort == .lift ? "Lift" : "Trap") → \(graaf.naam(verdieping: volgendeVerdieping))"
        } else {
            markerTekst = doel.naam
        }
        renderer.toon(route: wereldPunten, doelNaam: markerTekst)
        resterendeMeters = graaf.lengte(route).rounded()
        return true
    }

    private func volgMelding(naar doel: Knoop) -> String {
        if let tussenDoel, let volgendeVerdieping {
            return "Volg de pijlen naar de \(tussenDoel.soort == .lift ? "lift" : "trap") (\(graaf.naam(verdieping: volgendeVerdieping)), richting \(doel.naam))."
        }
        return "Volg de pijlen naar \(doel.naam)."
    }

    /// Vloerhoogte = hoogste horizontale vlak dat minstens 80 cm onder de camera ligt.
    private func werkVloerBij(_ frame: ARFrame) {
        let cameraY = frame.camera.transform.columns.3.y
        let hoogtes = frame.anchors
            .compactMap { $0 as? ARPlaneAnchor }
            .filter { vlak in
                guard vlak.alignment == .horizontal else { return false }
                if ARPlaneAnchor.isClassificationSupported, case .floor = vlak.classification { return true }
                return !ARPlaneAnchor.isClassificationSupported
            }
            .map { $0.transform.columns.3.y }
            .filter { $0 < cameraY - 0.8 }
        if let hoogste = hoogtes.max() {
            vloerHoogte = hoogste
        }
    }
}
