import SwiftUI
import RealityKit
import ARKit

// Brengt de RealityKit-ARView (UIKit) naar SwiftUI.
struct ARViewContainer: UIViewRepresentable {
    let status: ARStatus
    let navigator: Navigator

    func makeCoordinator() -> Coordinator {
        Coordinator(status: status, navigator: navigator)
    }

    func makeUIView(context: Context) -> ARView {
        // We configureren de sessie zelf, dus automatische configuratie uit.
        let arView = ARView(frame: .zero, cameraMode: .ar, automaticallyConfigureSession: false)

        // Wereldtracking met detectie van horizontale vlakken (vloer, tafels).
        // Autofocus staat standaard aan: nodig om QR-codes van dichtbij scherp te lezen.
        let configuratie = ARWorldTrackingConfiguration()
        configuratie.planeDetection = [.horizontal]
        configuratie.environmentTexturing = .automatic

        arView.session.delegate = context.coordinator
        context.coordinator.arView = arView
        navigator.koppel(arView)

        // Apple's eigen instructie-overlay ("beweeg je toestel…") tot er een vlak is.
        let coaching = ARCoachingOverlayView()
        coaching.session = arView.session
        coaching.goal = .horizontalPlane
        coaching.activatesAutomatically = true
        coaching.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        arView.addSubview(coaching)

        arView.session.run(configuratie)
        return arView
    }

    func updateUIView(_ uiView: ARView, context: Context) {
        // Alles loopt via de Navigator; hier niets te doen.
    }

    static func dismantleUIView(_ uiView: ARView, coordinator: Coordinator) {
        uiView.session.pause()
    }

    // MARK: - Coordinator: ontvangt de ARKit-gebeurtenissen

    @MainActor
    final class Coordinator: NSObject, ARSessionDelegate {
        weak var arView: ARView?
        private let status: ARStatus
        private let navigator: Navigator

        /// Per ARKit-vlak de RealityKit-entiteit die het tekent.
        private var vlakken: [UUID: AnchorEntity] = [:]
        /// Identifiers van de vlakken die als vloer herkend zijn.
        private var vloerVlakken: Set<UUID> = []
        /// Zijn de vlakken momenteel zichtbaar?
        private var vlakkenZichtbaar = true

        init(status: ARStatus, navigator: Navigator) {
            self.status = status
            self.navigator = navigator
        }

        // ARKit roept deze methodes op de hoofdthread aan (delegateQueue is nil),
        // daarom mogen we hier veilig naar de MainActor springen.

        nonisolated func session(_ session: ARSession, didUpdate frame: ARFrame) {
            MainActor.assumeIsolated {
                navigator.verwerk(frame: frame, sessie: session)
                zetVlakkenZichtbaar(navigator.toonVlakken)
            }
        }

        nonisolated func session(_ session: ARSession, didAdd anchors: [ARAnchor]) {
            MainActor.assumeIsolated {
                for anchor in anchors {
                    if let vlak = anchor as? ARPlaneAnchor {
                        voegVlakToe(vlak)
                    }
                }
                werkStatusBij()
            }
        }

        nonisolated func session(_ session: ARSession, didUpdate anchors: [ARAnchor]) {
            MainActor.assumeIsolated {
                for anchor in anchors {
                    if let vlak = anchor as? ARPlaneAnchor {
                        werkVlakBij(vlak)
                    }
                }
                werkStatusBij()
            }
        }

        nonisolated func session(_ session: ARSession, didRemove anchors: [ARAnchor]) {
            MainActor.assumeIsolated {
                for anchor in anchors {
                    vloerVlakken.remove(anchor.identifier)
                    if let entiteit = vlakken.removeValue(forKey: anchor.identifier) {
                        arView?.scene.removeAnchor(entiteit)
                    }
                }
                werkStatusBij()
            }
        }

        nonisolated func session(_ session: ARSession, cameraDidChangeTrackingState camera: ARCamera) {
            let tekst: String
            switch camera.trackingState {
            case .normal:
                tekst = "Tracking: goed"
            case .notAvailable:
                tekst = "Tracking: niet beschikbaar"
            case .limited(let reden):
                switch reden {
                case .initializing: tekst = "Tracking: opstarten…"
                case .excessiveMotion: tekst = "Tracking: beweeg trager"
                case .insufficientFeatures: tekst = "Tracking: te weinig details (meer licht of textuur nodig)"
                case .relocalizing: tekst = "Tracking: opnieuw lokaliseren…"
                @unknown default: tekst = "Tracking: beperkt"
                }
            }
            MainActor.assumeIsolated {
                status.trackingTekst = tekst
            }
        }

