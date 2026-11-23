import SwiftUI
import simd

/// Overzichtsscherm na het kiezen van een bestemming: plattegrond van bovenaf met de
/// route, de afstand en de looptijd. "Start" zet de AR-pijlen aan.
struct RouteOverzichtView: View {
    let overzicht: RouteOverzicht
    let graaf: Graaf
    let opStart: () -> Void
    let opAnnuleer: () -> Void

    @State private var gekozenVerdieping: Int

    init(overzicht: RouteOverzicht, graaf: Graaf, opStart: @escaping () -> Void, opAnnuleer: @escaping () -> Void) {
        self.overzicht = overzicht
        self.graaf = graaf
        self.opStart = opStart
        self.opAnnuleer = opAnnuleer
        _gekozenVerdieping = State(initialValue: overzicht.start?.verdieping ?? 0)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline) {
                Text("Naar \(overzicht.doel?.naam ?? "?")")
                    .font(.title2.bold())
                Spacer()
                Button("Ander lokaal", action: opAnnuleer)
                    .font(.footnote)
            }

            HStack(spacing: 14) {
                Label(overzicht.afstandTekst, systemImage: "figure.walk")
                Label(overzicht.tijdTekst, systemImage: "clock")
                if let verdiepingen = overzicht.verdiepingTekst {
                    Label(verdiepingen, systemImage: "stairs")
                }
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(.secondary)

            if overzicht.delen.count > 1 {
                Picker("Verdieping", selection: $gekozenVerdieping) {
                    ForEach(overzicht.delen) { deel in
                        Text(graaf.naam(verdieping: deel.verdieping)).tag(deel.verdieping)
                    }
                }
                .pickerStyle(.segmented)
            }

            PlanKaart(graaf: graaf, overzicht: overzicht, verdieping: gekozenVerdieping)
                .frame(height: 260)
                .background(Color(.systemBackground).opacity(0.9), in: RoundedRectangle(cornerRadius: 12))

            if let deel = overzicht.delen.first(where: { $0.verdieping == gekozenVerdieping }) {
                Text(beschrijving(van: deel))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }

            Button(action: opStart) {
                Label("Start", systemImage: "arkit")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
        }
    }

    /// Eén zin per verdieping, bv. "Verdieping 0: 22 m tot de trap."
    private func beschrijving(van deel: RouteOverzicht.Deel) -> String {
        let naam = graaf.naam(verdieping: deel.verdieping)
        let meter = "\(Int(deel.meter.rounded())) m"
        guard let laatste = deel.knopen.last else { return naam }
        if laatste.id == overzicht.doel?.id {
            return "\(naam): \(meter) tot \(laatste.naam)."
        }
        let middel = laatste.soort == .lift ? "de lift" : "de trap"
        return "\(naam): \(meter) tot \(middel), daarna een QR-code scannen op de volgende verdieping."
    }
}

/// Tekent één verdieping van de graaf: gangen grijs, de route blauw, start en doel.
/// Er is (nog) geen afbeelding van het plan in de app: de gangen zelf vormen de kaart.
private struct PlanKaart: View {
    let graaf: Graaf
    let overzicht: RouteOverzicht
    let verdieping: Int

    var body: some View {
        Canvas { context, grootte in
            let knopen = graaf.knopen.values.filter { $0.verdieping == verdieping }
            guard !knopen.isEmpty else { return }

            // Schaal zodat alle punten van deze verdieping passen (noorden van het plan boven).
            let xs = knopen.map(\.x), zs = knopen.map(\.z)
            let minX = xs.min()!, maxX = xs.max()!, minZ = zs.min()!, maxZ = zs.max()!
            let marge: CGFloat = 28
            let breedte = CGFloat(max(maxX - minX, 1)), hoogte = CGFloat(max(maxZ - minZ, 1))
            let schaal = min((grootte.width - 2 * marge) / breedte, (grootte.height - 2 * marge) / hoogte)
            let dx = (grootte.width - breedte * schaal) / 2, dz = (grootte.height - hoogte * schaal) / 2
            func punt(_ k: Knoop) -> CGPoint {
                CGPoint(x: dx + CGFloat(k.x - minX) * schaal, y: dz + CGFloat(k.z - minZ) * schaal)
            }

            // Gangen
            var gangen = Path()
            for (a, b) in graaf.verbindingenOp(verdieping: verdieping) {
                gangen.move(to: punt(a))
                gangen.addLine(to: punt(b))
            }
            context.stroke(gangen, with: .color(.gray.opacity(0.35)),
                           style: StrokeStyle(lineWidth: max(6, 1.6 * schaal), lineCap: .round, lineJoin: .round))

            // Lokalen
            for k in knopen where k.soort == .lokaal {
                let p = punt(k)
                context.fill(Path(ellipseIn: CGRect(x: p.x - 3, y: p.y - 3, width: 6, height: 6)), with: .color(.gray))
                context.draw(Text(k.naam).font(.system(size: 10)).foregroundColor(.gray),
                             at: CGPoint(x: p.x, y: p.y - 10))
            }
            for k in knopen where k.soort == .trap || k.soort == .lift {
                let p = punt(k)
                context.draw(Image(systemName: k.soort == .lift ? "arrow.up.arrow.down.square.fill" : "stairs"),
                             at: p)
            }

            // Route op deze verdieping
            guard let deel = overzicht.delen.first(where: { $0.verdieping == verdieping }),
                  let eerste = deel.knopen.first, let laatste = deel.knopen.last else { return }
            var route = Path()
            route.move(to: punt(eerste))
            for k in deel.knopen.dropFirst() { route.addLine(to: punt(k)) }
            context.stroke(route, with: .color(.blue),
                           style: StrokeStyle(lineWidth: 5, lineCap: .round, lineJoin: .round))

            // Begin: jij (bij de QR-code) of de trap waar je aankomt.
            let begin = punt(eerste)
            context.fill(Path(ellipseIn: CGRect(x: begin.x - 8, y: begin.y - 8, width: 16, height: 16)), with: .color(.green))
            context.stroke(Path(ellipseIn: CGRect(x: begin.x - 8, y: begin.y - 8, width: 16, height: 16)), with: .color(.white), lineWidth: 2)
            context.draw(Text(eerste.id == overzicht.start?.id ? "Jij" : eerste.naam).font(.caption.bold()),
                         at: CGPoint(x: begin.x, y: begin.y + 16))

            // Einde: bestemming of de trap/lift naar de volgende verdieping.
            let einde = punt(laatste)
            let isDoel = laatste.id == overzicht.doel?.id
            context.fill(Path(ellipseIn: CGRect(x: einde.x - 8, y: einde.y - 8, width: 16, height: 16)),
                         with: .color(isDoel ? .red : .orange))
            context.stroke(Path(ellipseIn: CGRect(x: einde.x - 8, y: einde.y - 8, width: 16, height: 16)), with: .color(.white), lineWidth: 2)
            context.draw(Text(isDoel ? laatste.naam : "naar \(laatste.soort == .lift ? "lift" : "trap")").font(.caption.bold()),
                         at: CGPoint(x: einde.x, y: einde.y + 16))
        }
        .accessibilityLabel("Plattegrond met de route")
    }
}
