import RealityKit
import UIKit
import simd

/// Tekent de route als chevrons (">") op de vloer en een paaltje met de naam bij de bestemming.
/// Alleen chevrons binnen ~8 m en vóór de gebruiker zijn zichtbaar.
@MainActor
final class RouteRenderer {
    let zichtbareAfstand: Float = 8
    let tussenafstand: Float = 0.7
    /// Iets boven de vloer zodat de pijlen niet "in" het vloervlak flikkeren.
    let hoogteBovenVloer: Float = 0.02

    private let wortel = AnchorEntity(world: .zero)
    private var punten: [SIMD3<Float>] = []
    /// Afgelegde afstand langs de route tot aan elk punt.
    private var cumulatief: [Float] = []
    private var totaal: Float = 0
    private var chevrons: [(entiteit: Entity, boog: Float)] = []

    private let armMesh = MeshResource.generateBox(size: SIMD3<Float>(0.07, 0.01, 0.32), cornerRadius: 0.005)
    private let chevronMateriaal: UnlitMaterial = {
        var materiaal = UnlitMaterial(color: UIColor(red: 0.10, green: 0.60, blue: 1.00, alpha: 1))
        materiaal.blending = .transparent(opacity: 0.9)
        return materiaal
    }()

    init(arView: ARView) {
        arView.scene.addAnchor(wortel)
    }

    /// Toont een nieuwe route (punten in de AR-wereld, op vloerhoogte).
    func toon(route: [SIMD3<Float>], doelNaam: String) {
        wis()
        guard route.count >= 2 else { return }
        punten = route.map { $0 + SIMD3<Float>(0, hoogteBovenVloer, 0) }

        cumulatief = [0]
        for i in 1..<punten.count {
            cumulatief.append(cumulatief[i - 1] + simd_distance(punten[i - 1], punten[i]))
        }
        totaal = cumulatief.last ?? 0

        // Eerste chevron een halve meter van de startdeur, laatste net voor de doeldeur.
        var boog: Float = 0.5
        while boog < totaal - 0.3 {
            let (positie, richting) = punt(opAfstand: boog)
            let chevron = maakChevron()
            chevron.position = positie
            chevron.orientation = RouteRenderer.draaiing(naar: richting)
            wortel.addChild(chevron)
            chevrons.append((chevron, boog))
            boog += tussenafstand
        }

        let laatsteRichting = punten[punten.count - 1] - punten[punten.count - 2]
        let marker = maakDoelMarker(naam: doelNaam)
        marker.position = punten[punten.count - 1]
        marker.orientation = RouteRenderer.draaiing(naar: laatsteRichting)
        wortel.addChild(marker)
    }

    func wis() {
        for kind in Array(wortel.children) { kind.removeFromParent() }
        punten = []
        cumulatief = []
        chevrons = []
        totaal = 0
    }

    /// Per frame: zichtbaarheid bijwerken en de stand van de gebruiker op de route berekenen.
    /// - Returns: resterende afstand langs de route en hoe ver de gebruiker naast de route staat.
    func werkBij(camera: SIMD3<Float>) -> (resterend: Float, afwijking: Float) {
        guard punten.count >= 2 else { return (0, 0) }
        let c = SIMD2<Float>(camera.x, camera.z)

        // Dichtstbijzijnde punt op de route (alleen horizontaal).
        var besteAfstand = Float.infinity
        var besteBoog: Float = 0
        for i in 0..<(punten.count - 1) {
            let a = SIMD2<Float>(punten[i].x, punten[i].z)
            let b = SIMD2<Float>(punten[i + 1].x, punten[i + 1].z)
            let ab = b - a
            let lengte2 = simd_length_squared(ab)
            let t = lengte2 > 0 ? min(max(simd_dot(c - a, ab) / lengte2, 0), 1) : 0
            let afstand = simd_distance(c, a + ab * t)
            if afstand < besteAfstand {
                besteAfstand = afstand
                besteBoog = cumulatief[i] + sqrt(lengte2) * t
            }
        }

        for (entiteit, boog) in chevrons {
            let p = entiteit.position
            let dichtbij = simd_distance(SIMD2<Float>(p.x, p.z), c) < zichtbareAfstand
            let nogVoorJe = boog > besteBoog - 0.3
            let zichtbaar = dichtbij && nogVoorJe
            if entiteit.isEnabled != zichtbaar { entiteit.isEnabled = zichtbaar }
        }

        return (max(0, totaal - besteBoog), besteAfstand)
    }

