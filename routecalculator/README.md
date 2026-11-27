# Routecalculator met kaarteditor

Kotlin-desktopvenster (Swing) rond de routecalculator, zonder HTML en zonder extra bibliotheken.
Het project is in 8 stappen opgebouwd. Elke zip is een volledig, werkend project; `STAP.md` zegt wat er in deze stap bijkwam.

| Stap | Inhoud |
|---|---|
| 1 | Opzet: calculator, bouwen met `run.sh`, testen |
| 2 | Editor, basisopmaak: venster, werkbalk, lijst, kaart, tabbladen, statusbalk |
| 3 | Opmaak van de editor: stijl, symbolen, legende |
| 4 | Kaarten inladen: plannen, afbeelding, schaal, zoomen, opslaan en openen |
| 5 | Nodes toevoegen: plaatsen, verbinden, eigenschappen, trappen en liften |
| 6 | Routeberekening: kaart omzetten naar de calculator, controle, route op de kaart |
| 7 | Tweede invoer: vertrek, bestemming en profiel, aanwijzingen |
| 8 | Uitvoer: route als png per verdieping, `route.txt` en `route.json` |

## Installeren en starten (Mac)

```
brew install openjdk kotlin
bash run.sh          # de interface starten
bash run.sh test     # de testen draaien
```

De eerste keer bouwen duurt een halve minuut. In VS Code kan het ook via Terminal > Taak uitvoeren.

## Werkwijze

1. **Kaart > Plan toevoegen**: gebouwcode (bv. `B`), gebouwnaam, verdieping en de plattegrond (png of jpg).
   Een pdf zet je eerst om naar png (Voorvertoning > Archief > Exporteer). Zonder afbeelding krijg je een leeg raster.
2. **Schaal instellen**: klik twee punten waarvan je de echte afstand kent en geef die in meter.

Zoomen met het scrollwiel, schuiven met de rechtermuisknop, *Passend* toont het hele plan.

## Pakketten

| Pakket | Inhoud |
|---|---|
| `model`, `netwerk`, `kosten`, `toegang`, `zoeken`, `aanwijzingen` | De calculator (stap 1) |
| `editor` | De interface (vanaf stap 2) |

De calculator weet niets van de interface.
