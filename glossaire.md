# Glossaire & conventions

Cette page fixe les mots clés et les conventions utilisées dans le projet.

## Coordonnées

- **Origine** : (0,0) en haut à gauche.
- **x** augmente vers la droite.
- **y** augmente vers le bas.
- Les indices commencent à 0.

## Types de cases (tokens)

| Token | Signification | Marchable |
| --- | --- | --- |
| `Mu` | Mur | Non |
| `So` | Sol | Oui |
| `Bs` | Bonus saut (frites) | Oui |
| `Bp` | Bonus trois pas (bières) | Oui |
| Nombre | Moule (points) | Oui |

## Commandes envoyées

| Type | Exemple | Description |
| --- | --- | --- |
| Simple | `N`, `S`, `E`, `O`, `C` | Déplacement ou passe | 
| Saut | `Bs-S` | Avance de 2 cases si la case d'arrivée est valide |
| 3 pas | `Bp-E-E-S` | Enchaîne 3 déplacements (les invalides sont ignorés) |

## Règles exactes

- **Saut** :
  - La case à +2 doit être marchable.
  - Si +2 est invalide, le joueur tente un pas simple.
  - Le bonus n'est consommé que si +2 est valide.

- **Trois pas** :
  - Les pas sont tentés dans l'ordre.
  - Un pas invalide est ignoré (le reste continue).

- **Moules / bonus** :
  - Une case bonus ou moule disparaît après passage.
  - Un joueur peut partager une case avec un autre joueur.

## Conventions côté IA

- **Distance** : nombre de tours minimum pour atteindre une case.
- **Cible** : case choisie comme objectif principal (moule ou bonus).
- **Top‑K** : liste courte des meilleures cibles candidates.
- **Stagnation** : suite de tours sans points.

## Conventions de log

- `Tour X` : numéro de tour.
- `Action` : commande envoyée.
- `Arrivée` : case atteinte.
- `Gain points` : points gagnés ce tour.
