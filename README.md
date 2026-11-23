# BinnenNav

Application de navigation intérieure en réalité augmentée pour le campus Kaai (EhB). L'utilisateur scanne le QR code collé à côté d'une porte, choisit sa destination (par exemple « B059 » ou « Zaal 1 »), et l'app dessine des chevrons sur le sol jusqu'au local.

L'app finale doit exister sur **iOS et Android**. Tout ce qui est partagé (format JSON du bâtiment, contenu des QR codes, éditeur de plans) est donc indépendant de la plateforme.

## Contenu du repo

| Dossier | Contenu |
|---|---|
| `ios/` | App iOS (Swift, SwiftUI, ARKit, RealityKit, iOS 17+). État : étape 3 + aperçu du trajet, testée sur iPhone. |
| `editor/` | Éditeur de plans web, hors ligne : ouvrir `editor/index.html` dans un navigateur. Charger un plan, poser les points, exporter le JSON, imprimer les QR codes. |
| `docs/FORMAAT.md` | Format JSON `binnennav-graaf` v1 : repère, champs, règles de calcul (coût, A*, temps de marche). À respecter aussi côté Android. |
| `data/kaai/` | Graphe du campus Kaai (7 niveaux, kelder → 5e) et PDF des QR codes. Voir `data/kaai/README.md`. |
| `data/testgebouw/` | Petit bâtiment fictif sur 2 étages pour tester sans être sur place. |
| `tools/kaai/` | Scripts Python qui reconstruisent `data/kaai/kaai.json` à partir du PDF des plans. |

## Les plans de l'école ne sont pas dans le repo

Le repo est public. Les plans EhB (PDF, images par étage, vues de contrôle) restent dans le dossier `plannen/`, ignoré par git. Demande-les à Ayoub. Le JSON ne contient que des points et des liaisons.

## Lancer l'app iOS

1. `brew install xcodegen` (une seule fois).
2. `cd ios && xcodegen generate`, puis ouvrir `BinnenNav.xcodeproj`.
3. Dans `project.yml`, remplacer `DEVELOPMENT_TEAM` et `PRODUCT_BUNDLE_IDENTIFIER` par les tiens (compte Apple gratuit = Personal Team), puis relancer `xcodegen generate`.
4. Lancer sur un vrai iPhone (ARKit ne marche pas dans le simulateur).

L'app embarque `ios/BinnenNav/gebouw.json` (le bâtiment de test). Pour Kaai : dans l'écran « Kies je startpunt », importer `data/kaai/kaai.json`, ou remplacer `gebouw.json` par ce fichier.

## Reconstruire le graphe Kaai

```
pip install -r tools/kaai/requirements.txt
tools/kaai/bouw.sh plannen/2025_plannen_RITCS_kaai.pdf
```

Résultat : `data/kaai/kaai.json` et `data/kaai/QR_kaai.pdf` ; les images des étages et les vues de contrôle vont dans `plannen/kaai/` (hors git). Pour corriger à la main : ouvrir le JSON dans l'éditeur, charger l'image de l'étage depuis `plannen/kaai/`, corriger, exporter.
