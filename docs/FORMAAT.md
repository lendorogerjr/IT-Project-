# JSON-formaat `binnennav-graaf` (versie 1)

Eén bestand per gebouw, zo gelezen door de iOS-app (en later Android). Het wordt gemaakt met de editor (`editor/index.html`). De veldnamen zijn Nederlands, zoals de code.

## Assenstelsel

- Eenheid: **meter**.
- Van bovenaf gezien, zoals een papieren plan: **x naar rechts**, **z naar beneden** in de afbeelding van het plan.
- De oorsprong (0, 0) is het punt dat in de editor gekozen wordt met het gereedschap "Oorsprong". Kies **hetzelfde fysieke punt op elke verdieping** (bijvoorbeeld een hoek van de traphal) als je wilt dat de verdiepingen op elkaar liggen. De app heeft dat vandaag niet nodig (ze herijkt bij elke QR-code), maar een latere 2D-weergave over meerdere verdiepingen wel.
- Geen georeferentie: het noorden speelt voor de app geen rol.

## Structuur

```json
{
  "formaat": "binnennav-graaf",
  "versie": 1,
  "gebouw": { "id": "testgebouw", "naam": "Testgebouw" },
  "verdiepingen": [
    {
      "niveau": 0,
      "naam": "Verdieping 0",
      "plan": {
        "bestand": "testgebouw_v0.png",
        "breedtePx": 1100, "hoogtePx": 900,
        "meterPerPixel": 0.02,
        "oorsprongPx": [100, 300]
      }
    }
  ],
  "knopen": [
    { "id": "B1", "naam": "B1", "soort": "lokaal", "verdieping": 0, "x": 1, "z": -1,
      "qr": { "code": "BINNENNAV:B1", "normaal": [0, 1] } },
    { "id": "G1", "naam": "Gang bij B1", "soort": "gang", "verdieping": 0, "x": 1, "z": 0 },
    { "id": "T0", "naam": "Trap", "soort": "trap", "verdieping": 0, "x": 14.5, "z": 6 }
  ],
  "verbindingen": [
    { "van": "B1", "naar": "G1" },
    { "van": "T0", "naar": "T1", "kost": 12 }
  ]
}
```

| Veld | Verplicht | Betekenis |
|---|---|---|
| `formaat` | ja | Altijd `"binnennav-graaf"`. |
| `versie` | ja | `1`. Een app weigert een bestand dat nieuwer is dan zijzelf. |
| `gebouw.id`, `gebouw.naam` | ja | Technische id en getoonde naam. |
| `verdiepingen[].niveau` | ja | Geheel getal: 0 = gelijkvloers, 1 = 1ste verdieping, -1 = kelder. |
| `verdiepingen[].plan` | nee | Info voor de editor (afbeelding, schaal, oorsprong). De app negeert dit voorlopig. |
| `knopen[].id` | ja | Uniek in het bestand. Letters, cijfers, `.`, `-`, `_`. Dit staat in de QR-code. |
| `knopen[].naam` | ja | Wat de gebruiker ziet en zoekt ("B4", "Aula"). |
| `knopen[].soort` | ja | `lokaal` (deur van een lokaal), `gang` (punt in een gang), `trap`, `lift`. |
| `knopen[].verdieping` | ja | `niveau` van de verdieping. |
| `knopen[].x`, `z` | ja | Positie in meter (op de cm). |
| `knopen[].qr` | nee | Aanwezig als er naast deze deur een QR-code hangt. |
| `qr.code` | nee | Altijd `"BINNENNAV:" + id`. Overbodig, maar handig voor Android en het afdrukken. |
| `qr.normaal` | ja als `qr` | Eenheidsvector `[x, z]` loodrecht op de muur, **van de muur naar de gang**. Die geeft de oriëntatie van het plan op het moment van de scan. |
| `verbindingen[].van/naar` | ja | Een doorgang die in beide richtingen begaanbaar is. |
| `verbindingen[].kost` | nee | Vaste kost in "meter". Zonder kost: de afstand (zelfde verdieping) of 10 per verdieping (trap/lift). |

## Rekenregels (gelijk voor iOS, Android en editor)

- Kost van een verbinding zonder `kost`: euclidische afstand in x/z op dezelfde verdieping, anders `10 × |Δverdieping|`. De x/z-afstand tussen twee verdiepingen wordt niet gebruikt, omdat twee plannen niet altijd op elkaar liggen.
- Route: A*, heuristiek = rechte lijn op de verdieping van de bestemming, 0 elders (ze overschat nooit, dus de route blijft optimaal).
- Routeoverzicht (afstand en tijd getoond voor "Start"): afstand = som van de x/z-lengtes van de verbindingen **op dezelfde verdieping** (een trap/lift telt 0 m); tijd = afstand / **1,3 m/s** + **30 s per verdieping**. Weergave: "< 1 min" onder 60 s, anders "± N min" naar boven afgerond. De route wordt opgedeeld in één stuk per verdieping.
- Inhoud van de QR-code: `BINNENNAV:<id>`. De code hangt plat tegen de muur naast de deur, midden op ongeveer 1,40 m hoogte.

## Geplande uitbreidingen (versie 2, niet compatibel)

`versie` gaat omhoog als we bijvoorbeeld deurplaten als visuele ankers toevoegen (stap 5), verbindingen in één richting, of toegankelijkheidsinfo (trap niet bruikbaar met rolstoel). Onbekende velden moeten door lezers genegeerd worden, dus een optioneel veld toevoegen blijft versie 1.
