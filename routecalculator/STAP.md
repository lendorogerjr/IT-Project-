# Stap 3: Opmaak van de editor

De stijl, los van de werking, zodat het venster en later de export dezelfde opmaak delen:

- `Stijl.kt` (nieuw): `Tekenaar` met de kleuren en symbolen per soort node (zoals in "Node Plaatsing":
  vierkant = eindpunt, cirkel = route, streepje = hulpnode, rood = deur, blauw = trap, groen = lift),
  labels, genummerde bolletjes, de `Legende`, `TypeWeergave` voor keuzelijsten, en `formulier()` voor dialogen.
- `Main.kt`: het uiterlijk van het besturingssysteem (op de Mac het Mac-uiterlijk).
- `Hoofdvenster.kt`: legende links, keuzelijst met de soorten nodes in de werkbalk, marges, positie in de statusbalk.
- `KaartPaneel.kt`: achtergrondkleur en vloeiende tekst.

**Testen:** `bash run.sh`: legende en keuzelijst met soorten nodes zijn zichtbaar.

## Bestanden ten opzichte van stap 2

Nieuw:
- `src/main/kotlin/routeberekening/editor/Stijl.kt`

Gewijzigd:
- `src/main/kotlin/routeberekening/editor/Hoofdvenster.kt`
- `src/main/kotlin/routeberekening/editor/KaartPaneel.kt`
- `src/main/kotlin/routeberekening/editor/Main.kt`
