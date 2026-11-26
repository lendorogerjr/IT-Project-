# Stap 2: Editor, basisopmaak

Het venster krijgt zijn indeling, nog zonder stijl of werking:

- `Modus.kt`: de vier modi van de kaart (selecteren, nodes plaatsen, verbinden, schaal instellen).
- `Hoofdvenster.kt`: menubalk, werkbalk met de modi, lijst "Plannen" links, kaart in het midden,
  tabbladen Node en Route rechts, statusbalk onderaan.
- `KaartPaneel.kt`: het middenvlak; toont voorlopig enkel de gekozen modus.

**Testen:** `bash run.sh`, klik op de modi: de statusbalk en de kaart volgen.

## Bestanden ten opzichte van stap 1

Nieuw:
- `src/main/kotlin/routeberekening/editor/Hoofdvenster.kt`
- `src/main/kotlin/routeberekening/editor/KaartPaneel.kt`
- `src/main/kotlin/routeberekening/editor/Modus.kt`

Gewijzigd:
- `src/main/kotlin/routeberekening/editor/Main.kt`