    // MARK: - Hulpfuncties

    /// Positie en richting op een bepaalde afstand langs de route.
    private func punt(opAfstand boog: Float) -> (SIMD3<Float>, SIMD3<Float>) {
        for i in 0..<(punten.count - 1) where boog <= cumulatief[i + 1] {
            let lengte = cumulatief[i + 1] - cumulatief[i]
            let t = lengte > 0 ? (boog - cumulatief[i]) / lengte : 0
            let positie = punten[i] + (punten[i + 1] - punten[i]) * t
            return (positie, punten[i + 1] - punten[i])
        }
        let n = punten.count
        return (punten[n - 1], punten[n - 1] - punten[n - 2])
    }

    /// Draaiing rond de y-as zodat de lokale -z-as langs de (horizontale) richting wijst.
    static func draaiing(naar richting: SIMD3<Float>) -> simd_quatf {
        simd_quatf(angle: atan2(-richting.x, -richting.z), axis: SIMD3<Float>(0, 1, 0))
    }

    /// Chevron "^" met de punt naar lokale -z: twee platte balkjes onder ±45°.
    private func maakChevron() -> Entity {
        let chevron = Entity()
        let punt = SIMD3<Float>(0, 0, -0.15)
        for teken: Float in [-1, 1] {
            let hoek = teken * Float.pi / 4
            let arm = ModelEntity(mesh: armMesh, materials: [chevronMateriaal])
            arm.orientation = simd_quatf(angle: hoek, axis: SIMD3<Float>(0, 1, 0))
            // Het balkje ligt langs z; na de draaiing wijst het naar (sin, 0, cos).
            let armRichting = SIMD3<Float>(sin(hoek), 0, cos(hoek))
            arm.position = punt + armRichting * 0.16
            chevron.addChild(arm)
        }
        return chevron
    }

    /// Paaltje met bol en de naam van het lokaal, bij de doeldeur.
    private func maakDoelMarker(naam: String) -> Entity {
        let marker = Entity()
        let kleur = UIColor(red: 0.20, green: 0.80, blue: 0.35, alpha: 1)

        let paal = ModelEntity(mesh: .generateBox(size: SIMD3<Float>(0.04, 1.0, 0.04)),
                               materials: [UnlitMaterial(color: kleur)])
        paal.position = SIMD3<Float>(0, 0.5, 0)
        marker.addChild(paal)

        let bol = ModelEntity(mesh: .generateSphere(radius: 0.12), materials: [UnlitMaterial(color: kleur)])
        bol.position = SIMD3<Float>(0, 1.1, 0)
        marker.addChild(bol)

        let tekstMesh = MeshResource.generateText(naam,
                                                  extrusionDepth: 0.02,
                                                  font: .systemFont(ofSize: 0.25, weight: .bold),
                                                  containerFrame: .zero,
                                                  alignment: .center,
                                                  lineBreakMode: .byWordWrapping)
        let tekst = ModelEntity(mesh: tekstMesh, materials: [UnlitMaterial(color: .white)])
        // Tekst centreren boven de bol. De voorkant van de tekst kijkt naar +z,
        // dus naar de gebruiker die langs de route aankomt.
        tekst.position = SIMD3<Float>(-tekstMesh.bounds.center.x, 1.3, 0)
        marker.addChild(tekst)

        return marker
    }
}
