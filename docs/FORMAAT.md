# Format JSON `binnennav-graaf` (version 1)

Un seul fichier par bâtiment, lu tel quel par l'app iOS (et plus tard Android). Il est produit par l'éditeur (`editor/index.html`). Les noms de champs sont en néerlandais, comme le code.

## Repère

- Unités : **mètres**.
- Vu de dessus, comme un plan papier : **x vers la droite**, **z vers le bas** de l'image du plan.
- L'origine (0, 0) est le point choisi dans l'éditeur avec l'outil « Oorsprong ». Prends le **même point physique à chaque étage** (un coin de la cage d'escalier par exemple) si tu veux que les étages se superposent. L'app n'en a pas besoin aujourd'hui (elle se recale à chaque QR), mais une vue 2D multi-étages en aura besoin plus tard.
- Pas de géoréférencement : le nord n'a aucune importance pour l'app.

## Structure

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

| Champ | Obligatoire | Signification |
|---|---|---|
| `formaat` | oui | Toujours `"binnennav-graaf"`. |
| `versie` | oui | `1`. Une app refuse un fichier plus récent qu'elle. |
| `gebouw.id`, `gebouw.naam` | oui | Identifiant technique et nom affiché. |
| `verdiepingen[].niveau` | oui | Entier : 0 = rez, 1 = 1er, -1 = sous-sol. |
| `verdiepingen[].plan` | non | Infos pour l'éditeur (image, échelle, origine). L'app les ignore pour l'instant. |
| `knopen[].id` | oui | Unique dans le fichier. Lettres, chiffres, `.`, `-`, `_`. C'est ce qui est écrit dans le QR. |
| `knopen[].naam` | oui | Ce que l'utilisateur voit et cherche (« B4 », « Aula »). |
| `knopen[].soort` | oui | `lokaal` (porte d'un local), `gang` (point de couloir), `trap`, `lift`. |
| `knopen[].verdieping` | oui | `niveau` de l'étage. |
| `knopen[].x`, `z` | oui | Position en mètres (au cm près). |
| `knopen[].qr` | non | Présent si un QR est collé à côté de cette porte. |
| `qr.code` | non | Toujours `"BINNENNAV:" + id`. Redondant, pratique pour Android et l'impression. |
| `qr.normaal` | oui si `qr` | Vecteur unitaire `[x, z]` perpendiculaire au mur, **du mur vers le couloir**. C'est lui qui donne l'orientation du plan au moment du scan. |
| `verbindingen[].van/naar` | oui | Un passage praticable dans les deux sens. |
| `verbindingen[].kost` | non | Coût fixe en « mètres ». Sans coût : la distance (même étage) ou 10 par étage (escalier/ascenseur). |

## Règles de calcul (identiques iOS, Android, éditeur)

- Coût d'une arête sans `kost` : distance euclidienne en x/z si même étage, sinon `10 × |Δétage|`. On n'utilise pas la distance x/z entre deux étages, parce que deux plans ne sont pas forcément calés l'un sur l'autre.
- Itinéraire : A*, heuristique = ligne droite sur le même étage que la destination, 0 ailleurs (elle ne surestime jamais, donc le chemin reste optimal).
- Aperçu du trajet (distance et temps affichés avant « Start ») : distance = somme des longueurs x/z des arêtes **du même étage** (un escalier/ascenseur compte 0 m) ; temps = distance / **1,3 m/s** + **30 s par étage changé**. Affichage : « < 1 min » sous 60 s, sinon « ± N min » arrondi vers le haut. Le trajet est découpé en un morceau par étage.
- Contenu du QR : `BINNENNAV:<id>`. Le QR est collé à plat sur le mur, à côté de la porte, centre à environ 1,40 m.

## Évolutions prévues (version 2, non compatibles)

On augmentera `versie` si on ajoute par exemple des plaques de porte comme ancres visuelles (étape 5), des arêtes à sens unique ou des marqueurs d'accessibilité (escalier interdit en fauteuil). Les champs inconnus doivent être ignorés par les lecteurs, donc ajouter un champ optionnel reste en version 1.
