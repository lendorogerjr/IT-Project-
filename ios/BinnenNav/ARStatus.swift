import Foundation
import Combine

// Kleine gedeelde toestand tussen de AR-sessie en de SwiftUI-interface.
// De ARKit-delegate schrijft hierin, de ContentView leest het.
@MainActor
final class ARStatus: ObservableObject {
    /// Tekst bovenaan het scherm.
    @Published var bericht: String = "Beweeg je iPhone langzaam over de vloer…"
    /// Aantal horizontale vlakken dat ARKit momenteel kent.
    @Published var aantalVlakken: Int = 0
    /// True zodra minstens één vlak als vloer herkend is.
    @Published var vloerGevonden: Bool = false
    /// Tekst over de kwaliteit van het volgen (tracking).
    @Published var trackingTekst: String = "Tracking: opstarten…"
}