        nonisolated func session(_ session: ARSession, didFailWithError error: Error) {
            let tekst = "AR-fout: \(error.localizedDescription)"
            MainActor.assumeIsolated {
                status.bericht = tekst
            }
        }

        // MARK: - Vlakken tekenen

        private func zetVlakkenZichtbaar(_ zichtbaar: Bool) {
            guard zichtbaar != vlakkenZichtbaar else { return }
            vlakkenZichtbaar = zichtbaar
            for entiteit in vlakken.values { entiteit.isEnabled = zichtbaar }
        }

        private func voegVlakToe(_ vlak: ARPlaneAnchor) {
            guard vlak.alignment == .horizontal, let arView else { return }

            // De AnchorEntity volgt het ARKit-anker automatisch.
            let ankerEntiteit = AnchorEntity(anchor: vlak)
            let model = ModelEntity(mesh: maakMesh(voor: vlak), materials: [materiaal(voor: vlak)])
            model.name = "vlak"
            plaats(model, volgens: vlak)

            ankerEntiteit.addChild(model)
            ankerEntiteit.isEnabled = vlakkenZichtbaar
            arView.scene.addAnchor(ankerEntiteit)
            vlakken[vlak.identifier] = ankerEntiteit
            onthoudVloer(vlak)
        }

        private func werkVlakBij(_ vlak: ARPlaneAnchor) {
            guard let ankerEntiteit = vlakken[vlak.identifier] else {
                // Kan gebeuren als een vlak pas later horizontaal wordt herkend.
                voegVlakToe(vlak)
                return
            }
            guard let model = ankerEntiteit.children.first as? ModelEntity else { return }

            // ARKit vergroot het vlak naarmate je meer van de vloer filmt.
            model.model?.mesh = maakMesh(voor: vlak)
            model.model?.materials = [materiaal(voor: vlak)]
            plaats(model, volgens: vlak)
            onthoudVloer(vlak)
        }

        /// De classificatie van een vlak kan tijdens de sessie nog veranderen.
        private func onthoudVloer(_ vlak: ARPlaneAnchor) {
            if isVloer(vlak) {
                vloerVlakken.insert(vlak.identifier)
            } else {
                vloerVlakken.remove(vlak.identifier)
            }
        }

        /// Rechthoek in het XZ-vlak (normaal naar boven) met de afmetingen van het ARKit-vlak.
        private func maakMesh(voor vlak: ARPlaneAnchor) -> MeshResource {
            MeshResource.generatePlane(width: vlak.planeExtent.width, depth: vlak.planeExtent.height)
        }

        /// Middelpunt en draaiing van het vlak binnen zijn anker.
        private func plaats(_ model: ModelEntity, volgens vlak: ARPlaneAnchor) {
            model.position = vlak.center
            model.orientation = simd_quatf(angle: vlak.planeExtent.rotationOnYAxis, axis: [0, 1, 0])
        }

        /// Groen voor de vloer, oranje voor andere horizontale vlakken (tafel, bank…).
        /// Lichter dan in stap 1, zodat de QR-zoeker en de pijlen goed zichtbaar blijven.
        private func materiaal(voor vlak: ARPlaneAnchor) -> UnlitMaterial {
            let kleur: UIColor = isVloer(vlak) ? .systemGreen : .systemOrange
            var materiaal = UnlitMaterial(color: kleur)
            materiaal.blending = .transparent(opacity: 0.25)
            return materiaal
        }

        private func isVloer(_ vlak: ARPlaneAnchor) -> Bool {
            // Classificatie werkt niet op elk toestel; zonder classificatie
            // beschouwen we elk horizontaal vlak als mogelijke vloer.
            guard ARPlaneAnchor.isClassificationSupported else { return true }
            if case .floor = vlak.classification { return true }
            return false
        }

        private func werkStatusBij() {
            status.aantalVlakken = vlakken.count
            let vloerAanwezig = !vloerVlakken.isEmpty
            status.vloerGevonden = vloerAanwezig
            if vloerAanwezig {
                status.bericht = "Vloer gevonden ✅"
            } else if !vlakken.isEmpty {
                status.bericht = "Vlak gevonden, maar nog geen vloer. Richt op de grond."
            } else {
                status.bericht = "Beweeg je iPhone langzaam over de vloer…"
            }
        }
    }
}
