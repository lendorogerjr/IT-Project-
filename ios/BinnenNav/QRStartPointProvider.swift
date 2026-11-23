import ARKit
import Vision
import simd

/// Inhoud van een QR-code van BinnenNav: "BINNENNAV:<knoop-id>", bv. "BINNENNAV:B1".
enum QRHerkenning {
    static let voorvoegsel = "BINNENNAV:"

    static func knoopID(uit tekst: String) -> String? {
        let opgekuist = tekst.trimmingCharacters(in: .whitespacesAndNewlines)
        guard opgekuist.uppercased().hasPrefix(voorvoegsel) else { return nil }
        let id = opgekuist.dropFirst(voorvoegsel.count).trimmingCharacters(in: .whitespaces)
        return id.isEmpty ? nil : id
    }

    /// Zoekt een BinnenNav-QR-code in een camerabeeld. Draait op een achtergrondthread.
    /// Hoeken komen terug in Vision-coördinaten (0…1, oorsprong linksonder) van het
    /// ruwe camerabeeld (liggend), dus zonder rekening te houden met hoe je de iPhone houdt.
    static func zoek(in beeld: CVPixelBuffer) -> QRResultaat? {
        let verzoek = VNDetectBarcodesRequest()
        verzoek.symbologies = [.qr]
        let handler = VNImageRequestHandler(cvPixelBuffer: beeld, orientation: .up, options: [:])
        do {
            try handler.perform([verzoek])
        } catch {
            return nil
        }
        for waarneming in verzoek.results ?? [] {
            guard let tekst = waarneming.payloadStringValue, let id = knoopID(uit: tekst) else { continue }
            let kader = waarneming.boundingBox
            return QRResultaat(knoopID: id,
                               midden: CGPoint(x: kader.midX, y: kader.midY),
                               hoeken: [waarneming.topLeft, waarneming.topRight,
                                        waarneming.bottomRight, waarneming.bottomLeft])
        }
        return nil
    }
}

struct QRResultaat {
    let knoopID: String
    let midden: CGPoint
    /// In volgorde rond de code: linksboven, rechtsboven, rechtsonder, linksonder.
    let hoeken: [CGPoint]
}

/// Camerabeeld + camerastand op het moment van de opname, om naar de achtergrond door te geven.
private struct QRBeeld: @unchecked Sendable {
    let pixels: CVPixelBuffer
    let cameraTransform: simd_float4x4
    let intrinsics: simd_float3x3
}

/// Startpunt via een QR-code naast de deur.
///
/// Werkwijze: we lezen de QR-code met Vision in het ARKit-camerabeeld (de AR-sessie blijft
/// dus gewoon lopen), schieten stralen door het midden en de 4 hoeken van de code naar
/// de muur (ARKit-raycast) en krijgen zo de 3D-positie van de code én de richting van de muur.
/// Pas na 4 overeenkomende metingen geven we een gemiddeld startpunt door.
@MainActor
final class QRStartPointProvider: StartPointProvider {
    var opStartpunt: ((StartpuntMeting) -> Void)?
    /// Tussentijdse tekst voor de gebruiker ("houd stil…").
    var opVoortgang: ((String) -> Void)?

    private let nodigeMetingen = 4
    private let analyseInterval: TimeInterval = 0.2
    private let maxAfstand: Float = 3.0

    private var bezig = false
    private var laatsteAnalyse: TimeInterval = 0
    private var reeks: [StartpuntMeting] = []
    private let wachtrij = DispatchQueue(label: "be.ehb.binnennav.qr", qos: .userInitiated)

