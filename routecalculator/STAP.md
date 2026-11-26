# Stap 1: Opzet

De basis van het project: de routecalculator, het bouwscript en de testen.

- `src/main/kotlin/routeberekening/…`: de calculator (model, netwerk, kosten, toegang, zoeken, aanwijzingen).
  Ten opzichte van jullie calculator zijn twee fouten opgelost: `Routezoeker` zoekt altijd in het hele netwerk
  (eerst op dezelfde verdieping zoeken gaf soms een veel langere route) en `AanwijzingMaker` geeft ook
  links/rechts bij een bocht op een hulpnode.
- `run.sh`: bouwt alles met `kotlinc` en start de interface (`bash run.sh`) of de testen (`bash run.sh test`).
- `src/test/…`: testrunner zonder bibliotheken met T1 tot T11.
- `editor/Main.kt`: opent een leeg venster, om te zien dat Kotlin en Swing werken.

**Testen:** `bash run.sh test` (15 geslaagd) en `bash run.sh` (een klein venster).
