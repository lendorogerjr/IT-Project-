import SwiftUI
import UniformTypeIdentifiers

// Hoofdscherm: AR-camera over het volledige scherm, statusbalk bovenaan
// en onderaan een paneel dat verandert volgens de fase (scannen → bestemming → navigeren).
struct ContentView: View {
    @StateObject private var status = ARStatus()
    @StateObject private var navigator = Navigator()

    @State private var zoekTekst = ""
    @State private var toonHandmatigeKeuze = false
    @State private var toonImporteren = false
    @FocusState private var zoekveldActief: Bool

    var body: some View {
        ZStack {
            ARViewContainer(status: status, navigator: navigator)
                .ignoresSafeArea()

            if navigator.fase == .scannen {
                qrZoeker
            }

            VStack {
                bovenbalk
                Spacer()
                onderpaneel
            }
        }
        .sheet(isPresented: $toonHandmatigeKeuze) {
            handmatigeKeuze
        }
    }

    // MARK: - Bovenbalk

    private var bovenbalk: some View {
        VStack(spacing: 4) {
            Text(navigator.melding)
                .font(.headline)
                .multilineTextAlignment(.center)
            Text("\(status.trackingTekst) · \(status.vloerGevonden ? "vloer gevonden" : "nog geen vloer")")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 14))
        .padding(.horizontal, 16)
        .padding(.top, 8)
    }

    // MARK: - QR-zoeker in het midden

    private var qrZoeker: some View {
        VStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 24)
                .strokeBorder(.white, style: StrokeStyle(lineWidth: 4, dash: [18, 10]))
                .frame(width: 230, height: 230)
                .overlay(
                    Image(systemName: "qrcode.viewfinder")
                        .font(.system(size: 54))
                        .foregroundStyle(.white.opacity(0.8))
                )
            Text("Scan de QR-code naast de deur")
                .font(.subheadline.bold())
                .foregroundStyle(.white)
                .shadow(radius: 4)
        }
        .allowsHitTesting(false)
    }

    // MARK: - Onderpaneel

    @ViewBuilder
    private var onderpaneel: some View {
        switch navigator.fase {
        case .scannen:
            Button {
                toonHandmatigeKeuze = true
            } label: {
                Label("Geen QR-code? Kies je startpunt", systemImage: "hand.tap")
            }
            .buttonStyle(.borderedProminent)
            .padding(.bottom, 24)

        case .bestemmingKiezen:
            bestemmingPaneel
                .paneel()

        case .overzicht:
            if let overzicht = navigator.overzicht {
                RouteOverzichtView(overzicht: overzicht,
                                   graaf: navigator.graaf,
                                   opStart: { navigator.startNavigatie() },
                                   opAnnuleer: { navigator.annuleerOverzicht() })
                    .id(overzicht.doel?.id)   // nieuwe route = verdieping opnieuw op de start zetten
                    .paneel()
            }

        case .navigeren:
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Naar \(navigator.bestemming?.naam ?? "?")")
                        .font(.title2.bold())
                    Text("Nog ongeveer \(Int(navigator.resterendeMeters)) m")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Button("Stop", role: .destructive) {
                    navigator.stop()
                }
                .buttonStyle(.bordered)
            }
            .paneel()

        case .aangekomen:
            VStack(spacing: 12) {
                Text("Je bent aangekomen bij \(navigator.bestemming?.naam ?? "je bestemming") 🎉")
                    .font(.title3.bold())
                    .multilineTextAlignment(.center)
                Button("Nieuwe bestemming") {
                    zoekTekst = ""
                    navigator.nieuweBestemming()
                }
                .buttonStyle(.borderedProminent)
            }
            .frame(maxWidth: .infinity)
            .paneel()
        }
    }

    private var suggesties: [Knoop] {
        navigator.graaf.zoekLokalen(zoekTekst).filter { $0.id != navigator.startKnoop?.id }
    }

    private var bestemmingPaneel: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Je staat bij \(navigator.startKnoop?.naam ?? "?")")
                .font(.headline)

            TextField("Naar welk lokaal? (bv. B4)", text: $zoekTekst)
                .textFieldStyle(.roundedBorder)
                .textInputAutocapitalization(.characters)
                .autocorrectionDisabled()
                .submitLabel(.go)
                .focused($zoekveldActief)
                .onSubmit {
                    if let eerste = suggesties.first { kies(eerste) }
                }

            if suggesties.isEmpty {
                Text("Geen lokaal gevonden voor „\(zoekTekst)”.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(suggesties) { lokaal in
                            Button(lokaal.naam) { kies(lokaal) }
                                .buttonStyle(.borderedProminent)
                        }
                    }
                }
            }

            Button {
                zoekTekst = ""
                navigator.opnieuwScannen()
            } label: {
                Label("Opnieuw scannen", systemImage: "qrcode.viewfinder")
            }
            .font(.footnote)
        }
    }

    private func kies(_ lokaal: Knoop) {
        zoekveldActief = false
        zoekTekst = ""
        navigator.kiesBestemming(lokaal)
    }

    // MARK: - Manuele keuze (zonder QR-code)

    private var handmatigeKeuze: some View {
        NavigationStack {
            List {
                ForEach(navigator.graaf.verdiepingen, id: \.self) { niveau in
                    let knopen = navigator.graaf.qrKnopen.filter { $0.verdieping == niveau }
                    if !knopen.isEmpty {
                        Section(navigator.graaf.naam(verdieping: niveau)) {
                            ForEach(knopen) { knoop in
                                Button(knoop.naam) {
                                    toonHandmatigeKeuze = false
                                    navigator.handmatigeStart(knoopID: knoop.id)
                                }
                            }
                        }
                    }
                }

                Section {
                    LabeledContent("Gebouw", value: navigator.graaf.gebouwNaam)
                    LabeledContent("Bron", value: navigator.gebouwBron)
                    LabeledContent("Lokalen", value: "\(navigator.graaf.lokalen.count)")
                    Button {
                        toonImporteren = true
                    } label: {
                        Label("Plattegrond importeren (JSON)…", systemImage: "square.and.arrow.down")
                    }
                    if GebouwLader.heeftEigenBestand {
                        Button(role: .destructive) {
                            toonHandmatigeKeuze = false
                            navigator.standaardGebouw()
                        } label: {
                            Label("Terug naar de ingebouwde plattegrond", systemImage: "arrow.uturn.backward")
                        }
                    }
                } header: {
                    Text("Plattegrond")
                } footer: {
                    Text("Ga vlak voor de deur van een lokaal staan en richt je iPhone recht naar de muur voordat je kiest. Een plattegrond maak je met de editor en zet je via AirDrop of Bestanden op je iPhone.")
                }
            }
            .navigationTitle("Waar sta je?")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuleer") { toonHandmatigeKeuze = false }
                }
            }
            .fileImporter(isPresented: $toonImporteren, allowedContentTypes: [.json]) { resultaat in
                switch resultaat {
                case .success(let url):
                    toonHandmatigeKeuze = false
                    navigator.importeerGebouw(van: url)
                case .failure(let fout):
                    navigator.toonMelding(fout.localizedDescription)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

private extension View {
    /// Gemeenschappelijke stijl voor de panelen onderaan.
    func paneel() -> some View {
        self
            .padding(14)
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
            .padding(.horizontal, 16)
            .padding(.bottom, 12)
    }
}

#Preview {
    // De preview toont alleen de interface; AR werkt enkel op een echte iPhone.
    ContentView()
}
