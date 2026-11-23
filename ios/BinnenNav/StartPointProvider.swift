import ARKit
import simd

/// Resultaat van een startpuntbron: welke knoop het is en waar hij in de AR-wereld ligt.
struct StartpuntMeting {
    /// Id van de knoop op het plan (bv. "B1").
    let knoopID: String
    /// Positie van de QR-code (of het toestel) in de AR-wereld, in meter.
    let wereldPositie: SIMD3<Float>
    /// Horizontale richting loodrecht op de muur, van de muur de gang in.
    let wereldNormaal: SIMD3<Float>
}

/// Abstractie voor "waar sta ik bij de start?".
/// Vandaag: QR-code (camera) of manuele keuze. Later: echte NFC-tag (betaald account).
/// De rest van de app kent alleen dit protocol.
@MainActor
protocol StartPointProvider: AnyObject {
    /// Wordt opgeroepen zodra de bron een betrouwbaar startpunt heeft.
    var opStartpunt: ((StartpuntMeting) -> Void)? { get set }
    /// Krijgt elk camerabeeld van ARKit.
    func verwerk(frame: ARFrame, sessie: ARSession)
}

/// Reservebron zonder QR-code: de gebruiker kiest zelf het lokaal waar hij voor staat,
/// met de iPhone naar de muur gericht. Handig om te testen zonder geprinte codes.
@MainActor
final class HandmatigStartPointProvider: StartPointProvider {
    var opStartpunt: ((StartpuntMeting) -> Void)?
    private var laatsteCamera: simd_float4x4?

    func verwerk(frame: ARFrame, sessie: ARSession) {
        laatsteCamera = frame.camera.transform
    }

    /// We nemen aan dat de muur ongeveer 50 cm voor de camera staat.
    /// Geeft false terug als de iPhone te plat gehouden wordt om een richting te bepalen.
    @discardableResult
    func kies(knoopID: String) -> Bool {
        guard let camera = laatsteCamera else { return false }
        let positie = SIMD3<Float>(camera.columns.3.x, camera.columns.3.y, camera.columns.3.z)
        // De camera kijkt langs haar eigen -z-as; enkel het horizontale deel telt.
        let vooruit = SIMD3<Float>(-camera.columns.2.x, 0, -camera.columns.2.z)
        guard simd_length(vooruit) > 0.2 else { return false }
        let richting = simd_normalize(vooruit)
        opStartpunt?(StartpuntMeting(knoopID: knoopID,
                                     wereldPositie: positie + richting * 0.5,
                                     wereldNormaal: -richting))
        return true
    }
}
