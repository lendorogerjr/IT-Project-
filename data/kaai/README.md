# Campus Kaai: navigatiegraaf

`kaai.json` (formaat versie 1, zie `docs/FORMAAT.md`) wordt automatisch gegenereerd door `tools/kaai/` uit de PDF "basisplan campus Nijverheidskaai" van 05/11/2025 (A3, 1/500, vectorieel).

## Cijfers

| Niveau | Lokalen |
|---|---|
| Kelder (-1) | 52 |
| Gelijkvloers (0) | 196 |
| 1ste verdieping | 89 |
| 2de verdieping | 64 |
| 3de verdieping | 40 |
| 4de verdieping | 28 |
| 5de verdieping | 2 |

In totaal 3 479 punten (471 lokalen, 40 trapbordessen, de rest gangpunten en deuren) en 4 248 verbindingen. 458 van de 471 lokalen zijn vanuit elk ander lokaal bereikbaar. 249 lokalen hebben een QR-code (`QR_kaai.pdf`, 12 per A4-pagina).

## Hoe het gebouwd wordt

1. **Labels**: de lokaalnummers (blauwe tekst in de PDF) en namen ("zaal 1", "auditorium 5"...) worden rechtstreeks uit de PDF gelezen.
2. **Muren en deuren**: elke verdieping wordt zonder tekst gerenderd; muren en lijnen vormen de obstakels. Deuren zijn de cirkelbogen (kwartcirkel) die in de PDF getekend staan.
3. **Ruimtes**: elke gesloten zone met een nummer is een lokaal. Een zone met 3 of meer buren, of groter dan 250 m², wordt als gang of hal behandeld: daarvan wordt het skelet (middellijn) berekend, opgedeeld in punten om de ~3 m.
4. **Lokaal**: het punt `lokaal` ligt op de hoofddeur (de deur naar de gang). `qr.normaal` wijst van het lokaal naar de gang, afgerond op 45°.
5. **Trappen**: de labels `I-1`, `II-1`... geven een punt `trap` per verdieping (id `TRAP-<nummer>-<niveau>`). Twee opeenvolgende verdiepingen van dezelfde trap worden verbonden (kost 12).
6. **Verbindingen**: als twee stukken gang enkel door een dunne lijn gescheiden zijn (schuifdeur, glazen deur, drempel), worden ze verbonden via een pad dat de dikke muren vermijdt.

Assenstelsel: x naar rechts, z naar beneden, in meter, oorsprong = linkerbovenhoek van de pagina (gelijk voor alle verdiepingen, de plannen liggen op elkaar). 1 PDF-punt = 0,1764 m.

## Id's

`<gebouw><nummer>`, bijvoorbeeld `A113`, `B059`, `C110A`. Nummers komen in meerdere gebouwen voor, vandaar de letter. **De letter is afgeleid**: het is de dichtstbijzijnde grote letter A/B/C/D op het plan. Na te kijken met de officiële notatie van de school. Getoonde namen: "Zaal 1 (B059)", "Auditorium 5", anders het nummer.

## Gekende beperkingen (ter plaatse na te kijken)

- **Geen liften**: ze staan niet op de plannen. Manueel toe te voegen in de editor (`soort: lift`).
- **Grijze zones = daken en vides** (nummers x80 tot x89, bijvoorbeeld 180, 185, 280): niet beloopbaar, dus genegeerd.
- Op het gelijkvloers zijn het noordelijke deel (C, D, agora) en het zuidelijke deel (A, B) niet verbonden op hetzelfde niveau: volgens de plannen loopt de verbinding buiten of via een andere verdieping. Routes gaan dus soms via de kelder of de 1ste verdieping. Ter plaatse te bevestigen.
- Grote open ruimtes (parking in de kelder, zalen) krijgen automatisch gegenereerde gangpunten: de route klopt, maar is niet altijd de meest natuurlijke.
- Lokalen die nog niet bereikbaar zijn (deur niet herkend): A018, A027, C011, C011B (gelijkvloers), A118, B102b, B102c, D101 (1ste), D280 (2de), A323, D380, D382 (3de), A504 (5de).
- De schaal komt uit het titelblok (1/500 op A3). Een echte meting (bijvoorbeeld de lengte van hal 114 in gebouw A) zou de waarde bevestigen.
