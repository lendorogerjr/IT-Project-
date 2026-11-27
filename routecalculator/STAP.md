# Stap 5: Nodes toevoegen

De eerste invoer, deel 2: het netwerk op de plattegrond tekenen.

- `Nodes.kt` (nieuw): `ENode` en `EVerbinding`, hulpfuncties (id maken, verwijderen, trappen en liften met
  dezelfde naam tussen verdiepingen verbinden) en het tekenen van nodes en verbindingen.
- `NodePaneel.kt` (nieuw): tabblad Node: naam, type, toegang per profiel, gesloten, verwijderen.
- `Voorbeeld.kt` (nieuw): het testnetwerk als kaart (Bestand > Voorbeeld openen).
- `Project.kt`: profielen, nodes en verbindingen, ook bij opslaan en openen.
- `KaartPaneel.kt`: nodes plaatsen, selecteren, verslepen, verbinden, Delete, uitlijnen op rechte hoeken, voorproefje.
- `Hoofdvenster.kt`: plaatsen, verbinden (ook tussen gebouwen met een afstand), profielen, automatisch verbinden.

**Testen:** Bestand > Voorbeeld openen, of teken zelf een gang met lokalen, deuren en een trap.
`bash run.sh test` (23 geslaagd).

## Bestanden ten opzichte van stap 4

Nieuw:
- `src/main/kotlin/routeberekening/editor/NodePaneel.kt`
- `src/main/kotlin/routeberekening/editor/Nodes.kt`
- `src/main/kotlin/routeberekening/editor/Voorbeeld.kt`

Gewijzigd:
- `README.md`
- `src/main/kotlin/routeberekening/editor/Hoofdvenster.kt`
- `src/main/kotlin/routeberekening/editor/KaartPaneel.kt`
- `src/main/kotlin/routeberekening/editor/Project.kt`
- `src/test/kotlin/routeberekening/EditorTests.kt`
- `src/test/kotlin/routeberekening/Tests.kt`
