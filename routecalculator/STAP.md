# Stap 4: Kaarten inladen

De eerste invoer, deel 1: plattegronden inladen.

- `Plan.kt` (nieuw): één verdieping van één gebouw, met afbeelding en schaal (meter per pixel), en het tekenen
  van de plattegrond (of een raster als er geen afbeelding is).
- `Project.kt` (nieuw): de kaart met gebouwen en plannen, opslaan en openen als `.json`
  (afbeeldingen relatief tegenover het kaartbestand).
- `Json.kt` (nieuw): kleine JSON-lezer en -schrijver zonder bibliotheek.
- `KaartPaneel.kt`: plan tonen, zoomen (scrollwiel), schuiven (slepen), *Passend*, schaal instellen met twee klikken.
- `Hoofdvenster.kt`: menu Bestand (nieuw, openen, opslaan) en Kaart (plan toevoegen, afbeelding wijzigen,
  schaal, plan verwijderen), plannenlijst, vragen om op te slaan bij afsluiten.

**Testen:** Kaart > Plan toevoegen, kies een png van een grondplan, stel de schaal in, sla op en open opnieuw.
`bash run.sh test` (19 geslaagd).

## Bestanden ten opzichte van stap 3

Nieuw:
- `src/main/kotlin/routeberekening/editor/Json.kt`
- `src/main/kotlin/routeberekening/editor/Plan.kt`
- `src/main/kotlin/routeberekening/editor/Project.kt`
- `src/test/kotlin/routeberekening/EditorTests.kt`

Gewijzigd:
- `README.md`
- `src/main/kotlin/routeberekening/editor/Hoofdvenster.kt`
- `src/main/kotlin/routeberekening/editor/KaartPaneel.kt`
- `src/test/kotlin/routeberekening/Tests.kt`