    func verwerk(frame: ARFrame, sessie: ARSession) {
        guard !bezig, frame.timestamp - laatsteAnalyse >= analyseInterval else { return }
        // Zonder goede tracking is een raycast onbetrouwbaar.
        guard case .normal = frame.camera.trackingState else { return }

        bezig = true
        laatsteAnalyse = frame.timestamp
        // Alleen de pixelbuffer en de cameragegevens bewaren, nooit het ARFrame zelf
        // (ARKit hergebruikt frames; vasthouden blokkeert de camera).
        let beeld = QRBeeld(pixels: frame.capturedImage,
                            cameraTransform: frame.camera.transform,
                            intrinsics: frame.camera.intrinsics)

        wachtrij.async { [weak self] in
            let gevonden = QRHerkenning.zoek(in: beeld.pixels)
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.bezig = false
                guard let gevonden else { return }
                self.verwerk(gevonden, beeld: beeld, sessie: sessie)
            }
        }
    }

    // MARK: - Van 2D-QR-code naar 3D-startpunt

    private func verwerk(_ qr: QRResultaat, beeld: QRBeeld, sessie: ARSession) {
        let camera = beeld.cameraTransform
        let cameraPositie = SIMD3<Float>(camera.columns.3.x, camera.columns.3.y, camera.columns.3.z)

        guard let midden = raak(qr.midden, beeld: beeld, sessie: sessie),
              simd_distance(midden, cameraPositie) <= maxAfstand else {
            opVoortgang?("QR-code \(qr.knoopID) gezien. Kom iets dichterbij en houd stil…")
            return
        }

        // Ruwe schatting van de muurrichting: van de muur naar de camera.
        let naarCamera = SIMD3<Float>(cameraPositie.x - midden.x, 0, cameraPositie.z - midden.z)
        guard simd_length(naarCamera) > 0.05 else { return }
        var normaal = simd_normalize(naarCamera)

        // Nauwkeuriger: het vlak door de 4 hoeken van de code (werkt ook als je schuin staat).
        let hoeken = qr.hoeken.compactMap { raak($0, beeld: beeld, sessie: sessie) }
        if hoeken.count == 4 {
            let diagonaal1 = hoeken[2] - hoeken[0]   // linksboven → rechtsonder
            let diagonaal2 = hoeken[3] - hoeken[1]   // rechtsboven → linksonder
            var vlakNormaal = simd_cross(diagonaal1, diagonaal2)
            vlakNormaal.y = 0
            if simd_length(vlakNormaal) > 1e-6 {
                vlakNormaal = simd_normalize(vlakNormaal)
                if simd_dot(vlakNormaal, normaal) < 0 { vlakNormaal = -vlakNormaal }
                // Hoeken die te veel afwijken zijn waarschijnlijk ruis (bv. glanzend papier).
                if simd_dot(vlakNormaal, normaal) > cos(Float.pi * 50 / 180) {
                    normaal = vlakNormaal
                }
            }
        }

        let meting = StartpuntMeting(knoopID: qr.knoopID, wereldPositie: midden, wereldNormaal: normaal)
        if reeks.first?.knoopID != qr.knoopID { reeks.removeAll() }
        reeks.append(meting)

        if reeks.count < nodigeMetingen {
            opVoortgang?("QR-code \(qr.knoopID) gevonden. Houd stil… (\(reeks.count)/\(nodigeMetingen))")
            return
        }

        // Gemiddelde van de reeks = stabieler startpunt.
        let aantal = Float(reeks.count)
        let gemiddeldePositie = reeks.reduce(SIMD3<Float>.zero) { $0 + $1.wereldPositie } / aantal
        let somNormaal = reeks.reduce(SIMD3<Float>.zero) { $0 + $1.wereldNormaal }
        reeks.removeAll()
        guard simd_length(somNormaal) > 1e-3 else { return }

        opStartpunt?(StartpuntMeting(knoopID: qr.knoopID,
                                     wereldPositie: gemiddeldePositie,
                                     wereldNormaal: simd_normalize(somNormaal)))
    }

    /// Schiet een straal door een punt van het camerabeeld en geeft het 3D-punt terug
    /// waar die straal een (geschat) oppervlak raakt.
    private func raak(_ punt: CGPoint, beeld: QRBeeld, sessie: ARSession) -> SIMD3<Float>? {
        let breedte = Float(CVPixelBufferGetWidth(beeld.pixels))
        let hoogte = Float(CVPixelBufferGetHeight(beeld.pixels))
        // Vision: oorsprong linksonder → pixels: oorsprong linksboven.
        let px = Float(punt.x) * breedte
        let py = Float(1 - punt.y) * hoogte

        // Cameramodel (pinhole): richting in het assenstelsel van de camera.
        // ARKit-camera: x rechts, y omhoog, kijkt langs -z (in de liggende sensorstand).
        let k = beeld.intrinsics
        let fx = k.columns.0.x, fy = k.columns.1.y
        let cx = k.columns.2.x, cy = k.columns.2.y
        let lokaal = SIMD4<Float>((px - cx) / fx, -(py - cy) / fy, -1, 0)

        let wereld = beeld.cameraTransform * lokaal
        let richting = simd_normalize(SIMD3<Float>(wereld.x, wereld.y, wereld.z))
        let oorsprong = SIMD3<Float>(beeld.cameraTransform.columns.3.x,
                                     beeld.cameraTransform.columns.3.y,
                                     beeld.cameraTransform.columns.3.z)

        let query = ARRaycastQuery(origin: oorsprong, direction: richting,
                                   allowing: .estimatedPlane, alignment: .any)
        guard let treffer = sessie.raycast(query).first else { return nil }
        let t = treffer.worldTransform.columns.3
        return SIMD3<Float>(t.x, t.y, t.z)
    }
}
