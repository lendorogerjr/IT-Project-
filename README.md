# Indoornavigatie in augmented reality (werktitel)

App om de weg te vinden binnen campus Kaai (EhB). De gebruiker scant de QR-code naast een deur, kiest een bestemming (bijvoorbeeld "B059" of "Zaal 1") en de app tekent pijlen op de vloer tot aan het lokaal.

De naam van de app ligt nog niet vast. "BinnenNav" is enkel een werknaam in de code (Xcode-target, formaat `binnennav-graaf`, QR-prefix `BINNENNAV:`).

De uiteindelijke app moet op **iOS en Android** draaien. Alles wat gedeeld wordt (JSON-formaat van het gebouw, inhoud van de QR-codes, plattegrondeditor) is daarom platformonafhankelijk.

## Inhoud van de repo

| Map | Inhoud |
|---|---|
| `ios/` | iOS-app (Swift, SwiftUI, ARKit, RealityKit, iOS 17+). Stand: stap 3 + routeoverzicht, getest op iPhone. |
| `editor/` | Offline webeditor voor plattegronden: open `editor/index.html` in een browser. Plan laden, punten plaatsen, JSON exporteren, QR-codes afdrukken. |
| `docs/FORMAAT.md` | JSON-formaat van de graaf (versie 1): assenstelsel, velden, rekenregels (kost, A*, looptijd). Ook te volgen in de Android-app. |
| `data/kaai/` | Graaf van campus Kaai (7 niveaus, kelder tot 5de verdieping) en PDF met de QR-codes. Zie `data/kaai/README.md`. |
| `data/testgebouw/` | Klein fictief gebouw met 2 verdiepingen om te testen zonder ter plaatse te zijn. |
| `tools/kaai/` | Python-scripts die `data/kaai/kaai.json` opnieuw opbouwen uit de PDF met de plannen. |

## De plannen van de school staan niet in de repo

De repo is publiek. De plannen van EhB (PDF, afbeeldingen per verdieping, controlebeelden) horen in de map `plannen/`, die git negeert. Vraag ze aan Ayoub. De JSON bevat enkel punten en verbindingen.

## De iOS-app starten

1. `brew install xcodegen` (eenmalig).
2. `cd ios && xcodegen generate` en open daarna het gegenereerde `.xcodeproj`.
3. Vervang in `project.yml` `DEVELOPMENT_TEAM` en `PRODUCT_BUNDLE_IDENTIFIER` door die van jezelf (gratis Apple-account = Personal Team) en voer `xcodegen generate` opnieuw uit.
4. Starten op een echte iPhone (ARKit werkt niet in de simulator).

De app bevat standaard `ios/BinnenNav/gebouw.json` (het testgebouw). Voor Kaai: importeer `data/kaai/kaai.json` via het scherm "Kies je startpunt", of vervang `gebouw.json` door dat bestand.

## De graaf van Kaai opnieuw opbouwen

```
pip install -r tools/kaai/requirements.txt
tools/kaai/bouw.sh plannen/2025_plannen_RITCS_kaai.pdf
```

Resultaat: `data/kaai/kaai.json` en `data/kaai/QR_kaai.pdf`. De afbeeldingen per verdieping en de controlebeelden komen in `plannen/kaai/` (buiten git). Manueel verbeteren: open de JSON in de editor, laad de afbeelding van de verdieping uit `plannen/kaai/`, pas aan en exporteer.
