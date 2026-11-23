# Campus Kaai : graphe de navigation

`kaai.json` (format `binnennav-graaf` v1, voir `docs/FORMAAT.md`) est généré automatiquement par `tools/kaai/` à partir du PDF « basisplan campus Nijverheidskaai » du 05/11/2025 (A3, 1/500, vectoriel).

## Chiffres

| Niveau | Locaux |
|---|---|
| Kelder (-1) | 52 |
| Gelijkvloers (0) | 196 |
| 1ste verdieping | 89 |
| 2de verdieping | 64 |
| 3de verdieping | 40 |
| 4de verdieping | 28 |
| 5de verdieping | 2 |

Au total 3 479 points (471 locaux, 40 paliers d'escalier, le reste = points de couloir et portes) et 4 248 liaisons. 458 locaux sur 471 sont joignables depuis n'importe quel autre. 249 locaux ont un QR code (`QR_kaai.pdf`, 12 par page A4).

## Comment c'est construit

1. **Étiquettes** : les numéros de local (texte bleu du PDF) et les noms (« zaal 1 », « auditorium 5 »…) sont lus directement dans le PDF.
2. **Murs et portes** : chaque étage est rendu sans texte ; les murs et traits forment les obstacles. Les portes sont les arcs de cercle (quart de cercle) dessinés dans le PDF.
3. **Espaces** : chaque zone fermée qui contient un numéro est un local. Une zone avec 3 voisins ou plus, ou plus de 250 m², est traitée comme un couloir ou un hall : on calcule son squelette (ligne médiane), découpé en points tous les ~3 m.
4. **Local** : le point `lokaal` est placé sur la porte principale (celle qui donne sur le couloir). `qr.normaal` pointe du local vers le couloir, arrondi à 45°.
5. **Escaliers** : les étiquettes `I-1`, `II-1`… donnent un point `trap` par étage (id `TRAP-<numéro>-<niveau>`). Deux étages consécutifs du même escalier sont reliés (coût 12).
6. **Raccords** : quand deux morceaux de couloir ne sont séparés que par un trait fin (porte coulissante, porte vitrée, seuil), un chemin qui évite les murs épais les relie.

Repère : x vers la droite, z vers le bas, en mètres, origine = coin haut-gauche de la page (identique pour tous les étages, les plans sont calés entre eux). 1 point PDF = 0,1764 m.

## Identifiants

`<bâtiment><numéro>`, par exemple `A113`, `B059`, `C110A`. Les numéros se répètent d'un bâtiment à l'autre, d'où la lettre. **La lettre est déduite** : c'est la grande lettre A/B/C/D la plus proche sur le plan. À vérifier avec la notation officielle de l'école. Noms affichés : « Zaal 1 (B059) », « Auditorium 5 », sinon le numéro.

## Limites connues (à vérifier sur place)

- **Pas d'ascenseurs** : ils ne sont pas marqués sur les plans. À ajouter à la main dans l'éditeur (`soort: lift`).
- **Zones grises = toits et vides** (numéros x80 à x89, par exemple 180, 185, 280) : non praticables, donc ignorées.
- Au rez-de-chaussée, la partie nord (C, D, agora) et la partie sud (A, B) ne sont pas reliées au même niveau : sur les plans, la liaison semble passer par l'extérieur ou par un autre étage. Les itinéraires passent donc parfois par le kelder ou le 1er étage. À confirmer sur place.
- Les grands espaces ouverts (parking du kelder, zalen) ont des points de couloir générés automatiquement : le trajet est correct mais pas toujours le plus naturel.
- Locaux pas encore joignables (porte non reconnue) : A018, A027, C011, C011B (gelijkvloers), A118, B102b, B102c, D101 (1er), D280 (2e), A323, D380, D382 (3e), A504 (5e).
- L'échelle vient du cartouche (1/500 sur A3). Une mesure réelle (par exemple la longueur du hall 114 dans le bâtiment A) confirmerait la valeur.
